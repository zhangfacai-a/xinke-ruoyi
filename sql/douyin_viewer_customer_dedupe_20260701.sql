-- Customer-level dedupe migration for Douyin live viewer leads.
-- Run once on existing databases.
-- Goal:
--   dy_viewer = one customer profile per sec_uid
--   dy_viewer_daily_lead = one visit row per day + room + sec_uid
--   dy_viewer_comment = comment detail rows

ALTER TABLE dy_viewer
  ADD COLUMN has_comment TINYINT DEFAULT 0 COMMENT 'Has comments' AFTER last_seen_date,
  ADD COLUMN comment_count INT DEFAULT 0 COMMENT 'Comment count' AFTER has_comment,
  ADD COLUMN first_comment_time DATETIME DEFAULT NULL COMMENT 'First comment time' AFTER comment_count,
  ADD COLUMN last_comment_time DATETIME DEFAULT NULL COMMENT 'Last comment time' AFTER first_comment_time,
  ADD COLUMN last_comment_content VARCHAR(1000) DEFAULT NULL COMMENT 'Last comment content' AFTER last_comment_time,
  ADD COLUMN status VARCHAR(32) DEFAULT 'new' COMMENT 'new/following/ordered/invalid' AFTER last_comment_content,
  ADD COLUMN intent VARCHAR(32) DEFAULT 'unknown' COMMENT 'unknown/low/medium/high' AFTER status,
  ADD COLUMN owner_id BIGINT DEFAULT NULL COMMENT 'Owner user ID' AFTER intent,
  ADD COLUMN owner_name VARCHAR(50) DEFAULT NULL COMMENT 'Owner name' AFTER owner_id,
  ADD COLUMN owner_assigned_time DATETIME DEFAULT NULL COMMENT 'Owner assigned time' AFTER owner_name,
  ADD COLUMN order_no VARCHAR(64) DEFAULT NULL COMMENT 'Matched order number' AFTER owner_assigned_time,
  ADD COLUMN remark VARCHAR(500) DEFAULT NULL COMMENT 'Remark' AFTER order_no,
  ADD COLUMN last_follow_time DATETIME DEFAULT NULL COMMENT 'Last follow time' AFTER remark,
  ADD COLUMN manual_update_time DATETIME DEFAULT NULL COMMENT 'Manual update time' AFTER last_follow_time;

CREATE INDEX idx_dy_viewer_owner_status ON dy_viewer(owner_id, status);
CREATE INDEX idx_dy_viewer_status_seen ON dy_viewer(status, last_seen_date);
CREATE INDEX idx_dy_viewer_has_comment ON dy_viewer(has_comment);
CREATE INDEX idx_dy_viewer_order_no ON dy_viewer(order_no);

UPDATE dy_viewer v
LEFT JOIN (
    SELECT viewer_id,
           COUNT(1) AS comment_count,
           MIN(captured_at) AS first_comment_time,
           MAX(captured_at) AS last_comment_time
    FROM dy_viewer_comment
    WHERE viewer_id IS NOT NULL
    GROUP BY viewer_id
) cs ON cs.viewer_id = v.viewer_id
LEFT JOIN (
    SELECT c.viewer_id, c.content
    FROM dy_viewer_comment c
    INNER JOIN (
        SELECT viewer_id, MAX(comment_id) AS comment_id
        FROM dy_viewer_comment
        WHERE viewer_id IS NOT NULL
        GROUP BY viewer_id
    ) picked ON picked.comment_id = c.comment_id
) lc ON lc.viewer_id = v.viewer_id
LEFT JOIN (
    SELECT l.*
    FROM dy_viewer_daily_lead l
    INNER JOIN (
        SELECT viewer_id, MAX(lead_id) AS lead_id
        FROM dy_viewer_daily_lead
        GROUP BY viewer_id
    ) picked ON picked.lead_id = l.lead_id
) latest ON latest.viewer_id = v.viewer_id
SET v.nickname = COALESCE(NULLIF(v.nickname, ''), latest.nickname),
    v.has_comment = IF(IFNULL(cs.comment_count, 0) > 0, 1, 0),
    v.comment_count = IFNULL(cs.comment_count, 0),
    v.first_comment_time = cs.first_comment_time,
    v.last_comment_time = cs.last_comment_time,
    v.last_comment_content = lc.content,
    v.owner_id = latest.owner_id,
    v.owner_name = latest.owner_name,
    v.owner_assigned_time = latest.owner_assigned_time,
    v.order_no = latest.order_no,
    v.remark = latest.remark,
    v.last_follow_time = latest.last_follow_time,
    v.manual_update_time = latest.manual_update_time,
    v.intent = CASE WHEN IFNULL(cs.comment_count, 0) > 0 THEN 'high' ELSE IFNULL(latest.intent, 'unknown') END,
    v.status = CASE
        WHEN latest.order_no IS NOT NULL AND TRIM(latest.order_no) != '' THEN 'ordered'
        WHEN v.last_seen_date < DATE_SUB(CURDATE(), INTERVAL 1 MONTH) THEN 'invalid'
        WHEN latest.owner_name IS NOT NULL AND TRIM(latest.owner_name) != '' THEN 'following'
        ELSE 'new'
    END,
    v.update_time = SYSDATE();

