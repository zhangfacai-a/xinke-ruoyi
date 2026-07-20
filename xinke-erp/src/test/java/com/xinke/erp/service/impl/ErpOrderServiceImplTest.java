package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.ErpAfterSaleCreateRequest;
import com.xinke.erp.domain.ErpFulfillmentQuantity;
import com.xinke.erp.domain.ErpShipmentCreateRequest;
import com.xinke.erp.mapper.ErpOrderMapper;
import com.xinke.erp.service.IErpFlowService;

@ExtendWith(MockitoExtension.class)
class ErpOrderServiceImplTest
{
    @Mock private ErpOrderMapper orderMapper;
    @Mock private IErpFlowService flowService;
    @InjectMocks private ErpOrderServiceImpl service;

    @Test
    void createShipmentRejectsQuantityReservedByAnotherDraft()
    {
        ErpShipmentCreateRequest request = shipmentRequest(1L, 2L, 10L, 2);
        when(orderMapper.selectSalesOrderForUpdate(1L)).thenReturn(order(1L));
        when(orderMapper.selectWarehouseForFulfillment(2L)).thenReturn(warehouse(2L));
        when(orderMapper.insertShipment(any())).thenAnswer(invocation -> {
            Map<String, Object> shipment = invocation.getArgument(0);
            shipment.put("shipmentId", 100L);
            return 1;
        });
        when(orderMapper.selectSalesOrderItemForUpdate(10L))
                .thenReturn(item(1L, 10L, 5, 2, 1));
        when(orderMapper.selectReservedShipmentQuantity(10L)).thenReturn(1);

        assertThrows(ServiceException.class, () -> service.createShipment(request, "tester"));
    }

    @Test
    void shipShipmentCreatesInventoryOutAndUpdatesOrderQuantity()
    {
        Map<String, Object> shipment = map(
                "shipmentStatus", "draft", "logisticsNo", "SF-001", "salesId", 1L,
                "shipmentId", 100L, "warehouseId", 2L, "warehouseName", "Main warehouse");
        when(orderMapper.selectShipmentForUpdate("SHP-001")).thenReturn(shipment);
        when(orderMapper.selectSalesOrderForUpdate(1L)).thenReturn(order(1L));
        when(orderMapper.selectShipmentItems(100L))
                .thenReturn(List.of(map("salesItemId", 10L, "quantity", 2)));
        when(orderMapper.selectSalesOrderItemForUpdate(10L))
                .thenReturn(item(1L, 10L, 5, 1, 0));
        when(flowService.createInventoryOut(any(), eq("tester"))).thenReturn(1);
        when(orderMapper.increaseItemShippedQuantity(10L, 2)).thenReturn(1);
        when(orderMapper.updateShipmentStatus("SHP-001", "draft", "shipped", "tester", null))
                .thenReturn(1);

        assertEquals(1, service.shipShipment("SHP-001", "tester"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> inventory = ArgumentCaptor.forClass(Map.class);
        verify(flowService).createInventoryOut(inventory.capture(), eq("tester"));
        assertEquals(2L, inventory.getValue().get("warehouseId"));
        assertEquals(2, inventory.getValue().get("quantity"));
        assertEquals("sales_shipment", inventory.getValue().get("sourceType"));
        verify(orderMapper).increaseItemShippedQuantity(10L, 2);
    }

    @Test
    void returnRefundRequiresReturnWarehouse()
    {
        ErpAfterSaleCreateRequest request = new ErpAfterSaleCreateRequest();
        request.setSalesItemId(10L);
        request.setAfterSaleType("return_refund");
        request.setQuantity(1);
        request.setRefundAmount(new BigDecimal("10.00"));
        request.setReason("Return requested");

        when(orderMapper.selectSalesOrderItem(10L)).thenReturn(map("salesId", 1L));
        when(orderMapper.selectSalesOrderForUpdate(1L)).thenReturn(order(1L));
        when(orderMapper.selectSalesOrderItemForUpdate(10L))
                .thenReturn(item(1L, 10L, 5, 3, 0));
        when(orderMapper.selectReservedAfterSale(10L))
                .thenReturn(map("reservedQuantity", 0, "reservedAmount", BigDecimal.ZERO));
        when(orderMapper.selectAfterSales(1L)).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> service.createAfterSale(request, "tester"));
    }

    @Test
    void completeReturnRefundRestocksAndWritesRefundAmount()
    {
        Map<String, Object> afterSale = map(
                "afterSaleStatus", "approved", "afterSaleType", "return_refund",
                "salesId", 1L, "salesItemId", 10L, "quantity", 1,
                "refundAmount", new BigDecimal("49.90"), "returnWarehouseId", 2L,
                "returnWarehouseName", "Returns warehouse");
        when(orderMapper.selectAfterSaleForUpdate("AS-001")).thenReturn(afterSale);
        when(orderMapper.selectSalesOrderForUpdate(1L)).thenReturn(order(1L));
        when(orderMapper.selectSalesOrderItemForUpdate(10L))
                .thenReturn(item(1L, 10L, 5, 3, 0));
        when(orderMapper.increaseItemRefundedQuantity(10L, 1)).thenReturn(1);
        when(flowService.createInventoryIn(any(), eq("tester"))).thenReturn(1);
        when(orderMapper.updateAfterSaleStatus("AS-001", "approved", "completed", "tester"))
                .thenReturn(1);
        when(orderMapper.updateDwdRefundCost(500L, new BigDecimal("49.90"))).thenReturn(1);

        assertEquals(1, service.completeAfterSale("AS-001", "tester"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> inventory = ArgumentCaptor.forClass(Map.class);
        verify(flowService).createInventoryIn(inventory.capture(), eq("tester"));
        assertEquals("sales_return", inventory.getValue().get("sourceType"));
        assertEquals(1, inventory.getValue().get("quantity"));
        verify(orderMapper).updateDwdRefundCost(500L, new BigDecimal("49.90"));
    }

    @Test
    void rejectAfterSaleReleasesPendingRequest()
    {
        when(orderMapper.selectAfterSaleForUpdate("AS-002")).thenReturn(map(
                "afterSaleStatus", "pending", "salesId", 1L, "salesItemId", 10L));
        when(orderMapper.updateAfterSaleStatus("AS-002", "pending", "rejected", "tester"))
                .thenReturn(1);

        assertEquals(1, service.rejectAfterSale("AS-002", "Invalid evidence", "tester"));
        verify(orderMapper).updateAfterSaleStatus("AS-002", "pending", "rejected", "tester");
    }

    private ErpShipmentCreateRequest shipmentRequest(Long salesId, Long warehouseId,
                                                      Long salesItemId, int quantity)
    {
        ErpFulfillmentQuantity item = new ErpFulfillmentQuantity();
        item.setSalesItemId(salesItemId);
        item.setQuantity(quantity);
        ErpShipmentCreateRequest request = new ErpShipmentCreateRequest();
        request.setSalesId(salesId);
        request.setWarehouseId(warehouseId);
        request.setItems(List.of(item));
        return request;
    }

    private Map<String, Object> order(Long salesId)
    {
        return map("sales_id", salesId, "sales_no", "SO-001", "main_order_no", "MAIN-001",
                "fulfillment_status", "pending");
    }

    private Map<String, Object> warehouse(Long warehouseId)
    {
        return map("warehouseId", warehouseId, "warehouseName", "Main warehouse", "status", "0");
    }

    private Map<String, Object> item(Long salesId, Long salesItemId, int quantity,
                                     int shippedQuantity, int refundedQuantity)
    {
        return map(
                "sales_id", salesId, "sales_item_id", salesItemId, "sub_order_no", "SUB-001",
                "main_order_no", "MAIN-001", "sku_id", 20L, "sku_code", "SKU-020",
                "sku_name", "Test SKU", "quantity", quantity, "shipped_quantity", shippedQuantity,
                "refunded_quantity", refundedQuantity, "pay_amount", new BigDecimal("249.50"),
                "product_cost", new BigDecimal("150.00"), "dwd_order_item_id", 500L);
    }

    private Map<String, Object> map(Object... values)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2)
        {
            result.put(String.valueOf(values[index]), values[index + 1]);
        }
        return result;
    }
}
