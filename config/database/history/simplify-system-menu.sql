-- 将系统监控和系统工具归入系统管理，并删除已停用的通用 Quartz 菜单和示例数据。
-- 直播业务定时任务使用 Spring Scheduling，不依赖 sys_job 或 Quartz 表。

START TRANSACTION;

-- 原来能看到系统监控或系统工具的角色，需要同时拥有新的父目录“系统管理”。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_id, 1
FROM sys_role_menu
WHERE menu_id IN (2, 3);

UPDATE sys_menu
SET parent_id = 1,
    order_num = CASE menu_id WHEN 2 THEN 10 WHEN 3 THEN 11 ELSE order_num END
WHERE menu_id IN (2, 3);

CREATE TEMPORARY TABLE tmp_quartz_menu_ids (
  menu_id BIGINT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_quartz_menu_ids (menu_id)
WITH RECURSIVE quartz_menus AS (
  SELECT menu_id FROM sys_menu WHERE menu_id = 110
  UNION DISTINCT
  SELECT child.menu_id
  FROM sys_menu child
  JOIN quartz_menus parent ON child.parent_id = parent.menu_id
)
SELECT menu_id FROM quartz_menus;

DELETE role_menu
FROM sys_role_menu role_menu
JOIN tmp_quartz_menu_ids removed ON removed.menu_id = role_menu.menu_id;

DELETE menu
FROM sys_menu menu
JOIN tmp_quartz_menu_ids removed ON removed.menu_id = menu.menu_id;

DROP TEMPORARY TABLE tmp_quartz_menu_ids;

DELETE FROM sys_job_log;
DELETE FROM sys_job;

COMMIT;

SELECT menu_id, parent_id, menu_name, path, component
FROM sys_menu
WHERE menu_id IN (1, 2, 3, 110) OR parent_id IN (1, 2, 3, 110)
ORDER BY parent_id, order_num, menu_id;
