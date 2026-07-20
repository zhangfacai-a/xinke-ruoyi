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
@RequestMapping("/finance/reconcile")
public class FinanceReconcileController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:reconcile:list')")
    @GetMapping("/task/list")
    public TableDataInfo taskList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectReconcileTaskList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:reconcile:list')")
    @GetMapping("/diff/list")
    public TableDataInfo diffList(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectReconcileDiffList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:reconcile:run')")
    @Log(title = "财务对账任务", businessType = BusinessType.INSERT)
    @PostMapping("/run")
    public AjaxResult run(@RequestBody Map<String, Object> form)
    {
        return success(financeWorkflowService.runReconcile(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:reconcile:handle')")
    @Log(title = "财务对账差异处理", businessType = BusinessType.UPDATE)
    @PostMapping("/diff/{diffNo}/resolve")
    public AjaxResult resolve(@PathVariable String diffNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.resolveReconcileDiff(diffNo, form, getUsername()));
    }
}
