package com.ruoyi.erp.controller;

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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.erp.service.IFinanceCoreService;
import com.ruoyi.erp.service.IFinanceWorkflowService;

@RestController
@RequestMapping("/finance/payable")
public class FinancePayableController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectPayableList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:writeoff')")
    @Log(title = "财务应付核销", businessType = BusinessType.UPDATE)
    @PostMapping("/{payableNo}/writeoff")
    public AjaxResult writeoff(@PathVariable String payableNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.writeoffPayable(payableNo, form, getUsername()));
    }
}
