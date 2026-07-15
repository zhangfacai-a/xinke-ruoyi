package com.xinke.erp.controller;

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
import com.xinke.common.annotation.Log;
import com.xinke.common.core.controller.BaseController;
import com.xinke.common.core.domain.AjaxResult;
import com.xinke.common.core.page.TableDataInfo;
import com.xinke.common.enums.BusinessType;
import com.xinke.erp.service.IFinanceCoreService;
import com.xinke.erp.service.IFinanceWorkflowService;

@RestController
@RequestMapping("/finance/close")
public class FinanceCloseController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:close:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectPeriodCloseList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:close:check')")
    @Log(title = "财务月结检查", businessType = BusinessType.UPDATE)
    @PostMapping("/check")
    public AjaxResult check(@RequestBody Map<String, Object> form)
    {
        return success(financeWorkflowService.checkPeriodClose(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:close:close')")
    @Log(title = "财务月结关账", businessType = BusinessType.UPDATE)
    @PostMapping("/close")
    public AjaxResult close(@RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.closePeriod(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:close:reopen')")
    @Log(title = "财务月结反关账", businessType = BusinessType.UPDATE)
    @PostMapping("/reopen")
    public AjaxResult reopen(@RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.reopenPeriod(form, getUsername()));
    }
}
