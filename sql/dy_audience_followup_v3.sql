-- 观众跟单 V3：拆分客户阶段、联系结果和意向等级。
-- 可重复执行；不会删除或重置现有跟单数据。

set @db_name := database();

set @ddl := if(
  exists(select 1 from information_schema.columns where table_schema = @db_name and table_name = 'dy_audience_followup' and column_name = 'follow_result_code'),
  'select 1',
  'alter table dy_audience_followup add column follow_result_code varchar(24) null comment ''本次跟进结果代码'' after status'
);
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @ddl := if(
  exists(select 1 from information_schema.columns where table_schema = @db_name and table_name = 'dy_audience_followup' and column_name = 'intent_level'),
  'select 1',
  'alter table dy_audience_followup add column intent_level varchar(16) null comment ''HIGH/MEDIUM/LOW/UNKNOWN'' after follow_result_code'
);
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

set @ddl := if(
  exists(select 1 from information_schema.columns where table_schema = @db_name and table_name = 'dy_audience_followup' and column_name = 'close_reason_code'),
  'select 1',
  'alter table dy_audience_followup add column close_reason_code varchar(24) null comment ''NO_NEED/PRICE/NO_RESPONSE/DUPLICATE/OTHER'' after close_reason'
);
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;

update dy_audience_followup
set follow_result_code = case status
  when 'CONTACTED' then 'CONTACTED'
  when 'QUALIFIED' then 'CONSIDERING'
  when 'QUOTED' then 'QUOTED'
  when 'ORDER_PENDING' then 'ORDER_PENDING'
  when 'ORDERED' then 'ORDERED'
  when 'PAUSED' then 'PAUSED'
  when 'INVALID' then 'INVALID'
  else follow_result_code
end
where follow_result_code is null;

update dy_audience_followup
set intent_level = case
  when status in ('QUALIFIED', 'QUOTED', 'ORDER_PENDING') then 'HIGH'
  when status = 'CONTACTED' then 'MEDIUM'
  else 'UNKNOWN'
end
where intent_level is null;

set @ddl := if(
  exists(select 1 from information_schema.statistics where table_schema = @db_name and table_name = 'dy_audience_followup' and index_name = 'idx_dy_audience_followup_intent'),
  'select 1',
  'create index idx_dy_audience_followup_intent on dy_audience_followup(intent_level, status)'
);
prepare stmt from @ddl; execute stmt; deallocate prepare stmt;
