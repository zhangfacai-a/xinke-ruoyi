package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.service.IFinanceCoreService;
import com.xinke.erp.service.IFinanceWorkflowService;

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

    @PreAuthorize("@ss.hasPermi('finance:cashflow:list')")
    @GetMapping("/summary")
    public AjaxResult summary(@RequestParam Map<String, Object> query)
    {
        return success(financeCoreService.selectCashFlowSummary(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:cashflow:list')")
    @GetMapping("/account-options")
    public AjaxResult accountOptions()
    {
        return success(financeCoreService.selectCashFlowAccountOptions());
    }

    @PreAuthorize("@ss.hasPermi('finance:cashflow:add')")
    @Log(title = "财务收付款流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.insertCashFlow(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:cashflow:post')")
    @Log(title = "财务流水入账", businessType = BusinessType.UPDATE)
    @PostMapping("/{flowNo}/post")
    public AjaxResult post(@PathVariable String flowNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.postCashFlow(flowNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:cashflow:edit')")
    @Log(title = "财务流水草稿作废", businessType = BusinessType.UPDATE)
    @PostMapping("/{flowNo}/void")
    public AjaxResult voidDraft(@PathVariable String flowNo)
    {
        return toAjax(financeWorkflowService.voidCashFlow(flowNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:cashflow:reverse')")
    @Log(title = "财务流水冲销", businessType = BusinessType.UPDATE)
    @PostMapping("/{flowNo}/reverse")
    public AjaxResult reverse(@PathVariable String flowNo, @RequestBody Map<String, Object> form)
    {
        return success(financeWorkflowService.reverseCashFlow(flowNo, form, getUsername()));
    }
}
