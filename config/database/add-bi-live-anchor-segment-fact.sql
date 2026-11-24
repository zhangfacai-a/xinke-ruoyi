-- 主播区间 BI 事实宽表。
-- 可重复执行：建表后按源 segment_id 幂等回填，不修改主播业务源表。

CREATE TABLE IF NOT EXISTS bi_live_anchor_segment_fact (
  segment_id BIGINT NOT NULL COMMENT '主播区间记录ID，源 live_metric_anchor_segment.segment_id',
  session_id BIGINT NOT NULL COMMENT '直播场次内部ID，关联 live_metric_session.session_id',
  platform_session_id VARCHAR(64) NOT NULL COMMENT '平台直播场次ID，抖音 live_room_id',
  business_date DATE NOT NULL COMMENT '业务归属日期，取本场直播开播日期',
  room_id BIGINT NOT NULL COMMENT '直播间内部ID，关联 live_room.room_id',
  room_name VARCHAR(160) NOT NULL COMMENT '直播间名称快照，保留同步时名称',
  anchor_user_id BIGINT NOT NULL COMMENT '主播用户ID，关联 sys_user.user_id',
  anchor_name VARCHAR(100) NOT NULL COMMENT '主播名称快照，保留同步时名称',
  session_live_start_at DATETIME NOT NULL COMMENT '本场直播开播时间',
  segment_start_at DATETIME NOT NULL COMMENT '主播负责区间开始时间',
  segment_end_at DATETIME NOT NULL COMMENT '主播负责区间结束时间',
  duration_seconds INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '主播负责区间时长，单位秒',
  watch_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增观看人数，由累计快照差值计算',
  interaction_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增互动人数，由累计快照差值计算',
  new_fan_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增粉丝数，由累计快照差值计算',
  deal_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增成交人数，由累计快照差值计算',
  gmv_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '区间成交金额，单位元',
  ad_cost_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '区间千川消耗金额，单位元',
  presale_order_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增预售订单数',
  exposure_count_est DECIMAL(20,4) NULL COMMENT '估算曝光次数，由观看人数/曝光观看率反推；NULL表示无法估算',
  exposure_watch_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间曝光-观看率(%)；聚合=SUM(watch_count)/SUM(exposure_count_est)*100',
  watch_follow_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间观看-关注率(%)；聚合=SUM(new_fan_count)/SUM(watch_count)*100',
  watch_interaction_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间观看-互动率(%)；聚合=SUM(interaction_count)/SUM(watch_count)*100',
  watch_deal_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间观看-成交率(%)；聚合=SUM(deal_count)/SUM(watch_count)*100',
  source_type VARCHAR(20) NOT NULL COMMENT '数据来源类型，取源记录 source，如插件上报或手工补录',
  precision_status VARCHAR(20) NOT NULL COMMENT '区间精度状态，如 EXACT 表示边界精确',
  quality_status VARCHAR(16) NOT NULL COMMENT '数据质量等级：VALID有效、WARNING需关注、INVALID不可用',
  quality_flags VARCHAR(500) NULL COMMENT '数据质量标记，多项以英文逗号分隔；NULL表示无异常',
  source_version INT NOT NULL DEFAULT 0 COMMENT '源记录版本号，用于识别增量变更',
  source_created_at DATETIME NOT NULL COMMENT '源记录创建时间',
  source_updated_at DATETIME NOT NULL COMMENT '源记录最后更新时间',
  synced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本事实记录最近同步时间',
  is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记：0有效，1源记录已删除、作废或未完成',
  row_hash CHAR(64) NOT NULL COMMENT '参与同步字段的SHA-256哈希，用于数据一致性核验',
  PRIMARY KEY (segment_id),
  KEY idx_bi_anchor_date_room_user (business_date,room_id,anchor_user_id,is_deleted),
  KEY idx_bi_anchor_source_update (source_updated_at,segment_id),
  KEY idx_bi_anchor_quality_date (quality_status,business_date),
  KEY idx_bi_anchor_session (session_id,segment_start_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='BI主播直播区间事实宽表';

-- 已存在的表也同步更新字段注释；仅修改元数据定义，不改业务数据。
ALTER TABLE bi_live_anchor_segment_fact
  MODIFY COLUMN segment_id BIGINT NOT NULL COMMENT '主播区间记录ID，源 live_metric_anchor_segment.segment_id',
  MODIFY COLUMN session_id BIGINT NOT NULL COMMENT '直播场次内部ID，关联 live_metric_session.session_id',
  MODIFY COLUMN platform_session_id VARCHAR(64) NOT NULL COMMENT '平台直播场次ID，抖音 live_room_id',
  MODIFY COLUMN business_date DATE NOT NULL COMMENT '业务归属日期，取本场直播开播日期',
  MODIFY COLUMN room_id BIGINT NOT NULL COMMENT '直播间内部ID，关联 live_room.room_id',
  MODIFY COLUMN room_name VARCHAR(160) NOT NULL COMMENT '直播间名称快照，保留同步时名称',
  MODIFY COLUMN anchor_user_id BIGINT NOT NULL COMMENT '主播用户ID，关联 sys_user.user_id',
  MODIFY COLUMN anchor_name VARCHAR(100) NOT NULL COMMENT '主播名称快照，保留同步时名称',
  MODIFY COLUMN session_live_start_at DATETIME NOT NULL COMMENT '本场直播开播时间',
  MODIFY COLUMN segment_start_at DATETIME NOT NULL COMMENT '主播负责区间开始时间',
  MODIFY COLUMN segment_end_at DATETIME NOT NULL COMMENT '主播负责区间结束时间',
  MODIFY COLUMN duration_seconds INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '主播负责区间时长，单位秒',
  MODIFY COLUMN watch_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增观看人数，由累计快照差值计算',
  MODIFY COLUMN interaction_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增互动人数，由累计快照差值计算',
  MODIFY COLUMN new_fan_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增粉丝数，由累计快照差值计算',
  MODIFY COLUMN deal_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增成交人数，由累计快照差值计算',
  MODIFY COLUMN gmv_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '区间成交金额，单位元',
  MODIFY COLUMN ad_cost_amount DECIMAL(18,4) NOT NULL DEFAULT 0 COMMENT '区间千川消耗金额，单位元',
  MODIFY COLUMN presale_order_count BIGINT NOT NULL DEFAULT 0 COMMENT '区间新增预售订单数',
  MODIFY COLUMN exposure_count_est DECIMAL(20,4) NULL COMMENT '估算曝光次数，由观看人数/曝光观看率反推；NULL表示无法估算',
  MODIFY COLUMN exposure_watch_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间曝光-观看率(%)；聚合=SUM(watch_count)/SUM(exposure_count_est)*100',
  MODIFY COLUMN watch_follow_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间观看-关注率(%)；聚合=SUM(new_fan_count)/SUM(watch_count)*100',
  MODIFY COLUMN watch_interaction_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间观看-互动率(%)；聚合=SUM(interaction_count)/SUM(watch_count)*100',
  MODIFY COLUMN watch_deal_rate DECIMAL(10,4) NOT NULL DEFAULT 0 COMMENT '区间观看-成交率(%)；聚合=SUM(deal_count)/SUM(watch_count)*100',
  MODIFY COLUMN source_type VARCHAR(20) NOT NULL COMMENT '数据来源类型，取源记录 source，如插件上报或手工补录',
  MODIFY COLUMN precision_status VARCHAR(20) NOT NULL COMMENT '区间精度状态，如 EXACT 表示边界精确',
  MODIFY COLUMN quality_status VARCHAR(16) NOT NULL COMMENT '数据质量等级：VALID有效、WARNING需关注、INVALID不可用',
  MODIFY COLUMN quality_flags VARCHAR(500) NULL COMMENT '数据质量标记，多项以英文逗号分隔；NULL表示无异常',
  MODIFY COLUMN source_version INT NOT NULL DEFAULT 0 COMMENT '源记录版本号，用于识别增量变更',
  MODIFY COLUMN source_created_at DATETIME NOT NULL COMMENT '源记录创建时间',
  MODIFY COLUMN source_updated_at DATETIME NOT NULL COMMENT '源记录最后更新时间',
  MODIFY COLUMN synced_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '本事实记录最近同步时间',
  MODIFY COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删除标记：0有效，1源记录已删除、作废或未完成',
  MODIFY COLUMN row_hash CHAR(64) NOT NULL COMMENT '参与同步字段的SHA-256哈希，用于数据一致性核验',
  COMMENT='BI主播直播区间事实宽表：每行代表一名主播在一场直播中的一个负责区间';

INSERT INTO bi_live_anchor_segment_fact (
  segment_id,session_id,platform_session_id,business_date,room_id,room_name,
  anchor_user_id,anchor_name,session_live_start_at,segment_start_at,segment_end_at,duration_seconds,
  watch_count,interaction_count,new_fan_count,deal_count,gmv_amount,ad_cost_amount,presale_order_count,
  exposure_count_est,exposure_watch_rate,watch_follow_rate,watch_interaction_rate,watch_deal_rate,
  source_type,precision_status,quality_status,quality_flags,source_version,
  source_created_at,source_updated_at,synced_at,is_deleted,row_hash
)
SELECT
  g.segment_id,g.session_id,s.platform_session_id,DATE(s.live_start_time),g.room_id,
  COALESCE(r.room_name,s.reported_room_name),g.anchor_user_id,COALESCE(u.nick_name,CONCAT('用户#',g.anchor_user_id)),
  s.live_start_time,g.start_time,g.end_time,
  GREATEST(TIMESTAMPDIFF(SECOND,g.start_time,g.end_time),0),
  COALESCE(g.watch_delta,0),COALESCE(g.interaction_delta,0),COALESCE(g.new_fan_delta,0),
  COALESCE(g.deal_delta,0),COALESCE(g.gmv_delta,0),COALESCE(g.ad_cost_delta,0),COALESCE(g.presale_order_delta,0),
  CASE WHEN COALESCE(g.watch_delta,0)=0 THEN 0
       WHEN COALESCE(g.exposure_watch_rate,0)>0 THEN ROUND(g.watch_delta*100/g.exposure_watch_rate,4)
       ELSE NULL END,
  COALESCE(g.exposure_watch_rate,0),COALESCE(g.watch_follow_rate,0),
  COALESCE(g.watch_interaction_rate,0),COALESCE(g.watch_deal_rate,0),g.source,g.precision_status,
  CASE
    WHEN g.end_time IS NULL OR g.end_time<=g.start_time OR g.room_id IS NULL OR g.anchor_user_id IS NULL
      OR COALESCE(g.watch_delta,0)<0 OR COALESCE(g.interaction_delta,0)<0 OR COALESCE(g.new_fan_delta,0)<0
      OR COALESCE(g.deal_delta,0)<0 OR COALESCE(g.gmv_delta,0)<0 OR COALESCE(g.ad_cost_delta,0)<0
      OR COALESCE(g.presale_order_delta,0)<0 THEN 'INVALID'
    WHEN TIMESTAMPDIFF(HOUR,g.start_time,g.end_time)>12 OR g.precision_status<>'EXACT'
      OR g.source='MANUAL' OR (COALESCE(g.watch_delta,0)>0 AND COALESCE(g.exposure_watch_rate,0)=0)
      OR (s.status='LIVE' AND s.live_start_time<NOW()-INTERVAL 36 HOUR) THEN 'WARNING'
    ELSE 'VALID' END,
  NULLIF(CONCAT_WS(',',
    IF(g.end_time IS NULL OR g.end_time<=g.start_time,'INVALID_TIME',NULL),
    IF(g.room_id IS NULL,'MISSING_ROOM',NULL),IF(g.anchor_user_id IS NULL,'MISSING_ANCHOR',NULL),
    IF(COALESCE(g.watch_delta,0)<0 OR COALESCE(g.interaction_delta,0)<0 OR COALESCE(g.new_fan_delta,0)<0
      OR COALESCE(g.deal_delta,0)<0 OR COALESCE(g.gmv_delta,0)<0 OR COALESCE(g.ad_cost_delta,0)<0
      OR COALESCE(g.presale_order_delta,0)<0,'NEGATIVE_METRIC',NULL),
    IF(TIMESTAMPDIFF(HOUR,g.start_time,g.end_time)>12,'LONGER_THAN_12H',NULL),
    IF(g.precision_status<>'EXACT','NON_EXACT',NULL),IF(g.source='MANUAL','MANUAL_SOURCE',NULL),
    IF(COALESCE(g.watch_delta,0)>0 AND COALESCE(g.exposure_watch_rate,0)=0,'MISSING_EXPOSURE_RATE',NULL),
    IF(s.status='LIVE' AND s.live_start_time<NOW()-INTERVAL 36 HOUR,'SESSION_STALE',NULL)
  ),''),
  g.version,g.create_time,COALESCE(g.update_time,g.create_time),NOW(),0,
  SHA2(CONCAT_WS('|',g.segment_id,g.session_id,s.platform_session_id,g.room_id,g.anchor_user_id,
    DATE_FORMAT(g.start_time,'%Y-%m-%d %H:%i:%s'),DATE_FORMAT(g.end_time,'%Y-%m-%d %H:%i:%s'),
    COALESCE(g.watch_delta,0),COALESCE(g.interaction_delta,0),COALESCE(g.new_fan_delta,0),
    COALESCE(g.deal_delta,0),COALESCE(g.gmv_delta,0),COALESCE(g.ad_cost_delta,0),
    COALESCE(g.presale_order_delta,0),COALESCE(g.exposure_watch_rate,0),COALESCE(g.watch_follow_rate,0),
    COALESCE(g.watch_interaction_rate,0),COALESCE(g.watch_deal_rate,0),g.source,g.precision_status,g.version),256)
FROM live_metric_anchor_segment g
JOIN live_metric_session s ON s.session_id=g.session_id
LEFT JOIN live_room r ON r.room_id=g.room_id
LEFT JOIN sys_user u ON u.user_id=g.anchor_user_id
WHERE g.status='CLOSED' AND g.end_time IS NOT NULL
ON DUPLICATE KEY UPDATE
  session_id=VALUES(session_id),platform_session_id=VALUES(platform_session_id),business_date=VALUES(business_date),
  room_id=VALUES(room_id),room_name=VALUES(room_name),anchor_user_id=VALUES(anchor_user_id),anchor_name=VALUES(anchor_name),
  session_live_start_at=VALUES(session_live_start_at),segment_start_at=VALUES(segment_start_at),segment_end_at=VALUES(segment_end_at),
  duration_seconds=VALUES(duration_seconds),watch_count=VALUES(watch_count),interaction_count=VALUES(interaction_count),
  new_fan_count=VALUES(new_fan_count),deal_count=VALUES(deal_count),gmv_amount=VALUES(gmv_amount),
  ad_cost_amount=VALUES(ad_cost_amount),presale_order_count=VALUES(presale_order_count),
  exposure_count_est=VALUES(exposure_count_est),exposure_watch_rate=VALUES(exposure_watch_rate),
  watch_follow_rate=VALUES(watch_follow_rate),watch_interaction_rate=VALUES(watch_interaction_rate),
  watch_deal_rate=VALUES(watch_deal_rate),source_type=VALUES(source_type),precision_status=VALUES(precision_status),
  quality_status=VALUES(quality_status),quality_flags=VALUES(quality_flags),source_version=VALUES(source_version),
  source_created_at=VALUES(source_created_at),source_updated_at=VALUES(source_updated_at),synced_at=NOW(),
  is_deleted=0,row_hash=VALUES(row_hash);

-- 源表已不存在或已作废/未完成的记录在 BI 表中软删除，保留审计线索。
UPDATE bi_live_anchor_segment_fact b
LEFT JOIN live_metric_anchor_segment g ON g.segment_id=b.segment_id
SET b.is_deleted=1,b.synced_at=NOW()
WHERE (g.segment_id IS NULL OR g.status<>'CLOSED' OR g.end_time IS NULL) AND b.is_deleted=0;
