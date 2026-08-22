-- 将旧版直播人员迁入系统用户并删除多余人员表。
-- 执行前请先备份数据库。默认密码 xk123456，仅用于新建账户。

create table if not exists live_ding_dept_bind (
  ding_dept_id bigint not null, sys_dept_id bigint not null,
  parent_ding_dept_id bigint not null default 0, dept_name varchar(100) not null,
  sync_batch varchar(64) not null, last_sync_time datetime not null,
  create_time datetime default current_timestamp,
  primary key (ding_dept_id), unique key uk_ding_dept_sys (sys_dept_id),
  key idx_ding_dept_batch (sync_batch)
) engine=InnoDB default charset=utf8mb4 comment='钉钉部门与系统部门绑定';

create table if not exists live_ding_user_bind (
  ding_user_id varchar(128) not null, user_id bigint not null,
  sync_batch varchar(64) not null, last_sync_time datetime not null,
  create_time datetime default current_timestamp,
  primary key (ding_user_id), key idx_ding_user_sys (user_id),
  key idx_ding_user_batch (sync_batch)
) engine=InnoDB default charset=utf8mb4 comment='钉钉人员与系统用户绑定';

create table if not exists live_daily_user (
  daily_id bigint not null, user_id bigint not null, role_code varchar(32) not null,
  primary key (daily_id, user_id, role_code), key idx_daily_user (user_id, role_code)
) engine=InnoDB default charset=utf8mb4 comment='每日直播系统用户';

drop procedure if exists migrate_live_staff_to_sys_user;
delimiter //
create procedure migrate_live_staff_to_sys_user()
begin
  declare invalid_count int default 0;
  declare conflict_count int default 0;
  declare ding_root_dept_id bigint;

  create table if not exists live_staff_backup_before_sys_user_migration like live_staff;
  create table if not exists live_staff_role_backup_before_sys_user_migration like live_staff_role;
  create table if not exists live_daily_staff_backup_before_sys_user_migration like live_daily_staff;
  insert ignore into live_staff_backup_before_sys_user_migration select * from live_staff;
  insert ignore into live_staff_role_backup_before_sys_user_migration select * from live_staff_role;
  insert ignore into live_daily_staff_backup_before_sys_user_migration select * from live_daily_staff;

  select count(*) into invalid_count
  from live_staff
  where mobile is null or mobile not regexp '^1[3-9][0-9]{9}$';
  if invalid_count > 0 then
    signal sqlstate '45000' set message_text='迁移停止：live_staff 中存在无效手机号，请修正后重试';
  end if;

  select count(*) into conflict_count
  from live_staff s
  where (select count(*) from sys_user u
    where (u.user_name=s.mobile or u.phonenumber=s.mobile) and u.del_flag='0') > 1;
  if conflict_count > 0 then
    signal sqlstate '45000' set message_text='迁移停止：系统用户中同一手机号匹配多个账户，请先合并账户';
  end if;

  insert into sys_dept(parent_id,ancestors,dept_name,order_num,status,del_flag,create_by,create_time)
  select 0,'0','钉钉通讯录',100,'0','0','admin',sysdate()
  where not exists(select 1 from sys_dept where parent_id=0 and dept_name='钉钉通讯录' and del_flag='0');
  select dept_id into ding_root_dept_id from sys_dept
  where parent_id=0 and dept_name='钉钉通讯录' and del_flag='0' order by dept_id limit 1;

  insert into sys_user(dept_id,user_name,nick_name,phonenumber,avatar,sex,password,status,del_flag,create_by,create_time,remark)
  select ding_root_dept_id,s.mobile,left(s.staff_name,30),s.mobile,left(s.avatar_url,100),'2',
    '$2a$10$eaSo8.rhQXKld/rfiOTeQeU4S5OpQtgG6gGEDu/0sgj0L0WGLIA6S',s.status,'0','admin',sysdate(),'由旧版钉钉人员迁移'
  from live_staff s
  where s.staff_id=(select min(x.staff_id) from live_staff x where x.mobile=s.mobile)
    and not exists(select 1 from sys_user u where (u.user_name=s.mobile or u.phonenumber=s.mobile) and u.del_flag='0');

  update sys_user u join live_staff s on u.phonenumber=s.mobile and u.del_flag='0'
    left join sys_user x on x.user_name=s.mobile and x.user_id<>u.user_id and x.del_flag='0'
  set u.user_name=s.mobile,u.nick_name=left(s.staff_name,30),u.avatar=coalesce(nullif(left(s.avatar_url,100),''),u.avatar),
    u.status=s.status,u.update_by='admin',u.update_time=sysdate()
  where s.staff_id=(select min(x.staff_id) from live_staff x where x.mobile=s.mobile)
    and x.user_id is null;

  insert into live_ding_user_bind(ding_user_id,user_id,sync_batch,last_sync_time,create_time)
  select s.ding_user_id,u.user_id,'legacy-migration',sysdate(),sysdate()
  from live_staff s join sys_user u on (u.user_name=s.mobile or u.phonenumber=s.mobile) and u.del_flag='0'
  on duplicate key update user_id=values(user_id),sync_batch=values(sync_batch),last_sync_time=sysdate();

  insert ignore into live_daily_user(daily_id,user_id,role_code)
  select ds.daily_id,b.user_id,ds.role_code
  from live_daily_staff ds join live_staff s on s.staff_id=ds.staff_id
    join live_ding_user_bind b on b.ding_user_id=s.ding_user_id;

  update live_room r join live_staff s on s.staff_id=r.owner_staff_id
    join live_ding_user_bind b on b.ding_user_id=s.ding_user_id
  set r.owner_staff_id=b.user_id;

  drop table live_daily_staff;
  drop table live_staff_role;
  drop table live_staff;
end//
delimiter ;

call migrate_live_staff_to_sys_user();
drop procedure migrate_live_staff_to_sys_user;
