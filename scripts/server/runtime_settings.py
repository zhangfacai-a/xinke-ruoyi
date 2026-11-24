"""Resolve deployment settings from environment and ignored local Spring config."""
import os
from pathlib import Path
import re
from urllib.parse import urlsplit

import yaml

ROOT = Path(__file__).resolve().parents[2]


def _merge(target, source):
    for key, value in (source or {}).items():
        if isinstance(value, dict):
            _merge(target.setdefault(key, {}), value)
        else:
            target[key] = value


def _resolve(value):
    def replace(match):
        name, separator, fallback = match.group(1).partition(":")
        result = os.environ.get(name, fallback if separator else None)
        if result is None:
            raise ValueError(f"Required environment variable is missing: {name}")
        return result
    return re.sub(r"\$\{([^}]+)}", replace, str(value))


def database_settings():
    config = {}
    for path in [ROOT / "xinke-admin/src/main/resources/application-druid.yml",
                 ROOT / "config/local/application-local.yml"]:
        if path.is_file():
            _merge(config, yaml.safe_load(path.read_text(encoding="utf-8-sig")))
    master = config["spring"]["datasource"]["druid"]["master"]
    url = urlsplit(_resolve(master["url"]).removeprefix("jdbc:"))
    settings = {
        "host": os.environ.get("MYSQL_HOST", url.hostname or "127.0.0.1"),
        "port": os.environ.get("MYSQL_PORT", str(url.port or 3306)),
        "database": os.environ.get("MYSQL_DATABASE", url.path.lstrip("/")),
        "username": os.environ.get("MYSQL_USERNAME", _resolve(master["username"])),
        "password": os.environ.get("MYSQL_PASSWORD", _resolve(master.get("password", ""))),
    }
    if not settings["database"]:
        raise ValueError("MYSQL_DATABASE or a database URL is required")
    return settings
