-- 直播数据采集 V4：设备心跳、断播多场计数和小服务器查询索引。
CREATE TABLE IF NOT EXISTS live_metric_automation_device (
  worker_id VARCHAR(80) NOT NULL COMMENT '设备唯一编号',
  device_name VARCHAR(120) NOT NULL DEFAULT '' COMMENT '电脑名称',
  room_id BIGINT NULL COMMENT '负责直播间',
  client_version VARCHAR(32) NOT NULL DEFAULT '' COMMENT '采集助手版本',
  plugin_version VARCHAR(32) NOT NULL DEFAULT '' COMMENT '采集插件版本',
  enabled TINYINT NOT NULL DEFAULT 1,
  current_task_id BIGINT NULL,
  last_error VARCHAR(500) NULL,
  last_seen_at DATETIME NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL,
  PRIMARY KEY (worker_id),
  KEY idx_metric_device_room_seen (room_id,last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播数据自动采集设备';

SET @column_exists=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='live_metric_automation_task' AND column_name='submission_count');
SET @ddl=IF(@column_exists=0,'ALTER TABLE live_metric_automation_task ADD COLUMN submission_count INT NOT NULL DEFAULT 0 AFTER submission_id','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @column_exists=(SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='live_metric_submission' AND column_name='anomaly_fields');
SET @ddl=IF(@column_exists=0,'ALTER TABLE live_metric_submission ADD COLUMN anomaly_fields VARCHAR(255) NULL AFTER match_status','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists=(SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='live_metric_submission' AND index_name='idx_metric_submission_report');
SET @ddl=IF(@index_exists=0,'ALTER TABLE live_metric_submission ADD KEY idx_metric_submission_report (submitted_at,match_status,session_id,shift_id)','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists=(SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='live_metric_submission' AND index_name='idx_metric_submission_latest');
SET @ddl=IF(@index_exists=0,'ALTER TABLE live_metric_submission ADD KEY idx_metric_submission_latest (session_id,shift_id,status,is_effective,snapshot_at,submission_id)','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @index_exists=(SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='live_metric_correction' AND index_name='idx_metric_correction_submission_time');
SET @ddl=IF(@index_exists=0,'ALTER TABLE live_metric_correction ADD KEY idx_metric_correction_submission_time (submission_id,corrected_at)','SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
