-- Add menu entry for live viewer BI dashboard.

set @parent_id := (
    select menu_id
    from sys_menu
    where menu_name = '直播运营'
      and parent_id = 0
    order by menu_id desc
    limit 1
);

insert into sys_menu(
    menu_name, parent_id, order_num, path, component, query, route_name,
    is_frame, is_cache, menu_type, visible, status, perms, icon,
    create_by, create_time, remark
)
select
    '直播追单BI', @parent_id, 3, 'live-bi', 'live/bi/index', null, 'LiveViewerBi',
    1, 0, 'C', '0', '0', 'live:viewer:list', 'chart',
    'admin', sysdate(), '直播追单经营分析看板'
where @parent_id is not null
  and not exists (
      select 1
      from sys_menu
      where parent_id = @parent_id
        and path = 'live-bi'
  );
