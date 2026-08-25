-- 取消“礼品”和“直播资料”目录，建立平级的礼品工作台、直播间/店铺、礼品与库存。
-- 可重复执行。
start transaction;

set @live_root_id=(select menu_id from sys_menu where menu_type='M' and path='live-ops' order by menu_id limit 1);
set @gift_dir_id=(select menu_id from sys_menu where parent_id=@live_root_id and menu_type='M' and path='gift' order by menu_id limit 1);
set @material_dir_id=(select menu_id from sys_menu where menu_type='M' and path='materials' order by menu_id limit 1);
set @ledger_perm_id=(select menu_id from sys_menu where perms='live:gift:ledger' order by menu_id limit 1);
set @preference_perm_id=(select menu_id from sys_menu where perms='live:gift:preference' order by menu_id limit 1);
set @summary_perm_id=(select menu_id from sys_menu where perms='live:gift:summary' order by menu_id limit 1);
set @template_perm_id=(select menu_id from sys_menu where perms='live:gift:template' order by menu_id limit 1);
set @subject_page_id=(select menu_id from sys_menu where component='live/liveSubject/index' order by menu_id limit 1);
set @material_page_id=(select menu_id from sys_menu where component='live/giftMaterial/index' order by menu_id limit 1);
set @live_dir_id=(select menu_id from sys_menu where parent_id=@live_root_id and menu_type='M' and path='live' order by menu_id limit 1);
set @audience_page_id=(select menu_id from sys_menu where component='live/audienceRank/index' order by menu_id limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '礼品工作台',@live_root_id,1,'giftWorkbench','live/giftWorkbench/index',null,'GiftWorkbench',1,0,'C','0','0',null,'shopping','admin',sysdate(),'礼品记录、个人礼品设置、礼品经营BI和快捷模板'
where @live_root_id is not null
  and not exists(select 1 from sys_menu where route_name='GiftWorkbench' or component='live/giftWorkbench/index');

set @workbench_id=(select menu_id from sys_menu where route_name='GiftWorkbench' or component='live/giftWorkbench/index' order by menu_id limit 1);

update sys_menu set icon='shopping',visible='0',status='0' where menu_id=@workbench_id;

-- 原页面菜单改为工作台内部权限节点，保留原角色授权和权限字符串。
update sys_menu set parent_id=@workbench_id,order_num=1,path='',component=null,route_name='',menu_type='F',icon='#',remark='礼品工作台：礼品记录'
where menu_id=@ledger_perm_id;
update sys_menu set parent_id=@workbench_id,order_num=2,path='',component=null,route_name='',menu_type='F',icon='#',remark='礼品工作台：我的礼品设置'
where menu_id=@preference_perm_id;
update sys_menu set parent_id=@workbench_id,order_num=3,path='',component=null,route_name='',menu_type='F',icon='#',remark='礼品工作台：礼品经营BI'
where menu_id=@summary_perm_id;
update sys_menu set parent_id=@workbench_id,order_num=4,path='',component=null,route_name='',menu_type='F',icon='#',remark='礼品工作台：快捷模板'
where menu_id=@template_perm_id;

-- 两个资料页面与礼品工作台平级。
update sys_menu set parent_id=@live_root_id,order_num=2,path='liveSubject',icon='peoples',visible='0',status='0' where menu_id=@subject_page_id;
update sys_menu set parent_id=@live_root_id,order_num=3,path='giftMaterial',icon='list',visible='0',status='0' where menu_id=@material_page_id;
update sys_menu set visible='0',status='0' where perms='live:gift:inventory';
update sys_menu set parent_id=@live_root_id,order_num=4,icon='user',visible='0',status='0' where menu_id=@audience_page_id;

-- 拥有任一工作台功能的角色都能看到工作台入口。
insert ignore into sys_role_menu(role_id,menu_id)
select distinct role_id,@workbench_id from sys_role_menu
where menu_id in (@ledger_perm_id,@preference_perm_id,@summary_perm_id,@template_perm_id)
  and @workbench_id is not null;

-- 平级页面沿用自身授权，并补齐顶层直播运营目录授权。
insert ignore into sys_role_menu(role_id,menu_id)
select distinct role_id,@live_root_id from sys_role_menu
where menu_id in (@workbench_id,@subject_page_id,@material_page_id,@audience_page_id) and @live_root_id is not null;

-- 两个旧目录已无页面，不再保留。
delete from sys_role_menu where menu_id in (@material_dir_id,@gift_dir_id,@live_dir_id);
delete from sys_menu where menu_id=@material_dir_id;
delete from sys_menu where menu_id=@gift_dir_id;
delete from sys_menu where menu_id=@live_dir_id;

commit;
