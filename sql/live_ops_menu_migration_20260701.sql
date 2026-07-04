-- Move live operation pages from ERP to a standalone "直播运营" directory.

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon,
                     create_by, create_time, remark)
select '直播运营', 0, 6, 'live-ops', null, '', '', 1, 0, 'M', '0', '0', '', 'chart',
       'admin', sysdate(), '直播追单、直播间映射等直播运营工具'
where not exists (
    select 1 from sys_menu where parent_id = 0 and menu_name = '直播运营'
);

set @live_ops_menu_id := (
    select menu_id
    from sys_menu
    where parent_id = 0 and menu_name = '直播运营'
    order by menu_id desc
    limit 1
);

update sys_menu
set parent_id = @live_ops_menu_id,
    order_num = 1,
    menu_name = '直播追单',
    path = 'live-viewer',
    component = 'live/viewer/index',
    route_name = 'LiveViewerLead',
    icon = 'peoples',
    visible = '0',
    status = '0',
    update_time = sysdate()
where perms = 'live:viewer:list'
  and @live_ops_menu_id is not null;

update sys_menu
set parent_id = @live_ops_menu_id,
    order_num = 2,
    menu_name = '直播间映射',
    path = 'live-room',
    component = 'live/room/index',
    route_name = 'LiveRoomMapping',
    icon = 'tree-table',
    visible = '0',
    status = '0',
    update_time = sysdate()
where perms = 'live:room:list'
  and @live_ops_menu_id is not null;

update sys_menu set menu_name = '追单详情', update_time = sysdate() where perms = 'live:viewer:query';
update sys_menu set menu_name = '编辑追单', update_time = sysdate() where perms = 'live:viewer:edit';
update sys_menu set menu_name = '记录跟进', update_time = sysdate() where perms = 'live:viewer:follow';
update sys_menu set menu_name = '映射详情', update_time = sysdate() where perms = 'live:room:query';
update sys_menu set menu_name = '新增映射', update_time = sysdate() where perms = 'live:room:add';
update sys_menu set menu_name = '编辑映射', update_time = sysdate() where perms = 'live:room:edit';
update sys_menu set menu_name = '删除映射', update_time = sysdate() where perms = 'live:room:remove';
