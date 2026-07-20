package com.xinke.erp.service.impl;

import java.util.List;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.utils.StringUtils;
import com.xinke.erp.domain.ErpCloudInventorySyncItem;
import com.xinke.erp.domain.ErpCloudInventorySyncRequest;
import com.xinke.erp.domain.ErpInventoryBalance;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.mapper.ErpInventoryMapper;
import com.xinke.erp.mapper.ErpWarehouseMapper;
import com.xinke.erp.service.IErpInventoryService;

@Service
public class ErpInventoryServiceImpl implements IErpInventoryService
{
    @Autowired
    private ErpInventoryMapper inventoryMapper;

    @Autowired
    private ErpWarehouseMapper warehouseMapper;

    @Override
    public List<ErpInventoryBalance> selectInventoryList(ErpInventoryBalance inventory)
    {
        return inventoryMapper.selectInventoryList(inventory);
    }

    @Override
    public Map<String, Object> selectInventorySummary(ErpInventoryBalance inventory)
    {
        return inventoryMapper.selectInventorySummary(inventory);
    }

    @Override
    @Transactional
    public Map<String, Object> syncCloudInventory(ErpCloudInventorySyncRequest request, String username)
    {
        ErpWarehouse warehouse = warehouseMapper.selectWarehouseById(request.getWarehouseId());
        if (warehouse == null)
        {
            throw new ServiceException("云仓不存在");
        }
        if (!("cloud".equals(warehouse.getWarehouseType()) || "third_party".equals(warehouse.getWarehouseType())))
        {
            throw new ServiceException("只有云仓可以同步服务商库存");
        }
        if (!"0".equals(warehouse.getStatus()))
        {
            throw new ServiceException("云仓已停用，不能同步库存");
        }

        LocalDateTime syncStarted = LocalDateTime.now();
        Set<String> skuCodes = new HashSet<>();
        for (ErpCloudInventorySyncItem item : request.getItems())
        {
            String skuCode = item.getSkuCode().trim().toUpperCase(Locale.ROOT);
            if (!skuCodes.add(skuCode))
            {
                throw new ServiceException("同步明细存在重复SKU：" + skuCode);
            }
            item.setSkuCode(skuCode);
            if (item.getLockedQty() == null) item.setLockedQty(0);
        }

        Set<String> existingSkuCodes = new HashSet<>(inventoryMapper.selectExistingSkuCodes(List.copyOf(skuCodes)));
        int matchedCount = existingSkuCodes.size();
        for (ErpCloudInventorySyncItem item : request.getItems())
        {
            if (existingSkuCodes.contains(item.getSkuCode()))
            {
                inventoryMapper.syncCloudInventoryItem(warehouse.getWarehouseId(), warehouse.getWarehouseName(), item);
            }
        }

        int totalCount = request.getItems().size();
        int unmatchedCount = totalCount - matchedCount;
        int differenceCount = inventoryMapper.countCloudDifference(warehouse.getWarehouseId());
        String syncStatus = unmatchedCount > 0 || differenceCount > 0 ? "warning" : "success";
        warehouseMapper.updateWarehouseSyncStatus(warehouse.getWarehouseId(), syncStatus);

        String batchNo = "CLOUD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Map<String, Object> log = new LinkedHashMap<>();
        log.put("syncBatchNo", batchNo);
        log.put("warehouseId", warehouse.getWarehouseId());
        log.put("sourceType", StringUtils.defaultIfBlank(request.getSource(), "manual"));
        log.put("syncStatus", syncStatus);
        log.put("totalSkuCount", totalCount);
        log.put("matchedSkuCount", matchedCount);
        log.put("unmatchedSkuCount", unmatchedCount);
        log.put("differenceSkuCount", differenceCount);
        log.put("startedTime", syncStarted);
        log.put("message", unmatchedCount > 0 ? "存在未建档SKU，请先维护商品SKU档案" : "库存快照已完成对账");
        log.put("createBy", username);
        inventoryMapper.insertCloudSyncLog(log);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchNo", batchNo);
        result.put("status", syncStatus);
        result.put("totalCount", totalCount);
        result.put("matchedCount", matchedCount);
        result.put("unmatchedCount", unmatchedCount);
        result.put("differenceCount", differenceCount);
        return result;
    }

    @Override
    public List<Map<String, Object>> selectCloudSyncLogList(Long warehouseId)
    {
        ErpWarehouse warehouse = warehouseMapper.selectWarehouseById(warehouseId);
        if (warehouse == null)
        {
            throw new ServiceException("仓库不存在");
        }
        return inventoryMapper.selectCloudSyncLogList(warehouseId);
    }
}
