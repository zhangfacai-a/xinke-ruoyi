package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.ErpAfterSaleCreateRequest;
import com.xinke.erp.domain.ErpFulfillmentQuantity;
import com.xinke.erp.domain.ErpOrderItem;
import com.xinke.erp.domain.ErpShipmentCreateRequest;
import com.xinke.erp.mapper.ErpOrderMapper;
import com.xinke.erp.service.IErpFlowService;
import com.xinke.erp.service.IErpOrderService;

@Service
public class ErpOrderServiceImpl implements IErpOrderService
{
    private static final DateTimeFormatter NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> AFTER_SALE_TYPES = Set.of("refund_only", "return_refund");

    @Autowired
    private ErpOrderMapper orderMapper;

    @Autowired
    private IErpFlowService flowService;

    @Override
    public List<ErpOrderItem> selectOrderItemList(ErpOrderItem orderItem)
    {
        return orderMapper.selectOrderItemList(orderItem);
    }

    @Override
    public ErpOrderItem selectOrderItemById(Long orderItemId)
    {
        return orderMapper.selectOrderItemById(orderItemId);
    }

    @Override
    public List<Map<String, Object>> selectFulfillmentOrderList(Map<String, Object> query)
    {
        return orderMapper.selectFulfillmentOrderList(query);
    }

    @Override
    public Map<String, Object> selectFulfillmentSummary(Map<String, Object> query)
    {
        Map<String, Object> summary = orderMapper.selectFulfillmentSummary(query);
        return summary == null ? new LinkedHashMap<>() : summary;
    }

    @Override
    public Map<String, Object> selectFulfillmentDetail(Long salesId)
    {
        Map<String, Object> order = orderMapper.selectSalesOrderById(salesId);
        if (order == null)
        {
            throw new ServiceException("销售订单不存在");
        }
        List<Map<String, Object>> shipments = orderMapper.selectSalesShipments(salesId);
        for (Map<String, Object> shipment : shipments)
        {
            shipment.put("items", orderMapper.selectShipmentItems(longValue(shipment.get("shipmentId"))));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", orderMapper.selectSalesOrderItems(salesId));
        result.put("shipments", shipments);
        result.put("afterSales", orderMapper.selectAfterSales(salesId));
        result.put("warehouses", orderMapper.selectAvailableWarehouses());
        result.put("logs", orderMapper.selectFulfillmentLogs(salesId));
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> syncFulfillmentOrders(String username)
    {
        int orderRows = orderMapper.syncSalesOrdersFromDwd();
        int itemRows = orderMapper.syncSalesOrderItemsFromDwd();
        orderMapper.refreshSalesOrderQuantity();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderRows", orderRows);
        result.put("itemRows", itemRows);
        result.put("operator", username);
        result.put("syncTime", LocalDateTime.now());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createShipment(ErpShipmentCreateRequest request, String username)
    {
        String idempotencyKey = trimToNull(request.getIdempotencyKey());
        if (idempotencyKey != null)
        {
            Map<String, Object> existing = orderMapper.selectShipmentByIdempotencyKey(idempotencyKey);
            if (existing != null)
            {
                return text(existing.get("shipmentNo"));
            }
        }

        Map<String, Object> order = requireOrderForUpdate(request.getSalesId());
        if ("refunded".equals(text(order.get("fulfillment_status"))))
        {
            throw new ServiceException("订单已全部退款，不能创建发货单");
        }
        Map<String, Object> warehouse = requireWarehouse(request.getWarehouseId());

        String shipmentNo = nextNo("SHP");
        Map<String, Object> shipment = new LinkedHashMap<>();
        shipment.put("shipmentNo", shipmentNo);
        shipment.put("salesId", request.getSalesId());
        shipment.put("salesNo", order.get("sales_no"));
        shipment.put("mainOrderNo", order.get("main_order_no"));
        shipment.put("packageNo", shipmentNo);
        shipment.put("warehouseId", request.getWarehouseId());
        shipment.put("warehouseName", warehouse.get("warehouseName"));
        shipment.put("logisticsCompany", trimToNull(request.getLogisticsCompany()));
        shipment.put("logisticsNo", trimToNull(request.getLogisticsNo()));
        shipment.put("idempotencyKey", idempotencyKey);
        shipment.put("createBy", username);
        shipment.put("remark", trimToNull(request.getRemark()));
        orderMapper.insertShipment(shipment);

        Set<Long> selectedItems = new HashSet<>();
        for (ErpFulfillmentQuantity requested : request.getItems())
        {
            if (!selectedItems.add(requested.getSalesItemId()))
            {
                throw new ServiceException("同一订单商品不能重复添加到一个包裹");
            }
            Map<String, Object> item = requireItemForUpdate(requested.getSalesItemId());
            if (!request.getSalesId().equals(longValue(item.get("sales_id"))))
            {
                throw new ServiceException("发货商品不属于当前主订单");
            }
            int reserved = orderMapper.selectReservedShipmentQuantity(requested.getSalesItemId());
            int available = intValue(item.get("quantity")) - intValue(item.get("shipped_quantity"))
                    - intValue(item.get("refunded_quantity")) - reserved;
            if (requested.getQuantity() > available)
            {
                throw new ServiceException("子订单 " + item.get("sub_order_no") + " 可发数量不足，可发 " + available);
            }
            Map<String, Object> shipmentItem = new LinkedHashMap<>();
            shipmentItem.put("shipmentId", shipment.get("shipmentId"));
            shipmentItem.put("shipmentNo", shipmentNo);
            shipmentItem.put("salesId", request.getSalesId());
            shipmentItem.put("salesItemId", requested.getSalesItemId());
            shipmentItem.put("salesNo", order.get("sales_no"));
            shipmentItem.put("mainOrderNo", order.get("main_order_no"));
            shipmentItem.put("subOrderNo", item.get("sub_order_no"));
            shipmentItem.put("skuId", item.get("sku_id"));
            shipmentItem.put("skuCode", item.get("sku_code"));
            shipmentItem.put("skuName", item.get("sku_name"));
            shipmentItem.put("quantity", requested.getQuantity());
            orderMapper.insertShipmentItem(shipmentItem);
        }
        insertLog(request.getSalesId(), null, "shipment", shipmentNo, "create", null, "draft",
                username, "创建发货包裹，仓库：" + warehouse.get("warehouseName"));
        return shipmentNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int shipShipment(String shipmentNo, String username)
    {
        Map<String, Object> shipment = requireShipmentForUpdate(shipmentNo);
        requireStatus(shipment.get("shipmentStatus"), "draft", "只有草稿发货单可以确认发货");
        if (trimToNull(text(shipment.get("logisticsNo"))) == null)
        {
            throw new ServiceException("确认发货前必须填写物流单号");
        }
        Long salesId = longValue(shipment.get("salesId"));
        requireOrderForUpdate(salesId);
        List<Map<String, Object>> items = orderMapper.selectShipmentItems(longValue(shipment.get("shipmentId")));
        if (items.isEmpty())
        {
            throw new ServiceException("发货单没有商品明细");
        }
        for (Map<String, Object> shipmentItem : items)
        {
            Long salesItemId = longValue(shipmentItem.get("salesItemId"));
            Map<String, Object> orderItem = requireItemForUpdate(salesItemId);
            int quantity = intValue(shipmentItem.get("quantity"));
            int available = intValue(orderItem.get("quantity")) - intValue(orderItem.get("shipped_quantity"))
                    - intValue(orderItem.get("refunded_quantity"));
            if (quantity > available)
            {
                throw new ServiceException("子订单 " + orderItem.get("sub_order_no") + " 可发数量不足");
            }

            Map<String, Object> inventory = new LinkedHashMap<>();
            inventory.put("warehouseId", shipment.get("warehouseId"));
            inventory.put("warehouseName", shipment.get("warehouseName"));
            inventory.put("skuId", orderItem.get("sku_id"));
            inventory.put("skuCode", orderItem.get("sku_code"));
            inventory.put("skuName", orderItem.get("sku_name"));
            inventory.put("quantity", quantity);
            inventory.put("costPrice", unitCost(orderItem));
            inventory.put("sourceType", "sales_shipment");
            inventory.put("sourceNo", shipmentNo);
            inventory.put("remark", "销售发货：" + orderItem.get("sub_order_no"));
            flowService.createInventoryOut(inventory, username);

            if (orderMapper.increaseItemShippedQuantity(salesItemId, quantity) != 1)
            {
                throw new ServiceException("订单商品发货数量发生并发变化，请刷新后重试");
            }
        }
        if (orderMapper.updateShipmentStatus(shipmentNo, "draft", "shipped", username, null) != 1)
        {
            throw new ServiceException("发货单状态已变化，请刷新后重试");
        }
        refreshOrderStatus(salesId);
        insertLog(salesId, null, "shipment", shipmentNo, "ship", "draft", "shipped",
                username, "确认发货并扣减仓库库存");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int signShipment(String shipmentNo, String username)
    {
        Map<String, Object> shipment = requireShipmentForUpdate(shipmentNo);
        requireStatus(shipment.get("shipmentStatus"), "shipped", "只有已发货包裹可以签收");
        if (orderMapper.updateShipmentStatus(shipmentNo, "shipped", "signed", username, null) != 1)
        {
            throw new ServiceException("发货单状态已变化，请刷新后重试");
        }
        insertLog(longValue(shipment.get("salesId")), null, "shipment", shipmentNo, "sign",
                "shipped", "signed", username, "物流包裹已签收");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cancelShipment(String shipmentNo, String reason, String username)
    {
        if (trimToNull(reason) == null)
        {
            throw new ServiceException("取消发货单必须填写原因");
        }
        Map<String, Object> shipment = requireShipmentForUpdate(shipmentNo);
        requireStatus(shipment.get("shipmentStatus"), "draft", "只有草稿发货单可以取消");
        if (orderMapper.updateShipmentStatus(shipmentNo, "draft", "canceled", username, reason) != 1)
        {
            throw new ServiceException("发货单状态已变化，请刷新后重试");
        }
        insertLog(longValue(shipment.get("salesId")), null, "shipment", shipmentNo, "cancel",
                "draft", "canceled", username, reason);
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAfterSale(ErpAfterSaleCreateRequest request, String username)
    {
        String type = trimToNull(request.getAfterSaleType());
        if (!AFTER_SALE_TYPES.contains(type))
        {
            throw new ServiceException("售后类型仅支持仅退款或退货退款");
        }
        String idempotencyKey = trimToNull(request.getIdempotencyKey());
        if (idempotencyKey != null)
        {
            Map<String, Object> existing = orderMapper.selectAfterSaleByIdempotencyKey(idempotencyKey);
            if (existing != null)
            {
                return text(existing.get("afterSaleNo"));
            }
        }

        Map<String, Object> itemRef = orderMapper.selectSalesOrderItem(request.getSalesItemId());
        if (itemRef == null)
        {
            throw new ServiceException("销售订单商品不存在");
        }
        Long salesId = longValue(itemRef.get("salesId"));
        Map<String, Object> order = requireOrderForUpdate(salesId);
        Map<String, Object> item = requireItemForUpdate(request.getSalesItemId());
        Map<String, Object> reserved = orderMapper.selectReservedAfterSale(request.getSalesItemId());
        int reservedQuantity = intValue(reserved == null ? null : reserved.get("reservedQuantity"));
        int baseQuantity = "return_refund".equals(type)
                ? intValue(item.get("shipped_quantity")) : intValue(item.get("quantity"));
        int availableQuantity = baseQuantity - intValue(item.get("refunded_quantity")) - reservedQuantity;
        if (request.getQuantity() > availableQuantity)
        {
            throw new ServiceException("可申请售后数量不足，可申请 " + Math.max(availableQuantity, 0));
        }
        BigDecimal reservedAmount = decimal(reserved == null ? null : reserved.get("reservedAmount"));
        BigDecimal completedAmount = completedRefundAmount(salesId, request.getSalesItemId());
        BigDecimal availableAmount = decimal(item.get("pay_amount")).subtract(reservedAmount).subtract(completedAmount);
        if (request.getRefundAmount().compareTo(availableAmount) > 0)
        {
            throw new ServiceException("退款金额超过剩余可退金额 " + availableAmount.max(BigDecimal.ZERO));
        }

        Map<String, Object> returnWarehouse = null;
        if ("return_refund".equals(type))
        {
            if (request.getReturnWarehouseId() == null)
            {
                throw new ServiceException("退货退款必须选择退货仓库");
            }
            returnWarehouse = requireWarehouse(request.getReturnWarehouseId());
        }

        String afterSaleNo = nextNo("AS");
        Map<String, Object> afterSale = new LinkedHashMap<>();
        afterSale.put("afterSaleNo", afterSaleNo);
        afterSale.put("salesId", salesId);
        afterSale.put("salesItemId", request.getSalesItemId());
        afterSale.put("salesNo", order.get("sales_no"));
        afterSale.put("mainOrderNo", item.get("main_order_no"));
        afterSale.put("subOrderNo", item.get("sub_order_no"));
        afterSale.put("skuId", item.get("sku_id"));
        afterSale.put("skuCode", item.get("sku_code"));
        afterSale.put("skuName", item.get("sku_name"));
        afterSale.put("quantity", request.getQuantity());
        afterSale.put("afterSaleType", type);
        afterSale.put("refundAmount", request.getRefundAmount());
        afterSale.put("returnWarehouseId", request.getReturnWarehouseId());
        afterSale.put("returnWarehouseName", returnWarehouse == null ? null : returnWarehouse.get("warehouseName"));
        afterSale.put("idempotencyKey", idempotencyKey);
        afterSale.put("reason", request.getReason().trim());
        afterSale.put("createBy", username);
        afterSale.put("remark", trimToNull(request.getRemark()));
        orderMapper.insertAfterSale(afterSale);
        insertLog(salesId, request.getSalesItemId(), "after_sale", afterSaleNo, "create",
                null, "pending", username, request.getReason().trim());
        return afterSaleNo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveAfterSale(String afterSaleNo, String username)
    {
        Map<String, Object> afterSale = requireAfterSaleForUpdate(afterSaleNo);
        requireStatus(afterSale.get("afterSaleStatus"), "pending", "只有待审核售后单可以审核");
        if (orderMapper.updateAfterSaleStatus(afterSaleNo, "pending", "approved", username) != 1)
        {
            throw new ServiceException("售后单状态已变化，请刷新后重试");
        }
        insertLog(longValue(afterSale.get("salesId")), longValue(afterSale.get("salesItemId")),
                "after_sale", afterSaleNo, "approve", "pending", "approved", username, "售后审核通过");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rejectAfterSale(String afterSaleNo, String reason, String username)
    {
        String rejectReason = trimToNull(reason);
        if (rejectReason == null)
        {
            throw new ServiceException("驳回售后单必须填写原因");
        }
        Map<String, Object> afterSale = requireAfterSaleForUpdate(afterSaleNo);
        requireStatus(afterSale.get("afterSaleStatus"), "pending", "只有待审核售后单可以驳回");
        if (orderMapper.updateAfterSaleStatus(afterSaleNo, "pending", "rejected", username) != 1)
        {
            throw new ServiceException("售后单状态已变化，请刷新后重试");
        }
        insertLog(longValue(afterSale.get("salesId")), longValue(afterSale.get("salesItemId")),
                "after_sale", afterSaleNo, "reject", "pending", "rejected", username, rejectReason);
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int completeAfterSale(String afterSaleNo, String username)
    {
        Map<String, Object> afterSale = requireAfterSaleForUpdate(afterSaleNo);
        requireStatus(afterSale.get("afterSaleStatus"), "approved", "只有已审核售后单可以完成");
        Long salesId = longValue(afterSale.get("salesId"));
        requireOrderForUpdate(salesId);
        Long salesItemId = longValue(afterSale.get("salesItemId"));
        Map<String, Object> item = requireItemForUpdate(salesItemId);
        int quantity = intValue(afterSale.get("quantity"));
        if (orderMapper.increaseItemRefundedQuantity(salesItemId, quantity) != 1)
        {
            throw new ServiceException("售后数量发生并发变化，请刷新后重试");
        }

        if ("return_refund".equals(text(afterSale.get("afterSaleType"))))
        {
            Map<String, Object> inventory = new LinkedHashMap<>();
            inventory.put("warehouseId", afterSale.get("returnWarehouseId"));
            inventory.put("warehouseName", afterSale.get("returnWarehouseName"));
            inventory.put("skuId", item.get("sku_id"));
            inventory.put("skuCode", item.get("sku_code"));
            inventory.put("skuName", item.get("sku_name"));
            inventory.put("quantity", quantity);
            inventory.put("costPrice", unitCost(item));
            inventory.put("batchNo", afterSaleNo);
            inventory.put("inboundDate", LocalDate.now().toString());
            inventory.put("sourceType", "sales_return");
            inventory.put("sourceNo", afterSaleNo);
            inventory.put("remark", "销售退货：" + item.get("sub_order_no"));
            flowService.createInventoryIn(inventory, username);
        }

        if (orderMapper.updateAfterSaleStatus(afterSaleNo, "approved", "completed", username) != 1)
        {
            throw new ServiceException("售后单状态已变化，请刷新后重试");
        }
        Long dwdOrderItemId = longValue(item.get("dwd_order_item_id"));
        if (dwdOrderItemId != null)
        {
            orderMapper.updateDwdRefundCost(dwdOrderItemId, decimal(afterSale.get("refundAmount")));
        }
        refreshOrderStatus(salesId);
        insertLog(salesId, salesItemId, "after_sale", afterSaleNo, "complete",
                "approved", "completed", username, "售后完成并更新退款及库存");
        return 1;
    }

    private Map<String, Object> requireOrderForUpdate(Long salesId)
    {
        Map<String, Object> order = orderMapper.selectSalesOrderForUpdate(salesId);
        if (order == null)
        {
            throw new ServiceException("销售订单不存在");
        }
        return order;
    }

    private Map<String, Object> requireItemForUpdate(Long salesItemId)
    {
        Map<String, Object> item = orderMapper.selectSalesOrderItemForUpdate(salesItemId);
        if (item == null)
        {
            throw new ServiceException("销售订单商品不存在");
        }
        return item;
    }

    private Map<String, Object> requireWarehouse(Long warehouseId)
    {
        Map<String, Object> warehouse = orderMapper.selectWarehouseForFulfillment(warehouseId);
        if (warehouse == null)
        {
            throw new ServiceException("仓库不存在");
        }
        if (!"0".equals(text(warehouse.get("status"))))
        {
            throw new ServiceException("仓库已停用");
        }
        return warehouse;
    }

    private Map<String, Object> requireShipmentForUpdate(String shipmentNo)
    {
        Map<String, Object> shipment = orderMapper.selectShipmentForUpdate(shipmentNo);
        if (shipment == null)
        {
            throw new ServiceException("发货单不存在");
        }
        return shipment;
    }

    private Map<String, Object> requireAfterSaleForUpdate(String afterSaleNo)
    {
        Map<String, Object> afterSale = orderMapper.selectAfterSaleForUpdate(afterSaleNo);
        if (afterSale == null)
        {
            throw new ServiceException("售后单不存在");
        }
        return afterSale;
    }

    private void requireStatus(Object currentStatus, String expectedStatus, String message)
    {
        if (!expectedStatus.equals(text(currentStatus)))
        {
            throw new ServiceException(message);
        }
    }

    private void refreshOrderStatus(Long salesId)
    {
        orderMapper.refreshSalesOrderItemStatus(salesId);
        orderMapper.refreshSalesOrderStatus(salesId);
    }

    private BigDecimal completedRefundAmount(Long salesId, Long salesItemId)
    {
        return orderMapper.selectAfterSales(salesId).stream()
                .filter(row -> salesItemId.equals(longValue(row.get("salesItemId"))))
                .filter(row -> "completed".equals(text(row.get("afterSaleStatus"))))
                .map(row -> decimal(row.get("refundAmount")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal unitCost(Map<String, Object> item)
    {
        int quantity = intValue(item.get("quantity"));
        return quantity <= 0 ? BigDecimal.ZERO
                : decimal(item.get("product_cost")).divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP);
    }

    private void insertLog(Long salesId, Long salesItemId, String bizType, String bizNo,
                           String actionType, String fromStatus, String toStatus,
                           String username, String detail)
    {
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("salesId", salesId);
        log.put("salesItemId", salesItemId);
        log.put("bizType", bizType);
        log.put("bizNo", bizNo);
        log.put("actionType", actionType);
        log.put("fromStatus", fromStatus);
        log.put("toStatus", toStatus);
        log.put("operatorName", username);
        log.put("detail", detail);
        orderMapper.insertFulfillmentLog(log);
    }

    private String nextNo(String prefix)
    {
        return prefix + LocalDateTime.now().format(NO_TIME)
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String trimToNull(String value)
    {
        if (value == null || value.trim().isEmpty())
        {
            return null;
        }
        return value.trim();
    }

    private String text(Object value)
    {
        return value == null ? "" : String.valueOf(value);
    }

    private Long longValue(Object value)
    {
        if (value == null || text(value).isBlank()) return null;
        if (value instanceof Number number) return number.longValue();
        return Long.valueOf(text(value));
    }

    private int intValue(Object value)
    {
        if (value == null || text(value).isBlank()) return 0;
        if (value instanceof Number number) return number.intValue();
        return new BigDecimal(text(value)).intValue();
    }

    private BigDecimal decimal(Object value)
    {
        if (value == null || text(value).isBlank()) return BigDecimal.ZERO;
        if (value instanceof BigDecimal decimal) return decimal;
        return new BigDecimal(text(value));
    }
}
