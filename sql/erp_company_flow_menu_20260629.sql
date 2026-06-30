set names utf8mb4;

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '经营闭环', menu_id, 99, 'flow', 'erp/flow/index', null, 'ErpFlowWorkbench', 1, 0, 'C', '0', '0', 'erp:flow:list', 'tree-table', 'admin', sysdate(), '采购、库存、销售、商品、财务闭环工作台'
from sys_menu
where path = 'erp' and menu_type = 'M'
  and not exists (select 1 from sys_menu where perms = 'erp:flow:list');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '闭环查询', menu_id, 1, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:flow:query', '#', 'admin', sysdate(), '经营闭环详情查询'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:flow:query');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '闭环新增', menu_id, 2, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:flow:add', '#', 'admin', sysdate(), '经营闭环新增'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:flow:add');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '闭环修改', menu_id, 3, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:flow:edit', '#', 'admin', sysdate(), '经营闭环修改'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:flow:edit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '闭环删除', menu_id, 4, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:flow:remove', '#', 'admin', sysdate(), '经营闭环删除'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:flow:remove');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '闭环审批', menu_id, 5, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:flow:audit', '#', 'admin', sysdate(), '经营闭环审批'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:flow:audit');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存入库', menu_id, 6, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:inventory:in', '#', 'admin', sysdate(), '库存入库动作'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:inventory:in');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存出库', menu_id, 7, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:inventory:out', '#', 'admin', sysdate(), '库存出库动作'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:inventory:out');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存调拨', menu_id, 8, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:inventory:transfer', '#', 'admin', sysdate(), '库存调拨动作'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:inventory:transfer');

insert into sys_menu(menu_name, parent_id, order_num, path, component, query, route_name, is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
select '库存报损', menu_id, 9, '#', '', null, '', 1, 0, 'F', '0', '0', 'erp:inventory:loss', '#', 'admin', sysdate(), '库存报损动作'
from sys_menu
where perms = 'erp:flow:list'
  and not exists (select 1 from sys_menu where perms = 'erp:inventory:loss');
