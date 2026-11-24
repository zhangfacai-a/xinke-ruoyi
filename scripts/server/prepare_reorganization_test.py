"""Restore configuration from a backup and prepare an isolated website test instance."""
import argparse
import bcrypt
import gzip
import json
from pathlib import Path
import re
import secrets
import yaml

from reorganize_website import connect, migrate
from runtime_settings import database_settings, ROOT


def prepare(name, backup):
    if not re.fullmatch(r'xinke_reorg_test_\d+',name): raise ValueError('Invalid isolated test database name')
    source=database_settings()['database']
    if not re.fullmatch(r'[A-Za-z0-9_]+',source): raise ValueError('Invalid source database identifier')
    with connect() as c:
        with c.cursor() as q:
            q.execute('CREATE DATABASE `'+name+'` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci')
            q.execute("select table_name from information_schema.tables where table_schema=database() and table_type='BASE TABLE'")
            tables=[r[0] for r in q.fetchall()]
            for table in tables: q.execute(f'CREATE TABLE `{name}`.`{table}` LIKE `{source}`.`{table}`')
    with connect(name) as c:
        with c.cursor() as q:
            q.execute('SELECT DATABASE()'); assert q.fetchone()[0]==name
            q.execute('DROP TABLE sys_menu')
            pending=[];restored=False
            with gzip.open(backup,'rt',encoding='utf-8') as dump:
                for line in dump:
                    if line.startswith('CREATE TABLE `sys_menu`'): pending=[line]
                    elif pending: pending.append(line)
                    elif line.startswith('INSERT INTO `sys_menu`'): q.execute(line);restored=True
                    elif restored and line.startswith('-- Table structure'): break
                    if pending and line.rstrip().endswith(';'): q.execute(''.join(pending));pending=[]
            if not restored: raise RuntimeError('The backup does not contain sys_menu')
            q.execute('select count(*) from sys_menu'); restored_count=q.fetchone()[0]
            copy=[t for t in tables if t!='sys_menu' and (
                (t.startswith('sys_') and not any(v in t for v in ['backup','log','logininfor'])) or t in [
                    'live_subject_user_map','live_auto_access_rule','live_shop_staff_group','live_shop_staff_group_member',
                    'live_shop_staff_group_shop','live_room','erp_shop','live_metric_session','live_metric_snapshot',
                    'live_metric_anchor_segment','live_metric_segment_correction','live_metric_room_alias',
                    'live_retention_order','erp_gift','erp_gift_cost','live_gift_inventory'])]
            for table in copy:
                q.execute("select column_name from information_schema.columns where table_schema=database() and table_name=%s and extra not like '%%GENERATED%%' order by ordinal_position",(table,))
                columns=','.join('`'+r[0]+'`' for r in q.fetchall())
                q.execute(f'INSERT INTO `{table}` ({columns}) SELECT {columns} FROM `{source}`.`{table}`')
        c.commit()
        result=migrate(c)
        assert migrate(c)['alreadyApplied']
        password=secrets.token_hex(8)
        with c.cursor() as q:
            q.execute("update sys_user set password=%s,status='0',local_disabled=0,pwd_update_date=now() where user_id=1",(bcrypt.hashpw(password.encode(),bcrypt.gensalt()).decode(),))
            q.execute("select user_name from sys_user where user_id=1");username=q.fetchone()[0]
            q.execute("update sys_config set config_value='false' where config_key in ('sys.account.captchaEnabled','sys.account.initPasswordModify')")
        c.commit()
    local=yaml.safe_load((ROOT/'config/local/application-local.yml').read_text(encoding='utf-8'))
    master=local['spring']['datasource']['druid']['master']
    master['url']=re.sub(r'/'+re.escape(source)+r'(?=\?)','/'+name,master['url'])
    if '/'+name+'?' not in master['url']: raise RuntimeError('Failed to isolate datasource URL')
    local['spring']['data']['redis']['database']=15
    local['server']={'port':18081}
    local['token']={'secret':secrets.token_hex(40),'expireTime':60}
    local['xinke']={'profile':str(ROOT/'.codex-tmp/reorganization-uploads').replace('\\','/')}
    local['live']={'scheduling':{'enabled':False},'cos':{'enabled':False},'dingtalk':{'app-key':'','app-secret':''},
                   'anchor':{'release-store':str(ROOT/'.codex-tmp/test-app-releases'),'release-feed':str(ROOT/'.codex-tmp/test-app-feed')}}
    (ROOT/'config/local/reorganization-test.yml').write_text(yaml.safe_dump(local,allow_unicode=True),encoding='utf-8')
    (ROOT/'.codex-tmp/reorganization-test-login.json').write_text(json.dumps({'username':username,'password':password}),encoding='utf-8')
    report={'isolatedDatabase':name,'backupMenuRowsRestored':restored_count,'copiedTables':len(copy),
            'migration':result,'idempotencePassed':True,'schedulingEnabled':False,'redisDatabase':15}
    (ROOT/'.codex-tmp/reorganization-rehearsal.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report))


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--database',required=True);parser.add_argument('--backup',required=True)
    args=parser.parse_args();prepare(args.database,args.backup)
