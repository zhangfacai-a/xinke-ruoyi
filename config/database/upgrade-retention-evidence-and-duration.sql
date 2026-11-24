-- 挽单结果凭证、首次跟进修复和完成时长统计。可重复执行。

DROP PROCEDURE IF EXISTS upgrade_retention_evidence_duration;
DELIMITER $$
CREATE PROCEDURE upgrade_retention_evidence_duration()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='live_retention_order' AND column_name='completed_at'
  ) THEN
    ALTER TABLE live_retention_order ADD COLUMN completed_at DATETIME NULL AFTER first_followed_at;
  END IF;
END$$
DELIMITER ;
CALL upgrade_retention_evidence_duration();
DROP PROCEDURE IF EXISTS upgrade_retention_evidence_duration;

CREATE TABLE IF NOT EXISTS live_retention_attachment (
 id BIGINT NOT NULL AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 object_key VARCHAR(500) NOT NULL,
 original_name VARCHAR(255) NOT NULL,
 content_type VARCHAR(100) NOT NULL,
 file_size BIGINT NOT NULL,
 uploaded_by BIGINT NOT NULL,
 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(id),
 KEY idx_retention_attachment_order(order_id,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='挽单转推结果凭证';

-- 修复旧数据：跟进动作是首次跟进时间的可靠来源。
UPDATE live_retention_order o
JOIN (
  SELECT order_id,MIN(created_at) first_at
  FROM live_retention_history
  WHERE action='FOLLOWUP'
  GROUP BY order_id
) h ON h.order_id=o.id
SET o.first_followed_at=h.first_at
WHERE o.first_followed_at IS NULL;

-- 旧记录没有明确的完成时间时，优先使用首次写入 COMPLETED 的审计时间。
UPDATE live_retention_order o
JOIN (
  SELECT order_id,MIN(created_at) completed_at
  FROM live_retention_history
  WHERE new_value LIKE '%"progress":"COMPLETED"%'
  GROUP BY order_id
) h ON h.order_id=o.id
SET o.completed_at=h.completed_at
WHERE o.progress='COMPLETED' AND o.completed_at IS NULL;

-- 兜底：历史审计格式无法识别的已完成订单使用最后更新时间。
UPDATE live_retention_order
SET completed_at=updated_at
WHERE progress='COMPLETED' AND completed_at IS NULL;

