-- Live Operations / Plugin Client Management.
-- Execute after deploying backend. This table stores one row per plugin computer.

create table if not exists dy_plugin_client (
    client_id varchar(80) not null comment 'Plugin computer unique id',
    client_name varchar(120) default null comment 'Display name',
    plugin_version varchar(32) default null comment 'Plugin version',
    source varchar(64) default null comment 'Report source',
    room_key varchar(128) default null comment 'Last room key',
    live_room_name varchar(120) default null comment 'Last live room name',
    page_url varchar(500) default null comment 'Last page url',
    request_url varchar(500) default null comment 'Last request url',
    status char(1) not null default '0' comment '0 enabled, 1 disabled',
    report_count bigint not null default 0 comment 'Report count',
    fail_count bigint not null default 0 comment 'Blocked or failed count',
    last_report_time datetime default null comment 'Last report time',
    last_success_time datetime default null comment 'Last success time',
    last_error_time datetime default null comment 'Last error time',
    last_error_msg varchar(500) default null comment 'Last error message',
    remark varchar(500) default null comment 'Remark',
    create_time datetime default null comment 'Create time',
    update_time datetime default null comment 'Update time',
    primary key (client_id),
    key idx_dy_plugin_client_status (status),
    key idx_dy_plugin_client_last_report (last_report_time),
    key idx_dy_plugin_client_error (last_error_time)
) engine=InnoDB default charset=utf8mb4 comment='Douyin capture plugin clients';

-- Repair the menu name if older SQL was executed under a wrong client encoding.
update sys_menu
set menu_name = '插件控制'
where perms = 'live:plugin:control';
