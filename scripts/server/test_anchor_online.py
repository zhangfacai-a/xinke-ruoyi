"""Exercise production endpoints using an isolated, labelled fixture and remove only that fixture."""
from pathlib import Path
from datetime import datetime,timedelta
import json,time,subprocess
from anchor_database import connect,ROOT

stamp=str(int(time.time()*1000));name='CODEX_APP_TEST_'+stamp
fixture={'roomName':name,'platformId':str(9000000000000000000+int(stamp)),'liveStartTime':(datetime.now()-timedelta(hours=1)).strftime('%Y-%m-%d %H:%M:%S')}
path=ROOT/'.codex-deploy/anchor-test-fixture.json'
with connect() as conn:
    with conn.cursor() as c:
        c.execute("insert into sys_user(user_name,nick_name,status,del_flag,password,create_by,create_time) values(%s,%s,'0','0','!test-no-login','anchor-app-test',now())",(name,name));fixture['anchorId']=c.lastrowid
        c.execute("insert into live_room(room_code,room_name,status,create_by,remark) values(%s,%s,'0','anchor-app-test','Temporary automated test fixture')",(name,name));fixture['roomId']=c.lastrowid
    conn.commit()
path.write_text(json.dumps(fixture),encoding='utf-8')
try:
    subprocess.run(['dotnet','run','--project',str(ROOT/'clients/tests/desktop-api/DesktopApiTests.csproj'),'--','--integration',str(path)],check=True,cwd=ROOT)
finally:
    with connect() as conn:
        with conn.cursor() as c:
            c.execute('select session_id from live_metric_session where platform_session_id=%s and reported_room_name=%s',(fixture['platformId'],name));ids=[r[0] for r in c.fetchall()]
            for session in ids:
                c.execute('select segment_id from live_metric_anchor_segment where session_id=%s',(session,));segments=[r[0] for r in c.fetchall()]
                for segment in segments:c.execute('delete from bi_live_anchor_segment_fact where segment_id=%s',(segment,))
                c.execute('delete from live_metric_segment_correction where session_id=%s',(session,))
                c.execute('delete from live_metric_anchor_segment where session_id=%s',(session,))
                c.execute('delete from live_metric_snapshot where session_id=%s',(session,))
                c.execute('delete from live_metric_session where session_id=%s',(session,))
            c.execute("delete from live_subject_user_map where subject_type='ROOM' and subject_id=%s",(fixture['roomId'],))
            c.execute('delete from live_metric_room_alias where room_id=%s',(fixture['roomId'],))
            c.execute('delete from live_room where room_id=%s and room_code=%s',(fixture['roomId'],name))
            c.execute('delete from sys_user_role where user_id=%s',(fixture['anchorId'],))
            c.execute("delete from sys_user where user_id=%s and user_name=%s and create_by='anchor-app-test'",(fixture['anchorId'],name))
        conn.commit()
    path.unlink(missing_ok=True)
    print('Isolated online test fixture removed; business records were not used for test writes.')
