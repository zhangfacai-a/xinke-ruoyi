-- Database cleanup for xinkeerp.
-- Run after a full mysqldump backup. Review the DROP TABLE section before production execution.

-- 1) Stop storing Douyin avatar URLs.
-- The application no longer writes or reads these columns.
alter table dy_viewer drop column avatar;
alter table dy_viewer_daily_lead drop column avatar;

-- 2) Tables not referenced by current runtime code.
-- Keep Quartz QRTZ_* tables even though they are framework-managed and not directly referenced in source.
drop table if exists fin_voucher_template_entry;
drop table if exists fin_reimbursement_item;
drop table if exists fin_platform_settlement_item;
drop table if exists fin_invoice;
drop table if exists fin_cost_ledger;
drop table if exists erp_sales_shipment_item;
drop table if exists erp_sales_order_item;
drop table if exists erp_sales_order;
drop table if exists erp_settlement_bill;
drop table if exists erp_refund_order;
drop table if exists erp_purchase_return_item;
drop table if exists erp_purchase_request_item;
drop table if exists erp_purchase_receipt_item;
drop table if exists erp_purchase_order_item;
drop table if exists erp_product_bundle_item;
drop table if exists erp_inventory_transfer_item;
drop table if exists erp_inventory_stocktake_item;
drop table if exists erp_import_batch;
drop table if exists erp_customer;
