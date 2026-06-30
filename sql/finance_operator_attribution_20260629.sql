-- Finance operator attribution extension.
-- Shop is only the business context. Operator + product/SKU + traffic channel is the profit sharing dimension.

create table if not exists erp_operator_attribution_rule (
  rule_id bigint not null auto_increment,
  rule_code varchar(64) not null,
  rule_name varchar(128) not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  product_id bigint null,
  product_name varchar(128) null,
  sku_id bigint null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  channel_code varchar(64) null,
  channel_name varchar(128) null,
  operator_id bigint not null,
  operator_name varchar(64) not null,
  share_rate decimal(8,4) default 1.0000,
  priority int default 100,
  effective_start date null,
  effective_end date null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (rule_id),
  unique key uk_operator_attr_rule_code (rule_code),
  key idx_operator_attr_rule_match (shop_id, sku_id, product_id, channel_code, status, priority),
  key idx_operator_attr_rule_operator (operator_id)
) engine=innodb default charset=utf8mb4 comment='ERP operator attribution rule';

create table if not exists fin_order_profit_attribution (
  attribution_id bigint not null auto_increment,
  sales_no varchar(64) not null,
  order_id bigint null,
  order_item_id bigint null,
  shop_id bigint null,
  shop_name varchar(128) null,
  product_id bigint null,
  product_name varchar(128) null,
  sku_id bigint null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  channel_code varchar(64) null,
  channel_name varchar(128) null,
  operator_id bigint not null,
  operator_name varchar(64) not null,
  attribution_rule_id bigint null,
  attribution_rule_code varchar(64) null,
  share_rate decimal(8,4) default 1.0000,
  pay_amount decimal(14,2) default 0.00,
  net_amount decimal(14,2) default 0.00,
  product_cost decimal(14,2) default 0.00,
  platform_fee decimal(14,2) default 0.00,
  promotion_fee decimal(14,2) default 0.00,
  freight_fee decimal(14,2) default 0.00,
  after_sale_cost decimal(14,2) default 0.00,
  profit_amount decimal(14,2) default 0.00,
  share_profit_amount decimal(14,2) default 0.00,
  settlement_amount decimal(14,2) default 0.00,
  profit_date date null,
  period_code varchar(20) null,
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  remark varchar(500) null,
  primary key (attribution_id),
  key idx_fin_order_profit_attr_sales (sales_no),
  key idx_fin_order_profit_attr_operator (profit_date, operator_id),
  key idx_fin_order_profit_attr_channel (profit_date, channel_code),
  key idx_fin_order_profit_attr_shop (profit_date, shop_id)
) engine=innodb default charset=utf8mb4 comment='Finance order profit operator attribution';

create table if not exists fin_operator_profit_day (
  summary_id bigint not null auto_increment,
  dt date not null,
  period_code varchar(20) null,
  shop_id bigint null,
  shop_name varchar(128) null,
  operator_id bigint not null,
  operator_name varchar(64) not null,
  channel_code varchar(64) null,
  channel_name varchar(128) null,
  order_count int default 0,
  gmv decimal(14,2) default 0.00,
  net_amount decimal(14,2) default 0.00,
  product_cost decimal(14,2) default 0.00,
  platform_fee decimal(14,2) default 0.00,
  ad_cost decimal(14,2) default 0.00,
  freight_fee decimal(14,2) default 0.00,
  after_sale_cost decimal(14,2) default 0.00,
  profit_amount decimal(14,2) default 0.00,
  share_profit_amount decimal(14,2) default 0.00,
  settlement_amount decimal(14,2) default 0.00,
  create_time datetime default current_timestamp,
  update_time datetime null,
  primary key (summary_id),
  unique key uk_fin_operator_profit_day (dt, shop_id, operator_id, channel_code),
  key idx_fin_operator_profit_period (period_code, operator_id),
  key idx_fin_operator_profit_shop (dt, shop_id)
) engine=innodb default charset=utf8mb4 comment='Finance operator day profit';

create table if not exists fin_operator_profit_settlement (
  settlement_id bigint not null auto_increment,
  settlement_no varchar(64) not null,
  period_code varchar(20) not null,
  operator_id bigint not null,
  operator_name varchar(64) not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  channel_code varchar(64) null,
  channel_name varchar(128) null,
  profit_amount decimal(14,2) default 0.00,
  share_profit_amount decimal(14,2) default 0.00,
  paid_amount decimal(14,2) default 0.00,
  remain_amount decimal(14,2) default 0.00,
  settlement_status varchar(32) default 'draft',
  approve_by varchar(64) default '',
  approve_time datetime null,
  create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (settlement_id),
  unique key uk_fin_operator_settlement_no (settlement_no),
  key idx_fin_operator_settlement_period (period_code, operator_id, settlement_status)
) engine=innodb default charset=utf8mb4 comment='Finance operator profit settlement';

drop procedure if exists add_column_if_missing;
delimiter $$
create procedure add_column_if_missing(
  in p_table_name varchar(64),
  in p_column_name varchar(64),
  in p_column_definition varchar(500)
)
begin
  if not exists (
    select 1
    from information_schema.columns
    where table_schema = database()
      and table_name = p_table_name
      and column_name = p_column_name
  ) then
    set @ddl = concat('alter table ', p_table_name, ' add column ', p_column_name, ' ', p_column_definition);
    prepare stmt from @ddl;
    execute stmt;
    deallocate prepare stmt;
  end if;
end$$
delimiter ;

call add_column_if_missing('dwd_order_item', 'channel_code', "varchar(64) null comment 'Traffic channel code'");
call add_column_if_missing('dwd_order_item', 'channel_name', "varchar(128) null comment 'Traffic channel name'");
call add_column_if_missing('dwd_order_item', 'operator_id', "bigint null comment 'Attributed operator id'");
call add_column_if_missing('dwd_order_item', 'operator_name', "varchar(64) null comment 'Attributed operator name'");
call add_column_if_missing('dwd_order_item', 'attribution_rule_id', "bigint null comment 'Attribution rule id'");

call add_column_if_missing('ods_order_raw', 'channel_code', "varchar(64) null comment 'Traffic channel code'");
call add_column_if_missing('ods_order_raw', 'channel_name', "varchar(128) null comment 'Traffic channel name'");

call add_column_if_missing('erp_order_profit', 'channel_code', "varchar(64) null comment 'Traffic channel code'");
call add_column_if_missing('erp_order_profit', 'channel_name', "varchar(128) null comment 'Traffic channel name'");
call add_column_if_missing('erp_order_profit', 'operator_id', "bigint null comment 'Attributed operator id'");
call add_column_if_missing('erp_order_profit', 'operator_name', "varchar(64) null comment 'Attributed operator name'");
call add_column_if_missing('erp_order_profit', 'share_rate', "decimal(8,4) default 1.0000 comment 'Operator profit share rate'");
call add_column_if_missing('erp_order_profit', 'share_profit_amount', "decimal(14,2) default 0.00 comment 'Operator shared profit amount'");

call add_column_if_missing('fin_expense_bill', 'operator_id', "bigint null comment 'Operator id'");
call add_column_if_missing('fin_expense_bill', 'operator_name', "varchar(64) null comment 'Operator name'");
call add_column_if_missing('fin_expense_bill', 'channel_code', "varchar(64) null comment 'Traffic channel code'");
call add_column_if_missing('fin_expense_bill', 'channel_name', "varchar(128) null comment 'Traffic channel name'");

drop procedure if exists add_column_if_missing;
