package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.ErpFlowMapper;
import com.xinke.erp.mapper.FinanceCoreMapper;
import com.xinke.erp.service.IErpFlowService;

@Service
public class ErpFlowServiceImpl implements IErpFlowService
{
    private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private static final Map<String, FlowMeta> FLOW_META = new LinkedHashMap<>();

    private static final Set<String> READ_ONLY_FLOW_TYPES = Set.of(
            "inventory-ageing",
            "order-profit",
            "operator-profit-day",
            "ledger-entry",
            "trial-balance",
            "profit-statement"
    );

    static
    {
        register("purchase-request", "erp_purchase_request", "request_id", "request_no", "request_status", "PR",
                "request_no", "request_title", "requester_name", "department_name", "supplier_id", "supplier_name",
                "warehouse_id", "expected_date", "total_amount", "request_status", "approve_by", "approve_time",
                "create_by", "create_time", "update_by", "update_time", "remark");
        register("purchase-receipt", "erp_purchase_receipt", "receipt_id", "receipt_no", "receipt_status", "RCV",
                "receipt_no", "purchase_no", "supplier_id", "supplier_name", "warehouse_id", "warehouse_name",
                "receipt_date", "total_qty", "total_amount", "receipt_status", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("purchase-return", "erp_purchase_return", "return_id", "return_no", "return_status", "PRT",
                "return_no", "purchase_no", "receipt_no", "supplier_id", "supplier_name", "warehouse_id",
                "return_date", "total_qty", "total_amount", "return_reason", "return_status", "create_by",
                "create_time", "update_by", "update_time", "remark");
        register("supplier-reconcile", "erp_supplier_reconcile", "reconcile_id", "reconcile_no", "reconcile_status", "SRC",
                "reconcile_no", "supplier_id", "supplier_name", "period_code", "purchase_amount", "return_amount",
                "payable_amount", "diff_amount", "reconcile_status", "create_by", "create_time", "update_by",
                "update_time", "remark");
        register("inventory-transfer", "erp_inventory_transfer", "transfer_id", "transfer_no", "transfer_status", "TRF",
                "transfer_no", "from_warehouse_id", "from_warehouse_name", "to_warehouse_id", "to_warehouse_name",
                "transfer_date", "total_qty", "transfer_status", "create_by", "create_time", "update_by",
                "update_time", "remark");
        register("inventory-stocktake", "erp_inventory_stocktake", "stocktake_id", "stocktake_no", "stocktake_status", "STK",
                "stocktake_no", "warehouse_id", "warehouse_name", "stocktake_date", "total_sku_count",
                "profit_loss_amount", "stocktake_status", "create_by", "create_time", "update_by", "update_time", "remark");
        register("inventory-loss", "erp_inventory_loss", "loss_id", "loss_no", "loss_status", "LOS",
                "loss_no", "warehouse_id", "sku_id", "sku_code", "sku_name", "quantity", "cost_price",
                "loss_amount", "loss_reason", "loss_status", "create_by", "create_time", "update_by", "update_time", "remark");
        register("inventory-warning", "erp_inventory_warning_rule", "rule_id", "rule_id", "status", "IWR",
                "warehouse_id", "sku_id", "sku_code", "min_qty", "max_qty", "warning_level", "status",
                "create_by", "create_time", "update_by", "update_time", "remark");
        register("inventory-ageing", "erp_inventory_ageing_snapshot", "snapshot_id", "snapshot_id", "", "AGE",
                "snapshot_date", "warehouse_id", "sku_id", "sku_code", "sku_name", "qty_0_30", "qty_31_60",
                "qty_61_90", "qty_90_plus", "amount_90_plus", "create_time");
        register("shipment", "erp_sales_shipment", "shipment_id", "shipment_no", "shipment_status", "SHP",
                "shipment_no", "sales_no", "warehouse_id", "logistics_company", "logistics_no", "shipped_time",
                "shipment_status", "create_by", "create_time", "update_by", "update_time", "remark");
        register("after-sale", "erp_after_sale_order", "after_sale_id", "after_sale_no", "after_sale_status", "AS",
                "after_sale_no", "sales_no", "refund_no", "after_sale_type", "after_sale_status",
                "customer_name", "amount", "reason", "create_by", "create_time", "update_by", "update_time", "remark");
        register("compensation", "erp_compensation_order", "compensation_id", "compensation_no", "compensation_status", "CMP",
                "compensation_no", "sales_no", "shop_id", "sku_id", "compensation_type", "compensation_amount",
                "compensation_status", "reason", "create_by", "create_time", "update_by", "update_time", "remark");
        register("settlement-match", "erp_platform_settlement_match", "match_id", "match_no", "match_status", "MT",
                "match_no", "settlement_no", "sales_no", "refund_no", "platform_order_no", "settlement_amount",
                "order_amount", "diff_amount", "match_status", "create_by", "create_time", "update_by", "update_time", "remark");
        register("order-profit", "erp_order_profit", "profit_id", "sales_no", "", "OP",
                "sales_no", "shop_id", "sku_id", "channel_code", "operator_id", "operator_name", "pay_amount",
                "product_cost", "platform_fee", "promotion_fee", "freight_fee", "after_sale_cost", "profit_amount",
                "share_rate", "share_profit_amount", "profit_rate", "profit_date", "create_time");
        register("operator-attribution", "erp_operator_attribution_rule", "rule_id", "rule_code", "status", "OAR",
                "rule_code", "rule_name", "shop_id", "shop_name", "product_id", "product_name", "sku_id",
                "sku_code", "sku_name", "channel_code", "channel_name", "operator_id", "operator_name",
                "share_rate", "priority", "effective_start", "effective_end", "status", "create_by",
                "create_time", "update_by", "update_time", "remark");
        register("operator-profit-day", "fin_operator_profit_day", "summary_id", "summary_id", "", "OPD",
                "dt", "period_code", "shop_id", "shop_name", "operator_id", "operator_name", "channel_code",
                "channel_name", "order_count", "gmv", "net_amount", "product_cost", "platform_fee", "ad_cost",
                "freight_fee", "after_sale_cost", "profit_amount", "share_profit_amount", "settlement_amount",
                "create_time", "update_time");
        register("operator-settlement", "fin_operator_profit_settlement", "settlement_id", "settlement_no", "settlement_status", "OPS",
                "settlement_no", "period_code", "operator_id", "operator_name", "shop_id", "shop_name",
                "channel_code", "channel_name", "profit_amount", "share_profit_amount", "paid_amount",
                "remain_amount", "settlement_status", "approve_by", "approve_time", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("sku-barcode", "erp_sku_barcode", "barcode_id", "barcode", "status", "BC",
                "sku_id", "sku_code", "barcode", "barcode_type", "status", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("platform-sku", "erp_platform_sku_mapping", "mapping_id", "platform_sku_id", "status", "PSKU",
                "platform_code", "shop_id", "platform_product_id", "platform_sku_id", "platform_sku_name",
                "sku_id", "sku_code", "split_rule_json", "status", "create_by", "create_time", "update_by",
                "update_time", "remark");
        register("bundle", "erp_product_bundle", "bundle_id", "bundle_sku_code", "bundle_status", "BDL",
                "bundle_sku_id", "bundle_sku_code", "bundle_name", "bundle_status", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("cost-history", "erp_sku_cost_history", "cost_id", "cost_id", "", "COST",
                "sku_id", "sku_code", "cost_price", "effective_date", "source_type", "source_no",
                "create_by", "create_time", "remark");
        register("supplier-quote", "erp_supplier_quote", "quote_id", "quote_no", "quote_status", "QT",
                "quote_no", "supplier_id", "supplier_name", "sku_id", "sku_code", "quote_price",
                "min_order_qty", "effective_date", "expire_date", "quote_status", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("payment-request", "fin_payment_request", "payment_id", "payment_no", "payment_status", "PAY",
                "payment_no", "source_type", "source_no", "counterparty_type", "counterparty_id", "counterparty_name",
                "payable_no", "payment_amount", "requested_pay_date", "payment_status", "approve_by", "approve_time",
                "create_by", "create_time", "update_by", "update_time", "remark");
        register("payment-execute", "fin_payment_execute", "execute_id", "execute_no", "execute_status", "PEX",
                "execute_no", "payment_no", "bank_account_id", "cash_flow_no", "execute_amount", "execute_time",
                "execute_status", "operator_name", "create_by", "create_time", "remark");
        register("reimbursement", "fin_reimbursement", "reimbursement_id", "reimbursement_no", "reimbursement_status", "RBM",
                "reimbursement_no", "employee_id", "employee_name", "department_name", "reimbursement_title",
                "expense_amount", "invoice_amount", "reimbursement_status", "approve_by", "approve_time",
                "create_by", "create_time", "update_by", "update_time", "remark");
        register("contract", "fin_contract", "contract_id", "contract_no", "contract_status", "CT",
                "contract_no", "contract_name", "contract_type", "counterparty_id", "counterparty_name",
                "contract_amount", "signed_date", "start_date", "end_date", "archive_url", "contract_status",
                "approve_by", "approve_time", "create_by", "create_time", "update_by", "update_time", "remark");
        register("contract-plan", "fin_contract_plan", "plan_id", "plan_id", "plan_status", "CP",
                "contract_no", "plan_type", "plan_date", "plan_amount", "actual_amount", "plan_status",
                "source_no", "create_by", "create_time", "update_by", "update_time", "remark");
        register("subject-mapping", "fin_subject_mapping", "mapping_id", "mapping_id", "status", "SM",
                "biz_type", "fee_type_code", "source_type", "debit_subject_code", "debit_subject_name",
                "credit_subject_code", "credit_subject_name", "status", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("voucher-template", "fin_voucher_template", "template_id", "template_code", "template_status", "VT",
                "template_code", "template_name", "source_type", "template_status", "create_by", "create_time",
                "update_by", "update_time", "remark");
        register("ledger-entry", "fin_ledger_entry", "ledger_id", "voucher_no", "", "LE",
                "voucher_no", "period_code", "voucher_date", "subject_code", "subject_name", "dc", "amount",
                "counterparty_name", "source_type", "source_no", "create_time");
        register("trial-balance", "fin_trial_balance", "trial_id", "subject_code", "", "TB",
                "period_code", "subject_code", "subject_name", "begin_debit", "begin_credit", "current_debit",
                "current_credit", "end_debit", "end_credit", "create_time");
        register("profit-statement", "fin_profit_statement", "statement_id", "period_code", "", "FS",
                "period_code", "revenue_amount", "cost_amount", "gross_profit", "expense_amount", "net_profit", "create_time");
    }

    @Autowired
    private ErpFlowMapper erpFlowMapper;

    @Autowired
    private FinanceCoreMapper financeCoreMapper;

    @Override
    public List<Map<String, Object>> list(String flowType, Map<String, Object> query)
    {
        FlowMeta meta = meta(flowType);
        return erpFlowMapper.selectFlowList(meta.tableName, meta.noColumn, meta.statusColumn, query);
    }

    @Override
    public Map<String, Object> getInfo(String flowType, Long id)
    {
        FlowMeta meta = meta(flowType);
        return erpFlowMapper.selectFlowById(meta.tableName, meta.pkColumn, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int add(String flowType, Map<String, Object> form, String username)
    {
        assertWritable(flowType);
        FlowMeta meta = meta(flowType);
        Map<String, Object> data = sanitize(meta, form);
        validateFlowWarehouses(flowType, data);
        if (!data.containsKey(meta.noColumn) && !meta.pkColumn.equals(meta.noColumn))
        {
            data.put(meta.noColumn, nextNo(meta.noPrefix));
        }
        defaultAudit(meta, data, username, true);
        defaultStatus(meta, data);
        assertPeriodOpen(data.get("period_code"));
        return erpFlowMapper.insertFlow(meta.tableName, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int edit(String flowType, Long id, Map<String, Object> form, String username)
    {
        assertWritable(flowType);
        FlowMeta meta = meta(flowType);
        Map<String, Object> data = sanitize(meta, form);
        validateFlowWarehouses(flowType, data);
        data.remove(meta.pkColumn);
        if (meta.columns.contains("update_by"))
        {
            data.put("update_by", username);
        }
        if (meta.columns.contains("update_time"))
        {
            data.put("update_time", now());
        }
        assertRowPeriodOpen(meta, id);
        assertPeriodOpen(data.get("period_code"));
        return erpFlowMapper.updateFlow(meta.tableName, meta.pkColumn, id, data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int remove(String flowType, Long[] ids)
    {
        assertWritable(flowType);
        FlowMeta meta = meta(flowType);
        if (ids != null && meta.columns.contains("period_code"))
        {
            for (Long id : ids)
            {
                assertRowPeriodOpen(meta, id);
            }
        }
        return erpFlowMapper.deleteFlowByIds(meta.tableName, meta.pkColumn, ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approve(String flowType, String bizNo, String username)
    {
        FlowMeta meta = meta(flowType);
        if (isBlank(meta.statusColumn))
        {
            throw new ServiceException("Current flow does not support approval");
        }
        if (meta.columns.contains("period_code"))
        {
            Map<String, Object> row = erpFlowMapper.selectFlowByNo(meta.tableName, meta.noColumn, bizNo);
            if (row != null)
            {
                assertPeriodOpen(row.get("period_code"));
            }
        }
        return erpFlowMapper.updateFlowStatus(meta.tableName, meta.noColumn, meta.statusColumn, bizNo, "approved", username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createInventoryIn(Map<String, Object> form, String username)
    {
        return applyInventory(form, "in", 1, username, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createInventoryOut(Map<String, Object> form, String username)
    {
        return applyInventory(form, "out", -1, username, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createInventoryTransfer(Map<String, Object> form, String username)
    {
        Map<String, Object> out = new LinkedHashMap<>(form);
        out.put("warehouseId", form.get("fromWarehouseId"));
        out.put("sourceType", "transfer");
        out.put("sourceNo", defaultText(form.get("transferNo"), nextNo("TRF")));
        applyInventory(out, "transfer_out", -1, username, false);

        Map<String, Object> in = new LinkedHashMap<>(form);
        in.put("warehouseId", form.get("toWarehouseId"));
        in.put("sourceType", "transfer");
        in.put("sourceNo", out.get("sourceNo"));
        return applyInventory(in, "transfer_in", 1, username, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createInventoryLoss(Map<String, Object> form, String username)
    {
        return applyInventory(form, "loss", -1, username, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generatePayableFromSupplierReconcile(String reconcileNo, String username)
    {
        FlowMeta meta = meta("supplier-reconcile");
        Map<String, Object> reconcile = erpFlowMapper.selectFlowByNo(meta.tableName, meta.noColumn, reconcileNo);
        if (reconcile == null)
        {
            throw new ServiceException("Supplier reconciliation not found");
        }
        assertPeriodOpen(reconcile.get("period_code"));
        String payableNo = "AP-" + reconcileNo;
        if (financeCoreMapper.selectPayableByNo(payableNo) != null)
        {
            return 1;
        }
        BigDecimal amount = decimal(reconcile.get("payable_amount"));
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("Payable amount must be greater than zero");
        }
        Map<String, Object> payable = new LinkedHashMap<>();
        payable.put("payableNo", payableNo);
        payable.put("sourceType", "supplier_reconcile");
        payable.put("sourceNo", reconcileNo);
        payable.put("counterpartyType", "supplier");
        payable.put("counterpartyId", reconcile.get("supplier_id"));
        payable.put("counterpartyName", reconcile.get("supplier_name"));
        payable.put("periodCode", reconcile.get("period_code"));
        payable.put("billAmount", amount);
        payable.put("writeoffAmount", BigDecimal.ZERO);
        payable.put("remainAmount", amount);
        payable.put("billStatus", "open");
        payable.put("createBy", username);
        return financeCoreMapper.insertPayable(payable);
    }

    private int applyInventory(Map<String, Object> form, String movementType, int direction, String username, boolean defective)
    {
        Long warehouseId = longValue(form.get("warehouseId"));
        Long skuId = longValue(form.get("skuId"));
        int quantity = intValue(form.get("quantity"));
        if (warehouseId == null || skuId == null || quantity <= 0)
        {
            throw new ServiceException("warehouseId, skuId and quantity are required");
        }

        Map<String, Object> warehousePolicy = erpFlowMapper.selectWarehouseInventoryPolicy(warehouseId);
        if (warehousePolicy == null)
        {
            throw new ServiceException("Warehouse does not exist");
        }
        if (!"0".equals(String.valueOf(warehousePolicy.get("status"))))
        {
            throw new ServiceException("Warehouse is disabled");
        }

        Map<String, Object> balance = erpFlowMapper.selectInventoryBalance(warehouseId, skuId);
        int beforeQty = balance == null ? 0 : intValue(balance.get("available_qty"));
        int beforeDefectiveQty = balance == null ? 0 : intValue(balance.get("defective_qty"));
        int afterQty = beforeQty + quantity * direction;
        if (afterQty < 0 && !"1".equals(String.valueOf(warehousePolicy.get("allow_negative_stock"))))
        {
            throw new ServiceException("Insufficient inventory");
        }

        BigDecimal existingCost = balance == null ? BigDecimal.ZERO : decimal(balance.get("cost_price"));
        BigDecimal transactionCost = decimal(defaultValue(form.get("costPrice"), existingCost));
        BigDecimal balanceCost = existingCost;
        if (direction > 0)
        {
            if (beforeQty > 0 && afterQty > 0)
            {
                balanceCost = existingCost.multiply(BigDecimal.valueOf(beforeQty))
                        .add(transactionCost.multiply(BigDecimal.valueOf(quantity)))
                        .divide(BigDecimal.valueOf(afterQty), 2, RoundingMode.HALF_UP);
            }
            else
            {
                balanceCost = transactionCost.setScale(2, RoundingMode.HALF_UP);
            }
        }
        BigDecimal movementCost = direction > 0 ? transactionCost : balanceCost;
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("warehouseId", warehouseId);
        data.put("warehouseName", defaultValue(form.get("warehouseName"), warehousePolicy.get("warehouse_name")));
        data.put("skuId", skuId);
        data.put("skuCode", form.get("skuCode"));
        data.put("skuName", form.get("skuName"));
        data.put("availableQty", afterQty);
        data.put("defectiveQty", defective ? beforeDefectiveQty + quantity : beforeDefectiveQty);
        data.put("safetyQty", intValue(form.get("safetyQty")));
        data.put("costPrice", balanceCost);
        if (balance == null)
        {
            erpFlowMapper.insertInventoryBalance(data);
        }
        else
        {
            erpFlowMapper.updateInventoryBalance(data);
        }

        Map<String, Object> movement = new LinkedHashMap<>();
        movement.put("movementNo", nextNo("MV"));
        movement.put("warehouseId", warehouseId);
        movement.put("skuId", skuId);
        movement.put("movementType", movementType);
        movement.put("sourceType", defaultText(form.get("sourceType"), movementType));
        movement.put("sourceNo", defaultText(form.get("sourceNo"), movement.get("movementNo")));
        movement.put("quantity", quantity * direction);
        movement.put("beforeQty", beforeQty);
        movement.put("afterQty", afterQty);
        movement.put("costPrice", movementCost);
        movement.put("operatorName", username);
        movement.put("createBy", username);
        movement.put("remark", form.get("remark"));
        erpFlowMapper.insertInventoryMovement(movement);

        if (direction > 0)
        {
            Map<String, Object> layer = new LinkedHashMap<>();
            layer.put("warehouseId", warehouseId);
            layer.put("skuId", skuId);
            layer.put("batchNo", form.get("batchNo"));
            layer.put("sourceType", movement.get("sourceType"));
            layer.put("sourceNo", movement.get("sourceNo"));
            layer.put("remainQty", quantity);
            layer.put("costPrice", transactionCost);
            layer.put("inboundDate", defaultText(form.get("inboundDate"), LocalDate.now().toString()));
            erpFlowMapper.insertCostLayer(layer);
        }
        return 1;
    }

    private void validateFlowWarehouses(String flowType, Map<String, Object> data)
    {
        Map<String, Object> warehouse = validateWarehouseField(data, "warehouse_id", "warehouse_name");
        Map<String, Object> fromWarehouse = validateWarehouseField(data, "from_warehouse_id", "from_warehouse_name");
        Map<String, Object> toWarehouse = validateWarehouseField(data, "to_warehouse_id", "to_warehouse_name");

        if (("purchase-request".equals(flowType) || "purchase-receipt".equals(flowType)) && warehouse != null)
        {
            String usage = String.valueOf(warehouse.get("warehouse_usage"));
            if ("return".equals(usage) || "defective".equals(usage))
            {
                throw new ServiceException("采购业务不能直接进入退货仓或残次仓");
            }
        }
        if ("inventory-stocktake".equals(flowType) && warehouse != null && isCloudWarehouse(warehouse))
        {
            throw new ServiceException("云仓请使用库存同步对账，不能发起实体盘点单");
        }
        if ("inventory-transfer".equals(flowType) && fromWarehouse != null && toWarehouse != null
                && String.valueOf(fromWarehouse.get("warehouse_id")).equals(String.valueOf(toWarehouse.get("warehouse_id"))))
        {
            throw new ServiceException("调出仓和调入仓不能相同");
        }
    }

    private Map<String, Object> validateWarehouseField(Map<String, Object> data, String idKey, String nameKey)
    {
        Long warehouseId = longValue(data.get(idKey));
        if (warehouseId == null)
        {
            return null;
        }
        Map<String, Object> warehouse = erpFlowMapper.selectWarehouseInventoryPolicy(warehouseId);
        if (warehouse == null)
        {
            throw new ServiceException("仓库不存在：" + warehouseId);
        }
        if (!"0".equals(String.valueOf(warehouse.get("status"))))
        {
            throw new ServiceException("仓库已停用：" + warehouse.get("warehouse_name"));
        }
        if (data.containsKey(nameKey))
        {
            data.put(nameKey, warehouse.get("warehouse_name"));
        }
        return warehouse;
    }

    private boolean isCloudWarehouse(Map<String, Object> warehouse)
    {
        String warehouseType = String.valueOf(warehouse.get("warehouse_type"));
        return "cloud".equals(warehouseType) || "third_party".equals(warehouseType);
    }

    private static void register(String flowType, String tableName, String pkColumn, String noColumn, String statusColumn,
                                 String noPrefix, String... columns)
    {
        FLOW_META.put(flowType, new FlowMeta(tableName, pkColumn, noColumn, statusColumn, noPrefix,
                Arrays.stream(columns).collect(Collectors.toSet())));
    }

    private FlowMeta meta(String flowType)
    {
        FlowMeta meta = FLOW_META.get(flowType);
        if (meta == null)
        {
            throw new ServiceException("Unsupported flow type: " + flowType);
        }
        return meta;
    }

    private void assertWritable(String flowType)
    {
        if (READ_ONLY_FLOW_TYPES.contains(flowType))
        {
            throw new ServiceException("Current flow is a read-only report and cannot be modified");
        }
    }

    private void assertPeriodOpen(Object periodCode)
    {
        if (isBlank(periodCode))
        {
            return;
        }
        Map<String, Object> close = financeCoreMapper.selectPeriodCloseByCode(String.valueOf(periodCode).trim(), "company");
        if (close != null && "closed".equals(String.valueOf(close.get("closeStatus")).trim()))
        {
            throw new ServiceException("当前会计期间已结账，不能继续修改：" + periodCode);
        }
    }

    private void assertRowPeriodOpen(FlowMeta meta, Long id)
    {
        if (id == null || !meta.columns.contains("period_code"))
        {
            return;
        }
        Map<String, Object> row = erpFlowMapper.selectFlowById(meta.tableName, meta.pkColumn, id);
        if (row != null)
        {
            assertPeriodOpen(row.get("period_code"));
        }
    }

    private Map<String, Object> sanitize(FlowMeta meta, Map<String, Object> form)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        form.forEach((key, value) -> {
            String column = camelToUnderline(key);
            if (meta.columns.contains(column) && value != null)
            {
                data.put(column, value);
            }
        });
        if (data.isEmpty())
        {
            throw new ServiceException("No valid fields to save");
        }
        return data;
    }

    private void defaultAudit(FlowMeta meta, Map<String, Object> data, String username, boolean create)
    {
        if (create)
        {
            if (meta.columns.contains("create_by"))
            {
                data.putIfAbsent("create_by", username);
            }
            if (meta.columns.contains("create_time"))
            {
                data.putIfAbsent("create_time", now());
            }
        }
        if (meta.columns.contains("update_by"))
        {
            data.putIfAbsent("update_by", username);
        }
        if (meta.columns.contains("update_time"))
        {
            data.putIfAbsent("update_time", now());
        }
    }

    private void defaultStatus(FlowMeta meta, Map<String, Object> data)
    {
        if (!isBlank(meta.statusColumn))
        {
            data.putIfAbsent(meta.statusColumn, "draft");
        }
    }

    private static String nextNo(String prefix)
    {
        return prefix + LocalDateTime.now().format(NO_TIME) + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private static String now()
    {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static String camelToUnderline(String text)
    {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++)
        {
            char c = text.charAt(i);
            if (Character.isUpperCase(c))
            {
                sb.append('_').append(Character.toLowerCase(c));
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isBlank(Object value)
    {
        return value == null || value.toString().trim().isEmpty();
    }

    private static String defaultText(Object value, Object defaultValue)
    {
        return isBlank(value) ? String.valueOf(defaultValue) : String.valueOf(value);
    }

    private static Object defaultValue(Object value, Object defaultValue)
    {
        return value == null ? defaultValue : value;
    }

    private static BigDecimal decimal(Object value)
    {
        if (value == null || value.toString().trim().isEmpty())
        {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString());
    }

    private static Integer intValue(Object value)
    {
        if (value == null || value.toString().trim().isEmpty())
        {
            return 0;
        }
        return new BigDecimal(value.toString()).intValue();
    }

    private static Long longValue(Object value)
    {
        if (value == null || value.toString().trim().isEmpty())
        {
            return null;
        }
        return new BigDecimal(value.toString()).longValue();
    }

    private static class FlowMeta
    {
        private final String tableName;
        private final String pkColumn;
        private final String noColumn;
        private final String statusColumn;
        private final String noPrefix;
        private final Set<String> columns;

        private FlowMeta(String tableName, String pkColumn, String noColumn, String statusColumn, String noPrefix, Set<String> columns)
        {
            this.tableName = tableName;
            this.pkColumn = pkColumn;
            this.noColumn = noColumn;
            this.statusColumn = statusColumn;
            this.noPrefix = noPrefix;
            this.columns = columns;
        }
    }
}
