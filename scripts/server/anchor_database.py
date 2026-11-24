"""Back up related production tables, then run the additive desktop migration."""
import argparse,gzip,json,re
from pathlib import Path
from datetime import datetime
import pymysql
from runtime_settings import database_settings
ROOT=Path(__file__).resolve().parents[2]
def connect():
    settings=database_settings()
    return pymysql.connect(host=settings['host'],port=int(settings['port']),user=settings['username'],password=settings['password'],database=settings['database'],charset='utf8mb4',autocommit=False,connect_timeout=10)
def statements(text):
    delimiter=';';pending=[]
    for line in text.splitlines():
        if line.strip().startswith('--'):continue
        if line.strip().upper().startswith('DELIMITER '):delimiter=line.strip().split()[1];continue
        pending.append(line)
        joined='\n'.join(pending).strip()
        if joined.endswith(delimiter):
            yield joined[:-len(delimiter)].strip();pending=[]
    if '\n'.join(pending).strip():raise ValueError('Unterminated SQL')
def backup(conn,path):
    tables=['live_metric_session','live_metric_snapshot','live_metric_anchor_segment','live_metric_segment_correction','live_metric_room_alias','live_subject_user_map','sys_menu','sys_role','sys_role_menu','sys_user_role']
    with conn.cursor() as c,gzip.open(path,'wt',encoding='utf-8') as out:
        c.execute('START TRANSACTION WITH CONSISTENT SNAPSHOT')
        out.write('SET FOREIGN_KEY_CHECKS=0;\n')
        for table in tables:
            c.execute(f'SHOW CREATE TABLE `{table}`');out.write(c.fetchone()[1]+';\n')
            c.execute(f'SELECT * FROM `{table}`');columns=[d[0] for d in c.description]
            while rows:=c.fetchmany(200):
                for row in rows:out.write('INSERT INTO `'+table+'` ('+','.join('`'+x+'`' for x in columns)+') VALUES ('+','.join(conn.escape(v) for v in row)+');\n')
        out.write('SET FOREIGN_KEY_CHECKS=1;\n')
    conn.rollback()
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--migrate',action='store_true');args=parser.parse_args()
    with connect() as conn:
        if args.migrate:
            path=ROOT/'.codex-deploy'/('anchor-db-before-'+datetime.now().strftime('%Y%m%d-%H%M%S')+'.sql.gz');backup(conn,path);print('Backup saved:',path.name)
            with conn.cursor() as c:
                for sql in statements((ROOT/'config/database/upgrade-anchor-desktop-v1.sql').read_text(encoding='utf-8')):
                    c.execute(sql)
                    while c.nextset():pass
            conn.commit();print('Additive desktop migration committed.')
        with conn.cursor() as c:
            c.execute("select column_name from information_schema.columns where table_schema=database() and table_name='live_metric_anchor_segment' and column_name in ('operator_user_id','client_version')");print('Audit columns:',c.fetchall())
            c.execute("select menu_id,perms from sys_menu where perms in ('system:appRelease:manage','live:metrics:report')");print('App permissions:',c.fetchall())
