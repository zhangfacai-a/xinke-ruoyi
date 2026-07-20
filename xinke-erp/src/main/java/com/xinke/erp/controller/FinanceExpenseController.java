package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RequestMapping("/finance/expense")
public class FinanceExpenseController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:expense:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectExpenseList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:expense:add')")
    @Log(title = "财务费用单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.insertExpense(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:expense:edit')")
    @Log(title = "财务费用单草稿修改", businessType = BusinessType.UPDATE)
    @PutMapping("/{expenseNo}")
    public AjaxResult edit(@PathVariable String expenseNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.updateExpense(expenseNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:expense:edit')")
    @Log(title = "财务费用单草稿作废", businessType = BusinessType.UPDATE)
    @PostMapping("/{expenseNo}/void")
    public AjaxResult voidDraft(@PathVariable String expenseNo)
    {
        return toAjax(financeWorkflowService.voidExpense(expenseNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:expense:audit')")
    @Log(title = "财务费用审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{expenseNo}/approve")
    public AjaxResult approve(@PathVariable String expenseNo)
    {
        return toAjax(financeWorkflowService.approveExpense(expenseNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:voucher:add')")
    @Log(title = "费用单生成凭证", businessType = BusinessType.INSERT)
    @PostMapping("/{expenseNo}/voucher")
    public AjaxResult createVoucher(@PathVariable String expenseNo)
    {
        return toAjax(financeWorkflowService.createExpenseVoucher(expenseNo, getUsername()));
    }
}
