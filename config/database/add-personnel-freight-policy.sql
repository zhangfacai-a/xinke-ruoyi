-- Explicit personal freight policy. No row means retain existing role/business rules.
CREATE TABLE IF NOT EXISTS live_personnel_freight_policy (
 user_id BIGINT PRIMARY KEY COMMENT '登录账号ID，不以姓名关联',
 actions VARCHAR(100) NOT NULL COMMENT '允许动作，以逗号分隔',
 view_scope VARCHAR(16) NOT NULL COMMENT 'OWN/ASSIGNED/SELECTED/ALL',
 operate_scope VARCHAR(16) NOT NULL COMMENT 'OWN/ASSIGNED/SELECTED/ALL',
 view_shop_ids TEXT NOT NULL COMMENT '指定查看店铺ID，逗号分隔',
 operate_shop_ids TEXT NOT NULL COMMENT '指定操作店铺ID，逗号分隔',
 version BIGINT NOT NULL DEFAULT 1,
 updated_by BIGINT NOT NULL,
 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员运费权限配置，独立控制查看和操作';
CREATE TABLE IF NOT EXISTS live_personnel_access_audit (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,
 user_id BIGINT NOT NULL,
 business VARCHAR(32) NOT NULL,
 before_config TEXT NOT NULL,
 after_config TEXT NOT NULL,
 actor_id BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 KEY idx_personnel_audit(user_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='人员业务权限变更记录';
