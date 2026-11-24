#!/usr/bin/env python3
"""One-time setup for key-based, least-privilege XinKe deployments."""

import argparse
import os
from pathlib import Path
import re
import stat

import paramiko


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--host", default="47.116.62.234")
    parser.add_argument("--public-key", required=True)
    parser.add_argument("--root-user", default="root")
    args = parser.parse_args()

    root_password = os.environ.get("XINKE_DEPLOY_PASSWORD", "")
    if not root_password:
        raise RuntimeError("XINKE_DEPLOY_PASSWORD is required for one-time bootstrap")

    repo = Path(__file__).resolve().parents[2]
    public_key = Path(args.public_key).read_text(encoding="ascii").strip()
    if not public_key.startswith("ssh-ed25519 "):
        raise RuntimeError("the deployment public key must be Ed25519")
    db = read_database_defaults(repo / "xinke-admin/src/main/resources/application-druid.yml")
    wrapper = repo / "scripts/server/xinke-release"

    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(args.host, username=args.root_user, password=root_password, timeout=10)
    try:
        run(client, "id -u deploy >/dev/null 2>&1 || useradd --create-home --shell /bin/bash deploy")
        run(client, "if passwd -S deploy | awk '{exit ($2 ~ /^L/ ? 0 : 1)}'; then deploy_secret=$(openssl rand -base64 48); printf 'deploy:%s\\n' \"$deploy_secret\" | chpasswd; unset deploy_secret; fi")
        run(client, "install -d -o deploy -g deploy -m 0700 /home/deploy/.ssh /home/deploy/releases")
        run(client, "touch /home/deploy/.ssh/authorized_keys && chown deploy:deploy /home/deploy/.ssh/authorized_keys && chmod 0600 /home/deploy/.ssh/authorized_keys")

        sftp = client.open_sftp()
        try:
            upload_text(sftp, "/tmp/xinke-deploy.pub", public_key + "\n", 0o600)
            sftp.put(str(wrapper), "/tmp/xinke-release")
            sftp.chmod("/tmp/xinke-release", 0o755)
            upload_text(
                sftp,
                "/tmp/xinke-deploy-db.cnf",
                "[client]\n"
                f"host={db['host']}\nport={db['port']}\nuser={db['username']}\n"
                f"password={db['password']}\ndefault-character-set=utf8mb4\n",
                0o600,
            )
        finally:
            sftp.close()

        run(client, "bash -n /tmp/xinke-release")
        run(client, "cat /tmp/xinke-deploy.pub > /home/deploy/.ssh/authorized_keys")
        run(client, "chown deploy:deploy /home/deploy/.ssh/authorized_keys")
        run(client, "install -o root -g root -m 0755 /tmp/xinke-release /usr/local/sbin/xinke-release")
        run(client, "install -o root -g root -m 0600 /tmp/xinke-deploy-db.cnf /etc/xinke-deploy-db.cnf")
        sudoers = "deploy ALL=(root) NOPASSWD: /usr/local/sbin/xinke-release *\n"
        sftp = client.open_sftp()
        try:
            upload_text(sftp, "/tmp/xinke-deploy-sudoers", sudoers, 0o440)
        finally:
            sftp.close()
        run(client, "visudo -cf /tmp/xinke-deploy-sudoers >/dev/null && install -o root -g root -m 0440 /tmp/xinke-deploy-sudoers /etc/sudoers.d/xinke-deploy")
        run(client, "rm -f /tmp/xinke-deploy.pub /tmp/xinke-release /tmp/xinke-deploy-db.cnf /tmp/xinke-deploy-sudoers")
        run(client, "sudo -u deploy test -r /home/deploy/.ssh/authorized_keys")
        print("BOOTSTRAP_OK user=deploy command=/usr/local/sbin/xinke-release")
        return 0
    finally:
        client.close()


def upload_text(sftp: paramiko.SFTPClient, path: str, value: str, mode: int) -> None:
    with sftp.file(path, "w") as handle:
        handle.write(value)
    sftp.chmod(path, mode)


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
