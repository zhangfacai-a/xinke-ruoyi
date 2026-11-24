-- 在应用数据库执行整个脚本（MySQL）。仅生成脚本，不自动执行。
-- 沿用系统的店铺删除方式 status='2'，清除被删店铺人员配置，保留历史订单引用。
-- 保留店铺现有运营/客服配置，额外添加指定人员；可重复执行。
-- 姓名按 sys_user.nick_name 匹配；缺失、停用、同名或店铺缺失时全部回滚。
SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS configure_retention_shop_staff_20261013;
DELIMITER $$
CREATE PROCEDURE configure_retention_shop_staff_20261013()
BEGIN
  DECLARE invalid_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    DROP TEMPORARY TABLE IF EXISTS tmp_keep_retention_shop;
    DROP TEMPORARY TABLE IF EXISTS tmp_retention_staff;
    RESIGNAL;
  END;

  DROP TEMPORARY TABLE IF EXISTS tmp_keep_retention_shop;
  DROP TEMPORARY TABLE IF EXISTS tmp_retention_staff;
  CREATE TEMPORARY TABLE tmp_keep_retention_shop (
    shop_name VARCHAR(100) NOT NULL PRIMARY KEY,
    shop_id BIGINT NULL
  ) ENGINE=InnoDB;
  CREATE TEMPORARY TABLE tmp_retention_staff (
    person_name VARCHAR(100) NOT NULL PRIMARY KEY,
    role_code VARCHAR(32) NOT NULL,
    user_id BIGINT NULL
  ) ENGINE=InnoDB;

  INSERT INTO tmp_keep_retention_shop(shop_name) VALUES
  ('小天鹅Littleswan电器旗舰店'),
  ('小天鹅厨房电器旗舰店'),
  ('海尔海盟专卖店'),
  ('美的鑫多专卖店'),
  ('美的空调套装旗舰店'),
  ('美的全自动洗衣机旗舰店'),
  ('美的朋乐专卖店'),
  ('小天鹅烘干机旗舰店'),
  ('美的初一大家电专卖店'),
  ('小天鹅朋悦专卖店'),
  ('小天鹅宸华专卖店'),
  ('美的洗烘套装旗舰店');

  INSERT INTO tmp_retention_staff(person_name,role_code) VALUES
  ('李龙','customer_admin'),
  ('张帅','customer_admin'),
  ('王修晨','customer_service'),
  ('徐顺','customer_service'),
  ('万莹','customer_service'),
  ('赵丹丹','customer_service');

  START TRANSACTION;
  SELECT COUNT(*) INTO invalid_count
  FROM tmp_keep_retention_shop t
  WHERE (SELECT COUNT(*) FROM erp_shop s WHERE s.shop_name=t.shop_name) <> 1;
  IF invalid_count > 0 THEN
    SELECT t.shop_name, (SELECT COUNT(*) FROM erp_shop s WHERE s.shop_name=t.shop_name) matching_count
    FROM tmp_keep_retention_shop t
    WHERE (SELECT COUNT(*) FROM erp_shop s WHERE s.shop_name=t.shop_name) <> 1;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Required shop missing or duplicated; no changes applied';
  END IF;

  SELECT COUNT(*) INTO invalid_count
  FROM tmp_retention_staff t
  WHERE (SELECT COUNT(*) FROM sys_user u WHERE u.nick_name=t.person_name AND u.del_flag='0') <> 1
     OR (SELECT COUNT(*) FROM sys_user u WHERE u.nick_name=t.person_name AND u.del_flag='0' AND u.status='0') <> 1;
  IF invalid_count > 0 THEN
    SELECT t.person_name,
      (SELECT COUNT(*) FROM sys_user u WHERE u.nick_name=t.person_name AND u.del_flag='0') matching_count,
      (SELECT COUNT(*) FROM sys_user u WHERE u.nick_name=t.person_name AND u.del_flag='0' AND u.status='0') active_count
    FROM tmp_retention_staff t;
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Required user missing, duplicated or disabled; no changes applied';
  END IF;

  UPDATE tmp_keep_retention_shop t JOIN erp_shop s ON s.shop_name=t.shop_name
  SET t.shop_id=s.shop_id;
  UPDATE tmp_retention_staff t JOIN sys_user u ON u.nick_name=t.person_name AND u.del_flag='0' AND u.status='0'
  SET t.user_id=u.user_id;

  -- 删除名单之外的店铺（与网页删除按钮一致）。
  DELETE m FROM live_subject_user_map m
  LEFT JOIN tmp_keep_retention_shop t ON t.shop_id=m.subject_id
  WHERE m.subject_type='SHOP' AND t.shop_id IS NULL;
  UPDATE erp_shop s LEFT JOIN tmp_keep_retention_shop t ON t.shop_id=s.shop_id
  SET s.status='2',s.update_by='shop-staff-migration',s.update_time=NOW()
  WHERE t.shop_id IS NULL AND s.status<>'2';

  UPDATE erp_shop s JOIN tmp_keep_retention_shop t ON t.shop_id=s.shop_id
  SET s.status='0',s.update_by='shop-staff-migration',s.update_time=NOW();

  INSERT INTO live_subject_user_map(subject_type,subject_id,user_id,role_code,create_by,create_time)
  SELECT 'SHOP',s.shop_id,u.user_id,u.role_code,'shop-staff-migration',NOW()
  FROM tmp_keep_retention_shop s CROSS JOIN tmp_retention_staff u
  WHERE NOT EXISTS (
    SELECT 1 FROM live_subject_user_map m
    WHERE m.subject_type='SHOP' AND m.subject_id=s.shop_id
      AND m.user_id=u.user_id AND m.role_code=u.role_code
  );
  COMMIT;

  SELECT s.shop_name AS '店铺',
    GROUP_CONCAT(DISTINCT CASE WHEN m.role_code='customer_admin' THEN u.nick_name END ORDER BY u.nick_name SEPARATOR '、') AS '客服管理员',
    GROUP_CONCAT(DISTINCT CASE WHEN m.role_code='customer_service' THEN u.nick_name END ORDER BY u.nick_name SEPARATOR '、') AS '电话客服'
  FROM erp_shop s JOIN tmp_keep_retention_shop t ON t.shop_id=s.shop_id
  LEFT JOIN live_subject_user_map m ON m.subject_type='SHOP' AND m.subject_id=s.shop_id
  LEFT JOIN sys_user u ON u.user_id=m.user_id
  GROUP BY s.shop_id,s.shop_name ORDER BY s.shop_id;

  DROP TEMPORARY TABLE tmp_keep_retention_shop;
  DROP TEMPORARY TABLE tmp_retention_staff;
END$$
DELIMITER ;
CALL configure_retention_shop_staff_20261013();
DROP PROCEDURE IF EXISTS configure_retention_shop_staff_20261013;
