-- 直播数据排班 V3 全新初始化。
-- 本脚本会永久清空 live_metric_* 模块数据，不迁移旧场次；不会修改 live_room、sys_user 或其他业务表。

CREATE TABLE IF NOT EXISTS live_metric_automation_task (
  task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  shift_id BIGINT NOT NULL COMMENT '班次ID',
  room_id BIGINT NOT NULL COMMENT '直播间ID',
  scheduled_at DATETIME NOT NULL COMMENT '计划采集时间',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  worker_id VARCHAR(80) NULL,
  attempt_count INT NOT NULL DEFAULT 0,
  next_run_at DATETIME NOT NULL,
  lease_until DATETIME NULL,
  submission_id BIGINT NULL,
  last_error VARCHAR(500) NULL,
  completed_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL,
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_live_metric_automation_shift (shift_id),
  KEY idx_live_metric_automation_claim (room_id,status,next_run_at,lease_until),
  KEY idx_live_metric_automation_submission (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播经营数据自动采集任务';

SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE live_metric_automation_task;
TRUNCATE TABLE live_metric_reminder_log;
TRUNCATE TABLE live_metric_correction;
TRUNCATE TABLE live_metric_shift_summary;
TRUNCATE TABLE live_metric_submission;
TRUNCATE TABLE live_metric_live_session;
TRUNCATE TABLE live_metric_shift;
TRUNCATE TABLE live_metric_room_alias;
TRUNCATE TABLE live_metric_user_hidden_staff;
SET FOREIGN_KEY_CHECKS=1;

ALTER TABLE live_metric_live_session
  MODIFY COLUMN platform_session_id VARCHAR(64) NOT NULL COMMENT '抖音单场直播 live_room_id';

SET @legacy_name_index = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='live_metric_live_session'
    AND index_name='uk_metric_session_name_start'
);
SET @ddl = IF(@legacy_name_index>0,
  'ALTER TABLE live_metric_live_session DROP INDEX uk_metric_session_name_start',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @legacy_room_index = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='live_metric_live_session'
    AND index_name='uk_metric_session_room_start'
);
SET @ddl = IF(@legacy_room_index>0,
  'ALTER TABLE live_metric_live_session DROP INDEX uk_metric_session_room_start',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @platform_index = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='live_metric_live_session'
    AND index_name='uk_live_metric_platform_session'
);
SET @ddl = IF(@platform_index=0,
  'ALTER TABLE live_metric_live_session ADD UNIQUE KEY uk_live_metric_platform_session (platform_session_id)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
