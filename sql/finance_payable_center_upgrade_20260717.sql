-- Finance AP center upgrade: supplier invoice matching, payment reservation and execution.
-- Back up the database before execution. This script can be executed repeatedly.
set names utf8mb4;

drop procedure if exists add_column_if_not_exists;
delimiter //
create procedure add_column_if_not_exists(
    in p_table_name varchar(64),
    in p_column_name varchar(64),
    in p_column_definition varchar(1000)
)
begin
    if not exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = p_table_name
          and column_name = p_column_name
    ) then
        set @ddl = concat('alter table `', p_table_name, '` add column `', p_column_name, '` ', p_column_definition);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end //
delimiter ;

drop procedure if exists add_index_if_not_exists;
delimiter //
create procedure add_index_if_not_exists(
    in p_table_name varchar(64),
    in p_index_name varchar(64),
    in p_index_definition varchar(1000)
)
begin
    if not exists (
        select 1 from information_schema.statistics
        where table_schema = database()
          and table_name = p_table_name
          and index_name = p_index_name
    ) then
        set @ddl = concat('alter table `', p_table_name, '` add index `', p_index_name, '` ', p_index_definition);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end //
delimiter ;

create table if not exists fin_supplier_invoice (
  invoice_id bigint not null auto_increment,
  invoice_no varchar(64) not null comment 'internal invoice number',
  supplier_invoice_no varchar(64) not null comment 'supplier invoice number',
  purchase_no varchar(64) not null,
  receipt_refs varchar(500) null,
  supplier_id bigint not null,
  supplier_name varchar(128) not null,
  invoice_date date not null,
  due_date date not null,
  period_code char(7) not null,
  amount_ex_tax decimal(14,2) not null default 0.00,
  tax_amount decimal(14,2) not null default 0.00,
  line_total_amount decimal(14,2) not null default 0.00,
  total_amount decimal(14,2) not null default 0.00,
  header_variance decimal(14,2) not null default 0.00,
  match_policy varchar(32) not null default 'three_way',
  price_tolerance_percent decimal(8,4) not null default 3.0000,
  quantity_tolerance decimal(14,4) not null default 0.0000,
  amount_tolerance decimal(14,2) not null default 1.00,
  match_status varchar(32) not null default 'pending',
  invoice_status varchar(32) not null default 'pending',
  exception_reason varchar(500) null,
  payable_no varchar(64) null,
  approve_by varchar(64) null,
  approve_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (invoice_id),
  unique key uk_fin_supplier_invoice_no (invoice_no),
  unique key uk_fin_supplier_external_invoice (supplier_id, supplier_invoice_no),
  key idx_fin_supplier_invoice_purchase (purchase_no),
  key idx_fin_supplier_invoice_status (invoice_status, match_status),
  key idx_fin_supplier_invoice_period (period_code, supplier_id)
) engine=innodb default charset=utf8mb4 comment='Finance supplier invoice and three-way match';

create table if not exists fin_supplier_invoice_item (
  item_id bigint not null auto_increment,
  invoice_id bigint not null,
  invoice_no varchar(64) not null,
  purchase_item_id bigint not null,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  sku_name varchar(128) null,
  invoice_quantity decimal(14,4) not null default 0.0000,
  invoice_unit_price decimal(14,4) not null default 0.0000,
  invoice_tax_rate decimal(8,4) not null default 0.0000,
  invoice_amount decimal(14,2) not null default 0.00,
  tax_amount decimal(14,2) not null default 0.00,
  total_amount decimal(14,2) not null default 0.00,
  order_quantity decimal(14,4) not null default 0.0000,
  order_unit_price decimal(14,4) not null default 0.0000,
  received_quantity decimal(14,4) not null default 0.0000,
  previous_invoiced_quantity decimal(14,4) not null default 0.0000,
  available_received_quantity decimal(14,4) not null default 0.0000,
  quantity_variance decimal(14,4) not null default 0.0000,
  price_variance decimal(14,4) not null default 0.0000,
  price_variance_percent decimal(12,4) not null default 0.0000,
  amount_variance decimal(14,2) not null default 0.00,
  line_match_status varchar(32) not null default 'pending',
  match_message varchar(500) null,
  primary key (item_id),
  unique key uk_fin_supplier_invoice_item (invoice_id, purchase_item_id),
  key idx_fin_supplier_invoice_item_no (invoice_no),
  key idx_fin_supplier_invoice_item_sku (sku_id)
) engine=innodb default charset=utf8mb4 comment='Finance supplier invoice match lines';

call add_column_if_not_exists('fin_payment_request', 'paid_amount',
    "decimal(14,2) not null default 0.00 comment 'executed payment amount'");
call add_column_if_not_exists('fin_payment_execute', 'bank_reference',
    "varchar(128) null comment 'bank transaction reference'");
call add_index_if_not_exists('fin_payment_request', 'idx_fin_payment_request_payable_status',
    '(payable_no, payment_status)');

insert into fin_account_subject(
    subject_code, subject_name, subject_type, parent_code, balance_direction,
    status, create_by, create_time, remark
)
select '222101', '应交增值税-进项税额', 'liability', '2221', 'debit',
       '0', 'admin', sysdate(), '供应商发票进项税额'
where not exists (select 1 from fin_account_subject where subject_code = '222101');

update fin_account_subject
set subject_name = '应交增值税-进项税额', remark = '供应商发票进项税额'
where subject_code = '222101';

set @payable_menu_id := (select menu_id from sys_menu where perms = 'finance:payable:list' limit 1);

insert into sys_menu(
    menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark
)
select '供应商发票登记', @payable_menu_id, 2, '#', '', 1, 0,
       'F', '0', '0', 'finance:payable:add', '#', 'admin', sysdate(), ''
where @payable_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:payable:add');

insert into sys_menu(
    menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark
)
select '应付审批', @payable_menu_id, 3, '#', '', 1, 0,
       'F', '0', '0', 'finance:payable:approve', '#', 'admin', sysdate(), ''
where @payable_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:payable:approve');

insert into sys_menu(
    menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark
)
select '付款申请与执行', @payable_menu_id, 4, '#', '', 1, 0,
       'F', '0', '0', 'finance:payable:payment', '#', 'admin', sysdate(), ''
where @payable_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:payable:payment');

insert into sys_menu(
    menu_name, parent_id, order_num, path, component, is_frame, is_cache,
    menu_type, visible, status, perms, icon, create_by, create_time, remark
)
select '应付核销', @payable_menu_id, 5, '#', '', 1, 0,
       'F', '0', '0', 'finance:payable:writeoff', '#', 'admin', sysdate(), ''
where @payable_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:payable:writeoff');

update sys_menu set menu_name = '供应商发票登记' where perms = 'finance:payable:add';
update sys_menu set menu_name = '应付审批' where perms = 'finance:payable:approve';
update sys_menu set menu_name = '付款申请与执行' where perms = 'finance:payable:payment';
update sys_menu set menu_name = '应付核销' where perms = 'finance:payable:writeoff';

insert into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, fm.menu_id
from sys_role_menu rm
inner join sys_menu fm on fm.parent_id = @payable_menu_id
where rm.menu_id = @payable_menu_id
  and fm.perms in ('finance:payable:add', 'finance:payable:approve',
                   'finance:payable:payment', 'finance:payable:writeoff')
  and not exists (
      select 1 from sys_role_menu existing
      where existing.role_id = rm.role_id and existing.menu_id = fm.menu_id
  );

drop procedure if exists add_index_if_not_exists;
drop procedure if exists add_column_if_not_exists;
