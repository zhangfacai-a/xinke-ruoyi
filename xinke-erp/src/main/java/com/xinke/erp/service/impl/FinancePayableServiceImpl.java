package com.xinke.erp.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.FinanceCoreMapper;
import com.xinke.erp.service.IFinancePayableService;
import com.xinke.erp.service.IFinanceWorkflowService;

@Service
public class FinancePayableServiceImpl implements IFinancePayableService
{
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal CENT = new BigDecimal("0.01");
    private static final BigDecimal DEFAULT_PRICE_TOLERANCE = new BigDecimal("3.00");
    private static final BigDecimal DEFAULT_QUANTITY_TOLERANCE = BigDecimal.ZERO;
    private static final BigDecimal DEFAULT_AMOUNT_TOLERANCE = new BigDecimal("1.00");

    @Autowired
    private FinanceCoreMapper financeCoreMapper;

    @Autowired
    private IFinanceWorkflowService financeWorkflowService;

    @Override
    public Map<String, Object> selectPayableSummary(Map<String, Object> query)
    {
        Map<String, Object> summary = financeCoreMapper.selectPayableSummary(query);
        return summary == null ? new HashMap<>() : summary;
    }

    @Override
    public List<Map<String, Object>> selectPayableAging(Map<String, Object> query)
    {
        return financeCoreMapper.selectPayableAging(query);
    }

    @Override
    public List<Map<String, Object>> selectSupplierInvoiceList(Map<String, Object> query)
    {
        return financeCoreMapper.selectSupplierInvoiceList(query);
    }

    @Override
    public List<Map<String, Object>> selectSupplierInvoiceItemList(String invoiceNo)
    {
        return financeCoreMapper.selectSupplierInvoiceItemList(invoiceNo);
    }

    @Override
    public List<Map<String, Object>> selectInvoicePurchaseOptions(Map<String, Object> query)
    {
        return financeCoreMapper.selectInvoicePurchaseOptions(query);
    }

    @Override
    public Map<String, Object> selectPurchaseInvoiceContext(String purchaseNo)
    {
        Map<String, Object> purchase = financeCoreMapper.selectInvoicePurchaseByNo(purchaseNo);
        if (purchase == null)
        {
            throw new ServiceException("采购单不存在");
        }
        Map<String, Object> result = new LinkedHashMap<>(purchase);
        result.put("items", financeCoreMapper.selectInvoicePurchaseItemList(purchaseNo));
        result.put("priceTolerancePercent", DEFAULT_PRICE_TOLERANCE);
        result.put("quantityTolerance", DEFAULT_QUANTITY_TOLERANCE);
        result.put("amountTolerance", DEFAULT_AMOUNT_TOLERANCE);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createSupplierInvoice(Map<String, Object> form, String username)
    {
        require(form, "purchaseNo", "采购单不能为空");
        require(form, "supplierInvoiceNo", "供应商发票号不能为空");
        require(form, "invoiceDate", "开票日期不能为空");

        String purchaseNo = text(form.get("purchaseNo"));
        Map<String, Object> purchase = financeCoreMapper.selectInvoicePurchaseByNo(purchaseNo);
        if (purchase == null)
        {
            throw new ServiceException("采购单不存在");
        }
        String purchaseStatus = text(purchase.get("purchaseStatus"));
        if (!Set.of("approved", "processing", "receiving", "completed", "done", "closed").contains(purchaseStatus))
        {
            throw new ServiceException("采购单尚未审批，不能登记供应商发票");
        }

        Long supplierId = longValue(purchase.get("supplierId"));
        String supplierInvoiceNo = text(form.get("supplierInvoiceNo"));
        if (financeCoreMapper.selectSupplierInvoiceBySupplierNo(supplierId, supplierInvoiceNo) != null)
        {
            throw new ServiceException("该供应商发票号已存在");
        }

        List<Map<String, Object>> purchaseItems = financeCoreMapper.selectInvoicePurchaseItemList(purchaseNo);
        Map<Long, Map<String, Object>> purchaseItemMap = new HashMap<>();
        for (Map<String, Object> item : purchaseItems)
        {
            purchaseItemMap.put(longValue(item.get("purchaseItemId")), item);
        }
        List<Map<String, Object>> invoiceItems = itemList(form.get("items"));
        if (invoiceItems.isEmpty())
        {
            throw new ServiceException("发票明细不能为空");
        }

        BigDecimal priceTolerance = nonNegative(form.get("priceTolerancePercent"), DEFAULT_PRICE_TOLERANCE,
            "单价容差不能小于0");
        BigDecimal quantityTolerance = nonNegative(form.get("quantityTolerance"), DEFAULT_QUANTITY_TOLERANCE,
            "数量容差不能小于0");
        BigDecimal amountTolerance = nonNegative(form.get("amountTolerance"), DEFAULT_AMOUNT_TOLERANCE,
            "金额容差不能小于0");
        String invoiceNo = nextNo("VIN");
        BigDecimal lineNetTotal = ZERO;
        BigDecimal taxTotal = ZERO;
        boolean allMatched = true;
        Set<Long> seenItemIds = new HashSet<>();
        List<Map<String, Object>> normalizedItems = new ArrayList<>();

        for (Map<String, Object> source : invoiceItems)
        {
            Long purchaseItemId = longValue(source.get("purchaseItemId"));
            Map<String, Object> purchaseItem = purchaseItemMap.get(purchaseItemId);
            if (purchaseItem == null || !seenItemIds.add(purchaseItemId))
            {
                throw new ServiceException("发票明细不存在或重复");
            }

            BigDecimal invoiceQuantity = decimal(source.get("invoiceQuantity"));
            BigDecimal invoiceUnitPrice = decimal(source.get("invoiceUnitPrice"));
            BigDecimal taxRate = decimal(source.get("taxRate"));
            if (invoiceQuantity.compareTo(ZERO) <= 0 || invoiceUnitPrice.compareTo(ZERO) < 0)
            {
                throw new ServiceException(text(purchaseItem.get("skuCode")) + " 的发票数量必须大于0，单价不能小于0");
            }
            if (taxRate.compareTo(ZERO) < 0 || taxRate.compareTo(BigDecimal.ONE) > 0)
            {
                throw new ServiceException(text(purchaseItem.get("skuCode")) + " 的税率必须在0到1之间");
            }

            BigDecimal availableReceived = decimal(purchaseItem.get("availableReceivedQuantity"));
            BigDecimal orderUnitPrice = decimal(purchaseItem.get("orderUnitPrice"));
            BigDecimal quantityVariance = invoiceQuantity.subtract(availableReceived);
            BigDecimal priceVariance = invoiceUnitPrice.subtract(orderUnitPrice);
            BigDecimal priceVariancePercent = orderUnitPrice.compareTo(ZERO) == 0
                ? (invoiceUnitPrice.compareTo(ZERO) == 0 ? ZERO : new BigDecimal("999999.00"))
                : priceVariance.abs().multiply(new BigDecimal("100")).divide(orderUnitPrice, 4, RoundingMode.HALF_UP);
            BigDecimal lineAmount = invoiceQuantity.multiply(invoiceUnitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal expectedAmount = invoiceQuantity.multiply(orderUnitPrice).setScale(2, RoundingMode.HALF_UP);
            BigDecimal amountVariance = lineAmount.subtract(expectedAmount);
            BigDecimal taxAmount = lineAmount.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalAmount = lineAmount.add(taxAmount);

            List<String> messages = new ArrayList<>();
            if (quantityVariance.compareTo(quantityTolerance) > 0)
            {
                messages.add("发票数量超过可开票入库数量 " + quantityVariance.stripTrailingZeros().toPlainString());
            }
            if (priceVariancePercent.compareTo(priceTolerance) > 0)
            {
                messages.add("单价偏差 " + priceVariancePercent.setScale(2, RoundingMode.HALF_UP) + "%");
            }
            boolean lineMatched = messages.isEmpty();
            allMatched = allMatched && lineMatched;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("invoiceNo", invoiceNo);
            item.put("purchaseItemId", purchaseItemId);
            item.put("skuId", purchaseItem.get("skuId"));
            item.put("skuCode", purchaseItem.get("skuCode"));
            item.put("skuName", purchaseItem.get("skuName"));
            item.put("invoiceQuantity", invoiceQuantity);
            item.put("invoiceUnitPrice", invoiceUnitPrice);
            item.put("invoiceTaxRate", taxRate);
            item.put("invoiceAmount", lineAmount);
            item.put("taxAmount", taxAmount);
            item.put("totalAmount", totalAmount);
            item.put("orderQuantity", purchaseItem.get("orderQuantity"));
            item.put("orderUnitPrice", orderUnitPrice);
            item.put("receivedQuantity", purchaseItem.get("receivedQuantity"));
            item.put("previousInvoicedQuantity", purchaseItem.get("invoicedQuantity"));
            item.put("availableReceivedQuantity", availableReceived);
            item.put("quantityVariance", quantityVariance);
            item.put("priceVariance", priceVariance);
            item.put("priceVariancePercent", priceVariancePercent);
            item.put("amountVariance", amountVariance);
            item.put("lineMatchStatus", lineMatched ? "matched" : "exception");
            item.put("matchMessage", lineMatched ? "数量和单价匹配" : String.join("；", messages));
            normalizedItems.add(item);
            lineNetTotal = lineNetTotal.add(lineAmount);
            taxTotal = taxTotal.add(taxAmount);
        }

        BigDecimal lineTotalAmount = lineNetTotal.add(taxTotal).setScale(2, RoundingMode.HALF_UP);
        BigDecimal declaredTotal = isBlank(form.get("totalAmount"))
            ? lineTotalAmount : decimal(form.get("totalAmount")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal headerVariance = declaredTotal.subtract(lineTotalAmount);
        if (headerVariance.abs().compareTo(amountTolerance) > 0)
        {
            allMatched = false;
        }
        BigDecimal accountingNetAmount = declaredTotal.subtract(taxTotal).setScale(2, RoundingMode.HALF_UP);
        if (accountingNetAmount.compareTo(ZERO) < 0)
        {
            throw new ServiceException("价税合计不能小于税额");
        }

        String invoiceDate = text(form.get("invoiceDate"));
        String dueDate = text(form.get("dueDate"));
        if (dueDate.isEmpty())
        {
            int paymentDays = intValue(purchase.get("paymentDays"), 30);
            dueDate = LocalDate.parse(invoiceDate).plusDays(paymentDays).toString();
        }
        String periodCode = invoiceDate.substring(0, 7);
        assertPeriodOpen(periodCode);

        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNo", invoiceNo);
        invoice.put("supplierInvoiceNo", supplierInvoiceNo);
        invoice.put("purchaseNo", purchaseNo);
        invoice.put("receiptRefs", purchase.get("receiptRefs"));
        invoice.put("supplierId", supplierId);
        invoice.put("supplierName", purchase.get("supplierName"));
        invoice.put("invoiceDate", invoiceDate);
        invoice.put("dueDate", dueDate);
        invoice.put("periodCode", periodCode);
        invoice.put("amountExTax", accountingNetAmount);
        invoice.put("taxAmount", taxTotal);
        invoice.put("lineTotalAmount", lineTotalAmount);
        invoice.put("totalAmount", declaredTotal);
        invoice.put("headerVariance", headerVariance);
        invoice.put("matchPolicy", "three_way");
        invoice.put("priceTolerancePercent", priceTolerance);
        invoice.put("quantityTolerance", quantityTolerance);
        invoice.put("amountTolerance", amountTolerance);
        invoice.put("matchStatus", allMatched ? "matched" : "exception");
        invoice.put("invoiceStatus", "pending");
        invoice.put("createBy", username);
        invoice.put("remark", form.get("remark"));
        financeCoreMapper.insertSupplierInvoice(invoice);
        for (Map<String, Object> item : normalizedItems)
        {
            item.put("invoiceId", invoice.get("invoiceId"));
            financeCoreMapper.insertSupplierInvoiceItem(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("invoiceNo", invoiceNo);
        result.put("matchStatus", invoice.get("matchStatus"));
        result.put("lineCount", normalizedItems.size());
        result.put("totalAmount", declaredTotal);
        result.put("headerVariance", headerVariance);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveSupplierInvoice(String invoiceNo, Map<String, Object> form, String username)
    {
        Map<String, Object> invoice = requireInvoice(invoiceNo);
        if ("approved".equals(text(invoice.get("invoiceStatus"))))
        {
            return 1;
        }
        if (!"pending".equals(text(invoice.get("invoiceStatus"))))
        {
            throw new ServiceException("只有待审批发票可以确认应付");
        }
        assertPeriodOpen(invoice.get("periodCode"));

        boolean matched = "matched".equals(text(invoice.get("matchStatus")));
        boolean override = booleanValue(form.get("overrideMatch"));
        String exceptionReason = text(form.get("exceptionReason"));
        if (!matched && (!override || exceptionReason.length() < 5))
        {
            throw new ServiceException("三单匹配存在差异，例外放行必须填写至少5个字的原因");
        }

        String payableNo = "AP-" + invoiceNo;
        Map<String, Object> update = new HashMap<>();
        update.put("invoiceNo", invoiceNo);
        update.put("payableNo", payableNo);
        update.put("matchStatus", matched ? "matched" : "override");
        update.put("exceptionReason", exceptionReason);
        update.put("approveBy", username);
        int rows = financeCoreMapper.updateSupplierInvoiceApproved(update);
        if (rows != 1)
        {
            throw new ServiceException("供应商发票状态已变化，请刷新后重试");
        }

        if (financeCoreMapper.selectPayableByNo(payableNo) == null)
        {
            Map<String, Object> payable = new LinkedHashMap<>();
            payable.put("payableNo", payableNo);
            payable.put("sourceType", "supplier_invoice");
            payable.put("sourceNo", invoiceNo);
            payable.put("counterpartyType", "supplier");
            payable.put("counterpartyId", invoice.get("supplierId"));
            payable.put("counterpartyName", invoice.get("supplierName"));
            payable.put("periodCode", invoice.get("periodCode"));
            payable.put("billAmount", decimal(invoice.get("totalAmount")));
            payable.put("writeoffAmount", ZERO);
            payable.put("remainAmount", decimal(invoice.get("totalAmount")));
            payable.put("dueDate", invoice.get("dueDate"));
            payable.put("billStatus", "open");
            payable.put("createBy", username);
            payable.put("remark", "供应商发票 " + text(invoice.get("supplierInvoiceNo")));
            financeCoreMapper.insertPayable(payable);
        }

        createSupplierInvoiceVoucher(invoice, username);
        return rows;
    }

    @Override
    public List<Map<String, Object>> selectPaymentRequestList(Map<String, Object> query)
    {
        return financeCoreMapper.selectPaymentRequestList(query);
    }

    @Override
    public List<Map<String, Object>> selectBankAccountOptions()
    {
        return financeCoreMapper.selectBankAccountOptions();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int createPaymentRequest(String payableNo, Map<String, Object> form, String username)
    {
        Map<String, Object> payable = requirePayable(payableNo);
        assertPeriodOpen(payable.get("periodCode"));
        if ("settled".equals(text(payable.get("billStatus"))))
        {
            throw new ServiceException("应付已结清，不能再次申请付款");
        }
        BigDecimal amount = decimal(form.get("paymentAmount"));
        if (amount.compareTo(ZERO) <= 0)
        {
            throw new ServiceException("申请付款金额必须大于0");
        }
        BigDecimal reserved = decimal(financeCoreMapper.selectReservedPaymentAmount(payableNo));
        BigDecimal available = decimal(payable.get("remainAmount")).subtract(reserved);
        if (amount.compareTo(available) > 0)
        {
            throw new ServiceException("申请金额超过可申请余额，可申请金额为 " + available.setScale(2, RoundingMode.HALF_UP));
        }

        Map<String, Object> payment = new LinkedHashMap<>();
        payment.put("paymentNo", nextNo("PAY"));
        payment.put("sourceType", payable.get("sourceType"));
        payment.put("sourceNo", payable.get("sourceNo"));
        payment.put("counterpartyType", "supplier");
        payment.put("counterpartyId", payable.get("counterpartyId"));
        payment.put("counterpartyName", payable.get("counterpartyName"));
        payment.put("payableNo", payableNo);
        payment.put("paymentAmount", amount);
        payment.put("paidAmount", ZERO);
        payment.put("requestedPayDate", defaultText(form.get("requestedPayDate"), LocalDate.now().toString()));
        payment.put("paymentStatus", "submitted");
        payment.put("createBy", username);
        payment.put("remark", form.get("remark"));
        return financeCoreMapper.insertPaymentRequest(payment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approvePaymentRequest(String paymentNo, String username)
    {
        Map<String, Object> payment = requirePayment(paymentNo);
        if ("approved".equals(text(payment.get("paymentStatus"))))
        {
            return 1;
        }
        if (!"submitted".equals(text(payment.get("paymentStatus"))))
        {
            throw new ServiceException("只有已提交的付款申请可以审批");
        }
        Map<String, Object> payable = requirePayable(text(payment.get("payableNo")));
        assertPeriodOpen(payable.get("periodCode"));
        Map<String, Object> update = new HashMap<>();
        update.put("paymentNo", paymentNo);
        update.put("approveBy", username);
        int rows = financeCoreMapper.updatePaymentRequestApproved(update);
        if (rows != 1)
        {
            throw new ServiceException("付款申请状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int rejectPaymentRequest(String paymentNo, Map<String, Object> form, String username)
    {
        Map<String, Object> payment = requirePayment(paymentNo);
        if (!"submitted".equals(text(payment.get("paymentStatus"))))
        {
            throw new ServiceException("只有待审批的付款申请可以驳回");
        }
        require(form, "rejectReason", "驳回原因不能为空");
        String rejectReason = text(form.get("rejectReason"));
        if (rejectReason.length() < 5)
        {
            throw new ServiceException("驳回原因至少填写5个字");
        }
        Map<String, Object> update = new HashMap<>();
        update.put("paymentNo", paymentNo);
        update.put("rejectReason", rejectReason);
        update.put("approveBy", username);
        int rows = financeCoreMapper.updatePaymentRequestRejected(update);
        if (rows != 1)
        {
            throw new ServiceException("付款申请状态已变化，请刷新后重试");
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int executePaymentRequest(String paymentNo, Map<String, Object> form, String username)
    {
        Map<String, Object> payment = requirePayment(paymentNo);
        String status = text(payment.get("paymentStatus"));
        if (!Set.of("approved", "partially_paid").contains(status))
        {
            throw new ServiceException("只有已审批的付款申请可以执行付款");
        }
        require(form, "bankReference", "银行流水号不能为空");
        Long bankAccountId = longValue(form.get("bankAccountId"));
        if (bankAccountId == null || financeCoreMapper.selectBankAccountById(bankAccountId) == null)
        {
            throw new ServiceException("请选择有效的付款银行账户");
        }
        Map<String, Object> payable = requirePayable(text(payment.get("payableNo")));
        assertPeriodOpen(payable.get("periodCode"));

        BigDecimal requestRemain = decimal(payment.get("paymentAmount")).subtract(decimal(payment.get("paidAmount")));
        BigDecimal executeAmount = isBlank(form.get("executeAmount")) ? requestRemain : decimal(form.get("executeAmount"));
        if (executeAmount.compareTo(ZERO) <= 0 || executeAmount.compareTo(requestRemain) > 0)
        {
            throw new ServiceException("付款金额必须大于0且不能超过申请剩余金额");
        }
        if (executeAmount.compareTo(decimal(payable.get("remainAmount"))) > 0)
        {
            throw new ServiceException("付款金额不能超过应付未核销余额");
        }

        String executeTime = defaultText(form.get("executeTime"),
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        String executeNo = nextNo("PEX");
        Map<String, Object> cashFlow = new LinkedHashMap<>();
        cashFlow.put("flowType", "out");
        cashFlow.put("flowCategory", "supplier_payment");
        cashFlow.put("bankAccountId", bankAccountId);
        cashFlow.put("counterpartyType", "supplier");
        cashFlow.put("counterpartyName", payment.get("counterpartyName"));
        cashFlow.put("amount", executeAmount);
        cashFlow.put("feeAmount", ZERO);
        cashFlow.put("flowTime", executeTime);
        cashFlow.put("businessDate", executeTime.substring(0, 10));
        cashFlow.put("sourceType", "payable");
        cashFlow.put("sourceNo", payable.get("payableNo"));
        cashFlow.put("externalReference", form.get("bankReference"));
        cashFlow.put("entryStatus", "posted");
        cashFlow.put("generateVoucher", false);
        cashFlow.put("remark", defaultText(form.get("bankReference"), text(form.get("remark"))));
        financeWorkflowService.insertCashFlow(cashFlow, username);

        Map<String, Object> writeoff = new HashMap<>();
        writeoff.put("cashFlowNo", cashFlow.get("flowNo"));
        writeoff.put("writeoffAmount", executeAmount);
        writeoff.put("remark", "付款申请 " + paymentNo + " 自动核销");
        financeWorkflowService.writeoffPayable(text(payable.get("payableNo")), writeoff, username);

        Map<String, Object> execute = new LinkedHashMap<>();
        execute.put("executeNo", executeNo);
        execute.put("paymentNo", paymentNo);
        execute.put("bankAccountId", bankAccountId);
        execute.put("cashFlowNo", cashFlow.get("flowNo"));
        execute.put("bankReference", form.get("bankReference"));
        execute.put("executeAmount", executeAmount);
        execute.put("executeTime", executeTime);
        execute.put("executeStatus", "done");
        execute.put("operatorName", username);
        execute.put("createBy", username);
        execute.put("remark", form.get("remark"));
        financeCoreMapper.insertPaymentExecute(execute);

        BigDecimal newPaidAmount = decimal(payment.get("paidAmount")).add(executeAmount);
        Map<String, Object> update = new HashMap<>();
        update.put("paymentNo", paymentNo);
        update.put("expectedStatus", status);
        update.put("oldPaidAmount", decimal(payment.get("paidAmount")));
        update.put("paidAmount", newPaidAmount);
        update.put("paymentStatus", newPaidAmount.add(CENT).compareTo(decimal(payment.get("paymentAmount"))) >= 0
            ? "paid" : "partially_paid");
        update.put("updateBy", username);
        int rows = financeCoreMapper.updatePaymentRequestPaid(update);
        if (rows != 1)
        {
            throw new ServiceException("付款申请已被其他操作更新，本次付款已回滚，请刷新后重试");
        }
        createPaymentVoucher(executeNo, payment, payable, executeAmount, executeTime.substring(0, 10), username);
        return 1;
    }

    private void createSupplierInvoiceVoucher(Map<String, Object> invoice, String username)
    {
        String invoiceNo = text(invoice.get("invoiceNo"));
        if (financeCoreMapper.selectVoucherBySource("supplier_invoice", invoiceNo) != null)
        {
            return;
        }
        BigDecimal netAmount = decimal(invoice.get("amountExTax"));
        BigDecimal taxAmount = decimal(invoice.get("taxAmount"));
        BigDecimal totalAmount = decimal(invoice.get("totalAmount"));
        assertBalanced(netAmount.add(taxAmount), totalAmount);

        Map<String, Object> voucher = new LinkedHashMap<>();
        voucher.put("voucherNo", nextNo("VCH"));
        voucher.put("periodCode", invoice.get("periodCode"));
        voucher.put("voucherDate", invoice.get("invoiceDate"));
        voucher.put("sourceType", "supplier_invoice");
        voucher.put("sourceNo", invoiceNo);
        voucher.put("summary", "供应商发票确认应付：" + text(invoice.get("supplierInvoiceNo")));
        voucher.put("debitAmount", totalAmount);
        voucher.put("creditAmount", totalAmount);
        voucher.put("voucherStatus", "draft");
        voucher.put("createBy", username);
        financeCoreMapper.insertVoucher(voucher);
        Long voucherId = longValue(voucher.get("voucherId"));

        int seq = 1;
        insertVoucherEntry(voucherId, voucher, seq++, "1405", "库存商品", "debit", netAmount,
            invoice.get("supplierName"), "采购发票确认存货成本");
        if (taxAmount.compareTo(ZERO) > 0)
        {
            insertVoucherEntry(voucherId, voucher, seq++, "222101", "应交增值税-进项税额", "debit", taxAmount,
                invoice.get("supplierName"), "采购发票进项税额");
        }
        insertVoucherEntry(voucherId, voucher, seq, "2202", "应付账款", "credit", totalAmount,
            invoice.get("supplierName"), "确认供应商应付");
    }

    private void createPaymentVoucher(String executeNo, Map<String, Object> payment, Map<String, Object> payable,
        BigDecimal amount, String businessDate, String username)
    {
        if (financeCoreMapper.selectVoucherBySource("payment_execute", executeNo) != null)
        {
            return;
        }
        Map<String, Object> voucher = new LinkedHashMap<>();
        voucher.put("voucherNo", nextNo("VCH"));
        voucher.put("periodCode", businessDate.substring(0, 7));
        voucher.put("voucherDate", businessDate);
        voucher.put("sourceType", "payment_execute");
        voucher.put("sourceNo", executeNo);
        voucher.put("summary", "供应商付款：" + text(payment.get("paymentNo")));
        voucher.put("debitAmount", amount);
        voucher.put("creditAmount", amount);
        voucher.put("voucherStatus", "draft");
        voucher.put("createBy", username);
        financeCoreMapper.insertVoucher(voucher);
        Long voucherId = longValue(voucher.get("voucherId"));
        insertVoucherEntry(voucherId, voucher, 1, "2202", "应付账款", "debit", amount,
            payable.get("counterpartyName"), "核销供应商应付");
        insertVoucherEntry(voucherId, voucher, 2, "1002", "银行存款", "credit", amount,
            payable.get("counterpartyName"), "银行付款");
    }

    private void insertVoucherEntry(Long voucherId, Map<String, Object> voucher, int seq, String subjectCode,
        String subjectName, String direction, BigDecimal amount, Object counterpartyName, String summary)
    {
        if (amount.compareTo(ZERO) <= 0)
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
        entry.put("counterpartyName", counterpartyName);
        entry.put("summary", summary);
        financeCoreMapper.insertVoucherEntry(entry);
    }

    private Map<String, Object> requireInvoice(String invoiceNo)
    {
        Map<String, Object> invoice = financeCoreMapper.selectSupplierInvoiceByNo(invoiceNo);
        if (invoice == null)
        {
            throw new ServiceException("供应商发票不存在");
        }
        return invoice;
    }

    private Map<String, Object> requirePayable(String payableNo)
    {
        Map<String, Object> payable = financeCoreMapper.selectPayableByNo(payableNo);
        if (payable == null)
        {
            throw new ServiceException("应付单不存在");
        }
        return payable;
    }

    private Map<String, Object> requirePayment(String paymentNo)
    {
        Map<String, Object> payment = financeCoreMapper.selectPaymentRequestByNo(paymentNo);
        if (payment == null)
        {
            throw new ServiceException("付款申请不存在");
        }
        return payment;
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
            throw new ServiceException("会计期间已结账，不能继续处理：" + text(periodCode));
        }
    }

    private void assertBalanced(BigDecimal debitAmount, BigDecimal creditAmount)
    {
        if (debitAmount.subtract(creditAmount).abs().compareTo(CENT) > 0)
        {
            throw new ServiceException("借贷不平衡，不能生成凭证");
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> itemList(Object value)
    {
        if (!(value instanceof List<?>))
        {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : (List<?>) value)
        {
            if (item instanceof Map<?, ?>)
            {
                result.add((Map<String, Object>) item);
            }
        }
        return result;
    }

    private BigDecimal nonNegative(Object value, BigDecimal defaultValue, String message)
    {
        BigDecimal result = isBlank(value) ? defaultValue : decimal(value);
        if (result.compareTo(ZERO) < 0)
        {
            throw new ServiceException(message);
        }
        return result;
    }

    private void require(Map<String, Object> form, String key, String message)
    {
        if (isBlank(form.get(key)))
        {
            throw new ServiceException(message);
        }
    }

    private boolean booleanValue(Object value)
    {
        return value instanceof Boolean ? (Boolean) value : "true".equalsIgnoreCase(text(value)) || "1".equals(text(value));
    }

    private boolean isBlank(Object value)
    {
        return value == null || text(value).isEmpty();
    }

    private String defaultText(Object value, String defaultValue)
    {
        return isBlank(value) ? defaultValue : text(value);
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
        return new BigDecimal(String.valueOf(value));
    }

    private Long longValue(Object value)
    {
        if (value == null || text(value).isEmpty())
        {
            return null;
        }
        return value instanceof Number ? ((Number) value).longValue() : Long.valueOf(text(value));
    }

    private int intValue(Object value, int defaultValue)
    {
        if (value == null || text(value).isEmpty())
        {
            return defaultValue;
        }
        return value instanceof Number ? ((Number) value).intValue() : Integer.parseInt(text(value));
    }

    private String nextNo(String prefix)
    {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
    }
}
