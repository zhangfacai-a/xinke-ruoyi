package com.ruoyi.erp.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.erp.mapper.FinanceCoreMapper;
import com.ruoyi.erp.service.IFinanceWorkflowService;

@Service
public class FinanceWorkflowServiceImpl implements IFinanceWorkflowService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Autowired
    private FinanceCoreMapper financeCoreMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertSettlement(Map<String, Object> form, String username)
    {
        require(form, "platformCode", "平台编码不能为空");
        require(form, "periodCode", "期间不能为空");
        require(form, "billStartDate", "账单开始日期不能为空");
        require(form, "billEndDate", "账单结束日期不能为空");

        defaultString(form, "settlementNo", nextNo("SET"));
        if (financeCoreMapper.selectSettlementByNo(text(form.get("settlementNo"))) != null)
        {
            throw new ServiceException("结算单号已存在");
        }

        BigDecimal receivableAmount = decimal(form.get("incomeAmount"))
            .subtract(decimal(form.get("refundAmount")))
            .subtract(decimal(form.get("commissionFee")))
            .subtract(decimal(form.get("paymentFee")))
            .subtract(decimal(form.get("adFee")))
            .subtract(decimal(form.get("serviceFee")))
            .subtract(decimal(form.get("freightFee")))
            .subtract(decimal(form.get("otherFee")));

        form.put("receivableAmount", receivableAmount);
        form.put("receivedAmount", decimal(form.get("receivedAmount")));
        form.put("diffAmount", receivableAmount.subtract(decimal(form.get("receivedAmount"))));
        form.put("settlementStatus", defaultText(form.get("settlementStatus"), "draft"));
        form.put("reconcileStatus", defaultText(form.get("reconcileStatus"), "unmatched"));
        form.put("createBy", username);
        return financeCoreMapper.insertSettlement(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveSettlement(String settlementNo, String username)
    {
        Map<String, Object> settlement = getSettlement(settlementNo);
        if ("confirmed".equals(text(settlement.get("settlementStatus"))))
        {
            return 1;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("settlementNo", settlementNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.updateSettlementApproved(update);

        if (financeCoreMapper.selectReceivableByNo("AR-" + settlementNo) == null
            && financeCoreMapper.selectVoucherBySource("receivable", "AR-" + settlementNo) == null)
        {
            Map<String, Object> receivable = new HashMap<>();
            receivable.put("receivableNo", "AR-" + settlementNo);
            receivable.put("sourceType", "settlement");
            receivable.put("sourceNo", settlementNo);
            receivable.put("counterpartyType", "platform");
            receivable.put("counterpartyName", settlement.get("platformName"));
            receivable.put("shopId", settlement.get("shopId"));
            receivable.put("shopName", settlement.get("shopName"));
            receivable.put("periodCode", settlement.get("periodCode"));
            receivable.put("billAmount", decimal(settlement.get("receivableAmount")));
            receivable.put("writeoffAmount", ZERO);
            receivable.put("remainAmount", decimal(settlement.get("receivableAmount")));
            receivable.put("dueDate", settlement.get("billEndDate"));
            receivable.put("billStatus", isPositive(decimal(settlement.get("receivableAmount"))) ? "open" : "settled");
            receivable.put("createBy", username);
            financeCoreMapper.insertReceivable(receivable);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createSettlementVoucher(String settlementNo, String username)
    {
        Map<String, Object> settlement = getSettlement(settlementNo);
        if (financeCoreMapper.selectVoucherBySource("settlement", settlementNo) != null)
        {
            throw new ServiceException("该结算单已生成凭证");
        }

        BigDecimal receivableAmount = decimal(settlement.get("receivableAmount"));
        BigDecimal salesFee = decimal(settlement.get("commissionFee"))
            .add(decimal(settlement.get("adFee")))
            .add(decimal(settlement.get("serviceFee")))
            .add(decimal(settlement.get("freightFee")))
            .add(decimal(settlement.get("otherFee")));
        BigDecimal paymentFee = decimal(settlement.get("paymentFee"));
        BigDecimal revenueAmount = decimal(settlement.get("incomeAmount")).subtract(decimal(settlement.get("refundAmount")));
        BigDecimal debitAmount = receivableAmount.add(salesFee).add(paymentFee);
        assertBalanced(debitAmount, revenueAmount);

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucherNo", nextNo("VCH"));
        voucher.put("periodCode", settlement.get("periodCode"));
        voucher.put("voucherDate", settlement.get("billEndDate"));
        voucher.put("sourceType", "settlement");
        voucher.put("sourceNo", settlementNo);
        voucher.put("summary", "平台结算确认收入：" + settlementNo);
        voucher.put("debitAmount", debitAmount);
        voucher.put("creditAmount", revenueAmount);
        voucher.put("voucherStatus", "draft");
        voucher.put("createBy", username);
        financeCoreMapper.insertVoucher(voucher);
        Long voucherId = voucherId(voucher);

        int seq = 1;
        if (isPositive(receivableAmount))
        {
            insertEntry(voucherId, voucher, seq++, "1122", "应收账款", "debit", receivableAmount, settlement, "确认平台应收");
        }
        if (isPositive(salesFee))
        {
            insertEntry(voucherId, voucher, seq++, "6601", "销售费用", "debit", salesFee, settlement, "平台佣金/推广/物流等费用");
        }
        if (isPositive(paymentFee))
        {
            insertEntry(voucherId, voucher, seq++, "6603", "财务费用", "debit", paymentFee, settlement, "支付手续费");
        }
        insertEntry(voucherId, voucher, seq, "6001", "主营业务收入", "credit", revenueAmount, settlement, "确认平台销售收入");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertCashFlow(Map<String, Object> form, String username)
    {
        require(form, "flowType", "流水类型不能为空");
        require(form, "amount", "流水金额不能为空");
        defaultString(form, "flowNo", nextNo("CF"));
        if (financeCoreMapper.selectCashFlowByNo(text(form.get("flowNo"))) != null)
        {
            throw new ServiceException("流水号已存在");
        }
        if (isBlank(form.get("flowTime")))
        {
            form.put("flowTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        if (isBlank(form.get("businessDate")))
        {
            form.put("businessDate", text(form.get("flowTime")).substring(0, 10));
        }
        form.put("feeAmount", decimal(form.get("feeAmount")));
        form.put("matchStatus", defaultText(form.get("matchStatus"), "unmatched"));
        form.put("createBy", username);
        return financeCoreMapper.insertCashFlow(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int writeoffReceivable(String receivableNo, Map<String, Object> form, String username)
    {
        Map<String, Object> bill = getReceivable(receivableNo);
        return writeoffBill("receivable", receivableNo, bill, form, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int writeoffPayable(String payableNo, Map<String, Object> form, String username)
    {
        Map<String, Object> bill = getPayable(payableNo);
        return writeoffBill("payable", payableNo, bill, form, username);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertExpense(Map<String, Object> form, String username)
    {
        require(form, "feeTypeCode", "费用类型不能为空");
        require(form, "occurredDate", "发生日期不能为空");
        require(form, "expenseAmount", "费用金额不能为空");

        defaultString(form, "expenseNo", nextNo("EXP"));
        if (financeCoreMapper.selectExpenseByNo(text(form.get("expenseNo"))) != null)
        {
            throw new ServiceException("费用单号已存在");
        }
        Map<String, Object> feeType = financeCoreMapper.selectFeeTypeByCode(text(form.get("feeTypeCode")));
        if (feeType == null)
        {
            throw new ServiceException("费用类型不存在");
        }
        form.put("feeTypeName", defaultText(form.get("feeTypeName"), text(feeType.get("feeName"))));
        form.put("periodCode", defaultText(form.get("periodCode"), periodFromDate(text(form.get("occurredDate")))));
        BigDecimal expenseAmount = decimal(form.get("expenseAmount"));
        BigDecimal taxAmount = decimal(form.get("taxAmount"));
        form.put("taxAmount", taxAmount);
        form.put("totalAmount", expenseAmount.add(taxAmount));
        form.put("allocationDimension", defaultText(form.get("allocationDimension"), "shop_month"));
        form.put("expenseStatus", defaultText(form.get("expenseStatus"), "draft"));
        form.put("createBy", username);
        return financeCoreMapper.insertExpense(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveExpense(String expenseNo, String username)
    {
        Map<String, Object> expense = getExpense(expenseNo);
        if ("approved".equals(text(expense.get("expenseStatus"))))
        {
            return 1;
        }
        Map<String, Object> update = new HashMap<>();
        update.put("expenseNo", expenseNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.updateExpenseApproved(update);

        String payableNo = "AP-" + expenseNo;
        if (financeCoreMapper.selectPayableByNo(payableNo) == null && isPositive(decimal(expense.get("totalAmount"))))
        {
            Map<String, Object> payable = new HashMap<>();
            payable.put("payableNo", payableNo);
            payable.put("sourceType", "expense");
            payable.put("sourceNo", expenseNo);
            payable.put("counterpartyType", isBlank(expense.get("supplierId")) ? "other" : "supplier");
            payable.put("counterpartyId", expense.get("supplierId"));
            payable.put("counterpartyName", defaultText(expense.get("supplierName"), "费用供应商"));
            payable.put("periodCode", expense.get("periodCode"));
            payable.put("billAmount", decimal(expense.get("totalAmount")));
            payable.put("writeoffAmount", ZERO);
            payable.put("remainAmount", decimal(expense.get("totalAmount")));
            payable.put("dueDate", expense.get("occurredDate"));
            payable.put("billStatus", "open");
            payable.put("createBy", username);
            financeCoreMapper.insertPayable(payable);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createExpenseVoucher(String expenseNo, String username)
    {
        Map<String, Object> expense = getExpense(expenseNo);
        if (financeCoreMapper.selectVoucherBySource("expense", expenseNo) != null)
        {
            throw new ServiceException("该费用单已生成凭证");
        }

        Map<String, Object> feeType = financeCoreMapper.selectFeeTypeByCode(text(expense.get("feeTypeCode")));
        String subjectCode = feeType == null ? "6601" : defaultText(feeType.get("defaultSubjectCode"), "6601");
        String subjectName = "6603".equals(subjectCode) ? "财务费用" : ("6602".equals(subjectCode) ? "管理费用" : "销售费用");
        BigDecimal expenseAmount = decimal(expense.get("expenseAmount"));
        BigDecimal taxAmount = decimal(expense.get("taxAmount"));
        BigDecimal totalAmount = decimal(expense.get("totalAmount"));
        assertBalanced(expenseAmount.add(taxAmount), totalAmount);

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucherNo", nextNo("VCH"));
        voucher.put("periodCode", expense.get("periodCode"));
        voucher.put("voucherDate", expense.get("occurredDate"));
        voucher.put("sourceType", "expense");
        voucher.put("sourceNo", expenseNo);
        voucher.put("summary", "费用确认：" + expenseNo);
        voucher.put("debitAmount", totalAmount);
        voucher.put("creditAmount", totalAmount);
        voucher.put("voucherStatus", "draft");
        voucher.put("createBy", username);
        financeCoreMapper.insertVoucher(voucher);
        Long voucherId = voucherId(voucher);

        int seq = 1;
        insertEntry(voucherId, voucher, seq++, subjectCode, subjectName, "debit", expenseAmount, expense, "确认费用");
        if (isPositive(taxAmount))
        {
            insertEntry(voucherId, voucher, seq++, "2221", "应交税费", "debit", taxAmount, expense, "确认进项税额");
        }
        insertEntry(voucherId, voucher, seq, "2202", "应付账款", "credit", totalAmount, expense, "确认应付");
        return 1;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> runReconcile(Map<String, Object> form, String username)
    {
        require(form, "periodCode", "期间不能为空");
        form.put("reconcileType", defaultText(form.get("reconcileType"), "settlement_cash"));
        form.put("taskNo", nextNo("REC"));
        form.put("createBy", username);
        financeCoreMapper.insertReconcileTask(form);

        List<Map<String, Object>> rows = financeCoreMapper.selectSettlementReconcileRows(form);
        int diffCount = 0;
        for (Map<String, Object> row : rows)
        {
            BigDecimal expected = decimal(row.get("expectedAmount"));
            BigDecimal actual = decimal(row.get("actualAmount"));
            BigDecimal diff = actual.subtract(expected);

            Map<String, Object> update = new HashMap<>();
            update.put("settlementNo", row.get("settlementNo"));
            update.put("receivedAmount", actual);
            update.put("diffAmount", expected.subtract(actual));
            update.put("reconcileStatus", diff.abs().compareTo(new BigDecimal("0.01")) <= 0 ? "matched" : "diff");
            update.put("updateBy", username);
            financeCoreMapper.updateSettlementReconcileResult(update);

            if (!"matched".equals(update.get("reconcileStatus")))
            {
                Map<String, Object> diffRow = new HashMap<>();
                diffRow.put("taskId", form.get("taskId"));
                diffRow.put("diffNo", nextNo("DIF"));
                diffRow.put("diffType", actual.compareTo(ZERO) == 0 ? "missing_cash" : "amount_diff");
                diffRow.put("sourceType", "settlement");
                diffRow.put("sourceNo", row.get("settlementNo"));
                diffRow.put("expectedAmount", expected);
                diffRow.put("actualAmount", actual);
                diffRow.put("diffAmount", diff);
                diffRow.put("diffStatus", "open");
                diffRow.put("handleResult", "待财务核对平台结算与收款流水");
                diffRow.put("createBy", username);
                financeCoreMapper.insertReconcileDiff(diffRow);
                diffCount++;
            }
        }

        form.put("totalCount", rows.size());
        form.put("diffCount", diffCount);
        form.put("taskStatus", "completed");
        financeCoreMapper.updateReconcileTaskResult(form);
        Map<String, Object> result = new HashMap<>();
        result.put("taskNo", form.get("taskNo"));
        result.put("totalCount", rows.size());
        result.put("diffCount", diffCount);
        return result;
    }

    @Override
    public List<Map<String, Object>> selectVoucherEntryList(String voucherNo)
    {
        return financeCoreMapper.selectVoucherEntryList(voucherNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int auditVoucher(String voucherNo, String username)
    {
        if (financeCoreMapper.selectVoucherByNo(voucherNo) == null)
        {
            throw new ServiceException("凭证不存在");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("voucherNo", voucherNo);
        update.put("updateBy", username);
        return financeCoreMapper.updateVoucherPosted(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> checkPeriodClose(Map<String, Object> form, String username)
    {
        require(form, "periodCode", "期间不能为空");
        String periodCode = text(form.get("periodCode"));
        String closeScope = defaultText(form.get("closeScope"), "company");
        Map<String, Object> close = financeCoreMapper.selectPeriodCloseByCode(periodCode, closeScope);
        if (close == null)
        {
            Map<String, Object> insert = new HashMap<>();
            insert.put("periodCode", periodCode);
            insert.put("closeScope", closeScope);
            insert.put("createBy", username);
            financeCoreMapper.insertPeriodClose(insert);
        }

        Map<String, Object> summary = financeCoreMapper.selectCloseCheckSummary(periodCode);
        String orderComplete = "1";
        String settlementComplete = intValue(summary.get("draftSettlementCount")) == 0
            && intValue(summary.get("unmatchedSettlementCount")) == 0 ? "1" : "0";
        String bankComplete = intValue(summary.get("unmatchedCashCount")) == 0 ? "1" : "0";
        String voucherComplete = intValue(summary.get("draftVoucherCount")) == 0 ? "1" : "0";
        String checkStatus = "1".equals(orderComplete) && "1".equals(settlementComplete)
            && "1".equals(bankComplete) && "1".equals(voucherComplete) ? "passed" : "failed";

        Map<String, Object> update = new HashMap<>();
        update.put("periodCode", periodCode);
        update.put("closeScope", closeScope);
        update.put("checkStatus", checkStatus);
        update.put("orderComplete", orderComplete);
        update.put("settlementComplete", settlementComplete);
        update.put("bankComplete", bankComplete);
        update.put("voucherComplete", voucherComplete);
        update.put("updateBy", username);
        update.put("remark", buildCloseRemark(summary));
        financeCoreMapper.updatePeriodCloseCheck(update);

        update.putAll(summary);
        return update;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int closePeriod(Map<String, Object> form, String username)
    {
        Map<String, Object> check = checkPeriodClose(form, username);
        if (!"passed".equals(check.get("checkStatus")))
        {
            throw new ServiceException("月结检查未通过，不能关账：" + check.get("remark"));
        }
        Map<String, Object> update = new HashMap<>();
        update.put("periodCode", text(form.get("periodCode")));
        update.put("closeScope", defaultText(form.get("closeScope"), "company"));
        update.put("closeBy", username);
        update.put("updateBy", username);
        financeCoreMapper.updateAccountPeriodClosed(update);
        return financeCoreMapper.updatePeriodClosed(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int reopenPeriod(Map<String, Object> form, String username)
    {
        require(form, "periodCode", "期间不能为空");
        Map<String, Object> update = new HashMap<>();
        update.put("periodCode", text(form.get("periodCode")));
        update.put("closeScope", defaultText(form.get("closeScope"), "company"));
        update.put("updateBy", username);
        financeCoreMapper.updateAccountPeriodReopened(update);
        return financeCoreMapper.updatePeriodReopened(update);
    }

    private int writeoffBill(String billType, String billNo, Map<String, Object> bill, Map<String, Object> form, String username)
    {
        require(form, "cashFlowNo", "流水号不能为空");
        BigDecimal amount = decimal(form.get("writeoffAmount"));
        if (!isPositive(amount))
        {
            throw new ServiceException("核销金额必须大于0");
        }

        Map<String, Object> cashFlow = financeCoreMapper.selectCashFlowByNo(text(form.get("cashFlowNo")));
        if (cashFlow == null)
        {
            throw new ServiceException("流水不存在");
        }
        if ("matched".equals(text(cashFlow.get("matchStatus"))))
        {
            throw new ServiceException("流水已匹配，不能重复核销");
        }

        BigDecimal remainAmount = decimal(bill.get("remainAmount"));
        if (amount.compareTo(remainAmount) > 0)
        {
            throw new ServiceException("核销金额不能大于未核销金额");
        }

        BigDecimal newRemain = remainAmount.subtract(amount);
        Map<String, Object> update = new HashMap<>();
        update.put("billNo", billNo);
        update.put("writeoffAmount", amount);
        update.put("remainAmount", newRemain);
        update.put("billStatus", newRemain.compareTo(new BigDecimal("0.01")) <= 0 ? "settled" : "partial");
        update.put("updateBy", username);
        if ("receivable".equals(billType))
        {
            financeCoreMapper.updateReceivableWriteoff(update);
        }
        else
        {
            financeCoreMapper.updatePayableWriteoff(update);
        }

        Map<String, Object> cashUpdate = new HashMap<>();
        cashUpdate.put("flowNo", form.get("cashFlowNo"));
        cashUpdate.put("sourceType", billType);
        cashUpdate.put("sourceNo", billNo);
        cashUpdate.put("updateBy", username);
        financeCoreMapper.updateCashFlowMatched(cashUpdate);

        Map<String, Object> record = new HashMap<>();
        record.put("writeoffNo", nextNo("WOF"));
        record.put("billType", billType);
        record.put("billNo", billNo);
        record.put("cashFlowNo", form.get("cashFlowNo"));
        record.put("writeoffAmount", amount);
        record.put("operatorName", username);
        record.put("createBy", username);
        record.put("remark", form.get("remark"));
        return financeCoreMapper.insertWriteoffRecord(record);
    }

    private void insertEntry(Long voucherId, Map<String, Object> voucher, int seq, String subjectCode, String subjectName,
        String direction, BigDecimal amount, Map<String, Object> source, String summary)
    {
        if (!isPositive(amount))
        {
            return;
        }
        Map<String, Object> entry = new HashMap<>();
        entry.put("voucherId", voucherId);
        entry.put("voucherNo", voucher.get("voucherNo"));
        entry.put("entrySeq", seq);
        entry.put("subjectCode", subjectCode);
        entry.put("subjectName", subjectName);
        entry.put("direction", direction);
        entry.put("amount", amount);
        entry.put("shopId", source.get("shopId"));
        entry.put("skuId", source.get("skuId"));
        entry.put("counterpartyName", source.get("counterpartyName"));
        entry.put("summary", summary);
        financeCoreMapper.insertVoucherEntry(entry);
    }

    private Map<String, Object> getSettlement(String settlementNo)
    {
        Map<String, Object> settlement = financeCoreMapper.selectSettlementByNo(settlementNo);
        if (settlement == null)
        {
            throw new ServiceException("结算单不存在");
        }
        return settlement;
    }

    private Map<String, Object> getReceivable(String receivableNo)
    {
        Map<String, Object> bill = financeCoreMapper.selectReceivableByNo(receivableNo);
        if (bill == null)
        {
            throw new ServiceException("应收单不存在");
        }
        return bill;
    }

    private Map<String, Object> getPayable(String payableNo)
    {
        Map<String, Object> bill = financeCoreMapper.selectPayableByNo(payableNo);
        if (bill == null)
        {
            throw new ServiceException("应付单不存在");
        }
        return bill;
    }

    private Map<String, Object> getExpense(String expenseNo)
    {
        Map<String, Object> expense = financeCoreMapper.selectExpenseByNo(expenseNo);
        if (expense == null)
        {
            throw new ServiceException("费用单不存在");
        }
        return expense;
    }

    private Long voucherId(Map<String, Object> voucher)
    {
        Object value = voucher.get("voucherId");
        if (value instanceof Number)
        {
            return ((Number) value).longValue();
        }
        Map<String, Object> row = financeCoreMapper.selectVoucherByNo(text(voucher.get("voucherNo")));
        if (row == null)
        {
            throw new ServiceException("凭证生成失败");
        }
        return ((Number) row.get("voucherId")).longValue();
    }

    private void assertBalanced(BigDecimal debitAmount, BigDecimal creditAmount)
    {
        if (debitAmount.subtract(creditAmount).abs().compareTo(new BigDecimal("0.01")) > 0)
        {
            throw new ServiceException("借贷不平衡，不能生成凭证");
        }
    }

    private String buildCloseRemark(Map<String, Object> summary)
    {
        List<String> messages = new ArrayList<>();
        messages.add("草稿结算单：" + intValue(summary.get("draftSettlementCount")));
        messages.add("未完成对账结算单：" + intValue(summary.get("unmatchedSettlementCount")));
        messages.add("未匹配流水：" + intValue(summary.get("unmatchedCashCount")));
        messages.add("未过账凭证：" + intValue(summary.get("draftVoucherCount")));
        messages.add("未结应收：" + intValue(summary.get("openReceivableCount")));
        messages.add("未结应付：" + intValue(summary.get("openPayableCount")));
        return String.join("；", messages);
    }

    private void require(Map<String, Object> form, String key, String message)
    {
        if (isBlank(form.get(key)))
        {
            throw new ServiceException(message);
        }
    }

    private void defaultString(Map<String, Object> form, String key, String value)
    {
        if (isBlank(form.get(key)))
        {
            form.put(key, value);
        }
    }

    private String defaultText(Object value, String defaultValue)
    {
        return isBlank(value) ? defaultValue : text(value);
    }

    private boolean isBlank(Object value)
    {
        return value == null || text(value).trim().isEmpty();
    }

    private String text(Object value)
    {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private BigDecimal decimal(Object value)
    {
        if (value == null || text(value).isEmpty())
        {
            return ZERO;
        }
        if (value instanceof BigDecimal)
        {
            return (BigDecimal) value;
        }
        if (value instanceof Number)
        {
            return new BigDecimal(value.toString());
        }
        return new BigDecimal(text(value));
    }

    private boolean isPositive(BigDecimal value)
    {
        return value != null && value.compareTo(ZERO) > 0;
    }

    private int intValue(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        if (value instanceof Number)
        {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(text(value));
    }

    private String periodFromDate(String date)
    {
        if (date == null || date.length() < 7)
        {
            return LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        }
        return date.substring(0, 7);
    }

    private String nextNo(String prefix)
    {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
    }
}
