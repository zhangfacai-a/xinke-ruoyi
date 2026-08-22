-- 为每个登录账号保存礼品的个人显示、隐藏和置顶偏好。
-- 不修改 erp_gift 全局启停状态，执行前可先备份数据库。
create table if not exists live_user_gift_preference (
  user_id bigint not null, gift_id bigint not null,
  hidden tinyint(1) not null default 0, pinned tinyint(1) not null default 0,
  sort_order int not null default 0,
  update_by varchar(64) default '', update_time datetime default current_timestamp,
  primary key (user_id, gift_id), key idx_user_gift_preference_gift (gift_id),
  key idx_user_gift_preference_order (user_id, pinned, sort_order)
) engine=InnoDB default charset=utf8mb4 comment='用户个人礼品显示与置顶偏好';
