CREATE TABLE IF NOT EXISTS live_metric_shift_summary (
  shift_id BIGINT NOT NULL COMMENT '班次ID',
  session_count INT NOT NULL DEFAULT 0 COMMENT '计入业绩的直播场次数',
  submission_count INT NOT NULL DEFAULT 0 COMMENT '原始上报记录数',
  latest_submission_id BIGINT DEFAULT NULL COMMENT '最新上报记录ID',
  last_submitted_at DATETIME DEFAULT NULL COMMENT '最后上报时间',
  exposure_delta BIGINT DEFAULT NULL COMMENT '班次曝光人数',
  watch_delta BIGINT DEFAULT NULL COMMENT '班次观看人数',
  interaction_delta BIGINT DEFAULT NULL COMMENT '班次互动人数',
  new_fan_delta BIGINT DEFAULT NULL COMMENT '班次新增粉丝',
  deal_delta BIGINT DEFAULT NULL COMMENT '班次成交人数',
  anomaly_count INT NOT NULL DEFAULT 0 COMMENT '异常场次数',
  suspect_count INT NOT NULL DEFAULT 0 COMMENT '疑似漏报场次数',
  update_time DATETIME NOT NULL COMMENT '汇总更新时间',
  PRIMARY KEY (shift_id),
  KEY idx_live_metric_shift_summary_last_submitted (last_submitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直播班次经营数据汇总';

INSERT INTO live_metric_shift_summary (
  shift_id, session_count, submission_count, latest_submission_id, last_submitted_at,
  exposure_delta, watch_delta, interaction_delta, new_fan_delta, deal_delta,
  anomaly_count, suspect_count, update_time
)
SELECT
  x.shift_id,
  COUNT(DISTINCT CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.exposure_delta IS NOT NULL THEN x.session_id END),
  COUNT(*), MAX(x.submission_id), MAX(x.submitted_at),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.exposure_delta IS NOT NULL THEN x.exposure_delta END),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.watch_delta IS NOT NULL THEN x.watch_delta END),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.interaction_delta IS NOT NULL THEN x.interaction_delta END),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.new_fan_delta IS NOT NULL THEN x.new_fan_delta END),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.deal_delta IS NOT NULL THEN x.deal_delta END),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.match_status='ANOMALY' THEN 1 ELSE 0 END),
  SUM(CASE WHEN x.status='CONFIRMED' AND x.is_effective=1 AND x.match_status='SUSPECT_MISSING' THEN 1 ELSE 0 END),
  NOW()
FROM live_metric_submission x
WHERE x.shift_id IS NOT NULL
GROUP BY x.shift_id
ON DUPLICATE KEY UPDATE
  session_count=VALUES(session_count), submission_count=VALUES(submission_count),
  latest_submission_id=VALUES(latest_submission_id), last_submitted_at=VALUES(last_submitted_at),
  exposure_delta=VALUES(exposure_delta), watch_delta=VALUES(watch_delta),
  interaction_delta=VALUES(interaction_delta), new_fan_delta=VALUES(new_fan_delta),
  deal_delta=VALUES(deal_delta), anomaly_count=VALUES(anomaly_count),
  suspect_count=VALUES(suspect_count), update_time=VALUES(update_time);
