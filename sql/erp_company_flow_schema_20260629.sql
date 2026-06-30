-- Company ERP flow extension: procurement, inventory, sales, product and finance workflow.

create table if not exists erp_purchase_request (
  request_id bigint not null auto_increment,
  request_no varchar(64) not null,
  request_title varchar(128) null,
  requester_name varchar(64) null,
  department_name varchar(128) null,
  supplier_id bigint null,
  supplier_name varchar(128) null,
  warehouse_id bigint null,
  expected_date date null,
  total_amount decimal(14,2) default 0.00,
  request_status varchar(32) default 'draft',
  approve_by varchar(64) default '',
  approve_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (request_id),
  unique key uk_erp_purchase_request_no (request_no),
  key idx_erp_purchase_request_status (request_status)
) engine=innodb default charset=utf8mb4 comment='ERP purchase request';

create table if not exists erp_purchase_request_item (
  item_id bigint not null auto_increment,
  request_no varchar(64) not null,
  sku_id bigint null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  quantity int default 0,
  quote_price decimal(12,2) default 0.00,
  amount decimal(14,2) default 0.00,
  remark varchar(500) null,
  primary key (item_id),
  key idx_erp_purchase_request_item_no (request_no)
) engine=innodb default charset=utf8mb4 comment='ERP purchase request item';

create table if not exists erp_purchase_receipt (
  receipt_id bigint not null auto_increment,
  receipt_no varchar(64) not null,
  purchase_no varchar(64) null,
  supplier_id bigint null,
  supplier_name varchar(128) null,
  warehouse_id bigint not null,
  warehouse_name varchar(128) null,
  receipt_date date not null,
  total_qty int default 0,
  total_amount decimal(14,2) default 0.00,
  receipt_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (receipt_id),
  unique key uk_erp_purchase_receipt_no (receipt_no),
  key idx_erp_purchase_receipt_purchase (purchase_no)
) engine=innodb default charset=utf8mb4 comment='ERP purchase receipt';

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
) engine=innodb default charset=utf8mb4 comment='ERP purchase receipt item';

create table if not exists erp_purchase_return (
  return_id bigint not null auto_increment,
  return_no varchar(64) not null,
  purchase_no varchar(64) null,
  receipt_no varchar(64) null,
  supplier_id bigint null,
  supplier_name varchar(128) null,
  warehouse_id bigint not null,
  return_date date not null,
  total_qty int default 0,
  total_amount decimal(14,2) default 0.00,
  return_reason varchar(255) null,
  return_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (return_id),
  unique key uk_erp_purchase_return_no (return_no),
  key idx_erp_purchase_return_purchase (purchase_no)
) engine=innodb default charset=utf8mb4 comment='ERP purchase return';

create table if not exists erp_purchase_return_item (
  item_id bigint not null auto_increment,
  return_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  quantity int default 0,
  cost_price decimal(12,2) default 0.00,
  amount decimal(14,2) default 0.00,
  primary key (item_id),
  key idx_erp_purchase_return_item_no (return_no)
) engine=innodb default charset=utf8mb4 comment='ERP purchase return item';

create table if not exists erp_supplier_reconcile (
  reconcile_id bigint not null auto_increment,
  reconcile_no varchar(64) not null,
  supplier_id bigint not null,
  supplier_name varchar(128) null,
  period_code char(7) not null,
  purchase_amount decimal(14,2) default 0.00,
  return_amount decimal(14,2) default 0.00,
  payable_amount decimal(14,2) default 0.00,
  diff_amount decimal(14,2) default 0.00,
  reconcile_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (reconcile_id),
  unique key uk_erp_supplier_reconcile_no (reconcile_no),
  key idx_erp_supplier_reconcile_supplier (supplier_id, period_code)
) engine=innodb default charset=utf8mb4 comment='ERP supplier reconciliation';

create table if not exists erp_inventory_transfer (
  transfer_id bigint not null auto_increment,
  transfer_no varchar(64) not null,
  from_warehouse_id bigint not null,
  from_warehouse_name varchar(128) null,
  to_warehouse_id bigint not null,
  to_warehouse_name varchar(128) null,
  transfer_date date not null,
  total_qty int default 0,
  transfer_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (transfer_id),
  unique key uk_erp_inventory_transfer_no (transfer_no)
) engine=innodb default charset=utf8mb4 comment='ERP inventory transfer';

create table if not exists erp_inventory_transfer_item (
  item_id bigint not null auto_increment,
  transfer_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  quantity int default 0,
  cost_price decimal(12,2) default 0.00,
  primary key (item_id),
  key idx_erp_inventory_transfer_item_no (transfer_no)
) engine=innodb default charset=utf8mb4 comment='ERP inventory transfer item';

create table if not exists erp_inventory_stocktake (
  stocktake_id bigint not null auto_increment,
  stocktake_no varchar(64) not null,
  warehouse_id bigint not null,
  warehouse_name varchar(128) null,
  stocktake_date date not null,
  total_sku_count int default 0,
  profit_loss_amount decimal(14,2) default 0.00,
  stocktake_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (stocktake_id),
  unique key uk_erp_inventory_stocktake_no (stocktake_no)
) engine=innodb default charset=utf8mb4 comment='ERP inventory stocktake';

create table if not exists erp_inventory_stocktake_item (
  item_id bigint not null auto_increment,
  stocktake_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  book_qty int default 0,
  actual_qty int default 0,
  diff_qty int default 0,
  cost_price decimal(12,2) default 0.00,
  diff_amount decimal(14,2) default 0.00,
  primary key (item_id),
  key idx_erp_stocktake_item_no (stocktake_no)
) engine=innodb default charset=utf8mb4 comment='ERP inventory stocktake item';

create table if not exists erp_inventory_loss (
  loss_id bigint not null auto_increment,
  loss_no varchar(64) not null,
  warehouse_id bigint not null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  quantity int default 0,
  cost_price decimal(12,2) default 0.00,
  loss_amount decimal(14,2) default 0.00,
  loss_reason varchar(255) null,
  loss_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (loss_id),
  unique key uk_erp_inventory_loss_no (loss_no)
) engine=innodb default charset=utf8mb4 comment='ERP inventory loss';

create table if not exists erp_inventory_warning_rule (
  rule_id bigint not null auto_increment,
  warehouse_id bigint null,
  sku_id bigint null,
  sku_code varchar(64) null,
  min_qty int default 0,
  max_qty int default 0,
  warning_level varchar(32) default 'normal',
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (rule_id),
  key idx_erp_inventory_warning_sku (warehouse_id, sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP inventory warning rule';

create table if not exists erp_inventory_cost_layer (
  layer_id bigint not null auto_increment,
  warehouse_id bigint not null,
  sku_id bigint not null,
  batch_no varchar(64) null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  remain_qty int default 0,
  cost_price decimal(12,2) default 0.00,
  inbound_date date not null,
  create_time datetime null,
  primary key (layer_id),
  key idx_erp_inventory_cost_layer_sku (warehouse_id, sku_id, inbound_date)
) engine=innodb default charset=utf8mb4 comment='ERP inventory cost layer';

create table if not exists erp_inventory_ageing_snapshot (
  snapshot_id bigint not null auto_increment,
  snapshot_date date not null,
  warehouse_id bigint not null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  qty_0_30 int default 0,
  qty_31_60 int default 0,
  qty_61_90 int default 0,
  qty_90_plus int default 0,
  amount_90_plus decimal(14,2) default 0.00,
  create_time datetime null,
  primary key (snapshot_id),
  key idx_erp_inventory_ageing_sku (snapshot_date, warehouse_id, sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP inventory ageing snapshot';

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

create table if not exists erp_sales_shipment_item (
  item_id bigint not null auto_increment,
  shipment_no varchar(64) not null,
  sales_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  sku_name varchar(128) null,
  quantity int default 0,
  primary key (item_id),
  key idx_erp_shipment_item_no (shipment_no)
) engine=innodb default charset=utf8mb4 comment='ERP sales shipment item';

create table if not exists erp_after_sale_order (
  after_sale_id bigint not null auto_increment,
  after_sale_no varchar(64) not null,
  sales_no varchar(64) null,
  refund_no varchar(64) null,
  after_sale_type varchar(32) default 'refund',
  after_sale_status varchar(32) default 'processing',
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

create table if not exists erp_compensation_order (
  compensation_id bigint not null auto_increment,
  compensation_no varchar(64) not null,
  sales_no varchar(64) null,
  shop_id bigint null,
  sku_id bigint null,
  compensation_type varchar(32) default 'cash',
  compensation_amount decimal(14,2) default 0.00,
  compensation_status varchar(32) default 'draft',
  reason varchar(255) null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (compensation_id),
  unique key uk_erp_compensation_no (compensation_no)
) engine=innodb default charset=utf8mb4 comment='ERP compensation order';

create table if not exists erp_platform_settlement_match (
  match_id bigint not null auto_increment,
  match_no varchar(64) not null,
  settlement_no varchar(64) not null,
  sales_no varchar(64) null,
  refund_no varchar(64) null,
  platform_order_no varchar(128) null,
  settlement_amount decimal(14,2) default 0.00,
  order_amount decimal(14,2) default 0.00,
  diff_amount decimal(14,2) default 0.00,
  match_status varchar(32) default 'unmatched',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (match_id),
  unique key uk_erp_platform_match_no (match_no),
  key idx_erp_platform_match_settlement (settlement_no)
) engine=innodb default charset=utf8mb4 comment='ERP platform settlement match';

create table if not exists erp_order_profit (
  profit_id bigint not null auto_increment,
  sales_no varchar(64) not null,
  shop_id bigint null,
  sku_id bigint null,
  pay_amount decimal(14,2) default 0.00,
  product_cost decimal(14,2) default 0.00,
  platform_fee decimal(14,2) default 0.00,
  promotion_fee decimal(14,2) default 0.00,
  freight_fee decimal(14,2) default 0.00,
  after_sale_cost decimal(14,2) default 0.00,
  profit_amount decimal(14,2) default 0.00,
  profit_rate decimal(8,4) default 0.0000,
  profit_date date null,
  create_time datetime null,
  primary key (profit_id),
  unique key uk_erp_order_profit_sales (sales_no),
  key idx_erp_order_profit_date (profit_date, shop_id)
) engine=innodb default charset=utf8mb4 comment='ERP order profit';

create table if not exists erp_sku_barcode (
  barcode_id bigint not null auto_increment,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  barcode varchar(128) not null,
  barcode_type varchar(32) default 'inner',
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (barcode_id),
  unique key uk_erp_sku_barcode (barcode)
) engine=innodb default charset=utf8mb4 comment='ERP SKU barcode';

create table if not exists erp_platform_sku_mapping (
  mapping_id bigint not null auto_increment,
  platform_code varchar(32) not null,
  shop_id bigint null,
  platform_product_id varchar(128) null,
  platform_sku_id varchar(128) not null,
  platform_sku_name varchar(255) null,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  split_rule_json text null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (mapping_id),
  unique key uk_erp_platform_sku_mapping (platform_code, platform_sku_id),
  key idx_erp_platform_sku_mapping_sku (sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP platform SKU mapping';

create table if not exists erp_product_bundle (
  bundle_id bigint not null auto_increment,
  bundle_sku_id bigint not null,
  bundle_sku_code varchar(64) not null,
  bundle_name varchar(128) null,
  bundle_status varchar(32) default 'enabled',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (bundle_id),
  unique key uk_erp_product_bundle_sku (bundle_sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP product bundle';

create table if not exists erp_product_bundle_item (
  item_id bigint not null auto_increment,
  bundle_id bigint not null,
  component_sku_id bigint not null,
  component_sku_code varchar(64) not null,
  component_sku_name varchar(128) null,
  quantity int default 1,
  cost_price decimal(12,2) default 0.00,
  primary key (item_id),
  key idx_erp_bundle_item_bundle (bundle_id)
) engine=innodb default charset=utf8mb4 comment='ERP product bundle item';

create table if not exists erp_sku_cost_history (
  cost_id bigint not null auto_increment,
  sku_id bigint not null,
  sku_code varchar(64) null,
  cost_price decimal(12,2) not null,
  effective_date date not null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  create_by varchar(64) default '',
  create_time datetime null,
  remark varchar(500) null,
  primary key (cost_id),
  key idx_erp_sku_cost_history_sku (sku_id, effective_date)
) engine=innodb default charset=utf8mb4 comment='ERP SKU cost history';

create table if not exists erp_supplier_quote (
  quote_id bigint not null auto_increment,
  quote_no varchar(64) not null,
  supplier_id bigint not null,
  supplier_name varchar(128) null,
  sku_id bigint not null,
  sku_code varchar(64) null,
  quote_price decimal(12,2) not null,
  min_order_qty int default 1,
  effective_date date not null,
  expire_date date null,
  quote_status varchar(32) default 'enabled',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (quote_id),
  unique key uk_erp_supplier_quote_no (quote_no),
  key idx_erp_supplier_quote_sku (supplier_id, sku_id)
) engine=innodb default charset=utf8mb4 comment='ERP supplier quote';

create table if not exists fin_payment_request (
  payment_id bigint not null auto_increment,
  payment_no varchar(64) not null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  counterparty_type varchar(32) default 'supplier',
  counterparty_id bigint null,
  counterparty_name varchar(128) null,
  payable_no varchar(64) null,
  payment_amount decimal(14,2) default 0.00,
  requested_pay_date date null,
  payment_status varchar(32) default 'draft',
  approve_by varchar(64) default '',
  approve_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (payment_id),
  unique key uk_fin_payment_request_no (payment_no),
  key idx_fin_payment_request_source (source_type, source_no)
) engine=innodb default charset=utf8mb4 comment='Finance payment request';

create table if not exists fin_payment_execute (
  execute_id bigint not null auto_increment,
  execute_no varchar(64) not null,
  payment_no varchar(64) not null,
  bank_account_id bigint null,
  cash_flow_no varchar(64) null,
  execute_amount decimal(14,2) default 0.00,
  execute_time datetime null,
  execute_status varchar(32) default 'done',
  operator_name varchar(64) null,
  create_by varchar(64) default '',
  create_time datetime null,
  remark varchar(500) null,
  primary key (execute_id),
  unique key uk_fin_payment_execute_no (execute_no),
  key idx_fin_payment_execute_payment (payment_no)
) engine=innodb default charset=utf8mb4 comment='Finance payment execution';

create table if not exists fin_reimbursement (
  reimbursement_id bigint not null auto_increment,
  reimbursement_no varchar(64) not null,
  employee_id bigint null,
  employee_name varchar(64) null,
  department_name varchar(128) null,
  reimbursement_title varchar(128) null,
  expense_amount decimal(14,2) default 0.00,
  invoice_amount decimal(14,2) default 0.00,
  reimbursement_status varchar(32) default 'draft',
  approve_by varchar(64) default '',
  approve_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (reimbursement_id),
  unique key uk_fin_reimbursement_no (reimbursement_no)
) engine=innodb default charset=utf8mb4 comment='Finance reimbursement';

create table if not exists fin_reimbursement_item (
  item_id bigint not null auto_increment,
  reimbursement_no varchar(64) not null,
  fee_type_code varchar(32) null,
  fee_type_name varchar(128) null,
  occurred_date date null,
  expense_amount decimal(14,2) default 0.00,
  invoice_no varchar(128) null,
  invoice_file_url varchar(500) null,
  remark varchar(500) null,
  primary key (item_id),
  key idx_fin_reimbursement_item_no (reimbursement_no)
) engine=innodb default charset=utf8mb4 comment='Finance reimbursement item';

create table if not exists fin_contract (
  contract_id bigint not null auto_increment,
  contract_no varchar(64) not null,
  contract_name varchar(128) not null,
  contract_type varchar(32) default 'purchase',
  counterparty_id bigint null,
  counterparty_name varchar(128) null,
  contract_amount decimal(14,2) default 0.00,
  signed_date date null,
  start_date date null,
  end_date date null,
  archive_url varchar(500) null,
  contract_status varchar(32) default 'draft',
  approve_by varchar(64) default '',
  approve_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (contract_id),
  unique key uk_fin_contract_no (contract_no),
  key idx_fin_contract_counterparty (counterparty_id)
) engine=innodb default charset=utf8mb4 comment='Finance contract ledger';

create table if not exists fin_contract_plan (
  plan_id bigint not null auto_increment,
  contract_no varchar(64) not null,
  plan_type varchar(16) not null,
  plan_date date not null,
  plan_amount decimal(14,2) default 0.00,
  actual_amount decimal(14,2) default 0.00,
  plan_status varchar(32) default 'open',
  source_no varchar(64) null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (plan_id),
  key idx_fin_contract_plan_no (contract_no, plan_date)
) engine=innodb default charset=utf8mb4 comment='Finance contract payment plan';

create table if not exists fin_subject_mapping (
  mapping_id bigint not null auto_increment,
  biz_type varchar(64) not null,
  fee_type_code varchar(32) null,
  source_type varchar(32) null,
  debit_subject_code varchar(32) null,
  debit_subject_name varchar(128) null,
  credit_subject_code varchar(32) null,
  credit_subject_name varchar(128) null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (mapping_id),
  key idx_fin_subject_mapping_biz (biz_type, fee_type_code, source_type)
) engine=innodb default charset=utf8mb4 comment='Finance subject mapping';

create table if not exists fin_voucher_template (
  template_id bigint not null auto_increment,
  template_code varchar(64) not null,
  template_name varchar(128) not null,
  source_type varchar(32) not null,
  template_status varchar(32) default 'enabled',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (template_id),
  unique key uk_fin_voucher_template_code (template_code)
) engine=innodb default charset=utf8mb4 comment='Finance voucher template';

create table if not exists fin_voucher_template_entry (
  entry_id bigint not null auto_increment,
  template_code varchar(64) not null,
  entry_seq int default 1,
  subject_code varchar(32) not null,
  subject_name varchar(128) null,
  dc varchar(8) not null,
  amount_expr varchar(255) null,
  summary_expr varchar(255) null,
  primary key (entry_id),
  key idx_fin_voucher_template_entry_code (template_code)
) engine=innodb default charset=utf8mb4 comment='Finance voucher template entry';

create table if not exists fin_ledger_entry (
  ledger_id bigint not null auto_increment,
  voucher_no varchar(64) not null,
  period_code char(7) not null,
  voucher_date date not null,
  subject_code varchar(32) not null,
  subject_name varchar(128) null,
  dc varchar(8) not null,
  amount decimal(14,2) default 0.00,
  counterparty_name varchar(128) null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  create_time datetime null,
  primary key (ledger_id),
  key idx_fin_ledger_subject (period_code, subject_code),
  key idx_fin_ledger_source (source_type, source_no)
) engine=innodb default charset=utf8mb4 comment='Finance ledger entry';

create table if not exists fin_trial_balance (
  trial_id bigint not null auto_increment,
  period_code char(7) not null,
  subject_code varchar(32) not null,
  subject_name varchar(128) null,
  begin_debit decimal(14,2) default 0.00,
  begin_credit decimal(14,2) default 0.00,
  current_debit decimal(14,2) default 0.00,
  current_credit decimal(14,2) default 0.00,
  end_debit decimal(14,2) default 0.00,
  end_credit decimal(14,2) default 0.00,
  create_time datetime null,
  primary key (trial_id),
  unique key uk_fin_trial_balance_subject (period_code, subject_code)
) engine=innodb default charset=utf8mb4 comment='Finance trial balance';

create table if not exists fin_profit_statement (
  statement_id bigint not null auto_increment,
  period_code char(7) not null,
  revenue_amount decimal(14,2) default 0.00,
  cost_amount decimal(14,2) default 0.00,
  gross_profit decimal(14,2) default 0.00,
  expense_amount decimal(14,2) default 0.00,
  net_profit decimal(14,2) default 0.00,
  create_time datetime null,
  primary key (statement_id),
  unique key uk_fin_profit_statement_period (period_code)
) engine=innodb default charset=utf8mb4 comment='Finance profit statement';
