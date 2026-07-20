-- ERP 双仓模型升级：实体仓 + 云仓
-- 执行前请备份 xinkeerp；本脚本可重复执行。

drop procedure if exists add_column_if_not_exists;
delimiter //
create procedure add_column_if_not_exists(
    in p_table_name varchar(64),
    in p_column_name varchar(64),
    in p_column_definition varchar(1000)
)
begin
    if not exists (
        select 1 from information_schema.columns
        where table_schema = database()
          and table_name = p_table_name
          and column_name = p_column_name
    ) then
        set @ddl = concat('alter table `', p_table_name, '` add column `', p_column_name, '` ', p_column_definition);
        prepare stmt from @ddl;
        execute stmt;
        deallocate prepare stmt;
    end if;
end //
delimiter ;

call add_column_if_not_exists('erp_warehouse', 'warehouse_usage', "varchar(32) not null default 'normal' comment 'normal正品/return退货/defective残次/transit中转'");
call add_column_if_not_exists('erp_warehouse', 'ownership_type', "varchar(32) not null default 'self_operated' comment 'self_operated自营/outsourced外包'");
call add_column_if_not_exists('erp_warehouse', 'provider_name', "varchar(128) null comment '云仓服务商'");
call add_column_if_not_exists('erp_warehouse', 'external_warehouse_code', "varchar(128) null comment '服务商仓库编码'");
call add_column_if_not_exists('erp_warehouse', 'owner_code', "varchar(64) null comment '货主编码'");
call add_column_if_not_exists('erp_warehouse', 'sync_mode', "varchar(32) not null default 'manual' comment 'manual/api/file'");
call add_column_if_not_exists('erp_warehouse', 'sync_status', "varchar(32) not null default 'never' comment 'never/success/warning/failed/not_applicable'");
call add_column_if_not_exists('erp_warehouse', 'last_sync_time', "datetime null comment '最后同步时间'");
call add_column_if_not_exists('erp_warehouse', 'enable_location', "char(1) not null default '1' comment '是否启用库位'");
call add_column_if_not_exists('erp_warehouse', 'allow_negative_stock', "char(1) not null default '0' comment '是否允许负库存'");
call add_column_if_not_exists('erp_warehouse', 'priority', "int not null default 100 comment '出库优先级，数字越小越优先'");

update erp_warehouse
set warehouse_usage = 'return'
where warehouse_type = 'return';

update erp_warehouse
set warehouse_type = case
        when warehouse_type = 'third_party' then 'cloud'
        when warehouse_type in ('self', 'return') then 'physical'
        else warehouse_type
    end,
    ownership_type = case when warehouse_type in ('third_party', 'cloud') then 'outsourced' else ownership_type end,
    enable_location = case when warehouse_type in ('third_party', 'cloud') then '0' else enable_location end,
    sync_status = case when warehouse_type in ('third_party', 'cloud') then 'never' else 'not_applicable' end
where warehouse_type in ('self', 'third_party', 'return');

call add_column_if_not_exists('erp_inventory_balance', 'external_available_qty', "int null comment '云仓服务商可用库存'");
call add_column_if_not_exists('erp_inventory_balance', 'external_locked_qty', "int null comment '云仓服务商锁定库存'");
call add_column_if_not_exists('erp_inventory_balance', 'sync_diff_qty', "int null comment '云仓库存-ERP账面库存'");
call add_column_if_not_exists('erp_inventory_balance', 'sync_status', "varchar(32) not null default 'not_applicable' comment 'never/matched/difference/failed/not_applicable'");
call add_column_if_not_exists('erp_inventory_balance', 'last_sync_time', "datetime null comment '最后同步时间'");

update erp_inventory_balance b
inner join erp_warehouse w on w.warehouse_id = b.warehouse_id
set b.sync_status = case when w.warehouse_type = 'cloud' then 'never' else 'not_applicable' end
where b.last_sync_time is null;

create table if not exists erp_warehouse_location (
  location_id bigint not null auto_increment,
  warehouse_id bigint not null,
  location_code varchar(64) not null,
  location_name varchar(128) not null,
  zone_code varchar(64) null,
  usage_type varchar(32) not null default 'normal',
  barcode varchar(128) null,
  sort_order int not null default 100,
  status char(1) not null default '0',
  create_by varchar(64) default '',
  create_time datetime null,
  update_by varchar(64) default '',
  update_time datetime null,
  remark varchar(500) null,
  primary key (location_id),
  unique key uk_erp_location_code (warehouse_id, location_code),
  key idx_erp_location_status (warehouse_id, status)
) engine=innodb default charset=utf8mb4 comment='实体仓库位档案';

create table if not exists erp_warehouse_sync_log (
  sync_log_id bigint not null auto_increment,
  sync_batch_no varchar(64) not null,
  warehouse_id bigint not null,
  source_type varchar(64) not null default 'manual',
  sync_status varchar(32) not null,
  total_sku_count int not null default 0,
  matched_sku_count int not null default 0,
  unmatched_sku_count int not null default 0,
  difference_sku_count int not null default 0,
  started_time datetime null,
  finished_time datetime null,
  message varchar(500) null,
  create_by varchar(64) default '',
  create_time datetime null,
  primary key (sync_log_id),
  unique key uk_erp_warehouse_sync_batch (sync_batch_no),
  key idx_erp_warehouse_sync_time (warehouse_id, create_time)
) engine=innodb default charset=utf8mb4 comment='云仓库存同步日志';

set @inventory_menu_id := (select menu_id from sys_menu where perms = 'erp:inventory:list' limit 1);
insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '云仓库存同步', @inventory_menu_id, 5, '#', '', '', '', 1, 0, 'F', '0', '0', 'erp:inventory:sync', '#', 'admin', sysdate(), '云仓库存快照同步与差异核对'
where @inventory_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'erp:inventory:sync');

drop procedure if exists add_column_if_not_exists;
