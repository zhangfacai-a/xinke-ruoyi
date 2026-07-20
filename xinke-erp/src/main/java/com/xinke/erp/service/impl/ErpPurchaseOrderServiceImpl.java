package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.ErpProductSku;
import com.xinke.erp.domain.ErpPurchaseOrder;
import com.xinke.erp.domain.ErpPurchaseOrderItem;
import com.xinke.erp.domain.ErpPurchaseReceiveItem;
import com.xinke.erp.domain.ErpPurchaseReceiveRequest;
import com.xinke.erp.domain.ErpSupplier;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.mapper.ErpProductSkuMapper;
import com.xinke.erp.mapper.ErpPurchaseOrderMapper;
import com.xinke.erp.mapper.ErpSupplierMapper;
import com.xinke.erp.mapper.ErpWarehouseMapper;
import com.xinke.erp.service.IErpPurchaseOrderService;
import com.xinke.erp.service.IErpFlowService;

@Service
public class ErpPurchaseOrderServiceImpl implements IErpPurchaseOrderService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final Map<String, Set<String>> STATUS_TRANSITIONS = new HashMap<>();

    static
    {
        STATUS_TRANSITIONS.put("draft", Set.of("submitted", "closed"));
        STATUS_TRANSITIONS.put("submitted", Set.of("draft", "approved", "closed"));
        STATUS_TRANSITIONS.put("approved", Set.of("receiving", "closed"));
        STATUS_TRANSITIONS.put("processing", Set.of("receiving", "completed", "closed"));
        STATUS_TRANSITIONS.put("receiving", Set.of("completed", "closed"));
        STATUS_TRANSITIONS.put("done", Set.of("closed"));
    }

    @Autowired
    private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Autowired
    private ErpWarehouseMapper warehouseMapper;
    @Autowired
    private ErpSupplierMapper supplierMapper;
    @Autowired
    private ErpProductSkuMapper skuMapper;
    @Autowired
    private IErpFlowService flowService;

    @Override
    public List<ErpPurchaseOrder> selectPurchaseOrderList(ErpPurchaseOrder purchaseOrder)
    {
        return purchaseOrderMapper.selectPurchaseOrderList(purchaseOrder);
    }

    @Override
    public ErpPurchaseOrder selectPurchaseOrderById(Long purchaseId)
    {
        ErpPurchaseOrder order = requireOrder(purchaseId);
        order.setItems(purchaseOrderMapper.selectPurchaseOrderItemsByPurchaseId(purchaseId));
        return order;
    }

    @Override
    public List<Map<String, Object>> selectSkuPurchaseHistory(Long skuId, Integer limit)
    {
        if (skuId == null)
        {
            throw new ServiceException("SKU 不能为空");
        }
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        return purchaseOrderMapper.selectSkuPurchaseHistory(skuId, safeLimit);
    }

    @Override
    public Map<String, Object> selectPurchaseOrderSummary()
    {
        return purchaseOrderMapper.selectPurchaseOrderSummary();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertPurchaseOrder(ErpPurchaseOrder purchaseOrder)
    {
        defaultPurchaseDate(purchaseOrder);
        validateMasterData(purchaseOrder);
        purchaseOrder.setPurchaseStatus("draft");
        calculateOrder(purchaseOrder, true);
        int rows = purchaseOrderMapper.insertPurchaseOrder(purchaseOrder);
        bindItems(purchaseOrder);
        purchaseOrderMapper.batchInsertPurchaseOrderItems(purchaseOrder.getItems());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePurchaseOrder(ErpPurchaseOrder purchaseOrder)
    {
        ErpPurchaseOrder current = requireOrder(purchaseOrder.getPurchaseId());
        if (!"draft".equals(current.getPurchaseStatus()))
        {
            throw new ServiceException("只有草稿采购单可以修改，请先撤回审批");
        }
        defaultPurchaseDate(purchaseOrder);
        validateMasterData(purchaseOrder);
        calculateOrder(purchaseOrder, true);
        int rows = purchaseOrderMapper.updatePurchaseOrder(purchaseOrder);
        if (rows != 1)
        {
            throw new ServiceException("采购单状态已变化，请刷新后重试");
        }
        purchaseOrderMapper.deletePurchaseOrderItemsByPurchaseId(purchaseOrder.getPurchaseId());
        bindItems(purchaseOrder);
        purchaseOrderMapper.batchInsertPurchaseOrderItems(purchaseOrder.getItems());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePurchaseOrderStatus(Long purchaseId, String targetStatus, String operator)
    {
        ErpPurchaseOrder order = selectPurchaseOrderById(purchaseId);
        Set<String> allowedTargets = STATUS_TRANSITIONS.getOrDefault(order.getPurchaseStatus(), Set.of());
        if (!allowedTargets.contains(targetStatus))
        {
            throw new ServiceException("采购单不能从 " + order.getPurchaseStatus() + " 流转到 " + targetStatus);
        }
        if ("submitted".equals(targetStatus))
        {
            validateBeforeSubmit(order);
        }
        if ("completed".equals(targetStatus) && order.getReceivedQuantity() < order.getTotalQuantity())
        {
            throw new ServiceException("采购数量尚未全部到货，不能直接完成");
        }
        int rows = purchaseOrderMapper.updatePurchaseOrderStatus(
                purchaseId, order.getPurchaseStatus(), targetStatus, operator);
        if (rows != 1)
        {
            throw new ServiceException("采购单已被其他人处理，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int receivePurchaseOrder(Long purchaseId, ErpPurchaseReceiveRequest request, String operator)
    {
        ErpPurchaseOrder order = selectPurchaseOrderById(purchaseId);
        if (!Set.of("approved", "processing", "receiving").contains(order.getPurchaseStatus()))
        {
            throw new ServiceException("只有已审批或收货中的采购单可以登记到货");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty())
        {
            throw new ServiceException("请填写本次到货数量");
        }
        Map<Long, ErpPurchaseOrderItem> orderItems = new HashMap<>();
        for (ErpPurchaseOrderItem item : order.getItems())
        {
            orderItems.put(item.getItemId(), item);
        }
        Set<Long> receivedItemIds = new HashSet<>();
        List<Map<String, Object>> receiptItems = new ArrayList<>();
        String receiptNo = nextReceiptNo();
        int totalQty = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (ErpPurchaseReceiveItem received : request.getItems())
        {
            if (received.getQuantity() == null || received.getQuantity() <= 0)
            {
                continue;
            }
            ErpPurchaseOrderItem item = orderItems.get(received.getItemId());
            if (item == null || !receivedItemIds.add(received.getItemId()))
            {
                throw new ServiceException("到货明细不存在或重复");
            }
            int remainingQty = item.getQuantity() - item.getReceivedQty();
            if (received.getQuantity() > remainingQty)
            {
                throw new ServiceException(item.getSkuCode() + " 本次到货数量超过未到货数量 " + remainingQty);
            }
            Map<String, Object> receiptItem = new HashMap<>();
            BigDecimal lineAmount = item.getPurchasePrice()
                    .multiply(BigDecimal.valueOf(received.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            receiptItem.put("skuId", item.getSkuId());
            receiptItem.put("orderItemId", item.getItemId());
            receiptItem.put("skuCode", item.getSkuCode());
            receiptItem.put("skuName", item.getSkuName());
            receiptItem.put("quantity", received.getQuantity());
            receiptItem.put("costPrice", item.getPurchasePrice());
            receiptItem.put("amount", lineAmount);
            receiptItem.put("batchNo", isBlank(received.getBatchNo()) ? receiptNo : received.getBatchNo());
            receiptItems.add(receiptItem);
            totalQty += received.getQuantity();
            totalAmount = totalAmount.add(lineAmount);
        }
        if (receiptItems.isEmpty())
        {
            throw new ServiceException("本次到货总数量必须大于0");
        }

        for (Map<String, Object> receiptItem : receiptItems)
        {
            Long orderItemId = (Long) receiptItem.get("orderItemId");
            Integer receivedQty = (Integer) receiptItem.get("quantity");
            int rows = purchaseOrderMapper.incrementReceivedQty(purchaseId, orderItemId, receivedQty);
            if (rows != 1)
            {
                throw new ServiceException("到货数量已被其他人更新，请刷新后重试");
            }
            Map<String, Object> inventory = new HashMap<>();
            inventory.put("warehouseId", order.getWarehouseId());
            inventory.put("warehouseName", order.getWarehouseName());
            inventory.put("skuId", receiptItem.get("skuId"));
            inventory.put("skuCode", receiptItem.get("skuCode"));
            inventory.put("skuName", receiptItem.get("skuName"));
            inventory.put("quantity", receiptItem.get("quantity"));
            inventory.put("costPrice", receiptItem.get("costPrice"));
            inventory.put("batchNo", receiptItem.get("batchNo"));
            inventory.put("sourceType", "purchase_receipt");
            inventory.put("sourceNo", receiptNo);
            inventory.put("remark", request.getRemark());
            flowService.createInventoryIn(inventory, operator);

            Map<String, Object> costHistory = new HashMap<>();
            costHistory.put("skuId", receiptItem.get("skuId"));
            costHistory.put("skuCode", receiptItem.get("skuCode"));
            costHistory.put("costPrice", receiptItem.get("costPrice"));
            costHistory.put("effectiveDate", LocalDate.now().toString());
            costHistory.put("sourceType", "purchase_receipt");
            costHistory.put("sourceNo", receiptNo);
            costHistory.put("remark", "采购单 " + order.getPurchaseNo() + " 到货成本");
            flowService.add("cost-history", costHistory, operator);
        }

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("receiptNo", receiptNo);
        receipt.put("purchaseNo", order.getPurchaseNo());
        receipt.put("supplierId", order.getSupplierId());
        receipt.put("supplierName", order.getSupplierName());
        receipt.put("warehouseId", order.getWarehouseId());
        receipt.put("warehouseName", order.getWarehouseName());
        receipt.put("receiptDate", LocalDate.now().toString());
        receipt.put("totalQty", totalQty);
        receipt.put("totalAmount", totalAmount);
        receipt.put("receiptStatus", "approved");
        receipt.put("remark", request.getRemark());
        flowService.add("purchase-receipt", receipt, operator);
        purchaseOrderMapper.batchInsertPurchaseReceiptItems(receiptNo, order.getPurchaseNo(), receiptItems);

        String currentStatus = order.getPurchaseStatus();
        if (!"receiving".equals(currentStatus))
        {
            if (purchaseOrderMapper.updatePurchaseOrderStatus(purchaseId, currentStatus, "receiving", operator) != 1)
            {
                throw new ServiceException("采购单状态已变化，请刷新后重试");
            }
            currentStatus = "receiving";
        }
        ErpPurchaseOrder refreshed = purchaseOrderMapper.selectPurchaseOrderById(purchaseId);
        if (refreshed.getTotalQuantity() > 0 && refreshed.getReceivedQuantity() >= refreshed.getTotalQuantity())
        {
            purchaseOrderMapper.updatePurchaseOrderStatus(purchaseId, currentStatus, "completed", operator);
        }
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deletePurchaseOrderByIds(Long[] purchaseIds)
    {
        for (Long purchaseId : purchaseIds)
        {
            if (!"draft".equals(requireOrder(purchaseId).getPurchaseStatus()))
            {
                throw new ServiceException("只有草稿采购单可以删除");
            }
        }
        purchaseOrderMapper.deletePurchaseOrderItemsByPurchaseIds(purchaseIds);
        return purchaseOrderMapper.deletePurchaseOrderByIds(purchaseIds);
    }

    private ErpPurchaseOrder requireOrder(Long purchaseId)
    {
        ErpPurchaseOrder order = purchaseOrderMapper.selectPurchaseOrderById(purchaseId);
        if (order == null)
        {
            throw new ServiceException("采购单不存在");
        }
        return order;
    }

    private void defaultPurchaseDate(ErpPurchaseOrder purchaseOrder)
    {
        if (purchaseOrder.getPurchaseDate() == null)
        {
            purchaseOrder.setPurchaseDate(java.sql.Date.valueOf(LocalDate.now()));
        }
    }

    private void validateMasterData(ErpPurchaseOrder purchaseOrder)
    {
        ErpSupplier supplier = supplierMapper.selectSupplierById(purchaseOrder.getSupplierId());
        if (supplier == null || !"0".equals(supplier.getStatus()))
        {
            throw new ServiceException("供应商不存在或已停用");
        }
        purchaseOrder.setSupplierName(supplier.getSupplierName());
        validateReceivingWarehouse(purchaseOrder.getWarehouseId());
    }

    private void validateBeforeSubmit(ErpPurchaseOrder order)
    {
        if (order.getItems() == null || order.getItems().isEmpty())
        {
            throw new ServiceException("采购单没有商品明细，不能提交审批");
        }
        if (order.getExpectedTime() == null)
        {
            throw new ServiceException("请填写预计到货时间后再提交审批");
        }
        ErpSupplier supplier = supplierMapper.selectSupplierById(order.getSupplierId());
        if (supplier != null && supplier.getMinOrderAmount() != null
                && order.getTotalWithTax().compareTo(supplier.getMinOrderAmount()) < 0)
        {
            throw new ServiceException("采购金额低于供应商起订额 " + supplier.getMinOrderAmount());
        }
    }

    private void calculateOrder(ErpPurchaseOrder purchaseOrder, boolean resetReceivedQty)
    {
        List<ErpPurchaseOrderItem> items = purchaseOrder.getItems();
        if (items == null || items.isEmpty())
        {
            throw new ServiceException("请至少添加一条采购商品明细");
        }
        Set<Long> skuIds = new HashSet<>();
        BigDecimal totalAmount = ZERO;
        BigDecimal totalTax = ZERO;
        for (ErpPurchaseOrderItem item : items)
        {
            if (item.getSkuId() == null || !skuIds.add(item.getSkuId()))
            {
                throw new ServiceException("采购明细中的 SKU 不能为空或重复");
            }
            ErpProductSku sku = skuMapper.selectSkuById(item.getSkuId());
            if (sku == null || !"0".equals(sku.getStatus()))
            {
                throw new ServiceException("SKU 不存在或已停用：" + item.getSkuId());
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0)
            {
                throw new ServiceException("SKU " + sku.getSkuCode() + " 的采购数量必须大于0");
            }
            BigDecimal price = defaultDecimal(item.getPurchasePrice());
            BigDecimal taxRate = item.getTaxRate() == null ? new BigDecimal("0.13") : item.getTaxRate();
            if (price.signum() < 0 || taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0)
            {
                throw new ServiceException("SKU " + sku.getSkuCode() + " 的价格或税率不正确");
            }
            BigDecimal amount = price.multiply(BigDecimal.valueOf(item.getQuantity())).setScale(2, RoundingMode.HALF_UP);
            BigDecimal taxAmount = amount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            item.setSkuCode(sku.getSkuCode());
            item.setSkuName(sku.getSkuName());
            item.setPurchasePrice(price.setScale(2, RoundingMode.HALF_UP));
            item.setTaxRate(taxRate.setScale(4, RoundingMode.HALF_UP));
            item.setAmount(amount);
            item.setTaxAmount(taxAmount);
            item.setTaxInclusiveAmount(amount.add(taxAmount));
            if (resetReceivedQty)
            {
                item.setReceivedQty(0);
            }
            totalAmount = totalAmount.add(amount);
            totalTax = totalTax.add(taxAmount);
        }
        purchaseOrder.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        purchaseOrder.setTaxAmount(totalTax.setScale(2, RoundingMode.HALF_UP));
        purchaseOrder.setTotalWithTax(totalAmount.add(totalTax).setScale(2, RoundingMode.HALF_UP));
    }

    private void bindItems(ErpPurchaseOrder purchaseOrder)
    {
        for (ErpPurchaseOrderItem item : purchaseOrder.getItems())
        {
            item.setPurchaseId(purchaseOrder.getPurchaseId());
            item.setPurchaseNo(purchaseOrder.getPurchaseNo());
        }
    }

    private BigDecimal defaultDecimal(BigDecimal value)
    {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String nextReceiptNo()
    {
        return "RCV" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + ThreadLocalRandom.current().nextInt(100, 1000);
    }

    private boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private void validateReceivingWarehouse(Long warehouseId)
    {
        ErpWarehouse warehouse = warehouseMapper.selectWarehouseById(warehouseId);
        if (warehouse == null || !"0".equals(warehouse.getStatus()))
        {
            throw new ServiceException("采购入库仓不存在或已停用");
        }
        if ("return".equals(warehouse.getWarehouseUsage()) || "defective".equals(warehouse.getWarehouseUsage()))
        {
            throw new ServiceException("采购订单不能直接进入退货仓或残次仓");
        }
    }
}
