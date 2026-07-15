package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/finance/voucher")
public class FinanceVoucherController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:voucher:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectVoucherList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:voucher:query')")
    @GetMapping("/{voucherNo}/entries")
    public AjaxResult entries(@PathVariable String voucherNo)
    {
        return success(financeWorkflowService.selectVoucherEntryList(voucherNo));
    }

    @PreAuthorize("@ss.hasPermi('finance:voucher:audit')")
    @Log(title = "财务凭证过账", businessType = BusinessType.UPDATE)
    @PostMapping("/{voucherNo}/audit")
    public AjaxResult audit(@PathVariable String voucherNo)
    {
        return toAjax(financeWorkflowService.auditVoucher(voucherNo, getUsername()));
    }
}
