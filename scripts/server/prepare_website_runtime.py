"""Externalize the current deployed runtime secrets without printing or changing them."""
import argparse
from pathlib import Path
import zipfile
import yaml
import paramiko


def prepare(jar,host='47.116.62.234'):
    with zipfile.ZipFile(jar) as archive:
        current=yaml.safe_load(archive.read('BOOT-INF/classes/application.yml').decode('utf-8-sig'))
        druid=yaml.safe_load(archive.read('BOOT-INF/classes/application-druid.yml').decode('utf-8-sig'))
    private={'spring':{'datasource':{'druid':{'master':druid['spring']['datasource']['druid']['master']}},
                       'data':{'redis':current['spring']['data']['redis']}},
             'token':current['token'],'live':{}}
    for key in ['dingtalk','audience-rank','cos']:
        if key in current.get('live',{}): private['live'][key]=current['live'][key]
    # Existing service environment/JVM overrides continue to take precedence.
    client=paramiko.SSHClient();client.load_system_host_keys(str(Path.home()/'.ssh/known_hosts'))
    client.set_missing_host_key_policy(paramiko.RejectPolicy())
    client.connect(host,username='deploy',key_filename=str(Path.home()/'.ssh/id_ed25519_xinke_prod'),look_for_keys=False)
    try:
        with client.open_sftp() as sftp:
            directory='/home/deploy/xinke-config'
            try:sftp.mkdir(directory,0o700)
            except OSError:
                sftp.stat(directory)
            target=directory+'/application-private.yml'
            try:existing=sftp.stat(target)
            except FileNotFoundError:pass
            else:
                if existing.st_size:raise RuntimeError('Private runtime configuration already exists; refusing to replace it')
                sftp.remove(target)  # Empty residue from an interrupted initial preparation.
            temporary=target+'.next'
            with sftp.file(temporary,'wx') as output:
                sftp.chmod(temporary,0o600)
                output.write(yaml.safe_dump(private,allow_unicode=True,sort_keys=False).encode('utf-8'))
            sftp.rename(temporary,target)
            if sftp.stat(target).st_size<100:raise RuntimeError('Private configuration is incomplete')
        print('Current runtime credentials preserved in a private external configuration (mode 0600).')
    finally:client.close()


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--current-jar',required=True)
    args=parser.parse_args();prepare(args.current_jar)
