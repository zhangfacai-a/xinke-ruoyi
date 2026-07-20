set names utf8mb4;

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '财务管理', 0, 10, 'finance', null, 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), 'Finance'
where not exists (select 1 from sys_menu where path = 'finance' and parent_id = 0);

update sys_menu set menu_name = '财务管理', icon = 'money' where path = 'finance' and parent_id = 0;

set @finance_parent_id := (select menu_id from sys_menu where path = 'finance' and parent_id = 0 limit 1);

update sys_menu set menu_name = '财务看板', component = 'finance/profit' where perms = 'finance:profit:daily';

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '财务配置', @finance_parent_id, 2, 'config', 'finance/config', 1, 0, 'C', '0', '0', 'finance:config:list', 'tool', 'admin', sysdate(), 'Finance config'
where not exists (select 1 from sys_menu where perms = 'finance:config:list');
update sys_menu set route_name = 'FinanceConfig' where perms = 'finance:config:list';

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '平台结算', @finance_parent_id, 3, 'settlement', 'finance/settlement', 1, 0, 'C', '0', '0', 'finance:settlement:list', 'money', 'admin', sysdate(), 'Platform settlement'
where not exists (select 1 from sys_menu where perms = 'finance:settlement:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '应收账款', @finance_parent_id, 4, 'receivable', 'finance/receivable', 1, 0, 'C', '0', '0', 'finance:receivable:list', 'wallet', 'admin', sysdate(), 'Receivable'
where not exists (select 1 from sys_menu where perms = 'finance:receivable:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '应付账款', @finance_parent_id, 5, 'payable', 'finance/payable', 1, 0, 'C', '0', '0', 'finance:payable:list', 'shopping', 'admin', sysdate(), 'Payable'
where not exists (select 1 from sys_menu where perms = 'finance:payable:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '收付款流水', @finance_parent_id, 6, 'cashflow', 'finance/cashflow', 1, 0, 'C', '0', '0', 'finance:cashflow:list', 'log', 'admin', sysdate(), 'Cash flow'
where not exists (select 1 from sys_menu where perms = 'finance:cashflow:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '费用管理', @finance_parent_id, 7, 'expense', 'finance/expense', 1, 0, 'C', '0', '0', 'finance:expense:list', 'form', 'admin', sysdate(), 'Expense'
where not exists (select 1 from sys_menu where perms = 'finance:expense:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '对账差异', @finance_parent_id, 8, 'reconcile', 'finance/reconcile', 1, 0, 'C', '0', '0', 'finance:reconcile:list', 'eye-open', 'admin', sysdate(), 'Reconcile'
where not exists (select 1 from sys_menu where perms = 'finance:reconcile:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '凭证管理', @finance_parent_id, 9, 'voucher', 'finance/voucher', 1, 0, 'C', '0', '0', 'finance:voucher:list', 'documentation', 'admin', sysdate(), 'Voucher'
where not exists (select 1 from sys_menu where perms = 'finance:voucher:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '月结管理', @finance_parent_id, 10, 'close', 'finance/close', 1, 0, 'C', '0', '0', 'finance:close:list', 'lock', 'admin', sysdate(), 'Period close'
where not exists (select 1 from sys_menu where perms = 'finance:close:list');

set @config_menu_id := (select menu_id from sys_menu where perms = 'finance:config:list' limit 1);
set @settlement_menu_id := (select menu_id from sys_menu where perms = 'finance:settlement:list' limit 1);
set @receivable_menu_id := (select menu_id from sys_menu where perms = 'finance:receivable:list' limit 1);
set @payable_menu_id := (select menu_id from sys_menu where perms = 'finance:payable:list' limit 1);
set @cashflow_menu_id := (select menu_id from sys_menu where perms = 'finance:cashflow:list' limit 1);
set @expense_menu_id := (select menu_id from sys_menu where perms = 'finance:expense:list' limit 1);
set @reconcile_menu_id := (select menu_id from sys_menu where perms = 'finance:reconcile:list' limit 1);
set @voucher_menu_id := (select menu_id from sys_menu where perms = 'finance:voucher:list' limit 1);
set @close_menu_id := (select menu_id from sys_menu where perms = 'finance:close:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '配置查询', @config_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:config:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:config:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '结算查询', @settlement_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:settlement:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:settlement:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '应收查询', @receivable_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:receivable:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:receivable:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '应付查询', @payable_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:payable:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:payable:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '流水查询', @cashflow_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:cashflow:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:cashflow:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '费用查询', @expense_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:expense:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:expense:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '对账查询', @reconcile_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:reconcile:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:reconcile:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '凭证查询', @voucher_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:voucher:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:voucher:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '月结查询', @close_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'finance:close:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:close:query');
