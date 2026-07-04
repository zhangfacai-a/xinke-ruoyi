-- 系统监控 / Token工具菜单
set @parent_id := (select menu_id from sys_menu where menu_name = '系统监控' and parent_id = 0 limit 1);
set @next_id := (select ifnull(max(menu_id), 2000) + 1 from sys_menu);

insert into sys_menu (
    menu_id, menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, update_by, update_time, remark
)
select
    @next_id, 'Token工具', @parent_id, 9, 'token', 'monitor/token/index', '', '',
    1, 0, 'C', '0', '0', 'monitor:token:view', 'password',
    'admin', sysdate(), '', null, '外部BI嵌入Token获取工具'
where @parent_id is not null
  and not exists (
      select 1 from sys_menu where parent_id = @parent_id and path = 'token'
  );
