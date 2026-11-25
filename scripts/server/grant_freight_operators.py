"""Add freight operations for the four explicitly approved accounts."""
import argparse
import json
from pathlib import Path
from anchor_database import connect
from grant_freight_viewers import TARGETS


def grant(c):
    c.begin()
    with c.cursor() as q:
        for uid, name in TARGETS.items():
            q.execute('select nick_name,status,del_flag from sys_user where user_id=%s for update', (uid,))
            if q.fetchone() != (name, '0', '0'):
                raise RuntimeError('Active account mismatch')
        q.execute("select menu_id,parent_id from sys_menu where perms='live:freight:access' and menu_type='F' and status='0'")
        nodes=q.fetchall()
        if len(nodes)!=1: raise RuntimeError('Expected one operation permission')
        action,page=nodes[0]
        q.execute('select parent_id from sys_menu where menu_id=%s',(page,))
        parent=q.fetchone()[0]
        q.execute("select role_id,status,del_flag from sys_role where role_key='freight_approved_operators'")
        roles=q.fetchall()
        if len(roles)>1: raise RuntimeError('Duplicate role')
        if roles:
            role,status,deleted=roles[0]
            if (status,deleted)!=('0','0'): raise RuntimeError('Disabled role')
        else:
            q.execute("""insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
                values('运费征收指定操作员','freight_approved_operators',97,'5','1','1','0','0','freight-operation-grant',now(),'经确认授予四个账号运费业务操作，保留原店铺操作范围校验')""")
            role=q.lastrowid
        q.execute('select menu_id from sys_role_menu where role_id=%s',(role,))
        if any(x[0] not in {parent,page,action} for x in q.fetchall()): raise RuntimeError('Unexpected role menus')
        for menu in (parent,page,action):
            q.execute('insert ignore into sys_role_menu(role_id,menu_id) values(%s,%s)',(role,menu))
        for uid in TARGETS:
            q.execute('insert ignore into sys_user_role(user_id,role_id) values(%s,%s)',(uid,role))
        q.execute('select user_id from sys_user_role where role_id=%s order by user_id',(role,))
        if [x[0] for x in q.fetchall()]!=sorted(TARGETS): raise RuntimeError('Unexpected role membership')
    c.commit()
    return {'roleId':role,'userIds':sorted(TARGETS),'permission':'live:freight:access'}


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--apply',action='store_true');a=p.parse_args()
    if not a.apply: print('Preview: add freight operations to four approved users')
    else:
        with connect() as c:
            with c.cursor() as q:
                q.execute('select user_id,role_id from sys_user_role where user_id in (107,151,153,154)')
                before=q.fetchall()
            backup=Path('.codex-deploy/freight-operators-before-20261125.json')
            if not backup.exists(): backup.write_text(json.dumps(before),encoding='utf-8')
            print(json.dumps(grant(c)))
