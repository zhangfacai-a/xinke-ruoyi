-- 抖音旧观众采集、影刀/RPA 数据清理
--
-- 这是一次有意的数据删除迁移。执行前请先对 xinkeerp 做完整备份，确认
-- 旧数据不再需要。脚本可重复执行；不存在的表、菜单和配置会被跳过。
--
-- 保留：live_room（礼品/直播资料模块使用）、erp_gift_*、订单、库存、钉钉
-- 人员同步以及新榜单表 dy_audience_rank_*。

-- 旧触发器可能依赖将要删除的表，先移除。
drop trigger if exists trg_dy_viewer_comment_bi;
drop trigger if exists trg_dy_viewer_comment_ai;
drop trigger if exists trg_dy_viewer_status_bi;
drop trigger if exists trg_dy_viewer_status_bu;
drop trigger if exists trg_dy_viewer_daily_lead_status_bi;
drop trigger if exists trg_dy_viewer_daily_lead_status_bu;

-- 这些表属于旧插件/影刀或旧观众追单链路，不属于新双榜导入模块。
set foreign_key_checks = 0;
drop table if exists dy_rpa_outreach_task;
drop table if exists dy_rpa_outreach_batch;
drop table if exists dy_rpa_task_claim;
drop table if exists dy_rpa_task_lease;
drop table if exists dy_rpa_task_batch;
drop table if exists dy_rpa_batch_lease;
drop table if exists dy_rpa_result_request;
drop table if exists dy_rpa_shop_viewer;
drop table if exists dy_rpa_marketing_suppression;
drop table if exists dy_rpa_viewer_blacklist;
drop table if exists dy_rpa_viewer_rule;
drop table if exists dy_rpa_tracking_config;
drop table if exists dy_rpa_room_shop;
drop table if exists dy_rpa_shop_config;
drop table if exists dy_live_audience_observation;
drop table if exists dy_live_viewer_stay;
drop table if exists dy_live_session;
drop table if exists dy_live_room;
drop table if exists dy_capture_batch;
drop table if exists dy_plugin_client;
drop table if exists dy_viewer_follow_record;
drop table if exists dy_viewer_comment;
drop table if exists dy_viewer_daily_lead;
drop table if exists dy_viewer;
set foreign_key_checks = 1;

-- 删除旧系统菜单及其功能按钮权限。新“观众榜单”菜单使用独立权限，
-- 不会被下面的条件匹配。
create temporary table if not exists tmp_dy_obsolete_menu_ids (
  menu_id bigint not null primary key
);
create temporary table if not exists tmp_dy_obsolete_parent_ids (
  menu_id bigint not null primary key
);

insert ignore into tmp_dy_obsolete_menu_ids(menu_id)
select menu_id
from sys_menu
where perms in (
  'live:plugin:control',
  'live:viewer:list', 'live:viewer:query', 'live:viewer:edit',
  'live:viewer:follow', 'live:viewer:export',
  'live:rpa:config',
  'live:room:list', 'live:room:query', 'live:room:add',
  'live:room:edit', 'live:room:remove',
  'monitor:token:view'
)
or component in (
  'live/plugin/index', 'live/viewer/index', 'live/bi/index',
  'live/room/index', 'live/rpaWorkbench/index', 'monitor/token/index'
)
or route_name in ('LivePluginControl', 'LiveViewerLead', 'LiveViewerBi', 'RpaWorkbench')
or path in ('live-plugin', 'live-viewer', 'live-bi', 'live-room', 'rpa-workbench');

-- 旧追单和插件菜单的按钮通常挂在页面菜单下，补收一层子菜单。
insert ignore into tmp_dy_obsolete_parent_ids(menu_id)
select menu_id from tmp_dy_obsolete_menu_ids;
insert ignore into tmp_dy_obsolete_menu_ids(menu_id)
select child.menu_id
from sys_menu child
join tmp_dy_obsolete_parent_ids parent on parent.menu_id = child.parent_id;

delete from sys_role_menu
where menu_id in (select menu_id from tmp_dy_obsolete_menu_ids);
delete from sys_menu
where menu_id in (select menu_id from tmp_dy_obsolete_menu_ids);
drop temporary table if exists tmp_dy_obsolete_parent_ids;
drop temporary table if exists tmp_dy_obsolete_menu_ids;

-- 清理旧插件开关、版本策略和影刀租约配置；新扩展使用
-- live.audience-rank.upload-key（应用配置）和 X-Audience-Upload-Key。
delete from sys_config
where config_key in (
  'live.plugin.enabled',
  'live.plugin.minVersion',
  'live.plugin.latestVersion'
)
or config_key like 'live.rpa.%';
