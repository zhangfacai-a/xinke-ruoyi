package com.xinke.erp.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.erp.domain.ErpInventoryMovement;
import com.xinke.erp.service.IErpInventoryMovementService;

@RestController
@RequestMapping("/erp/movement")
public class ErpInventoryMovementController extends BaseController
{
    @Autowired
    private IErpInventoryMovementService movementService;

    @PreAuthorize("@ss.hasPermi('erp:movement:list')")
    @GetMapping("/list")
    public TableDataInfo list(ErpInventoryMovement movement)
    {
        startPage();
        List<ErpInventoryMovement> list = movementService.selectMovementList(movement);
        return getDataTable(list);
    }
}
