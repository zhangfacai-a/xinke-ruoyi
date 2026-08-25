-- 观众客户 V5：区分“已采集观众”和“需要跟进的客户”，并增加直播间进客规则。
-- 可重复执行；不会删除客户、商机、订单或沟通历史。

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_followup' and column_name = 'qualification_reason') = 0,
  'alter table dy_audience_followup add column qualification_reason varchar(255) null comment ''进入待办的原因'' after reactivation_pending',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_followup' and column_name = 'qualified_at') = 0,
  'alter table dy_audience_followup add column qualified_at datetime(3) null comment ''首次进入待办时间'' after qualification_reason',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'qualification_enabled') = 0,
  'alter table dy_audience_assignment_rule add column qualification_enabled tinyint(1) not null default 1 after next_member_index',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'comment_rank_threshold') = 0,
  'alter table dy_audience_assignment_rule add column comment_rank_threshold int not null default 30 after qualification_enabled',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'watch_rank_threshold') = 0,
  'alter table dy_audience_assignment_rule add column watch_rank_threshold int not null default 30 after comment_rank_threshold',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'min_pay_level') = 0,
  'alter table dy_audience_assignment_rule add column min_pay_level int not null default 10 after watch_rank_threshold',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'min_visit_days') = 0,
  'alter table dy_audience_assignment_rule add column min_visit_days int not null default 2 after min_pay_level',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'follower_qualifies') = 0,
  'alter table dy_audience_assignment_rule add column follower_qualifies tinyint(1) not null default 0 after min_visit_days',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_assignment_rule' and column_name = 'following_qualifies') = 0,
  'alter table dy_audience_assignment_rule add column following_qualifies tinyint(1) not null default 0 after follower_qualifies',
  'select 1'
);
prepare audience_stmt from @audience_sql; execute audience_stmt; deallocate prepare audience_stmt;

-- 只整理从未领取、从未联系、没有订单且没有人工重点标记的旧测试式待领取记录。
-- 命中默认规则的记录继续保留在待领取池，其余转入观察中。
drop temporary table if exists tmp_audience_visit_days;
create temporary table tmp_audience_visit_days as
select visits.sec_uid, count(distinct visits.visit_date) as appearance_days
from (
  select s.sec_uid, b.comment_data_date as visit_date
  from dy_audience_rank_snapshot s
  join dy_audience_rank_batch b on b.batch_id = s.batch_id and b.is_current = 1
  where s.comment_rank is not null and b.comment_data_date is not null
  union all
  select s.sec_uid, b.watch_data_date as visit_date
  from dy_audience_rank_snapshot s
  join dy_audience_rank_batch b on b.batch_id = s.batch_id and b.is_current = 1
  where s.watch_rank is not null and b.watch_data_date is not null
) visits
group by visits.sec_uid;

update dy_audience_followup f
left join dy_audience_rank_snapshot s
  on s.batch_id = f.last_source_batch_id and s.sec_uid = f.sec_uid
left join tmp_audience_visit_days v on v.sec_uid = f.sec_uid
set f.status = 'OBSERVING', f.qualification_reason = null, f.qualified_at = null,
    f.status_changed_at = current_timestamp(3), f.update_time = current_timestamp(3)
where f.status = 'UNASSIGNED'
  and f.owner_user_id is null
  and f.last_contact_at is null
  and (f.order_no is null or trim(f.order_no) = '')
  and coalesce(f.priority, 0) = 0
  and not (
    (coalesce(s.comment_rank, 0) between 1 and 30)
    or (coalesce(s.watch_rank, 0) between 1 and 30)
    or coalesce(s.pay_level, 0) >= 10
    or coalesce(v.appearance_days, 0) >= 2
  );

update dy_audience_followup f
left join dy_audience_rank_snapshot s
  on s.batch_id = f.last_source_batch_id and s.sec_uid = f.sec_uid
left join tmp_audience_visit_days v on v.sec_uid = f.sec_uid
set f.qualification_reason = case
      when f.priority = 1 then '人工重点客户'
      when coalesce(s.comment_rank, 0) between 1 and 30 then concat('评论榜前30名')
      when coalesce(s.watch_rank, 0) between 1 and 30 then concat('观看榜前30名')
      when coalesce(s.pay_level, 0) >= 10 then '消费等级达到10级'
      when coalesce(v.appearance_days, 0) >= 2 then concat('累计到访', v.appearance_days, '天')
      else f.qualification_reason
    end,
    f.qualified_at = coalesce(f.qualified_at, f.first_seen_at, f.create_time)
where f.status != 'OBSERVING' and f.qualification_reason is null;

update dy_audience_opportunity o
join dy_audience_followup f on f.followup_id = o.followup_id and o.is_current = 1
set o.status = f.status
where o.status = 'UNASSIGNED' and f.status = 'OBSERVING';

drop temporary table if exists tmp_audience_visit_days;
