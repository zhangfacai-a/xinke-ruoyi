-- 直播经营数据自动采集任务。可重复执行；不会修改现有上报数据。

CREATE TABLE IF NOT EXISTS live_metric_automation_task (
  task_id BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  shift_id BIGINT NOT NULL COMMENT '班次ID',
  room_id BIGINT NOT NULL COMMENT '直播间ID',
  scheduled_at DATETIME NOT NULL COMMENT '计划采集时间（下播后3分钟）',
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/RETRY/COMPLETED/FAILED',
  worker_id VARCHAR(80) NULL COMMENT '领取任务的电脑编号',
  attempt_count INT NOT NULL DEFAULT 0 COMMENT '执行次数',
  next_run_at DATETIME NOT NULL COMMENT '下次可执行时间',
  lease_until DATETIME NULL COMMENT '任务租约到期时间',
  submission_id BIGINT NULL COMMENT '成功生成的上报记录ID',
  last_error VARCHAR(500) NULL COMMENT '最近错误',
  completed_at DATETIME NULL COMMENT '完成时间',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL,
  PRIMARY KEY (task_id),
  UNIQUE KEY uk_live_metric_automation_shift (shift_id),
  KEY idx_live_metric_automation_claim (room_id,status,next_run_at,lease_until),
  KEY idx_live_metric_automation_submission (submission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播经营数据自动采集任务';

SET @shift_end_index_exists = (
  SELECT COUNT(*) FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='live_metric_shift'
    AND index_name='idx_live_metric_shift_room_end'
);
SET @ddl = IF(@shift_end_index_exists=0,
  'ALTER TABLE live_metric_shift ADD KEY idx_live_metric_shift_room_end (room_id,end_time,status)',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
