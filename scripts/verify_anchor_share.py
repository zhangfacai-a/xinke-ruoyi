"""Validate the shared installer archive against local files and its signed release."""
from pathlib import Path
import hashlib
import zipfile
import json
import base64
from cryptography.hazmat.primitives.serialization import load_der_public_key
from cryptography.hazmat.primitives.asymmetric.padding import PKCS1v15
from cryptography.hazmat.primitives.hashes import SHA256
from verify_anchor_assets import validate

root = Path(__file__).resolve().parents[1]
folder = Path.home() / 'Desktop/鑫客主播工作台-0.3.4-分享包'
archive = Path(str(folder) + '.zip')
expected = {'鑫客主播工作台安装包-0.3.4.exe', 'MicrosoftEdgeWebView2RuntimeInstallerX64.exe', '使用说明.html', '先看这里-快速开始.txt', '本版更新说明.txt', 'SHA256SUMS.txt'}
assert {p.name for p in folder.iterdir()} == expected
for line in (folder / 'SHA256SUMS.txt').read_text(encoding='utf-8-sig').splitlines():
    digest, name = line.split('  ', 1)
    assert hashlib.sha256((folder / name).read_bytes()).hexdigest() == digest
with zipfile.ZipFile(archive) as zipped:
    assert zipped.testzip() is None
    contents = [i for i in zipped.infolist() if not i.is_dir()]
    assert {Path(i.filename).name for i in contents} == expected
    for item in contents:
        assert zipped.read(item) == (folder / Path(item.filename).name).read_bytes()
envelope = json.loads((root / 'clients/build/anchor-app-release/latest.json').read_text(encoding='utf-8-sig'))
payload = base64.b64decode(envelope['payload'])
key = load_der_public_key(base64.b64decode((root / 'clients/anchor-metrics-probe/update-public-key.txt').read_text().strip()))
key.verify(base64.b64decode(envelope['signature']), payload, PKCS1v15(), SHA256())
release = json.loads(payload)
assert release['version'] == '0.3.4'
publish_directory = (root/'clients/build/anchor-app-release/publish-directory.txt').read_text().strip()
asset_result = validate(publish_directory, root/'clients/anchor-metrics-probe')
assert hashlib.sha256((folder / '鑫客主播工作台安装包-0.3.4.exe').read_bytes()).hexdigest() == release['sha256']
help_text = (folder / '使用说明.html').read_text(encoding='utf-8-sig')
assert 'WebView2 Runtime' in help_text and '0.3.4' in help_text and '<script' not in help_text
print(json.dumps({'files': len(contents), 'zip_integrity': 'passed', 'file_hashes': 'passed', 'update_signature': 'verified', 'version': release['version'], 'verified_app_assets':asset_result['verified_assets'], 'archive_MiB': round(archive.stat().st_size / 1024**2, 2)}))
