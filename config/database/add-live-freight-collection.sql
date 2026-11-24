-- Run after add-live-staff-groups-and-auto-access.sql and before deploying the feature.
CREATE TABLE IF NOT EXISTS live_freight_collection (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  shop_id BIGINT NOT NULL COMMENT '店铺ID',
  shop_name_snapshot VARCHAR(100) NOT NULL COMMENT '录入时店铺名称',
  order_no VARCHAR(64) NOT NULL COMMENT '订单号，全局唯一',
  delivery_status VARCHAR(24) NOT NULL COMMENT 'SIGNED已签收；SHIPPED_UNSIGNED已发货未签收',
  freight_amount DECIMAL(14,2) NOT NULL COMMENT '实际征收运费，元',
  deduction_method VARCHAR(24) NOT NULL COMMENT 'ALIPAY_PERSONAL支付宝个人账户；ORDER_PAYMENT订单打款',
  deduction_rate DECIMAL(5,4) NOT NULL COMMENT '固定比例：0.09或0.07，不参与运费金额计算',
  collected_at DATETIME NULL COMMENT '实际征收时间',
  created_by_user_id BIGINT NOT NULL COMMENT '录入人账号ID，取登录态',
  created_by_name_snapshot VARCHAR(100) NOT NULL COMMENT '录入人姓名快照',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
  updated_by_user_id BIGINT NULL COMMENT '最近修改人账号ID',
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近修改时间',
  remark VARCHAR(500) NULL COMMENT '备注',
  version INT NOT NULL DEFAULT 0 COMMENT '编辑版本',
  PRIMARY KEY (id),
  UNIQUE KEY uk_live_freight_order_no (order_no),
  KEY idx_live_freight_created_shop (created_at,shop_id),
  KEY idx_live_freight_status_created (delivery_status,created_at),
  KEY idx_live_freight_creator_created (created_by_user_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运费征收明细';

-- The existing live-ops parent and automatic-access infrastructure must exist.
DROP PROCEDURE IF EXISTS verify_live_freight_menu;
DELIMITER $$
CREATE PROCEDURE verify_live_freight_menu()
BEGIN
  IF (SELECT COUNT(*) FROM sys_menu WHERE path='live-ops' AND menu_type='M' AND status='0')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Expected one active live-ops parent menu';
  END IF;
  IF (SELECT COUNT(*) FROM sys_menu WHERE component='live/freight/index')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Freight menu duplicated';
  END IF;
  IF EXISTS(SELECT 1 FROM sys_menu m LEFT JOIN sys_menu p ON p.menu_id=m.parent_id
    WHERE m.component='live/freight/index' AND
      (m.menu_type<>'C' OR m.status<>'0' OR m.path<>'freight-collection'
       OR COALESCE(m.perms,'')<>'live:freight:access' OR p.menu_id IS NULL
       OR p.path<>'live-ops' OR p.menu_type<>'M' OR p.status<>'0')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing freight menu has unexpected configuration';
  END IF;
  IF (SELECT COUNT(*) FROM sys_role WHERE role_key='auto_live_freight')>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Freight automatic role duplicated';
  END IF;
  IF EXISTS(SELECT 1 FROM sys_role WHERE role_key='auto_live_freight' AND (status<>'0' OR del_flag<>'0')) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Existing freight automatic role is disabled';
  END IF;
  IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='live_auto_access_rule')<>1 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Apply staff auto-access migration first';
  END IF;
END$$
DELIMITER ;
CALL verify_live_freight_menu();
DROP PROCEDURE verify_live_freight_menu;

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '运费征收明细',p.menu_id,9,'freight-collection','live/freight/index',1,0,'C','0','0','live:freight:access','money','system',NOW()
FROM sys_menu p WHERE p.path='live-ops' AND p.menu_type='M' AND p.status='0'
AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE component='live/freight/index');

UPDATE sys_menu SET menu_name='运费征收明细'
WHERE component='live/freight/index' AND menu_type='C';

INSERT INTO sys_role(role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
SELECT '运费征收自动授权','auto_live_freight',98,'5','1','1','0','0','system',NOW(),'店铺客服和客服管理员自动获得运费征收明细权限'
WHERE NOT EXISTS(SELECT 1 FROM sys_role WHERE role_key='auto_live_freight');

INSERT INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
WHERE r.role_key='auto_live_freight' AND r.status='0' AND r.del_flag='0' AND m.status='0'
AND ((m.path='live-ops' AND m.menu_type='M') OR (m.component='live/freight/index' AND m.menu_type='C'))
AND NOT EXISTS(SELECT 1 FROM sys_role_menu rm WHERE rm.role_id=r.role_id AND rm.menu_id=m.menu_id);

INSERT INTO live_auto_access_rule(rule_code,subject_type,role_code,grant_role_id)
SELECT 'freight_staff','SHOP','customer_service',role_id FROM sys_role WHERE role_key='auto_live_freight' AND status='0' AND del_flag='0'
ON DUPLICATE KEY UPDATE grant_role_id=VALUES(grant_role_id);
INSERT INTO live_auto_access_rule(rule_code,subject_type,role_code,grant_role_id)
SELECT 'freight_staff','SHOP','customer_admin',role_id FROM sys_role WHERE role_key='auto_live_freight' AND status='0' AND del_flag='0'
ON DUPLICATE KEY UPDATE grant_role_id=VALUES(grant_role_id);

SELECT r.role_id,r.role_key,m.menu_id,m.menu_name FROM sys_role r JOIN sys_role_menu rm ON rm.role_id=r.role_id
JOIN sys_menu m ON m.menu_id=rm.menu_id WHERE r.role_key='auto_live_freight';
SELECT rule_code,subject_type,role_code,grant_role_id FROM live_auto_access_rule WHERE rule_code='freight_staff';
