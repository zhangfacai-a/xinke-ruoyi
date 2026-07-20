package com.xinke.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.annotation.Log;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.domain.ErpCloudInventorySyncRequest;
import com.xinke.erp.domain.ErpInventoryBalance;
import jakarta.validation.Valid;
import com.xinke.erp.service.IErpInventoryService;

@RestController
@RequestMapping("/erp/inventory")
public class ErpInventoryController extends BaseController
{
    @Autowired
    private IErpInventoryService inventoryService;

    @PreAuthorize("@ss.hasPermi('erp:inventory:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpInventoryBalance inventory)
    {
        startPage();
        List<ErpInventoryBalance> list = inventoryService.selectInventoryList(inventory);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:list')")
    @GetMapping("/summary")
    public AjaxResult summary(ErpInventoryBalance inventory)
    {
        return success(inventoryService.selectInventorySummary(inventory));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:sync')")
    @Log(title = "ERP 云仓库存同步", businessType = BusinessType.IMPORT)
    @PostMapping("/cloud/sync")
    public AjaxResult syncCloudInventory(@Valid @RequestBody ErpCloudInventorySyncRequest request)
    {
        return success(inventoryService.syncCloudInventory(request, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('erp:inventory:list')")
    @GetMapping("/cloud/sync/log/{warehouseId}")
    public AjaxResult cloudSyncLog(@PathVariable Long warehouseId)
    {
        return success(inventoryService.selectCloudSyncLogList(warehouseId));
    }
}
