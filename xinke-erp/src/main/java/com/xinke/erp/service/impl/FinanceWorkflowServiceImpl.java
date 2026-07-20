package com.xinke.erp.service.impl;

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
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.FinanceCoreMapper;
import com.xinke.erp.service.IFinanceWorkflowService;

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
        validateSettlementDates(form);

        assertPeriodOpen(text(form.get("periodCode")));
        defaultString(form, "settlementNo", nextNo("SET"));
        if (financeCoreMapper.selectSettlementByNo(text(form.get("settlementNo"))) != null)
        {
            throw new ServiceException("结算单号已存在");
        }

        BigDecimal receivableAmount = calculateSettlementReceivable(form);
        if (receivableAmount.compareTo(ZERO) < 0)
        {
            throw new ServiceException("平台应收不能小于0，请检查收入、退款和扣费金额");
        }

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
    public int updateSettlement(String settlementNo, Map<String, Object> form, String username)
    {
        Map<String, Object> current = getSettlement(settlementNo);
        if (!"draft".equals(text(current.get("settlementStatus"))))
        {
            throw new ServiceException("只有草稿结算单可以修改");
        }
        require(form, "platformCode", "平台编码不能为空");
        require(form, "periodCode", "期间不能为空");
        require(form, "billStartDate", "账单开始日期不能为空");
        require(form, "billEndDate", "账单结束日期不能为空");
        validateSettlementDates(form);
        assertPeriodOpen(form.get("periodCode"));
        BigDecimal receivableAmount = calculateSettlementReceivable(form);
        if (receivableAmount.compareTo(ZERO) < 0)
        {
            throw new ServiceException("平台应收不能小于0，请检查收入、退款和扣费金额");
        }
        form.put("settlementNo", settlementNo);
        form.put("receivableAmount", receivableAmount);
        form.put("diffAmount", receivableAmount.subtract(decimal(current.get("receivedAmount"))));
        form.put("updateBy", username);
        int rows = financeCoreMapper.updateSettlementDraft(form);
        if (rows != 1)
        {
            throw new ServiceException("结算单状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int voidSettlement(String settlementNo, String username)
    {
        Map<String, Object> current = getSettlement(settlementNo);
        assertPeriodOpen(current.get("periodCode"));
        if (!"draft".equals(text(current.get("settlementStatus"))))
        {
            throw new ServiceException("只有草稿结算单可以作废");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("settlementNo", settlementNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.voidSettlementDraft(update);
        if (rows != 1)
        {
            throw new ServiceException("结算单状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveSettlement(String settlementNo, String username)
    {
        Map<String, Object> settlement = getSettlement(settlementNo);
        assertPeriodOpen(settlement.get("periodCode"));
        String settlementStatus = text(settlement.get("settlementStatus"));
        if ("confirmed".equals(settlementStatus) || "approved".equals(settlementStatus))
        {
            return 1;
        }
        if (!"draft".equals(settlementStatus))
        {
            throw new ServiceException("只有草稿结算单可以确认");
        }

        Map<String, Object> update = new HashMap<>();
        update.put("settlementNo", settlementNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.updateSettlementApproved(update);
        if (rows != 1)
        {
            throw new ServiceException("结算单状态已变化，请刷新后重试");
        }

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
        assertPeriodOpen(settlement.get("periodCode"));
        String settlementStatus = text(settlement.get("settlementStatus"));
        if (!"confirmed".equals(settlementStatus) && !"approved".equals(settlementStatus))
        {
            throw new ServiceException("结算单确认后才能生成会计凭证");
        }
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
        String flowType = text(form.get("flowType"));
        if (!"in".equals(flowType) && !"out".equals(flowType))
        {
            throw new ServiceException("流水类型只能是收入或支出");
        }
        BigDecimal amount = decimal(form.get("amount"));
        BigDecimal feeAmount = decimal(form.get("feeAmount"));
        if (!isPositive(amount))
        {
            throw new ServiceException("流水金额必须大于0");
        }
        if (feeAmount.compareTo(ZERO) < 0)
        {
            throw new ServiceException("手续费不能小于0");
        }
        if ("in".equals(flowType) && feeAmount.compareTo(amount) >= 0)
        {
            throw new ServiceException("收入手续费必须小于收入金额");
        }
        boolean hasBankAccount = !isBlank(form.get("bankAccountId"));
        boolean hasPlatformAccount = !isBlank(form.get("platformAccountId"));
        if (hasBankAccount == hasPlatformAccount)
        {
            throw new ServiceException("银行账户和平台账户必须且只能选择一个");
        }
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
        assertPeriodOpen(periodFromDate(text(form.get("businessDate"))));
        String entryStatus = defaultText(form.get("entryStatus"), "posted");
        if (!"draft".equals(entryStatus) && !"posted".equals(entryStatus))
        {
            throw new ServiceException("入账状态只能是草稿或已入账");
        }
        form.put("amount", amount);
        form.put("feeAmount", feeAmount);
        form.put("netAmount", "in".equals(flowType) ? amount.subtract(feeAmount) : amount.add(feeAmount));
        form.put("settledAmount", ZERO);
        form.put("flowCategory", defaultText(form.get("flowCategory"), "other"));
        form.put("counterpartyType", defaultText(form.get("counterpartyType"), "other"));
        form.put("currency", defaultText(form.get("currency"), "CNY").toUpperCase());
        form.put("matchStatus", "unmatched");
        form.put("entryStatus", entryStatus);
        form.put("postedBy", "posted".equals(entryStatus) ? username : null);
        form.put("postedTime", "posted".equals(entryStatus)
            ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        form.put("createBy", username);
        int rows = financeCoreMapper.insertCashFlow(form);
        if ("posted".equals(entryStatus) && Boolean.TRUE.equals(form.get("generateVoucher")))
        {
            createCashFlowVoucher(form, username);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int postCashFlow(String flowNo, Map<String, Object> form, String username)
    {
        Map<String, Object> cashFlow = getCashFlow(flowNo);
        if ("posted".equals(text(cashFlow.get("entryStatus"))))
        {
            return 1;
        }
        if (!"draft".equals(text(cashFlow.get("entryStatus"))))
        {
            throw new ServiceException("只有草稿流水可以入账");
        }
        assertPeriodOpen(periodFromDate(text(cashFlow.get("businessDate"))));
        Map<String, Object> update = new HashMap<>();
        update.put("flowNo", flowNo);
        update.put("postedBy", username);
        int rows = financeCoreMapper.updateCashFlowPosted(update);
        if (rows != 1)
        {
            throw new ServiceException("资金流水状态已变化，请刷新后重试");
        }
        cashFlow.put("entryStatus", "posted");
        if (form == null || !Boolean.FALSE.equals(form.get("generateVoucher")))
        {
            createCashFlowVoucher(cashFlow, username);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int voidCashFlow(String flowNo, String username)
    {
        Map<String, Object> cashFlow = getCashFlow(flowNo);
        assertPeriodOpen(periodFromDate(text(cashFlow.get("businessDate"))));
        if (!"draft".equals(text(cashFlow.get("entryStatus"))))
        {
            throw new ServiceException("只有草稿流水可以作废");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("flowNo", flowNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.voidCashFlowDraft(update);
        if (rows != 1)
        {
            throw new ServiceException("资金流水状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> reverseCashFlow(String flowNo, Map<String, Object> form, String username)
    {
        Map<String, Object> original = getCashFlow(flowNo);
        if (!"posted".equals(text(original.get("entryStatus"))))
        {
            throw new ServiceException("只有已入账且未冲销的流水可以冲销");
        }
        if (decimal(original.get("settledAmount")).compareTo(new BigDecimal("0.01")) >= 0)
        {
            throw new ServiceException("流水已有核销记录，请先撤销核销后再冲销");
        }
        String reason = form == null ? "" : text(form.get("reason"));
        if (reason.length() < 4)
        {
            throw new ServiceException("请填写至少4个字的冲销原因");
        }
        assertPeriodOpen(periodFromDate(text(original.get("businessDate"))));

        Map<String, Object> reversal = new HashMap<>();
        reversal.put("flowNo", nextNo("CFR"));
        reversal.put("flowType", "in".equals(text(original.get("flowType"))) ? "out" : "in");
        reversal.put("flowCategory", "reversal");
        reversal.put("bankAccountId", original.get("bankAccountId"));
        reversal.put("platformAccountId", original.get("platformAccountId"));
        reversal.put("counterpartyType", original.get("counterpartyType"));
        reversal.put("counterpartyName", original.get("counterpartyName"));
        reversal.put("sourceType", "cash_flow_reversal");
        reversal.put("sourceNo", flowNo);
        reversal.put("currency", original.get("currency"));
        reversal.put("externalReference", original.get("externalReference"));
        reversal.put("amount", original.get("amount"));
        reversal.put("feeAmount", original.get("feeAmount"));
        reversal.put("netAmount", original.get("netAmount"));
        reversal.put("settledAmount", ZERO);
        reversal.put("flowTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        reversal.put("businessDate", LocalDate.now().toString());
        reversal.put("matchStatus", "unmatched");
        reversal.put("entryStatus", "posted");
        reversal.put("postedBy", username);
        reversal.put("postedTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        reversal.put("createBy", username);
        reversal.put("remark", "冲销 " + flowNo + "：" + reason);
        financeCoreMapper.insertCashFlow(reversal);
        createCashFlowReversalVoucher(original, reversal, username);

        Map<String, Object> update = new HashMap<>();
        update.put("flowNo", flowNo);
        update.put("reversalFlowNo", reversal.get("flowNo"));
        update.put("reversedBy", username);
        int rows = financeCoreMapper.updateCashFlowReversed(update);
        if (rows != 1)
        {
            throw new ServiceException("资金流水状态已变化，请刷新后重试");
        }
        return reversal;
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
        assertPeriodOpen(form.get("periodCode"));
        BigDecimal expenseAmount = decimal(form.get("expenseAmount"));
        BigDecimal taxAmount = decimal(form.get("taxAmount"));
        if (!isPositive(expenseAmount) || taxAmount.compareTo(ZERO) < 0)
        {
            throw new ServiceException("费用金额必须大于0，税额不能小于0");
        }
        form.put("taxAmount", taxAmount);
        form.put("totalAmount", expenseAmount.add(taxAmount));
        form.put("allocationDimension", defaultText(form.get("allocationDimension"), "shop_month"));
        form.put("expenseStatus", defaultText(form.get("expenseStatus"), "draft"));
        form.put("createBy", username);
        return financeCoreMapper.insertExpense(form);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateExpense(String expenseNo, Map<String, Object> form, String username)
    {
        Map<String, Object> current = getExpense(expenseNo);
        if (!"draft".equals(text(current.get("expenseStatus"))))
        {
            throw new ServiceException("只有草稿费用单可以修改");
        }
        require(form, "feeTypeCode", "费用类型不能为空");
        require(form, "occurredDate", "发生日期不能为空");
        require(form, "expenseAmount", "费用金额不能为空");
        Map<String, Object> feeType = financeCoreMapper.selectFeeTypeByCode(text(form.get("feeTypeCode")));
        if (feeType == null)
        {
            throw new ServiceException("费用类型不存在");
        }
        BigDecimal expenseAmount = decimal(form.get("expenseAmount"));
        BigDecimal taxAmount = decimal(form.get("taxAmount"));
        if (!isPositive(expenseAmount) || taxAmount.compareTo(ZERO) < 0)
        {
            throw new ServiceException("费用金额必须大于0，税额不能小于0");
        }
        form.put("expenseNo", expenseNo);
        form.put("feeTypeName", text(feeType.get("feeName")));
        form.put("periodCode", defaultText(form.get("periodCode"), periodFromDate(text(form.get("occurredDate")))));
        assertPeriodOpen(form.get("periodCode"));
        form.put("taxAmount", taxAmount);
        form.put("totalAmount", expenseAmount.add(taxAmount));
        form.put("allocationDimension", defaultText(form.get("allocationDimension"), "shop_month"));
        form.put("updateBy", username);
        int rows = financeCoreMapper.updateExpenseDraft(form);
        if (rows != 1)
        {
            throw new ServiceException("费用单状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int voidExpense(String expenseNo, String username)
    {
        Map<String, Object> current = getExpense(expenseNo);
        assertPeriodOpen(current.get("periodCode"));
        if (!"draft".equals(text(current.get("expenseStatus"))))
        {
            throw new ServiceException("只有草稿费用单可以作废");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("expenseNo", expenseNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.voidExpenseDraft(update);
        if (rows != 1)
        {
            throw new ServiceException("费用单状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveExpense(String expenseNo, String username)
    {
        Map<String, Object> expense = getExpense(expenseNo);
        assertPeriodOpen(expense.get("periodCode"));
        String expenseStatus = text(expense.get("expenseStatus"));
        if ("approved".equals(expenseStatus))
        {
            return 1;
        }
        if (!"draft".equals(expenseStatus))
        {
            throw new ServiceException("只有草稿费用单可以审核");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("expenseNo", expenseNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.updateExpenseApproved(update);
        if (rows != 1)
        {
            throw new ServiceException("费用单状态已变化，请刷新后重试");
        }

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
        assertPeriodOpen(expense.get("periodCode"));
        if (!"approved".equals(text(expense.get("expenseStatus"))))
        {
            throw new ServiceException("费用单审核通过后才能生成会计凭证");
        }
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
        assertPeriodOpen(form.get("periodCode"));
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
            update.put("reconcileStatus", diff.abs().compareTo(new BigDecimal("0.01")) <= 0 ? "matched" : "difference");
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
    @Transactional(rollbackFor = Exception.class)
    public int resolveReconcileDiff(String diffNo, Map<String, Object> form, String username)
    {
        Map<String, Object> diff = financeCoreMapper.selectReconcileDiffByNo(diffNo);
        if (diff == null)
        {
            throw new ServiceException("对账差异不存在");
        }
        if ("resolved".equals(text(diff.get("diffStatus"))))
        {
            return 1;
        }
        if (!"open".equals(text(diff.get("diffStatus"))))
        {
            throw new ServiceException("只有待处理差异可以标记完成");
        }
        String handleResult = form == null ? "" : text(form.get("handleResult"));
        if (handleResult.length() < 5)
        {
            throw new ServiceException("处理说明至少填写5个字");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("diffNo", diffNo);
        update.put("handleResult", handleResult);
        update.put("updateBy", username);
        int rows = financeCoreMapper.updateReconcileDiffResolved(update);
        if (rows != 1)
        {
            throw new ServiceException("差异状态已变化，请刷新后重试");
        }
        return rows;
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
        Map<String, Object> voucher = financeCoreMapper.selectVoucherByNo(voucherNo);
        if (voucher == null)
        {
            throw new ServiceException("凭证不存在");
        }
        assertPeriodOpen(voucher.get("periodCode"));
        if ("posted".equals(text(voucher.get("voucherStatus"))))
        {
            postVoucherToLedger(voucherNo, text(voucher.get("periodCode")));
            return 1;
        }
        if (!"draft".equals(text(voucher.get("voucherStatus"))))
        {
            throw new ServiceException("只有草稿凭证可以过账");
        }
        BigDecimal debitAmount = decimal(voucher.get("debitAmount"));
        BigDecimal creditAmount = decimal(voucher.get("creditAmount"));
        if (!isPositive(debitAmount) || !isPositive(creditAmount))
        {
            throw new ServiceException("凭证借贷金额必须大于0");
        }
        assertBalanced(debitAmount, creditAmount);
        if (financeCoreMapper.selectVoucherEntryList(voucherNo).isEmpty())
        {
            throw new ServiceException("凭证没有会计分录，不能过账");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("voucherNo", voucherNo);
        update.put("updateBy", username);
        int rows = financeCoreMapper.updateVoucherPosted(update);
        if (rows != 1)
        {
            throw new ServiceException("凭证状态已变化，请刷新后重试");
        }
        postVoucherToLedger(voucherNo, text(voucher.get("periodCode")));
        return rows;
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
        String bankComplete = intValue(summary.get("unmatchedCashCount")) == 0
            && intValue(summary.get("unpostedCashCount")) == 0 ? "1" : "0";
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
        String reason = text(form.get("reason"));
        if (reason.length() < 5)
        {
            throw new ServiceException("反关账原因至少填写5个字");
        }
        String closeScope = defaultText(form.get("closeScope"), "company");
        Map<String, Object> close = financeCoreMapper.selectPeriodCloseByCode(text(form.get("periodCode")), closeScope);
        if (close == null || !"closed".equals(text(close.get("closeStatus"))))
        {
            throw new ServiceException("只有已关账期间可以反关账");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("periodCode", text(form.get("periodCode")));
        update.put("closeScope", closeScope);
        update.put("reason", reason);
        update.put("updateBy", username);
        financeCoreMapper.updateAccountPeriodReopened(update);
        return financeCoreMapper.updatePeriodReopened(update);
    }

    private int writeoffBill(String billType, String billNo, Map<String, Object> bill, Map<String, Object> form, String username)
    {
        require(form, "cashFlowNo", "流水号不能为空");
        assertPeriodOpen(bill.get("periodCode"));
        if ("settled".equals(text(bill.get("billStatus"))) || !isPositive(decimal(bill.get("remainAmount"))))
        {
            throw new ServiceException("该账单已经结清，无需再次核销");
        }
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
        assertPeriodOpen(periodFromDate(text(cashFlow.get("businessDate"))));
        if (!"posted".equals(text(cashFlow.get("entryStatus"))))
        {
            throw new ServiceException("只有已入账流水可以核销");
        }
        if ("matched".equals(text(cashFlow.get("matchStatus"))))
        {
            throw new ServiceException("流水已匹配，不能重复核销");
        }
        String expectedFlowType = "receivable".equals(billType) ? "in" : "out";
        if (!expectedFlowType.equals(text(cashFlow.get("flowType"))))
        {
            throw new ServiceException("流水收支方向与应收应付不一致");
        }
        BigDecimal availableAmount = decimal(cashFlow.get("amount")).subtract(decimal(cashFlow.get("settledAmount")));
        if (amount.compareTo(availableAmount) > 0)
        {
            throw new ServiceException("核销金额不能大于流水可用余额");
        }

        BigDecimal remainAmount = decimal(bill.get("remainAmount"));
        if (amount.compareTo(remainAmount) > 0)
        {
            throw new ServiceException("核销金额不能大于未核销金额");
        }

        BigDecimal newRemain = remainAmount.subtract(amount);
        Map<String, Object> update = new HashMap<>();
        update.put("billNo", billNo);
        update.put("oldRemainAmount", remainAmount);
        update.put("writeoffAmount", amount);
        update.put("remainAmount", newRemain);
        update.put("billStatus", newRemain.compareTo(new BigDecimal("0.01")) <= 0 ? "settled" : "partial");
        update.put("updateBy", username);
        int billRows;
        if ("receivable".equals(billType))
        {
            billRows = financeCoreMapper.updateReceivableWriteoff(update);
        }
        else
        {
            billRows = financeCoreMapper.updatePayableWriteoff(update);
        }
        if (billRows != 1)
        {
            throw new ServiceException("账单余额已变化，本次核销已回滚，请刷新后重试");
        }

        Map<String, Object> cashUpdate = new HashMap<>();
        cashUpdate.put("flowNo", form.get("cashFlowNo"));
        cashUpdate.put("oldSettledAmount", decimal(cashFlow.get("settledAmount")));
        cashUpdate.put("sourceType", billType);
        cashUpdate.put("sourceNo", billNo);
        cashUpdate.put("writeoffAmount", amount);
        cashUpdate.put("updateBy", username);
        int cashRows = financeCoreMapper.updateCashFlowMatched(cashUpdate);
        if (cashRows != 1)
        {
            throw new ServiceException("流水可用余额已变化，本次核销已回滚，请刷新后重试");
        }

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

    private void createCashFlowVoucher(Map<String, Object> cashFlow, String username)
    {
        String flowNo = text(cashFlow.get("flowNo"));
        if (!isBlank(cashFlow.get("voucherNo")) || financeCoreMapper.selectVoucherBySource("cash_flow", flowNo) != null)
        {
            return;
        }
        BigDecimal amount = decimal(cashFlow.get("amount"));
        BigDecimal feeAmount = decimal(cashFlow.get("feeAmount"));
        BigDecimal netAmount = decimal(cashFlow.get("netAmount"));
        String flowType = text(cashFlow.get("flowType"));
        String category = text(cashFlow.get("flowCategory"));
        String accountCode = isBlank(cashFlow.get("platformAccountId")) ? "1002" : "1012";
        String accountName = "1002".equals(accountCode) ? "银行存款" : "其他货币资金";
        String[] offsetSubject = cashFlowOffsetSubject(flowType, category);
        BigDecimal voucherAmount = "in".equals(flowType) ? amount : amount.add(feeAmount);
        assertBalanced(voucherAmount, voucherAmount);

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucherNo", nextNo("VCH"));
        voucher.put("periodCode", periodFromDate(text(cashFlow.get("businessDate"))));
        voucher.put("voucherDate", cashFlow.get("businessDate"));
        voucher.put("sourceType", "cash_flow");
        voucher.put("sourceNo", flowNo);
        voucher.put("summary", ("in".equals(flowType) ? "资金收入：" : "资金支出：") + flowNo);
        voucher.put("debitAmount", voucherAmount);
        voucher.put("creditAmount", voucherAmount);
        voucher.put("voucherStatus", "draft");
        voucher.put("createBy", username);
        financeCoreMapper.insertVoucher(voucher);
        Long voucherId = voucherId(voucher);

        int seq = 1;
        if ("in".equals(flowType))
        {
            insertEntry(voucherId, voucher, seq++, accountCode, accountName, "debit", netAmount, cashFlow, "资金到账");
            if (isPositive(feeAmount))
            {
                insertEntry(voucherId, voucher, seq++, "6603", "财务费用", "debit", feeAmount, cashFlow, "收款手续费");
            }
            insertEntry(voucherId, voucher, seq, offsetSubject[0], offsetSubject[1], "credit", amount, cashFlow, "确认资金收入");
        }
        else
        {
            insertEntry(voucherId, voucher, seq++, offsetSubject[0], offsetSubject[1], "debit", amount, cashFlow, "确认资金支出");
            if (isPositive(feeAmount))
            {
                insertEntry(voucherId, voucher, seq++, "6603", "财务费用", "debit", feeAmount, cashFlow, "付款手续费");
            }
            insertEntry(voucherId, voucher, seq, accountCode, accountName, "credit", netAmount, cashFlow, "资金付出");
        }

        Map<String, Object> update = new HashMap<>();
        update.put("flowNo", flowNo);
        update.put("voucherNo", voucher.get("voucherNo"));
        update.put("updateBy", username);
        financeCoreMapper.updateCashFlowVoucher(update);
        cashFlow.put("voucherNo", voucher.get("voucherNo"));
    }

    private void createCashFlowReversalVoucher(Map<String, Object> original, Map<String, Object> reversal, String username)
    {
        String originalVoucherNo = text(original.get("voucherNo"));
        if (originalVoucherNo.isEmpty())
        {
            return;
        }
        Map<String, Object> originalVoucher = financeCoreMapper.selectVoucherByNo(originalVoucherNo);
        List<Map<String, Object>> originalEntries = financeCoreMapper.selectVoucherEntryList(originalVoucherNo);
        if (originalVoucher == null || originalEntries.isEmpty())
        {
            throw new ServiceException("原流水凭证不完整，不能自动冲销");
        }

        Map<String, Object> voucher = new HashMap<>();
        voucher.put("voucherNo", nextNo("VCH"));
        voucher.put("periodCode", periodFromDate(text(reversal.get("businessDate"))));
        voucher.put("voucherDate", reversal.get("businessDate"));
        voucher.put("sourceType", "cash_flow");
        voucher.put("sourceNo", reversal.get("flowNo"));
        voucher.put("summary", "冲销资金流水：" + original.get("flowNo"));
        voucher.put("debitAmount", originalVoucher.get("creditAmount"));
        voucher.put("creditAmount", originalVoucher.get("debitAmount"));
        voucher.put("voucherStatus", "draft");
        voucher.put("createBy", username);
        financeCoreMapper.insertVoucher(voucher);
        Long voucherId = voucherId(voucher);

        int seq = 1;
        for (Map<String, Object> originalEntry : originalEntries)
        {
            Map<String, Object> entry = new HashMap<>();
            entry.put("voucherId", voucherId);
            entry.put("voucherNo", voucher.get("voucherNo"));
            entry.put("entrySeq", seq++);
            entry.put("subjectCode", originalEntry.get("subjectCode"));
            entry.put("subjectName", originalEntry.get("subjectName"));
            entry.put("direction", "debit".equals(text(originalEntry.get("direction"))) ? "credit" : "debit");
            entry.put("amount", originalEntry.get("amount"));
            entry.put("shopId", originalEntry.get("shopId"));
            entry.put("skuId", originalEntry.get("skuId"));
            entry.put("counterpartyName", originalEntry.get("counterpartyName"));
            entry.put("summary", "冲销：" + text(originalEntry.get("summary")));
            financeCoreMapper.insertVoucherEntry(entry);
        }

        Map<String, Object> update = new HashMap<>();
        update.put("flowNo", reversal.get("flowNo"));
        update.put("voucherNo", voucher.get("voucherNo"));
        update.put("updateBy", username);
        financeCoreMapper.updateCashFlowVoucher(update);
        reversal.put("voucherNo", voucher.get("voucherNo"));
    }

    private String[] cashFlowOffsetSubject(String flowType, String category)
    {
        if ("in".equals(flowType))
        {
            if ("capital".equals(category)) return new String[] { "4001", "实收资本" };
            if ("other".equals(category)) return new String[] { "2241", "其他应付款" };
            return new String[] { "1122", "应收账款" };
        }
        if ("expense_payment".equals(category)) return new String[] { "6602", "管理费用" };
        if ("refund".equals(category)) return new String[] { "6001", "主营业务收入" };
        if ("tax_payment".equals(category)) return new String[] { "2221", "应交税费" };
        if ("payroll".equals(category)) return new String[] { "2211", "应付职工薪酬" };
        if ("other".equals(category)) return new String[] { "1221", "其他应收款" };
        return new String[] { "2202", "应付账款" };
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

    private Map<String, Object> getCashFlow(String flowNo)
    {
        Map<String, Object> cashFlow = financeCoreMapper.selectCashFlowByNo(flowNo);
        if (cashFlow == null)
        {
            throw new ServiceException("资金流水不存在");
        }
        return cashFlow;
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

    private BigDecimal calculateSettlementReceivable(Map<String, Object> form)
    {
        String[] amountKeys = {
            "incomeAmount", "refundAmount", "commissionFee", "paymentFee",
            "adFee", "serviceFee", "freightFee", "otherFee"
        };
        for (String key : amountKeys)
        {
            if (decimal(form.get(key)).compareTo(ZERO) < 0)
            {
                throw new ServiceException("结算金额不能为负数：" + key);
            }
        }
        return decimal(form.get("incomeAmount"))
            .subtract(decimal(form.get("refundAmount")))
            .subtract(decimal(form.get("commissionFee")))
            .subtract(decimal(form.get("paymentFee")))
            .subtract(decimal(form.get("adFee")))
            .subtract(decimal(form.get("serviceFee")))
            .subtract(decimal(form.get("freightFee")))
            .subtract(decimal(form.get("otherFee")));
    }

    private void validateSettlementDates(Map<String, Object> form)
    {
        String startDate = text(form.get("billStartDate"));
        String endDate = text(form.get("billEndDate"));
        if (startDate.compareTo(endDate) > 0)
        {
            throw new ServiceException("账单开始日期不能晚于结束日期");
        }
    }

    private void assertBalanced(BigDecimal debitAmount, BigDecimal creditAmount)
    {
        if (debitAmount.subtract(creditAmount).abs().compareTo(new BigDecimal("0.01")) > 0)
        {
            throw new ServiceException("借贷不平衡，不能生成凭证");
        }
    }

    private void postVoucherToLedger(String voucherNo, String periodCode)
    {
        if (financeCoreMapper.selectLedgerEntryCount(voucherNo) == 0)
        {
            int rows = financeCoreMapper.insertLedgerEntriesFromVoucher(voucherNo);
            if (rows == 0)
            {
                throw new ServiceException("凭证过账失败：没有可写入账簿的分录");
            }
        }
        financeCoreMapper.deleteTrialBalanceByPeriod(periodCode);
        financeCoreMapper.rebuildTrialBalance(periodCode);
    }

    private void assertPeriodOpen(Object periodCode)
    {
        if (isBlank(periodCode))
        {
            return;
        }
        Map<String, Object> close = financeCoreMapper.selectPeriodCloseByCode(text(periodCode), "company");
        if (close != null && "closed".equals(text(close.get("closeStatus"))))
        {
            throw new ServiceException("当前会计期间已结账，不能继续修改：" + text(periodCode));
        }
    }

    private String buildCloseRemark(Map<String, Object> summary)
    {
        List<String> messages = new ArrayList<>();
        messages.add("草稿结算单：" + intValue(summary.get("draftSettlementCount")));
        messages.add("未完成对账结算单：" + intValue(summary.get("unmatchedSettlementCount")));
        messages.add("未匹配流水：" + intValue(summary.get("unmatchedCashCount")));
        messages.add("未入账流水：" + intValue(summary.get("unpostedCashCount")));
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
