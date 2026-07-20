-- Purchase management upgrade: item pricing, approval audit and supplier performance.
-- Back up the database before execution. This script can be executed repeatedly.
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

call add_column_if_not_exists('erp_supplier', 'supplier_level', "varchar(8) not null default 'B' comment 'A/B/C/D'");
call add_column_if_not_exists('erp_supplier', 'lead_time_days', "int not null default 7 comment 'standard delivery lead time'");
call add_column_if_not_exists('erp_supplier', 'min_order_amount', "decimal(14,2) not null default 0.00 comment 'minimum order amount'");
call add_column_if_not_exists('erp_supplier', 'quality_score', "decimal(5,2) not null default 80.00 comment 'quality score 0-100'");
call add_column_if_not_exists('erp_supplier', 'delivery_score', "decimal(5,2) not null default 80.00 comment 'delivery score 0-100'");
call add_column_if_not_exists('erp_supplier', 'service_score', "decimal(5,2) not null default 80.00 comment 'service score 0-100'");
call add_column_if_not_exists('erp_supplier', 'preferred_flag', "char(1) not null default '0' comment 'preferred supplier flag'");

call add_column_if_not_exists('erp_purchase_order', 'tax_amount', "decimal(14,2) not null default 0.00 comment 'tax amount'");
call add_column_if_not_exists('erp_purchase_order', 'total_with_tax', "decimal(14,2) not null default 0.00 comment 'tax inclusive total'");
call add_column_if_not_exists('erp_purchase_order', 'purchase_date', "date null comment 'purchase business date'");
call add_column_if_not_exists('erp_purchase_order', 'submit_by', "varchar(64) null comment 'submit operator'");
call add_column_if_not_exists('erp_purchase_order', 'submit_time', "datetime null comment 'submit time'");
call add_column_if_not_exists('erp_purchase_order', 'approve_by', "varchar(64) null comment 'approve operator'");
call add_column_if_not_exists('erp_purchase_order', 'approve_time', "datetime null comment 'approve time'");
call add_column_if_not_exists('erp_purchase_order', 'close_by', "varchar(64) null comment 'close operator'");
call add_column_if_not_exists('erp_purchase_order', 'close_time', "datetime null comment 'close time'");

create table if not exists erp_purchase_order_item (
  item_id bigint not null auto_increment,
  purchase_id bigint not null,
  purchase_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  sku_name varchar(128) null,
  quantity int default 0,
  received_qty int default 0,
  purchase_price decimal(12,2) default 0.00,
  tax_rate decimal(6,4) default 0.0000,
  amount decimal(14,2) default 0.00,
  tax_amount decimal(14,2) not null default 0.00,
  tax_inclusive_amount decimal(14,2) not null default 0.00,
  primary key (item_id),
  key idx_erp_purchase_item_order (purchase_id),
  key idx_erp_purchase_item_sku (sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP purchase order items';

create table if not exists erp_purchase_receipt_item (
  item_id bigint not null auto_increment,
  receipt_no varchar(64) not null,
  purchase_no varchar(64) null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  quantity int default 0,
  cost_price decimal(12,2) default 0.00,
  amount decimal(14,2) default 0.00,
  batch_no varchar(64) null,
  production_date date null,
  primary key (item_id),
  key idx_erp_receipt_item_no (receipt_no),
  key idx_erp_receipt_item_sku (sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP purchase receipt items';

call add_column_if_not_exists('erp_purchase_order_item', 'tax_amount', "decimal(14,2) not null default 0.00 comment 'line tax amount'");
call add_column_if_not_exists('erp_purchase_order_item', 'tax_inclusive_amount', "decimal(14,2) not null default 0.00 comment 'line tax inclusive amount'");

update erp_purchase_order_item
set tax_amount = round(coalesce(amount, 0) * coalesce(tax_rate, 0), 2),
    tax_inclusive_amount = round(coalesce(amount, 0) * (1 + coalesce(tax_rate, 0)), 2)
where tax_inclusive_amount = 0;

update erp_purchase_order p
left join (
    select purchase_id, sum(amount) as amount, sum(tax_amount) as tax_amount,
           sum(tax_inclusive_amount) as total_with_tax
    from erp_purchase_order_item group by purchase_id
) i on i.purchase_id = p.purchase_id
set p.total_amount = coalesce(i.amount, p.total_amount, 0),
    p.tax_amount = coalesce(i.tax_amount, 0),
    p.total_with_tax = coalesce(i.total_with_tax, p.total_amount, 0)
where p.total_with_tax = 0;

update erp_purchase_order
set purchase_date = date(coalesce(create_time, sysdate()))
where purchase_date is null;

alter table erp_purchase_order modify column purchase_date date not null comment 'purchase business date';

drop procedure if exists add_column_if_not_exists;
