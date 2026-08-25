-- 直播资料中心升级：礼品版本、直播间/店铺人员映射、个人快捷模板。
-- MySQL 8.0+，可重复执行。

alter table live_room modify shop_id bigint null;

create table if not exists erp_gift_version (
  version_id bigint not null auto_increment,
  gift_id bigint not null,
  action_type varchar(20) not null,
  gift_code varchar(32) not null,
  gift_name varchar(120) not null,
  short_name varchar(60) null,
  brand varchar(60) null,
  model varchar(80) null,
  specification varchar(120) null,
  unit varchar(20) not null,
  category varchar(60) null,
  purchase_type varchar(30) null,
  status char(1) not null,
  sort_order int not null,
  aliases_snapshot varchar(1000) null,
  remark varchar(500) null,
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  primary key (version_id),
  key idx_gift_version_gift (gift_id, version_id)
) engine=InnoDB default charset=utf8mb4 comment='礼品档案版本历史';

-- 手工调成本是追加记录；同一生效日允许再次追加，以最后创建的记录为准。
set @gift_cost_unique=(select count(*) from information_schema.statistics
  where table_schema=database() and table_name='erp_gift_cost' and index_name='uk_gift_cost_date');
set @drop_gift_cost_unique=if(@gift_cost_unique>0,
  'alter table erp_gift_cost drop index uk_gift_cost_date','select 1');
prepare gift_cost_stmt from @drop_gift_cost_unique;
execute gift_cost_stmt;
deallocate prepare gift_cost_stmt;
set @gift_cost_version_index=(select count(*) from information_schema.statistics
  where table_schema=database() and table_name='erp_gift_cost' and index_name='idx_gift_cost_date_version');
set @create_gift_cost_version_index=if(@gift_cost_version_index=0,
  'create index idx_gift_cost_date_version on erp_gift_cost(gift_id,effective_date,cost_id)','select 1');
prepare gift_cost_index_stmt from @create_gift_cost_version_index;
execute gift_cost_index_stmt;
deallocate prepare gift_cost_index_stmt;

insert into erp_gift_version(
  gift_id,action_type,gift_code,gift_name,short_name,brand,model,specification,unit,
  category,purchase_type,status,sort_order,aliases_snapshot,remark,create_by,create_time)
select g.gift_id,'BASELINE',g.gift_code,g.gift_name,g.short_name,g.brand,g.model,g.specification,g.unit,
  g.category,g.purchase_type,g.status,g.sort_order,
  (select group_concat(a.alias_name order by a.alias_name separator '、') from erp_gift_alias a where a.gift_id=g.gift_id),
  g.remark,'migration',sysdate()
from erp_gift g
where not exists(select 1 from erp_gift_version v where v.gift_id=g.gift_id);

create table if not exists live_subject_user_map (
  subject_type varchar(16) not null comment 'ROOM或SHOP',
  subject_id bigint not null,
  user_id bigint not null,
  role_code varchar(32) not null comment 'anchor或controller',
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  primary key (subject_type,subject_id,user_id,role_code),
  key idx_live_subject_user (user_id,role_code)
) engine=InnoDB default charset=utf8mb4 comment='直播间或店铺与主播场控映射';

create table if not exists live_quick_template (
  template_id bigint not null auto_increment,
  user_id bigint not null,
  template_name varchar(100) not null,
  content_json json not null,
  status char(1) not null default '0',
  sort_order int not null default 100,
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  update_by varchar(64) default '',
  update_time datetime null,
  primary key (template_id),
  unique key uk_live_quick_template_name (user_id,template_name),
  key idx_live_quick_template_user (user_id,status,sort_order)
) engine=InnoDB default charset=utf8mb4 comment='用户私有直播快捷模板';

-- “直播资料”作为资料中心入口，子功能分别授权。
set @live_admin_id=(select menu_id from sys_menu where component='live/giftAdmin/index' limit 1);
set @gift_catalog_id=(select menu_id from sys_menu where perms='live:gift:catalog' limit 1);

update sys_menu set menu_name='直播资料',perms='live:gift:admin',remark='礼品、直播间店铺、人员映射和个人模板资料中心'
where menu_id=@live_admin_id;

update sys_menu set menu_name='礼品管理',parent_id=@live_admin_id,order_num=1,path='',component=null,
  route_name='',menu_type='F',icon='#',remark='资料中心礼品管理页签'
where menu_id=@gift_catalog_id and @live_admin_id is not null;

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '直播间或店铺管理',@live_admin_id,2,'',null,null,'',1,0,'F','0','0','live:gift:room','#','admin',sysdate(),'资料中心直播间或店铺管理页签'
where not exists(select 1 from sys_menu where perms='live:gift:room');

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '人员映射',@live_admin_id,3,'',null,null,'',1,0,'F','0','0','live:gift:mapping','#','admin',sysdate(),'直播间或店铺主播场控映射'
where not exists(select 1 from sys_menu where perms='live:gift:mapping');

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '个人快捷模板',@live_admin_id,4,'',null,null,'',1,0,'F','0','0','live:gift:template','#','admin',sysdate(),'当前登录账号私有快捷模板'
where not exists(select 1 from sys_menu where perms='live:gift:template');

-- 旧直播登记权限不再使用；保留历史登记数据表。
delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms='live:gift:daily');
delete from sys_menu where perms='live:gift:daily';

-- 超级管理员获得资料中心全部权限。
insert ignore into sys_role_menu(role_id,menu_id)
select 1,menu_id from sys_menu
where menu_id=@live_admin_id or parent_id=@live_admin_id;
