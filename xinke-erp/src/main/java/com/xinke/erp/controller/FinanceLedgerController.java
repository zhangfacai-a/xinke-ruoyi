package com.xinke.erp.controller;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.erp.service.IFinanceCoreService;

@RestController
@RequestMapping("/finance/ledger")
public class FinanceLedgerController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @PreAuthorize("@ss.hasPermi('finance:ledger:list')")
    @GetMapping("/list")
    public TableDataInfo ledgerList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(financeCoreService.selectLedgerList(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:ledger:list')")
    @GetMapping("/trial-balance")
    public TableDataInfo trialBalance(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(financeCoreService.selectTrialBalanceList(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:ledger:list')")
    @GetMapping("/trial-balance/summary")
    public AjaxResult trialBalanceSummary(@RequestParam Map<String, Object> query)
    {
        return success(financeCoreService.selectTrialBalanceSummary(query));
    }
}
