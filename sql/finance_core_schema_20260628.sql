set names utf8mb4;

create table if not exists fin_account_period (
  period_id bigint not null auto_increment,
  period_code char(7) not null,
  start_date date not null,
  end_date date not null,
  close_status varchar(32) default 'open',
  close_by varchar(64) default '',
  close_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (period_id),
  unique key uk_fin_period_code (period_code)
) engine=innodb default charset=utf8mb4 comment='财务会计期间';

create table if not exists fin_account_subject (
  subject_id bigint not null auto_increment,
  subject_code varchar(32) not null,
  subject_name varchar(128) not null,
  subject_type varchar(32) not null,
  parent_code varchar(32) null,
  balance_direction varchar(16) not null,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (subject_id),
  unique key uk_fin_subject_code (subject_code),
  key idx_fin_subject_parent (parent_code)
) engine=innodb default charset=utf8mb4 comment='财务会计科目';

create table if not exists fin_platform_account (
  platform_account_id bigint not null auto_increment,
  platform_code varchar(32) not null,
  platform_name varchar(64) not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  account_no varchar(128) null,
  currency varchar(16) default 'CNY',
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (platform_account_id),
  unique key uk_fin_platform_account (platform_code, shop_id, account_no)
) engine=innodb default charset=utf8mb4 comment='财务平台资金账户';

create table if not exists fin_bank_account (
  bank_account_id bigint not null auto_increment,
  account_name varchar(128) not null,
  bank_name varchar(128) not null,
  bank_no varchar(128) not null,
  currency varchar(16) default 'CNY',
  opening_balance decimal(14,2) default 0.00,
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (bank_account_id),
  unique key uk_fin_bank_no (bank_no)
) engine=innodb default charset=utf8mb4 comment='财务银行账户';

create table if not exists fin_fee_type (
  fee_type_id bigint not null auto_increment,
  fee_code varchar(64) not null,
  fee_name varchar(128) not null,
  fee_category varchar(32) not null,
  default_subject_code varchar(32) null,
  allocate_rule varchar(32) default 'shop_month',
  status char(1) default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (fee_type_id),
  unique key uk_fin_fee_code (fee_code)
) engine=innodb default charset=utf8mb4 comment='财务费用类型';

create table if not exists fin_platform_settlement (
  settlement_id bigint not null auto_increment,
  settlement_no varchar(64) not null,
  platform_code varchar(32) not null,
  platform_name varchar(64) null,
  shop_id bigint null,
  shop_name varchar(128) null,
  period_code char(7) not null,
  bill_start_date date not null,
  bill_end_date date not null,
  income_amount decimal(14,2) default 0.00,
  refund_amount decimal(14,2) default 0.00,
  commission_fee decimal(14,2) default 0.00,
  payment_fee decimal(14,2) default 0.00,
  ad_fee decimal(14,2) default 0.00,
  service_fee decimal(14,2) default 0.00,
  freight_fee decimal(14,2) default 0.00,
  other_fee decimal(14,2) default 0.00,
  receivable_amount decimal(14,2) default 0.00,
  received_amount decimal(14,2) default 0.00,
  diff_amount decimal(14,2) default 0.00,
  settlement_status varchar(32) default 'draft',
  reconcile_status varchar(32) default 'unmatched',
  source_file varchar(255) null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (settlement_id),
  unique key uk_fin_settlement_no (settlement_no),
  key idx_fin_settlement_shop_period (shop_id, period_code),
  key idx_fin_settlement_status (settlement_status, reconcile_status)
) engine=innodb default charset=utf8mb4 comment='财务平台结算单';

create table if not exists fin_platform_settlement_item (
  item_id bigint not null auto_increment,
  settlement_id bigint not null,
  settlement_no varchar(64) not null,
  platform_order_no varchar(128) null,
  order_id bigint null,
  refund_no varchar(64) null,
  item_type varchar(32) not null,
  fee_type_code varchar(64) null,
  sku_code varchar(64) null,
  amount decimal(14,2) default 0.00,
  occurred_time datetime null,
  remark varchar(500) null,
  primary key (item_id),
  key idx_fin_settlement_item_bill (settlement_id),
  key idx_fin_settlement_item_order (platform_order_no, order_id),
  key idx_fin_settlement_item_type (item_type, fee_type_code)
) engine=innodb default charset=utf8mb4 comment='财务平台结算明细';

create table if not exists fin_receivable_bill (
  receivable_id bigint not null auto_increment,
  receivable_no varchar(64) not null,
  source_type varchar(32) not null,
  source_no varchar(64) not null,
  counterparty_type varchar(32) default 'platform',
  counterparty_id bigint null,
  counterparty_name varchar(128) null,
  shop_id bigint null,
  shop_name varchar(128) null,
  period_code char(7) not null,
  bill_amount decimal(14,2) default 0.00,
  writeoff_amount decimal(14,2) default 0.00,
  remain_amount decimal(14,2) default 0.00,
  due_date date null,
  bill_status varchar(32) default 'open',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (receivable_id),
  unique key uk_fin_receivable_no (receivable_no),
  key idx_fin_receivable_source (source_type, source_no),
  key idx_fin_receivable_status (period_code, bill_status)
) engine=innodb default charset=utf8mb4 comment='财务应收单';

create table if not exists fin_payable_bill (
  payable_id bigint not null auto_increment,
  payable_no varchar(64) not null,
  source_type varchar(32) not null,
  source_no varchar(64) not null,
  counterparty_type varchar(32) default 'supplier',
  counterparty_id bigint null,
  counterparty_name varchar(128) null,
  period_code char(7) not null,
  bill_amount decimal(14,2) default 0.00,
  writeoff_amount decimal(14,2) default 0.00,
  remain_amount decimal(14,2) default 0.00,
  due_date date null,
  bill_status varchar(32) default 'open',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (payable_id),
  unique key uk_fin_payable_no (payable_no),
  key idx_fin_payable_source (source_type, source_no),
  key idx_fin_payable_status (period_code, bill_status)
) engine=innodb default charset=utf8mb4 comment='财务应付单';

create table if not exists fin_cash_flow (
  cash_flow_id bigint not null auto_increment,
  flow_no varchar(64) not null,
  flow_type varchar(16) not null,
  bank_account_id bigint null,
  platform_account_id bigint null,
  counterparty_name varchar(128) null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  amount decimal(14,2) default 0.00,
  fee_amount decimal(14,2) default 0.00,
  flow_time datetime not null,
  business_date date not null,
  match_status varchar(32) default 'unmatched',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (cash_flow_id),
  unique key uk_fin_cash_flow_no (flow_no),
  key idx_fin_cash_flow_date (business_date, flow_type),
  key idx_fin_cash_flow_match (match_status)
) engine=innodb default charset=utf8mb4 comment='财务收付款流水';

create table if not exists fin_writeoff_record (
  writeoff_id bigint not null auto_increment,
  writeoff_no varchar(64) not null,
  bill_type varchar(16) not null,
  bill_no varchar(64) not null,
  cash_flow_no varchar(64) not null,
  writeoff_amount decimal(14,2) default 0.00,
  writeoff_time datetime not null,
  operator_name varchar(64) null,
  create_by varchar(64) default '',
  create_time datetime null,
  remark varchar(500) null,
  primary key (writeoff_id),
  unique key uk_fin_writeoff_no (writeoff_no),
  key idx_fin_writeoff_bill (bill_type, bill_no),
  key idx_fin_writeoff_cash (cash_flow_no)
) engine=innodb default charset=utf8mb4 comment='财务核销记录';

create table if not exists fin_expense_bill (
  expense_id bigint not null auto_increment,
  expense_no varchar(64) not null,
  fee_type_code varchar(64) not null,
  fee_type_name varchar(128) not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  sku_id bigint null,
  sku_code varchar(64) null,
  order_no varchar(128) null,
  supplier_id bigint null,
  supplier_name varchar(128) null,
  period_code char(7) not null,
  expense_amount decimal(14,2) default 0.00,
  tax_amount decimal(14,2) default 0.00,
  total_amount decimal(14,2) default 0.00,
  allocation_dimension varchar(32) default 'shop_month',
  expense_status varchar(32) default 'draft',
  occurred_date date not null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (expense_id),
  unique key uk_fin_expense_no (expense_no),
  key idx_fin_expense_period_shop (period_code, shop_id),
  key idx_fin_expense_type (fee_type_code)
) engine=innodb default charset=utf8mb4 comment='财务费用单';

create table if not exists fin_cost_ledger (
  cost_id bigint not null auto_increment,
  cost_no varchar(64) not null,
  sku_id bigint not null,
  sku_code varchar(64) not null,
  warehouse_id bigint null,
  source_type varchar(32) not null,
  source_no varchar(64) not null,
  cost_type varchar(32) not null,
  quantity int default 0,
  unit_cost decimal(12,4) default 0.0000,
  cost_amount decimal(14,2) default 0.00,
  business_time datetime not null,
  period_code char(7) not null,
  create_by varchar(64) default '',
  create_time datetime null,
  remark varchar(500) null,
  primary key (cost_id),
  unique key uk_fin_cost_no (cost_no),
  key idx_fin_cost_sku_period (sku_id, period_code),
  key idx_fin_cost_source (source_type, source_no)
) engine=innodb default charset=utf8mb4 comment='财务成本台账';

create table if not exists fin_invoice (
  invoice_id bigint not null auto_increment,
  invoice_no varchar(64) not null,
  invoice_type varchar(32) not null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  buyer_name varchar(128) null,
  seller_name varchar(128) null,
  amount decimal(14,2) default 0.00,
  tax_amount decimal(14,2) default 0.00,
  total_amount decimal(14,2) default 0.00,
  invoice_status varchar(32) default 'draft',
  issue_date date null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (invoice_id),
  unique key uk_fin_invoice_no (invoice_no),
  key idx_fin_invoice_source (source_type, source_no)
) engine=innodb default charset=utf8mb4 comment='财务发票';

create table if not exists fin_voucher (
  voucher_id bigint not null auto_increment,
  voucher_no varchar(64) not null,
  period_code char(7) not null,
  voucher_date date not null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  summary varchar(255) null,
  debit_amount decimal(14,2) default 0.00,
  credit_amount decimal(14,2) default 0.00,
  voucher_status varchar(32) default 'draft',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (voucher_id),
  unique key uk_fin_voucher_no (voucher_no),
  key idx_fin_voucher_period (period_code, voucher_status),
  key idx_fin_voucher_source (source_type, source_no)
) engine=innodb default charset=utf8mb4 comment='财务凭证';

create table if not exists fin_voucher_entry (
  entry_id bigint not null auto_increment,
  voucher_id bigint not null,
  voucher_no varchar(64) not null,
  entry_seq int not null,
  subject_code varchar(32) not null,
  subject_name varchar(128) not null,
  direction varchar(16) not null,
  amount decimal(14,2) default 0.00,
  shop_id bigint null,
  sku_id bigint null,
  counterparty_name varchar(128) null,
  summary varchar(255) null,
  primary key (entry_id),
  key idx_fin_voucher_entry_voucher (voucher_id),
  key idx_fin_voucher_entry_subject (subject_code)
) engine=innodb default charset=utf8mb4 comment='财务凭证分录';

create table if not exists fin_reconciliation_task (
  task_id bigint not null auto_increment,
  task_no varchar(64) not null,
  reconcile_type varchar(32) not null,
  period_code char(7) not null,
  shop_id bigint null,
  shop_name varchar(128) null,
  task_status varchar(32) default 'created',
  total_count int default 0,
  diff_count int default 0,
  start_time datetime null,
  finish_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (task_id),
  unique key uk_fin_reconcile_task_no (task_no),
  key idx_fin_reconcile_task_period (period_code, reconcile_type)
) engine=innodb default charset=utf8mb4 comment='财务对账任务';

create table if not exists fin_reconciliation_diff (
  diff_id bigint not null auto_increment,
  task_id bigint not null,
  diff_no varchar(64) not null,
  diff_type varchar(32) not null,
  source_type varchar(32) null,
  source_no varchar(64) null,
  expected_amount decimal(14,2) default 0.00,
  actual_amount decimal(14,2) default 0.00,
  diff_amount decimal(14,2) default 0.00,
  diff_status varchar(32) default 'open',
  handle_result varchar(255) null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (diff_id),
  unique key uk_fin_reconcile_diff_no (diff_no),
  key idx_fin_reconcile_diff_task (task_id),
  key idx_fin_reconcile_diff_status (diff_status)
) engine=innodb default charset=utf8mb4 comment='财务对账差异';

create table if not exists fin_period_close (
  close_id bigint not null auto_increment,
  period_code char(7) not null,
  close_scope varchar(32) default 'company',
  check_status varchar(32) default 'unchecked',
  close_status varchar(32) default 'open',
  order_complete char(1) default '0',
  settlement_complete char(1) default '0',
  bank_complete char(1) default '0',
  voucher_complete char(1) default '0',
  close_by varchar(64) default '',
  close_time datetime null,
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (close_id),
  unique key uk_fin_period_close (period_code, close_scope)
) engine=innodb default charset=utf8mb4 comment='财务期间结账';

insert into fin_account_period(period_code, start_date, end_date, close_status, create_by, create_time)
select '2026-06', '2026-06-01', '2026-06-30', 'open', 'admin', sysdate()
where not exists (select 1 from fin_account_period where period_code = '2026-06');

insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '1002', '银行存款', 'asset', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '1002');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '1122', '应收账款', 'asset', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '1122');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '1405', '库存商品', 'asset', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '1405');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '2202', '应付账款', 'liability', null, 'credit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '2202');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '2221', '应交税费', 'liability', null, 'credit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '2221');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '6001', '主营业务收入', 'revenue', null, 'credit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '6001');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '6401', '主营业务成本', 'cost', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '6401');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '6601', '销售费用', 'expense', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '6601');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '6602', '管理费用', 'expense', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '6602');
insert into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time)
select '6603', '财务费用', 'expense', null, 'debit', '0', 'admin', sysdate()
where not exists (select 1 from fin_account_subject where subject_code = '6603');

insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'platform_commission', '平台佣金', 'platform', '6601', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'platform_commission');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'payment_fee', '支付手续费', 'platform', '6603', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'payment_fee');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'ad_fee', '推广费用', 'ad', '6601', 'shop_day', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'ad_fee');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'influencer_commission', '达人佣金', 'ad', '6601', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'influencer_commission');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'freight_fee', '运费', 'logistics', '6601', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'freight_fee');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'warehouse_fee', '仓储费', 'warehouse', '6601', 'shop_month', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'warehouse_fee');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'package_fee', '包装耗材', 'warehouse', '6601', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'package_fee');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'after_sale_compensation', '售后补偿', 'after_sale', '6601', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'after_sale_compensation');
insert into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time)
select 'refund_loss', '退款损失', 'after_sale', '6601', 'order', '0', 'admin', sysdate()
where not exists (select 1 from fin_fee_type where fee_code = 'refund_loss');

insert into fin_platform_account(platform_code, platform_name, shop_id, shop_name, account_no, currency, status, create_by, create_time, remark)
select coalesce(s.platform_code, 'douyin'), coalesce(s.platform_name, '抖音电商'), s.shop_id, s.shop_name, concat('PLAT-', s.shop_code), 'CNY', '0', 'admin', sysdate(), '初始化平台账户'
from erp_shop s
where s.shop_code = 'SHOP-DEMO-001'
  and not exists (select 1 from fin_platform_account where shop_id = s.shop_id and account_no = concat('PLAT-', s.shop_code));

insert into fin_bank_account(account_name, bank_name, bank_no, currency, opening_balance, status, create_by, create_time, remark)
select '公司基本户', '演示银行', 'BANK-DEMO-001', 'CNY', 0.00, '0', 'admin', sysdate(), '初始化银行账户'
where not exists (select 1 from fin_bank_account where bank_no = 'BANK-DEMO-001');

insert into fin_period_close(period_code, close_scope, check_status, close_status, create_by, create_time)
select '2026-06', 'company', 'unchecked', 'open', 'admin', sysdate()
where not exists (select 1 from fin_period_close where period_code = '2026-06' and close_scope = 'company');
