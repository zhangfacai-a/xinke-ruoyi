package com.xinke.erp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.erp.service.IFinanceCoreService;

@RestController
@RequestMapping("/finance/dashboard")
public class FinanceDashboardController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @PreAuthorize("@ss.hasPermi('finance:profit:daily')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return success(financeCoreService.selectDashboardOverview());
    }
}
