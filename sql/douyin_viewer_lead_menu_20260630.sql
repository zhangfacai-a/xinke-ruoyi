-- Douyin live viewer lead menu and permissions.

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '直播追单', menu_id, 20, 'live-viewer', 'live/viewer/index', null, 'LiveViewerLead', 1, 0, 'C', '0', '0', 'live:viewer:list', 'peoples', 'admin', sysdate(), '抖音直播观众追单池'
from sys_menu
where menu_name = 'ERP'
  and parent_id = 0
  and not exists (select 1 from sys_menu where perms = 'live:viewer:list');

set @live_viewer_menu_id := (select menu_id from sys_menu where perms = 'live:viewer:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '追单详情', @live_viewer_menu_id, 1, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:viewer:query', '#', 'admin', sysdate(), '查看直播追单详情'
where @live_viewer_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:viewer:query');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '编辑追单', @live_viewer_menu_id, 2, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:viewer:edit', '#', 'admin', sysdate(), '编辑直播追单状态'
where @live_viewer_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:viewer:edit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '记录跟进', @live_viewer_menu_id, 3, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:viewer:follow', '#', 'admin', sysdate(), '新增直播追单跟进记录'
where @live_viewer_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:viewer:follow');
