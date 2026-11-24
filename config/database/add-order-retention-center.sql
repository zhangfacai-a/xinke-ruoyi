-- 挽单与转推中心；可重复执行，不导入示例客户资料。
CREATE TABLE IF NOT EXISTS live_retention_order (
 id BIGINT NOT NULL AUTO_INCREMENT,
 shop_id BIGINT NOT NULL,
 order_no VARCHAR(64) NOT NULL,
 address VARCHAR(500) NOT NULL DEFAULT '',
 owner_id BIGINT NULL,
 progress VARCHAR(16) NOT NULL DEFAULT 'PENDING',
 retention_result VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',
 retention_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
 referral_success TINYINT NOT NULL DEFAULT 0,
 referral_shop_id BIGINT NULL,
 referral_order_no VARCHAR(64) NOT NULL DEFAULT '',
 referral_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
 freight_collected TINYINT NOT NULL DEFAULT 0,
 freight_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
 first_followed_at DATETIME NULL,
 completed_at DATETIME NULL,
 latest_note VARCHAR(2000) NOT NULL DEFAULT '',
 version INT NOT NULL DEFAULT 0,
 created_by BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), UNIQUE KEY uk_retention_shop_order(shop_id,order_no),
 KEY idx_retention_created(created_at,id),
 KEY idx_retention_shop_date(shop_id,created_at),
 KEY idx_retention_progress_date(progress,created_at),
 KEY idx_retention_owner_date(owner_id,created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挽单转推订单及当前结果';

CREATE TABLE IF NOT EXISTS live_retention_history (
 id BIGINT NOT NULL AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 action VARCHAR(20) NOT NULL,
 note VARCHAR(2000) NOT NULL,
 old_value TEXT NULL,
 new_value TEXT NULL,
 operator_id BIGINT NOT NULL,
 operator_name VARCHAR(100) NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), KEY idx_retention_history_order(order_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挽单转推跟进及修改记录';

CREATE TABLE IF NOT EXISTS live_retention_attachment (
 id BIGINT NOT NULL AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 object_key VARCHAR(500) NOT NULL,
 original_name VARCHAR(255) NOT NULL,
 content_type VARCHAR(100) NOT NULL,
 file_size BIGINT NOT NULL,
 uploaded_by BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id), KEY idx_retention_attachment_order(order_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挽单转推结果凭证';

-- 复用直播运营目录，给现有正常角色开放菜单。
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '直播运营',0,6,'live-ops',NULL,1,0,'M','0','0','','chart','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE path='live-ops' AND menu_type='M');
SET @parent_id=(SELECT menu_id FROM sys_menu WHERE path='live-ops' AND menu_type='M' LIMIT 1);
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '挽单与转推中心',COALESCE(@parent_id,0),8,'retention-center','live/retention/index',1,0,'C','0','0','','documentation','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE component='live/retention/index');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.status='0' AND r.del_flag='0' AND (m.component='live/retention/index' OR m.menu_id=@parent_id);
