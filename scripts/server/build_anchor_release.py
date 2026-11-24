"""Patch the exact current production archive, preserving unrelated deployed code/assets."""
from pathlib import Path
from io import BytesIO
from zipfile import ZipFile
import hashlib,tarfile,shutil
ROOT=Path(__file__).resolve().parents[2];STAGE=ROOT/'.codex-deploy/anchor-release-stage'
BASE=ROOT/'.codex-deploy/anchor-production-base-20261124.jar'
EXPECTED='b6c4c325d9e7635b606da8c99be7bdcf89f4efd92f8311189dc67f152111949c'
if hashlib.sha256(BASE.read_bytes()).hexdigest()!=EXPECTED:raise RuntimeError('Production baseline differs')
STAGE.mkdir(parents=True,exist_ok=True)
files={}
for folder,patterns in {
    'com/xinke/erp/controller':['LiveMetricsController*.class','LiveDesktopController*.class','AnchorAppReleaseController*.class'],
    'com/xinke/erp/service':['LiveHandoverService*.class','LiveDesktopService*.class','AnchorAppReleaseService*.class'],
    'com/xinke/erp/mapper':['LiveMetricsMapper.class'],
    'mapper/erp':['LiveMetricsMapper.xml'],
    '':['anchor-update-public-key.txt']
}.items():
    for pattern in patterns:
        matches=list((ROOT/'xinke-erp/target/classes'/folder).glob(pattern))
        if not matches:raise RuntimeError('Compiled file missing: '+pattern)
        for file in matches:files[file.relative_to(ROOT/'xinke-erp/target/classes').as_posix()]=file.read_bytes()
entry='BOOT-INF/lib/xinke-erp-3.9.2.jar'
with ZipFile(BASE) as outer:
    data=BytesIO()
    with ZipFile(BytesIO(outer.read(entry))) as inner,ZipFile(data,'w') as updated:
        for info in inner.infolist():
            if info.filename not in files:updated.writestr(info,inner.read(info.filename))
        for name,content in files.items():updated.writestr(name,content)
    config=ROOT/'xinke-admin/target/classes/com/xinke/web/core/config/AnchorDesktopConfig.class'
    if not config.exists():raise RuntimeError('TLS configuration not compiled')
    with ZipFile(STAGE/'xinke-admin.jar','w') as output:
        for info in outer.infolist():output.writestr(info,data.getvalue() if info.filename==entry else outer.read(info.filename))
        output.writestr('BOOT-INF/classes/com/xinke/web/core/config/AnchorDesktopConfig.class',config.read_bytes())
with ZipFile(STAGE/'xinke-admin.jar') as check:
    assert check.testzip() is None
    with ZipFile(BytesIO(check.read(entry))) as checkInner:
        assert checkInner.testzip() is None
        for name,content in files.items():assert checkInner.read(name)==content
ui=STAGE/'ui-root';ui.mkdir(exist_ok=True)
with tarfile.open(ROOT/'.codex-deploy/anchor-ui-base-20261124.tar.gz') as archive:archive.extractall(ui,filter='data')
shutil.copytree(ROOT/'.codex-deploy/anchor-console',ui/'anchor-console',dirs_exist_ok=True)
# Existing main website is kept byte-for-byte except the report source label.
for file in (ui/'static/js').glob('*.js'):
    text=file.read_text(encoding='utf-8')
    if 'PLUGIN:"插件上报"' in text:
        file.write_text(text.replace('PLUGIN:"插件上报"','APP:"App上报",PLUGIN:"插件上报"'),encoding='utf-8')
        for gz in [file.with_suffix(file.suffix+'.gz'),file.with_suffix(file.suffix+'.br')]:
            if gz.exists():gz.unlink()
with tarfile.open(STAGE/'xinke-ui.tar.gz','w:gz') as archive:archive.add(ui,arcname='.')
print('Release built: selective backend patch, website assets preserved, update console included.')
