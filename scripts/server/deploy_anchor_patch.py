"""Deploy the tested backend patch and console, retaining the currently deployed site."""
from pathlib import Path
import argparse,tarfile,subprocess
ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser();parser.add_argument('--stamp',required=True);parser.add_argument('--full',action='store_true');args=parser.parse_args()
if not __import__('re').fullmatch(r'\d{8}-\d{6}',args.stamp):raise ValueError('Invalid stamp')
remote='/home/deploy/releases/release-'+args.stamp
def run(*args):subprocess.run(args,check=True,cwd=ROOT)
archive=ROOT/'.codex-deploy/anchor-console.tar.gz'
with tarfile.open(archive,'w:gz') as tar:tar.add(ROOT/'.codex-deploy/anchor-console',arcname='anchor-console')
run('ssh','xinke-prod','mkdir -p '+remote)
for file,name in [(ROOT/'.codex-deploy/anchor-release-stage/xinke-admin.jar','xinke-admin.jar'),(ROOT/'config/database/add-live-metrics-void-report.sql','add-live-metrics-void-report.sql'),(archive,'console.tar.gz')]:run('scp',str(file),'xinke-prod:'+remote+'/'+name)
if args.full:run('scp',str(ROOT/'.codex-deploy/anchor-release-stage/xinke-ui.tar.gz'),'xinke-prod:'+remote+'/xinke-ui.tar.gz')
else:run('ssh','xinke-prod',f'mkdir {remote}/ui; cp -r /ruoyi/dist/. {remote}/ui/; tar -xzf {remote}/console.tar.gz -C {remote}/ui; tar -czf {remote}/xinke-ui.tar.gz -C {remote}/ui .')
run('ssh','xinke-prod',f'sudo -n /usr/local/sbin/xinke-release deploy {remote} {args.stamp}')
