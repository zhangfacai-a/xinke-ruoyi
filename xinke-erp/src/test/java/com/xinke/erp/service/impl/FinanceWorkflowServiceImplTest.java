package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
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

@ExtendWith(MockitoExtension.class)
class FinanceWorkflowServiceImplTest
{
    @Mock private FinanceCoreMapper financeCoreMapper;
    @InjectMocks private FinanceWorkflowServiceImpl service;

    @Test
    void incomingCashFlowCalculatesNetAmountAndPosts()
    {
        when(financeCoreMapper.selectCashFlowByNo(any())).thenReturn(null);
        when(financeCoreMapper.insertCashFlow(any())).thenReturn(1);
        Map<String, Object> form = new HashMap<>();
        form.put("flowType", "in");
        form.put("flowCategory", "receivable");
        form.put("bankAccountId", 1L);
        form.put("amount", new BigDecimal("1000.00"));
        form.put("feeAmount", new BigDecimal("6.00"));
        form.put("businessDate", "2026-07-17");
        form.put("flowTime", "2026-07-17 12:00:00");

        assertEquals(1, service.insertCashFlow(form, "admin"));

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(financeCoreMapper).insertCashFlow(captor.capture());
        assertEquals(new BigDecimal("994.00"), captor.getValue().get("netAmount"));
        assertEquals("posted", captor.getValue().get("entryStatus"));
        assertEquals("unmatched", captor.getValue().get("matchStatus"));
    }

    @Test
    void cashFlowRequiresExactlyOneAccount()
    {
        Map<String, Object> form = new HashMap<>();
        form.put("flowType", "out");
        form.put("amount", new BigDecimal("100.00"));
        form.put("bankAccountId", 1L);
        form.put("platformAccountId", 2L);
        assertThrows(ServiceException.class, () -> service.insertCashFlow(form, "admin"));
    }

    @Test
    void writeoffUsesRemainingCashFlowBalance()
    {
        when(financeCoreMapper.selectReceivableByNo("AR-1")).thenReturn(Map.of(
            "receivableNo", "AR-1", "periodCode", "2026-07", "remainAmount", new BigDecimal("80.00")));
        when(financeCoreMapper.selectCashFlowByNo("CF-1")).thenReturn(Map.ofEntries(
            Map.entry("flowNo", "CF-1"), Map.entry("flowType", "in"), Map.entry("entryStatus", "posted"),
            Map.entry("matchStatus", "partial"), Map.entry("amount", new BigDecimal("100.00")),
            Map.entry("settledAmount", new BigDecimal("20.00")), Map.entry("businessDate", "2026-07-17")));
        when(financeCoreMapper.updateReceivableWriteoff(any())).thenReturn(1);
        when(financeCoreMapper.updateCashFlowMatched(any())).thenReturn(1);
        when(financeCoreMapper.insertWriteoffRecord(any())).thenReturn(1);
        Map<String, Object> form = new HashMap<>();
        form.put("cashFlowNo", "CF-1");
        form.put("writeoffAmount", new BigDecimal("50.00"));

        assertEquals(1, service.writeoffReceivable("AR-1", form, "admin"));

        ArgumentCaptor<Map<String, Object>> cashCaptor = ArgumentCaptor.forClass(Map.class);
        verify(financeCoreMapper).updateCashFlowMatched(cashCaptor.capture());
        assertEquals(new BigDecimal("50.00"), cashCaptor.getValue().get("writeoffAmount"));
    }

    @Test
    void writeoffCannotExceedRemainingCashBalance()
    {
        when(financeCoreMapper.selectReceivableByNo("AR-1")).thenReturn(Map.of(
            "receivableNo", "AR-1", "periodCode", "2026-07", "remainAmount", new BigDecimal("100.00")));
        when(financeCoreMapper.selectCashFlowByNo("CF-1")).thenReturn(Map.ofEntries(
            Map.entry("flowNo", "CF-1"), Map.entry("flowType", "in"), Map.entry("entryStatus", "posted"),
            Map.entry("matchStatus", "partial"), Map.entry("amount", new BigDecimal("100.00")),
            Map.entry("settledAmount", new BigDecimal("80.00")), Map.entry("businessDate", "2026-07-17")));
        Map<String, Object> form = Map.of("cashFlowNo", "CF-1", "writeoffAmount", new BigDecimal("30.00"));
        assertThrows(ServiceException.class, () -> service.writeoffReceivable("AR-1", form, "admin"));
    }

    @Test
    void draftSettlementCannotCreateVoucher()
    {
        when(financeCoreMapper.selectSettlementByNo("SET-1")).thenReturn(Map.of(
            "settlementNo", "SET-1", "periodCode", "2026-07", "settlementStatus", "draft"));

        assertThrows(ServiceException.class, () -> service.createSettlementVoucher("SET-1", "admin"));
    }

    @Test
    void settlementCannotHaveNegativeReceivable()
    {
        Map<String, Object> form = new HashMap<>();
        form.put("platformCode", "douyin");
        form.put("periodCode", "2026-07");
        form.put("billStartDate", "2026-07-01");
        form.put("billEndDate", "2026-07-31");
        form.put("incomeAmount", new BigDecimal("100.00"));
        form.put("refundAmount", new BigDecimal("120.00"));

        assertThrows(ServiceException.class, () -> service.insertSettlement(form, "admin"));
    }

    @Test
    void expenseAmountMustBePositive()
    {
        when(financeCoreMapper.selectExpenseByNo(any())).thenReturn(null);
        when(financeCoreMapper.selectFeeTypeByCode("ad_fee")).thenReturn(Map.of("feeName", "推广费"));
        Map<String, Object> form = new HashMap<>();
        form.put("feeTypeCode", "ad_fee");
        form.put("occurredDate", "2026-07-19");
        form.put("expenseAmount", BigDecimal.ZERO);

        assertThrows(ServiceException.class, () -> service.insertExpense(form, "admin"));
    }

    @Test
    void draftExpenseCannotCreateVoucher()
    {
        when(financeCoreMapper.selectExpenseByNo("EXP-1")).thenReturn(Map.of(
            "expenseNo", "EXP-1", "periodCode", "2026-07", "expenseStatus", "draft"));

        assertThrows(ServiceException.class, () -> service.createExpenseVoucher("EXP-1", "admin"));
    }

    @Test
    void voucherWithoutEntriesCannotBePosted()
    {
        when(financeCoreMapper.selectVoucherByNo("VCH-1")).thenReturn(Map.of(
            "voucherNo", "VCH-1", "periodCode", "2026-07", "voucherStatus", "draft",
            "debitAmount", new BigDecimal("100.00"), "creditAmount", new BigDecimal("100.00")));
        when(financeCoreMapper.selectVoucherEntryList("VCH-1")).thenReturn(List.of());

        assertThrows(ServiceException.class, () -> service.auditVoucher("VCH-1", "admin"));
    }

    @Test
    void postingVoucherWritesLedgerAndRebuildsTrialBalance()
    {
        when(financeCoreMapper.selectVoucherByNo("VCH-2")).thenReturn(Map.of(
            "voucherNo", "VCH-2", "periodCode", "2026-07", "voucherStatus", "draft",
            "debitAmount", new BigDecimal("100.00"), "creditAmount", new BigDecimal("100.00")));
        when(financeCoreMapper.selectVoucherEntryList("VCH-2")).thenReturn(List.of(
            Map.of("direction", "debit", "amount", new BigDecimal("100.00")),
            Map.of("direction", "credit", "amount", new BigDecimal("100.00"))));
        when(financeCoreMapper.updateVoucherPosted(any())).thenReturn(1);
        when(financeCoreMapper.selectLedgerEntryCount("VCH-2")).thenReturn(0);
        when(financeCoreMapper.insertLedgerEntriesFromVoucher("VCH-2")).thenReturn(2);

        assertEquals(1, service.auditVoucher("VCH-2", "admin"));

        verify(financeCoreMapper).insertLedgerEntriesFromVoucher("VCH-2");
        verify(financeCoreMapper).deleteTrialBalanceByPeriod("2026-07");
        verify(financeCoreMapper).rebuildTrialBalance("2026-07");
    }

    @Test
    void reconcileDifferenceRequiresARealHandlingConclusion()
    {
        when(financeCoreMapper.selectReconcileDiffByNo("DIF-1")).thenReturn(Map.of(
            "diffNo", "DIF-1", "diffStatus", "open"));

        assertThrows(ServiceException.class,
            () -> service.resolveReconcileDiff("DIF-1", Map.of("handleResult", "短"), "admin"));
    }

    @Test
    void reconcileDifferenceCanBeResolved()
    {
        when(financeCoreMapper.selectReconcileDiffByNo("DIF-2")).thenReturn(Map.of(
            "diffNo", "DIF-2", "diffStatus", "open"));
        when(financeCoreMapper.updateReconcileDiffResolved(any())).thenReturn(1);

        assertEquals(1, service.resolveReconcileDiff("DIF-2", Map.of("handleResult", "已核对银行回单并完成调整"), "admin"));

        verify(financeCoreMapper).updateReconcileDiffResolved(any());
    }

    @Test
    void reopeningPeriodRequiresReason()
    {
        assertThrows(ServiceException.class,
            () -> service.reopenPeriod(Map.of("periodCode", "2026-07", "reason", "短"), "admin"));
    }
}
