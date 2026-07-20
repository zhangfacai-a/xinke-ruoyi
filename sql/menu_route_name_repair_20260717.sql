set names utf8mb4;

-- Vue Router 要求动态路由名全局唯一，否则后注册的菜单会移除同名旧路由。
update sys_menu
set route_name = 'ErpSupplier', update_by = 'admin', update_time = sysdate()
where perms = 'erp:supplier:list';

update sys_menu
set route_name = 'ErpPurchaseOrder', update_by = 'admin', update_time = sysdate()
where perms = 'erp:purchase:list';

update sys_menu
set route_name = 'PurchaseMatch', update_by = 'admin', update_time = sysdate()
where perms = 'purchase:root';

update sys_menu
set route_name = 'PurchaseMatchSupplier', update_by = 'admin', update_time = sysdate()
where perms = 'purchase:supplier:list';

update sys_menu
set route_name = 'FinanceConfig', update_by = 'admin', update_time = sysdate()
where perms = 'finance:config:list';
