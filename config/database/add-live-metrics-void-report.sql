-- 主播上报软作废与审计字段。可重复执行。
DROP PROCEDURE IF EXISTS upgrade_live_metrics_void_report;
DELIMITER $$
CREATE PROCEDURE upgrade_live_metrics_void_report()
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='void_reason'
  ) THEN
    ALTER TABLE live_metric_anchor_segment ADD COLUMN void_reason VARCHAR(500) NULL COMMENT '作废原因' AFTER remark;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='void_by'
  ) THEN
    ALTER TABLE live_metric_anchor_segment ADD COLUMN void_by BIGINT NULL COMMENT '作废操作人' AFTER void_reason;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='void_name'
  ) THEN
    ALTER TABLE live_metric_anchor_segment ADD COLUMN void_name VARCHAR(100) NULL COMMENT '作废操作人姓名' AFTER void_by;
  END IF;
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='void_at'
  ) THEN
    ALTER TABLE live_metric_anchor_segment ADD COLUMN void_at DATETIME NULL COMMENT '作废时间' AFTER void_name;
  END IF;

  ALTER TABLE live_metric_anchor_segment
    MODIFY COLUMN status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/VOID';
END$$
DELIMITER ;
CALL upgrade_live_metrics_void_report();
DROP PROCEDURE IF EXISTS upgrade_live_metrics_void_report;
