#!/usr/bin/env python3
"""Prepare, migrate, upload, and deploy the XinKe production release."""

import argparse
import os
from pathlib import Path
import re
import stat
import subprocess
import sys

import paramiko


DEPLOY_ROOT = "/ruoyi"
DB_TABLES = (
    "live_metric_anchor_segment",
    "live_metric_segment_correction",
    "bi_live_anchor_segment_fact",
)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="47.116.62.234")
    parser.add_argument("--release", required=True)
    parser.add_argument("--stamp", required=True)
    parser.add_argument("--username", default="root")
    args = parser.parse_args()

    password = os.environ.get("XINKE_DEPLOY_PASSWORD", "")
    if not password:
        raise RuntimeError("XINKE_DEPLOY_PASSWORD is required")

    release = Path(args.release).resolve()
    jar = release / "xinke-admin.jar"
    ui = release / "xinke-ui.tar.gz"
    repo = Path(__file__).resolve().parents[2]
    migration = repo / "config/database/add-live-metrics-void-report.sql"
    for path in (jar, ui, migration):
        if not path.is_file() or path.stat().st_size == 0:
            raise RuntimeError(f"missing release artifact: {path}")

    db_config = read_database_defaults(repo / "xinke-admin/src/main/resources/application-druid.yml")
    incoming = f"{DEPLOY_ROOT}/incoming/release-{args.stamp}"
    backup = f"{DEPLOY_ROOT}/backup/deploy-{args.stamp}"
    remote_cnf = f"{incoming}/mysql-client.cnf"
    remote_sql = f"{incoming}/add-live-metrics-void-report.sql"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(args.host, username=args.username, password=password, timeout=10)
    try:
        print("[1/6] production preflight")
        run(client, "systemctl is-active --quiet ruoyi")
        run(client, "nginx -t")
        run(client, "test $(df -Pk /ruoyi | awk 'NR==2 {print $4}') -gt 1048576")
        run(client, f"mkdir -p {incoming} {backup}")

        print("[2/6] uploading release and migration")
        sftp = client.open_sftp()
        try:
            sftp.put(str(jar), f"{incoming}/xinke-admin.jar")
            sftp.put(str(ui), f"{incoming}/xinke-ui.tar.gz")
            sftp.put(str(migration), remote_sql)
            with sftp.file(remote_cnf, "w") as handle:
                handle.write(
                    "[client]\n"
                    f"host={db_config['host']}\n"
                    f"port={db_config['port']}\n"
                    f"user={db_config['username']}\n"
                    f"password={db_config['password']}\n"
                    "default-character-set=utf8mb4\n"
                )
            sftp.chmod(remote_cnf, stat.S_IRUSR | stat.S_IWUSR)
        finally:
            sftp.close()

        print("[3/6] backing up affected database tables")
        existing = run(
            client,
            f"mysql --defaults-extra-file={remote_cnf} -N -B -e \"SELECT table_name FROM information_schema.tables WHERE table_schema='{db_config['database']}' AND table_name IN ({','.join(repr(name) for name in DB_TABLES)}) ORDER BY table_name\"",
        ).split()
        if "live_metric_anchor_segment" not in existing:
            raise RuntimeError("required table live_metric_anchor_segment is missing")
        table_args = " ".join(existing)
        run(
            client,
            f"mysqldump --defaults-extra-file={remote_cnf} --single-transaction --routines --triggers {db_config['database']} {table_args} | gzip -c > {backup}/live-metrics.sql.gz",
            timeout=180,
        )
        run(client, f"test -s {backup}/live-metrics.sql.gz")

        print("[4/6] applying idempotent database migration")
        run(
            client,
            f"mysql --defaults-extra-file={remote_cnf} {db_config['database']} < {remote_sql}",
            timeout=120,
        )
        column_count = run(
            client,
            f"mysql --defaults-extra-file={remote_cnf} -N -B {db_config['database']} -e \"SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name IN ('void_reason','void_by','void_name','void_at')\"",
        ).strip()
        if column_count != "4":
            raise RuntimeError(f"database migration verification failed: expected 4 columns, got {column_count}")
        run(client, f"rm -f {remote_cnf}")

        print("[5/6] atomically deploying application")
        deploy = repo / "scripts/server/deploy_release.py"
        completed = subprocess.run(
            [
                sys.executable,
                str(deploy),
                "--host",
                args.host,
                "--incoming",
                incoming,
                "--stamp",
                args.stamp,
                "--username",
                args.username,
            ],
            check=False,
        )
        if completed.returncode:
            raise RuntimeError("application deployment failed; deploy_release.py performed rollback")

        print("[6/6] production smoke checks")
        run(client, "systemctl is-active --quiet ruoyi")
        run(client, "curl -fsS --max-time 5 http://127.0.0.1/ >/dev/null")
        run(client, "curl -fsS --max-time 5 http://127.0.0.1:8080/ >/dev/null")
        print(f"DEPLOY_COMPLETE backup={backup} incoming={incoming}")
        return 0
    finally:
        try:
            run(client, f"rm -f {remote_cnf}")
        except Exception:
            pass
        client.close()


def read_database_defaults(path: Path) -> dict[str, str]:
    from runtime_settings import database_settings
    return database_settings()


def run(client: paramiko.SSHClient, command: str, timeout: int = 30) -> str:
    _, stdout, stderr = client.exec_command(command, timeout=timeout)
    output = stdout.read().decode("utf-8", "replace")
    error = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    if code:
        raise RuntimeError(f"remote command failed ({code}): {command}\n{error}")
    return output


if __name__ == "__main__":
    raise SystemExit(main())
