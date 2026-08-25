-- 直播订单礼品管理。执行前请先备份数据库；适用于 MySQL 8.0+。

create table if not exists live_ding_dept_bind (
  ding_dept_id bigint not null, sys_dept_id bigint not null,
  parent_ding_dept_id bigint not null default 0, dept_name varchar(100) not null,
  sync_batch varchar(64) not null, last_sync_time datetime not null,
  create_time datetime default current_timestamp,
  primary key (ding_dept_id), unique key uk_ding_dept_sys (sys_dept_id),
  key idx_ding_dept_batch (sync_batch)
) engine=InnoDB default charset=utf8mb4 comment='钉钉部门与系统部门绑定';

create table if not exists live_ding_user_bind (
  ding_user_id varchar(128) not null, user_id bigint not null,
  sync_batch varchar(64) not null, last_sync_time datetime not null,
  create_time datetime default current_timestamp,
  primary key (ding_user_id), key idx_ding_user_sys (user_id),
  key idx_ding_user_batch (sync_batch)
) engine=InnoDB default charset=utf8mb4 comment='钉钉人员与系统用户绑定';

create table if not exists live_room (
  room_id bigint not null auto_increment, room_code varchar(64) not null,
  room_name varchar(100) not null, shop_id bigint not null,
  platform_room_id varchar(128) null, live_account varchar(128) null,
  owner_staff_id bigint null, status char(1) not null default '0',
  create_by varchar(64) default '', create_time datetime default current_timestamp,
  update_by varchar(64) default '', update_time datetime null, remark varchar(500) null,
  primary key (room_id), unique key uk_live_room_code (room_code), unique key uk_live_room_name (room_name),
  key idx_live_room_shop (shop_id, status)
) engine=InnoDB default charset=utf8mb4 comment='直播间';

create table if not exists live_daily_record (
  daily_id bigint not null auto_increment, live_date date not null,
  shop_id bigint not null, room_id bigint not null,
  create_by varchar(64) default '', create_time datetime default current_timestamp,
  update_by varchar(64) default '', update_time datetime null, remark varchar(500) null,
  primary key (daily_id), unique key uk_live_daily (live_date, shop_id, room_id),
  key idx_live_daily_date (live_date)
) engine=InnoDB default charset=utf8mb4 comment='每日直播登记';

create table if not exists live_daily_user (
  daily_id bigint not null, user_id bigint not null, role_code varchar(32) not null,
  primary key (daily_id, user_id, role_code), key idx_daily_user (user_id, role_code)
) engine=InnoDB default charset=utf8mb4 comment='每日直播系统用户';

create table if not exists erp_gift (
  gift_id bigint not null auto_increment, gift_code varchar(32) not null,
  gift_name varchar(120) not null, short_name varchar(60) null,
  brand varchar(60) null, model varchar(80) null, specification varchar(120) null,
  unit varchar(20) not null default '件', category varchar(60) null,
  image_url varchar(500) null, purchase_type varchar(30) null,
  status char(1) not null default '0', sort_order int not null default 100,
  create_by varchar(64) default '', create_time datetime default current_timestamp,
  update_by varchar(64) default '', update_time datetime null, remark varchar(500) null,
  primary key (gift_id), unique key uk_erp_gift_code (gift_code),
  key idx_erp_gift_name (gift_name), key idx_erp_gift_status (status)
) engine=InnoDB default charset=utf8mb4 comment='礼品档案';

create table if not exists erp_gift_alias (
  alias_id bigint not null auto_increment, gift_id bigint not null,
  alias_name varchar(120) not null, primary key (alias_id),
  unique key uk_gift_alias (gift_id, alias_name), key idx_alias_name (alias_name)
) engine=InnoDB default charset=utf8mb4 comment='礼品搜索别名';

create table if not exists live_user_gift_preference (
  user_id bigint not null, gift_id bigint not null,
  hidden tinyint(1) not null default 0, pinned tinyint(1) not null default 0,
  sort_order int not null default 0,
  update_by varchar(64) default '', update_time datetime default current_timestamp,
  primary key (user_id, gift_id), key idx_user_gift_preference_gift (gift_id),
  key idx_user_gift_preference_order (user_id, pinned, sort_order)
) engine=InnoDB default charset=utf8mb4 comment='用户个人礼品显示与置顶偏好';

create table if not exists erp_gift_cost (
  cost_id bigint not null auto_increment, gift_id bigint not null,
  unit_cost decimal(12,2) not null, effective_date date not null,
  change_reason varchar(500) null, create_by varchar(64) default '',
  create_time datetime default current_timestamp,
  primary key (cost_id), unique key uk_gift_cost_date (gift_id, effective_date),
  key idx_gift_cost_lookup (gift_id, effective_date)
) engine=InnoDB default charset=utf8mb4 comment='礼品历史成本';

create table if not exists erp_order_gift_status (
  order_no varchar(64) not null, process_status varchar(30) not null default 'selected',
  daily_id bigint null, operator_note varchar(500) null,
  anchor_user_id bigint null, anchor_name_snapshot varchar(120) null,
  controller_user_id bigint null, controller_name_snapshot varchar(120) null,
  refund_amount decimal(12,2) null, refund_reason varchar(500) null,
  other_remark varchar(500) null, after_sale_compensation varchar(500) null,
  service_mark varchar(500) null, extended_warranty tinyint(1) null,
  price_protection tinyint(1) null, `delayed` tinyint(1) null, follow_up tinyint(1) null,
  urgent tinyint(1) null, template_id bigint null, template_name_snapshot varchar(100) null,
  parsed_text varchar(2000) null,
  create_by varchar(64) default '', create_time datetime default current_timestamp,
  update_by varchar(64) default '', update_time datetime null,
  primary key (order_no), key idx_order_gift_daily (daily_id),
  key idx_order_gift_process (process_status)
) engine=InnoDB default charset=utf8mb4 comment='订单礼品处理状态';

create table if not exists erp_order_gift (
  order_gift_id bigint not null auto_increment, order_no varchar(64) not null,
  gift_id bigint not null, quantity int not null,
  gift_name_snapshot varchar(120) not null, specification_snapshot varchar(120) null,
  unit_snapshot varchar(20) not null, unit_cost_snapshot decimal(12,2) not null,
  total_cost decimal(12,2) not null, price_effective_date date not null,
  cost_status varchar(30) not null default 'included',
  create_by varchar(64) default '', create_time datetime default current_timestamp,
  update_by varchar(64) default '', update_time datetime null,
  primary key (order_gift_id), unique key uk_order_gift (order_no, gift_id),
  key idx_order_gift_gift (gift_id), key idx_order_gift_cost_status (cost_status)
) engine=InnoDB default charset=utf8mb4 comment='订单礼品明细';

create table if not exists erp_order_gift_log (
  log_id bigint not null auto_increment, order_no varchar(64) not null,
  action_type varchar(30) not null, detail_json text null,
  operator_name varchar(64) not null, action_time datetime default current_timestamp,
  primary key (log_id), key idx_order_gift_log_order (order_no, action_time)
) engine=InnoDB default charset=utf8mb4 comment='订单礼品操作日志';

create table if not exists live_ding_sync_log (
  sync_id bigint not null auto_increment, sync_status varchar(20) not null,
  total_count int not null default 0, success_count int not null default 0,
  failure_count int not null default 0, error_message varchar(1000) null,
  create_by varchar(64) default '', create_time datetime default current_timestamp,
  primary key (sync_id)
) engine=InnoDB default charset=utf8mb4 comment='钉钉人员同步日志';

-- 资料中心的版本历史、人员映射和个人模板见 live_gift_material_center.sql。
