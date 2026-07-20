package com.xinke.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/finance/settlement")
public class FinanceSettlementController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @PreAuthorize("@ss.hasPermi('finance:settlement:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectSettlementList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:settlement:add')")
    @Log(title = "财务结算单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.insertSettlement(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:settlement:edit')")
    @Log(title = "财务结算单草稿修改", businessType = BusinessType.UPDATE)
    @PutMapping("/{settlementNo}")
    public AjaxResult edit(@PathVariable String settlementNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.updateSettlement(settlementNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:settlement:edit')")
    @Log(title = "财务结算单草稿作废", businessType = BusinessType.UPDATE)
    @PostMapping("/{settlementNo}/void")
    public AjaxResult voidDraft(@PathVariable String settlementNo)
    {
        return toAjax(financeWorkflowService.voidSettlement(settlementNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:settlement:audit')")
    @Log(title = "财务结算单确认", businessType = BusinessType.UPDATE)
    @PostMapping("/{settlementNo}/approve")
    public AjaxResult approve(@PathVariable String settlementNo)
    {
        return toAjax(financeWorkflowService.approveSettlement(settlementNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:voucher:add')")
    @Log(title = "结算单生成凭证", businessType = BusinessType.INSERT)
    @PostMapping("/{settlementNo}/voucher")
    public AjaxResult createVoucher(@PathVariable String settlementNo)
    {
        return toAjax(financeWorkflowService.createSettlementVoucher(settlementNo, getUsername()));
    }
}
