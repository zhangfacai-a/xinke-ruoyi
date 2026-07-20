set names utf8mb4;

set @today := curdate();
set @yesterday := date_sub(@today, interval 1 day);
set @two_days_ago := date_sub(@today, interval 2 day);
set @period := date_format(@today, '%Y-%m');

delete from fin_voucher_entry where voucher_no like 'DEMO-%';
delete from fin_voucher where voucher_no like 'DEMO-%';
delete from fin_voucher_template_entry where template_code like 'DEMO-%';
delete from fin_voucher_template where template_code like 'DEMO-%';
delete from fin_subject_mapping where remark like '演示科目映射%';
delete from fin_writeoff_record where writeoff_no like 'DEMO-%';
delete from fin_cash_flow where flow_no like 'DEMO-%';
delete from fin_reconciliation_diff where diff_no like 'DEMO-%';
delete from fin_reconciliation_task where task_no like 'DEMO-%';
delete from fin_receivable_bill where receivable_no like 'DEMO-%';
delete from fin_payable_bill where payable_no like 'DEMO-%';
delete from fin_expense_bill where expense_no like 'DEMO-%';
delete from fin_platform_settlement_item where settlement_no like 'DEMO-%';
delete from fin_platform_settlement where settlement_no like 'DEMO-%';
delete from fin_payment_execute where execute_no like 'DEMO-%';
delete from fin_payment_request where payment_no like 'DEMO-%';
delete from fin_reimbursement_item where reimbursement_no like 'DEMO-%';
delete from fin_reimbursement where reimbursement_no like 'DEMO-%';
delete from fin_contract_plan where contract_no like 'DEMO-%';
delete from fin_contract where contract_no like 'DEMO-%';
delete from fin_ledger_entry where voucher_no like 'DEMO-%';
delete from fin_trial_balance where period_code = @period;
delete from fin_profit_statement where period_code = @period;
delete from fin_period_close where period_code = @period and close_scope = 'demo_company';
delete from fin_platform_account where account_no like 'DEMO-%';
delete from fin_bank_account where bank_no like 'DEMO-%';
delete from fin_operator_profit_settlement where settlement_no like 'DEMO-%';
delete from fin_operator_profit_day where operator_name in ('赵运营', '钱运营', '孙运营');
delete from fin_order_profit_attribution where sales_no like '9100%';

delete from erp_platform_settlement_match where match_no like 'DEMO-%';
delete from erp_order_profit where sales_no like 'DEMO-%' or sales_no like '9100%';
delete from erp_after_sale_order where after_sale_no like 'DEMO-%';
delete from erp_compensation_order where compensation_no like 'DEMO-%';
delete from erp_sales_shipment_item where shipment_no like 'DEMO-%';
delete from erp_sales_shipment where shipment_no like 'DEMO-%';
delete from erp_purchase_request_item where request_no like 'DEMO-%';
delete from erp_purchase_request where request_no like 'DEMO-%';
delete from erp_purchase_receipt_item where receipt_no like 'DEMO-%';
delete from erp_purchase_receipt where receipt_no like 'DEMO-%';
delete from erp_purchase_return_item where return_no like 'DEMO-%';
delete from erp_purchase_return where return_no like 'DEMO-%';
delete from erp_supplier_reconcile where reconcile_no like 'DEMO-%';
delete from erp_inventory_transfer_item where transfer_no like 'DEMO-%';
delete from erp_inventory_transfer where transfer_no like 'DEMO-%';
delete from erp_inventory_stocktake_item where stocktake_no like 'DEMO-%';
delete from erp_inventory_stocktake where stocktake_no like 'DEMO-%';
delete from erp_inventory_loss where loss_no like 'DEMO-%';
delete from erp_inventory_movement where movement_no like 'DEMO-%';
delete from erp_inventory_cost_layer where source_no like 'DEMO-%';
delete from erp_inventory_ageing_snapshot where sku_code like 'DEMO-SKU-%';
delete from erp_inventory_warning_rule where sku_code like 'DEMO-SKU-%';
delete from erp_sku_barcode where sku_code like 'DEMO-SKU-%';
delete from erp_platform_sku_mapping where platform_sku_id like 'DEMO-%';
delete from erp_product_bundle_item where component_sku_code like 'DEMO-SKU-%';
delete from erp_product_bundle where bundle_sku_code like 'DEMO-SKU-%';
delete from erp_sku_cost_history where sku_code like 'DEMO-SKU-%';
delete from erp_supplier_quote where quote_no like 'DEMO-%';
delete from erp_operator_attribution_rule where rule_code like 'DEMO-%';
delete from erp_refund_order where refund_no like 'DEMO-%';
delete from erp_settlement_bill where settlement_no like 'DEMO-%';
delete from erp_import_batch where batch_no like 'DEMO-%';
delete from erp_sales_order_item where sales_no like 'DEMO-%';
delete from erp_sales_order where sales_no like 'DEMO-%';
delete from erp_purchase_order_item where purchase_no like 'DEMO-%';
delete from erp_purchase_order where purchase_no like 'DEMO-%';
delete from erp_inventory_balance where sku_code like 'DEMO-SKU-%';

delete from dws_finance_day_profit where dt in (@today, @yesterday, @two_days_ago);
delete from dwd_order_item where order_id between 910001 and 910099;
delete from ods_order_raw where order_id between 910001 and 910099;

delete from erp_customer where customer_code like 'DEMO-%';
delete from erp_product_sku where sku_code like 'DEMO-SKU-%';
delete from erp_product where product_code like 'DEMO-P%';
delete from erp_supplier where supplier_code like 'DEMO-SUP%';
delete from erp_warehouse where warehouse_code like 'DEMO-WH%';
delete from erp_shop where shop_code like 'DEMO-SHOP%';

insert into erp_shop(shop_code, shop_name, platform_code, platform_name, owner_name, status, create_by, create_time, remark) values
('DEMO-SHOP-DY', '抖音旗舰店', 'douyin', '抖音电商', '赵运营', '0', 'admin', sysdate(), '演示店铺'),
('DEMO-SHOP-TM', '天猫官方店', 'tmall', '天猫', '钱运营', '0', 'admin', sysdate(), '演示店铺'),
('DEMO-SHOP-JD', '京东自营店', 'jd', '京东', '孙运营', '0', 'admin', sysdate(), '演示店铺');

insert into erp_warehouse(
    warehouse_code, warehouse_name, warehouse_type, warehouse_usage, ownership_type,
    provider_name, external_warehouse_code, owner_code, sync_mode, sync_status,
    enable_location, allow_negative_stock, priority,
    contact_name, contact_phone, address, status, create_by, create_time, remark
) values
('DEMO-WH-HZ', '杭州中心仓', 'physical', 'normal', 'self_operated',
 null, null, null, 'manual', 'not_applicable', '1', '0', 10,
 '李仓管', '13800001111', '杭州市余杭区未来科技城', '0', 'admin', sysdate(), '演示实体仓'),
('DEMO-WH-SH', '上海云仓', 'cloud', 'normal', 'outsourced',
 '演示云仓服务商', 'CLOUD-SH-01', 'OWNER-XINKE', 'manual', 'never', '0', '0', 20,
 '周仓管', '13800002222', '上海市青浦区云仓园区', '0', 'admin', sysdate(), '演示云仓');

insert into erp_supplier(supplier_code, supplier_name, contact_name, contact_phone, settlement_type, payment_days, status, create_by, create_time, remark) values
('DEMO-SUP-A', '广州源头供应链有限公司', '王经理', '13900001111', 'monthly', 30, '0', 'admin', sysdate(), '主营小家电'),
('DEMO-SUP-B', '深圳优选科技有限公司', '陈经理', '13900002222', 'monthly', 15, '0', 'admin', sysdate(), '主营数码配件');

insert into erp_customer(customer_code, customer_name, customer_type, contact_name, contact_phone, status, create_by, create_time, remark) values
('DEMO-CUS-001', '抖音买家-李女士', 'platform_buyer', '李女士', '13700000001', '0', 'admin', sysdate(), '演示客户'),
('DEMO-CUS-002', '天猫买家-张先生', 'platform_buyer', '张先生', '13700000002', '0', 'admin', sysdate(), '演示客户'),
('DEMO-CUS-003', '京东买家-刘女士', 'platform_buyer', '刘女士', '13700000003', '0', 'admin', sysdate(), '演示客户');

insert into erp_product(product_code, product_name, category_name, brand_name, cost_price, sale_price, status, create_by, create_time, remark) values
('DEMO-P001', '便携榨汁杯', '小家电', 'Xinke Home', 68.00, 129.00, '0', 'admin', sysdate(), '演示商品'),
('DEMO-P002', '无线蓝牙耳机', '数码配件', 'Xinke Audio', 89.00, 199.00, '0', 'admin', sysdate(), '演示商品'),
('DEMO-P003', '智能保温杯', '居家百货', 'Xinke Life', 45.00, 99.00, '0', 'admin', sysdate(), '演示商品'),
('DEMO-P004', '夏季防晒套装', '美妆个护', 'Xinke Beauty', 52.00, 149.00, '0', 'admin', sysdate(), '组合商品演示');

set @p1 := (select product_id from erp_product where product_code = 'DEMO-P001');
set @p2 := (select product_id from erp_product where product_code = 'DEMO-P002');
set @p3 := (select product_id from erp_product where product_code = 'DEMO-P003');
set @p4 := (select product_id from erp_product where product_code = 'DEMO-P004');

insert into erp_product_sku(product_id, sku_code, sku_name, cost_price, sale_price, stock_qty, status, create_by, create_time, remark) values
(@p1, 'DEMO-SKU-001', '榨汁杯-奶白色', 68.00, 129.00, 420, '0', 'admin', sysdate(), '爆款SKU'),
(@p1, 'DEMO-SKU-002', '榨汁杯-薄荷绿', 70.00, 139.00, 260, '0', 'admin', sysdate(), '渠道款'),
(@p2, 'DEMO-SKU-003', '蓝牙耳机-标准版', 89.00, 199.00, 180, '0', 'admin', sysdate(), '高毛利SKU'),
(@p2, 'DEMO-SKU-004', '蓝牙耳机-Pro版', 128.00, 269.00, 95, '0', 'admin', sysdate(), '高客单SKU'),
(@p3, 'DEMO-SKU-005', '保温杯-钛灰色', 45.00, 99.00, 320, '0', 'admin', sysdate(), '稳定款'),
(@p4, 'DEMO-SKU-006', '防晒套装-三件套', 52.00, 149.00, 150, '0', 'admin', sysdate(), '组合装');

set @sku1 := (select sku_id from erp_product_sku where sku_code = 'DEMO-SKU-001');
set @sku2 := (select sku_id from erp_product_sku where sku_code = 'DEMO-SKU-002');
set @sku3 := (select sku_id from erp_product_sku where sku_code = 'DEMO-SKU-003');
set @sku4 := (select sku_id from erp_product_sku where sku_code = 'DEMO-SKU-004');
set @sku5 := (select sku_id from erp_product_sku where sku_code = 'DEMO-SKU-005');
set @sku6 := (select sku_id from erp_product_sku where sku_code = 'DEMO-SKU-006');
set @shop_dy := (select shop_id from erp_shop where shop_code = 'DEMO-SHOP-DY');
set @shop_tm := (select shop_id from erp_shop where shop_code = 'DEMO-SHOP-TM');
set @shop_jd := (select shop_id from erp_shop where shop_code = 'DEMO-SHOP-JD');
set @wh_hz := (select warehouse_id from erp_warehouse where warehouse_code = 'DEMO-WH-HZ');
set @wh_sh := (select warehouse_id from erp_warehouse where warehouse_code = 'DEMO-WH-SH');
set @sup_a := (select supplier_id from erp_supplier where supplier_code = 'DEMO-SUP-A');
set @sup_b := (select supplier_id from erp_supplier where supplier_code = 'DEMO-SUP-B');
set @cus1 := (select customer_id from erp_customer where customer_code = 'DEMO-CUS-001');
set @cus2 := (select customer_id from erp_customer where customer_code = 'DEMO-CUS-002');
set @cus3 := (select customer_id from erp_customer where customer_code = 'DEMO-CUS-003');

insert into erp_inventory_balance(warehouse_id, warehouse_name, sku_id, sku_code, sku_name, available_qty, locked_qty, inbound_qty, defective_qty, safety_qty, cost_price, create_time, update_time) values
(@wh_hz, '杭州中心仓', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 420, 18, 60, 3, 80, 68.00, sysdate(), sysdate()),
(@wh_hz, '杭州中心仓', @sku2, 'DEMO-SKU-002', '榨汁杯-薄荷绿', 260, 8, 30, 1, 60, 70.00, sysdate(), sysdate()),
(@wh_hz, '杭州中心仓', @sku3, 'DEMO-SKU-003', '蓝牙耳机-标准版', 180, 12, 40, 2, 50, 89.00, sysdate(), sysdate()),
(@wh_sh, '上海前置仓', @sku4, 'DEMO-SKU-004', '蓝牙耳机-Pro版', 95, 5, 20, 0, 30, 128.00, sysdate(), sysdate()),
(@wh_sh, '上海前置仓', @sku5, 'DEMO-SKU-005', '保温杯-钛灰色', 320, 10, 50, 4, 70, 45.00, sysdate(), sysdate()),
(@wh_sh, '上海前置仓', @sku6, 'DEMO-SKU-006', '防晒套装-三件套', 150, 6, 25, 1, 40, 52.00, sysdate(), sysdate());

insert into erp_purchase_order(purchase_no, supplier_id, supplier_name, warehouse_id, total_amount, tax_amount, total_with_tax, purchase_status, purchase_date, expected_time, create_by, create_time, remark) values
('DEMO-PO-001', @sup_a, '广州源头供应链有限公司', @wh_hz, 28600.00, 3718.00, 32318.00, 'approved', date_sub(curdate(), interval 10 day), date_add(sysdate(), interval 3 day), 'admin', sysdate(), '榨汁杯补货'),
('DEMO-PO-002', @sup_b, '深圳优选科技有限公司', @wh_sh, 34500.00, 4485.00, 38985.00, 'processing', date_sub(curdate(), interval 5 day), date_add(sysdate(), interval 5 day), 'admin', sysdate(), '耳机和套装补货'),
('DEMO-PO-003', @sup_a, '广州源头供应链有限公司', @wh_hz, 13500.00, 1755.00, 15255.00, 'draft', curdate(), date_add(sysdate(), interval 7 day), 'admin', sysdate(), '保温杯试单');

set @po1 := (select purchase_id from erp_purchase_order where purchase_no = 'DEMO-PO-001');
set @po2 := (select purchase_id from erp_purchase_order where purchase_no = 'DEMO-PO-002');
insert into erp_purchase_order_item(purchase_id, purchase_no, sku_id, sku_code, sku_name, quantity, received_qty, purchase_price, tax_rate, amount, tax_amount, tax_inclusive_amount) values
(@po1, 'DEMO-PO-001', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 300, 180, 66.00, 0.13, 19800.00, 2574.00, 22374.00),
(@po1, 'DEMO-PO-001', @sku2, 'DEMO-SKU-002', '榨汁杯-薄荷绿', 120, 80, 73.33, 0.13, 8800.00, 1144.00, 9944.00),
(@po2, 'DEMO-PO-002', @sku3, 'DEMO-SKU-003', '蓝牙耳机-标准版', 160, 90, 90.00, 0.13, 14400.00, 1872.00, 16272.00),
(@po2, 'DEMO-PO-002', @sku4, 'DEMO-SKU-004', '蓝牙耳机-Pro版', 80, 20, 131.25, 0.13, 10500.00, 1365.00, 11865.00),
(@po2, 'DEMO-PO-002', @sku6, 'DEMO-SKU-006', '防晒套装-三件套', 180, 60, 53.33, 0.13, 9600.00, 1248.00, 10848.00);

insert into erp_sales_order(sales_no, platform_order_no, shop_id, shop_name, customer_id, order_status, pay_amount, pay_time, create_by, create_time, remark) values
('DEMO-SO-001', 'DY202606290001', @shop_dy, '抖音旗舰店', @cus1, 'paid', 387.00, concat(@today, ' 10:12:00'), 'admin', sysdate(), '直播间订单'),
('DEMO-SO-002', 'TM202606290002', @shop_tm, '天猫官方店', @cus2, 'shipped', 199.00, concat(@today, ' 11:05:00'), 'admin', sysdate(), '搜索订单'),
('DEMO-SO-003', 'JD202606280003', @shop_jd, '京东自营店', @cus3, 'completed', 537.00, concat(@yesterday, ' 16:44:00'), 'admin', sysdate(), '站外投放订单');

set @so1 := (select sales_id from erp_sales_order where sales_no = 'DEMO-SO-001');
set @so2 := (select sales_id from erp_sales_order where sales_no = 'DEMO-SO-002');
set @so3 := (select sales_id from erp_sales_order where sales_no = 'DEMO-SO-003');
insert into erp_sales_order_item(sales_id, sales_no, sku_id, sku_code, sku_name, quantity, pay_amount, product_cost) values
(@so1, 'DEMO-SO-001', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 2, 258.00, 136.00),
(@so1, 'DEMO-SO-001', @sku5, 'DEMO-SKU-005', '保温杯-钛灰色', 1, 129.00, 45.00),
(@so2, 'DEMO-SO-002', @sku3, 'DEMO-SKU-003', '蓝牙耳机-标准版', 1, 199.00, 89.00),
(@so3, 'DEMO-SO-003', @sku4, 'DEMO-SKU-004', '蓝牙耳机-Pro版', 1, 269.00, 128.00),
(@so3, 'DEMO-SO-003', @sku6, 'DEMO-SKU-006', '防晒套装-三件套', 2, 268.00, 104.00);

insert into ods_order_raw(order_id, shop_id, shop_name, order_status, pay_time, platform_fee, ad_cost, freight_fee, refund_cost, channel_code, channel_name, sku_order_list, raw_payload, create_time) values
(910001, @shop_dy, '抖音旗舰店', 1, concat(@today, ' 10:12:00'), 18.20, 42.00, 9.00, 0.00, 'live', '直播间', json_array(json_object('productId', @p1, 'productName', '便携榨汁杯', 'skuId', @sku1, 'skuName', '榨汁杯-奶白色', 'quantity', 2, 'payAmount', 258.00, 'productCost', 136.00), json_object('productId', @p3, 'productName', '智能保温杯', 'skuId', @sku5, 'skuName', '保温杯-钛灰色', 'quantity', 1, 'payAmount', 129.00, 'productCost', 45.00)), json_object('source', 'demo'), sysdate()),
(910002, @shop_tm, '天猫官方店', 1, concat(@today, ' 11:05:00'), 9.95, 18.00, 6.00, 0.00, 'search', '平台搜索', json_array(json_object('productId', @p2, 'productName', '无线蓝牙耳机', 'skuId', @sku3, 'skuName', '蓝牙耳机-标准版', 'quantity', 1, 'payAmount', 199.00, 'productCost', 89.00)), json_object('source', 'demo'), sysdate()),
(910003, @shop_jd, '京东自营店', 1, concat(@yesterday, ' 16:44:00'), 21.48, 55.00, 12.00, 0.00, 'ad_feed', '信息流投放', json_array(json_object('productId', @p2, 'productName', '无线蓝牙耳机', 'skuId', @sku4, 'skuName', '蓝牙耳机-Pro版', 'quantity', 1, 'payAmount', 269.00, 'productCost', 128.00), json_object('productId', @p4, 'productName', '夏季防晒套装', 'skuId', @sku6, 'skuName', '防晒套装-三件套', 'quantity', 2, 'payAmount', 268.00, 'productCost', 104.00)), json_object('source', 'demo'), sysdate()),
(910004, @shop_dy, '抖音旗舰店', 1, concat(@yesterday, ' 19:30:00'), 12.90, 26.00, 8.00, 20.00, 'short_video', '短视频自然流', json_array(json_object('productId', @p1, 'productName', '便携榨汁杯', 'skuId', @sku2, 'skuName', '榨汁杯-薄荷绿', 'quantity', 1, 'payAmount', 139.00, 'productCost', 70.00)), json_object('source', 'demo'), sysdate()),
(910005, @shop_tm, '天猫官方店', 1, concat(@two_days_ago, ' 14:22:00'), 14.90, 30.00, 7.00, 0.00, 'search', '平台搜索', json_array(json_object('productId', @p4, 'productName', '夏季防晒套装', 'skuId', @sku6, 'skuName', '防晒套装-三件套', 'quantity', 2, 'payAmount', 298.00, 'productCost', 104.00)), json_object('source', 'demo'), sysdate()),
(910006, @shop_jd, '京东自营店', 1, concat(@two_days_ago, ' 09:38:00'), 8.91, 12.00, 6.00, 0.00, 'affiliate', '达人分销', json_array(json_object('productId', @p3, 'productName', '智能保温杯', 'skuId', @sku5, 'skuName', '保温杯-钛灰色', 'quantity', 1, 'payAmount', 99.00, 'productCost', 45.00)), json_object('source', 'demo'), sysdate());

insert into dwd_order_item(order_id, shop_id, shop_name, product_id, product_name, sku_id, sku_name, quantity, channel_code, channel_name, pay_amount, product_cost, platform_fee, ad_cost, freight_fee, refund_cost, profit_amount, order_status, pay_time, dt) values
(910001, @shop_dy, '抖音旗舰店', @p1, '便携榨汁杯', @sku1, '榨汁杯-奶白色', 2, 'live', '直播间', 258.00, 136.00, 12.13, 28.00, 6.00, 0.00, 75.87, 1, concat(@today, ' 10:12:00'), @today),
(910001, @shop_dy, '抖音旗舰店', @p3, '智能保温杯', @sku5, '保温杯-钛灰色', 1, 'live', '直播间', 129.00, 45.00, 6.07, 14.00, 3.00, 0.00, 60.93, 1, concat(@today, ' 10:12:00'), @today),
(910002, @shop_tm, '天猫官方店', @p2, '无线蓝牙耳机', @sku3, '蓝牙耳机-标准版', 1, 'search', '平台搜索', 199.00, 89.00, 9.95, 18.00, 6.00, 0.00, 76.05, 1, concat(@today, ' 11:05:00'), @today),
(910003, @shop_jd, '京东自营店', @p2, '无线蓝牙耳机', @sku4, '蓝牙耳机-Pro版', 1, 'ad_feed', '信息流投放', 269.00, 128.00, 10.74, 27.50, 6.00, 0.00, 96.76, 1, concat(@yesterday, ' 16:44:00'), @yesterday),
(910003, @shop_jd, '京东自营店', @p4, '夏季防晒套装', @sku6, '防晒套装-三件套', 2, 'ad_feed', '信息流投放', 268.00, 104.00, 10.74, 27.50, 6.00, 0.00, 119.76, 1, concat(@yesterday, ' 16:44:00'), @yesterday),
(910004, @shop_dy, '抖音旗舰店', @p1, '便携榨汁杯', @sku2, '榨汁杯-薄荷绿', 1, 'short_video', '短视频自然流', 139.00, 70.00, 12.90, 26.00, 8.00, 20.00, 2.10, 1, concat(@yesterday, ' 19:30:00'), @yesterday),
(910005, @shop_tm, '天猫官方店', @p4, '夏季防晒套装', @sku6, '防晒套装-三件套', 2, 'search', '平台搜索', 298.00, 104.00, 14.90, 30.00, 7.00, 0.00, 142.10, 1, concat(@two_days_ago, ' 14:22:00'), @two_days_ago),
(910006, @shop_jd, '京东自营店', @p3, '智能保温杯', @sku5, '保温杯-钛灰色', 1, 'affiliate', '达人分销', 99.00, 45.00, 8.91, 12.00, 6.00, 0.00, 27.09, 1, concat(@two_days_ago, ' 09:38:00'), @two_days_ago);

insert into dws_finance_day_profit(dt, shop_id, shop_name, gmv, net_amount, product_cost, platform_fee, ad_cost, freight_fee, after_sale_cost, profit_amount, order_count) values
(@today, @shop_dy, '抖音旗舰店', 387.00, 387.00, 181.00, 18.20, 42.00, 9.00, 0.00, 136.80, 1),
(@today, @shop_tm, '天猫官方店', 199.00, 199.00, 89.00, 9.95, 18.00, 6.00, 0.00, 76.05, 1),
(@yesterday, @shop_jd, '京东自营店', 537.00, 537.00, 232.00, 21.48, 55.00, 12.00, 0.00, 216.52, 1),
(@yesterday, @shop_dy, '抖音旗舰店', 139.00, 119.00, 70.00, 12.90, 26.00, 8.00, 20.00, 2.10, 1),
(@two_days_ago, @shop_tm, '天猫官方店', 298.00, 298.00, 104.00, 14.90, 30.00, 7.00, 0.00, 142.10, 1),
(@two_days_ago, @shop_jd, '京东自营店', 99.00, 99.00, 45.00, 8.91, 12.00, 6.00, 0.00, 27.09, 1);

insert into erp_operator_attribution_rule(rule_code, rule_name, shop_id, shop_name, product_id, product_name, sku_id, sku_code, sku_name, channel_code, channel_name, operator_id, operator_name, share_rate, priority, effective_start, effective_end, status, create_by, create_time, remark) values
('DEMO-RULE-001', '赵运营-抖音直播榨汁杯', @shop_dy, '抖音旗舰店', @p1, '便携榨汁杯', null, null, null, 'live', '直播间', 1001, '赵运营', 1.0000, 10, date_sub(@today, interval 30 day), null, '0', 'admin', sysdate(), '按商品+渠道归因'),
('DEMO-RULE-002', '钱运营-天猫搜索数码', @shop_tm, '天猫官方店', @p2, '无线蓝牙耳机', null, null, null, 'search', '平台搜索', 1002, '钱运营', 1.0000, 20, date_sub(@today, interval 30 day), null, '0', 'admin', sysdate(), '按商品+渠道归因'),
('DEMO-RULE-003', '孙运营-京东投放', @shop_jd, '京东自营店', null, null, null, null, null, 'ad_feed', '信息流投放', 1003, '孙运营', 0.9000, 30, date_sub(@today, interval 30 day), null, '0', 'admin', sysdate(), '投放渠道扣除10%协作成本'),
('DEMO-RULE-004', '赵运营-抖音自然流', @shop_dy, '抖音旗舰店', null, null, null, null, null, 'short_video', '短视频自然流', 1001, '赵运营', 1.0000, 40, date_sub(@today, interval 30 day), null, '0', 'admin', sysdate(), '自然流归运营'),
('DEMO-RULE-005', '钱运营-套装搜索', @shop_tm, '天猫官方店', @p4, '夏季防晒套装', @sku6, 'DEMO-SKU-006', '防晒套装-三件套', 'search', '平台搜索', 1002, '钱运营', 0.9500, 5, date_sub(@today, interval 30 day), null, '0', 'admin', sysdate(), 'SKU级别规则优先'),
('DEMO-RULE-006', '孙运营-达人分销', @shop_jd, '京东自营店', null, null, null, null, null, 'affiliate', '达人分销', 1003, '孙运营', 0.8500, 15, date_sub(@today, interval 30 day), null, '0', 'admin', sysdate(), '达人分销归因');

insert into fin_order_profit_attribution(sales_no, order_id, order_item_id, shop_id, shop_name, product_id, product_name, sku_id, sku_code, sku_name, channel_code, channel_name, operator_id, operator_name, attribution_rule_code, share_rate, pay_amount, net_amount, product_cost, platform_fee, promotion_fee, freight_fee, after_sale_cost, profit_amount, share_profit_amount, profit_date, period_code, create_time, remark)
select cast(order_id as char), order_id, order_item_id, shop_id, shop_name, product_id, product_name, sku_id, null, sku_name, channel_code, channel_name,
       case when shop_id = @shop_dy then 1001 when shop_id = @shop_tm then 1002 else 1003 end,
       case when shop_id = @shop_dy then '赵运营' when shop_id = @shop_tm then '钱运营' else '孙运营' end,
       case when shop_id = @shop_dy then 'DEMO-RULE-001' when shop_id = @shop_tm then 'DEMO-RULE-002' else 'DEMO-RULE-003' end,
       case when channel_code in ('ad_feed') then 0.9000 when channel_code in ('affiliate') then 0.8500 when sku_id = @sku6 and shop_id = @shop_tm then 0.9500 else 1.0000 end,
       pay_amount, pay_amount - refund_cost, product_cost, platform_fee, ad_cost, freight_fee, refund_cost, profit_amount,
       round(profit_amount * (case when channel_code in ('ad_feed') then 0.9000 when channel_code in ('affiliate') then 0.8500 when sku_id = @sku6 and shop_id = @shop_tm then 0.9500 else 1.0000 end), 2),
       dt, date_format(dt, '%Y-%m'), sysdate(), '演示归因明细'
from dwd_order_item
where order_id between 910001 and 910099;

insert into fin_operator_profit_day(dt, period_code, shop_id, shop_name, operator_id, operator_name, channel_code, channel_name, order_count, gmv, net_amount, product_cost, platform_fee, ad_cost, freight_fee, after_sale_cost, profit_amount, share_profit_amount, settlement_amount, create_time, update_time)
select profit_date, max(period_code), shop_id, max(shop_name), operator_id, max(operator_name), channel_code, max(channel_name), count(distinct sales_no),
       sum(pay_amount), sum(net_amount), sum(product_cost), sum(platform_fee), sum(promotion_fee), sum(freight_fee), sum(after_sale_cost), sum(profit_amount), sum(share_profit_amount), 0.00, sysdate(), sysdate()
from fin_order_profit_attribution
where sales_no like '9100%'
group by profit_date, shop_id, operator_id, channel_code;

insert into fin_operator_profit_settlement(settlement_no, period_code, operator_id, operator_name, shop_id, shop_name, channel_code, channel_name, profit_amount, share_profit_amount, paid_amount, remain_amount, settlement_status, create_by, create_time, remark) values
('DEMO-OPS-001', @period, 1001, '赵运营', @shop_dy, '抖音旗舰店', 'live', '直播间', 136.80, 136.80, 0.00, 136.80, 'draft', 'admin', sysdate(), '演示运营分润结算'),
('DEMO-OPS-002', @period, 1002, '钱运营', @shop_tm, '天猫官方店', 'search', '平台搜索', 218.15, 211.05, 80.00, 131.05, 'approved', 'admin', sysdate(), '演示运营分润结算'),
('DEMO-OPS-003', @period, 1003, '孙运营', @shop_jd, '京东自营店', 'ad_feed', '信息流投放', 216.52, 194.87, 100.00, 94.87, 'processing', 'admin', sysdate(), '演示运营分润结算');

insert into erp_purchase_request(request_no, request_title, requester_name, department_name, supplier_id, supplier_name, warehouse_id, expected_date, total_amount, request_status, approve_by, approve_time, create_by, create_time, remark) values
('DEMO-PR-001', '榨汁杯直播爆款补货申请', '赵运营', '电商运营部', @sup_a, '广州源头供应链有限公司', @wh_hz, date_add(@today, interval 5 day), 28600.00, 'approved', '财务主管', sysdate(), 'admin', sysdate(), '演示采购申请'),
('DEMO-PR-002', '耳机大促备货申请', '钱运营', '电商运营部', @sup_b, '深圳优选科技有限公司', @wh_sh, date_add(@today, interval 7 day), 34500.00, 'draft', '', null, 'admin', sysdate(), '演示采购申请');
insert into erp_purchase_request_item(request_no, sku_id, sku_code, sku_name, quantity, quote_price, amount, remark) values
('DEMO-PR-001', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 300, 66.00, 19800.00, '直播预估销量'),
('DEMO-PR-001', @sku2, 'DEMO-SKU-002', '榨汁杯-薄荷绿', 120, 73.33, 8800.00, '渠道款补货'),
('DEMO-PR-002', @sku3, 'DEMO-SKU-003', '蓝牙耳机-标准版', 160, 90.00, 14400.00, '大促备货');

insert into erp_purchase_receipt(receipt_no, purchase_no, supplier_id, supplier_name, warehouse_id, warehouse_name, receipt_date, total_qty, total_amount, receipt_status, create_by, create_time, remark) values
('DEMO-RCV-001', 'DEMO-PO-001', @sup_a, '广州源头供应链有限公司', @wh_hz, '杭州中心仓', @today, 260, 24200.00, 'done', 'admin', sysdate(), '采购到货入库'),
('DEMO-RCV-002', 'DEMO-PO-002', @sup_b, '深圳优选科技有限公司', @wh_sh, '上海前置仓', @yesterday, 170, 27100.00, 'processing', 'admin', sysdate(), '分批到货');
insert into erp_purchase_receipt_item(receipt_no, purchase_no, sku_id, sku_code, sku_name, quantity, cost_price, amount, batch_no, production_date) values
('DEMO-RCV-001', 'DEMO-PO-001', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 180, 66.00, 11880.00, 'BATCH-DEMO-001', date_sub(@today, interval 12 day)),
('DEMO-RCV-001', 'DEMO-PO-001', @sku2, 'DEMO-SKU-002', '榨汁杯-薄荷绿', 80, 73.33, 5866.40, 'BATCH-DEMO-002', date_sub(@today, interval 10 day)),
('DEMO-RCV-002', 'DEMO-PO-002', @sku3, 'DEMO-SKU-003', '蓝牙耳机-标准版', 90, 90.00, 8100.00, 'BATCH-DEMO-003', date_sub(@today, interval 8 day));

insert into erp_purchase_return(return_no, purchase_no, receipt_no, supplier_id, supplier_name, warehouse_id, return_date, total_qty, total_amount, return_reason, return_status, create_by, create_time, remark) values
('DEMO-PRT-001', 'DEMO-PO-001', 'DEMO-RCV-001', @sup_a, '广州源头供应链有限公司', @wh_hz, @today, 6, 396.00, '外观瑕疵退回供应商', 'approved', 'admin', sysdate(), '演示采购退货');
insert into erp_purchase_return_item(return_no, sku_id, sku_code, sku_name, quantity, cost_price, amount) values
('DEMO-PRT-001', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 6, 66.00, 396.00);

insert into erp_supplier_reconcile(reconcile_no, supplier_id, supplier_name, period_code, purchase_amount, return_amount, payable_amount, diff_amount, reconcile_status, create_by, create_time, remark) values
('DEMO-SRC-001', @sup_a, '广州源头供应链有限公司', @period, 28600.00, 396.00, 28204.00, 0.00, 'approved', 'admin', sysdate(), '演示供应商对账'),
('DEMO-SRC-002', @sup_b, '深圳优选科技有限公司', @period, 34500.00, 0.00, 34500.00, 120.00, 'processing', 'admin', sysdate(), '待确认差异');

insert into erp_inventory_movement(movement_no, warehouse_id, sku_id, movement_type, source_type, source_no, quantity, before_qty, after_qty, cost_price, operator_name, movement_time, create_by, create_time, remark) values
('DEMO-MOV-001', @wh_hz, @sku1, 'in', 'purchase_receipt', 'DEMO-RCV-001', 180, 240, 420, 66.00, '李仓管', concat(@today, ' 09:00:00'), 'admin', sysdate(), '采购入库'),
('DEMO-MOV-002', @wh_hz, @sku1, 'out', 'sales_order', 'DEMO-SO-001', 2, 422, 420, 68.00, '系统', concat(@today, ' 10:20:00'), 'admin', sysdate(), '销售出库'),
('DEMO-MOV-003', @wh_sh, @sku6, 'transfer_in', 'transfer', 'DEMO-TRF-001', 40, 110, 150, 52.00, '周仓管', concat(@yesterday, ' 15:00:00'), 'admin', sysdate(), '调拨入库'),
('DEMO-MOV-004', @wh_sh, @sku5, 'loss', 'inventory_loss', 'DEMO-LOS-001', 4, 324, 320, 45.00, '周仓管', concat(@today, ' 15:30:00'), 'admin', sysdate(), '盘点报损');
insert into erp_inventory_transfer(transfer_no, from_warehouse_id, from_warehouse_name, to_warehouse_id, to_warehouse_name, transfer_date, total_qty, transfer_status, create_by, create_time, remark) values
('DEMO-TRF-001', @wh_hz, '杭州中心仓', @wh_sh, '上海前置仓', @yesterday, 40, 'done', 'admin', sysdate(), '华东前置仓补货');
insert into erp_inventory_transfer_item(transfer_no, sku_id, sku_code, sku_name, quantity, cost_price) values
('DEMO-TRF-001', @sku6, 'DEMO-SKU-006', '防晒套装-三件套', 40, 52.00);
insert into erp_inventory_stocktake(stocktake_no, warehouse_id, warehouse_name, stocktake_date, total_sku_count, profit_loss_amount, stocktake_status, create_by, create_time, remark) values
('DEMO-STK-001', @wh_sh, '上海前置仓', @today, 3, -180.00, 'done', 'admin', sysdate(), '月中抽盘');
insert into erp_inventory_stocktake_item(stocktake_no, sku_id, sku_code, sku_name, book_qty, actual_qty, diff_qty, cost_price, diff_amount) values
('DEMO-STK-001', @sku5, 'DEMO-SKU-005', '保温杯-钛灰色', 324, 320, -4, 45.00, -180.00);
insert into erp_inventory_loss(loss_no, warehouse_id, sku_id, sku_code, sku_name, quantity, cost_price, loss_amount, loss_reason, loss_status, create_by, create_time, remark) values
('DEMO-LOS-001', @wh_sh, @sku5, 'DEMO-SKU-005', '保温杯-钛灰色', 4, 45.00, 180.00, '包装破损', 'approved', 'admin', sysdate(), '演示报损');
insert into erp_inventory_warning_rule(warehouse_id, sku_id, sku_code, min_qty, max_qty, warning_level, status, create_by, create_time, remark) values
(@wh_sh, @sku4, 'DEMO-SKU-004', 120, 500, 'high', '0', 'admin', sysdate(), 'Pro版库存低于安全线'),
(@wh_hz, @sku2, 'DEMO-SKU-002', 200, 600, 'normal', '0', 'admin', sysdate(), '薄荷绿库存关注');
insert into erp_inventory_cost_layer(warehouse_id, sku_id, batch_no, source_type, source_no, remain_qty, cost_price, inbound_date, create_time) values
(@wh_hz, @sku1, 'BATCH-DEMO-001', 'purchase_receipt', 'DEMO-RCV-001', 174, 66.00, @today, sysdate()),
(@wh_sh, @sku6, 'BATCH-DEMO-004', 'transfer', 'DEMO-TRF-001', 40, 52.00, @yesterday, sysdate());
insert into erp_inventory_ageing_snapshot(snapshot_date, warehouse_id, sku_id, sku_code, sku_name, qty_0_30, qty_31_60, qty_61_90, qty_90_plus, amount_90_plus, create_time) values
(@today, @wh_hz, @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 360, 50, 10, 0, 0.00, sysdate()),
(@today, @wh_sh, @sku4, 'DEMO-SKU-004', '蓝牙耳机-Pro版', 60, 20, 10, 5, 640.00, sysdate());

insert into erp_sales_shipment(shipment_no, sales_no, warehouse_id, logistics_company, logistics_no, shipped_time, shipment_status, create_by, create_time, remark) values
('DEMO-SHP-001', 'DEMO-SO-001', @wh_hz, '顺丰速运', 'SF-DEMO-001', concat(@today, ' 13:00:00'), 'shipped', 'admin', sysdate(), '演示发货'),
('DEMO-SHP-002', 'DEMO-SO-003', @wh_sh, '京东物流', 'JD-DEMO-003', concat(@yesterday, ' 18:10:00'), 'signed', 'admin', sysdate(), '演示发货');
insert into erp_sales_shipment_item(shipment_no, sales_no, sku_id, sku_code, sku_name, quantity) values
('DEMO-SHP-001', 'DEMO-SO-001', @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 2),
('DEMO-SHP-001', 'DEMO-SO-001', @sku5, 'DEMO-SKU-005', '保温杯-钛灰色', 1),
('DEMO-SHP-002', 'DEMO-SO-003', @sku4, 'DEMO-SKU-004', '蓝牙耳机-Pro版', 1);
insert into erp_after_sale_order(after_sale_no, sales_no, refund_no, after_sale_type, after_sale_status, customer_name, amount, reason, create_by, create_time, remark) values
('DEMO-AS-001', 'DEMO-SO-001', 'DEMO-RF-001', 'refund', 'processing', '抖音买家-李女士', 20.00, '物流延迟补偿退款', 'admin', sysdate(), '演示售后'),
('DEMO-AS-002', 'DEMO-SO-003', 'DEMO-RF-002', 'return_refund', 'done', '京东买家-刘女士', 99.00, '七天无理由', 'admin', sysdate(), '演示售后');
insert into erp_compensation_order(compensation_no, sales_no, shop_id, sku_id, compensation_type, compensation_amount, compensation_status, reason, create_by, create_time, remark) values
('DEMO-CMP-001', 'DEMO-SO-001', @shop_dy, @sku1, 'cash', 10.00, 'approved', '客服体验补偿', 'admin', sysdate(), '演示补偿');
insert into erp_refund_order(refund_no, sales_no, shop_id, sku_id, refund_type, refund_status, refund_amount, return_freight, reason, apply_time, finish_time, create_by, create_time, remark) values
('DEMO-RF-001', 'DEMO-SO-001', @shop_dy, @sku1, 'refund_only', 'processing', 20.00, 0.00, '物流延迟', sysdate(), null, 'admin', sysdate(), '演示退款'),
('DEMO-RF-002', 'DEMO-SO-003', @shop_jd, @sku5, 'return_refund', 'done', 99.00, 8.00, '七天无理由', date_sub(sysdate(), interval 1 day), sysdate(), 'admin', sysdate(), '演示退款');
insert into erp_platform_settlement_match(match_no, settlement_no, sales_no, refund_no, platform_order_no, settlement_amount, order_amount, diff_amount, match_status, create_by, create_time, remark) values
('DEMO-MT-001', 'DEMO-FPS-001', 'DEMO-SO-001', null, 'DY202606290001', 362.80, 387.00, -24.20, 'matched', 'admin', sysdate(), '扣除平台费用'),
('DEMO-MT-002', 'DEMO-FPS-002', 'DEMO-SO-002', null, 'TM202606290002', 183.05, 199.00, -15.95, 'matched', 'admin', sysdate(), '扣除平台费用');
insert into erp_order_profit(sales_no, shop_id, sku_id, channel_code, channel_name, operator_id, operator_name, pay_amount, product_cost, platform_fee, promotion_fee, freight_fee, after_sale_cost, profit_amount, share_rate, share_profit_amount, profit_rate, profit_date, create_time) values
('DEMO-SO-001', @shop_dy, @sku1, 'live', '直播间', 1001, '赵运营', 387.00, 181.00, 18.20, 42.00, 9.00, 0.00, 136.80, 1.0000, 136.80, 0.3535, @today, sysdate()),
('DEMO-SO-002', @shop_tm, @sku3, 'search', '平台搜索', 1002, '钱运营', 199.00, 89.00, 9.95, 18.00, 6.00, 0.00, 76.05, 1.0000, 76.05, 0.3822, @today, sysdate()),
('DEMO-SO-003', @shop_jd, @sku4, 'ad_feed', '信息流投放', 1003, '孙运营', 537.00, 232.00, 21.48, 55.00, 12.00, 0.00, 216.52, 0.9000, 194.87, 0.4032, @yesterday, sysdate());

insert into erp_sku_barcode(sku_id, sku_code, barcode, barcode_type, status, create_by, create_time, remark) values
(@sku1, 'DEMO-SKU-001', '6900000000011', 'inner', '0', 'admin', sysdate(), '演示条码'),
(@sku3, 'DEMO-SKU-003', '6900000000035', 'inner', '0', 'admin', sysdate(), '演示条码'),
(@sku6, 'DEMO-SKU-006', '6900000000066', 'outer', '0', 'admin', sysdate(), '演示条码');
insert into erp_platform_sku_mapping(platform_code, shop_id, platform_product_id, platform_sku_id, platform_sku_name, sku_id, sku_code, split_rule_json, status, create_by, create_time, remark) values
('douyin', @shop_dy, 'DY-P-DEMO-001', 'DEMO-DY-SKU-001', '抖音榨汁杯白色', @sku1, 'DEMO-SKU-001', json_object('qty', 1), '0', 'admin', sysdate(), '演示平台SKU映射'),
('tmall', @shop_tm, 'TM-P-DEMO-003', 'DEMO-TM-SKU-003', '天猫蓝牙耳机标准版', @sku3, 'DEMO-SKU-003', json_object('qty', 1), '0', 'admin', sysdate(), '演示平台SKU映射'),
('jd', @shop_jd, 'JD-P-DEMO-006', 'DEMO-JD-SKU-006', '京东防晒套装', @sku6, 'DEMO-SKU-006', json_object('qty', 1), '0', 'admin', sysdate(), '演示平台SKU映射');
insert into erp_product_bundle(bundle_sku_id, bundle_sku_code, bundle_name, bundle_status, create_by, create_time, remark) values
(@sku6, 'DEMO-SKU-006', '防晒套装-三件套', 'enabled', 'admin', sysdate(), '组合商品演示');
set @bundle := (select bundle_id from erp_product_bundle where bundle_sku_code = 'DEMO-SKU-006');
insert into erp_product_bundle_item(bundle_id, component_sku_id, component_sku_code, component_sku_name, quantity, cost_price) values
(@bundle, @sku5, 'DEMO-SKU-005', '保温杯-钛灰色', 1, 45.00),
(@bundle, @sku1, 'DEMO-SKU-001', '榨汁杯-奶白色', 1, 68.00);
insert into erp_sku_cost_history(sku_id, sku_code, cost_price, effective_date, source_type, source_no, create_by, create_time, remark) values
(@sku1, 'DEMO-SKU-001', 66.00, date_sub(@today, interval 15 day), 'purchase_receipt', 'DEMO-RCV-001', 'admin', sysdate(), '最近入库成本'),
(@sku3, 'DEMO-SKU-003', 90.00, date_sub(@today, interval 10 day), 'purchase_receipt', 'DEMO-RCV-002', 'admin', sysdate(), '最近入库成本'),
(@sku6, 'DEMO-SKU-006', 52.00, date_sub(@today, interval 20 day), 'supplier_quote', 'DEMO-QT-003', 'admin', sysdate(), '组合套装成本');
insert into erp_supplier_quote(quote_no, supplier_id, supplier_name, sku_id, sku_code, quote_price, min_order_qty, effective_date, expire_date, quote_status, create_by, create_time, remark) values
('DEMO-QT-001', @sup_a, '广州源头供应链有限公司', @sku1, 'DEMO-SKU-001', 66.00, 100, date_sub(@today, interval 30 day), date_add(@today, interval 60 day), 'enabled', 'admin', sysdate(), '演示报价'),
('DEMO-QT-002', @sup_b, '深圳优选科技有限公司', @sku3, 'DEMO-SKU-003', 90.00, 80, date_sub(@today, interval 20 day), date_add(@today, interval 45 day), 'enabled', 'admin', sysdate(), '演示报价'),
('DEMO-QT-003', @sup_a, '广州源头供应链有限公司', @sku6, 'DEMO-SKU-006', 52.00, 120, date_sub(@today, interval 20 day), date_add(@today, interval 45 day), 'enabled', 'admin', sysdate(), '演示报价');

insert into erp_settlement_bill(settlement_no, shop_id, shop_name, bill_month, income_amount, refund_amount, platform_fee, ad_cost, freight_fee, net_amount, bill_status, create_by, create_time, remark) values
('DEMO-ESB-001', @shop_dy, '抖音旗舰店', @period, 526.00, 20.00, 31.10, 68.00, 17.00, 389.90, 'checked', 'admin', sysdate(), '演示店铺结算单'),
('DEMO-ESB-002', @shop_tm, '天猫官方店', @period, 497.00, 0.00, 24.85, 48.00, 13.00, 411.15, 'checked', 'admin', sysdate(), '演示店铺结算单');
insert into erp_import_batch(batch_no, import_type, file_name, total_count, success_count, fail_count, batch_status, error_message, create_by, create_time, update_time) values
('DEMO-IMP-001', 'order', 'demo_order_import.xlsx', 120, 118, 2, 'done', '2条缺少平台SKU映射', 'admin', sysdate(), sysdate()),
('DEMO-IMP-002', 'settlement', 'demo_settlement_import.xlsx', 35, 35, 0, 'done', null, 'admin', sysdate(), sysdate());

insert ignore into fin_account_period(period_code, start_date, end_date, close_status, create_by, create_time, remark)
values(@period, str_to_date(concat(@period, '-01'), '%Y-%m-%d'), last_day(str_to_date(concat(@period, '-01'), '%Y-%m-%d')), 'open', 'admin', sysdate(), '演示期间');
insert ignore into fin_account_subject(subject_code, subject_name, subject_type, parent_code, balance_direction, status, create_by, create_time) values
('1002-DEMO', '演示银行存款', 'asset', '1002', 'debit', '0', 'admin', sysdate()),
('1122-DEMO', '演示平台应收', 'asset', '1122', 'debit', '0', 'admin', sysdate()),
('2202-DEMO', '演示供应商应付', 'liability', '2202', 'credit', '0', 'admin', sysdate()),
('6001-DEMO', '演示主营收入', 'income', '6001', 'credit', '0', 'admin', sysdate()),
('6401-DEMO', '演示主营成本', 'expense', '6401', 'debit', '0', 'admin', sysdate());
insert ignore into fin_fee_type(fee_code, fee_name, fee_category, default_subject_code, allocate_rule, status, create_by, create_time, remark) values
('demo_ad_fee', '演示推广费', 'ad', '6401-DEMO', 'operator_channel_day', '0', 'admin', sysdate(), '按运营渠道归集'),
('demo_freight', '演示运费', 'logistics', '6401-DEMO', 'order', '0', 'admin', sysdate(), '按订单归集');
insert into fin_bank_account(account_name, bank_name, bank_no, currency, opening_balance, status, create_by, create_time, remark) values
('演示基本户', '招商银行杭州分行', 'DEMO-BANK-001', 'CNY', 580000.00, '0', 'admin', sysdate(), '演示银行账户');
insert into fin_platform_account(platform_code, platform_name, shop_id, shop_name, account_no, currency, status, create_by, create_time, remark) values
('douyin', '抖音电商', @shop_dy, '抖音旗舰店', 'DEMO-PLAT-DY', 'CNY', '0', 'admin', sysdate(), '演示平台账户'),
('tmall', '天猫', @shop_tm, '天猫官方店', 'DEMO-PLAT-TM', 'CNY', '0', 'admin', sysdate(), '演示平台账户')
on duplicate key update platform_name = values(platform_name);

insert into fin_platform_settlement(settlement_no, platform_code, platform_name, shop_id, shop_name, period_code, bill_start_date, bill_end_date, income_amount, refund_amount, commission_fee, payment_fee, ad_fee, service_fee, freight_fee, other_fee, receivable_amount, received_amount, diff_amount, settlement_status, reconcile_status, source_file, create_by, create_time, remark) values
('DEMO-FPS-001', 'douyin', '抖音电商', @shop_dy, '抖音旗舰店', @period, str_to_date(concat(@period, '-01'), '%Y-%m-%d'), last_day(str_to_date(concat(@period, '-01'), '%Y-%m-%d')), 526.00, 20.00, 31.10, 5.00, 68.00, 2.00, 17.00, 0.00, 382.90, 360.00, 22.90, 'approved', 'matched', 'demo_douyin_settlement.xlsx', 'admin', sysdate(), '演示平台结算'),
('DEMO-FPS-002', 'tmall', '天猫', @shop_tm, '天猫官方店', @period, str_to_date(concat(@period, '-01'), '%Y-%m-%d'), last_day(str_to_date(concat(@period, '-01'), '%Y-%m-%d')), 497.00, 0.00, 24.85, 4.00, 48.00, 2.00, 13.00, 0.00, 405.15, 405.15, 0.00, 'approved', 'matched', 'demo_tmall_settlement.xlsx', 'admin', sysdate(), '演示平台结算');
set @fps1 := (select settlement_id from fin_platform_settlement where settlement_no = 'DEMO-FPS-001');
set @fps2 := (select settlement_id from fin_platform_settlement where settlement_no = 'DEMO-FPS-002');
insert into fin_platform_settlement_item(settlement_id, settlement_no, platform_order_no, order_id, refund_no, item_type, fee_type_code, sku_code, amount, occurred_time, remark) values
(@fps1, 'DEMO-FPS-001', 'DY202606290001', 910001, null, 'income', 'order_income', 'DEMO-SKU-001', 387.00, concat(@today, ' 10:12:00'), '订单收入'),
(@fps1, 'DEMO-FPS-001', 'DY202606290001', 910001, null, 'fee', 'platform_commission', 'DEMO-SKU-001', -18.20, concat(@today, ' 10:12:00'), '平台扣点'),
(@fps2, 'DEMO-FPS-002', 'TM202606290002', 910002, null, 'income', 'order_income', 'DEMO-SKU-003', 199.00, concat(@today, ' 11:05:00'), '订单收入');
insert into fin_receivable_bill(receivable_no, source_type, source_no, counterparty_type, counterparty_id, counterparty_name, shop_id, shop_name, period_code, bill_amount, writeoff_amount, remain_amount, due_date, bill_status, create_by, create_time, remark) values
('DEMO-AR-001', 'platform_settlement', 'DEMO-FPS-001', 'platform', null, '抖音电商', @shop_dy, '抖音旗舰店', @period, 382.90, 360.00, 22.90, date_add(@today, interval 5 day), 'open', 'admin', sysdate(), '演示应收'),
('DEMO-AR-002', 'platform_settlement', 'DEMO-FPS-002', 'platform', null, '天猫', @shop_tm, '天猫官方店', @period, 405.15, 405.15, 0.00, date_add(@today, interval 3 day), 'closed', 'admin', sysdate(), '演示应收');
insert into fin_payable_bill(payable_no, source_type, source_no, counterparty_type, counterparty_id, counterparty_name, period_code, bill_amount, writeoff_amount, remain_amount, due_date, bill_status, create_by, create_time, remark) values
('DEMO-AP-001', 'supplier_reconcile', 'DEMO-SRC-001', 'supplier', @sup_a, '广州源头供应链有限公司', @period, 28204.00, 10000.00, 18204.00, date_add(@today, interval 15 day), 'open', 'admin', sysdate(), '演示应付'),
('DEMO-AP-002', 'supplier_reconcile', 'DEMO-SRC-002', 'supplier', @sup_b, '深圳优选科技有限公司', @period, 34500.00, 0.00, 34500.00, date_add(@today, interval 10 day), 'open', 'admin', sysdate(), '演示应付');
insert into fin_cash_flow(flow_no, flow_type, bank_account_id, platform_account_id, counterparty_name, source_type, source_no, amount, fee_amount, flow_time, business_date, match_status, create_by, create_time, remark) values
('DEMO-CF-001', 'in', null, null, '抖音电商', 'receivable', 'DEMO-AR-001', 360.00, 0.00, concat(@today, ' 16:10:00'), @today, 'matched', 'admin', sysdate(), '平台回款'),
('DEMO-CF-002', 'out', null, null, '广州源头供应链有限公司', 'payable', 'DEMO-AP-001', 10000.00, 5.00, concat(@today, ' 17:20:00'), @today, 'matched', 'admin', sysdate(), '供应商付款');
insert into fin_writeoff_record(writeoff_no, bill_type, bill_no, cash_flow_no, writeoff_amount, writeoff_time, operator_name, create_by, create_time, remark) values
('DEMO-WO-001', 'receivable', 'DEMO-AR-001', 'DEMO-CF-001', 360.00, sysdate(), '财务A', 'admin', sysdate(), '应收核销'),
('DEMO-WO-002', 'payable', 'DEMO-AP-001', 'DEMO-CF-002', 10000.00, sysdate(), '财务A', 'admin', sysdate(), '应付核销');
insert into fin_expense_bill(expense_no, fee_type_code, fee_type_name, shop_id, shop_name, sku_id, sku_code, order_no, supplier_id, supplier_name, operator_id, operator_name, channel_code, channel_name, period_code, expense_amount, tax_amount, total_amount, allocation_dimension, expense_status, occurred_date, create_by, create_time, remark) values
('DEMO-EXP-001', 'demo_ad_fee', '演示推广费', @shop_dy, '抖音旗舰店', @sku1, 'DEMO-SKU-001', '910001', null, null, 1001, '赵运营', 'live', '直播间', @period, 42.00, 2.52, 44.52, 'operator_channel_day', 'approved', @today, 'admin', sysdate(), '直播间推广费用'),
('DEMO-EXP-002', 'demo_freight', '演示运费', @shop_tm, '天猫官方店', @sku3, 'DEMO-SKU-003', '910002', null, null, 1002, '钱运营', 'search', '平台搜索', @period, 6.00, 0.36, 6.36, 'order', 'approved', @today, 'admin', sysdate(), '订单运费');

insert into fin_payment_request(payment_no, source_type, source_no, counterparty_type, counterparty_id, counterparty_name, payable_no, payment_amount, requested_pay_date, payment_status, approve_by, approve_time, create_by, create_time, remark) values
('DEMO-PAY-001', 'payable', 'DEMO-AP-001', 'supplier', @sup_a, '广州源头供应链有限公司', 'DEMO-AP-001', 10000.00, @today, 'approved', '财务主管', sysdate(), 'admin', sysdate(), '供应商首付款'),
('DEMO-PAY-002', 'operator_settlement', 'DEMO-OPS-002', 'employee', 1002, '钱运营', null, 80.00, @today, 'draft', '', null, 'admin', sysdate(), '运营分润付款');
insert into fin_payment_execute(execute_no, payment_no, bank_account_id, cash_flow_no, execute_amount, execute_time, execute_status, operator_name, create_by, create_time, remark) values
('DEMO-PEX-001', 'DEMO-PAY-001', null, 'DEMO-CF-002', 10000.00, sysdate(), 'done', '财务A', 'admin', sysdate(), '已付款');
insert into fin_reimbursement(reimbursement_no, employee_id, employee_name, department_name, reimbursement_title, expense_amount, invoice_amount, reimbursement_status, approve_by, approve_time, create_by, create_time, remark) values
('DEMO-RBM-001', 2001, '运营助理小林', '电商运营部', '直播间样品寄送报销', 286.00, 286.00, 'approved', '赵运营', sysdate(), 'admin', sysdate(), '演示报销');
insert into fin_reimbursement_item(reimbursement_no, fee_type_code, fee_type_name, occurred_date, expense_amount, invoice_no, invoice_file_url, remark) values
('DEMO-RBM-001', 'demo_freight', '演示运费', @yesterday, 86.00, 'INV-DEMO-001', '/demo/invoice-001.pdf', '样品快递'),
('DEMO-RBM-001', 'demo_ad_fee', '演示推广费', @yesterday, 200.00, 'INV-DEMO-002', '/demo/invoice-002.pdf', '素材采购');
insert into fin_contract(contract_no, contract_name, contract_type, counterparty_id, counterparty_name, contract_amount, signed_date, start_date, end_date, archive_url, contract_status, approve_by, approve_time, create_by, create_time, remark) values
('DEMO-CT-001', '广州源头供应链年度采购合同', 'purchase', @sup_a, '广州源头供应链有限公司', 800000.00, date_sub(@today, interval 20 day), date_sub(@today, interval 20 day), date_add(@today, interval 345 day), '/demo/contracts/demo-ct-001.pdf', 'approved', '总经理', sysdate(), 'admin', sysdate(), '演示合同');
insert into fin_contract_plan(contract_no, plan_type, plan_date, plan_amount, actual_amount, plan_status, source_no, create_by, create_time, remark) values
('DEMO-CT-001', 'pay', date_add(@today, interval 15 day), 28204.00, 10000.00, 'partial', 'DEMO-AP-001', 'admin', sysdate(), '首批货款计划'),
('DEMO-CT-001', 'pay', date_add(@today, interval 45 day), 50000.00, 0.00, 'open', null, 'admin', sysdate(), '下月货款计划');

insert into fin_subject_mapping(biz_type, fee_type_code, source_type, debit_subject_code, debit_subject_name, credit_subject_code, credit_subject_name, status, create_by, create_time, remark) values
('platform_settlement', 'order_income', 'settlement', '1122-DEMO', '演示平台应收', '6001-DEMO', '演示主营收入', '0', 'admin', sysdate(), '演示科目映射'),
('purchase_payable', null, 'supplier_reconcile', '6401-DEMO', '演示主营成本', '2202-DEMO', '演示供应商应付', '0', 'admin', sysdate(), '演示科目映射');
insert into fin_voucher_template(template_code, template_name, source_type, template_status, create_by, create_time, remark) values
('DEMO-VT-001', '平台结算自动凭证', 'platform_settlement', 'enabled', 'admin', sysdate(), '演示凭证模板'),
('DEMO-VT-002', '供应商应付自动凭证', 'supplier_reconcile', 'enabled', 'admin', sysdate(), '演示凭证模板');
insert into fin_voucher_template_entry(template_code, entry_seq, subject_code, subject_name, dc, amount_expr, summary_expr) values
('DEMO-VT-001', 1, '1122-DEMO', '演示平台应收', 'debit', 'receivable_amount', '平台结算应收'),
('DEMO-VT-001', 2, '6001-DEMO', '演示主营收入', 'credit', 'income_amount', '平台结算收入'),
('DEMO-VT-002', 1, '6401-DEMO', '演示主营成本', 'debit', 'payable_amount', '供应商对账成本'),
('DEMO-VT-002', 2, '2202-DEMO', '演示供应商应付', 'credit', 'payable_amount', '供应商对账应付');
insert into fin_voucher(voucher_no, period_code, voucher_date, source_type, source_no, summary, debit_amount, credit_amount, voucher_status, create_by, create_time, remark) values
('DEMO-VCH-001', @period, @today, 'platform_settlement', 'DEMO-FPS-001', '抖音平台结算确认收入', 526.00, 526.00, 'posted', 'admin', sysdate(), '演示凭证'),
('DEMO-VCH-002', @period, @today, 'supplier_reconcile', 'DEMO-SRC-001', '供应商对账确认应付', 28204.00, 28204.00, 'draft', 'admin', sysdate(), '演示凭证');
set @vch1 := (select voucher_id from fin_voucher where voucher_no = 'DEMO-VCH-001');
set @vch2 := (select voucher_id from fin_voucher where voucher_no = 'DEMO-VCH-002');
insert into fin_voucher_entry(voucher_id, voucher_no, entry_seq, subject_code, subject_name, direction, amount, shop_id, sku_id, counterparty_name, summary) values
(@vch1, 'DEMO-VCH-001', 1, '1122-DEMO', '演示平台应收', 'debit', 526.00, @shop_dy, null, '抖音电商', '确认应收'),
(@vch1, 'DEMO-VCH-001', 2, '6001-DEMO', '演示主营收入', 'credit', 526.00, @shop_dy, null, '抖音电商', '确认收入'),
(@vch2, 'DEMO-VCH-002', 1, '6401-DEMO', '演示主营成本', 'debit', 28204.00, null, null, '广州源头供应链有限公司', '确认成本'),
(@vch2, 'DEMO-VCH-002', 2, '2202-DEMO', '演示供应商应付', 'credit', 28204.00, null, null, '广州源头供应链有限公司', '确认应付');
insert into fin_ledger_entry(voucher_no, period_code, voucher_date, subject_code, subject_name, dc, amount, counterparty_name, source_type, source_no, create_time) values
('DEMO-VCH-001', @period, @today, '1122-DEMO', '演示平台应收', 'debit', 526.00, '抖音电商', 'platform_settlement', 'DEMO-FPS-001', sysdate()),
('DEMO-VCH-001', @period, @today, '6001-DEMO', '演示主营收入', 'credit', 526.00, '抖音电商', 'platform_settlement', 'DEMO-FPS-001', sysdate()),
('DEMO-VCH-002', @period, @today, '6401-DEMO', '演示主营成本', 'debit', 28204.00, '广州源头供应链有限公司', 'supplier_reconcile', 'DEMO-SRC-001', sysdate()),
('DEMO-VCH-002', @period, @today, '2202-DEMO', '演示供应商应付', 'credit', 28204.00, '广州源头供应链有限公司', 'supplier_reconcile', 'DEMO-SRC-001', sysdate());
insert into fin_trial_balance(period_code, subject_code, subject_name, begin_debit, begin_credit, current_debit, current_credit, end_debit, end_credit, create_time) values
(@period, '1122-DEMO', '演示平台应收', 0.00, 0.00, 526.00, 0.00, 526.00, 0.00, sysdate()),
(@period, '6001-DEMO', '演示主营收入', 0.00, 0.00, 0.00, 526.00, 0.00, 526.00, sysdate()),
(@period, '2202-DEMO', '演示供应商应付', 0.00, 0.00, 0.00, 28204.00, 0.00, 28204.00, sysdate());
insert into fin_profit_statement(period_code, revenue_amount, cost_amount, gross_profit, expense_amount, net_profit, create_time) values
(@period, 1527.00, 721.00, 806.00, 338.00, 468.00, sysdate());
insert into fin_reconciliation_task(task_no, reconcile_type, period_code, shop_id, shop_name, task_status, total_count, diff_count, start_time, finish_time, create_by, create_time, remark) values
('DEMO-REC-001', 'settlement', @period, @shop_dy, '抖音旗舰店', 'done', 36, 2, date_sub(sysdate(), interval 2 hour), date_sub(sysdate(), interval 1 hour), 'admin', sysdate(), '演示对账任务');
set @rec1 := (select task_id from fin_reconciliation_task where task_no = 'DEMO-REC-001');
insert into fin_reconciliation_diff(task_id, diff_no, diff_type, source_type, source_no, expected_amount, actual_amount, diff_amount, diff_status, handle_result, create_by, create_time, remark) values
(@rec1, 'DEMO-DIFF-001', 'amount_diff', 'platform_settlement', 'DEMO-FPS-001', 382.90, 360.00, -22.90, 'open', '待平台确认差额', 'admin', sysdate(), '演示差异'),
(@rec1, 'DEMO-DIFF-002', 'missing_order', 'order', 'DY202606290099', 129.00, 0.00, -129.00, 'processing', '疑似退款订单', 'admin', sysdate(), '演示差异');
insert into fin_period_close(period_code, close_scope, check_status, close_status, order_complete, settlement_complete, bank_complete, voucher_complete, close_by, close_time, create_by, create_time, remark) values
(@period, 'demo_company', 'passed', 'open', '1', '1', '1', '0', '', null, 'admin', sysdate(), '演示期末结账检查');
