package com.ruoyi.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.service.IFinanceCoreService;
import com.ruoyi.erp.service.IFinanceWorkflowService;

@RestController
@RequestMapping("/finance/cashflow")
public class FinanceCashFlowController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:cashflow:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectCashFlowList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:cashflow:add')")
    @Log(title = "财务收付款流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.insertCashFlow(form, getUsername()));
    }
}
