-- 删除已下线的传统 ERP、财务、旧 BI 和数据中心菜单、角色授权及 ERP ETL 定时任务。
-- 不删除任何业务表或业务数据。部署新版前端和后端后，在业务数据库执行一次。

START TRANSACTION;

CREATE TEMPORARY TABLE tmp_removed_menu_ids (
  menu_id BIGINT NOT NULL PRIMARY KEY
) ENGINE=MEMORY;

INSERT INTO tmp_removed_menu_ids(menu_id)
WITH RECURSIVE removed_menus AS (
  SELECT menu_id
  FROM sys_menu
  WHERE (parent_id = 0 AND path IN ('erp', 'finance', 'bi', 'datahub'))
     OR (parent_id = 0 AND path = 'purchase' AND menu_name = '采购匹配')
     OR perms REGEXP '^(erp|finance|bi|datahub|purchase):'
     OR component REGEXP '^(erp|finance|bi|datahub)/'
  UNION DISTINCT
  SELECT child.menu_id
  FROM sys_menu child
  JOIN removed_menus parent ON child.parent_id = parent.menu_id
)
SELECT menu_id FROM removed_menus;

DELETE role_menu
FROM sys_role_menu role_menu
JOIN tmp_removed_menu_ids removed ON removed.menu_id = role_menu.menu_id;

DELETE menu
FROM sys_menu menu
JOIN tmp_removed_menu_ids removed ON removed.menu_id = menu.menu_id;

DROP TEMPORARY TABLE tmp_removed_menu_ids;

-- 仅匹配已经删除的 ERP ETL 任务，不影响直播提醒和钉钉同步任务。
DELETE FROM sys_job
WHERE invoke_target REGEXP '^erpEtlTask[.]'
   OR invoke_target REGEXP '^com[.]xinke[.]erp[.]task[.]ErpEtlTask[.]';

COMMIT;

SELECT menu_id, menu_name, path, component, perms
FROM sys_menu
WHERE (parent_id = 0 AND path IN ('erp', 'finance', 'bi', 'datahub'))
   OR (parent_id = 0 AND path = 'purchase' AND menu_name = '采购匹配')
   OR perms REGEXP '^(erp|finance|bi|datahub|purchase):'
   OR component REGEXP '^(erp|finance|bi|datahub)/';
