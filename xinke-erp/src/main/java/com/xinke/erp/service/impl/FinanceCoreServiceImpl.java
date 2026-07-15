package com.xinke.erp.service.impl;

import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xinke.erp.mapper.FinanceCoreMapper;
import com.xinke.erp.service.IFinanceCoreService;

@Service
public class FinanceCoreServiceImpl implements IFinanceCoreService
{
    @Autowired
    private FinanceCoreMapper financeCoreMapper;

    @Override
    public List<Map<String, Object>> selectPeriodList(Map<String, Object> query) { return financeCoreMapper.selectPeriodList(query); }
    @Override
    public List<Map<String, Object>> selectSubjectList(Map<String, Object> query) { return financeCoreMapper.selectSubjectList(query); }
    @Override
    public List<Map<String, Object>> selectFeeTypeList(Map<String, Object> query) { return financeCoreMapper.selectFeeTypeList(query); }
    @Override
    public List<Map<String, Object>> selectPlatformAccountList(Map<String, Object> query) { return financeCoreMapper.selectPlatformAccountList(query); }
    @Override
    public List<Map<String, Object>> selectBankAccountList(Map<String, Object> query) { return financeCoreMapper.selectBankAccountList(query); }
    @Override
    public List<Map<String, Object>> selectSettlementList(Map<String, Object> query) { return financeCoreMapper.selectSettlementList(query); }
    @Override
    public List<Map<String, Object>> selectReceivableList(Map<String, Object> query) { return financeCoreMapper.selectReceivableList(query); }
    @Override
    public List<Map<String, Object>> selectPayableList(Map<String, Object> query) { return financeCoreMapper.selectPayableList(query); }
    @Override
    public List<Map<String, Object>> selectCashFlowList(Map<String, Object> query) { return financeCoreMapper.selectCashFlowList(query); }
    @Override
    public List<Map<String, Object>> selectExpenseList(Map<String, Object> query) { return financeCoreMapper.selectExpenseList(query); }
    @Override
    public List<Map<String, Object>> selectVoucherList(Map<String, Object> query) { return financeCoreMapper.selectVoucherList(query); }
    @Override
    public List<Map<String, Object>> selectReconcileTaskList(Map<String, Object> query) { return financeCoreMapper.selectReconcileTaskList(query); }
    @Override
    public List<Map<String, Object>> selectReconcileDiffList(Map<String, Object> query) { return financeCoreMapper.selectReconcileDiffList(query); }
    @Override
    public List<Map<String, Object>> selectPeriodCloseList(Map<String, Object> query) { return financeCoreMapper.selectPeriodCloseList(query); }
}
