package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.FinanceCoreMapper;
import com.xinke.erp.service.IFinanceWorkflowService;

@ExtendWith(MockitoExtension.class)
class FinancePayableServiceImplTest
{
    @Mock private FinanceCoreMapper financeCoreMapper;
    @Mock private IFinanceWorkflowService financeWorkflowService;
    @InjectMocks private FinancePayableServiceImpl service;

    @Test
    void matchingInvoiceIsMarkedMatched()
    {
        mockPurchase("100.00", 10, 10, 0);
        when(financeCoreMapper.insertSupplierInvoice(any())).thenAnswer(invocation -> {
            Map<String, Object> invoice = invocation.getArgument(0);
            invoice.put("invoiceId", 88L);
            return 1;
        });

        Map<String, Object> result = service.createSupplierInvoice(invoiceForm("100.00", 10), "admin");

        assertEquals("matched", result.get("matchStatus"));
        assertEquals(new BigDecimal("1130.00"), result.get("totalAmount"));
        ArgumentCaptor<Map<String, Object>> invoiceCaptor = mapCaptor();
        verify(financeCoreMapper).insertSupplierInvoice(invoiceCaptor.capture());
        assertEquals("pending", invoiceCaptor.getValue().get("invoiceStatus"));
        assertEquals("matched", invoiceCaptor.getValue().get("matchStatus"));
        verify(financeCoreMapper).insertSupplierInvoiceItem(any());
    }

    @Test
    void priceOutsideToleranceCreatesException()
    {
        mockPurchase("100.00", 10, 10, 0);
        when(financeCoreMapper.insertSupplierInvoice(any())).thenAnswer(invocation -> {
            Map<String, Object> invoice = invocation.getArgument(0);
            invoice.put("invoiceId", 89L);
            return 1;
        });
        Map<String, Object> form = invoiceForm("110.00", 10);
        form.put("totalAmount", new BigDecimal("1243.00"));

        Map<String, Object> result = service.createSupplierInvoice(form, "admin");

        assertEquals("exception", result.get("matchStatus"));
        ArgumentCaptor<Map<String, Object>> itemCaptor = mapCaptor();
        verify(financeCoreMapper).insertSupplierInvoiceItem(itemCaptor.capture());
        assertEquals("exception", itemCaptor.getValue().get("lineMatchStatus"));
    }

    @Test
    void exceptionInvoiceCannotBeApprovedWithoutReason()
    {
        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNo", "VIN-1");
        invoice.put("invoiceStatus", "pending");
        invoice.put("matchStatus", "exception");
        invoice.put("periodCode", "2026-07");
        when(financeCoreMapper.selectSupplierInvoiceByNo("VIN-1")).thenReturn(invoice);

        assertThrows(ServiceException.class,
            () -> service.approveSupplierInvoice("VIN-1", Map.of("overrideMatch", true, "exceptionReason", "短"), "admin"));
    }

    @Test
    void paymentRequestCannotExceedUnreservedBalance()
    {
        Map<String, Object> payable = new LinkedHashMap<>();
        payable.put("payableNo", "AP-1");
        payable.put("periodCode", "2026-07");
        payable.put("remainAmount", new BigDecimal("100.00"));
        payable.put("billStatus", "open");
        when(financeCoreMapper.selectPayableByNo("AP-1")).thenReturn(payable);
        when(financeCoreMapper.selectReservedPaymentAmount("AP-1")).thenReturn(new BigDecimal("80.00"));

        ServiceException exception = assertThrows(ServiceException.class,
            () -> service.createPaymentRequest("AP-1", Map.of("paymentAmount", 30), "admin"));

        assertEquals(true, exception.getMessage().contains("20.00"));
    }

    @Test
    void paymentExecutionRequiresBankReference()
    {
        when(financeCoreMapper.selectPaymentRequestByNo("PAY-1")).thenReturn(Map.of(
            "paymentNo", "PAY-1", "paymentStatus", "approved"));

        assertThrows(ServiceException.class,
            () -> service.executePaymentRequest("PAY-1", Map.of("bankAccountId", 1L), "admin"));
    }

    @Test
    void submittedPaymentCanBeRejectedWithReason()
    {
        when(financeCoreMapper.selectPaymentRequestByNo("PAY-2")).thenReturn(Map.of(
            "paymentNo", "PAY-2", "paymentStatus", "submitted"));
        when(financeCoreMapper.updatePaymentRequestRejected(any())).thenReturn(1);

        assertEquals(1, service.rejectPaymentRequest("PAY-2", Map.of("rejectReason", "收款账户信息填写错误"), "finance"));

        ArgumentCaptor<Map<String, Object>> captor = mapCaptor();
        verify(financeCoreMapper).updatePaymentRequestRejected(captor.capture());
        assertEquals("收款账户信息填写错误", captor.getValue().get("rejectReason"));
    }

    @Test
    void paymentRejectRequiresReason()
    {
        when(financeCoreMapper.selectPaymentRequestByNo("PAY-3")).thenReturn(Map.of(
            "paymentNo", "PAY-3", "paymentStatus", "submitted"));

        assertThrows(ServiceException.class,
            () -> service.rejectPaymentRequest("PAY-3", Map.of("rejectReason", "太短"), "finance"));
    }

    private void mockPurchase(String unitPrice, int orderQuantity, int receivedQuantity, int invoicedQuantity)
    {
        Map<String, Object> purchase = new LinkedHashMap<>();
        purchase.put("purchaseNo", "PO-1");
        purchase.put("purchaseStatus", "completed");
        purchase.put("supplierId", 1L);
        purchase.put("supplierName", "测试供应商");
        purchase.put("paymentDays", 30);
        purchase.put("receiptRefs", "RCV-1");
        when(financeCoreMapper.selectInvoicePurchaseByNo("PO-1")).thenReturn(purchase);
        when(financeCoreMapper.selectSupplierInvoiceBySupplierNo(1L, "SUP-INV-1")).thenReturn(null);

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("purchaseItemId", 11L);
        item.put("skuId", 21L);
        item.put("skuCode", "SKU-21");
        item.put("skuName", "测试商品");
        item.put("orderQuantity", orderQuantity);
        item.put("receivedQuantity", receivedQuantity);
        item.put("invoicedQuantity", invoicedQuantity);
        item.put("availableReceivedQuantity", receivedQuantity - invoicedQuantity);
        item.put("orderUnitPrice", new BigDecimal(unitPrice));
        item.put("taxRate", new BigDecimal("0.13"));
        when(financeCoreMapper.selectInvoicePurchaseItemList("PO-1")).thenReturn(List.of(item));
    }

    private Map<String, Object> invoiceForm(String invoicePrice, int quantity)
    {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("purchaseItemId", 11L);
        item.put("invoiceQuantity", quantity);
        item.put("invoiceUnitPrice", new BigDecimal(invoicePrice));
        item.put("taxRate", new BigDecimal("0.13"));

        Map<String, Object> form = new LinkedHashMap<>();
        form.put("purchaseNo", "PO-1");
        form.put("supplierInvoiceNo", "SUP-INV-1");
        form.put("invoiceDate", "2026-07-17");
        form.put("totalAmount", new BigDecimal("1130.00"));
        form.put("priceTolerancePercent", new BigDecimal("3.00"));
        form.put("quantityTolerance", BigDecimal.ZERO);
        form.put("amountTolerance", BigDecimal.ONE);
        form.put("items", List.of(item));
        return form;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private ArgumentCaptor<Map<String, Object>> mapCaptor()
    {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Map.class);
    }
}
