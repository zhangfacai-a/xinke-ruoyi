"""HTTP regression checks against the isolated localhost instance only."""
import json
import uuid
from pathlib import Path
import urllib.request
import urllib.error
import urllib.parse

BASE='http://127.0.0.1:18083'
ROOT=Path(__file__).resolve().parents[2]


def request(path,method='GET',body=None,token=None,content_type='application/json'):
    headers={'Content-Type':content_type}
    if token:headers['Authorization']='Bearer '+token
    raw=json.dumps(body).encode() if isinstance(body,dict) else body
    req=urllib.request.Request(BASE+path,headers=headers,data=raw,method=method)
    try:
        with urllib.request.urlopen(req,timeout=30) as response:
            data=response.read()
            if 'json' in response.headers.get('Content-Type',''):return json.loads(data)
            return {'code':200,'bytes':len(data)}
    except urllib.error.HTTPError as error:
        try:return json.load(error)
        except Exception:return {'code':error.code}


def main():
    admin=request('/login','POST',json.loads((ROOT/'.codex-tmp/reorganization-test-login.json').read_text()))['token']
    user=request('/login','POST',json.loads((ROOT/'.codex-tmp/personnel-test-user.json').read_text()))['token']
    uid=153
    path=f'/live/personnel/{uid}/freight'
    config=request(path,token=admin)['data']
    assert config['policy']['viewScope']=='ALL' and config['policy']['operateScope']=='ALL'
    shops=[s['shopId'] for s in config['shops'] if s['status']=='0']
    assert len(shops)>=2
    a,b=shops[:2]
    prefix='PERSONNEL-TEST-'+uuid.uuid4().hex[:8]
    def record(shop,suffix):return {'shopId':shop,'orderNo':prefix+suffix,'deliveryStatus':'SIGNED','freightAmount':'1.00','deductionMethod':'ORDER_PAYMENT'}
    first=request('/live/freight','POST',record(a,'-a'),admin);assert first['code']==200,first
    second=request('/live/freight','POST',record(b,'-b'),admin);assert second['code']==200,second
    def save(actions,view='ALL',operate='SELECTED',vs=None,os=None):
        current=request(path,token=admin)['data']['policy']
        body={'actions':actions,'viewScope':view,'operateScope':operate,'viewShopIds':vs or [],'operateShopIds':os or [a],'version':current['version'] if current else 0}
        response=request(path,'PUT',body,admin);assert response['code']==200,response
        return body
    def rows():
        response=request('/live/freight?orderNo='+prefix,token=user)
        assert response['code']==200,response
        return response['data']['rows']
    save(['view','dashboard','create','edit'],os=[a])
    assert len(rows())==2
    cap=request('/live/freight/capabilities',token=user)['data']
    assert cap['create'] and not cap['import'] and not cap['export']
    assert request('/live/freight','POST',record(a,'-own'),user)['code']==200
    assert request('/live/freight','POST',record(b,'-denied'),user)['code']!=200
    value=record(a,'-a');value['version']=0
    assert request('/live/freight/'+str(first['data']['id']),'PUT',value,user)['code']==200
    value['shopId']=b
    assert request('/live/freight/'+str(first['data']['id']),'PUT',value,user)['code']!=200
    assert request('/live/freight/export','GET',token=user)['code']==403
    multipart=b'--boundary\r\nContent-Disposition: form-data; name="file"; filename="test.xlsx"\r\nContent-Type: application/octet-stream\r\n\r\ntest\r\n--boundary--\r\n'
    assert request('/live/freight/import','POST',multipart,user,'multipart/form-data; boundary=boundary')['code']==403
    read_only=save(['view'],'SELECTED','OWN',[b],[])
    assert {r['shopId'] for r in rows()}=={b}
    assert request('/live/freight/dashboard',token=user)['code']==403
    assert request('/live/freight','POST',record(b,'-read-only'),user)['code']==403
    assert request(path,'PUT',read_only,user)['code']==403
    # A second save with the old version is rejected instead of overwriting the newer configuration.
    assert request(path,'PUT',read_only,admin)['code']!=200
    save(['view','export'],'OWN','OWN')
    own=rows();assert len(own)==1 and own[0]['createdByUserId']==uid
    assert request('/live/freight/export?orderNo='+prefix,token=user)['code']==200
    save(['view'],'ASSIGNED','OWN')
    # Empty assignment is handled by an explicit SQL false predicate, not by omitting scope.
    from reorganize_website import connect
    with connect('xinke_reorg_test_2026112601') as c:
        with c.cursor() as q:
            q.execute("delete from live_subject_user_map where user_id=%s and subject_type='SHOP'",(uid,))
        c.commit()
    assert rows()==[]
    save([],'OWN','OWN')
    assert request('/live/freight',token=user)['code']==403
    info=request('/getInfo',token=user)
    assert not any(p.startswith('live:freight:') for p in info['permissions'])
    assert 'live/freight/index' not in json.dumps(request('/getRouters',token=user))
    save(['view','dashboard','create','edit','import','export'],'ALL','ALL')
    assert request('/live/freight','POST',record(b,'-all'),user)['code']==200
    assert len(rows())==4
    # Basic role editing cannot remove the navigation role maintained by the personal policy.
    roles=request('/system/user/authRole/'+str(uid),token=admin)['roles']
    manual=[r['roleId'] for r in roles if r.get('flag') and not r['roleKey'].startswith(('auto_live_','personnel_freight_'))]
    query=urllib.parse.urlencode({'userId':uid,'roleIds':','.join(map(str,manual))})
    assert request('/system/user/authRole?'+query,'PUT',token=admin)['code']==200
    assert request('/live/freight/capabilities',token=user)['data']['create']
    detail=request('/live/personnel/'+str(uid),token=admin)['data']
    assert detail['account']['nickName'] and detail['account']['userName'] and detail['permissions']
    query=urllib.parse.urlencode({'params[personKeyword]':detail['account']['nickName']})
    search=request('/system/user/list?'+query,token=admin)
    assert any(r['userId']==uid for r in search['rows'])
    print('PASS: all/selected/own/empty-assignment scopes; action denial; cross-shop edit; immediate revoke; stale version; managed-role preservation; name search')


if __name__=='__main__':main()
