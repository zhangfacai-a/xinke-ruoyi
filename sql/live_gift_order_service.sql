-- 订单服务记录结构化字段。MySQL 8.0+，可重复执行。
-- 先执行本文件，再使用新的订单礼品页面。
set @db_name=database();

set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='anchor_user_id')=0,
  'alter table erp_order_gift_status add column anchor_user_id bigint null after operator_note','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='anchor_name_snapshot')=0,
  'alter table erp_order_gift_status add column anchor_name_snapshot varchar(120) null after anchor_user_id','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='controller_user_id')=0,
  'alter table erp_order_gift_status add column controller_user_id bigint null after anchor_name_snapshot','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='controller_name_snapshot')=0,
  'alter table erp_order_gift_status add column controller_name_snapshot varchar(120) null after controller_user_id','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='refund_amount')=0,
  'alter table erp_order_gift_status add column refund_amount decimal(12,2) null after controller_name_snapshot','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='refund_reason')=0,
  'alter table erp_order_gift_status add column refund_reason varchar(500) null after refund_amount','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='other_remark')=0,
  'alter table erp_order_gift_status add column other_remark varchar(500) null after refund_reason','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='after_sale_compensation')=0,
  'alter table erp_order_gift_status add column after_sale_compensation varchar(500) null after other_remark','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='service_mark')=0,
  'alter table erp_order_gift_status add column service_mark varchar(500) null after after_sale_compensation','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='extended_warranty')=0,
  'alter table erp_order_gift_status add column extended_warranty tinyint(1) null after service_mark','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='price_protection')=0,
  'alter table erp_order_gift_status add column price_protection tinyint(1) null after extended_warranty','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='delayed')=0,
  'alter table erp_order_gift_status add column `delayed` tinyint(1) null after price_protection','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='follow_up')=0,
  'alter table erp_order_gift_status add column follow_up tinyint(1) null after `delayed`','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='urgent')=0,
  'alter table erp_order_gift_status add column urgent tinyint(1) null after follow_up','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='template_id')=0,
  'alter table erp_order_gift_status add column template_id bigint null after urgent','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='template_name_snapshot')=0,
  'alter table erp_order_gift_status add column template_name_snapshot varchar(100) null after template_id','select 1'); prepare s from @sql; execute s; deallocate prepare s;
set @sql=if((select count(*) from information_schema.columns where table_schema=@db_name and table_name='erp_order_gift_status' and column_name='parsed_text')=0,
  'alter table erp_order_gift_status add column parsed_text varchar(2000) null after template_name_snapshot','select 1'); prepare s from @sql; execute s; deallocate prepare s;
