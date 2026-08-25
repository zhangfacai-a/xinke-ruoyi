-- 将直播运营菜单整理为“礼品”和“直播”两个二级目录，可重复执行。
start transaction;

set @live_parent_id = (select menu_id from sys_menu where menu_type='M' and path='live-ops' order by menu_id limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '礼品',@live_parent_id,1,'gift',null,null,'LiveGift',1,0,'M','0','0',null,'gift','admin',sysdate(),'礼品管理功能目录'
where @live_parent_id is not null
  and not exists(select 1 from sys_menu where route_name='GiftWorkbench' or component='live/giftWorkbench/index')
  and not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='gift');

insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '直播',@live_parent_id,2,'live',null,null,'LiveOps',1,0,'M','0','0',null,'video-camera','admin',sysdate(),'直播运营功能目录'
where @live_parent_id is not null
  and not exists(select 1 from sys_menu where route_name='GiftWorkbench' or component='live/giftWorkbench/index')
  and not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='live');

set @gift_parent_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='gift' limit 1);
set @stream_parent_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='live' limit 1);

-- 礼品相关页面
update sys_menu set parent_id=@gift_parent_id,order_num=1 where menu_id=2148 and @gift_parent_id is not null;
update sys_menu set parent_id=@gift_parent_id,order_num=2 where menu_id=2159 and @gift_parent_id is not null;
update sys_menu set parent_id=@gift_parent_id,order_num=3 where menu_id=2160 and @gift_parent_id is not null;
update sys_menu set parent_id=@gift_parent_id,order_num=4 where menu_id=2151 and @gift_parent_id is not null;
update sys_menu set parent_id=@gift_parent_id,order_num=5 where menu_id=2163 and @gift_parent_id is not null;

-- 直播相关页面
update sys_menu set parent_id=@stream_parent_id,order_num=1 where menu_id=2073 and @stream_parent_id is not null;
update sys_menu set parent_id=@stream_parent_id,order_num=2 where menu_id=2077 and @stream_parent_id is not null;
update sys_menu set parent_id=@stream_parent_id,order_num=3 where menu_id=2084 and @stream_parent_id is not null;
update sys_menu set parent_id=@stream_parent_id,order_num=4 where menu_id=2086 and @stream_parent_id is not null;
update sys_menu set parent_id=@gift_parent_id,order_num=6 where menu_id=2150 and @gift_parent_id is not null;

-- 让拥有子菜单权限的角色同时看到对应目录。
insert ignore into sys_role_menu(role_id,menu_id)
select distinct rm.role_id,@gift_parent_id from sys_role_menu rm
where rm.menu_id in (2148,2159,2160,2151,2163,2150) and @gift_parent_id is not null;
insert ignore into sys_role_menu(role_id,menu_id)
select distinct rm.role_id,@stream_parent_id from sys_role_menu rm
where rm.menu_id in (2073,2077,2084,2086) and @stream_parent_id is not null;

commit;
