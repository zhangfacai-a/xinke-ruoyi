-- 抖音观众跟单工作台
-- 榜单历史/观众画像只保存平台事实；本文件保存客服可编辑的跟单资料和追加式时间线。
-- 可重复执行，执行前请备份数据库。

create table if not exists dy_audience_followup (
  followup_id bigint not null auto_increment comment '跟单档案ID',
  profile_id bigint null comment '观众画像ID，不建立外键',
  room_scope_key char(64) not null comment '直播间范围标识',
  room_id bigint null comment '直播间ID，不建立外键',
  room_name_snapshot varchar(128) not null comment '直播间名称快照',
  sec_uid varchar(256) character set utf8mb4 collate utf8mb4_bin not null comment '抖音sec_uid',
  nickname_snapshot varchar(128) not null comment '观众昵称快照',
  owner_user_id bigint null comment '领取人系统用户ID',
  owner_name_snapshot varchar(128) null comment '领取人名称快照',
  anchor_user_id bigint null comment '跟单主播系统用户ID',
  anchor_name_snapshot varchar(128) null comment '跟单主播名称快照',
  controller_user_id bigint null comment '跟单场控系统用户ID',
  controller_name_snapshot varchar(128) null comment '跟单场控名称快照',
  status varchar(24) not null default 'UNASSIGNED' comment 'UNASSIGNED/PENDING/CONTACTED/QUALIFIED/QUOTED/ORDER_PENDING/ORDERED/CLOSED/PAUSED/INVALID',
  follow_result_code varchar(24) null comment '本次跟进结果代码',
  intent_level varchar(16) null comment 'HIGH/MEDIUM/LOW/UNKNOWN',
  consult_model varchar(256) null comment '咨询型号',
  order_no varchar(64) null comment '下单订单号',
  priority tinyint(1) not null default 0 comment '是否重点跟进',
  last_contact_at datetime(3) null comment '最近联系时间',
  next_follow_at datetime(3) null comment '下次跟进时间',
  last_follow_result varchar(500) null comment '最近跟进结果',
  remark varchar(1000) null comment '跟单备注',
  close_reason varchar(500) null comment '关闭原因',
  close_reason_code varchar(24) null comment 'NO_NEED/PRICE/NO_RESPONSE/DUPLICATE/OTHER',
  status_changed_at datetime(3) null comment '状态最近变更时间',
  version int not null default 0 comment '乐观锁版本',
  first_source_batch_id bigint null comment '首次导入批次ID',
  last_source_batch_id bigint null comment '最近导入批次ID',
  first_seen_at datetime(3) null comment '首次进入榜单时间',
  last_seen_at datetime(3) null comment '最近进入榜单时间',
  create_by varchar(64) default '' comment '创建人',
  create_time datetime(3) not null default current_timestamp(3),
  update_by varchar(64) default '' comment '更新人',
  update_time datetime(3) null on update current_timestamp(3),
  primary key (followup_id),
  unique key uk_dy_audience_followup_uid (sec_uid),
  key idx_dy_audience_followup_owner (owner_user_id, status),
  key idx_dy_audience_followup_status_due (status, next_follow_at),
  key idx_dy_audience_followup_anchor (anchor_user_id, status),
  key idx_dy_audience_followup_controller (controller_user_id, status),
  key idx_dy_audience_followup_room (room_id, status),
  key idx_dy_audience_followup_order (order_no),
  key idx_dy_audience_followup_uid (sec_uid)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='抖音观众跟单档案';

create table if not exists dy_audience_followup_log (
  log_id bigint not null auto_increment comment '跟单时间线ID',
  followup_id bigint not null comment '跟单档案ID，不建立外键',
  action_type varchar(32) not null comment 'CREATE/ASSIGN/CONTACT/STATUS/ORDER/MODEL/NOTE/REMINDER/UPDATE',
  before_json text null comment '修改前内容',
  after_json text null comment '修改后内容',
  contact_method varchar(32) null comment '联系渠道',
  content varchar(1000) null comment '本次联系或操作内容',
  result varchar(500) null comment '本次跟进结果',
  status_before varchar(24) null,
  status_after varchar(24) null,
  next_follow_at datetime(3) null,
  operator_user_id bigint null comment '操作人用户ID',
  operator_name_snapshot varchar(128) null comment '操作人名称快照',
  create_time datetime(3) not null default current_timestamp(3),
  primary key (log_id),
  key idx_dy_audience_followup_log_followup (followup_id, create_time),
  key idx_dy_audience_followup_log_operator (operator_user_id, create_time)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='抖音观众跟单时间线';

create table if not exists dy_audience_assignment_rule (
  rule_id bigint not null auto_increment comment '智能分配规则ID',
  room_id bigint not null comment '直播间ID，每个直播间一条规则',
  enabled tinyint(1) not null default 0 comment '是否启用',
  max_active_per_owner int not null default 100 comment '每位领取人的最大进行中客户数',
  reclaim_hours int not null default 24 comment '领取后未联系的自动回收小时数',
  next_member_index int not null default 0 comment '下次轮流分配位置',
  update_by varchar(64) default '' comment '更新人',
  create_time datetime(3) not null default current_timestamp(3),
  update_time datetime(3) null on update current_timestamp(3),
  primary key (rule_id),
  unique key uk_dy_audience_assignment_room (room_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='观众客户智能分配规则';

create table if not exists dy_audience_assignment_member (
  member_id bigint not null auto_increment comment '规则成员ID',
  rule_id bigint not null comment '分配规则ID',
  user_id bigint not null comment '领取人系统用户ID',
  sort_order int not null default 0 comment '轮流分配顺序',
  enabled tinyint(1) not null default 1 comment '是否参与分配',
  create_time datetime(3) not null default current_timestamp(3),
  primary key (member_id),
  unique key uk_dy_audience_assignment_member (rule_id, user_id),
  key idx_dy_audience_assignment_user (user_id, enabled)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='观众客户智能分配成员';
