package com.ruoyi.erp.controller;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
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
