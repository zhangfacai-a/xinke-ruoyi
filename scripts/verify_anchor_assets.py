"""Validate actual published/installed files and references, not just installer exit codes."""
from pathlib import Path
import argparse
import hashlib
import json
import re

def validate(directory, source=None):
    directory = Path(directory).resolve()
    manifest = json.loads((directory/'desktop-assets.json').read_text(encoding='utf-8-sig'))
    required = {'ui/anchor-desktop.html', 'help/使用说明.html', 'app.ico'}
    assert required <= manifest['files'].keys(), 'Incomplete resource manifest'
    if source:
        assert manifest == json.loads((Path(source)/'desktop-assets.json').read_text(encoding='utf-8-sig')), 'Published manifest differs from source'
    for name, digest in manifest['files'].items():
        file = (directory/name).resolve()
        assert directory in file.parents, 'Invalid resource path'
        assert file.is_file(), f'Missing installed resource: {name}'
        assert hashlib.sha256(file.read_bytes()).hexdigest() == digest, f'Damaged resource: {name}'
    entry = directory/'ui/anchor-desktop.html'
    for reference in re.findall(r'(?:src|href)=["\']([^"\']+)["\']', entry.read_text(encoding='utf-8')):
        assert not re.match(r'https?://', reference), 'Workbench must use local resources'
        referenced = (entry.parent/reference.split('?',1)[0]).resolve()
        assert referenced.is_file(), f'HTML resource is missing: {reference}'
    assert list((directory/'ui/assets').glob('*.js')), 'JavaScript assets missing'
    assert list((directory/'ui/assets').glob('*.css')), 'Stylesheet assets missing'
    return {'version': manifest['version'], 'verified_assets': len(manifest['files'])}

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('directory')
    parser.add_argument('--source')
    args = parser.parse_args()
    print(json.dumps(validate(args.directory,args.source)))
