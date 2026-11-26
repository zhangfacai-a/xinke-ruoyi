"""Add explicit freight policies and organize personnel navigation; rerunnable."""
import argparse
import json
import re
from pathlib import Path
from reorganize_website import connect

VERSION='personnel-permissions-v2'


def migrate(c):
    sql=(Path(__file__).resolve().parents[2]/'config/database/add-personnel-freight-policy.sql').read_text(encoding='utf-8')
    with c.cursor() as q:
        for statement in re.sub(r'--[^\n]*','',sql).split(';'):
            if statement.strip():q.execute(statement)
    c.commit()
    c.begin()
    with c.cursor() as q:
        q.execute('select version from sys_release_migration where version=%s for update',(VERSION,))
        if q.fetchone():c.rollback();return {'alreadyApplied':True}
        q.execute("select menu_id,parent_id from sys_menu where component='live/freight/index' and status='0'")
        pages=q.fetchall()
        if len(pages)!=1:raise RuntimeError('Expected freight page')
        page,_=pages[0]
        q.execute("select menu_id from sys_menu where perms='live:freight:entry'")
        if not q.fetchone():
            q.execute("""insert into sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,create_by,create_time)
                values('运费个人配置入口',%s,3,'','F','0','0','live:freight:entry',%s,now())""",(page,VERSION))
        # Convert existing combined grants to explicit ALL/ALL, preserving all established privileges.
        q.execute("""select ur.user_id from sys_user_role ur join sys_role r on r.role_id=ur.role_id and r.status='0' and r.del_flag='0'
            join sys_role_menu rm on rm.role_id=r.role_id join sys_menu m on m.menu_id=rm.menu_id and m.status='0'
            where m.perms in ('live:freight:access','live:freight:viewAll') group by ur.user_id having count(distinct m.perms)=2""")
        targets=[r[0] for r in q.fetchall()]
        for uid in targets:
            q.execute("""insert ignore into live_personnel_freight_policy(user_id,actions,view_scope,operate_scope,view_shop_ids,operate_shop_ids,updated_by)
                values(%s,'create,dashboard,edit,export,import,view','ALL','ALL','','',1)""",(uid,))
            if q.rowcount:
                snapshot=json.dumps({'actions':['create','dashboard','edit','export','import','view'],'viewScope':'ALL','operateScope':'ALL','viewShopIds':[],'operateShopIds':[],'version':1})
                q.execute("insert into live_personnel_access_audit(user_id,business,before_config,after_config,actor_id) values(%s,'FREIGHT',%s,%s,1)",(uid,json.dumps({'source':'existing roles preserved'}),snapshot))
        q.execute("select menu_id from sys_menu where path='ops-people' and parent_id=0")
        people=q.fetchall()
        if len(people)!=1:raise RuntimeError('Expected personnel category')
        parent=people[0][0]
        q.execute("update sys_menu set menu_name='人员与权限' where menu_id=%s",(parent,))
        q.execute("update sys_menu set menu_name='人员列表',order_num=1 where component='system/user/index'")
        q.execute("update sys_menu set parent_id=%s,menu_name='岗位与通用权限',order_num=2 where component='system/role/index'",(parent,))
        q.execute("update sys_menu set order_num=3 where parent_id=%s and path='shops'",(parent,))
        q.execute("update sys_menu set menu_name='直播间与主播分工',order_num=4 where parent_id=%s and component='live/liveSubject/index' and path<>'shops'",(parent,))
        # Preserve role page visibility when its parent moves.
        q.execute("""insert ignore into sys_role_menu(role_id,menu_id) select rm.role_id,%s from sys_role_menu rm
            join sys_menu m on m.menu_id=rm.menu_id where m.component='system/role/index'""",(parent,))
        q.execute('insert into sys_release_migration(version,details) values(%s,%s)',(VERSION,json.dumps({'seededPolicies':targets})))
    c.commit()
    return {'version':VERSION,'preservedGlobalOperators':targets}


if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--apply',action='store_true');p.add_argument('--database');a=p.parse_args()
    if a.database and not re.fullmatch(r'xinke_reorg_test_\d+',a.database):raise ValueError('Only isolated test database overrides are accepted')
    if not a.apply:print('Preview: add personnel policies, preserve existing privileges, organize navigation')
    else:
        with connect(a.database) as c:print(json.dumps(migrate(c)))
