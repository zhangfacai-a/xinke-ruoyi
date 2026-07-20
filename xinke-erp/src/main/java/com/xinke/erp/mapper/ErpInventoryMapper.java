package com.xinke.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.xinke.erp.domain.ErpCloudInventorySyncItem;
import com.xinke.erp.domain.ErpInventoryBalance;

@Mapper
public interface ErpInventoryMapper
{
    List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory);
    Map<String, Object> selectInventorySummary(ErpInventoryBalance inventory);
    List<String> selectExistingSkuCodes(@Param("skuCodes") List<String> skuCodes);
    int syncCloudInventoryItem(@Param("warehouseId") Long warehouseId,
            @Param("warehouseName") String warehouseName,
            @Param("item") ErpCloudInventorySyncItem item);
    int countCloudDifference(Long warehouseId);
    int insertCloudSyncLog(Map<String, Object> log);
    List<Map<String, Object>> selectCloudSyncLogList(Long warehouseId);
}
