-- 观众榜单去重规则 v2（当前数据为测试数据时执行）
-- 保留用户、直播间、直播间人员映射；清空榜单与跟单测试数据。

set foreign_key_checks = 0;
truncate table dy_audience_followup_log;
truncate table dy_audience_followup;
truncate table dy_audience_profile;
truncate table dy_audience_rank_snapshot;
truncate table dy_audience_rank_batch;
set foreign_key_checks = 1;

alter table dy_audience_rank_batch
  add column is_current tinyint(1) not null default 1 comment '同直播间同数据日期的当前有效版本' after room_match_status,
  add key idx_dy_audience_batch_current (room_scope_key, comment_data_date, watch_data_date, is_current);

alter table dy_audience_profile
  drop index uk_dy_audience_profile_scope_uid,
  add unique key uk_dy_audience_profile_uid (sec_uid);

alter table dy_audience_followup
  add column owner_user_id bigint null comment '领取人系统用户ID' after nickname_snapshot,
  add column owner_name_snapshot varchar(128) null comment '领取人名称快照' after owner_user_id,
  drop index uk_dy_audience_followup_scope_uid,
  add unique key uk_dy_audience_followup_uid (sec_uid),
  add key idx_dy_audience_followup_owner (owner_user_id, status);

alter table live_room
  add unique key uk_live_room_name (room_name);
