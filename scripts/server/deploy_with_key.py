#!/usr/bin/env python3
"""Upload a release and invoke the restricted production deployment command."""

import argparse
from pathlib import Path
import stat

import paramiko


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="47.116.62.234")
    parser.add_argument("--user", default="deploy")
    parser.add_argument("--key", default=str(Path.home() / ".ssh/id_ed25519_xinke_prod"))
    parser.add_argument("--release", required=True)
    parser.add_argument("--stamp", required=True)
    args = parser.parse_args()

    release = Path(args.release).resolve()
    repo = Path(__file__).resolve().parents[2]
    artifacts = {
        "xinke-admin.jar": release / "xinke-admin.jar",
        "xinke-ui.tar.gz": release / "xinke-ui.tar.gz",
        "add-live-metrics-void-report.sql": repo / "config/database/add-live-metrics-void-report.sql",
        "add-bi-live-anchor-segment-fact.sql": repo / "config/database/add-bi-live-anchor-segment-fact.sql",
    }
    for path in artifacts.values():
        if not path.is_file() or not path.stat().st_size:
            raise RuntimeError(f"missing artifact: {path}")

    remote = f"/home/deploy/releases/release-{args.stamp}"
    client = paramiko.SSHClient()
    client.load_system_host_keys(str(Path.home() / ".ssh/known_hosts"))
    client.set_missing_host_key_policy(paramiko.RejectPolicy())
    client.connect(args.host, username=args.user, key_filename=args.key, look_for_keys=False, timeout=10)
    try:
        run(client, f"mkdir -p {remote} && chmod 0700 {remote}")
        sftp = client.open_sftp()
        try:
            for name, path in artifacts.items():
                sftp.put(str(path), f"{remote}/{name}")
        finally:
            sftp.close()
        output = run(client, f"sudo -n /usr/local/sbin/xinke-release deploy {remote} {args.stamp}", timeout=240)
        print(output, end="")
        return 0
    finally:
        client.close()


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
