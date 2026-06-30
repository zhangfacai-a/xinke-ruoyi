set names utf8mb4;

update sys_menu set menu_name = '经营闭环', remark = '采购、库存、销售、商品、财务闭环工作台'
where perms = 'erp:flow:list';
update sys_menu set menu_name = '闭环查询', remark = '经营闭环详情查询'
where perms = 'erp:flow:query';
update sys_menu set menu_name = '闭环新增', remark = '经营闭环新增'
where perms = 'erp:flow:add';
update sys_menu set menu_name = '闭环修改', remark = '经营闭环修改'
where perms = 'erp:flow:edit';
update sys_menu set menu_name = '闭环删除', remark = '经营闭环删除'
where perms = 'erp:flow:remove';
update sys_menu set menu_name = '闭环审批', remark = '经营闭环审批'
where perms = 'erp:flow:audit';
update sys_menu set menu_name = '库存入库', remark = '库存入库动作'
where perms = 'erp:inventory:in';
update sys_menu set menu_name = '库存出库', remark = '库存出库动作'
where perms = 'erp:inventory:out';
update sys_menu set menu_name = '库存调拨', remark = '库存调拨动作'
where perms = 'erp:inventory:transfer';
update sys_menu set menu_name = '库存报损', remark = '库存报损动作'
where perms = 'erp:inventory:loss';
