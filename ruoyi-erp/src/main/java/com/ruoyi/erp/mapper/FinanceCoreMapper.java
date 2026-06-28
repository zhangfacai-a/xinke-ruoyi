package com.ruoyi.erp.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FinanceCoreMapper
{
    List<Map<String, Object>> selectPeriodList(Map<String, Object> query);
    List<Map<String, Object>> selectSubjectList(Map<String, Object> query);
    List<Map<String, Object>> selectFeeTypeList(Map<String, Object> query);
    List<Map<String, Object>> selectPlatformAccountList(Map<String, Object> query);
    List<Map<String, Object>> selectBankAccountList(Map<String, Object> query);
    List<Map<String, Object>> selectSettlementList(Map<String, Object> query);
    List<Map<String, Object>> selectReceivableList(Map<String, Object> query);
    List<Map<String, Object>> selectPayableList(Map<String, Object> query);
    List<Map<String, Object>> selectCashFlowList(Map<String, Object> query);
    List<Map<String, Object>> selectExpenseList(Map<String, Object> query);
    List<Map<String, Object>> selectVoucherList(Map<String, Object> query);
    List<Map<String, Object>> selectReconcileTaskList(Map<String, Object> query);
    List<Map<String, Object>> selectReconcileDiffList(Map<String, Object> query);
    List<Map<String, Object>> selectPeriodCloseList(Map<String, Object> query);

    Map<String, Object> selectSettlementByNo(@Param("settlementNo") String settlementNo);
    Map<String, Object> selectReceivableByNo(@Param("receivableNo") String receivableNo);
    Map<String, Object> selectPayableByNo(@Param("payableNo") String payableNo);
    Map<String, Object> selectCashFlowByNo(@Param("flowNo") String flowNo);
    Map<String, Object> selectExpenseByNo(@Param("expenseNo") String expenseNo);
    Map<String, Object> selectFeeTypeByCode(@Param("feeCode") String feeCode);
    Map<String, Object> selectVoucherByNo(@Param("voucherNo") String voucherNo);
    Map<String, Object> selectVoucherBySource(@Param("sourceType") String sourceType, @Param("sourceNo") String sourceNo);
    List<Map<String, Object>> selectVoucherEntryList(@Param("voucherNo") String voucherNo);
    List<Map<String, Object>> selectSettlementReconcileRows(Map<String, Object> query);
    Map<String, Object> selectCloseCheckSummary(@Param("periodCode") String periodCode);
    Map<String, Object> selectPeriodCloseByCode(@Param("periodCode") String periodCode, @Param("closeScope") String closeScope);

    int insertSettlement(Map<String, Object> form);
    int updateSettlementApproved(Map<String, Object> form);
    int updateSettlementReconcileResult(Map<String, Object> form);
    int insertReceivable(Map<String, Object> form);
    int updateReceivableWriteoff(Map<String, Object> form);
    int insertCashFlow(Map<String, Object> form);
    int updateCashFlowMatched(Map<String, Object> form);
    int insertWriteoffRecord(Map<String, Object> form);
    int insertExpense(Map<String, Object> form);
    int updateExpenseApproved(Map<String, Object> form);
    int insertPayable(Map<String, Object> form);
    int updatePayableWriteoff(Map<String, Object> form);
    int insertVoucher(Map<String, Object> form);
    int insertVoucherEntry(Map<String, Object> form);
    int updateVoucherPosted(Map<String, Object> form);
    int insertReconcileTask(Map<String, Object> form);
    int insertReconcileDiff(Map<String, Object> form);
    int updateReconcileTaskResult(Map<String, Object> form);
    int insertPeriodClose(Map<String, Object> form);
    int updatePeriodCloseCheck(Map<String, Object> form);
    int updatePeriodClosed(Map<String, Object> form);
    int updatePeriodReopened(Map<String, Object> form);
    int updateAccountPeriodClosed(Map<String, Object> form);
    int updateAccountPeriodReopened(Map<String, Object> form);
}
