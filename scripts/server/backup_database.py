"""Create a consistent compressed MySQL backup without credentials in argv/logs."""
import argparse
from datetime import datetime
import gzip
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tempfile

from runtime_settings import database_settings


def backup(destination, tables=()):
    destination = Path(destination).resolve()
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.exists():
        raise ValueError("Backup destination already exists")
    executable = shutil.which("mysqldump")
    if not executable:
        raise RuntimeError("Install MySQL client tools before backing up")
    settings = database_settings()
    def quoted(value):
        return '"' + str(value).replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n').replace('\r', '\\r') + '"'
    with tempfile.TemporaryDirectory(prefix="xinke-db-backup-") as directory:
        cnf = Path(directory) / "client.cnf"
        cnf.write_text("[client]\n" + "\n".join(
            key + "=" + quoted(settings[source]) for key, source in
            [("host", "host"), ("port", "port"), ("user", "username"), ("password", "password")]
        ) + "\ndefault-character-set=utf8mb4\n", encoding="utf-8")
        command = [executable, "--defaults-extra-file=" + str(cnf), "--single-transaction",
                   "--quick", "--triggers", "--no-tablespaces",
                   "--set-gtid-purged=OFF", "--column-statistics=0", "--hex-blob",
                   settings["database"], *tables]
        if not tables: command[2:2]=["--routines","--events"]
        error_path = Path(directory) / "stderr.txt"
        with error_path.open("wb") as errors:
            process = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=errors)
            with gzip.open(destination, "wb", compresslevel=3) as output:
                shutil.copyfileobj(process.stdout, output, length=1024 * 1024)
            code = process.wait()
        if code:
            # Never print server responses which may contain connection details.
            raise RuntimeError(f"mysqldump failed ({code}); incomplete backup: {destination.name}")
    digest = hashlib.sha256()
    with destination.open("rb") as source:
        while chunk := source.read(1024 * 1024):
            digest.update(chunk)
    raw_bytes = 0
    with gzip.open(destination, "rb") as source:
        while chunk := source.read(1024 * 1024):
            raw_bytes += len(chunk)
    if not raw_bytes:
        raise RuntimeError("Backup was empty")
    metadata = {"createdAt": datetime.now().astimezone().isoformat(), "file": destination.name,
                "tables": list(tables) or "all", "compressedBytes": destination.stat().st_size,
                "uncompressedBytes": raw_bytes, "sha256": digest.hexdigest(), "gzipVerified": True}
    destination.with_suffix(destination.suffix + ".json").write_text(json.dumps(metadata, indent=2), encoding="utf-8")
    print(json.dumps(metadata))
    return destination


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("destination")
    parser.add_argument("--tables", nargs="*", default=[])
    args = parser.parse_args()
    backup(args.destination, args.tables)
