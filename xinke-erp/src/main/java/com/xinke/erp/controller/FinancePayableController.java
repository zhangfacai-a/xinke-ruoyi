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
import com.xinke.erp.service.IFinancePayableService;
import com.xinke.erp.service.IFinanceWorkflowService;

@RestController
@RequestMapping("/finance/payable")
public class FinancePayableController extends BaseController
{
    @Autowired
    private IFinanceCoreService financeCoreService;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @Autowired
    private IFinancePayableService financePayableService;

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam Map<String, Object> query)
    {
        startPage();
        List<Map<String, Object>> list = financeCoreService.selectPayableList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/summary")
    public AjaxResult summary(@RequestParam Map<String, Object> query)
    {
        return success(financePayableService.selectPayableSummary(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/aging")
    public AjaxResult aging(@RequestParam Map<String, Object> query)
    {
        return success(financePayableService.selectPayableAging(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/invoice/list")
    public TableDataInfo invoiceList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(financePayableService.selectSupplierInvoiceList(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/invoice/{invoiceNo}/items")
    public AjaxResult invoiceItems(@PathVariable String invoiceNo)
    {
        return success(financePayableService.selectSupplierInvoiceItemList(invoiceNo));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/invoice/purchase/options")
    public AjaxResult purchaseOptions(@RequestParam Map<String, Object> query)
    {
        return success(financePayableService.selectInvoicePurchaseOptions(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/invoice/purchase/{purchaseNo}")
    public AjaxResult purchaseContext(@PathVariable String purchaseNo)
    {
        return success(financePayableService.selectPurchaseInvoiceContext(purchaseNo));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:add')")
    @Log(title = "供应商发票三单匹配", businessType = BusinessType.INSERT)
    @PostMapping("/invoice")
    public AjaxResult createInvoice(@RequestBody Map<String, Object> form)
    {
        return success(financePayableService.createSupplierInvoice(form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:approve')")
    @Log(title = "供应商发票确认应付", businessType = BusinessType.UPDATE)
    @PostMapping("/invoice/{invoiceNo}/approve")
    public AjaxResult approveInvoice(@PathVariable String invoiceNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financePayableService.approveSupplierInvoice(invoiceNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/payment/list")
    public TableDataInfo paymentList(@RequestParam Map<String, Object> query)
    {
        startPage();
        return getDataTable(financePayableService.selectPaymentRequestList(query));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:list')")
    @GetMapping("/payment/bank/options")
    public AjaxResult bankOptions()
    {
        return success(financePayableService.selectBankAccountOptions());
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:payment')")
    @Log(title = "应付付款申请", businessType = BusinessType.INSERT)
    @PostMapping("/{payableNo}/payment-request")
    public AjaxResult createPaymentRequest(@PathVariable String payableNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financePayableService.createPaymentRequest(payableNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:approve')")
    @Log(title = "付款申请审批", businessType = BusinessType.UPDATE)
    @PostMapping("/payment/{paymentNo}/approve")
    public AjaxResult approvePaymentRequest(@PathVariable String paymentNo)
    {
        return toAjax(financePayableService.approvePaymentRequest(paymentNo, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:approve')")
    @Log(title = "付款申请驳回", businessType = BusinessType.UPDATE)
    @PostMapping("/payment/{paymentNo}/reject")
    public AjaxResult rejectPaymentRequest(@PathVariable String paymentNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financePayableService.rejectPaymentRequest(paymentNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:payment')")
    @Log(title = "付款执行与自动核销", businessType = BusinessType.UPDATE)
    @PostMapping("/payment/{paymentNo}/execute")
    public AjaxResult executePaymentRequest(@PathVariable String paymentNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financePayableService.executePaymentRequest(paymentNo, form, getUsername()));
    }

    @PreAuthorize("@ss.hasPermi('finance:payable:writeoff')")
    @Log(title = "财务应付核销", businessType = BusinessType.UPDATE)
    @PostMapping("/{payableNo}/writeoff")
    public AjaxResult writeoff(@PathVariable String payableNo, @RequestBody Map<String, Object> form)
    {
        return toAjax(financeWorkflowService.writeoffPayable(payableNo, form, getUsername()));
    }
}
