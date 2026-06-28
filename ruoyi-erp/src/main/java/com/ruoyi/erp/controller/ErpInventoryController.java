package com.ruoyi.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.erp.domain.ErpInventoryBalance;
import com.ruoyi.erp.service.IErpInventoryService;

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
}
