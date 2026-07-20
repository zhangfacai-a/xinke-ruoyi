package com.xinke.erp.service;

import java.util.List;
import java.util.Map;

public interface IFinancePayableService
{
    Map<String, Object> selectPayableSummary(Map<String, Object> query);

    List<Map<String, Object>> selectPayableAging(Map<String, Object> query);

    List<Map<String, Object>> selectSupplierInvoiceList(Map<String, Object> query);

    List<Map<String, Object>> selectSupplierInvoiceItemList(String invoiceNo);

    List<Map<String, Object>> selectInvoicePurchaseOptions(Map<String, Object> query);

    Map<String, Object> selectPurchaseInvoiceContext(String purchaseNo);

    Map<String, Object> createSupplierInvoice(Map<String, Object> form, String username);

    int approveSupplierInvoice(String invoiceNo, Map<String, Object> form, String username);

    List<Map<String, Object>> selectPaymentRequestList(Map<String, Object> query);

    List<Map<String, Object>> selectBankAccountOptions();

    int createPaymentRequest(String payableNo, Map<String, Object> form, String username);

    int approvePaymentRequest(String paymentNo, String username);

    int rejectPaymentRequest(String paymentNo, Map<String, Object> form, String username);

    int executePaymentRequest(String paymentNo, Map<String, Object> form, String username);
}
