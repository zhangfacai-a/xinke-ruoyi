-- ERP + BI MVP tables for MySQL 8+

create table if not exists erp_product (
  product_id bigint not null auto_increment,
  product_code varchar(64) not null,
  product_name varchar(128) not null,
  category_name varchar(128) null,
  brand_name varchar(128) null,
  cost_price decimal(12,2) default 0.00,
  sale_price decimal(12,2) default 0.00,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (product_id),
  unique key uk_erp_product_code (product_code)
) engine=innodb default charset=utf8mb4 comment='ERP product';

create table if not exists erp_product_sku (
  sku_id bigint not null auto_increment,
  product_id bigint not null,
  sku_code varchar(64) not null,
  sku_name varchar(128) not null,
  cost_price decimal(12,2) default 0.00,
  sale_price decimal(12,2) default 0.00,
  stock_qty int default 0,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (sku_id),
  unique key uk_erp_sku_code (sku_code),
  key idx_erp_sku_product (product_id)
) engine=innodb default charset=utf8mb4 comment='ERP product sku';

create table if not exists ods_order_raw (
  raw_id bigint not null auto_increment,
  order_id bigint not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  order_status int null,
  pay_time datetime null,
  platform_fee decimal(12,2) default 0.00,
  ad_cost decimal(12,2) default 0.00,
  freight_fee decimal(12,2) default 0.00,
  refund_cost decimal(12,2) default 0.00,
  sku_order_list json null,
  raw_payload json null,
  create_time datetime null,
  primary key (raw_id),
  key idx_ods_order_pay_time (pay_time),
  key idx_ods_order_id (order_id)
) engine=innodb default charset=utf8mb4 comment='ODS raw ecommerce order';

create table if not exists dwd_order_item (
  order_item_id bigint not null auto_increment,
  order_id bigint not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  product_id bigint null,
  product_name varchar(128) null,
  sku_id bigint null,
  sku_name varchar(128) null,
  quantity int default 0,
  pay_amount decimal(12,2) default 0.00,
  product_cost decimal(12,2) default 0.00,
  platform_fee decimal(12,2) default 0.00,
  ad_cost decimal(12,2) default 0.00,
  freight_fee decimal(12,2) default 0.00,
  refund_cost decimal(12,2) default 0.00,
  profit_amount decimal(12,2) default 0.00,
  order_status int null,
  pay_time datetime null,
  dt date not null,
  primary key (order_item_id),
  key idx_dwd_order_item_order_id (order_id),
  key idx_dwd_order_item_dt_shop (dt, shop_id),
  key idx_dwd_order_item_product (product_id)
) engine=innodb default charset=utf8mb4 comment='DWD order item detail';

create table if not exists dws_finance_day_profit (
  dt date not null,
  shop_id bigint not null,
  shop_name varchar(128) null,
  gmv decimal(14,2) default 0.00,
  net_amount decimal(14,2) default 0.00,
  product_cost decimal(14,2) default 0.00,
  platform_fee decimal(14,2) default 0.00,
  ad_cost decimal(14,2) default 0.00,
  freight_fee decimal(14,2) default 0.00,
  after_sale_cost decimal(14,2) default 0.00,
  profit_amount decimal(14,2) default 0.00,
  order_count bigint default 0,
  primary key (dt, shop_id)
) engine=innodb default charset=utf8mb4 comment='DWS finance day profit';

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'ERP', 0, 8, 'erp', null, 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), 'ERP'
where not exists (select 1 from sys_menu where menu_name = 'ERP' and parent_id = 0);

set @erp_parent_id := (select menu_id from sys_menu where menu_name = 'ERP' and parent_id = 0 limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '订单管理', @erp_parent_id, 1, 'order', 'erp/order/index', 1, 0, 'C', '0', '0', 'erp:order:list', 'list', 'admin', sysdate(), 'ERP order'
where not exists (select 1 from sys_menu where perms = 'erp:order:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '商品管理', @erp_parent_id, 2, 'product', 'erp/product/index', 1, 0, 'C', '0', '0', 'erp:product:list', 'goods', 'admin', sysdate(), 'ERP product'
where not exists (select 1 from sys_menu where perms = 'erp:product:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'SKU管理', @erp_parent_id, 3, 'sku', 'erp/sku/index', 1, 0, 'C', '0', '0', 'erp:sku:list', 'component', 'admin', sysdate(), 'ERP SKU'
where not exists (select 1 from sys_menu where perms = 'erp:sku:list');

set @product_menu_id := (select menu_id from sys_menu where perms = 'erp:product:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '商品查询', @product_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:product:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:product:query');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '商品新增', @product_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:product:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:product:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '商品修改', @product_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'erp:product:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:product:edit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '商品删除', @product_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:product:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:product:remove');

set @order_menu_id := (select menu_id from sys_menu where perms = 'erp:order:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '订单查询', @order_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:order:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:order:query');

set @sku_menu_id := (select menu_id from sys_menu where perms = 'erp:sku:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'SKU查询', @sku_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:sku:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:sku:query');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'SKU新增', @sku_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:sku:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:sku:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'SKU修改', @sku_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'erp:sku:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:sku:edit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'SKU删除', @sku_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:sku:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:sku:remove');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'ETL执行', @erp_parent_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:etl:run', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:etl:run');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'BI', 0, 9, 'bi', null, 1, 0, 'M', '0', '0', '', 'chart', 'admin', sysdate(), 'BI'
where not exists (select 1 from sys_menu where menu_name = 'BI' and parent_id = 0);

set @bi_parent_id := (select menu_id from sys_menu where menu_name = 'BI' and parent_id = 0 limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'BI看板', @bi_parent_id, 1, 'dashboard', 'bi/dashboard', 1, 0, 'C', '0', '0', 'bi:profit:summary', 'chart', 'admin', sysdate(), 'BI dashboard'
where not exists (select 1 from sys_menu where perms = 'bi:profit:summary');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '店铺销量', @bi_parent_id, 2, '#', '', 1, 0, 'F', '0', '0', 'bi:shop:sale', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'bi:shop:sale');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '商品排行', @bi_parent_id, 3, '#', '', 1, 0, 'F', '0', '0', 'bi:product:rank', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'bi:product:rank');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Finance', 0, 10, 'finance', null, 1, 0, 'M', '0', '0', '', 'money', 'admin', sysdate(), 'Finance'
where not exists (select 1 from sys_menu where menu_name = 'Finance' and parent_id = 0);

set @finance_parent_id := (select menu_id from sys_menu where menu_name = 'Finance' and parent_id = 0 limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '财务看板', @finance_parent_id, 1, 'profit', 'finance/profit', 1, 0, 'C', '0', '0', 'finance:profit:daily', 'money', 'admin', sysdate(), 'Finance profit'
where not exists (select 1 from sys_menu where perms = 'finance:profit:daily');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '月利润报表', @finance_parent_id, 2, '#', '', 1, 0, 'F', '0', '0', 'finance:profit:monthly', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'finance:profit:monthly');
