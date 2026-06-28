package com.ruoyi.erp.service;

import java.util.List;
import java.util.Map;

public interface IFinanceWorkflowService
{
    int insertSettlement(Map<String, Object> form, String username);

    int approveSettlement(String settlementNo, String username);

    int createSettlementVoucher(String settlementNo, String username);

    int insertCashFlow(Map<String, Object> form, String username);

    int writeoffReceivable(String receivableNo, Map<String, Object> form, String username);

    int writeoffPayable(String payableNo, Map<String, Object> form, String username);

    int insertExpense(Map<String, Object> form, String username);

    int approveExpense(String expenseNo, String username);

    int createExpenseVoucher(String expenseNo, String username);

    Map<String, Object> runReconcile(Map<String, Object> form, String username);

    List<Map<String, Object>> selectVoucherEntryList(String voucherNo);

    int auditVoucher(String voucherNo, String username);

    Map<String, Object> checkPeriodClose(Map<String, Object> form, String username);

    int closePeriod(Map<String, Object> form, String username);

    int reopenPeriod(Map<String, Object> form, String username);
}
