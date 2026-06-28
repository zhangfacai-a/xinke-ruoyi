package com.ruoyi.erp.service;

import java.util.List;
import java.util.Map;

public interface IFinanceCoreService
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
}
