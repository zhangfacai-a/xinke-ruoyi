-- 礼品记录直播间快照与用户当前直播间，可重复执行。
set @ddl=if((select count(*) from information_schema.columns where table_schema=database() and table_name='erp_order_gift_status' and column_name='room_id')=0,'alter table erp_order_gift_status add column room_id bigint default null after daily_id','select 1'); prepare stmt from @ddl; execute stmt; deallocate prepare stmt;
set @ddl=if((select count(*) from information_schema.columns where table_schema=database() and table_name='erp_order_gift_status' and column_name='room_code_snapshot')=0,'alter table erp_order_gift_status add column room_code_snapshot varchar(50) default null after room_id','select 1'); prepare stmt from @ddl; execute stmt; deallocate prepare stmt;
set @ddl=if((select count(*) from information_schema.columns where table_schema=database() and table_name='erp_order_gift_status' and column_name='room_name_snapshot')=0,'alter table erp_order_gift_status add column room_name_snapshot varchar(100) default null after room_code_snapshot','select 1'); prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

create table if not exists live_user_room_preference (
  user_id bigint not null,
  room_id bigint default null,
  update_by varchar(64) default null,
  update_time datetime default null,
  primary key (user_id),
  key idx_live_user_room_preference_room (room_id)
) engine=InnoDB default charset=utf8mb4 comment='用户礼品录入当前直播间';
