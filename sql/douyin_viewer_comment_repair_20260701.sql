-- Repair Douyin live viewer comments after room_key drift.
-- Cause:
--   Some audience visits were saved with Douyin response room_id,
--   while comments were saved with plugin report room_key.
--   Matching by lead_date + room_key + nickname failed.
-- Fix:
--   1. Bind unmatched comments by same-day nickname.
--   2. Recalculate daily lead comment stats.
--   3. Recalculate customer profile comment stats.

UPDATE dy_viewer_comment c
INNER JOIN (
    SELECT c.comment_id, l.lead_id, l.viewer_id
    FROM dy_viewer_comment c
    INNER JOIN (
        SELECT c.comment_id, MAX(l.lead_id) AS lead_id
        FROM dy_viewer_comment c
        INNER JOIN dy_viewer_daily_lead l
            ON l.lead_date = c.lead_date
           AND l.nickname = c.nickname
        WHERE c.lead_id IS NULL
          AND c.viewer_id IS NULL
          AND c.nickname IS NOT NULL
          AND TRIM(c.nickname) != ''
        GROUP BY c.comment_id
    ) picked ON picked.comment_id = c.comment_id
    INNER JOIN dy_viewer_daily_lead l ON l.lead_id = picked.lead_id
) m ON m.comment_id = c.comment_id
SET c.lead_id = m.lead_id,
    c.viewer_id = m.viewer_id,
    c.match_type = 'nickname_any_room';

UPDATE dy_viewer_daily_lead l
LEFT JOIN (
    SELECT lead_id,
           COUNT(1) AS comment_count,
           MIN(captured_at) AS first_comment_time,
           MAX(captured_at) AS last_comment_time
    FROM dy_viewer_comment
    WHERE lead_id IS NOT NULL
    GROUP BY lead_id
) s ON s.lead_id = l.lead_id
LEFT JOIN (
    SELECT c.lead_id, c.content
    FROM dy_viewer_comment c
    INNER JOIN (
        SELECT lead_id, MAX(comment_id) AS comment_id
        FROM dy_viewer_comment
        WHERE lead_id IS NOT NULL
        GROUP BY lead_id
    ) picked ON picked.comment_id = c.comment_id
) last_comment ON last_comment.lead_id = l.lead_id
SET l.has_comment = IF(IFNULL(s.comment_count, 0) > 0, 1, 0),
    l.comment_count = IFNULL(s.comment_count, 0),
    l.intent = IF(IFNULL(s.comment_count, 0) > 0, 'high', l.intent),
    l.first_comment_time = s.first_comment_time,
    l.last_comment_time = s.last_comment_time,
    l.last_comment_content = last_comment.content,
    l.update_time = SYSDATE();

UPDATE dy_viewer v
LEFT JOIN (
    SELECT viewer_id,
           COUNT(1) AS comment_count,
           MIN(captured_at) AS first_comment_time,
           MAX(captured_at) AS last_comment_time
    FROM dy_viewer_comment
    WHERE viewer_id IS NOT NULL
    GROUP BY viewer_id
) s ON s.viewer_id = v.viewer_id
LEFT JOIN (
    SELECT c.viewer_id, c.content
    FROM dy_viewer_comment c
    INNER JOIN (
        SELECT viewer_id, MAX(comment_id) AS comment_id
        FROM dy_viewer_comment
        WHERE viewer_id IS NOT NULL
        GROUP BY viewer_id
    ) picked ON picked.comment_id = c.comment_id
) last_comment ON last_comment.viewer_id = v.viewer_id
SET v.has_comment = IF(IFNULL(s.comment_count, 0) > 0, 1, 0),
    v.comment_count = IFNULL(s.comment_count, 0),
    v.intent = IF(IFNULL(s.comment_count, 0) > 0, 'high', v.intent),
    v.first_comment_time = s.first_comment_time,
    v.last_comment_time = s.last_comment_time,
    v.last_comment_content = last_comment.content,
    v.update_time = SYSDATE();

