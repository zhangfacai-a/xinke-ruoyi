-- 抖音观众榜单菜单与权限
-- 可重复执行。先执行 dy_audience_rank.sql 建表，再执行本脚本。

set @live_root_id := (
  select menu_id
  from sys_menu
  where menu_type = 'M'
    and path = 'live-ops'
  order by menu_id
  limit 1
);

-- 优先放入“直播运营 > 直播”二级目录；未执行菜单分组时退回根目录。
set @live_parent_id := coalesce(
  (
    select menu_id
    from sys_menu
    where menu_type = 'M'
      and parent_id = @live_root_id
      and path = 'live'
    order by menu_id
    limit 1
  ),
  @live_root_id,
  (select menu_id from sys_menu where menu_type = 'M' and menu_name = '直播运营'
   and parent_id = 0 order by menu_id limit 1)
);

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, `query`, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select '观众客户', @live_parent_id, 5, 'audience-rank', 'live/audienceRank/index', null,
       'AudienceRank', 1, 0, 'C', '0', '0', 'live:audienceRank:list', 'user',
       'admin', sysdate(), '抖音评论榜与观看榜导入结果'
where @live_parent_id is not null
  and not exists (
    select 1 from sys_menu
    where perms = 'live:audienceRank:list'
  );

set @audience_rank_id := (
  select menu_id from sys_menu where perms = 'live:audienceRank:list' limit 1
);

update sys_menu
set menu_name = '观众客户', remark = '抖音观众客户跟进、团队运营与同步记录'
where menu_id = @audience_rank_id;

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, `query`, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select '查看榜单明细', @audience_rank_id, 1, '#', null, null, null,
       1, 0, 'F', '0', '0', 'live:audienceRank:query', '#',
       'admin', sysdate(), '查看抖音观众榜单明细和批次'
where @audience_rank_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:audienceRank:query');

insert ignore into sys_role_menu(role_id, menu_id)
select 1, menu_id
from sys_menu
where menu_id = @audience_rank_id
   or parent_id = @audience_rank_id;

-- 跟单工作台按钮权限。重复执行不会产生重复菜单。
insert into sys_menu(
  menu_name, parent_id, order_num, path, component, `query`, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select '跟单工作台', @audience_rank_id, 1, '#', null, null, null,
       1, 0, 'F', '0', '0', 'live:audienceRank:followup:list', '#',
       'admin', sysdate(), '查看观众跟单工作台'
where @audience_rank_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:audienceRank:followup:list');

set @followup_menu_id := (
  select menu_id from sys_menu where perms = 'live:audienceRank:followup:list' limit 1
);

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, `query`, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select x.menu_name, @followup_menu_id, x.order_num, '#', null, null, null,
       1, 0, 'F', '0', '0', x.perms, '#', 'admin', sysdate(), x.remark
from (
  select '查看跟单详情' menu_name, 1 order_num, 'live:audienceRank:followup:query' perms, '查看跟单档案详情' remark
  union all select '编辑跟单', 2, 'live:audienceRank:followup:edit', '编辑跟单资料与状态'
  union all select '分配跟单', 3, 'live:audienceRank:followup:assign', '领取或分配主播场控'
  union all select '跟单历史', 4, 'live:audienceRank:followup:history', '查看跟单时间线'
  union all select '导出跟单', 5, 'live:audienceRank:followup:export', '导出跟单资料'
) x
where @followup_menu_id is not null
  and not exists (select 1 from sys_menu m where m.perms = x.perms);

insert ignore into sys_role_menu(role_id, menu_id)
select 1, menu_id from sys_menu
where menu_id = @followup_menu_id or parent_id = @followup_menu_id;
