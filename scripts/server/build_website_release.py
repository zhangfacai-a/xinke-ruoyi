"""Build a full website release from source; retain the existing signed App feed."""
import argparse
from datetime import datetime
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import tarfile
import zipfile

ROOT=Path(__file__).resolve().parents[2]


def build(website_snapshot,skip_build=False):
    if not skip_build:
        subprocess.run(['mvn.cmd' if __import__('os').name=='nt' else 'mvn','-B','-pl','xinke-admin','-am','clean','verify'],cwd=ROOT,check=True)
        subprocess.run(['pnpm.cmd' if __import__('os').name=='nt' else 'pnpm','--dir','xinke-ui','build:prod'],cwd=ROOT,check=True)
    jar=ROOT/'xinke-admin/target/xinke-admin.jar'
    with zipfile.ZipFile(jar) as archive:
        assert not any('/xinke-generator-' in name for name in archive.namelist()),'Development generator must not be in production'
        assert 'BOOT-INF/classes/com/xinke/web/core/config/LiveAccessRefreshListener.class' in archive.namelist()
    stage=ROOT/'.codex-deploy'/('website-release-'+datetime.now().strftime('%Y%m%d-%H%M%S'))
    stage.mkdir(parents=True)
    shutil.copy2(jar,stage/'xinke-admin.jar')
    shutil.copytree(ROOT/'xinke-ui/dist',stage/'site')
    retained=[]
    with tarfile.open(website_snapshot,'r:gz') as snapshot:
        for member in snapshot.getmembers():
            relative=member.name.removeprefix('./')
            if relative.startswith('anchor-app/') and member.isfile():
                target=(stage/'site'/relative).resolve()
                if not target.is_relative_to((stage/'site/anchor-app').resolve()):raise ValueError('Invalid App feed archive path')
                target.parent.mkdir(parents=True,exist_ok=True)
                with snapshot.extractfile(member) as source,target.open('wb') as output:shutil.copyfileobj(source,output)
                retained.append(relative)
    assert 'anchor-app/latest.json' in retained,'Existing App update feed must be retained'
    for path in ['index.html','freight-app/freight.html','jd-customers-app/jd-customers.html','anchor-console/anchor-releases.html']:
        assert (stage/'site'/path).is_file(),path
    with tarfile.open(stage/'xinke-ui.tar.gz','w:gz') as archive:archive.add(stage/'site',arcname='.')
    manifest={'sourceCommit':subprocess.run(['git','rev-parse','HEAD'],cwd=ROOT,capture_output=True,text=True,check=True).stdout.strip(),
              'sourceDirty':bool(subprocess.run(['git','status','--porcelain'],cwd=ROOT,capture_output=True,text=True,check=True).stdout),
              'buildType':'complete-source-build','retainedAppFeedFiles':len(retained),'artifacts':{}}
    for name in ['xinke-admin.jar','xinke-ui.tar.gz']:
        path=stage/name
        manifest['artifacts'][name]={'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'bytes':path.stat().st_size}
    (stage/'release.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(stage)
    return stage


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--website-snapshot',required=True)
    parser.add_argument('--skip-build',action='store_true',help='Use only immediately after independently verified builds')
    args=parser.parse_args();build(args.website_snapshot,args.skip_build)
