set names utf8mb4;

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
) engine=innodb default charset=utf8mb4 comment='ERP 商品';

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
) engine=innodb default charset=utf8mb4 comment='ERP SKU';

create table if not exists erp_shop (
  shop_id bigint not null auto_increment,
  shop_code varchar(64) not null,
  shop_name varchar(128) not null,
  platform_code varchar(32) null,
  platform_name varchar(64) null,
  owner_name varchar(64) null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (shop_id),
  unique key uk_erp_shop_code (shop_code)
) engine=innodb default charset=utf8mb4 comment='ERP 店铺档案';

create table if not exists erp_warehouse (
  warehouse_id bigint not null auto_increment,
  warehouse_code varchar(64) not null,
  warehouse_name varchar(128) not null,
  warehouse_type varchar(32) default 'physical',
  warehouse_usage varchar(32) not null default 'normal',
  ownership_type varchar(32) not null default 'self_operated',
  provider_name varchar(128) null,
  external_warehouse_code varchar(128) null,
  owner_code varchar(64) null,
  sync_mode varchar(32) not null default 'manual',
  sync_status varchar(32) not null default 'not_applicable',
  last_sync_time datetime null,
  enable_location char(1) not null default '1',
  allow_negative_stock char(1) not null default '0',
  priority int not null default 100,
  contact_name varchar(64) null,
  contact_phone varchar(32) null,
  address varchar(255) null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (warehouse_id),
  unique key uk_erp_warehouse_code (warehouse_code)
) engine=innodb default charset=utf8mb4 comment='ERP 仓库档案';

create table if not exists erp_supplier (
  supplier_id bigint not null auto_increment,
  supplier_code varchar(64) not null,
  supplier_name varchar(128) not null,
  contact_name varchar(64) null,
  contact_phone varchar(32) null,
  settlement_type varchar(32) default 'monthly',
  payment_days int default 30,
  supplier_level varchar(8) not null default 'B',
  lead_time_days int not null default 7,
  min_order_amount decimal(14,2) not null default 0.00,
  quality_score decimal(5,2) not null default 80.00,
  delivery_score decimal(5,2) not null default 80.00,
  service_score decimal(5,2) not null default 80.00,
  preferred_flag char(1) not null default '0',
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (supplier_id),
  unique key uk_erp_supplier_code (supplier_code)
) engine=innodb default charset=utf8mb4 comment='ERP 供应商档案';

create table if not exists erp_customer (
  customer_id bigint not null auto_increment,
  customer_code varchar(64) not null,
  customer_name varchar(128) not null,
  customer_type varchar(32) default 'platform_buyer',
  contact_name varchar(64) null,
  contact_phone varchar(32) null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (customer_id),
  unique key uk_erp_customer_code (customer_code)
) engine=innodb default charset=utf8mb4 comment='ERP 客户档案';

create table if not exists erp_inventory_balance (
  balance_id bigint not null auto_increment,
  warehouse_id bigint not null,
  warehouse_name varchar(128) null,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  sku_name varchar(128) null,
  available_qty int default 0,
  locked_qty int default 0,
  inbound_qty int default 0,
  defective_qty int default 0,
  safety_qty int default 0,
  cost_price decimal(12,2) default 0.00,
  external_available_qty int null,
  external_locked_qty int null,
  sync_diff_qty int null,
  sync_status varchar(32) not null default 'not_applicable',
  last_sync_time datetime null,
  create_time datetime null,
  update_time datetime null,
  primary key (balance_id),
  unique key uk_erp_inventory_wh_sku (warehouse_id, sku_id),
  key idx_erp_inventory_sku (sku_code)
) engine=innodb default charset=utf8mb4 comment='ERP 库存余额';

create table if not exists erp_warehouse_location (
  location_id bigint not null auto_increment,
  warehouse_id bigint not null,
  location_code varchar(64) not null,
  location_name varchar(128) not null,
  zone_code varchar(64) null,
  usage_type varchar(32) not null default 'normal',
  barcode varchar(128) null,
  sort_order int not null default 100,
  status char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (location_id),
  unique key uk_erp_location_code (warehouse_id, location_code)
) engine=innodb default charset=utf8mb4 comment='实体仓库位档案';

create table if not exists erp_warehouse_sync_log (
  sync_log_id bigint not null auto_increment,
  sync_batch_no varchar(64) not null,
  warehouse_id bigint not null,
  source_type varchar(64) not null default 'manual',
  sync_status varchar(32) not null,
  total_sku_count int not null default 0,
  matched_sku_count int not null default 0,
  unmatched_sku_count int not null default 0,
  difference_sku_count int not null default 0,
  started_time datetime null,
  finished_time datetime null,
  message varchar(500) null,
  create_by varchar(64) default '',
  create_time datetime null,
  primary key (sync_log_id),
  unique key uk_erp_warehouse_sync_batch (sync_batch_no),
  key idx_erp_warehouse_sync_time (warehouse_id, create_time)
) engine=innodb default charset=utf8mb4 comment='云仓库存同步日志';

create table if not exists erp_inventory_movement (
  movement_id bigint not null auto_increment,
  movement_no varchar(64) not null,
  warehouse_id bigint not null,
  sku_id bigint not null,
  movement_type varchar(32) not null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  quantity int not null,
  before_qty int default 0,
  after_qty int default 0,
  cost_price decimal(12,2) default 0.00,
  operator_name varchar(64) null,
  movement_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  remark varchar(500) null,
  primary key (movement_id),
  unique key uk_erp_inventory_movement_no (movement_no),
  key idx_erp_inventory_movement_sku (sku_id),
  key idx_erp_inventory_movement_source (source_type, source_no)
) engine=innodb default charset=utf8mb4 comment='ERP 库存流水';

create table if not exists erp_purchase_order (
  purchase_id bigint not null auto_increment,
  purchase_no varchar(64) not null,
  supplier_id bigint not null,
  supplier_name varchar(128) null,
  warehouse_id bigint null,
  total_amount decimal(14,2) default 0.00,
  tax_amount decimal(14,2) not null default 0.00,
  total_with_tax decimal(14,2) not null default 0.00,
  purchase_status varchar(32) default 'draft',
  purchase_date date not null,
  expected_time datetime null,
  submit_by varchar(64) null,
  submit_time datetime null,
  approve_by varchar(64) null,
  approve_time datetime null,
  close_by varchar(64) null,
  close_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (purchase_id),
  unique key uk_erp_purchase_no (purchase_no)
) engine=innodb default charset=utf8mb4 comment='ERP 采购订单';

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
) engine=innodb default charset=utf8mb4 comment='ERP 采购订单明细';

create table if not exists erp_sales_order (
  sales_id bigint not null auto_increment,
  sales_no varchar(64) not null,
  platform_order_no varchar(128) null,
  shop_id bigint null,
  shop_name varchar(128) null,
  customer_id bigint null,
  order_status varchar(32) default 'paid',
  pay_amount decimal(14,2) default 0.00,
  pay_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (sales_id),
  unique key uk_erp_sales_no (sales_no),
  key idx_erp_sales_platform_order (platform_order_no),
  key idx_erp_sales_shop_time (shop_id, pay_time)
) engine=innodb default charset=utf8mb4 comment='ERP 销售订单';

create table if not exists erp_sales_order_item (
  item_id bigint not null auto_increment,
  sales_id bigint not null,
  sales_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  sku_name varchar(128) null,
  quantity int default 0,
  pay_amount decimal(14,2) default 0.00,
  product_cost decimal(14,2) default 0.00,
  primary key (item_id),
  key idx_erp_sales_item_order (sales_id),
  key idx_erp_sales_item_sku (sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP 销售订单明细';

create table if not exists erp_refund_order (
  refund_id bigint not null auto_increment,
  refund_no varchar(64) not null,
  sales_no varchar(64) null,
  shop_id bigint null,
  sku_id bigint null,
  refund_type varchar(32) default 'refund_only',
  refund_status varchar(32) default 'processing',
  refund_amount decimal(14,2) default 0.00,
  return_freight decimal(14,2) default 0.00,
  reason varchar(128) null,
  apply_time datetime null,
  finish_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (refund_id),
  unique key uk_erp_refund_no (refund_no),
  key idx_erp_refund_sales (sales_no)
) engine=innodb default charset=utf8mb4 comment='ERP 售后退款单';

create table if not exists erp_settlement_bill (
  settlement_id bigint not null auto_increment,
  settlement_no varchar(64) not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  bill_month char(7) not null,
  income_amount decimal(14,2) default 0.00,
  refund_amount decimal(14,2) default 0.00,
  platform_fee decimal(14,2) default 0.00,
  ad_cost decimal(14,2) default 0.00,
  freight_fee decimal(14,2) default 0.00,
  net_amount decimal(14,2) default 0.00,
  bill_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (settlement_id),
  unique key uk_erp_settlement_no (settlement_no),
  key idx_erp_settlement_shop_month (shop_id, bill_month)
) engine=innodb default charset=utf8mb4 comment='ERP 平台结算账单';

create table if not exists erp_import_batch (
  batch_id bigint not null auto_increment,
  batch_no varchar(64) not null,
  import_type varchar(32) not null,
  file_name varchar(255) null,
  total_count int default 0,
  success_count int default 0,
  fail_count int default 0,
  batch_status varchar(32) default 'created',
  error_message varchar(1000) null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_time datetime null,
  primary key (batch_id),
  unique key uk_erp_import_batch_no (batch_no)
) engine=innodb default charset=utf8mb4 comment='ERP 导入批次';

insert into erp_shop(shop_code, shop_name, platform_code, platform_name, owner_name, status, create_by, create_time)
select 'SHOP-DEMO-001', '演示旗舰店', 'douyin', '抖音电商', 'admin', '0', 'admin', sysdate()
where not exists (select 1 from erp_shop where shop_code = 'SHOP-DEMO-001');

insert into erp_warehouse(warehouse_code, warehouse_name, warehouse_type, contact_name, contact_phone, address, status, create_by, create_time)
select 'WH-DEMO-001', '上海主仓', 'physical', '仓库主管', '13800000000', '上海市', '0', 'admin', sysdate()
where not exists (select 1 from erp_warehouse where warehouse_code = 'WH-DEMO-001');

insert into erp_supplier(supplier_code, supplier_name, contact_name, contact_phone, settlement_type, payment_days, status, create_by, create_time)
select 'SUP-DEMO-001', '演示供应商', '供应商联系人', '13900000000', 'monthly', 30, '0', 'admin', sysdate()
where not exists (select 1 from erp_supplier where supplier_code = 'SUP-DEMO-001');

insert into erp_product(product_code, product_name, category_name, brand_name, cost_price, sale_price, status, create_by, create_time, remark)
select 'SKU-DEMO-001', '演示保温杯', '日用百货', 'Crucible', 18.50, 39.90, '0', 'admin', sysdate(), '初始化演示商品'
where not exists (select 1 from erp_product where product_code = 'SKU-DEMO-001');

insert into erp_product_sku(product_id, sku_code, sku_name, cost_price, sale_price, stock_qty, status, create_by, create_time, remark)
select p.product_id, 'SKU-DEMO-001-WHITE', '白色 500ml', 18.50, 39.90, 100, '0', 'admin', sysdate(), '初始化演示SKU'
from erp_product p
where p.product_code = 'SKU-DEMO-001'
  and not exists (select 1 from erp_product_sku where sku_code = 'SKU-DEMO-001-WHITE');

insert into erp_inventory_balance(warehouse_id, warehouse_name, sku_id, sku_code, sku_name, available_qty, locked_qty, inbound_qty, defective_qty, safety_qty, cost_price, create_time, update_time)
select w.warehouse_id, w.warehouse_name, s.sku_id, s.sku_code, s.sku_name, 100, 0, 0, 0, 10, s.cost_price, sysdate(), sysdate()
from erp_warehouse w
join erp_product_sku s on s.sku_code = 'SKU-DEMO-001-WHITE'
where w.warehouse_code = 'WH-DEMO-001'
  and not exists (
    select 1 from erp_inventory_balance b where b.warehouse_id = w.warehouse_id and b.sku_id = s.sku_id
  );

insert into erp_purchase_order(purchase_no, supplier_id, supplier_name, warehouse_id, total_amount, purchase_status, purchase_date, expected_time, create_by, create_time, remark)
select 'PO-DEMO-20260628-001', sup.supplier_id, sup.supplier_name, wh.warehouse_id, 1850.00, 'approved', curdate(), date_add(sysdate(), interval 3 day), 'admin', sysdate(), '初始化演示采购单'
from erp_supplier sup
join erp_warehouse wh on wh.warehouse_code = 'WH-DEMO-001'
where sup.supplier_code = 'SUP-DEMO-001'
  and not exists (select 1 from erp_purchase_order where purchase_no = 'PO-DEMO-20260628-001');

insert into erp_inventory_movement(movement_no, warehouse_id, sku_id, movement_type, source_type, source_no, quantity, before_qty, after_qty, cost_price, operator_name, movement_time, create_by, create_time, remark)
select 'MV-DEMO-20260628-001', wh.warehouse_id, sku.sku_id, 'IN', 'INIT', 'INIT-STOCK-001', 100, 0, 100, sku.cost_price, 'admin', sysdate(), 'admin', sysdate(), '初始化库存入库'
from erp_warehouse wh
join erp_product_sku sku on sku.sku_code = 'SKU-DEMO-001-WHITE'
where wh.warehouse_code = 'WH-DEMO-001'
  and not exists (select 1 from erp_inventory_movement where movement_no = 'MV-DEMO-20260628-001');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'ERP', 0, 8, 'erp', null, 1, 0, 'M', '0', '0', '', 'shopping', 'admin', sysdate(), 'ERP'
where not exists (select 1 from sys_menu where menu_name = 'ERP' and parent_id = 0);

set @erp_parent_id := (select menu_id from sys_menu where menu_name = 'ERP' and parent_id = 0 limit 1);

update sys_menu set menu_name = '订单管理', component = 'erp/order/index' where perms = 'erp:order:list';
update sys_menu set menu_name = '商品管理', component = 'erp/product/index' where perms = 'erp:product:list';
update sys_menu set menu_name = 'SKU管理', component = 'erp/sku/index' where perms = 'erp:sku:list';

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '店铺管理', @erp_parent_id, 5, 'shop', 'erp/shop/index', 1, 0, 'C', '0', '0', 'erp:shop:list', 'shop', 'admin', sysdate(), 'ERP shop'
where not exists (select 1 from sys_menu where perms = 'erp:shop:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '仓库管理', @erp_parent_id, 6, 'warehouse', 'erp/warehouse/index', 1, 0, 'C', '0', '0', 'erp:warehouse:list', 'tree', 'admin', sysdate(), 'ERP warehouse'
where not exists (select 1 from sys_menu where perms = 'erp:warehouse:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '供应商管理', @erp_parent_id, 7, 'supplier', 'erp/supplier/index', 1, 0, 'C', '0', '0', 'erp:supplier:list', 'peoples', 'admin', sysdate(), 'ERP supplier'
where not exists (select 1 from sys_menu where perms = 'erp:supplier:list');
update sys_menu set route_name = 'ErpSupplier' where perms = 'erp:supplier:list';

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存查询', @erp_parent_id, 8, 'inventory', 'erp/inventory/index', 1, 0, 'C', '0', '0', 'erp:inventory:list', 'monitor', 'admin', sysdate(), 'ERP inventory'
where not exists (select 1 from sys_menu where perms = 'erp:inventory:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '采购订单', @erp_parent_id, 9, 'purchase', 'erp/purchase/index', 1, 0, 'C', '0', '0', 'erp:purchase:list', 'form', 'admin', sysdate(), 'ERP purchase'
where not exists (select 1 from sys_menu where perms = 'erp:purchase:list');
update sys_menu set route_name = 'ErpPurchaseOrder' where perms = 'erp:purchase:list';

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存流水', @erp_parent_id, 10, 'movement', 'erp/movement/index', 1, 0, 'C', '0', '0', 'erp:movement:list', 'log', 'admin', sysdate(), 'ERP movement'
where not exists (select 1 from sys_menu where perms = 'erp:movement:list');

set @shop_menu_id := (select menu_id from sys_menu where perms = 'erp:shop:list' limit 1);
set @warehouse_menu_id := (select menu_id from sys_menu where perms = 'erp:warehouse:list' limit 1);
set @supplier_menu_id := (select menu_id from sys_menu where perms = 'erp:supplier:list' limit 1);
set @inventory_menu_id := (select menu_id from sys_menu where perms = 'erp:inventory:list' limit 1);
set @purchase_menu_id := (select menu_id from sys_menu where perms = 'erp:purchase:list' limit 1);
set @movement_menu_id := (select menu_id from sys_menu where perms = 'erp:movement:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '店铺查询', @shop_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:shop:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:shop:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '店铺新增', @shop_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:shop:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:shop:add');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '店铺修改', @shop_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'erp:shop:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:shop:edit');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '店铺删除', @shop_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:shop:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:shop:remove');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '仓库查询', @warehouse_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:warehouse:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:warehouse:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '仓库新增', @warehouse_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:warehouse:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:warehouse:add');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '仓库修改', @warehouse_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'erp:warehouse:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:warehouse:edit');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '仓库删除', @warehouse_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:warehouse:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:warehouse:remove');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '供应商查询', @supplier_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:supplier:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:supplier:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '供应商新增', @supplier_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:supplier:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:supplier:add');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '供应商修改', @supplier_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'erp:supplier:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:supplier:edit');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '供应商删除', @supplier_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:supplier:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:supplier:remove');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存查询', @inventory_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:inventory:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:inventory:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '云仓库存同步', @inventory_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:inventory:sync', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:inventory:sync');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '采购查询', @purchase_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:purchase:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:purchase:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '采购新增', @purchase_menu_id, 2, '#', '', 1, 0, 'F', '0', '0', 'erp:purchase:add', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:purchase:add');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '采购修改', @purchase_menu_id, 3, '#', '', 1, 0, 'F', '0', '0', 'erp:purchase:edit', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:purchase:edit');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '采购删除', @purchase_menu_id, 4, '#', '', 1, 0, 'F', '0', '0', 'erp:purchase:remove', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:purchase:remove');

insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '流水查询', @movement_menu_id, 1, '#', '', 1, 0, 'F', '0', '0', 'erp:movement:query', '#', 'admin', sysdate(), ''
where not exists (select 1 from sys_menu where perms = 'erp:movement:query');
