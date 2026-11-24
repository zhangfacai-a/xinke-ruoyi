-- One row per retention order. Run after add-order-retention-center.sql and
-- upgrade-retention-evidence-and-duration.sql, before deploying the sync task.
CREATE TABLE IF NOT EXISTS bi_live_retention_order_fact (
  retention_id BIGINT NOT NULL COMMENT 'Source live_retention_order.id',
  business_date DATE NOT NULL COMMENT 'Source order creation date',
  shop_id BIGINT NOT NULL,
  shop_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'Shop name at last sync',
  order_no VARCHAR(64) NOT NULL,
  owner_id BIGINT NULL,
  owner_name VARCHAR(100) NOT NULL DEFAULT '' COMMENT 'Owner name at last sync',
  progress VARCHAR(16) NOT NULL,
  retention_result VARCHAR(16) NOT NULL,
  retention_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  referral_success TINYINT(1) NOT NULL DEFAULT 0,
  referral_shop_id BIGINT NULL,
  referral_shop_name VARCHAR(100) NOT NULL DEFAULT '',
  referral_order_no VARCHAR(64) NOT NULL DEFAULT '',
  referral_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  freight_collected TINYINT(1) NOT NULL DEFAULT 0,
  freight_amount DECIMAL(14,2) NOT NULL DEFAULT 0,
  first_followed_at DATETIME NULL,
  completed_at DATETIME NULL,
  completion_seconds BIGINT NULL COMMENT 'Completed at minus created at, in seconds',
  source_version INT NOT NULL DEFAULT 0,
  source_created_at DATETIME NOT NULL,
  source_updated_at DATETIME NOT NULL,
  synced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '1 when source order was deleted',
  PRIMARY KEY (retention_id),
  KEY idx_bi_retention_date_shop (business_date,shop_id,is_deleted),
  KEY idx_bi_retention_owner_date (owner_id,business_date,is_deleted),
  KEY idx_bi_retention_result_date (retention_result,business_date,is_deleted),
  KEY idx_bi_retention_updated (source_updated_at,retention_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI retention and referral order detail';

-- Initial load; rerunning refreshes current source rows without duplicating them.
INSERT INTO bi_live_retention_order_fact (
  retention_id,business_date,shop_id,shop_name,order_no,owner_id,owner_name,
  progress,retention_result,retention_amount,referral_success,referral_shop_id,
  referral_shop_name,referral_order_no,referral_amount,freight_collected,freight_amount,
  first_followed_at,completed_at,completion_seconds,source_version,
  source_created_at,source_updated_at,synced_at,is_deleted
)
SELECT o.id,DATE(o.created_at),o.shop_id,COALESCE(s.shop_name,''),o.order_no,
  o.owner_id,COALESCE(u.nick_name,''),o.progress,o.retention_result,o.retention_amount,
  o.referral_success,o.referral_shop_id,COALESCE(rs.shop_name,''),o.referral_order_no,
  o.referral_amount,o.freight_collected,o.freight_amount,o.first_followed_at,o.completed_at,
  CASE WHEN o.progress='COMPLETED' AND o.completed_at>=o.created_at
    THEN TIMESTAMPDIFF(SECOND,o.created_at,o.completed_at) ELSE NULL END,
  o.version,o.created_at,o.updated_at,NOW(),0
FROM live_retention_order o
LEFT JOIN erp_shop s ON s.shop_id=o.shop_id
LEFT JOIN sys_user u ON u.user_id=o.owner_id
LEFT JOIN erp_shop rs ON rs.shop_id=o.referral_shop_id
ON DUPLICATE KEY UPDATE
  business_date=VALUES(business_date),shop_id=VALUES(shop_id),shop_name=VALUES(shop_name),
  order_no=VALUES(order_no),owner_id=VALUES(owner_id),owner_name=VALUES(owner_name),
  progress=VALUES(progress),retention_result=VALUES(retention_result),
  retention_amount=VALUES(retention_amount),referral_success=VALUES(referral_success),
  referral_shop_id=VALUES(referral_shop_id),referral_shop_name=VALUES(referral_shop_name),
  referral_order_no=VALUES(referral_order_no),referral_amount=VALUES(referral_amount),
  freight_collected=VALUES(freight_collected),freight_amount=VALUES(freight_amount),
  first_followed_at=VALUES(first_followed_at),completed_at=VALUES(completed_at),
  completion_seconds=VALUES(completion_seconds),source_version=VALUES(source_version),
  source_created_at=VALUES(source_created_at),source_updated_at=VALUES(source_updated_at),
  synced_at=NOW(),is_deleted=0;

UPDATE bi_live_retention_order_fact b
LEFT JOIN live_retention_order o ON o.id=b.retention_id
SET b.is_deleted=1,b.synced_at=NOW()
WHERE o.id IS NULL AND b.is_deleted=0;
