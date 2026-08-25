-- 抖音观众榜单导入模块
-- 只新增榜单历史与最新画像，不创建旧观众线索表的外键。
-- 可重复执行；正式环境请在备份并确认 MySQL 版本后执行。

create table if not exists dy_audience_rank_batch (
  batch_id bigint not null auto_increment comment '导入批次ID',
  payload_hash char(64) not null comment '标准化榜单内容SHA-256指纹',
  room_scope_key char(64) not null comment '直播账号范围标识（由直播间名称标准化生成）',
  room_name varchar(128) not null comment '上传时识别到的直播间名称',
  room_id bigint null comment '匹配到的直播间ID，不建立外键',
  room_match_status varchar(16) not null default 'UNMATCHED' comment 'MATCHED/UNMATCHED/AMBIGUOUS',
  is_current tinyint(1) not null default 1 comment '同直播间同数据日期的当前有效版本',
  comment_data_date date not null comment '评论榜实际数据日期',
  watch_data_date date not null comment '观看榜实际数据日期',
  captured_at datetime(3) not null comment '插件采集时间',
  comment_row_count int not null default 0 comment '评论榜原始行数',
  watch_row_count int not null default 0 comment '观看榜原始行数',
  unique_user_count int not null default 0 comment '两榜按sec_uid合并后的用户数',
  uploaded_ip varchar(64) null comment '上传来源IP',
  create_time datetime(3) not null default current_timestamp(3),
  primary key (batch_id),
  unique key uk_dy_audience_batch_hash (payload_hash),
  key idx_dy_audience_batch_room_date (room_scope_key, captured_at),
  key idx_dy_audience_batch_status (room_match_status, captured_at),
  key idx_dy_audience_batch_data_date (comment_data_date, watch_data_date),
  key idx_dy_audience_batch_current (room_scope_key, comment_data_date, watch_data_date, is_current)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='抖音观众榜单导入批次';

create table if not exists dy_audience_rank_snapshot (
  snapshot_id bigint not null auto_increment comment '榜单明细ID',
  batch_id bigint not null comment '导入批次ID，不建立外键以便历史留存',
  sec_uid varchar(256) character set utf8mb4 collate utf8mb4_bin not null comment '抖音sec_uid，按大小写精确识别',
  nickname varchar(128) not null comment '昵称快照',
  is_follower tinyint(1) null comment '是否关注主播',
  is_following tinyint(1) null comment '主播是否关注用户',
  pay_level int null comment '消费等级',
  pay_icon_url varchar(1000) null comment '消费等级图标地址',
  comment_count bigint null comment '评论次数，不在评论榜时为NULL',
  comment_rank int null comment '评论榜名次，不在评论榜时为NULL',
  watch_seconds bigint null comment '观看秒数，不在观看榜时为NULL',
  watch_rank int null comment '观看榜名次，不在观看榜时为NULL',
  create_time datetime(3) not null default current_timestamp(3),
  primary key (snapshot_id),
  unique key uk_dy_audience_snapshot_batch_uid (batch_id, sec_uid),
  key idx_dy_audience_snapshot_uid (sec_uid),
  key idx_dy_audience_snapshot_comment (comment_rank, comment_count),
  key idx_dy_audience_snapshot_watch (watch_rank, watch_seconds),
  key idx_dy_audience_snapshot_batch (batch_id, snapshot_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='抖音观众榜单历史明细';

create table if not exists dy_audience_profile (
  profile_id bigint not null auto_increment comment '观众画像ID',
  room_scope_key char(64) not null comment '直播账号范围标识',
  room_name varchar(128) not null comment '最近一次直播间名称',
  room_id bigint null comment '最近一次匹配的直播间ID，不建立外键',
  sec_uid varchar(256) character set utf8mb4 collate utf8mb4_bin not null comment '抖音sec_uid，按大小写精确识别',
  nickname varchar(128) not null comment '最近昵称',
  is_follower tinyint(1) null comment '最近是否关注主播',
  is_following tinyint(1) null comment '最近主播是否关注用户',
  pay_level int null comment '最近消费等级',
  pay_icon_url varchar(1000) null comment '最近消费等级图标地址',
  first_batch_id bigint not null comment '首次出现批次ID',
  last_batch_id bigint not null comment '最后出现批次ID',
  first_seen_at datetime(3) not null comment '首次出现时间',
  last_seen_at datetime(3) not null comment '最后出现时间',
  create_time datetime(3) not null default current_timestamp(3),
  update_time datetime(3) not null default current_timestamp(3) on update current_timestamp(3),
  primary key (profile_id),
  unique key uk_dy_audience_profile_uid (sec_uid),
  key idx_dy_audience_profile_uid (sec_uid),
  key idx_dy_audience_profile_last_seen (last_seen_at),
  key idx_dy_audience_profile_room (room_id, last_seen_at)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='抖音观众最新画像';
