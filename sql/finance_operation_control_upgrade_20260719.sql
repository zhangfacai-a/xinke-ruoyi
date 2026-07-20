set names utf8mb4;

-- 统一历史平台结算差异状态编码。
update fin_platform_settlement
set reconcile_status = 'difference'
where reconcile_status = 'diff';

-- 增加“处理对账差异”按钮权限，并授权给已拥有运行对账权限的角色。
set @reconcile_menu_id := (select menu_id from sys_menu where perms = 'finance:reconcile:list' limit 1);

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select
  '处理对账差异', @reconcile_menu_id, 3, '#', '', null, '',
  1, 0, 'F', '0', '0', 'finance:reconcile:handle', '#',
  'admin', sysdate(), '填写处理结论并关闭对账差异'
where @reconcile_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:reconcile:handle');

set @handle_menu_id := (select menu_id from sys_menu where perms = 'finance:reconcile:handle' limit 1);

insert ignore into sys_role_menu(role_id, menu_id)
select distinct role_id, @handle_menu_id
from sys_role_menu
where menu_id in (
  select menu_id from sys_menu where perms in ('finance:reconcile:list', 'finance:reconcile:run')
)
and @handle_menu_id is not null;

-- 草稿修订/作废权限。只有草稿可以修改；审核、确认或入账后必须走冲销或更正单。
set @settlement_menu_id := (select menu_id from sys_menu where perms = 'finance:settlement:list' limit 1);
set @expense_menu_id := (select menu_id from sys_menu where perms = 'finance:expense:list' limit 1);
set @cashflow_menu_id := (select menu_id from sys_menu where perms = 'finance:cashflow:list' limit 1);

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select '修改/作废结算草稿', @settlement_menu_id, 4, '#', '', null, '',
       1, 0, 'F', '0', '0', 'finance:settlement:edit', '#',
       'admin', sysdate(), '修改或作废未确认的平台结算单'
where @settlement_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:settlement:edit');

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select '修改/作废费用草稿', @expense_menu_id, 4, '#', '', null, '',
       1, 0, 'F', '0', '0', 'finance:expense:edit', '#',
       'admin', sysdate(), '修改或作废未审核的费用单'
where @expense_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:expense:edit');

insert into sys_menu(
  menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon,
  create_by, create_time, remark
)
select '作废资金流水草稿', @cashflow_menu_id, 4, '#', '', null, '',
       1, 0, 'F', '0', '0', 'finance:cashflow:edit', '#',
       'admin', sysdate(), '作废尚未入账的资金流水草稿'
where @cashflow_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:cashflow:edit');

set @settlement_edit_id := (select menu_id from sys_menu where perms = 'finance:settlement:edit' limit 1);
set @expense_edit_id := (select menu_id from sys_menu where perms = 'finance:expense:edit' limit 1);
set @cashflow_edit_id := (select menu_id from sys_menu where perms = 'finance:cashflow:edit' limit 1);

insert ignore into sys_role_menu(role_id, menu_id)
select distinct role_id, @settlement_edit_id
from sys_role_menu
where menu_id in (select menu_id from sys_menu where perms in ('finance:settlement:add', 'finance:settlement:audit'))
  and @settlement_edit_id is not null;

insert ignore into sys_role_menu(role_id, menu_id)
select distinct role_id, @expense_edit_id
from sys_role_menu
where menu_id in (select menu_id from sys_menu where perms in ('finance:expense:add', 'finance:expense:audit'))
  and @expense_edit_id is not null;

insert ignore into sys_role_menu(role_id, menu_id)
select distinct role_id, @cashflow_edit_id
from sys_role_menu
where menu_id in (select menu_id from sys_menu where perms in ('finance:cashflow:add', 'finance:cashflow:post'))
  and @cashflow_edit_id is not null;
