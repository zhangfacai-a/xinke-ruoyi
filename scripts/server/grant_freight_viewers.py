"""Grant the approved four active accounts global freight read access only."""
import argparse
import json
from anchor_database import connect

TARGETS={107:'李龙',153:'王启蒙',154:'刘玉平',151:'陈伟佳'}
ROLE='freight_all_shops_viewer'


def grant(connection):
    with connection.cursor() as q:
        for user,name in TARGETS.items():
            q.execute("select nick_name,status,del_flag from sys_user where user_id=%s for update",(user,))
            if q.fetchone()!=(name,'0','0'):raise RuntimeError('Expected active account mismatch: '+name)
        q.execute("select menu_id,parent_id from sys_menu where component='live/freight/index' and status='0'")
        pages=q.fetchall()
        if len(pages)!=1:raise RuntimeError('Expected one freight menu')
        page,parent=pages[0]
        q.execute("select role_id,status,del_flag from sys_role where role_key=%s",(ROLE,))
        roles=q.fetchall()
        if len(roles)>1:raise RuntimeError('Duplicate viewer role')
        if roles:
            role,status,deleted=roles[0]
            if (status,deleted)!=('0','0'):raise RuntimeError('Viewer role is disabled')
        else:
            q.execute("""insert into sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
                values('运费征收全店铺只读',%s,97,'5','1','1','0','0','freight-view-grant',now(),'仅查看所有店铺运费明细和看板，不授予录入、编辑、导入或导出')""",(ROLE,))
            role=q.lastrowid
        q.execute("select role_id from sys_role_menu where menu_id=%s order by role_id",(page,))
        original={r[0] for r in q.fetchall()}
        # Keep existing access on the page; the viewer permission lives in a separate button node.
        q.execute("select menu_id from sys_menu where perms='live:freight:viewAll'")
        nodes=q.fetchall()
        if len(nodes)>1:raise RuntimeError('Duplicate viewer permission node')
        if nodes:permission=nodes[0][0]
        else:
            q.execute("""insert into sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,create_by,create_time,remark)
                values('查看全部店铺运费',%s,1,'','F','0','0','live:freight:viewAll','freight-view-grant',now(),'明细和看板只读')""",(page,))
            permission=q.lastrowid
        # A menu node itself grants its perms, so viewers must not inherit legacy write access.
        # Move that existing permission to an action node, preserving it for every original page role.
        q.execute("select menu_id from sys_menu where perms='live:freight:access' and menu_type='F'")
        nodes=q.fetchall()
        if len(nodes)>1:raise RuntimeError('Duplicate freight write permission node')
        if nodes:write=nodes[0][0]
        else:
            q.execute("""insert into sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,create_by,create_time)
                values('运费业务操作',%s,2,'','F','0','0','live:freight:access','freight-view-grant',now())""",(page,))
            write=q.lastrowid
        for existing in original-{role}:q.execute('insert ignore into sys_role_menu(role_id,menu_id) values(%s,%s)',(existing,write))
        q.execute("update sys_menu set perms='' where menu_id=%s and perms='live:freight:access'",(page,))
        q.execute('select menu_id from sys_role_menu where role_id=%s',(role,))
        if any(r[0] not in {page,parent,permission} for r in q.fetchall()):raise RuntimeError('Viewer role contains unexpected privileges')
        for menu in [page,parent,permission]:q.execute('insert ignore into sys_role_menu(role_id,menu_id) values(%s,%s)',(role,menu))
        for user in TARGETS:q.execute('insert ignore into sys_user_role(user_id,role_id) values(%s,%s)',(user,role))
        q.execute("select distinct m.perms from sys_role_menu rm join sys_menu m on m.menu_id=rm.menu_id where rm.role_id=%s and m.perms<>''",(role,))
        if {r[0] for r in q.fetchall()}!={'live:freight:viewAll'}:raise RuntimeError('Read-only permission verification failed')
    connection.commit()
    return {'roleId':role,'accounts':list(TARGETS.values()),'scope':'all shops','permission':'live:freight:viewAll'}


if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--apply',action='store_true');args=parser.parse_args()
    if not args.apply:print('Preview only: '+', '.join(TARGETS.values())+' -> all-shop freight read access')
    else:
        with connect() as c:print(json.dumps(grant(c),ensure_ascii=False))
