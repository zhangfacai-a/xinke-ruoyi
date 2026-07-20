package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.erp.domain.ErpCloudInventorySyncItem;
import com.xinke.erp.domain.ErpCloudInventorySyncRequest;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.mapper.ErpInventoryMapper;
import com.xinke.erp.mapper.ErpWarehouseMapper;

@ExtendWith(MockitoExtension.class)
class ErpInventoryServiceImplTest
{
    @Mock
    private ErpInventoryMapper inventoryMapper;

    @Mock
    private ErpWarehouseMapper warehouseMapper;

    @InjectMocks
    private ErpInventoryServiceImpl service;

    @Test
    void cloudSyncRejectsUnknownSkuFromInventoryLedger()
    {
        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setWarehouseId(8L);
        warehouse.setWarehouseName("华东云仓");
        warehouse.setWarehouseType("cloud");
        warehouse.setStatus("0");
        when(warehouseMapper.selectWarehouseById(8L)).thenReturn(warehouse);
        when(inventoryMapper.selectExistingSkuCodes(anyList())).thenReturn(List.of("SKU-001"));
        when(inventoryMapper.syncCloudInventoryItem(eq(8L), eq("华东云仓"), any())).thenReturn(1);
        when(inventoryMapper.countCloudDifference(8L)).thenReturn(1);

        ErpCloudInventorySyncRequest request = new ErpCloudInventorySyncRequest();
        request.setWarehouseId(8L);
        request.setSource("manual");
        request.setItems(List.of(item("sku-001", 10), item("sku-unknown", 20)));

        Map<String, Object> result = service.syncCloudInventory(request, "admin");

        assertEquals(1, result.get("matchedCount"));
        assertEquals(1, result.get("unmatchedCount"));
        assertEquals("warning", result.get("status"));
        verify(warehouseMapper).updateWarehouseSyncStatus(8L, "warning");
        verify(inventoryMapper).insertCloudSyncLog(any());
        verify(inventoryMapper, never()).syncCloudInventoryItem(eq(8L), eq("华东云仓"), eq(request.getItems().get(1)));
    }

    private ErpCloudInventorySyncItem item(String skuCode, int availableQty)
    {
        ErpCloudInventorySyncItem item = new ErpCloudInventorySyncItem();
        item.setSkuCode(skuCode);
        item.setAvailableQty(availableQty);
        item.setLockedQty(0);
        return item;
    }
}
