-- 直播订单礼品管理菜单。可重复执行，不会重复创建同一路径菜单。
set @live_parent_id = (select menu_id from sys_menu where menu_type='M' and menu_name='直播运营' order by menu_id limit 1);
insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '直播运营',0,6,'liveOps',null,null,'LiveOps',1,0,'M','0','0','', 'monitor','admin',sysdate(),'直播运营与订单礼品管理'
where @live_parent_id is null;
set @live_parent_id = coalesce(@live_parent_id,last_insert_id());

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '订单录礼品',@live_parent_id,10,'giftEntry','live/giftEntry/index',null,'GiftEntry',1,0,'C','0','0','live:gift:entry','edit','admin',sysdate(),'订单快速选择礼品'
where not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftEntry');
set @gift_entry_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='giftEntry' limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '礼品记录',@live_parent_id,11,'giftLedger','live/giftLedger/index',null,'GiftLedger',1,0,'C','0','0','live:gift:ledger','list','admin',sysdate(),'订单礼品台账'
where not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftLedger');
set @gift_ledger_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='giftLedger' limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '礼品管理',@live_parent_id,12,'giftCatalog','live/giftCatalog/index',null,'GiftCatalog',1,0,'C','0','0','live:gift:catalog','shopping','admin',sysdate(),'礼品和成本管理'
where not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftCatalog');
set @gift_catalog_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='giftCatalog' limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '我的礼品设置',@live_parent_id,13,'giftPreference','live/giftPreference/index',null,'GiftPreference',1,0,'C','0','0','live:gift:preference','star','admin',sysdate(),'当前账号的礼品显示、隐藏和置顶设置'
where not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftPreference');
set @gift_preference_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='giftPreference' limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '直播资料',@live_parent_id,13,'giftAdmin','live/giftAdmin/index',null,'GiftAdmin',1,0,'C','0','0','live:gift:daily','peoples','admin',sysdate(),'直播间和每日直播登记'
where not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftAdmin');
set @gift_admin_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='giftAdmin' limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '礼品成本汇总',@live_parent_id,14,'giftSummary','live/giftSummary/index',null,'GiftSummary',1,0,'C','0','0','live:gift:summary','money','admin',sysdate(),'礼品成本汇总'
where not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftSummary');

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '调整礼品成本',@gift_catalog_id,1,'',null,null,'',1,0,'F','0','0','live:gift:cost','#','admin',sysdate(),'调整礼品历史成本'
where not exists(select 1 from sys_menu where perms='live:gift:cost');
insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '导出礼品记录',@gift_ledger_id,1,'',null,null,'',1,0,'F','0','0','live:gift:export','#','admin',sysdate(),'导出订单礼品台账'
where not exists(select 1 from sys_menu where perms='live:gift:export');
delete from sys_role_menu where menu_id in (select menu_id from sys_menu where perms='live:gift:staff');
delete from sys_menu where perms='live:gift:staff';

-- 默认给超级管理员角色补齐全部新增菜单权限。
insert ignore into sys_role_menu(role_id,menu_id)
select 1,menu_id from sys_menu where menu_id=@live_parent_id or parent_id=@live_parent_id or parent_id in (@gift_catalog_id,@gift_ledger_id,@gift_admin_id);
insert ignore into sys_role_menu(role_id,menu_id)
select distinct rm.role_id,@gift_preference_id from sys_role_menu rm join sys_menu m on m.menu_id=rm.menu_id where m.perms in ('live:gift:entry','live:gift:catalog') and @gift_preference_id is not null;

-- 订单录入已经合并到“礼品记录”，保留为按钮权限，不再单独显示页面。
update sys_menu
set menu_name='录入订单礼品',parent_id=@gift_ledger_id,order_num=1,path='',component=null,
    route_name='',menu_type='F',icon='#',remark='在礼品记录页手工输入订单号录入礼品'
where perms='live:gift:entry';
