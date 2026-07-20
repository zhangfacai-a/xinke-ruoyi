-- Sales order hierarchy and cash ledger upgrade.
-- Back up the database before execution. This script can be executed repeatedly.
set names utf8mb4;

drop procedure if exists xk_add_column;
delimiter //
create procedure xk_add_column(
    in p_table_name varchar(64),
    in p_column_name varchar(64),
    in p_column_definition varchar(1000)
)
begin
    if exists (
        select 1 from information_schema.tables
        where table_schema = database() and table_name = p_table_name
    ) and not exists (
        select 1 from information_schema.columns
        where table_schema = database() and table_name = p_table_name and column_name = p_column_name
    ) then
        set @ddl = concat('alter table `', p_table_name, '` add column `', p_column_name, '` ', p_column_definition);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end //
delimiter ;

drop procedure if exists xk_add_index;
delimiter //
create procedure xk_add_index(
    in p_table_name varchar(64),
    in p_index_name varchar(64),
    in p_index_definition varchar(1000)
)
begin
    if exists (
        select 1 from information_schema.tables
        where table_schema = database() and table_name = p_table_name
    ) and not exists (
        select 1 from information_schema.statistics
        where table_schema = database() and table_name = p_table_name and index_name = p_index_name
    ) then
        set @ddl = concat('alter table `', p_table_name, '` add index `', p_index_name, '` ', p_index_definition);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end //
delimiter ;

-- A main order represents one checkout/payment. A sub-order is the SKU-level accounting unit.
call xk_add_column('ods_order_raw', 'main_order_no', "varchar(128) null comment 'platform main order number' after order_id");
call xk_add_column('dwd_order_item', 'main_order_no', "varchar(128) null comment 'platform main order number' after order_id");
call xk_add_column('dwd_order_item', 'sub_order_no', "varchar(128) null comment 'platform sub order number' after main_order_no");
call xk_add_index('dwd_order_item', 'idx_dwd_order_main_no', '(main_order_no)');
call xk_add_index('dwd_order_item', 'idx_dwd_order_sub_no', '(sub_order_no)');

call xk_add_column('erp_sales_order', 'main_order_no', "varchar(128) null comment 'platform main order number' after platform_order_no");
call xk_add_index('erp_sales_order', 'idx_erp_sales_main_order', '(shop_id, main_order_no)');
call xk_add_column('erp_sales_order_item', 'main_order_no', "varchar(128) null comment 'platform main order number' after sales_no");
call xk_add_column('erp_sales_order_item', 'sub_order_no', "varchar(128) null comment 'platform sub order number' after main_order_no");
call xk_add_index('erp_sales_order_item', 'idx_erp_sales_item_main', '(main_order_no)');
call xk_add_index('erp_sales_order_item', 'idx_erp_sales_item_sub', '(sub_order_no)');

call xk_add_column('erp_refund_order', 'main_order_no', "varchar(128) null comment 'platform main order number' after sales_no");
call xk_add_column('erp_refund_order', 'sub_order_no', "varchar(128) null comment 'platform sub order number' after main_order_no");
call xk_add_index('erp_refund_order', 'idx_erp_refund_sub_order', '(sub_order_no)');

call xk_add_column('fin_platform_settlement_item', 'main_order_no', "varchar(128) null comment 'platform main order number' after platform_order_no");
call xk_add_column('fin_platform_settlement_item', 'sub_order_no', "varchar(128) null comment 'platform sub order number' after main_order_no");
call xk_add_index('fin_platform_settlement_item', 'idx_fin_settlement_sub_order', '(sub_order_no)');
call xk_add_column('erp_platform_settlement_match', 'main_order_no', "varchar(128) null comment 'platform main order number' after platform_order_no");
call xk_add_column('erp_platform_settlement_match', 'sub_order_no', "varchar(128) null comment 'platform sub order number' after main_order_no");
call xk_add_column('fin_expense_bill', 'main_order_no', "varchar(128) null comment 'platform main order number' after order_no");
call xk_add_column('fin_expense_bill', 'sub_order_no', "varchar(128) null comment 'platform sub order number' after main_order_no");

drop procedure if exists xk_backfill_order_numbers;
delimiter //
create procedure xk_backfill_order_numbers()
begin
    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'ods_order_raw') then
        update ods_order_raw
        set main_order_no = cast(order_id as char)
        where main_order_no is null or trim(main_order_no) = '';
    end if;
    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'dwd_order_item') then
        update dwd_order_item
        set main_order_no = cast(order_id as char),
            sub_order_no = concat(cast(order_id as char), '-', order_item_id)
        where main_order_no is null or trim(main_order_no) = ''
           or sub_order_no is null or trim(sub_order_no) = '';
    end if;
    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'erp_sales_order') then
        update erp_sales_order
        set main_order_no = coalesce(nullif(platform_order_no, ''), sales_no)
        where main_order_no is null or trim(main_order_no) = '';
    end if;
    if exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'erp_sales_order_item')
       and exists (select 1 from information_schema.tables where table_schema = database() and table_name = 'erp_sales_order') then
        update erp_sales_order_item i
        inner join erp_sales_order o on o.sales_id = i.sales_id
        set i.main_order_no = o.main_order_no,
            i.sub_order_no = concat(o.main_order_no, '-', i.item_id)
        where i.main_order_no is null or trim(i.main_order_no) = ''
           or i.sub_order_no is null or trim(i.sub_order_no) = '';
    end if;
end //
delimiter ;
call xk_backfill_order_numbers();
drop procedure if exists xk_backfill_order_numbers;

-- Cash ledger fields: posting, settlement balance, external references and accounting linkage.
call xk_add_column('fin_cash_flow', 'flow_category', "varchar(32) not null default 'other' comment 'business cash category' after flow_type");
call xk_add_column('fin_cash_flow', 'counterparty_type', "varchar(32) not null default 'other' comment 'counterparty type' after platform_account_id");
call xk_add_column('fin_cash_flow', 'currency', "char(3) not null default 'CNY' comment 'currency' after source_no");
call xk_add_column('fin_cash_flow', 'external_reference', "varchar(128) null comment 'bank or platform transaction reference' after currency");
call xk_add_column('fin_cash_flow', 'net_amount', "decimal(14,2) not null default 0.00 comment 'actual account movement' after fee_amount");
call xk_add_column('fin_cash_flow', 'settled_amount', "decimal(14,2) not null default 0.00 comment 'allocated amount' after net_amount");
call xk_add_column('fin_cash_flow', 'entry_status', "varchar(16) not null default 'posted' comment 'draft posted reversed' after match_status");
call xk_add_column('fin_cash_flow', 'voucher_no', "varchar(64) null comment 'linked accounting voucher' after entry_status");
call xk_add_column('fin_cash_flow', 'reversal_flow_no', "varchar(64) null comment 'reversal cash flow number' after voucher_no");
call xk_add_column('fin_cash_flow', 'posted_by', "varchar(64) null after reversal_flow_no");
call xk_add_column('fin_cash_flow', 'posted_time', "datetime null after posted_by");
call xk_add_column('fin_cash_flow', 'reversed_by', "varchar(64) null after posted_time");
call xk_add_column('fin_cash_flow', 'reversed_time', "datetime null after reversed_by");
call xk_add_index('fin_cash_flow', 'idx_fin_cash_flow_entry_status', '(entry_status, business_date)');
call xk_add_index('fin_cash_flow', 'idx_fin_cash_flow_external_ref', '(external_reference)');

update fin_cash_flow
set net_amount = case when flow_type = 'in' then greatest(amount - coalesce(fee_amount, 0), 0)
                      else amount + coalesce(fee_amount, 0) end,
    settled_amount = case when match_status = 'matched' then amount else 0 end,
    entry_status = coalesce(nullif(entry_status, ''), 'posted'),
    posted_by = coalesce(posted_by, create_by),
    posted_time = coalesce(posted_time, create_time, flow_time)
where net_amount = 0 or posted_time is null;

set @cash_menu_id := (select menu_id from sys_menu where perms = 'finance:cashflow:list' limit 1);
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '资金流水入账', @cash_menu_id, 2, '#', '', 1, 0,
       'F', '0', '0', 'finance:cashflow:post', '#', 'admin', sysdate(), ''
where @cash_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:cashflow:post');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache,
                     menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '资金流水冲销', @cash_menu_id, 3, '#', '', 1, 0,
       'F', '0', '0', 'finance:cashflow:reverse', '#', 'admin', sysdate(), ''
where @cash_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'finance:cashflow:reverse');

insert into sys_role_menu(role_id, menu_id)
select distinct rm.role_id, fm.menu_id
from sys_role_menu rm
inner join sys_menu fm on fm.parent_id = @cash_menu_id
where rm.menu_id = @cash_menu_id
  and fm.perms in ('finance:cashflow:post', 'finance:cashflow:reverse')
  and not exists (
      select 1 from sys_role_menu existing
      where existing.role_id = rm.role_id and existing.menu_id = fm.menu_id
  );

drop procedure if exists xk_add_index;
drop procedure if exists xk_add_column;
