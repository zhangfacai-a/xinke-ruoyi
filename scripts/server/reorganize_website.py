"""Idempotent website configuration migration. Defaults to a read-only preview."""
import argparse
from collections import defaultdict
import hashlib
import json
from pathlib import Path
import re

import pymysql
from runtime_settings import database_settings

VERSION = "website-reorganization-v1"


def connect(database=None):
    s=database_settings()
    return pymysql.connect(host=s['host'],port=int(s['port']),user=s['username'],password=s['password'],
                           database=database or s['database'],charset='utf8mb4',autocommit=False)


def assignments(cursor):
    cursor.execute("select subject_type,subject_id,user_id,role_code from live_subject_user_map order by subject_type,subject_id,user_id,role_code")
    return cursor.fetchall()


def ensure_menu(q,name,path,parent=0,component=None,order=1,perms='',query=None):
    q.execute("select menu_id from sys_menu where path=%s and parent_id=%s",(path,parent))
    matches=q.fetchall()
    if len(matches)>1: raise RuntimeError("Duplicate navigation path: "+path)
    if matches: return matches[0][0]
    q.execute("""insert into sys_menu(menu_name,parent_id,order_num,path,component,query,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
                 values(%s,%s,%s,%s,%s,%s,1,0,%s,'0','0',%s,'documentation',%s,now())""",
              (name,parent,order,path,component,query,'C' if component else 'M',perms,VERSION))
    return q.lastrowid


def migrate(connection):
    with connection.cursor() as q:
        # DDL precedes the data transaction: MySQL DDL implicitly commits.
        q.execute("select count(*) from information_schema.columns where table_schema=database() and table_name='sys_user' and column_name='local_disabled'")
        if not q.fetchone()[0]:
            q.execute("alter table sys_user add column local_disabled tinyint not null default 0 comment 'Website-managed disable; DingTalk cannot override'")
            q.execute("update sys_user set local_disabled=1 where status='1'")
            connection.commit()
        q.execute("""create table if not exists sys_release_migration (
             version varchar(100) primary key,applied_at datetime not null default current_timestamp,
             details text null) engine=InnoDB default charset=utf8mb4""")
        connection.begin()
        before=assignments(q)
        q.execute("select version from sys_release_migration where version=%s for update",(VERSION,))
        if q.fetchone():
            connection.rollback()
            return {'version':VERSION,'alreadyApplied':True}
        categories={key:ensure_menu(q,name,key,order=order) for key,name,order in [
            ('ops-customers','客户运营',2),('ops-anchors','主播运营',3),('ops-gifts','礼品管理',4),
            ('ops-freight','运费管理',5),('ops-people','人员与业务配置',6)]}
        targets=[
            ('liveCustomer/center/index','ops-customers','customer-center','直播客户中心'),
            ('live/jd-customers/index','ops-customers','jd-customers','京东客户信息'),
            ('live/retention/index','ops-customers','retention-center','挽单与转推中心'),
            ('live/metrics/index','ops-anchors','liveMetrics','主播数据'),
            ('live/giftWorkbench/index','ops-gifts','giftWorkbench','礼品工作台'),
            ('live/giftMaterial/index','ops-gifts','giftMaterial','礼品与库存'),
            ('live/freight/index','ops-freight','freight-collection','运费征收'),
            ('live/liveSubject/index','ops-people','liveSubject','直播间配置'),
        ]
        for order,(component,category,path,name) in enumerate(targets,1):
            q.execute("select menu_id from sys_menu where menu_type='C' and (component=%s or (%s='live/freight/index' and path like '%%/freight-app/freight.html') or (%s='live/jd-customers/index' and path like '%%/jd-customers-app/jd-customers.html'))",(component,component,component))
            found=q.fetchall()
            if len(found)!=1: raise RuntimeError("Expected one existing business menu: "+component)
            menu=found[0][0]
            q.execute("update sys_menu set parent_id=%s,path=%s,component=%s,menu_name=%s,is_frame=1,order_num=%s where menu_id=%s",(categories[category],path,component,name,order,menu))
        # Reuse the existing personnel page and preserve its permission identifiers.
        q.execute("update sys_menu set parent_id=%s,menu_name='人员管理' where component='system/user/index'",(categories['ops-people'],))
        shop=ensure_menu(q,'店铺与人员组','shops',categories['ops-people'],'live/liveSubject/index',2,'system:user:edit','{"kind":"SHOP"}')
        anchor=ensure_menu(q,'直播间与主播分配','assignments',categories['ops-anchors'],'live/liveSubject/index',2,'live:metrics:manage')
        q.execute("select menu_id from sys_menu where path='system' and parent_id=0")
        system=q.fetchone()[0]
        q.execute("update sys_menu set menu_name='系统维护' where menu_id=%s",(system,))
        q.execute("update sys_menu set parent_id=%s,path='anchor-releases',component='live/anchorReleases/index',is_frame=1,menu_name='App 版本发布' where component='live/anchorReleases/index' or path like '%%/anchor-console/anchor-releases.html'",(system,))
        # Keep old data, API grants and compatibility routes. Only disable abandoned menus.
        q.execute("update sys_menu set visible='1',status='1' where path in ('audience-rank','customer-workbench') and menu_type='C'")
        q.execute("update sys_menu set visible='1',status='1' where component in ('tool/build/index','tool/gen/index','tool/swagger/index')")
        q.execute("update sys_menu set visible='1' where component='system/post/index'")
        q.execute("update sys_menu set visible='1' where menu_type='M' and path in ('live-ops','tool')")
        # Give existing configuration administrators the new shop entry, without expanding users' actions.
        q.execute("""insert ignore into sys_role_menu(role_id,menu_id)
            select distinct rm.role_id,%s from sys_role_menu rm join sys_menu m on m.menu_id=rm.menu_id
            where m.perms='system:user:edit'""",(shop,))
        q.execute("""insert ignore into sys_role_menu(role_id,menu_id)
            select distinct rm.role_id,%s from sys_role_menu rm join sys_menu m on m.menu_id=rm.menu_id
            where m.perms='live:metrics:manage'""",(anchor,))
        # Ensure moved leaves remain reachable for every role that already owns them.
        q.execute("""insert ignore into sys_role_menu(role_id,menu_id)
            select distinct rm.role_id,m.parent_id from sys_role_menu rm join sys_menu m on m.menu_id=rm.menu_id
            join sys_menu p on p.menu_id=m.parent_id where p.parent_id=0 and m.parent_id<>0""")
        q.execute("select role_id from sys_role where role_key='auto_live_anchor_app' and status='0' and del_flag='0'")
        row=q.fetchone()
        if row:
            q.execute("insert into live_auto_access_rule(rule_code,subject_type,role_code,grant_role_id) values('anchor_app','ROOM','anchor',%s) on duplicate key update grant_role_id=values(grant_role_id)",(row[0],))
        q.execute("""insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
            select '登录基础访问','auto_live_authenticated',99,'5','1','1','0','0',%s,now(),'所有正常登录账号的公共业务页面'
            where not exists(select 1 from sys_role where role_key='auto_live_authenticated')""",(VERSION,))
        q.execute("""insert ignore into sys_role_menu(role_id,menu_id)
            select r.role_id,m.menu_id from sys_role r join sys_menu m
            where r.role_key='auto_live_authenticated' and (m.component='live/jd-customers/index' or m.menu_id=%s)""",(categories['ops-customers'],))
        # Adopt identical existing lists without rewriting any effective shop assignment.
        q.execute("select m.subject_id,m.user_id,m.role_code from live_subject_user_map m join erp_shop s on s.shop_id=m.subject_id and s.status='0' where m.subject_type='SHOP' order by m.subject_id,m.user_id,m.role_code")
        by_shop=defaultdict(list)
        for shop_id,user,role in q.fetchall(): by_shop[shop_id].append((user,role))
        by_members=defaultdict(list)
        for shop_id,members in by_shop.items(): by_members[tuple(members)].append(shop_id)
        adopted=[]
        for members,shops in sorted(by_members.items()):
            if len(shops)<2: continue
            key=hashlib.sha256(json.dumps(members).encode()).hexdigest()[:8]
            name='共享客服组-'+key
            q.execute("insert into live_shop_staff_group(group_name,created_by) values(%s,%s) on duplicate key update group_name=values(group_name)",(name,VERSION))
            q.execute("select group_id from live_shop_staff_group where group_name=%s",(name,));group=q.fetchone()[0]
            for user,role in members:
                q.execute("insert ignore into live_shop_staff_group_member(group_id,user_id,role_code) values(%s,%s,%s)",(group,user,role))
            for shop_id in shops:
                q.execute("insert ignore into live_shop_staff_group_shop(shop_id,group_id) values(%s,%s)",(shop_id,group))
            adopted.append({'groupId':group,'shops':len(shops),'memberships':len(members)})
        after=assignments(q)
        if before!=after: raise RuntimeError("Effective assignments changed during group adoption")
        # All active login accounts retain access to JD; the service enforces login independently of menu roles.
        details={'version':VERSION,'adoptedGroups':adopted,'assignmentsPreserved':len(after)}
        q.execute("insert into sys_release_migration(version,details) values(%s,%s)",(VERSION,json.dumps(details)))
        connection.commit()
        return details


if __name__=='__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('--database')
    parser.add_argument('--apply',action='store_true')
    args=parser.parse_args()
    if args.database and not re.fullmatch(r'xinke_reorg_test_\d+',args.database):
        parser.error('Only isolated reorganization test database names may override production')
    with connect(args.database) as c:
        if args.apply: print(json.dumps(migrate(c)))
        else:
            with c.cursor() as q:
                q.execute('SET SESSION TRANSACTION READ ONLY')
                q.execute('select count(*) from live_subject_user_map');print(json.dumps({'mode':'read-only','assignments':q.fetchone()[0]}))
