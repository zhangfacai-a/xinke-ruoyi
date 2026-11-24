"""HTTP regression against the isolated fixture; never write to the production schema."""
import json
from pathlib import Path
import secrets
import urllib.request
import urllib.error
import bcrypt
from reorganize_website import connect
from runtime_settings import ROOT

BASE='http://127.0.0.1:18081'
checks=[]


def call(path,token=None,method='GET',data=None,expected=200):
    headers={'Content-Type':'application/json'}
    if token: headers['Authorization']='Bearer '+token
    request=urllib.request.Request(BASE+path,data=json.dumps(data).encode() if data is not None else None,headers=headers,method=method)
    try:
        with urllib.request.urlopen(request,timeout=180) as response: value=json.load(response)
    except urllib.error.HTTPError as error: value=json.load(error)
    if expected is not None: assert value.get('code')==expected,(path,value.get('code'),value.get('msg'))
    return value


def passed(name):
    checks.append(name);print('PASS',name,flush=True)


def main():
    report=json.loads((ROOT/'.codex-tmp/reorganization-rehearsal.json').read_text())
    database=report['isolatedDatabase']
    assert database.startswith('xinke_reorg_test_')
    admin=call('/login',method='POST',data=json.loads((ROOT/'.codex-tmp/reorganization-test-login.json').read_text()))['token']
    (ROOT/'.codex-tmp/reorganization-test-admin-token.json').write_text(json.dumps({'token':admin}))
    passed('isolated admin login')
    routes=call('/getRouters',admin)['data']
    paths={r['path'] for r in routes}
    assert {'/ops-customers','/ops-anchors','/ops-gifts','/ops-freight','/ops-people'}<=paths,paths
    passed('unified business navigation')
    with connect(database) as c:
        with c.cursor() as q:
            q.execute('SELECT DATABASE()');assert q.fetchone()[0]==database
            password=secrets.token_hex(8);encoded=bcrypt.hashpw(password.encode(),bcrypt.gensalt()).decode()
            suffix=secrets.token_hex(3);fixtures={}
            for kind,status in [('staff','0'),('plain','0'),('disabled','1')]:
                account='reorg_'+kind+'_'+suffix
                q.execute("insert into sys_user(user_name,nick_name,dept_id,password,status,del_flag,local_disabled,create_by,create_time) values(%s,%s,100,%s,%s,'0',%s,'isolated-reorganization-test',now())",(account,'整理测试'+kind,encoded,status,1 if status=='1' else 0))
                fixtures[kind]={'id':q.lastrowid,'account':account}
        c.commit()
    call('/live/personnel/reconcile',admin,method='POST',data={})
    passed('automatic access reconciliation')
    plain=call('/login',method='POST',data={'username':fixtures['plain']['account'],'password':password})['token']
    staff=call('/login',method='POST',data={'username':fixtures['staff']['account'],'password':password})['token']
    call('/live/jd-customers/records',plain)
    call('/live/personnel/'+str(fixtures['staff']['id']),plain,expected=403)
    call('/live/personnel/reconcile',plain,method='POST',data={},expected=403)
    passed('ordinary account retains JD access and cannot manage personnel')
    with connect(database) as c:
        with c.cursor() as q:
            q.execute("select room_id from live_room where status='0' limit 1");room=q.fetchone()[0]
    call('/live/gift/mapping',plain,method='POST',data={'subjectType':'ROOM','subjectId':room,'anchorIds':[]},expected=403)
    rejected=call('/live/gift/mapping',admin,method='POST',data={'subjectType':'ROOM','subjectId':room,'anchorIds':[fixtures['disabled']['id']]},expected=None)
    assert rejected['code']!=200
    passed('room assignment is protected and rejects disabled accounts')
    shop_ids=[]
    for index in range(2):
        data=call('/live/gift/shop',admin,method='POST',data={'shopCode':f'REORG-{suffix}-{index}','shopName':f'隔离测试店铺{index}','platformName':'TEST','status':'0','staffGroupId':None})['data']
        shop_ids.append(data['shopId'])
    group={'groupName':'隔离测试组-'+suffix,'shopIds':shop_ids,'operatorIds':[],'customerAdminIds':[],'customerServiceIds':[fixtures['staff']['id']]}
    group['groupId']=call('/live/gift/staff-groups',admin,method='POST',data=group)['data']
    detail=call('/live/personnel/'+str(fixtures['staff']['id']),admin)['data']
    assert len(detail['assignments'])==2
    assert {'auto_live_retention','auto_live_freight'}<={r['roleKey'] for r in detail['roles']}
    assert 'live:retention:access' in call('/getInfo',staff)['permissions']
    passed('one shared group updates two shops and refreshes online permissions')
    group['shopIds']=[shop_ids[0]]
    call('/live/gift/staff-groups',admin,method='POST',data=group)
    detail=call('/live/personnel/'+str(fixtures['staff']['id']),admin)['data']
    assert len(detail['assignments'])==2
    assert any(row['subjectId']==shop_ids[1] and not row.get('groupName') for row in detail['assignments'])
    passed('detaching a shop preserves its effective staff list')
    call('/live/gift/mapping',admin,method='POST',data={'subjectType':'SHOP','subjectId':shop_ids[1],'operatorIds':[],'customerAdminIds':[],'customerServiceIds':[]})
    group['customerServiceIds']=[]
    call('/live/gift/staff-groups',admin,method='POST',data=group)
    detail=call('/live/personnel/'+str(fixtures['staff']['id']),admin)['data']
    assert not detail['assignments']
    assert 'live:retention:access' not in call('/getInfo',staff)['permissions']
    call('/live/jd-customers/records',staff)
    passed('removing the last staff assignment revokes only business auto access')
    call('/system/user/changeStatus',admin,method='PUT',data={'userId':fixtures['staff']['id'],'status':'1'})
    call('/getInfo',staff,expected=401)
    detail=call('/live/personnel/'+str(fixtures['staff']['id']),admin)['data']
    assert detail['localDisabled']==1
    passed('disabling an account terminates its session and records local management state')
    # Run the same UPDATE used by the DingTalk mapper on an isolated fixture.
    with connect(database) as c:
        with c.cursor() as q:
            q.execute("update sys_user set status=case when local_disabled=1 then '1' else '0' end where user_id=%s and del_flag='0'",(fixtures['staff']['id'],))
            q.execute('select status from sys_user where user_id=%s',(fixtures['staff']['id'],));assert q.fetchone()[0]=='1'
        c.commit()
    passed('DingTalk source enable cannot override website disable')
    call('/live/personnel?ids='+','.join(str(v['id']) for v in fixtures.values()),admin)
    call('/live/gift/catalog',admin)
    call('/live/metrics/rooms',admin)
    call('/live/retention/options',admin)
    call('/live/freight/dashboard',admin)
    call('/system/anchor-app-releases',admin)
    passed('personnel, gifts, anchor rooms, retention, freight and App release endpoints remain available')
    anonymous=call('/live/jd-customers/records',expected=None)
    assert anonymous.get('code')==401
    passed('anonymous customer access remains blocked')
    (ROOT/'.codex-tmp/reorganization-http-tests.json').write_text(json.dumps({'database':database,'checks':checks,'passed':len(checks)},ensure_ascii=False,indent=2),encoding='utf-8')
    print('ISOLATED_HTTP_TESTS_PASSED',len(checks))


if __name__=='__main__':main()
