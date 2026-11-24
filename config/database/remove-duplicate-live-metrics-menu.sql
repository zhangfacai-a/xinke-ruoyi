-- 主播交接已由插件单次上报和网站补录统一完成，删除重复的“主播排班”入口。
-- 保留“主播数据上报”入口及 live:metrics:manage 管理权限，不删除任何业务数据。

START TRANSACTION;

SET @keep_menu_id := (
  SELECT menu_id
  FROM sys_menu
  WHERE path = 'liveMetricsReport'
     OR component = 'live/metrics/report'
  ORDER BY menu_id
  LIMIT 1
);

SET @legacy_menu_id := (
  SELECT menu_id
  FROM sys_menu
  WHERE path = 'liveMetrics'
    AND component = 'live/metrics/index'
    AND menu_id <> COALESCE(@keep_menu_id, -1)
  ORDER BY menu_id
  LIMIT 1
);

-- 兼容只存在旧入口的环境：直接复用旧菜单，不新造菜单 ID。
SET @keep_menu_id := COALESCE(@keep_menu_id, @legacy_menu_id);

-- 合并两个入口已有的角色授权，避免清理后有人失去访问权限。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, @keep_menu_id
FROM sys_role_menu
WHERE menu_id = @legacy_menu_id
  AND @keep_menu_id IS NOT NULL
  AND @legacy_menu_id IS NOT NULL
  AND @keep_menu_id <> @legacy_menu_id;

UPDATE sys_menu
SET menu_name = '主播数据上报',
    path = 'liveMetrics',
    component = 'live/metrics/index',
    perms = 'live:metrics:list',
    update_time = NOW()
WHERE menu_id = @keep_menu_id;

DELETE FROM sys_role_menu
WHERE menu_id = @legacy_menu_id
  AND @legacy_menu_id IS NOT NULL
  AND @legacy_menu_id <> @keep_menu_id;

DELETE FROM sys_menu
WHERE menu_id = @legacy_menu_id
  AND @legacy_menu_id IS NOT NULL
  AND @legacy_menu_id <> @keep_menu_id;

COMMIT;

