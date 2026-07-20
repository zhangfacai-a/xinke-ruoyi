-- Sales fulfillment center: main orders, sub orders, shipments and after-sales.
-- This migration is idempotent and preserves existing shipment/after-sale data.

delimiter $$

drop procedure if exists xk_sf_add_column$$
create procedure xk_sf_add_column(
    in p_table varchar(64),
    in p_column varchar(64),
    in p_definition text
)
begin
    if exists (
        select 1 from information_schema.tables
        where table_schema = database() and table_name = p_table
    ) and not exists (
        select 1 from information_schema.columns
        where table_schema = database() and table_name = p_table and column_name = p_column
    ) then
        set @ddl = concat('alter table `', p_table, '` add column `', p_column, '` ', p_definition);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end$$

drop procedure if exists xk_sf_add_index$$
create procedure xk_sf_add_index(
    in p_table varchar(64),
    in p_index varchar(64),
    in p_columns text
)
begin
    if exists (
        select 1 from information_schema.tables
        where table_schema = database() and table_name = p_table
    ) and not exists (
        select 1 from information_schema.statistics
        where table_schema = database() and table_name = p_table and index_name = p_index
    ) then
        set @ddl = concat('alter table `', p_table, '` add index `', p_index, '` ', p_columns);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end$$

drop procedure if exists xk_sf_add_unique$$
create procedure xk_sf_add_unique(
    in p_table varchar(64),
    in p_index varchar(64),
    in p_columns text
)
begin
    if exists (
        select 1 from information_schema.tables
        where table_schema = database() and table_name = p_table
    ) and not exists (
        select 1 from information_schema.statistics
        where table_schema = database() and table_name = p_table and index_name = p_index
    ) then
        set @ddl = concat('alter table `', p_table, '` add unique key `', p_index, '` ', p_columns);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end$$

delimiter ;

create table if not exists erp_sales_order (
    sales_id bigint not null auto_increment,
    sales_no varchar(64) not null,
    platform_order_no varchar(128) null,
    main_order_no varchar(128) not null,
    shop_id bigint not null default 0,
    shop_name varchar(128) null,
    customer_id bigint null,
    customer_name varchar(128) null,
    source_type varchar(32) not null default 'dwd_sync',
    order_status varchar(32) not null default 'paid',
    fulfillment_status varchar(32) not null default 'pending',
    total_quantity int not null default 0,
    shipped_quantity int not null default 0,
    refunded_quantity int not null default 0,
    pay_amount decimal(14,2) not null default 0.00,
    refund_amount decimal(14,2) not null default 0.00,
    pay_time datetime null,
    data_sync_time datetime null,
    version int not null default 0,
    create_by varchar(64) default '',
    create_time datetime null,
    update_by varchar(64) default '',
    update_time datetime null,
    remark varchar(500) null,
    primary key (sales_id),
    unique key uk_erp_sales_no (sales_no),
    unique key uk_erp_sales_shop_main (shop_id, main_order_no),
    key idx_erp_sales_status_time (fulfillment_status, pay_time),
    key idx_erp_sales_shop_time (shop_id, pay_time)
) engine=innodb default charset=utf8mb4 comment='ERP sales order header';

create table if not exists erp_sales_order_item (
    item_id bigint not null auto_increment,
    sales_id bigint not null,
    sales_no varchar(64) not null,
    dwd_order_item_id bigint null,
    main_order_no varchar(128) not null,
    sub_order_no varchar(128) not null,
    sku_id bigint not null,
    sku_code varchar(64) null,
    sku_name varchar(128) null,
    product_name varchar(128) null,
    quantity int not null default 0,
    shipped_quantity int not null default 0,
    refunded_quantity int not null default 0,
    pay_amount decimal(14,2) not null default 0.00,
    product_cost decimal(14,2) not null default 0.00,
    item_status varchar(32) not null default 'paid',
    version int not null default 0,
    create_time datetime null,
    update_time datetime null,
    primary key (item_id),
    unique key uk_erp_sales_item_dwd (dwd_order_item_id),
    unique key uk_erp_sales_item_sub (sales_id, sub_order_no),
    key idx_erp_sales_item_main (main_order_no),
    key idx_erp_sales_item_sku (sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP sales order item';

create table if not exists erp_sales_shipment (
    shipment_id bigint not null auto_increment,
    shipment_no varchar(64) not null,
    sales_no varchar(64) not null,
    warehouse_id bigint null,
    logistics_company varchar(128) null,
    logistics_no varchar(128) null,
    shipped_time datetime null,
    shipment_status varchar(32) default 'draft',
    create_by varchar(64) default '',
    create_time datetime null,
    update_by varchar(64) default '',
    update_time datetime null,
    remark varchar(500) null,
    primary key (shipment_id),
    unique key uk_erp_sales_shipment_no (shipment_no),
    key idx_erp_sales_shipment_sales (sales_no)
) engine=innodb default charset=utf8mb4 comment='ERP sales shipment';

call xk_sf_add_column('erp_sales_shipment', 'sales_id', 'bigint null after shipment_no');
call xk_sf_add_column('erp_sales_shipment', 'main_order_no', 'varchar(128) null after sales_no');
call xk_sf_add_column('erp_sales_shipment', 'package_no', 'varchar(64) null after main_order_no');
call xk_sf_add_column('erp_sales_shipment', 'warehouse_name', 'varchar(128) null after warehouse_id');
call xk_sf_add_column('erp_sales_shipment', 'idempotency_key', 'varchar(64) null after logistics_no');
call xk_sf_add_column('erp_sales_shipment', 'shipped_by', 'varchar(64) null after shipped_time');
call xk_sf_add_column('erp_sales_shipment', 'signed_time', 'datetime null after shipped_by');
call xk_sf_add_column('erp_sales_shipment', 'cancel_by', 'varchar(64) null after signed_time');
call xk_sf_add_column('erp_sales_shipment', 'cancel_time', 'datetime null after cancel_by');
call xk_sf_add_column('erp_sales_shipment', 'cancel_reason', 'varchar(255) null after cancel_time');
call xk_sf_add_column('erp_sales_shipment', 'version', 'int not null default 0 after cancel_reason');
call xk_sf_add_index('erp_sales_shipment', 'idx_erp_shipment_sales_id', '(sales_id, shipment_status)');
call xk_sf_add_unique('erp_sales_shipment', 'uk_erp_shipment_idempotency', '(idempotency_key)');

create table if not exists erp_sales_shipment_item (
    shipment_item_id bigint not null auto_increment,
    shipment_id bigint not null,
    shipment_no varchar(64) not null,
    sales_id bigint not null,
    sales_item_id bigint not null,
    sales_no varchar(64) not null,
    main_order_no varchar(128) not null,
    sub_order_no varchar(128) not null,
    sku_id bigint not null,
    sku_code varchar(64) null,
    sku_name varchar(128) null,
    quantity int not null default 0,
    create_time datetime null,
    primary key (shipment_item_id),
    unique key uk_erp_shipment_sales_item (shipment_id, sales_item_id),
    key idx_erp_shipment_item_no (shipment_no),
    key idx_erp_shipment_item_sales (sales_id, sales_item_id)
) engine=innodb default charset=utf8mb4 comment='ERP sales shipment item';

create table if not exists erp_after_sale_order (
    after_sale_id bigint not null auto_increment,
    after_sale_no varchar(64) not null,
    sales_no varchar(64) null,
    refund_no varchar(64) null,
    after_sale_type varchar(32) default 'refund_only',
    after_sale_status varchar(32) default 'pending',
    customer_name varchar(128) null,
    amount decimal(14,2) default 0.00,
    reason varchar(255) null,
    create_by varchar(64) default '',
    create_time datetime null,
    update_by varchar(64) default '',
    update_time datetime null,
    remark varchar(500) null,
    primary key (after_sale_id),
    unique key uk_erp_after_sale_no (after_sale_no),
    key idx_erp_after_sale_sales (sales_no)
) engine=innodb default charset=utf8mb4 comment='ERP after sale order';

call xk_sf_add_column('erp_after_sale_order', 'sales_id', 'bigint null after after_sale_no');
call xk_sf_add_column('erp_after_sale_order', 'sales_item_id', 'bigint null after sales_id');
call xk_sf_add_column('erp_after_sale_order', 'main_order_no', 'varchar(128) null after sales_no');
call xk_sf_add_column('erp_after_sale_order', 'sub_order_no', 'varchar(128) null after main_order_no');
call xk_sf_add_column('erp_after_sale_order', 'sku_id', 'bigint null after sub_order_no');
call xk_sf_add_column('erp_after_sale_order', 'sku_code', 'varchar(64) null after sku_id');
call xk_sf_add_column('erp_after_sale_order', 'sku_name', 'varchar(128) null after sku_code');
call xk_sf_add_column('erp_after_sale_order', 'quantity', 'int not null default 1 after sku_name');
call xk_sf_add_column('erp_after_sale_order', 'refund_amount', 'decimal(14,2) not null default 0.00 after amount');
call xk_sf_add_column('erp_after_sale_order', 'return_warehouse_id', 'bigint null after refund_amount');
call xk_sf_add_column('erp_after_sale_order', 'return_warehouse_name', 'varchar(128) null after return_warehouse_id');
call xk_sf_add_column('erp_after_sale_order', 'idempotency_key', 'varchar(64) null after return_warehouse_name');
call xk_sf_add_column('erp_after_sale_order', 'approve_by', 'varchar(64) null after reason');
call xk_sf_add_column('erp_after_sale_order', 'approve_time', 'datetime null after approve_by');
call xk_sf_add_column('erp_after_sale_order', 'complete_by', 'varchar(64) null after approve_time');
call xk_sf_add_column('erp_after_sale_order', 'complete_time', 'datetime null after complete_by');
call xk_sf_add_column('erp_after_sale_order', 'version', 'int not null default 0 after complete_time');
call xk_sf_add_index('erp_after_sale_order', 'idx_erp_after_sale_item', '(sales_item_id, after_sale_status)');
call xk_sf_add_unique('erp_after_sale_order', 'uk_erp_after_sale_idempotency', '(idempotency_key)');

create table if not exists erp_sales_fulfillment_log (
    log_id bigint not null auto_increment,
    sales_id bigint null,
    sales_item_id bigint null,
    biz_type varchar(32) not null,
    biz_no varchar(64) not null,
    action_type varchar(32) not null,
    from_status varchar(32) null,
    to_status varchar(32) null,
    operator_name varchar(64) null,
    action_time datetime not null,
    detail varchar(1000) null,
    primary key (log_id),
    key idx_erp_fulfillment_log_sales (sales_id, action_time),
    key idx_erp_fulfillment_log_biz (biz_type, biz_no)
) engine=innodb default charset=utf8mb4 comment='Sales fulfillment action log';

-- Existing legacy rows keep their original business number as the visible main order number.
update erp_sales_shipment
set main_order_no = sales_no
where (main_order_no is null or main_order_no = '') and sales_no is not null;

update erp_after_sale_order
set main_order_no = sales_no,
    refund_amount = ifnull(nullif(refund_amount, 0), amount)
where sales_no is not null;

-- Backfill operational order headers from the analytics detail table.
insert into erp_sales_order(
    sales_no, platform_order_no, main_order_no, shop_id, shop_name,
    source_type, order_status, fulfillment_status, total_quantity,
    pay_amount, pay_time, data_sync_time, create_by, create_time, update_time
)
select concat('SO-', substr(sha2(concat(x.shop_id, '|', x.main_order_no), 256), 1, 24)),
       x.main_order_no, x.main_order_no,
       x.shop_id, x.shop_name, 'dwd_sync', 'paid', 'pending',
       x.total_quantity, x.pay_amount, x.pay_time,
       sysdate(), 'migration', sysdate(), sysdate()
from (
    select ifnull(d.shop_id, 0) shop_id,
           coalesce(nullif(d.main_order_no, ''), cast(d.order_id as char)) main_order_no,
           max(d.shop_name) shop_name,
           sum(ifnull(d.quantity, 0)) total_quantity,
           sum(ifnull(d.pay_amount, 0)) pay_amount,
           max(d.pay_time) pay_time
    from dwd_order_item d
    group by ifnull(d.shop_id, 0),
             coalesce(nullif(d.main_order_no, ''), cast(d.order_id as char))
) x
on duplicate key update
    shop_name = values(shop_name),
    total_quantity = values(total_quantity),
    pay_amount = values(pay_amount),
    pay_time = values(pay_time),
    data_sync_time = sysdate(),
    update_time = sysdate();

insert into erp_sales_order_item(
    sales_id, sales_no, dwd_order_item_id, main_order_no, sub_order_no,
    sku_id, sku_code, sku_name, product_name, quantity,
    pay_amount, product_cost, item_status, create_time, update_time
)
select o.sales_id, o.sales_no, d.order_item_id, o.main_order_no,
       coalesce(nullif(d.sub_order_no, ''), concat(o.main_order_no, '-', d.order_item_id)),
       d.sku_id, s.sku_code, d.sku_name, d.product_name, ifnull(d.quantity, 0),
       ifnull(d.pay_amount, 0), ifnull(d.product_cost, 0), 'paid', sysdate(), sysdate()
from dwd_order_item d
inner join erp_sales_order o
        on o.shop_id = ifnull(d.shop_id, 0)
       and o.main_order_no = coalesce(nullif(d.main_order_no, ''), cast(d.order_id as char))
left join erp_product_sku s on s.sku_id = d.sku_id
on duplicate key update
    sku_code = values(sku_code),
    sku_name = values(sku_name),
    product_name = values(product_name),
    quantity = values(quantity),
    pay_amount = values(pay_amount),
    product_cost = values(product_cost),
    update_time = sysdate();

update erp_sales_order o
left join (
    select sales_id, sum(quantity) total_qty, sum(shipped_quantity) shipped_qty,
           sum(refunded_quantity) refunded_qty
    from erp_sales_order_item
    group by sales_id
) x on x.sales_id = o.sales_id
set o.total_quantity = ifnull(x.total_qty, 0),
    o.shipped_quantity = ifnull(x.shipped_qty, 0),
    o.refunded_quantity = ifnull(x.refunded_qty, 0),
    o.update_time = sysdate();

-- Button permissions under the existing order menu.
set @order_menu_id = (select menu_id from sys_menu where perms = 'erp:order:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon,
                     create_by, create_time, remark)
select '同步履约订单', @order_menu_id, 1, '#', '', null, '', 1, 0, 'F', '0', '0',
       'erp:order:sync', '#', 'admin', sysdate(), 'Sync DWD orders into fulfillment center'
where @order_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'erp:order:sync');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon,
                     create_by, create_time, remark)
select '订单发货', @order_menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0',
       'erp:order:shipment', '#', 'admin', sysdate(), 'Create and confirm shipments'
where @order_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'erp:order:shipment');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name,
                     is_frame, is_cache, menu_type, visible, status, perms, icon,
                     create_by, create_time, remark)
select '订单售后', @order_menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0',
       'erp:order:after-sale', '#', 'admin', sysdate(), 'Create and process after-sales'
where @order_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'erp:order:after-sale');

insert ignore into sys_role_menu(role_id, menu_id)
select r.role_id, m.menu_id
from sys_role r
cross join sys_menu m
where r.role_key = 'admin'
  and m.perms in ('erp:order:sync', 'erp:order:shipment', 'erp:order:after-sale');

drop procedure if exists xk_sf_add_column;
drop procedure if exists xk_sf_add_index;
drop procedure if exists xk_sf_add_unique;
