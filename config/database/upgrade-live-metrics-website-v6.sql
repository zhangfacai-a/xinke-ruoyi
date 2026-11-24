-- 主播数据网站 V6：补齐区间进房率。
-- 可重复执行，不清空现有数据。

SET @column_exists=(
  SELECT COUNT(*) FROM information_schema.columns
  WHERE table_schema=DATABASE()
    AND table_name='live_metric_anchor_segment'
    AND column_name='exposure_watch_rate'
);
SET @ddl=IF(
  @column_exists=0,
  'ALTER TABLE live_metric_anchor_segment ADD COLUMN exposure_watch_rate DECIMAL(10,4) NULL AFTER presale_order_delta',
  'SELECT 1'
);
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 使用片段首尾累计快照回填历史区间进房率。
UPDATE live_metric_anchor_segment segment
JOIN live_metric_snapshot start_snapshot ON start_snapshot.snapshot_id=segment.start_snapshot_id
JOIN live_metric_snapshot end_snapshot ON end_snapshot.snapshot_id=segment.end_snapshot_id
SET segment.exposure_watch_rate=CASE
  WHEN segment.watch_delta<=0 THEN 0
  WHEN end_snapshot.exposure_watch_rate<=0 THEN 0
  WHEN (
    end_snapshot.watch_total*100/end_snapshot.exposure_watch_rate
    - CASE WHEN start_snapshot.watch_total=0 THEN 0
      WHEN start_snapshot.exposure_watch_rate>0 THEN start_snapshot.watch_total*100/start_snapshot.exposure_watch_rate
      ELSE end_snapshot.watch_total*100/end_snapshot.exposure_watch_rate END
  )<=0 THEN 0
  ELSE ROUND(segment.watch_delta*100/(
    end_snapshot.watch_total*100/end_snapshot.exposure_watch_rate
    - CASE WHEN start_snapshot.watch_total=0 THEN 0 ELSE start_snapshot.watch_total*100/start_snapshot.exposure_watch_rate END
  ),4)
END
WHERE segment.exposure_watch_rate IS NULL;

-- 将管理操作授权给已经拥有“主播数据上报”页面的角色。
SET @metrics_menu_id=(
  SELECT menu_id FROM sys_menu
  WHERE component IN ('live/metrics/index','live/metrics/report')
  ORDER BY menu_id DESC LIMIT 1
);
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,component,is_frame,is_cache,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '主播数据管理',@metrics_menu_id,1,'#',NULL,1,0,'F','0','0','live:metrics:manage','#','admin',NOW()
WHERE @metrics_menu_id IS NOT NULL
  AND NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='live:metrics:manage');
SET @manage_menu_id=(SELECT menu_id FROM sys_menu WHERE perms='live:metrics:manage' LIMIT 1);
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT page_role.role_id,@manage_menu_id
FROM sys_role_menu page_role
WHERE page_role.menu_id=@metrics_menu_id AND @manage_menu_id IS NOT NULL;
