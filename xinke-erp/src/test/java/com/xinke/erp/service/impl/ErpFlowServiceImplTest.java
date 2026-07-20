package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.erp.mapper.ErpFlowMapper;
import com.xinke.erp.mapper.FinanceCoreMapper;

@ExtendWith(MockitoExtension.class)
class ErpFlowServiceImplTest
{
    @Mock private ErpFlowMapper erpFlowMapper;
    @Mock private FinanceCoreMapper financeCoreMapper;
    @InjectMocks private ErpFlowServiceImpl service;

    @Test
    void inventoryReceiptUsesMovingWeightedAverageCost()
    {
        when(erpFlowMapper.selectWarehouseInventoryPolicy(1L)).thenReturn(Map.of(
                "warehouse_id", 1L, "warehouse_name", "测试仓", "status", "0",
                "allow_negative_stock", "0"));
        Map<String, Object> balance = new HashMap<>();
        balance.put("available_qty", 100);
        balance.put("defective_qty", 0);
        balance.put("cost_price", new BigDecimal("10.00"));
        when(erpFlowMapper.selectInventoryBalance(1L, 2L)).thenReturn(balance);

        Map<String, Object> receipt = new HashMap<>();
        receipt.put("warehouseId", 1L);
        receipt.put("skuId", 2L);
        receipt.put("quantity", 20);
        receipt.put("costPrice", new BigDecimal("16.00"));
        receipt.put("sourceType", "purchase_receipt");
        receipt.put("sourceNo", "RCV-TEST");
        service.createInventoryIn(receipt, "admin");

        ArgumentCaptor<Map<String, Object>> balanceCaptor = ArgumentCaptor.forClass(Map.class);
        verify(erpFlowMapper).updateInventoryBalance(balanceCaptor.capture());
        assertEquals(120, balanceCaptor.getValue().get("availableQty"));
        assertEquals(new BigDecimal("11.00"), balanceCaptor.getValue().get("costPrice"));

        ArgumentCaptor<Map<String, Object>> layerCaptor = ArgumentCaptor.forClass(Map.class);
        verify(erpFlowMapper).insertCostLayer(layerCaptor.capture());
        assertEquals(new BigDecimal("16.00"), layerCaptor.getValue().get("costPrice"));
    }

    @Test
    void costHistoryOnlyWritesAuditColumnsOwnedByItsTable()
    {
        Map<String, Object> history = new HashMap<>();
        history.put("skuId", 2L);
        history.put("skuCode", "SKU-002");
        history.put("costPrice", new BigDecimal("16.00"));
        history.put("effectiveDate", "2026-07-17");
        history.put("sourceType", "purchase_receipt");
        history.put("sourceNo", "RCV-TEST");

        service.add("cost-history", history, "admin");

        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(erpFlowMapper).insertFlow(eq("erp_sku_cost_history"), dataCaptor.capture());
        assertEquals("admin", dataCaptor.getValue().get("create_by"));
        assertFalse(dataCaptor.getValue().containsKey("update_by"));
        assertFalse(dataCaptor.getValue().containsKey("update_time"));
    }
}
