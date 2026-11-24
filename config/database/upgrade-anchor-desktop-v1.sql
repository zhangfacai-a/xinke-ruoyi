-- Additive migration. Does not rewrite or remove historical reports.
DROP PROCEDURE IF EXISTS upgrade_anchor_desktop_v1;
DELIMITER $$
CREATE PROCEDURE upgrade_anchor_desktop_v1()
BEGIN
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='operator_user_id') THEN
    ALTER TABLE live_metric_anchor_segment ADD COLUMN operator_user_id BIGINT NULL COMMENT 'App上报操作用户ID';
  END IF;
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='client_version') THEN
    ALTER TABLE live_metric_anchor_segment ADD COLUMN client_version VARCHAR(32) NULL COMMENT '上报App版本';
  END IF;
END$$
DELIMITER ;
CALL upgrade_anchor_desktop_v1();
DROP PROCEDURE upgrade_anchor_desktop_v1;

SET @metrics_page=(SELECT menu_id FROM sys_menu WHERE component='live/metrics/index' AND status='0' ORDER BY menu_id DESC LIMIT 1);
SET @ops_parent=(SELECT parent_id FROM sys_menu WHERE menu_id=@metrics_page);
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '主播App上报',@metrics_page,2,'#','F','0','0','live:metrics:report','#','anchor-app',NOW()
WHERE @metrics_page IS NOT NULL AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='live:metrics:report');
SET @report_menu=(SELECT menu_id FROM sys_menu WHERE perms='live:metrics:report' LIMIT 1);
INSERT INTO sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
SELECT '主播App上报（自动授权）','auto_live_anchor_app',98,'5','1','1','0','0','anchor-app',NOW(),'仅直播间主播名单自动管理，无主播管理权限'
WHERE NOT EXISTS(SELECT 1 FROM sys_role WHERE role_key='auto_live_anchor_app');
SET @app_role=(SELECT role_id FROM sys_role WHERE role_key='auto_live_anchor_app' LIMIT 1);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT @app_role,menu_id FROM sys_menu WHERE menu_id IN (@metrics_page,@report_menu,@ops_parent);
INSERT IGNORE INTO sys_user_role(user_id,role_id)
SELECT DISTINCT m.user_id,@app_role FROM live_subject_user_map m JOIN live_room r ON r.room_id=m.subject_id AND r.status='0'
JOIN sys_user u ON u.user_id=m.user_id AND u.status='0' AND u.del_flag='0' WHERE m.subject_type='ROOM' AND m.role_code='anchor';

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '主播App软件更新',COALESCE(@ops_parent,0),98,'http://47.116.62.234/anchor-console/anchor-releases.html',NULL,1,0,'C','0','0','system:appRelease:manage','upload','anchor-app',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='system:appRelease:manage');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id) SELECT 1,menu_id FROM sys_menu WHERE perms IN ('system:appRelease:manage','live:metrics:report');
