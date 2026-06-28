set names utf8mb4;

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '结算新增', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:settlement:add', '#', 'admin', sysdate(), '平台结算新增按钮'
from sys_menu
where menu_name = '平台结算' and perms = 'finance:settlement:list'
  and not exists (select 1 from sys_menu where perms = 'finance:settlement:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '结算确认', menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:settlement:audit', '#', 'admin', sysdate(), '平台结算确认按钮'
from sys_menu
where menu_name = '平台结算' and perms = 'finance:settlement:list'
  and not exists (select 1 from sys_menu where perms = 'finance:settlement:audit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '流水新增', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:cashflow:add', '#', 'admin', sysdate(), '收付款流水新增按钮'
from sys_menu
where menu_name = '收付款流水' and perms = 'finance:cashflow:list'
  and not exists (select 1 from sys_menu where perms = 'finance:cashflow:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '应收核销', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:receivable:writeoff', '#', 'admin', sysdate(), '应收核销按钮'
from sys_menu
where menu_name = '应收账款' and perms = 'finance:receivable:list'
  and not exists (select 1 from sys_menu where perms = 'finance:receivable:writeoff');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '应付核销', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:payable:writeoff', '#', 'admin', sysdate(), '应付核销按钮'
from sys_menu
where menu_name = '应付账款' and perms = 'finance:payable:list'
  and not exists (select 1 from sys_menu where perms = 'finance:payable:writeoff');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '费用新增', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:expense:add', '#', 'admin', sysdate(), '费用新增按钮'
from sys_menu
where menu_name = '费用管理' and perms = 'finance:expense:list'
  and not exists (select 1 from sys_menu where perms = 'finance:expense:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '费用审核', menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:expense:audit', '#', 'admin', sysdate(), '费用审核按钮'
from sys_menu
where menu_name = '费用管理' and perms = 'finance:expense:list'
  and not exists (select 1 from sys_menu where perms = 'finance:expense:audit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '凭证生成', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:voucher:add', '#', 'admin', sysdate(), '源单生成凭证按钮'
from sys_menu
where menu_name = '凭证管理' and perms = 'finance:voucher:list'
  and not exists (select 1 from sys_menu where perms = 'finance:voucher:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '凭证过账', menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:voucher:audit', '#', 'admin', sysdate(), '凭证过账按钮'
from sys_menu
where menu_name = '凭证管理' and perms = 'finance:voucher:list'
  and not exists (select 1 from sys_menu where perms = 'finance:voucher:audit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '运行对账', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:reconcile:run', '#', 'admin', sysdate(), '运行对账按钮'
from sys_menu
where menu_name = '对账差异' and perms = 'finance:reconcile:list'
  and not exists (select 1 from sys_menu where perms = 'finance:reconcile:run');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '月结检查', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:close:check', '#', 'admin', sysdate(), '月结检查按钮'
from sys_menu
where menu_name = '月结管理' and perms = 'finance:close:list'
  and not exists (select 1 from sys_menu where perms = 'finance:close:check');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '月结关账', menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:close:close', '#', 'admin', sysdate(), '月结关账按钮'
from sys_menu
where menu_name = '月结管理' and perms = 'finance:close:list'
  and not exists (select 1 from sys_menu where perms = 'finance:close:close');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '月结反关账', menu_id, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'finance:close:reopen', '#', 'admin', sysdate(), '月结反关账按钮'
from sys_menu
where menu_name = '月结管理' and perms = 'finance:close:list'
  and not exists (select 1 from sys_menu where perms = 'finance:close:reopen');
