-- 主播交接 V5 全新初始化。
-- 永久清空旧排班、旧上报、提醒及自动任务数据；保留 live_room、sys_user、live_subject_user_map。

SET FOREIGN_KEY_CHECKS=0;
DROP TABLE IF EXISTS live_metric_automation_task;
DROP TABLE IF EXISTS live_metric_automation_device;
DROP TABLE IF EXISTS live_metric_reminder_log;
DROP TABLE IF EXISTS live_metric_correction;
DROP TABLE IF EXISTS live_metric_shift_summary;
DROP TABLE IF EXISTS live_metric_submission;
DROP TABLE IF EXISTS live_metric_live_session;
DROP TABLE IF EXISTS live_metric_shift;
DROP TABLE IF EXISTS live_metric_shift_template;
DROP TABLE IF EXISTS live_metric_room_alias;
DROP TABLE IF EXISTS live_metric_user_hidden_staff;
DROP TABLE IF EXISTS live_metric_segment_correction;
DROP TABLE IF EXISTS live_metric_anchor_segment;
DROP TABLE IF EXISTS live_metric_snapshot;
DROP TABLE IF EXISTS live_metric_session;
SET FOREIGN_KEY_CHECKS=1;

CREATE TABLE live_metric_session (
  session_id BIGINT NOT NULL AUTO_INCREMENT,
  room_id BIGINT NULL,
  platform_session_id VARCHAR(64) NOT NULL COMMENT '地址栏 live_room_id',
  reported_room_name VARCHAR(160) NOT NULL,
  live_start_time DATETIME NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'LIVE' COMMENT 'LIVE/ENDED/EXCEPTION',
  version INT NOT NULL DEFAULT 0,
  last_snapshot_at DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NULL,
  PRIMARY KEY (session_id),
  UNIQUE KEY uk_metric_session_platform (platform_session_id),
  KEY idx_metric_session_room_time (room_id,live_start_time),
  KEY idx_metric_session_status_time (status,last_snapshot_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播场次';

CREATE TABLE live_metric_snapshot (
  snapshot_id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  snapshot_kind VARCHAR(20) NOT NULL COMMENT 'ZERO/AUTO/START/HANDOVER/END/MANUAL',
  captured_at DATETIME NOT NULL,
  watch_total BIGINT NOT NULL DEFAULT 0,
  interaction_total BIGINT NOT NULL DEFAULT 0,
  new_fan_total BIGINT NOT NULL DEFAULT 0,
  deal_total BIGINT NOT NULL DEFAULT 0,
  gmv_total DECIMAL(18,4) NOT NULL DEFAULT 0,
  ad_cost_total DECIMAL(18,4) NOT NULL DEFAULT 0,
  presale_order_total BIGINT NOT NULL DEFAULT 0,
  exposure_watch_rate DECIMAL(10,4) NOT NULL DEFAULT 0,
  watch_follow_rate DECIMAL(10,4) NOT NULL DEFAULT 0,
  watch_interaction_rate DECIMAL(10,4) NOT NULL DEFAULT 0,
  watch_deal_rate DECIMAL(10,4) NOT NULL DEFAULT 0,
  idempotency_key VARCHAR(100) NOT NULL,
  source VARCHAR(20) NOT NULL DEFAULT 'PLUGIN',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (snapshot_id),
  UNIQUE KEY uk_metric_snapshot_idempotency (idempotency_key),
  KEY idx_metric_snapshot_session_time (session_id,captured_at,snapshot_id),
  CONSTRAINT fk_metric_snapshot_session FOREIGN KEY (session_id) REFERENCES live_metric_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播累计数据快照';

CREATE TABLE live_metric_anchor_segment (
  segment_id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  anchor_user_id BIGINT NOT NULL,
  start_snapshot_id BIGINT NULL,
  end_snapshot_id BIGINT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/CLOSED/VOID',
  precision_status VARCHAR(20) NOT NULL DEFAULT 'EXACT' COMMENT 'EXACT/APPROXIMATE/UNSPLIT',
  version INT NOT NULL DEFAULT 0,
  source VARCHAR(20) NOT NULL DEFAULT 'PLUGIN',
  remark VARCHAR(500) NULL,
  void_reason VARCHAR(500) NULL COMMENT '作废原因',
  void_by BIGINT NULL COMMENT '作废操作人',
  void_name VARCHAR(100) NULL COMMENT '作废操作人姓名',
  void_at DATETIME NULL COMMENT '作废时间',
  active_session_id BIGINT GENERATED ALWAYS AS (CASE WHEN status='ACTIVE' THEN session_id ELSE NULL END) STORED,
  watch_delta BIGINT NULL,
  interaction_delta BIGINT NULL,
  new_fan_delta BIGINT NULL,
  deal_delta BIGINT NULL,
  gmv_delta DECIMAL(18,4) NULL,
  ad_cost_delta DECIMAL(18,4) NULL,
  presale_order_delta BIGINT NULL,
  exposure_watch_rate DECIMAL(10,4) NULL,
  watch_follow_rate DECIMAL(10,4) NULL,
  watch_interaction_rate DECIMAL(10,4) NULL,
  watch_deal_rate DECIMAL(10,4) NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_by VARCHAR(64) NULL,
  update_time DATETIME NULL,
  PRIMARY KEY (segment_id),
  UNIQUE KEY uk_metric_segment_active (active_session_id),
  KEY idx_metric_segment_session_time (session_id,start_time,segment_id),
  KEY idx_metric_segment_room_time (room_id,start_time),
  KEY idx_metric_segment_anchor_time (anchor_user_id,start_time),
  -- session_id 通过 start/end snapshot 间接受约束。不要再添加到 session 的级联外键，
  -- 否则 MySQL 会形成 session -> snapshot -> segment 与 session -> segment 两条删除路径。
  CONSTRAINT fk_metric_segment_start_snapshot FOREIGN KEY (start_snapshot_id) REFERENCES live_metric_snapshot(snapshot_id),
  CONSTRAINT fk_metric_segment_end_snapshot FOREIGN KEY (end_snapshot_id) REFERENCES live_metric_snapshot(snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主播实际直播片段';

CREATE TABLE live_metric_segment_correction (
  correction_id BIGINT NOT NULL AUTO_INCREMENT,
  session_id BIGINT NOT NULL,
  segment_id BIGINT NULL,
  action_type VARCHAR(30) NOT NULL,
  old_value TEXT NULL,
  new_value TEXT NULL,
  reason VARCHAR(500) NOT NULL,
  corrected_by BIGINT NULL,
  corrected_name VARCHAR(100) NOT NULL,
  corrected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (correction_id),
  KEY idx_metric_correction_session_time (session_id,corrected_at),
  KEY idx_metric_correction_segment_time (segment_id,corrected_at),
  CONSTRAINT fk_metric_correction_session FOREIGN KEY (session_id) REFERENCES live_metric_session(session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='主播片段调整日志';

CREATE TABLE live_metric_room_alias (
  alias_id BIGINT NOT NULL AUTO_INCREMENT,
  room_id BIGINT NOT NULL,
  alias_name VARCHAR(160) NOT NULL,
  alias_key VARCHAR(160) NOT NULL,
  create_by VARCHAR(64) NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (alias_id),
  UNIQUE KEY uk_metric_room_alias (alias_key),
  KEY idx_metric_room_alias_room (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播间名称别名';
