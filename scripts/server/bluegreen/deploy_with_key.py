#!/usr/bin/env python3
"""Upload a prepared backend release and invoke the blue-green helper."""

import argparse
from pathlib import Path
import re

import paramiko


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="47.116.62.234")
    parser.add_argument("--user", default="deploy")
    parser.add_argument("--key", default=str(Path.home() / ".ssh/id_ed25519_xinke_prod"))
    parser.add_argument("--release", required=True)
    parser.add_argument("--stamp", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"\d{8}-\d{6}", args.stamp):
        parser.error("stamp must be YYYYMMDD-HHMMSS")

    release = Path(args.release).resolve()
    jar = release / "xinke-admin.jar"
    if not jar.is_file() or not jar.stat().st_size:
        parser.error(f"missing backend jar: {jar}")
    ui = release / "xinke-ui.tar.gz"
    if ui.exists() and not ui.stat().st_size:
        parser.error(f"empty UI archive: {ui}")

    remote = f"/home/deploy/releases/release-{args.stamp}"
    client = paramiko.SSHClient()
    client.load_system_host_keys(str(Path.home() / ".ssh/known_hosts"))
    client.set_missing_host_key_policy(paramiko.RejectPolicy())
    client.connect(args.host, username=args.user, key_filename=args.key, look_for_keys=False, timeout=10)
    try:
        run(client, f"mkdir -p {remote} && chmod 0700 {remote}")
        with client.open_sftp() as sftp:
            sftp.put(str(jar), f"{remote}/xinke-admin.jar")
            if ui.is_file():
                sftp.put(str(ui), f"{remote}/xinke-ui.tar.gz")
        print(run(client, f"sudo -n /usr/local/sbin/xinke-bluegreen-release deploy {remote} {args.stamp}", timeout=300), end="")
        return 0
    finally:
        client.close()


def run(client: paramiko.SSHClient, command: str, timeout: int = 30) -> str:
    _, stdout, stderr = client.exec_command(command, timeout=timeout)
    output = stdout.read().decode("utf-8", "replace")
    error = stderr.read().decode("utf-8", "replace")
    if stdout.channel.recv_exit_status():
        raise RuntimeError(f"remote command failed: {command}\n{error}")
    return output


if __name__ == "__main__":
    raise SystemExit(main())
