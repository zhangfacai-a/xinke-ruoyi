-- Douyin live room mapping, owner locking, and comment stat repair.

create table if not exists dy_live_room (
  room_id bigint not null auto_increment comment 'Live room mapping ID',
  room_key varchar(64) not null comment 'Live room ID from Douyin capture',
  room_name varchar(100) not null comment 'Display live room name',
  source varchar(64) default null comment 'Source',
  status char(1) default '0' comment '0 normal, 1 disabled',
  remark varchar(500) default null comment 'Remark',
  last_report_time datetime default null comment 'Last report time',
  create_time datetime default current_timestamp comment 'Create time',
  update_time datetime default current_timestamp on update current_timestamp comment 'Update time',
  primary key (room_id),
  unique key uk_room_key (room_key),
  key idx_room_name (room_name)
) engine=innodb default charset=utf8mb4 comment='Douyin live room mapping';

set @has_owner_assigned_time := (
  select count(1)
  from information_schema.columns
  where table_schema = database()
    and table_name = 'dy_viewer_daily_lead'
    and column_name = 'owner_assigned_time'
);
set @ddl := if(
  @has_owner_assigned_time = 0,
  'alter table dy_viewer_daily_lead add column owner_assigned_time datetime default null comment ''Owner assigned time'' after owner_name',
  'select 1'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

update dy_viewer_daily_lead
set owner_assigned_time = ifnull(manual_update_time, update_time)
where owner_name is not null
  and owner_name != ''
  and owner_assigned_time is null;

insert into dy_live_room(room_key, room_name, source, last_report_time, create_time, update_time)
select room_key,
       max(coalesce(nullif(live_room_name, ''), room_key)) as room_name,
       'history',
       max(update_time),
       sysdate(),
       sysdate()
from dy_viewer_daily_lead
group by room_key
on duplicate key update
  room_name = if(dy_live_room.room_name is null or dy_live_room.room_name = '' or dy_live_room.room_name = values(room_key), values(room_name), dy_live_room.room_name),
  last_report_time = values(last_report_time),
  update_time = sysdate();

update dy_viewer_daily_lead l
set has_comment = if((select count(1) from dy_viewer_comment c where c.lead_id = l.lead_id) > 0, 1, 0),
    comment_count = (select count(1) from dy_viewer_comment c where c.lead_id = l.lead_id),
    first_comment_time = (select min(c.captured_at) from dy_viewer_comment c where c.lead_id = l.lead_id),
    last_comment_time = (select max(c.captured_at) from dy_viewer_comment c where c.lead_id = l.lead_id),
    last_comment_content = (
      select c.content
      from dy_viewer_comment c
      where c.lead_id = l.lead_id
      order by c.captured_at desc, c.comment_id desc
      limit 1
    );

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Live Room Mapping', menu_id, 21, 'live-room', 'live/room/index', null, 'LiveRoomMapping', 1, 0, 'C', '0', '0', 'live:room:list', 'list', 'admin', sysdate(), 'Live room ID and name mapping'
from sys_menu
where menu_name = 'ERP'
  and parent_id = 0
  and not exists (select 1 from sys_menu where perms = 'live:room:list');

set @live_room_menu_id := (select menu_id from sys_menu where perms = 'live:room:list' limit 1);

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Room Query', @live_room_menu_id, 1, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:room:query', '#', 'admin', sysdate(), 'Query live room mapping'
where @live_room_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:room:query');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Room Add', @live_room_menu_id, 2, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:room:add', '#', 'admin', sysdate(), 'Add live room mapping'
where @live_room_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:room:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Room Edit', @live_room_menu_id, 3, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:room:edit', '#', 'admin', sysdate(), 'Edit live room mapping'
where @live_room_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:room:edit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select 'Room Delete', @live_room_menu_id, 4, '#', '', null, null, 1, 0, 'F', '0', '0', 'live:room:remove', '#', 'admin', sysdate(), 'Delete live room mapping'
where @live_room_menu_id is not null
  and not exists (select 1 from sys_menu where perms = 'live:room:remove');
