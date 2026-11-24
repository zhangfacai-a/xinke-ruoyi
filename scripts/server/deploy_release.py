#!/usr/bin/env python3
"""Atomically deploy an uploaded XinKe backend jar and UI archive over SSH."""

import argparse
import os
import posixpath
import sys
import time

import paramiko


DEPLOY_ROOT = "/ruoyi"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", required=True)
    parser.add_argument("--incoming", required=True)
    parser.add_argument("--stamp", required=True)
    parser.add_argument("--username", default="root")
    args = parser.parse_args()
    password = os.environ.get("XINKE_DEPLOY_PASSWORD", "")
    if not password:
        raise RuntimeError("XINKE_DEPLOY_PASSWORD is required")
    incoming = normalized_remote(args.incoming)
    if not incoming.startswith(DEPLOY_ROOT + "/incoming/"):
        raise RuntimeError("incoming directory must be under /ruoyi/incoming")
    if not args.stamp.replace("-", "").isdigit():
        raise RuntimeError("stamp must contain only digits and hyphens")

    backup = f"{DEPLOY_ROOT}/backup/deploy-{args.stamp}"
    new_dist = f"{DEPLOY_ROOT}/incoming/dist-new-{args.stamp}"
    failed_dist = f"{DEPLOY_ROOT}/incoming/dist-failed-{args.stamp}"
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(args.host, username=args.username, password=password, timeout=10)
    try:
        run(client, f"mkdir -p {backup} {new_dist}")
        run(client, f"tar -tzf {incoming}/xinke-ui.tar.gz >/dev/null")
        run(client, f"tar -xzf {incoming}/xinke-ui.tar.gz -C {new_dist}")
        run(client, f"test -s {new_dist}/index.html")
        run(client, f"test -s {incoming}/xinke-admin.jar")

        run(client, "systemctl stop ruoyi")
        sftp = client.open_sftp()
        try:
            sftp.rename(f"{DEPLOY_ROOT}/xinke-admin.jar", f"{backup}/xinke-admin.jar")
            sftp.rename(f"{incoming}/xinke-admin.jar", f"{DEPLOY_ROOT}/xinke-admin.jar")
            sftp.rename(f"{DEPLOY_ROOT}/dist", f"{backup}/dist")
            sftp.rename(new_dist, f"{DEPLOY_ROOT}/dist")
        finally:
            sftp.close()
        run(client, "systemctl start ruoyi")

        for _ in range(45):
            if succeeds(client, "curl -fsS --max-time 3 http://127.0.0.1:8080/ >/dev/null"):
                break
            time.sleep(2)
        else:
            rollback(client, incoming, backup, failed_dist)
            raise RuntimeError("backend health check failed; the previous release was restored")

        run(client, "nginx -t")
        run(client, "curl -fsS --max-time 5 http://127.0.0.1/ >/dev/null")
        print(f"DEPLOY_OK backup={backup}")
        print(run(client, "systemctl show ruoyi --property=ActiveState,MainPID,MemoryCurrent"))
        return 0
    finally:
        client.close()


def normalized_remote(path: str) -> str:
    return posixpath.normpath("/" + path.lstrip("/"))


def run(client: paramiko.SSHClient, command: str, timeout: int = 30) -> str:
    _, stdout, stderr = client.exec_command(command, timeout=timeout)
    output = stdout.read().decode("utf-8", "replace")
    error = stderr.read().decode("utf-8", "replace")
    code = stdout.channel.recv_exit_status()
    if code:
        raise RuntimeError(f"remote command failed ({code}): {command}\n{error}")
    if error:
        print(error, file=sys.stderr, end="")
    return output


def succeeds(client: paramiko.SSHClient, command: str) -> bool:
    try:
        run(client, command, timeout=5)
        return True
    except Exception:
        return False


def rollback(client: paramiko.SSHClient, incoming: str, backup: str, failed_dist: str) -> None:
    succeeds(client, "systemctl stop ruoyi")
    sftp = client.open_sftp()
    try:
        sftp.rename(f"{DEPLOY_ROOT}/xinke-admin.jar", f"{incoming}/xinke-admin.failed.jar")
        sftp.rename(f"{backup}/xinke-admin.jar", f"{DEPLOY_ROOT}/xinke-admin.jar")
        sftp.rename(f"{DEPLOY_ROOT}/dist", failed_dist)
        sftp.rename(f"{backup}/dist", f"{DEPLOY_ROOT}/dist")
    finally:
        sftp.close()
    run(client, "systemctl start ruoyi")


if __name__ == "__main__":
    raise SystemExit(main())
