-- Apply before deploying the staff-group code. Existing per-shop assignments are unchanged.
CREATE TABLE IF NOT EXISTS live_shop_staff_group (
  group_id BIGINT NOT NULL AUTO_INCREMENT,
  group_name VARCHAR(100) NOT NULL,
  created_by VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_by VARCHAR(64) NULL,
  updated_at DATETIME NULL,
  PRIMARY KEY (group_id),
  UNIQUE KEY uk_live_shop_staff_group_name (group_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shared shop staffing groups';

CREATE TABLE IF NOT EXISTS live_shop_staff_group_member (
  group_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  PRIMARY KEY (group_id,user_id,role_code),
  KEY idx_staff_group_member_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Members of shared shop staffing groups';

CREATE TABLE IF NOT EXISTS live_shop_staff_group_shop (
  shop_id BIGINT NOT NULL,
  group_id BIGINT NOT NULL,
  PRIMARY KEY (shop_id),
  KEY idx_staff_group_shop_group (group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Shop staffing inheritance';

CREATE TABLE IF NOT EXISTS live_auto_access_rule (
  rule_code VARCHAR(64) NOT NULL,
  subject_type VARCHAR(16) NOT NULL,
  role_code VARCHAR(32) NOT NULL,
  grant_role_id BIGINT NOT NULL,
  PRIMARY KEY (rule_code,subject_type,role_code),
  KEY idx_live_auto_access_role (grant_role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Business staffing to page role rules';

DROP PROCEDURE IF EXISTS verify_live_retention_auto_access;
DELIMITER $$
CREATE PROCEDURE verify_live_retention_auto_access()
BEGIN
  DECLARE parent_count INT;
  DECLARE page_count INT;
  DECLARE role_count INT;
  DECLARE unexpected_menu_count INT;

  SELECT COUNT(*) INTO parent_count FROM sys_menu
  WHERE path='live-ops' AND menu_type='M' AND status='0';
  SELECT COUNT(*) INTO page_count FROM sys_menu page
  JOIN sys_menu parent ON parent.menu_id=page.parent_id
  WHERE page.component='live/retention/index' AND page.menu_type='C' AND page.status='0'
    AND parent.path='live-ops' AND parent.menu_type='M' AND parent.status='0';
  SELECT COUNT(*) INTO role_count FROM sys_role WHERE role_key='auto_live_retention';
  IF parent_count<>1 OR page_count<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Retention menu missing, duplicated, or under wrong parent';
  END IF;
  IF role_count>1 OR (role_count=1 AND EXISTS (
    SELECT 1 FROM sys_role WHERE role_key='auto_live_retention'
      AND (status<>'0' OR del_flag<>'0')
  )) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Automatic retention role duplicated or disabled';
  END IF;
  SELECT COUNT(*) INTO unexpected_menu_count FROM sys_role_menu rm
  JOIN sys_role r ON r.role_id=rm.role_id
  WHERE r.role_key='auto_live_retention' AND rm.menu_id NOT IN (
    SELECT menu_id FROM sys_menu WHERE path='live-ops' AND menu_type='M'
    UNION ALL
    SELECT menu_id FROM sys_menu WHERE component='live/retention/index' AND menu_type='C'
  );
  IF unexpected_menu_count>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Automatic retention role has unexpected menus';
  END IF;
END$$
DELIMITER ;
CALL verify_live_retention_auto_access();
DROP PROCEDURE verify_live_retention_auto_access;

INSERT INTO sys_role (role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
SELECT '挽单中心自动授权','auto_live_retention',98,'5','1','1','0','0','system',NOW(),'由店铺客服配置自动管理，不在用户管理中手动分配'
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_key='auto_live_retention');

INSERT INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
WHERE r.role_key='auto_live_retention' AND r.status='0' AND r.del_flag='0'
  AND ((m.path='live-ops' AND m.menu_type='M') OR (m.component='live/retention/index' AND m.menu_type='C' AND m.parent_id=(SELECT menu_id FROM sys_menu WHERE path='live-ops' AND menu_type='M' AND status='0')))
  AND m.status='0' AND NOT EXISTS (SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

UPDATE sys_menu SET perms=CASE WHEN perms IS NULL OR perms='' THEN 'live:retention:access'
  ELSE CONCAT(perms,',live:retention:access') END
WHERE component='live/retention/index' AND menu_type='C'
  AND FIND_IN_SET('live:retention:access',COALESCE(perms,''))=0;

INSERT INTO live_auto_access_rule(rule_code,subject_type,role_code,grant_role_id)
SELECT 'retention_staff','SHOP','customer_service',role_id FROM sys_role WHERE role_key='auto_live_retention'
ON DUPLICATE KEY UPDATE grant_role_id=VALUES(grant_role_id);
INSERT INTO live_auto_access_rule(rule_code,subject_type,role_code,grant_role_id)
SELECT 'retention_staff','SHOP','customer_admin',role_id FROM sys_role WHERE role_key='auto_live_retention'
ON DUPLICATE KEY UPDATE grant_role_id=VALUES(grant_role_id);

-- Existing shop staff receive the managed role on application startup. Verify the
-- two role-menu links and the two rules before starting the new application.
SELECT r.role_id,r.role_key,m.menu_id,m.menu_name,m.perms
FROM sys_role r JOIN sys_role_menu rm ON rm.role_id=r.role_id
JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE r.role_key='auto_live_retention';
SELECT * FROM live_auto_access_rule WHERE rule_code='retention_staff';
