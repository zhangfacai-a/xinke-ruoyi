package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import com.xinke.erp.domain.ErpCloudInventorySyncRequest;
import com.xinke.erp.domain.ErpInventoryBalance;

public interface IErpInventoryService
{
    List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory);
    Map<String, Object> selectInventorySummary(ErpInventoryBalance inventory);
    Map<String, Object> syncCloudInventory(ErpCloudInventorySyncRequest request, String username);
    List<Map<String, Object>> selectCloudSyncLogList(Long warehouseId);
}
