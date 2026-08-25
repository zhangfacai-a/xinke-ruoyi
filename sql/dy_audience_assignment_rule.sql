-- 观众客户智能分配规则。可重复执行，不修改现有客户归属。

create table if not exists dy_audience_assignment_rule (
  rule_id bigint not null auto_increment comment '智能分配规则ID',
  room_id bigint not null comment '直播间ID，每个直播间一条规则',
  enabled tinyint(1) not null default 0 comment '是否启用',
  max_active_per_owner int not null default 100 comment '每位领取人的最大进行中客户数',
  reclaim_hours int not null default 24 comment '领取后未联系的自动回收小时数',
  next_member_index int not null default 0 comment '下次轮流分配位置',
  qualification_enabled tinyint(1) not null default 1 comment '是否启用进客规则',
  comment_rank_threshold int not null default 30 comment '评论榜前N名进入待办，0表示关闭',
  watch_rank_threshold int not null default 30 comment '观看榜前N名进入待办，0表示关闭',
  min_pay_level int not null default 10 comment '最低消费等级，0表示关闭',
  min_visit_days int not null default 2 comment '累计到访天数，0表示关闭',
  follower_qualifies tinyint(1) not null default 0 comment '粉丝是否直接进入待办',
  following_qualifies tinyint(1) not null default 0 comment '已回关是否直接进入待办',
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
