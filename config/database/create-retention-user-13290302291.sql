-- Create the login 13290302291 for Yu Weihe 2 and grant only the retention menu.
-- Run in xinkeerp with UTF-8 encoding. Safe to rerun; never resets an existing password.
-- Initial password is supplied separately. Ask the user to change it after first login.
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS create_retention_user_13290302291;
DELIMITER $$
CREATE PROCEDURE create_retention_user_13290302291()
BEGIN
  DECLARE target_user_id BIGINT;
  DECLARE target_role_id BIGINT;
  DECLARE parent_menu_id BIGINT;
  DECLARE page_menu_id BIGINT;
  DECLARE target_dept_id BIGINT;
  DECLARE match_count INT;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  -- Match the existing Yu Weihe account's department without copying its access.
  SELECT COUNT(*) INTO match_count FROM sys_user
  WHERE user_id=362 AND nick_name='于维贺' AND status='0' AND del_flag='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reference account Yu Weihe changed; verify department first';
  END IF;
  SELECT dept_id INTO target_dept_id FROM sys_user WHERE user_id=362;
  SELECT COUNT(*) INTO match_count FROM sys_dept
  WHERE dept_id=target_dept_id AND status='0' AND del_flag='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Reference department is not active';
  END IF;

  SELECT COUNT(*) INTO match_count FROM sys_user
  WHERE user_name='13290302291' OR phonenumber='13290302291';
  IF match_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Login and phone belong to different users';
  END IF;
  IF match_count = 0 THEN
    INSERT INTO sys_user (
      dept_id,user_name,nick_name,phonenumber,password,status,del_flag,
      create_by,create_time,remark
    ) VALUES (
      target_dept_id,'13290302291','于维贺2','13290302291',
      '$2b$10$i3OSoVKM/VTe/h1RWMByTOuKeWayKyT0iwFd4L73T7LvdBKHko/Pu',
      '0','0','retention-menu-grant',NOW(),'挽单与转推中心独立账号'
    );
    SET target_user_id=LAST_INSERT_ID();
  ELSE
    SELECT COUNT(*) INTO match_count FROM sys_user
    WHERE user_name='13290302291' AND phonenumber='13290302291'
      AND nick_name='于维贺2' AND dept_id=target_dept_id
      AND status='0' AND del_flag='0';
    IF match_count <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing account details differ; no changes applied';
    END IF;
    SELECT user_id INTO target_user_id FROM sys_user
    WHERE user_name='13290302291' AND phonenumber='13290302291';
  END IF;

  SELECT COUNT(*) INTO match_count FROM sys_menu
  WHERE path='live-ops' AND menu_type='M' AND status='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Live operations parent menu missing or duplicated';
  END IF;
  SELECT menu_id INTO parent_menu_id FROM sys_menu
  WHERE path='live-ops' AND menu_type='M' AND status='0';
  SELECT COUNT(*) INTO match_count FROM sys_menu
  WHERE component='live/retention/index' AND menu_type='C'
    AND parent_id=parent_menu_id AND status='0';
  IF match_count <> 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Retention center menu missing or duplicated';
  END IF;
  SELECT menu_id INTO page_menu_id FROM sys_menu
  WHERE component='live/retention/index' AND menu_type='C'
    AND parent_id=parent_menu_id AND status='0';

  SELECT COUNT(*) INTO match_count FROM sys_role
  WHERE role_key='retention_menu_13290302291';
  IF match_count > 1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated role is duplicated';
  END IF;
  IF match_count = 0 THEN
    INSERT INTO sys_role (
      role_name,role_key,role_sort,data_scope,menu_check_strictly,
      dept_check_strictly,status,del_flag,create_by,create_time,remark
    ) VALUES (
      '挽单中心查看-13290302291','retention_menu_13290302291',99,'5','1',
      '1','0','0','retention-menu-grant',NOW(),'仅授予挽单与转推中心菜单'
    );
    SET target_role_id=LAST_INSERT_ID();
  ELSE
    SELECT role_id INTO target_role_id FROM sys_role
    WHERE role_key='retention_menu_13290302291';
    SELECT COUNT(*) INTO match_count FROM sys_role
    WHERE role_id=target_role_id AND status='0' AND del_flag='0';
    IF match_count <> 1 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated role is disabled';
    END IF;
    SELECT COUNT(*) INTO match_count FROM sys_user_role
    WHERE role_id=target_role_id AND user_id<>target_user_id;
    IF match_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated role is shared by another user';
    END IF;
    SELECT COUNT(*) INTO match_count FROM sys_role_menu
    WHERE role_id=target_role_id AND menu_id NOT IN (parent_menu_id,page_menu_id);
    IF match_count > 0 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Dedicated role has unexpected menus';
    END IF;
  END IF;

  INSERT INTO sys_role_menu(role_id,menu_id)
  SELECT target_role_id,parent_menu_id WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu
    WHERE role_id=target_role_id AND menu_id=parent_menu_id
  );
  INSERT INTO sys_role_menu(role_id,menu_id)
  SELECT target_role_id,page_menu_id WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_menu
    WHERE role_id=target_role_id AND menu_id=page_menu_id
  );
  INSERT INTO sys_user_role(user_id,role_id)
  SELECT target_user_id,target_role_id WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role
    WHERE user_id=target_user_id AND role_id=target_role_id
  );

  COMMIT;
  SELECT u.user_id,u.user_name,u.nick_name,d.dept_name,
         r.role_name,m.menu_name,m.component
  FROM sys_user u
  JOIN sys_dept d ON d.dept_id=u.dept_id
  JOIN sys_user_role ur ON ur.user_id=u.user_id
  JOIN sys_role r ON r.role_id=ur.role_id
  JOIN sys_role_menu rm ON rm.role_id=r.role_id
  JOIN sys_menu m ON m.menu_id=rm.menu_id
  WHERE u.user_id=target_user_id AND r.role_id=target_role_id
  ORDER BY m.menu_id;
END$$
DELIMITER ;

CALL create_retention_user_13290302291();
DROP PROCEDURE create_retention_user_13290302291;
