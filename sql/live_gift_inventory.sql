-- 礼品库存与订单扣减。可重复执行。
create table if not exists live_gift_inventory (
  gift_id bigint not null,
  stock_qty int not null default 0,
  safety_qty int not null default 0,
  update_by varchar(64) default null,
  update_time datetime default null,
  primary key (gift_id),
  constraint fk_live_gift_inventory_gift foreign key (gift_id) references erp_gift(gift_id)
) engine=InnoDB default charset=utf8mb4 comment='礼品库存余额';

create table if not exists live_gift_inventory_movement (
  movement_id bigint not null auto_increment,
  gift_id bigint not null,
  movement_type varchar(32) not null comment 'IN/OUT/ORDER_OUT/ORDER_RESTORE/SET',
  quantity int not null comment '正数入库，负数出库',
  before_qty int not null,
  after_qty int not null,
  source_type varchar(32) default null,
  source_no varchar(64) default null,
  remark varchar(500) default null,
  create_by varchar(64) default null,
  create_time datetime not null,
  primary key (movement_id),
  key idx_live_gift_inventory_movement_gift_time (gift_id, create_time),
  key idx_live_gift_inventory_movement_source (source_type, source_no),
  constraint fk_live_gift_inventory_movement_gift foreign key (gift_id) references erp_gift(gift_id)
) engine=InnoDB default charset=utf8mb4 comment='礼品库存流水';

create table if not exists live_gift_order_inventory (
  order_no varchar(64) not null,
  gift_id bigint not null,
  quantity int not null,
  create_by varchar(64) default null,
  create_time datetime not null,
  update_by varchar(64) default null,
  update_time datetime default null,
  primary key (order_no, gift_id),
  key idx_live_gift_order_inventory_gift (gift_id),
  constraint fk_live_gift_order_inventory_gift foreign key (gift_id) references erp_gift(gift_id)
) engine=InnoDB default charset=utf8mb4 comment='订单礼品库存扣减关联';

-- Use the stable route path instead of the localized menu label.
set @live_parent_id = (select menu_id from sys_menu where menu_type='M' and path='live-ops' order by menu_id limit 1);
insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '礼品库存',@live_parent_id,15,'giftInventory','live/giftInventory/index',null,'GiftInventory',1,0,'C','0','0','live:gift:inventory','goods','admin',sysdate(),'礼品库存与库存流水'
where @live_parent_id is not null
  and not exists(select 1 from sys_menu where parent_id=@live_parent_id and path='giftInventory');
set @gift_inventory_id=(select menu_id from sys_menu where parent_id=@live_parent_id and path='giftInventory' limit 1);
insert into sys_menu(menu_name,parent_id,order_num,path,component,`query`,route_name,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time,remark)
select '库存调整',@gift_inventory_id,1,'',null,null,'',1,0,'F','0','0','live:gift:inventory:adjust','#','admin',sysdate(),'礼品库存入库、出库和盘点调整'
where @gift_inventory_id is not null
  and not exists(select 1 from sys_menu where perms='live:gift:inventory:adjust');
insert ignore into sys_role_menu(role_id,menu_id)
select 1,menu_id from sys_menu where menu_id=@gift_inventory_id or parent_id=@gift_inventory_id;
