-- Live Operations / Plugin Control menu and default config.
-- Execute this on the target database after deploying backend/frontend.

insert into sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark)
select 'Live plugin enabled', 'live.plugin.enabled', 'true', 'Y', 'admin', sysdate(), 'When disabled, reports receive a stop response.'
where not exists (select 1 from sys_config where config_key = 'live.plugin.enabled');

insert into sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark)
select 'Live plugin minimum version', 'live.plugin.minVersion', '4.0.8', 'Y', 'admin', sysdate(), 'Reports from lower plugin versions are blocked.'
where not exists (select 1 from sys_config where config_key = 'live.plugin.minVersion');

insert into sys_config(config_name, config_key, config_value, config_type, create_by, create_time, remark)
select 'Live plugin latest version', 'live.plugin.latestVersion', '4.0.8', 'Y', 'admin', sysdate(), 'Recommended plugin version.'
where not exists (select 1 from sys_config where config_key = 'live.plugin.latestVersion');

set @live_ops_menu_id := (
    select menu_id
    from sys_menu
    where parent_id = 0 and path = 'live-ops'
    order by menu_id desc
    limit 1
);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon,
                     create_by, create_time, remark)
select '插件控制', @live_ops_menu_id, 4, 'live-plugin', 'live/plugin/index', '', 'LivePluginControl',
       1, 0, 'C', '0', '0', 'live:plugin:control', 'monitor',
       'admin', sysdate(), 'Live capture plugin switch and version policy'
where @live_ops_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:plugin:control');

update sys_menu
set menu_name = '插件控制',
    parent_id = @live_ops_menu_id,
    order_num = 4,
    path = 'live-plugin',
    component = 'live/plugin/index',
    route_name = 'LivePluginControl',
    visible = '0',
    status = '0',
    icon = 'monitor',
    update_time = sysdate()
where perms = 'live:plugin:control'
  and @live_ops_menu_id is not null;