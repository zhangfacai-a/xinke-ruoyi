-- 采购运单匹配系统
-- 适用项目：xinke / RuoYi Spring Boot 3

create table if not exists purchase_supplier (
  supplier_id bigint not null auto_increment comment '供应商ID',
  supplier_name varchar(120) not null comment '供应商名称',
  supplier_code varchar(64) default null comment '供应商编码',
  status char(1) default '0' comment '状态（0正常 1停用）',
  sort_order int default 0 comment '排序',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default null comment '备注',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (supplier_id),
  unique key uk_purchase_supplier_name (supplier_name, del_flag),
  key idx_purchase_supplier_status (status)
) engine=InnoDB default charset=utf8mb4 comment='采购供应商表';

create table if not exists purchase_supplier_alias (
  alias_id bigint not null auto_increment comment '别名ID',
  supplier_id bigint not null comment '供应商ID',
  alias_name varchar(120) not null comment '供应商别名',
  status char(1) default '0' comment '状态（0正常 1停用）',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default null comment '备注',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (alias_id),
  unique key uk_purchase_supplier_alias (alias_name, del_flag),
  key idx_purchase_supplier_alias_supplier (supplier_id)
) engine=InnoDB default charset=utf8mb4 comment='采购供应商别名表';

create table if not exists purchase_manual_order (
  manual_order_id bigint not null auto_increment comment '手工订单ID',
  supplier_id bigint not null comment '供应商ID',
  supplier_name_snapshot varchar(120) not null comment '供应商名称快照',
  active_unique_key varchar(220) not null comment '有效唯一键',
  order_date date default null comment '日期',
  order_no varchar(100) not null comment '订单号',
  product_model varchar(200) default null comment '型号',
  unit_price decimal(15,2) default null comment '单价',
  quantity decimal(15,2) default null comment '数量',
  total_amount decimal(15,2) default null comment '金额',
  customer_name varchar(120) default null comment '姓名',
  customer_phone varchar(80) default null comment '电话',
  customer_address varchar(500) default null comment '地址',
  order_remark varchar(1000) default null comment '备注',
  logistics_no varchar(120) default null comment '物流单号',
  after_sale_flag varchar(32) default null comment '是否售后',
  after_sale_request varchar(500) default null comment '售后申请',
  balance_amount decimal(15,2) default null comment '余款',
  receipt_status varchar(100) default null comment '回单状态',
  payment_amount decimal(15,2) default null comment '打款金额',
  invoice_status varchar(100) default null comment '开票情况',
  reconciliation_status varchar(100) default null comment '对账情况',
  amount_check_status varchar(32) default 'OK' comment '金额校验状态',
  data_status varchar(32) default 'NORMAL' comment '数据状态',
  upload_user_id bigint default null comment '首次上传人ID',
  upload_user_name varchar(100) default null comment '首次上传人姓名',
  upload_time datetime default null comment '首次上传时间',
  latest_upload_user_id bigint default null comment '最近上传人ID',
  latest_upload_user_name varchar(100) default null comment '最近上传人姓名',
  latest_upload_time datetime default null comment '最近上传时间',
  source_file_name varchar(255) default null comment '首次来源文件名',
  latest_source_file_name varchar(255) default null comment '最近来源文件名',
  import_batch_id bigint default null comment '首次导入批次ID',
  latest_import_batch_id bigint default null comment '最近导入批次ID',
  source_row_number int default null comment '首次原文件行号',
  latest_source_row_number int default null comment '最近原文件行号',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default null comment '备注',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (manual_order_id),
  unique key uk_purchase_manual_active (active_unique_key),
  key idx_purchase_manual_order_no (order_no),
  key idx_purchase_manual_logistics (logistics_no),
  key idx_purchase_manual_date (order_date),
  key idx_purchase_manual_upload_time (upload_time),
  key idx_purchase_manual_upload_user (upload_user_id),
  key idx_purchase_manual_supplier_order (supplier_id, order_no)
) engine=InnoDB default charset=utf8mb4 comment='采购手工订单总表';

create table if not exists purchase_summary (
  summary_id bigint not null auto_increment comment '采购汇总ID',
  document_supplier_id bigint default null comment '单据供应商ID',
  document_supplier_name varchar(120) not null comment '单据供应商',
  active_unique_key varchar(360) not null comment '有效唯一键',
  purchase_order_remark varchar(150) not null comment '采购单备注',
  contract_remark varchar(500) default null comment '合同备注',
  merchant_code varchar(150) not null comment '商家编码',
  purchase_quantity decimal(15,2) default null comment '采购数量',
  matched_logistics_no varchar(120) default null comment '匹配运单号',
  original_logistics_no varchar(120) default null comment '原运单号',
  shipping_check_status varchar(100) default null comment '核查发货',
  exception_reason varchar(500) default null comment '异常',
  purchaser_remark varchar(500) default null comment '采购人员/备注',
  matched_order_id bigint default null comment '匹配手工订单ID',
  match_status varchar(32) default 'PENDING' comment '匹配状态',
  match_time datetime default null comment '匹配时间',
  match_type varchar(32) default null comment '匹配类型',
  match_message varchar(500) default null comment '匹配说明',
  last_match_time datetime default null comment '最后匹配时间',
  data_status varchar(32) default 'NORMAL' comment '数据状态',
  upload_user_id bigint default null comment '首次上传人ID',
  upload_user_name varchar(100) default null comment '首次上传人姓名',
  upload_time datetime default null comment '首次上传时间',
  latest_upload_user_id bigint default null comment '最近上传人ID',
  latest_upload_user_name varchar(100) default null comment '最近上传人姓名',
  latest_upload_time datetime default null comment '最近上传时间',
  source_file_name varchar(255) default null comment '首次来源文件名',
  latest_source_file_name varchar(255) default null comment '最近来源文件名',
  import_batch_id bigint default null comment '首次导入批次ID',
  latest_import_batch_id bigint default null comment '最近导入批次ID',
  source_row_number int default null comment '首次原文件行号',
  latest_source_row_number int default null comment '最近原文件行号',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default null comment '备注',
  del_flag char(1) default '0' comment '删除标志（0存在 2删除）',
  primary key (summary_id),
  unique key uk_purchase_summary_active (active_unique_key),
  key idx_purchase_summary_remark (purchase_order_remark),
  key idx_purchase_summary_merchant (merchant_code),
  key idx_purchase_summary_logistics (matched_logistics_no),
  key idx_purchase_summary_status (match_status),
  key idx_purchase_summary_upload_time (upload_time),
  key idx_purchase_summary_upload_user (upload_user_id),
  key idx_purchase_summary_key (document_supplier_id, purchase_order_remark)
) engine=InnoDB default charset=utf8mb4 comment='采购汇总总表';

create table if not exists purchase_import_batch (
  batch_id bigint not null auto_increment comment '批次ID',
  batch_no varchar(80) not null comment '批次号',
  import_type varchar(32) not null comment '导入类型',
  supplier_id bigint default null comment '手工订单供应商ID',
  original_file_name varchar(255) default null comment '原始文件名',
  file_hash varchar(128) default null comment '文件Hash',
  sheet_name varchar(120) default null comment 'Sheet名称',
  total_rows int default 0 comment '总行数',
  success_rows int default 0 comment '成功数',
  duplicate_rows int default 0 comment '重复数',
  updated_rows int default 0 comment '补充更新数',
  conflict_rows int default 0 comment '冲突数',
  failed_rows int default 0 comment '失败数',
  match_success_rows int default 0 comment '匹配成功数',
  batch_status varchar(32) default 'PROCESSING' comment '批次状态',
  error_message varchar(1000) default null comment '错误信息',
  upload_user_id bigint default null comment '上传人ID',
  upload_user_name varchar(100) default null comment '上传人姓名',
  upload_time datetime default current_timestamp comment '上传时间',
  finish_time datetime default null comment '完成时间',
  create_time datetime default current_timestamp comment '创建时间',
  primary key (batch_id),
  unique key uk_purchase_import_batch_no (batch_no),
  key idx_purchase_import_type_time (import_type, upload_time)
) engine=InnoDB default charset=utf8mb4 comment='采购导入批次表';

create table if not exists purchase_import_detail (
  detail_id bigint not null auto_increment comment '明细ID',
  batch_id bigint not null comment '批次ID',
  source_row_number int default null comment '原文件行号',
  business_key varchar(500) default null comment '业务键',
  raw_data_json json default null comment '原始行JSON',
  target_table varchar(80) default null comment '目标表',
  target_record_id bigint default null comment '目标记录ID',
  check_status varchar(32) default null comment '检查状态',
  process_result varchar(32) default null comment '处理结果',
  error_code varchar(64) default null comment '错误码',
  error_message varchar(1000) default null comment '错误信息',
  before_data_json json default null comment '处理前JSON',
  after_data_json json default null comment '处理后JSON',
  upload_user_id bigint default null comment '上传人ID',
  upload_user_name varchar(100) default null comment '上传人姓名',
  upload_time datetime default current_timestamp comment '上传时间',
  create_time datetime default current_timestamp comment '创建时间',
  primary key (detail_id),
  key idx_purchase_detail_batch (batch_id),
  key idx_purchase_detail_result (process_result)
) engine=InnoDB default charset=utf8mb4 comment='采购逐行导入明细表';

create table if not exists purchase_data_conflict (
  conflict_id bigint not null auto_increment comment '冲突ID',
  conflict_type varchar(32) not null comment '冲突类型',
  target_table varchar(80) not null comment '目标表',
  target_record_id bigint not null comment '目标记录ID',
  batch_id bigint default null comment '批次ID',
  detail_id bigint default null comment '明细ID',
  business_key varchar(500) default null comment '业务键',
  old_data_json json default null comment '旧数据JSON',
  new_data_json json default null comment '新数据JSON',
  conflict_fields varchar(1000) default null comment '冲突字段',
  conflict_status varchar(32) default 'PENDING' comment '冲突状态',
  resolve_result varchar(32) default null comment '处理结果',
  resolve_user_id bigint default null comment '处理人ID',
  resolve_user_name varchar(100) default null comment '处理人',
  resolve_time datetime default null comment '处理时间',
  create_by varchar(64) default '' comment '创建者',
  create_time datetime default current_timestamp comment '创建时间',
  update_by varchar(64) default '' comment '更新者',
  update_time datetime default current_timestamp on update current_timestamp comment '更新时间',
  remark varchar(500) default null comment '备注',
  primary key (conflict_id),
  key idx_purchase_conflict_status (conflict_status),
  key idx_purchase_conflict_target (target_table, target_record_id)
) engine=InnoDB default charset=utf8mb4 comment='采购数据冲突表';

create table if not exists purchase_match_log (
  log_id bigint not null auto_increment comment '匹配日志ID',
  summary_id bigint default null comment '采购汇总ID',
  manual_order_id bigint default null comment '手工订单ID',
  supplier_id bigint default null comment '供应商ID',
  order_no varchar(150) default null comment '订单号',
  old_logistics_no varchar(120) default null comment '旧物流单号',
  new_logistics_no varchar(120) default null comment '新物流单号',
  match_status varchar(32) default null comment '匹配状态',
  match_type varchar(32) default null comment '匹配类型',
  match_message varchar(500) default null comment '匹配说明',
  operator_id bigint default null comment '操作人ID',
  operator_name varchar(100) default null comment '操作人',
  create_time datetime default current_timestamp comment '创建时间',
  primary key (log_id),
  key idx_purchase_match_summary (summary_id),
  key idx_purchase_match_key (supplier_id, order_no),
  key idx_purchase_match_time (create_time)
) engine=InnoDB default charset=utf8mb4 comment='采购匹配日志表';

create table if not exists purchase_field_change_log (
  log_id bigint not null auto_increment comment '字段变更ID',
  target_table varchar(80) not null comment '目标表',
  target_record_id bigint not null comment '目标记录ID',
  field_name varchar(100) not null comment '字段名',
  old_value text comment '旧值',
  new_value text comment '新值',
  change_type varchar(32) default 'UPDATE' comment '变更类型',
  batch_id bigint default null comment '批次ID',
  operator_id bigint default null comment '操作人ID',
  operator_name varchar(100) default null comment '操作人',
  create_time datetime default current_timestamp comment '创建时间',
  primary key (log_id),
  key idx_purchase_change_target (target_table, target_record_id),
  key idx_purchase_change_batch (batch_id)
) engine=InnoDB default charset=utf8mb4 comment='采购字段变更日志表';

insert ignore into purchase_supplier(supplier_name, supplier_code, sort_order, create_by)
values
('三休','SX',1,'system'),
('太原中硕','TYZS',2,'system'),
('河南云品通','HNYP',3,'system'),
('苏宁有货','SNYH',4,'system'),
('江苏特选','JSTX',5,'system'),
('揭阳市智艺电器有限公司','JYZY',6,'system'),
('爱净','AJ',7,'system'),
('湖南宇超','HNYC',8,'system'),
('济宁美人鱼信息科技有限公司','JNMYR',9,'system'),
('河南麦旺','HNMW',10,'system');

insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '采购匹配状态','purchase_match_status','0','admin',sysdate(),'采购汇总运单匹配状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_match_status');
insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 1,'匹配成功','SUCCESS','purchase_match_status',null,'success','N','0','admin',sysdate(),null where not exists (select 1 from sys_dict_data where dict_type='purchase_match_status' and dict_value='SUCCESS');
insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 2,'未找到订单','NOT_FOUND','purchase_match_status',null,'danger','N','0','admin',sysdate(),null where not exists (select 1 from sys_dict_data where dict_type='purchase_match_status' and dict_value='NOT_FOUND');
insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 3,'物流为空','LOGISTICS_EMPTY','purchase_match_status',null,'warning','N','0','admin',sysdate(),null where not exists (select 1 from sys_dict_data where dict_type='purchase_match_status' and dict_value='LOGISTICS_EMPTY');
insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 4,'供应商未识别','SUPPLIER_NOT_FOUND','purchase_match_status',null,'info','N','0','admin',sysdate(),null where not exists (select 1 from sys_dict_data where dict_type='purchase_match_status' and dict_value='SUPPLIER_NOT_FOUND');
insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 5,'冲突','CONFLICT','purchase_match_status',null,'danger','N','0','admin',sysdate(),null where not exists (select 1 from sys_dict_data where dict_type='purchase_match_status' and dict_value='CONFLICT');
insert into sys_dict_data(dict_sort, dict_label, dict_value, dict_type, css_class, list_class, is_default, status, create_by, create_time, remark)
select 6,'待匹配','PENDING','purchase_match_status',null,'primary','Y','0','admin',sysdate(),null where not exists (select 1 from sys_dict_data where dict_type='purchase_match_status' and dict_value='PENDING');

insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '采购导入状态','purchase_import_status','0','admin',sysdate(),'采购导入批次状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_import_status');
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '采购导入行状态','purchase_import_row_status','0','admin',sysdate(),'采购导入逐行处理状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_import_row_status');
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '采购冲突状态','purchase_conflict_status','0','admin',sysdate(),'采购数据冲突状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_conflict_status');
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '采购数据状态','purchase_data_status','0','admin',sysdate(),'采购数据状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_data_status');

insert into purchase_manual_order(
 supplier_id, supplier_name_snapshot, active_unique_key, order_date, order_no, product_model, unit_price, quantity, total_amount,
 customer_name, customer_phone, customer_address, logistics_no, amount_check_status, data_status,
 upload_user_id, upload_user_name, upload_time, latest_upload_user_id, latest_upload_user_name, latest_upload_time,
 source_file_name, latest_source_file_name, import_batch_id, latest_import_batch_id, source_row_number, latest_source_row_number, create_by)
select supplier_id, supplier_name, concat(supplier_id,'|DEMO-ORDER-001'), curdate(), 'DEMO-ORDER-001', '演示型号', 100, 2, 200,
 '演示客户', '138-0000-0000', '演示地址', 'SF-DEMO-001', 'OK', 'NORMAL',
 1, 'admin', sysdate(), 1, 'admin', sysdate(), 'demo', 'demo', 0, 0, 2, 2, 'admin'
from purchase_supplier where supplier_name='三休'
and not exists (select 1 from purchase_manual_order where order_no='DEMO-ORDER-001');

insert into purchase_summary(
 document_supplier_id, document_supplier_name, active_unique_key, purchase_order_remark, merchant_code, purchase_quantity,
 match_status, data_status, upload_user_id, upload_user_name, upload_time, latest_upload_user_id, latest_upload_user_name,
 latest_upload_time, source_file_name, latest_source_file_name, import_batch_id, latest_import_batch_id, source_row_number,
 latest_source_row_number, create_by)
select supplier_id, supplier_name, concat(supplier_id,'|DEMO-ORDER-001|SKU-DEMO-001'), 'DEMO-ORDER-001', 'SKU-DEMO-001', 2,
 'PENDING', 'NORMAL', 1, 'admin', sysdate(), 1, 'admin', sysdate(), 'demo', 'demo', 0, 0, 2, 2, 'admin'
from purchase_supplier where supplier_name='三休'
and not exists (select 1 from purchase_summary where purchase_order_remark='DEMO-ORDER-001' and merchant_code='SKU-DEMO-001');

-- 菜单权限：按当前 sys_menu 最大ID顺延，重复执行不会重复插入。
set @purchase_root := (select menu_id from sys_menu where perms = 'purchase:root' limit 1);
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
select '采购匹配', 0, 8, 'purchase', null, 1, 0, 'M', '0', '0', 'purchase:root', 'shopping', 'admin', sysdate()
where @purchase_root is null;
set @purchase_root := (select menu_id from sys_menu where perms = 'purchase:root' limit 1);
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
select '手工订单总表', @purchase_root, 2, 'manual', 'erp/purchaseMatch/manual/index', 1, 0, 'C', '0', '0', 'purchase:manual:list', 'list', 'admin', sysdate()
where not exists (select 1 from sys_menu where perms='purchase:manual:list');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
select '采购汇总总表', @purchase_root, 3, 'summary', 'erp/purchaseMatch/summary/index', 1, 0, 'C', '0', '0', 'purchase:summary:list', 'table', 'admin', sysdate()
where not exists (select 1 from sys_menu where perms='purchase:summary:list');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
select '数据异常', @purchase_root, 4, 'conflict', 'erp/purchaseMatch/conflict/index', 1, 0, 'C', '0', '0', 'purchase:conflict:list', 'bug', 'admin', sysdate()
where not exists (select 1 from sys_menu where perms='purchase:conflict:list');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
select '导入记录', @purchase_root, 5, 'import', 'erp/purchaseMatch/import/index', 1, 0, 'C', '0', '0', 'purchase:import:query', 'log', 'admin', sysdate()
where not exists (select 1 from sys_menu where perms='purchase:import:query');
insert into sys_menu(menu_name, parent_id, order_num, path, component, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time)
select '供应商设置', @purchase_root, 7, 'supplier', 'erp/purchaseMatch/supplier/index', 1, 0, 'C', '0', '0', 'purchase:supplier:list', 'peoples', 'admin', sysdate()
where not exists (select 1 from sys_menu where perms='purchase:supplier:list');

-- 补充按钮权限
set @menu_supplier := (select menu_id from sys_menu where perms='purchase:supplier:list' limit 1);
set @menu_manual := (select menu_id from sys_menu where perms='purchase:manual:list' limit 1);
set @menu_summary := (select menu_id from sys_menu where perms='purchase:summary:list' limit 1);
set @menu_conflict := (select menu_id from sys_menu where perms='purchase:conflict:list' limit 1);
set @menu_import := (select menu_id from sys_menu where perms='purchase:import:query' limit 1);

insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '供应商新增',@menu_supplier,1,'#','',1,0,'F','0','0','purchase:supplier:add','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:supplier:add');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '供应商编辑',@menu_supplier,2,'#','',1,0,'F','0','0','purchase:supplier:edit','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:supplier:edit');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '供应商删除',@menu_supplier,3,'#','',1,0,'F','0','0','purchase:supplier:remove','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:supplier:remove');

insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '手工订单新增',@menu_manual,1,'#','',1,0,'F','0','0','purchase:manual:add','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:manual:add');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '手工订单编辑',@menu_manual,2,'#','',1,0,'F','0','0','purchase:manual:edit','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:manual:edit');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '手工订单删除',@menu_manual,3,'#','',1,0,'F','0','0','purchase:manual:remove','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:manual:remove');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '手工订单导入',@menu_manual,4,'#','',1,0,'F','0','0','purchase:manual:import','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:manual:import');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '手工订单导出',@menu_manual,5,'#','',1,0,'F','0','0','purchase:manual:export','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:manual:export');

insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '采购汇总编辑',@menu_summary,1,'#','',1,0,'F','0','0','purchase:summary:edit','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:summary:edit');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '采购汇总删除',@menu_summary,2,'#','',1,0,'F','0','0','purchase:summary:remove','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:summary:remove');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '采购汇总导入',@menu_summary,3,'#','',1,0,'F','0','0','purchase:summary:import','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:summary:import');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '采购汇总导出',@menu_summary,4,'#','',1,0,'F','0','0','purchase:summary:export','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:summary:export');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '执行匹配',@menu_summary,5,'#','',1,0,'F','0','0','purchase:match:execute','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:match:execute');
insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '手工匹配',@menu_summary,6,'#','',1,0,'F','0','0','purchase:match:manual','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:match:manual');

insert into sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
select '冲突处理',@menu_conflict,1,'#','',1,0,'F','0','0','purchase:conflict:resolve','#','admin',sysdate()
where not exists (select 1 from sys_menu where perms='purchase:conflict:resolve');
-- 补充字典数据
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '是否售后','purchase_after_sale_flag','0','admin',sysdate(),'采购手工订单是否售后'
where not exists (select 1 from sys_dict_type where dict_type='purchase_after_sale_flag');
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '回单状态','purchase_receipt_status','0','admin',sysdate(),'采购回单状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_receipt_status');
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '开票状态','purchase_invoice_status','0','admin',sysdate(),'采购开票状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_invoice_status');
insert into sys_dict_type(dict_name, dict_type, status, create_by, create_time, remark)
select '对账状态','purchase_reconciliation_status','0','admin',sysdate(),'采购对账状态'
where not exists (select 1 from sys_dict_type where dict_type='purchase_reconciliation_status');

insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 1,'否','N','purchase_after_sale_flag','info','Y','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_after_sale_flag' and dict_value='N');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 2,'是','Y','purchase_after_sale_flag','warning','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_after_sale_flag' and dict_value='Y');

insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 1,'处理中','PROCESSING','purchase_import_status','primary','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_status' and dict_value='PROCESSING');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 2,'成功','SUCCESS','purchase_import_status','success','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_status' and dict_value='SUCCESS');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 3,'部分完成','PARTIAL_SUCCESS','purchase_import_status','warning','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_status' and dict_value='PARTIAL_SUCCESS');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 4,'失败','FAILED','purchase_import_status','danger','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_status' and dict_value='FAILED');

insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 1,'新增','INSERTED','purchase_import_row_status','success','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_row_status' and dict_value='INSERTED');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 2,'重复无变化','DUPLICATE','purchase_import_row_status','info','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_row_status' and dict_value='DUPLICATE');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 3,'补充空字段','FILLED_EMPTY_FIELDS','purchase_import_row_status','primary','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_row_status' and dict_value='FILLED_EMPTY_FIELDS');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 4,'冲突','CONFLICT','purchase_import_row_status','warning','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_row_status' and dict_value='CONFLICT');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 5,'无效','INVALID','purchase_import_row_status','danger','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_row_status' and dict_value='INVALID');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 6,'失败','FAILED','purchase_import_row_status','danger','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_import_row_status' and dict_value='FAILED');

insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 1,'待处理','PENDING','purchase_conflict_status','warning','Y','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_conflict_status' and dict_value='PENDING');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 2,'已处理','RESOLVED','purchase_conflict_status','success','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_conflict_status' and dict_value='RESOLVED');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 3,'已忽略','IGNORED','purchase_conflict_status','info','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_conflict_status' and dict_value='IGNORED');

insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 1,'正常','NORMAL','purchase_data_status','success','Y','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_data_status' and dict_value='NORMAL');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 2,'冲突','CONFLICT','purchase_data_status','warning','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_data_status' and dict_value='CONFLICT');
insert into sys_dict_data(dict_sort,dict_label,dict_value,dict_type,list_class,is_default,status,create_by,create_time)
select 3,'忽略','IGNORED','purchase_data_status','info','N','0','admin',sysdate()
where not exists (select 1 from sys_dict_data where dict_type='purchase_data_status' and dict_value='IGNORED');

-- 业务查询视图：保留业务表中的稳定状态码，同时提供中文状态列，方便直接用SQL查数。
create or replace view v_purchase_manual_order_cn as
select
  pmo.*,
  case pmo.data_status
    when 'NORMAL' then '正常'
    when 'CONFLICT' then '冲突'
    when 'IGNORED' then '已忽略'
    else pmo.data_status
  end as data_status_name,
  case pmo.amount_check_status
    when 'OK' then '金额正常'
    when 'AMOUNT_MISMATCH' then '金额异常'
    else pmo.amount_check_status
  end as amount_check_status_name
from purchase_manual_order pmo;

create or replace view v_purchase_summary_cn as
select
  ps.*,
  case ps.match_status
    when 'SUCCESS' then '匹配成功'
    when 'NOT_FOUND' then '未找到订单'
    when 'LOGISTICS_EMPTY' then '物流为空'
    when 'MULTIPLE' then '多条匹配'
    when 'SUPPLIER_NOT_FOUND' then '供应商未识别'
    when 'CONFLICT' then '数据冲突'
    when 'PENDING' then '待匹配'
    else ps.match_status
  end as match_status_name,
  case ps.data_status
    when 'NORMAL' then '正常'
    when 'CONFLICT' then '冲突'
    when 'IGNORED' then '已忽略'
    else ps.data_status
  end as data_status_name
from purchase_summary ps;

-- 不保留原始Excel文件：移除历史批次中的服务器文件路径字段。
set @drop_stored_file_name := if(
  (select count(1) from information_schema.columns where table_schema=database() and table_name='purchase_import_batch' and column_name='stored_file_name') > 0,
  'alter table purchase_import_batch drop column stored_file_name',
  'select 1'
);
prepare stmt from @drop_stored_file_name; execute stmt; deallocate prepare stmt;
set @drop_file_path := if(
  (select count(1) from information_schema.columns where table_schema=database() and table_name='purchase_import_batch' and column_name='file_path') > 0,
  'alter table purchase_import_batch drop column file_path',
  'select 1'
);
prepare stmt from @drop_file_path; execute stmt; deallocate prepare stmt;

-- 回滚说明：
-- 1. 若需回滚业务表：drop table purchase_field_change_log, purchase_match_log, purchase_data_conflict,
--    purchase_import_detail, purchase_import_batch, purchase_summary, purchase_manual_order,
--    purchase_supplier_alias, purchase_supplier;
-- 2. 若需回滚菜单和字典：按 perms like 'purchase:%' 删除 sys_menu，按 dict_type like 'purchase_%' 删除 sys_dict_data/sys_dict_type。
