-- 观众客户 V4：客户档案、购买商机、订单分离。
-- 兼容现有 dy_audience_followup 数据，可重复执行。

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_followup' and column_name = 'contact_phone') = 0,
  'alter table dy_audience_followup add column contact_phone varchar(64) null comment ''联系电话'' after nickname_snapshot',
  'select 1'
);
prepare audience_stmt from @audience_sql;
execute audience_stmt;
deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_followup' and column_name = 'contact_wechat') = 0,
  'alter table dy_audience_followup add column contact_wechat varchar(128) null comment ''微信号'' after contact_phone',
  'select 1'
);
prepare audience_stmt from @audience_sql;
execute audience_stmt;
deallocate prepare audience_stmt;

set @audience_sql = if(
  (select count(*) from information_schema.columns where table_schema = database()
   and table_name = 'dy_audience_followup' and column_name = 'reactivation_pending') = 0,
  'alter table dy_audience_followup add column reactivation_pending tinyint(1) not null default 0 comment ''结束后再次到访，等待重新激活'' after priority',
  'select 1'
);
prepare audience_stmt from @audience_sql;
execute audience_stmt;
deallocate prepare audience_stmt;

create table if not exists dy_audience_opportunity (
  opportunity_id bigint not null auto_increment comment '购买商机ID',
  followup_id bigint not null comment '客户档案ID',
  sequence_no int not null comment '该客户第几次商机',
  is_current tinyint(1) not null default 1 comment '是否当前商机',
  status varchar(24) not null default 'UNASSIGNED',
  follow_result_code varchar(24) null,
  intent_level varchar(16) null,
  consult_model varchar(256) null,
  source_room_id bigint null,
  source_room_name varchar(128) null,
  owner_user_id bigint null,
  owner_name_snapshot varchar(128) null,
  close_reason_code varchar(24) null,
  close_reason varchar(500) null,
  opened_at datetime(3) not null,
  closed_at datetime(3) null,
  create_by varchar(64) default '',
  create_time datetime(3) not null default current_timestamp(3),
  update_by varchar(64) default '',
  update_time datetime(3) null on update current_timestamp(3),
  primary key (opportunity_id),
  unique key uk_dy_audience_opportunity_seq (followup_id, sequence_no),
  key idx_dy_audience_opportunity_current (followup_id, is_current),
  key idx_dy_audience_opportunity_owner (owner_user_id, status)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='观众客户购买商机';

create table if not exists dy_audience_customer_order (
  customer_order_id bigint not null auto_increment comment '客户订单记录ID',
  followup_id bigint not null comment '客户档案ID',
  opportunity_id bigint null comment '来源商机ID',
  order_no varchar(64) not null comment '平台订单号',
  order_status varchar(24) not null default 'ORDERED' comment 'ORDERED/COMPLETED/CANCELLED/REFUNDED',
  product_model varchar(256) null,
  remark varchar(500) null,
  ordered_at datetime(3) not null,
  version int not null default 0,
  create_by varchar(64) default '',
  create_time datetime(3) not null default current_timestamp(3),
  update_by varchar(64) default '',
  update_time datetime(3) null on update current_timestamp(3),
  primary key (customer_order_id),
  unique key uk_dy_audience_customer_order_no (order_no),
  key idx_dy_audience_customer_order_customer (followup_id, ordered_at),
  key idx_dy_audience_customer_order_opportunity (opportunity_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='观众客户订单';

insert into dy_audience_opportunity(
  followup_id, sequence_no, is_current, status, follow_result_code, intent_level,
  consult_model, source_room_id, source_room_name, owner_user_id, owner_name_snapshot,
  close_reason_code, close_reason, opened_at, closed_at, create_by, create_time, update_by, update_time
)
select f.followup_id, 1, 1, f.status, f.follow_result_code, f.intent_level,
       f.consult_model, f.room_id, f.room_name_snapshot, f.owner_user_id, f.owner_name_snapshot,
       f.close_reason_code, f.close_reason, coalesce(f.first_seen_at, f.create_time, current_timestamp(3)),
       case when f.status in ('ORDERED', 'CLOSED', 'INVALID') then coalesce(f.status_changed_at, f.update_time) end,
       coalesce(f.create_by, ''), coalesce(f.create_time, current_timestamp(3)),
       coalesce(f.update_by, ''), f.update_time
from dy_audience_followup f
where not exists (
  select 1 from dy_audience_opportunity o where o.followup_id = f.followup_id
);

insert ignore into dy_audience_customer_order(
  followup_id, opportunity_id, order_no, order_status, product_model, ordered_at,
  create_by, create_time, update_by, update_time
)
select f.followup_id, o.opportunity_id, trim(f.order_no),
       case when f.status = 'CLOSED' then 'COMPLETED' else 'ORDERED' end,
       f.consult_model, coalesce(f.status_changed_at, f.update_time, current_timestamp(3)),
       coalesce(f.update_by, ''), coalesce(f.update_time, current_timestamp(3)),
       coalesce(f.update_by, ''), f.update_time
from dy_audience_followup f
left join dy_audience_opportunity o on o.followup_id = f.followup_id and o.is_current = 1
where f.order_no is not null and trim(f.order_no) != '';
