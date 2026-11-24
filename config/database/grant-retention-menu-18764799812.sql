-- Grant only the retention-center menu to one existing active user.
-- Does not grant shop administration or assign orders. Safe to rerun.
DROP PROCEDURE IF EXISTS grant_retention_menu_18764799812;
DELIMITER $$
CREATE PROCEDURE grant_retention_menu_18764799812()
BEGIN
  DECLARE target_user_id BIGINT;
  DECLARE target_role_id BIGINT;
  DECLARE parent_menu_id BIGINT;
  DECLARE page_menu_id BIGINT;
  DECLARE match_count INT;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT COUNT(*) INTO match_count
  FROM sys_user
  WHERE (phonenumber='18764799812' OR user_name='18764799812')
    AND status='0' AND del_flag='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected exactly one active user for 18764799812';
  END IF;
  SELECT user_id INTO target_user_id
  FROM sys_user
  WHERE (phonenumber='18764799812' OR user_name='18764799812')
    AND status='0' AND del_flag='0';

  SELECT COUNT(*) INTO match_count
  FROM sys_menu
  WHERE path='live-ops' AND menu_type='M' AND status='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Live operations parent menu missing or duplicated';
  END IF;
  SELECT menu_id INTO parent_menu_id
  FROM sys_menu WHERE path='live-ops' AND menu_type='M' AND status='0';

  SELECT COUNT(*) INTO match_count
  FROM sys_menu
  WHERE component='live/retention/index' AND menu_type='C'
    AND parent_id=parent_menu_id AND status='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Retention center menu missing or duplicated';
  END IF;
  SELECT menu_id INTO page_menu_id
  FROM sys_menu
  WHERE component='live/retention/index' AND menu_type='C'
    AND parent_id=parent_menu_id AND status='0';

  SELECT COUNT(*) INTO match_count
  FROM sys_role
  WHERE role_key='retention_menu_18764799812';
  IF match_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated retention role is duplicated';
  END IF;
  IF match_count = 0 THEN
    INSERT INTO sys_role (
      role_name,role_key,role_sort,data_scope,menu_check_strictly,
      dept_check_strictly,status,del_flag,create_by,create_time,remark
    ) VALUES (
      '挽单中心查看-18764799812','retention_menu_18764799812',99,'5','1',
      '1','0','0','retention-menu-grant',NOW(),'仅授予挽单与转推中心菜单'
    );
    SET target_role_id=LAST_INSERT_ID();
  ELSE
    SELECT role_id INTO target_role_id
    FROM sys_role WHERE role_key='retention_menu_18764799812';
    SELECT COUNT(*) INTO match_count
    FROM sys_role WHERE role_id=target_role_id AND status='0' AND del_flag='0';
    IF match_count <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated retention role is disabled';
    END IF;
    SELECT COUNT(*) INTO match_count
    FROM sys_user_role WHERE role_id=target_role_id AND user_id<>target_user_id;
    IF match_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated retention role is shared by another user';
    END IF;
    SELECT COUNT(*) INTO match_count
    FROM sys_role_menu
    WHERE role_id=target_role_id AND menu_id NOT IN (parent_menu_id,page_menu_id);
    IF match_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated retention role has unexpected menus';
    END IF;
  END IF;

  INSERT INTO sys_role_menu(role_id,menu_id)
  SELECT target_role_id,parent_menu_id
  WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu
    WHERE role_id=target_role_id AND menu_id=parent_menu_id
  );
  INSERT INTO sys_role_menu(role_id,menu_id)
  SELECT target_role_id,page_menu_id
  WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu
    WHERE role_id=target_role_id AND menu_id=page_menu_id
  );
  INSERT INTO sys_user_role(user_id,role_id)
  SELECT target_user_id,target_role_id
  WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role
    WHERE user_id=target_user_id AND role_id=target_role_id
  );

  COMMIT;

  SELECT u.user_id,u.user_name,r.role_name,m.menu_name,m.component
  FROM sys_user u
  JOIN sys_user_role ur ON ur.user_id=u.user_id
  JOIN sys_role r ON r.role_id=ur.role_id
  JOIN sys_role_menu rm ON rm.role_id=r.role_id
  JOIN sys_menu m ON m.menu_id=rm.menu_id
  WHERE u.user_id=target_user_id AND r.role_id=target_role_id
  ORDER BY m.menu_id;
END$$
DELIMITER ;

CALL grant_retention_menu_18764799812();
DROP PROCEDURE grant_retention_menu_18764799812;
