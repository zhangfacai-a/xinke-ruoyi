-- 将“直播资料”的三个页签拆成三个独立菜单页面，可重复执行。
start transaction;

set @material_id=(select menu_id from sys_menu where component='live/giftAdmin/index' order by menu_id limit 1);
set @gift_page_id=(select menu_id from sys_menu where perms='live:gift:catalog' order by (menu_type='C') desc,menu_id limit 1);
set @gift_perm_id=(select menu_id from sys_menu where perms='live:gift:catalog' and menu_id<>@gift_page_id order by menu_id limit 1);
set @subject_page_id=(select menu_id from sys_menu where perms='live:gift:room' order by menu_id limit 1);
set @template_page_id=(select menu_id from sys_menu where perms='live:gift:template' order by menu_id limit 1);
set @mapping_perm_id=(select menu_id from sys_menu where perms='live:gift:mapping' order by menu_id limit 1);
set @inventory_perm_id=(select menu_id from sys_menu where perms='live:gift:inventory' order by menu_id limit 1);

-- 原页面改为只负责菜单分组的目录。
update sys_menu
set menu_name='直播资料',path='materials',component=null,route_name='LiveMaterials',menu_type='M',perms=null,
    remark='礼品与库存、直播间店铺、快捷模板目录'
where menu_id=@material_id;

-- 复用原权限节点，角色授权关系无需重新配置。
update sys_menu
set menu_name='礼品与库存',parent_id=@material_id,order_num=1,path='giftMaterial',
    component='live/giftMaterial/index',route_name='GiftMaterial',menu_type='C',visible='0',status='0',icon='gift',
    remark='礼品资料、成本版本和库存管理'
where menu_id=@gift_page_id and @material_id is not null;

update sys_menu
set menu_name='直播间 / 店铺',parent_id=@material_id,order_num=2,path='liveSubject',
    component='live/liveSubject/index',route_name='LiveSubject',menu_type='C',icon='peoples',
    remark='直播间店铺资料及主播场控映射'
where menu_id=@subject_page_id and @material_id is not null;

update sys_menu
set menu_name='快捷模板',parent_id=@material_id,order_num=3,path='quickTemplate',
    component='live/quickTemplate/index',route_name='QuickTemplate',menu_type='C',icon='edit',
    remark='当前登录账号私有快捷模板'
where menu_id=@template_page_id and @material_id is not null;

-- 页面内按钮权限继续挂在对应页面下面。
update sys_menu set parent_id=@gift_page_id,menu_type='F',path='',component=null,route_name='',icon='#'
where menu_id=@gift_perm_id and @material_id is not null;
update sys_menu set parent_id=@gift_page_id,menu_type='F',path='',component=null,route_name='',icon='#'
where menu_id=@inventory_perm_id and @inventory_perm_id<>@gift_page_id and @material_id is not null;
update sys_menu set parent_id=@subject_page_id,menu_type='F',path='',component=null,route_name='',icon='#'
where menu_id=@mapping_perm_id and @mapping_perm_id<>@subject_page_id and @material_id is not null;

-- 持有任一子页面的角色都应能看到“直播资料”目录。
insert ignore into sys_role_menu(role_id,menu_id)
select distinct role_id,@material_id from sys_role_menu
where menu_id in (@gift_page_id,@gift_perm_id,@subject_page_id,@template_page_id,@mapping_perm_id,@inventory_perm_id)
  and @material_id is not null;

insert ignore into sys_role_menu(role_id,menu_id)
select distinct role_id,@gift_page_id from sys_role_menu
where menu_id in (@gift_perm_id,@inventory_perm_id) and @gift_page_id is not null and @material_id is not null;

commit;
