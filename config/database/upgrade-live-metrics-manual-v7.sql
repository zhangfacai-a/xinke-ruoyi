-- V7：支持主播记录自定义时间与手工指标。
-- 自动计算记录继续绑定快照；手工记录允许快照为空，完整修改历史写入 correction 表。

SET @start_nullable=(
  SELECT is_nullable FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='live_metric_anchor_segment' AND column_name='start_snapshot_id'
  LIMIT 1
);
SET @ddl=IF(
  @start_nullable='NO',
  'ALTER TABLE live_metric_anchor_segment MODIFY COLUMN start_snapshot_id BIGINT NULL',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

