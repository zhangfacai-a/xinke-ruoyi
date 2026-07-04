set @live_viewer_menu_id := (select menu_id from sys_menu where perms = 'live:viewer:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '导出明细', @live_viewer_menu_id, 4, '#', '', null, null,
       1, 0, 'F', '0', '0', 'live:viewer:export', '#', 'admin', sysdate(), '导出直播追单明细'
where @live_viewer_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:viewer:export');
