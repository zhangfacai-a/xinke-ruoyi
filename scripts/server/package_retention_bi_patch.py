#!/usr/bin/env python3
"""Add only the retention BI sync classes to the current production boot jar."""

import argparse
from hashlib import sha256
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile


EXPECTED_BASE_SHA256 = "eb38e1c69225716d9d8415a8babb0431aeeaff6e95a56caa0330e8bb48975acb"
ERP_JAR = "BOOT-INF/lib/xinke-erp-3.9.2.jar"
ADDITIONS = {
    "com/xinke/erp/config/LiveScheduleGate.class": "xinke-erp/target/classes/com/xinke/erp/config/LiveScheduleGate.class",
    "com/xinke/erp/mapper/BiLiveRetentionMapper.class": "xinke-erp/target/classes/com/xinke/erp/mapper/BiLiveRetentionMapper.class",
    "com/xinke/erp/task/BiLiveRetentionSyncTask.class": "xinke-erp/target/classes/com/xinke/erp/task/BiLiveRetentionSyncTask.class",
    "mapper/erp/BiLiveRetentionMapper.xml": "xinke-erp/src/main/resources/mapper/erp/BiLiveRetentionMapper.xml",
}


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    base = args.base.read_bytes()
    if sha256(base).hexdigest() != EXPECTED_BASE_SHA256:
        parser.error("base jar differs from the verified production release")
    if args.output.exists():
        parser.error("output already exists")
    repo = Path(__file__).resolve().parents[2]
    additions = {name: (repo / path).read_bytes() for name, path in ADDITIONS.items()}
    with ZipFile(BytesIO(base)) as outer:
        with ZipFile(BytesIO(outer.read(ERP_JAR))) as inner:
            existing = set(inner.namelist())
            overlap = existing.intersection(additions)
            if overlap:
                parser.error(f"patch would overwrite existing entries: {sorted(overlap)}")
            if not {"com/xinke/erp/task/BiLiveAnchorSyncTask.class", "com/xinke/erp/config/LiveScheduleConfig.class"} <= existing:
                parser.error("production ERP jar lacks expected dependency classes")
            nested_bytes = BytesIO()
            with ZipFile(nested_bytes, "w") as patched:
                for info in inner.infolist():
                    patched.writestr(info, inner.read(info.filename))
                for name, content in additions.items():
                    patched.writestr(name, content)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        with ZipFile(args.output, "w") as patched_outer:
            for info in outer.infolist():
                content = nested_bytes.getvalue() if info.filename == ERP_JAR else outer.read(info.filename)
                patched_outer.writestr(info, content)

    with ZipFile(args.output) as check:
        if check.testzip() is not None:
            raise RuntimeError("invalid patched boot jar")
        with ZipFile(BytesIO(check.read(ERP_JAR))) as inner:
            if inner.testzip() is not None:
                raise RuntimeError("invalid patched ERP jar")
            for name, content in additions.items():
                if inner.read(name) != content:
                    raise RuntimeError(f"patch verification failed: {name}")
    print(f"PATCH_OK sha256={sha256(args.output.read_bytes()).hexdigest()} entries={len(additions)}")


if __name__ == "__main__":
    main()
