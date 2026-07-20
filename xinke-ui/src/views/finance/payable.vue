<template>
  <div class="app-container payable-center">
    <section class="page-heading">
      <div>
        <span class="eyebrow">ACCOUNTS PAYABLE</span>
        <h1>应付与供应商发票中心</h1>
        <p>统一处理采购发票三单匹配、应付账龄、付款审批、银行付款和自动核销。</p>
      </div>
      <div class="heading-actions">
        <el-button icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" icon="Plus" @click="openInvoiceDialog" v-hasPermi="['finance:payable:add']">
          登记供应商发票
        </el-button>
      </div>
    </section>

    <section class="metric-grid">
      <button class="metric-card" type="button" @click="filterPayable()">
        <span class="metric-label">应付未结余额</span>
        <strong>{{ money(summary.outstandingAmount) }}</strong>
        <span>{{ summary.openCount || 0 }} 笔未结应付</span>
      </button>
      <button class="metric-card danger" type="button" @click="filterPayable('overdue')">
        <span class="metric-label">已逾期</span>
        <strong>{{ money(summary.overdueAmount) }}</strong>
        <span>{{ summary.overdueCount || 0 }} 笔需要优先处理</span>
      </button>
      <button class="metric-card warning" type="button" @click="filterPayable('due7')">
        <span class="metric-label">7 日内到期</span>
        <strong>{{ money(summary.due7Amount) }}</strong>
        <span>{{ summary.due7Count || 0 }} 笔进入付款窗口</span>
      </button>
      <button class="metric-card purple" type="button" @click="filterInvoice('exception')">
        <span class="metric-label">待处理发票</span>
        <strong>{{ summary.pendingInvoiceCount || 0 }}</strong>
        <span>{{ summary.exceptionInvoiceCount || 0 }} 笔存在匹配差异</span>
      </button>
      <button class="metric-card teal" type="button" @click="filterPayment()">
        <span class="metric-label">付款在途</span>
        <strong>{{ money(summary.pendingPaymentAmount) }}</strong>
        <span>已申请、待审批或待执行</span>
      </button>
    </section>

    <section class="aging-strip">
      <div class="aging-title">
        <strong>应付账龄</strong>
        <span>按到期日统计未核销余额</span>
      </div>
      <div v-for="item in agingRows" :key="item.agingBucket" class="aging-item">
        <div class="aging-meta">
          <span>{{ agingLabel(item.agingBucket) }}</span>
          <b>{{ money(item.remainAmount) }}</b>
        </div>
        <div class="aging-track">
          <i :style="{ width: `${agingPercent(item.remainAmount)}%` }" />
        </div>
      </div>
    </section>

    <section class="workbench">
      <div class="tabs-row">
        <el-tabs v-model="activeTab" @tab-change="handleTabChange">
          <el-tab-pane label="应付账款" name="payable" />
          <el-tab-pane label="供应商发票" name="invoice" />
          <el-tab-pane label="付款申请" name="payment" />
        </el-tabs>
      </div>

      <el-form v-if="activeTab === 'payable'" ref="payableQueryRef" :model="payableQuery" :inline="true" class="search-form">
        <el-form-item label="应付单" prop="payableNo">
          <el-input v-model="payableQuery.payableNo" placeholder="单号或关键字" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="供应商" prop="counterpartyName">
          <el-input v-model="payableQuery.counterpartyName" placeholder="供应商名称" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="期间" prop="periodCode">
          <el-date-picker v-model="payableQuery.periodCode" type="month" value-format="YYYY-MM" placeholder="会计期间" clearable />
        </el-form-item>
        <el-form-item label="到期" prop="dueStatus">
          <el-select v-model="payableQuery.dueStatus" placeholder="全部" clearable>
            <el-option label="已逾期" value="overdue" />
            <el-option label="7 日内到期" value="due7" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="billStatus">
          <el-select v-model="payableQuery.billStatus" placeholder="全部" clearable>
            <el-option label="未付款" value="open" />
            <el-option label="部分付款" value="partial" />
            <el-option label="已结清" value="settled" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" icon="Search" @click="queryCurrent">查询</el-button>
          <el-button icon="Refresh" @click="resetCurrent">重置</el-button>
        </el-form-item>
      </el-form>

      <el-form v-else-if="activeTab === 'invoice'" ref="invoiceQueryRef" :model="invoiceQuery" :inline="true" class="search-form">
        <el-form-item label="发票" prop="invoiceNo">
          <el-input v-model="invoiceQuery.invoiceNo" placeholder="系统号或供应商发票号" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="采购单" prop="purchaseNo">
          <el-input v-model="invoiceQuery.purchaseNo" placeholder="采购单号" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplierName">
          <el-input v-model="invoiceQuery.supplierName" placeholder="供应商名称" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="匹配" prop="matchStatus">
          <el-select v-model="invoiceQuery.matchStatus" placeholder="全部" clearable>
            <el-option label="匹配通过" value="matched" />
            <el-option label="存在差异" value="exception" />
            <el-option label="例外放行" value="override" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="invoiceStatus">
          <el-select v-model="invoiceQuery.invoiceStatus" placeholder="全部" clearable>
            <el-option label="待审批" value="pending" />
            <el-option label="已确认应付" value="approved" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" icon="Search" @click="queryCurrent">查询</el-button>
          <el-button icon="Refresh" @click="resetCurrent">重置</el-button>
        </el-form-item>
      </el-form>

      <el-form v-else ref="paymentQueryRef" :model="paymentQuery" :inline="true" class="search-form">
        <el-form-item label="付款单" prop="paymentNo">
          <el-input v-model="paymentQuery.paymentNo" placeholder="付款申请单号" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="应付单" prop="payableNo">
          <el-input v-model="paymentQuery.payableNo" placeholder="应付单号" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="供应商" prop="counterpartyName">
          <el-input v-model="paymentQuery.counterpartyName" placeholder="供应商名称" clearable @keyup.enter="queryCurrent" />
        </el-form-item>
        <el-form-item label="状态" prop="paymentStatus">
          <el-select v-model="paymentQuery.paymentStatus" placeholder="全部" clearable>
            <el-option label="待审批" value="submitted" />
            <el-option label="待付款" value="approved" />
            <el-option label="部分付款" value="partially_paid" />
            <el-option label="已付清" value="paid" />
            <el-option label="已驳回" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item class="search-actions">
          <el-button type="primary" icon="Search" @click="queryCurrent">查询</el-button>
          <el-button icon="Refresh" @click="resetCurrent">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-if="activeTab === 'payable'" v-loading="loading" :data="payableRows" row-key="payableId">
        <el-table-column label="应付单号" prop="payableNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="供应商" prop="counterpartyName" min-width="170" show-overflow-tooltip />
        <el-table-column label="业务来源" min-width="150">
          <template #default="{ row }">
            <span>{{ sourceLabel(row.sourceType) }}</span>
            <small class="cell-sub">{{ row.supplierInvoiceNo || row.sourceNo }}</small>
          </template>
        </el-table-column>
        <el-table-column label="应付金额" align="right" width="130">
          <template #default="{ row }">{{ money(row.billAmount) }}</template>
        </el-table-column>
        <el-table-column label="已付款" align="right" width="120">
          <template #default="{ row }">{{ money(row.writeoffAmount) }}</template>
        </el-table-column>
        <el-table-column label="未付余额" align="right" width="130">
          <template #default="{ row }"><b>{{ money(row.remainAmount) }}</b></template>
        </el-table-column>
        <el-table-column label="申请占用" align="right" width="120">
          <template #default="{ row }">{{ money(row.reservedAmount) }}</template>
        </el-table-column>
        <el-table-column label="到期日" width="120">
          <template #default="{ row }">
            <span :class="{ 'overdue-text': row.overdueFlag }">{{ row.dueDate || '-' }}</span>
            <small class="cell-sub">{{ agingLabel(row.agingBucket) }}</small>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="billTag(row.billStatus)" effect="light">{{ billStatusLabel(row.billStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="Number(row.availableAmount) > 0" link type="primary" @click="openPaymentRequest(row)" v-hasPermi="['finance:payable:payment']">申请付款</el-button>
            <el-button v-if="Number(row.remainAmount) > 0" link @click="openWriteoff(row)" v-hasPermi="['finance:payable:writeoff']">手工核销</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else-if="activeTab === 'invoice'" v-loading="loading" :data="invoiceRows" row-key="invoiceId">
        <el-table-column label="供应商发票" min-width="180">
          <template #default="{ row }">
            <b>{{ row.supplierInvoiceNo }}</b>
            <small class="cell-sub">{{ row.invoiceNo }}</small>
          </template>
        </el-table-column>
        <el-table-column label="采购 / 入库" min-width="190">
          <template #default="{ row }">
            <span>{{ row.purchaseNo }}</span>
            <small class="cell-sub" :title="row.receiptRefs">{{ row.receiptRefs || '暂无有效入库单' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="供应商" prop="supplierName" min-width="170" show-overflow-tooltip />
        <el-table-column label="开票 / 到期" width="130">
          <template #default="{ row }">
            <span>{{ row.invoiceDate }}</span>
            <small class="cell-sub">到期 {{ row.dueDate }}</small>
          </template>
        </el-table-column>
        <el-table-column label="价税合计" align="right" width="130">
          <template #default="{ row }">{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="三单匹配" width="125" align="center">
          <template #default="{ row }">
            <el-tag :type="matchTag(row.matchStatus)" effect="light">{{ matchLabel(row.matchStatus) }}</el-tag>
            <small v-if="row.exceptionLineCount" class="cell-sub">{{ row.exceptionLineCount }} 行差异</small>
          </template>
        </el-table-column>
        <el-table-column label="发票状态" width="120" align="center">
          <template #default="{ row }"><el-tag :type="row.invoiceStatus === 'approved' ? 'success' : 'warning'" effect="plain">{{ row.invoiceStatus === 'approved' ? '已确认应付' : '待审批' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-button link @click="showInvoiceItems(row)">匹配明细</el-button>
            <el-button v-if="row.invoiceStatus === 'pending'" link type="primary" @click="approveInvoice(row)" v-hasPermi="['finance:payable:approve']">确认应付</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-table v-else v-loading="loading" :data="paymentRows" row-key="paymentId">
        <el-table-column label="付款申请" prop="paymentNo" min-width="190" show-overflow-tooltip />
        <el-table-column label="供应商" prop="counterpartyName" min-width="180" show-overflow-tooltip />
        <el-table-column label="应付单号" prop="payableNo" min-width="180" show-overflow-tooltip />
        <el-table-column label="计划付款日" prop="requestedPayDate" width="120" />
        <el-table-column label="申请金额" align="right" width="130"><template #default="{ row }">{{ money(row.paymentAmount) }}</template></el-table-column>
        <el-table-column label="已付金额" align="right" width="120"><template #default="{ row }">{{ money(row.paidAmount) }}</template></el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }"><el-tag :type="paymentTag(row.paymentStatus)" effect="light">{{ paymentStatusLabel(row.paymentStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="申请 / 审批人" min-width="150">
          <template #default="{ row }">
            <span>{{ row.createBy || '-' }}</span>
            <small class="cell-sub">审批：{{ row.approveBy || '待审批' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="170" show-overflow-tooltip />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.paymentStatus === 'submitted'" link type="primary" @click="approvePayment(row)" v-hasPermi="['finance:payable:approve']">审批</el-button>
            <el-button v-if="row.paymentStatus === 'submitted'" link type="danger" @click="rejectPayment(row)" v-hasPermi="['finance:payable:approve']">驳回</el-button>
            <el-button v-if="['approved', 'partially_paid'].includes(row.paymentStatus)" link type="primary" @click="openExecute(row)" v-hasPermi="['finance:payable:payment']">执行付款</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="currentTotal > 0" :total="currentTotal" v-model:page="currentQuery.pageNum" v-model:limit="currentQuery.pageSize" @pagination="loadCurrent" />
    </section>

    <el-dialog v-model="invoiceOpen" title="登记供应商发票" width="min(1080px, 94vw)" append-to-body destroy-on-close>
      <el-form ref="invoiceFormRef" :model="invoiceForm" :rules="invoiceRules" label-width="110px">
        <div class="form-grid">
          <el-form-item label="采购单" prop="purchaseNo" class="span-2">
            <el-select v-model="invoiceForm.purchaseNo" filterable remote reserve-keyword :remote-method="searchPurchases" :loading="purchaseLoading" placeholder="输入采购单号或供应商" style="width: 100%" @change="loadPurchaseContext">
              <el-option v-for="item in purchaseOptions" :key="item.purchaseNo" :label="`${item.purchaseNo} · ${item.supplierName}`" :value="item.purchaseNo">
                <div class="purchase-option"><span>{{ item.purchaseNo }} · {{ item.supplierName }}</span><small>已收 {{ item.receivedQuantity }}/{{ item.orderQuantity }}</small></div>
              </el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="供应商发票号" prop="supplierInvoiceNo">
            <el-input v-model="invoiceForm.supplierInvoiceNo" placeholder="纸质或电子发票号码" />
          </el-form-item>
          <el-form-item label="开票日期" prop="invoiceDate">
            <el-date-picker v-model="invoiceForm.invoiceDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="到期日" prop="dueDate">
            <el-date-picker v-model="invoiceForm.dueDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="发票价税合计" prop="totalAmount">
            <el-input-number v-model="invoiceForm.totalAmount" :precision="2" :min="0" controls-position="right" style="width: 100%" />
          </el-form-item>
        </div>

        <div v-if="invoiceContext.purchaseNo" class="invoice-context">
          <div><span>供应商</span><b>{{ invoiceContext.supplierName }}</b></div>
          <div><span>采购金额</span><b>{{ money(invoiceContext.totalWithTax) }}</b></div>
          <div><span>入库单</span><b :title="invoiceContext.receiptRefs">{{ invoiceContext.receiptRefs || '-' }}</b></div>
          <div><span>仓库</span><b>{{ invoiceContext.warehouseName || '-' }}</b></div>
        </div>

        <el-table :data="invoiceForm.items" border class="invoice-lines" empty-text="选择采购单后加载可开票明细">
          <el-table-column label="SKU" min-width="180"><template #default="{ row }"><b>{{ row.skuCode }}</b><small class="cell-sub">{{ row.skuName }}</small></template></el-table-column>
          <el-table-column label="采购数量" prop="orderQuantity" width="90" align="right" />
          <el-table-column label="累计入库" prop="receivedQuantity" width="90" align="right" />
          <el-table-column label="已占发票" prop="invoicedQuantity" width="90" align="right" />
          <el-table-column label="本次数量" width="135"><template #default="{ row }"><el-input-number v-model="row.invoiceQuantity" :min="0" :precision="2" controls-position="right" /></template></el-table-column>
          <el-table-column label="采购单价" width="105" align="right"><template #default="{ row }">{{ number(row.orderUnitPrice) }}</template></el-table-column>
          <el-table-column label="发票单价" width="145"><template #default="{ row }"><el-input-number v-model="row.invoiceUnitPrice" :min="0" :precision="4" controls-position="right" /></template></el-table-column>
          <el-table-column label="税率" width="100"><template #default="{ row }">{{ number(row.taxRate * 100) }}%</template></el-table-column>
          <el-table-column label="价税合计" width="120" align="right"><template #default="{ row }">{{ money(invoiceLineTotal(row)) }}</template></el-table-column>
        </el-table>

        <div class="invoice-footer-grid">
          <div class="tolerance-box">
            <span>匹配容差</span>
            <label>单价 <el-input-number v-model="invoiceForm.priceTolerancePercent" :min="0" :precision="2" size="small" /> %</label>
            <label>数量 <el-input-number v-model="invoiceForm.quantityTolerance" :min="0" :precision="2" size="small" /></label>
            <label>总额 <el-input-number v-model="invoiceForm.amountTolerance" :min="0" :precision="2" size="small" /></label>
          </div>
          <div class="invoice-total">
            <span>明细价税合计</span>
            <strong>{{ money(calculatedInvoiceTotal) }}</strong>
            <small :class="{ 'overdue-text': invoiceHeaderDiff !== 0 }">票面差额 {{ money(invoiceHeaderDiff) }}</small>
          </div>
        </div>
        <el-form-item label="备注"><el-input v-model="invoiceForm.remark" type="textarea" :rows="2" placeholder="合同、发票附件或异常说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="invoiceOpen = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitInvoice">保存并执行三单匹配</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="invoiceItemsOpen" :title="`三单匹配明细 · ${selectedInvoice.invoiceNo || ''}`" width="min(1080px, 94vw)" append-to-body>
      <el-alert v-if="selectedInvoice.exceptionReason" type="warning" :closable="false" :title="`例外放行：${selectedInvoice.exceptionReason}`" class="mb16" />
      <el-table :data="invoiceItemRows" border>
        <el-table-column label="SKU" min-width="170"><template #default="{ row }"><b>{{ row.skuCode }}</b><small class="cell-sub">{{ row.skuName }}</small></template></el-table-column>
        <el-table-column label="订单数量" prop="orderQuantity" width="90" align="right" />
        <el-table-column label="累计入库" prop="receivedQuantity" width="90" align="right" />
        <el-table-column label="本次开票" prop="invoiceQuantity" width="90" align="right" />
        <el-table-column label="订单单价" prop="orderUnitPrice" width="100" align="right" />
        <el-table-column label="发票单价" prop="invoiceUnitPrice" width="100" align="right" />
        <el-table-column label="单价偏差" width="100" align="right"><template #default="{ row }">{{ number(row.priceVariancePercent) }}%</template></el-table-column>
        <el-table-column label="结果" width="110" align="center"><template #default="{ row }"><el-tag :type="row.lineMatchStatus === 'matched' ? 'success' : 'danger'">{{ row.lineMatchStatus === 'matched' ? '通过' : '差异' }}</el-tag></template></el-table-column>
        <el-table-column label="说明" prop="matchMessage" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog v-model="exceptionOpen" title="三单匹配例外放行" width="520px" append-to-body>
      <el-alert type="warning" :closable="false" title="该发票存在数量、单价或总额差异。放行后将按发票金额生成应付和凭证。" />
      <el-form ref="exceptionFormRef" :model="exceptionForm" :rules="exceptionRules" label-width="92px" class="dialog-form">
        <el-form-item label="发票号"><el-input :model-value="exceptionForm.supplierInvoiceNo" disabled /></el-form-item>
        <el-form-item label="放行原因" prop="exceptionReason"><el-input v-model="exceptionForm.exceptionReason" type="textarea" :rows="4" placeholder="至少填写 5 个字，说明差异原因和审批依据" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="exceptionOpen = false">取消</el-button><el-button type="warning" @click="submitExceptionApproval">确认例外放行</el-button></template>
    </el-dialog>

    <el-dialog v-model="paymentRequestOpen" title="发起付款申请" width="540px" append-to-body>
      <el-form ref="paymentRequestFormRef" :model="paymentRequestForm" :rules="paymentRequestRules" label-width="108px">
        <el-form-item label="应付单"><el-input v-model="paymentRequestForm.payableNo" disabled /></el-form-item>
        <el-form-item label="供应商"><el-input v-model="paymentRequestForm.counterpartyName" disabled /></el-form-item>
        <el-form-item label="可申请余额"><el-input :model-value="money(paymentRequestForm.availableAmount)" disabled /></el-form-item>
        <el-form-item label="申请金额" prop="paymentAmount"><el-input-number v-model="paymentRequestForm.paymentAmount" :min="0.01" :max="Number(paymentRequestForm.availableAmount || 0)" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="计划付款日" prop="requestedPayDate"><el-date-picker v-model="paymentRequestForm.requestedPayDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
        <el-form-item label="申请说明"><el-input v-model="paymentRequestForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="paymentRequestOpen = false">取消</el-button><el-button type="primary" @click="submitPaymentRequest">提交审批</el-button></template>
    </el-dialog>

    <el-dialog v-model="executeOpen" title="执行银行付款" width="560px" append-to-body>
      <el-form ref="executeFormRef" :model="executeForm" :rules="executeRules" label-width="108px">
        <el-form-item label="付款申请"><el-input v-model="executeForm.paymentNo" disabled /></el-form-item>
        <el-form-item label="待付金额"><el-input :model-value="money(executeForm.requestRemain)" disabled /></el-form-item>
        <el-form-item label="付款账户" prop="bankAccountId">
          <el-select v-model="executeForm.bankAccountId" placeholder="选择银行账户" style="width: 100%">
            <el-option v-for="item in bankOptions" :key="item.bankAccountId" :label="`${item.accountName} · ${maskBankNo(item.bankNo)}`" :value="item.bankAccountId" />
          </el-select>
        </el-form-item>
        <el-form-item label="本次付款" prop="executeAmount"><el-input-number v-model="executeForm.executeAmount" :min="0.01" :max="Number(executeForm.requestRemain || 0)" :precision="2" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="付款时间" prop="executeTime"><el-date-picker v-model="executeForm.executeTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" /></el-form-item>
        <el-form-item label="银行流水号" prop="bankReference"><el-input v-model="executeForm.bankReference" placeholder="网银回单或交易流水号（必填）" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="executeForm.remark" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="executeOpen = false">取消</el-button><el-button type="primary" @click="submitExecute">确认付款并自动核销</el-button></template>
    </el-dialog>

    <el-dialog v-model="writeoffOpen" title="手工核销应付" width="520px" append-to-body>
      <el-form ref="writeoffFormRef" :model="writeoffForm" :rules="writeoffRules" label-width="100px">
        <el-form-item label="应付单号"><el-input v-model="writeoffForm.payableNo" disabled /></el-form-item>
        <el-form-item label="付款流水" prop="cashFlowNo">
          <el-select v-model="writeoffForm.cashFlowNo" filterable placeholder="选择已入账且未完全核销的支出流水" style="width:100%" @change="handleWriteoffFlowChange">
            <el-option v-for="item in writeoffFlowOptions" :key="item.flowNo" :value="item.flowNo" :label="`${item.flowNo} · ${item.counterpartyName || '未填写往来方'} · 可用 ${money(item.availableAmount)}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="核销金额" prop="writeoffAmount"><el-input-number v-model="writeoffForm.writeoffAmount" :precision="2" :min="0.01" controls-position="right" style="width: 100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="writeoffForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="writeoffOpen = false">取消</el-button><el-button type="primary" @click="submitWriteoff">确认核销</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="FinancePayable">
import { ElMessageBox } from 'element-plus'
import {
  addPaymentRequest,
  addSupplierInvoice,
  approvePaymentRequest,
  approveSupplierInvoice,
  executePaymentRequest,
  getInvoicePurchaseContext,
  getPayableAging,
  getPayableSummary,
  listInvoicePurchaseOptions,
  listPayable,
  listPaymentBankOptions,
  listPaymentRequest,
  listSupplierInvoice,
  listSupplierInvoiceItems,
  rejectPaymentRequest,
  writeoffPayable
} from '@/api/finance/payable'
import { listCashFlow } from '@/api/finance/cashflow'

const { proxy } = getCurrentInstance()
const activeTab = ref('payable')
const loading = ref(false)
const submitLoading = ref(false)
const summary = ref({})
const agingRows = ref([])
const payableRows = ref([])
const invoiceRows = ref([])
const paymentRows = ref([])
const payableTotal = ref(0)
const invoiceTotal = ref(0)
const paymentTotal = ref(0)
const invoiceOpen = ref(false)
const invoiceItemsOpen = ref(false)
const exceptionOpen = ref(false)
const paymentRequestOpen = ref(false)
const executeOpen = ref(false)
const writeoffOpen = ref(false)
const purchaseLoading = ref(false)
const purchaseOptions = ref([])
const bankOptions = ref([])
const writeoffFlowOptions = ref([])
const invoiceItemRows = ref([])
const selectedInvoice = ref({})
const invoiceContext = ref({})

const payableQuery = ref({ pageNum: 1, pageSize: 10, payableNo: undefined, counterpartyName: undefined, periodCode: undefined, dueStatus: undefined, billStatus: undefined })
const invoiceQuery = ref({ pageNum: 1, pageSize: 10, invoiceNo: undefined, purchaseNo: undefined, supplierName: undefined, matchStatus: undefined, invoiceStatus: undefined })
const paymentQuery = ref({ pageNum: 1, pageSize: 10, paymentNo: undefined, payableNo: undefined, counterpartyName: undefined, paymentStatus: undefined })

const invoiceForm = ref({})
const exceptionForm = ref({})
const paymentRequestForm = ref({})
const executeForm = ref({})
const writeoffForm = ref({})

const invoiceRules = {
  purchaseNo: [{ required: true, message: '请选择采购单', trigger: 'change' }],
  supplierInvoiceNo: [{ required: true, message: '供应商发票号不能为空', trigger: 'blur' }],
  invoiceDate: [{ required: true, message: '开票日期不能为空', trigger: 'change' }],
  dueDate: [{ required: true, message: '到期日不能为空', trigger: 'change' }],
  totalAmount: [{ required: true, message: '发票价税合计不能为空', trigger: 'blur' }]
}
const exceptionRules = { exceptionReason: [{ required: true, min: 5, message: '请填写至少 5 个字的例外放行原因', trigger: 'blur' }] }
const paymentRequestRules = {
  paymentAmount: [{ required: true, message: '申请金额不能为空', trigger: 'blur' }],
  requestedPayDate: [{ required: true, message: '计划付款日不能为空', trigger: 'change' }]
}
const executeRules = {
  bankAccountId: [{ required: true, message: '请选择付款账户', trigger: 'change' }],
  executeAmount: [{ required: true, message: '付款金额不能为空', trigger: 'blur' }],
  executeTime: [{ required: true, message: '付款时间不能为空', trigger: 'change' }],
  bankReference: [{ required: true, message: '银行流水号不能为空', trigger: 'blur' }]
}
const writeoffRules = {
  cashFlowNo: [{ required: true, message: '付款流水号不能为空', trigger: 'blur' }],
  writeoffAmount: [{ required: true, message: '核销金额不能为空', trigger: 'blur' }]
}

const currentQuery = computed(() => activeTab.value === 'payable' ? payableQuery.value : activeTab.value === 'invoice' ? invoiceQuery.value : paymentQuery.value)
const currentTotal = computed(() => activeTab.value === 'payable' ? payableTotal.value : activeTab.value === 'invoice' ? invoiceTotal.value : paymentTotal.value)
const calculatedInvoiceTotal = computed(() => invoiceForm.value.items?.reduce((sum, row) => sum + invoiceLineTotal(row), 0) || 0)
const invoiceHeaderDiff = computed(() => Number(((Number(invoiceForm.value.totalAmount || 0) - calculatedInvoiceTotal.value)).toFixed(2)))
const maxAgingAmount = computed(() => Math.max(...agingRows.value.map(item => Number(item.remainAmount || 0)), 1))

function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function number(value) { return Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 4 }) }
function localDate(value) { return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10) }
function today() { return localDate(new Date()) }
function now() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 19).replace('T', ' ') }
function addDays(date, days) { const value = new Date(`${date}T00:00:00`); value.setDate(value.getDate() + days); return localDate(value) }
function agingPercent(value) { return Math.max(4, Math.round(Number(value || 0) * 100 / maxAgingAmount.value)) }
function agingLabel(value) { return ({ not_due: '未到期', no_due_date: '未设到期日', '1_30': '逾期 1-30 天', '31_60': '逾期 31-60 天', '61_90': '逾期 61-90 天', over_90: '逾期 90 天以上' })[value] || value || '-' }
function sourceLabel(value) { return ({ supplier_invoice: '供应商发票', supplier_reconcile: '供应商对账', expense: '费用报销' })[value] || value || '-' }
function billStatusLabel(value) { return ({ open: '未付款', partial: '部分付款', settled: '已结清' })[value] || value || '-' }
function billTag(value) { return ({ open: 'warning', partial: 'primary', settled: 'success' })[value] || 'info' }
function matchLabel(value) { return ({ matched: '匹配通过', exception: '存在差异', override: '例外放行' })[value] || value || '-' }
function matchTag(value) { return ({ matched: 'success', exception: 'danger', override: 'warning' })[value] || 'info' }
function paymentStatusLabel(value) { return ({ submitted: '待审批', approved: '待付款', partially_paid: '部分付款', paid: '已付清', rejected: '已驳回', cancelled: '已取消' })[value] || value || '-' }
function paymentTag(value) { return ({ submitted: 'warning', approved: 'primary', partially_paid: 'primary', paid: 'success', rejected: 'danger', cancelled: 'info' })[value] || 'info' }
function maskBankNo(value) { const text = String(value || ''); return text.length > 8 ? `${text.slice(0, 4)} **** ${text.slice(-4)}` : text }
function invoiceLineTotal(row) { return Number(row.invoiceQuantity || 0) * Number(row.invoiceUnitPrice || 0) * (1 + Number(row.taxRate || 0)) }

async function loadSummary() {
  const [summaryRes, agingRes] = await Promise.all([getPayableSummary({}), getPayableAging({})])
  summary.value = summaryRes.data || {}
  agingRows.value = agingRes.data || []
}
async function loadCurrent() {
  loading.value = true
  try {
    if (activeTab.value === 'payable') {
      const res = await listPayable(payableQuery.value); payableRows.value = res.rows || []; payableTotal.value = res.total || 0
    } else if (activeTab.value === 'invoice') {
      const res = await listSupplierInvoice(invoiceQuery.value); invoiceRows.value = res.rows || []; invoiceTotal.value = res.total || 0
    } else {
      const res = await listPaymentRequest(paymentQuery.value); paymentRows.value = res.rows || []; paymentTotal.value = res.total || 0
    }
  } finally { loading.value = false }
}
async function refreshAll() { await Promise.all([loadSummary(), loadCurrent()]) }
function handleTabChange() { loadCurrent() }
function queryCurrent() { currentQuery.value.pageNum = 1; loadCurrent() }
function resetCurrent() {
  const refName = activeTab.value === 'payable' ? 'payableQueryRef' : activeTab.value === 'invoice' ? 'invoiceQueryRef' : 'paymentQueryRef'
  proxy.resetForm(refName); queryCurrent()
}
function filterPayable(dueStatus) { activeTab.value = 'payable'; payableQuery.value.dueStatus = dueStatus; payableQuery.value.billStatus = dueStatus ? undefined : 'open'; queryCurrent() }
function filterInvoice(matchStatus) { activeTab.value = 'invoice'; invoiceQuery.value.invoiceStatus = 'pending'; invoiceQuery.value.matchStatus = matchStatus; queryCurrent() }
function filterPayment() { activeTab.value = 'payment'; paymentQuery.value.paymentStatus = undefined; queryCurrent() }

function resetInvoiceForm() {
  invoiceContext.value = {}
  invoiceForm.value = { purchaseNo: undefined, supplierInvoiceNo: undefined, invoiceDate: today(), dueDate: addDays(today(), 30), totalAmount: 0, priceTolerancePercent: 3, quantityTolerance: 0, amountTolerance: 1, items: [], remark: undefined }
}
async function openInvoiceDialog() { resetInvoiceForm(); invoiceOpen.value = true; await searchPurchases('') }
async function searchPurchases(keyword) {
  purchaseLoading.value = true
  try { const res = await listInvoicePurchaseOptions({ keyword }); purchaseOptions.value = res.data || [] } finally { purchaseLoading.value = false }
}
async function loadPurchaseContext(purchaseNo) {
  if (!purchaseNo) return
  const res = await getInvoicePurchaseContext(purchaseNo)
  const context = res.data || {}
  invoiceContext.value = context
  invoiceForm.value.priceTolerancePercent = Number(context.priceTolerancePercent ?? 3)
  invoiceForm.value.quantityTolerance = Number(context.quantityTolerance ?? 0)
  invoiceForm.value.amountTolerance = Number(context.amountTolerance ?? 1)
  invoiceForm.value.dueDate = addDays(invoiceForm.value.invoiceDate || today(), Number(context.paymentDays || 30))
  invoiceForm.value.items = (context.items || []).filter(item => Number(item.availableReceivedQuantity) > 0).map(item => ({ ...item, invoiceQuantity: Number(item.availableReceivedQuantity), invoiceUnitPrice: Number(item.orderUnitPrice), taxRate: Number(item.taxRate || 0) }))
  invoiceForm.value.totalAmount = Number(calculatedInvoiceTotal.value.toFixed(2))
}
function submitInvoice() {
  proxy.$refs.invoiceFormRef.validate(async valid => {
    if (!valid) return
    if (!invoiceForm.value.items?.length) return proxy.$modal.msgError('该采购单没有可开票的已入库数量')
    submitLoading.value = true
    try {
      const res = await addSupplierInvoice(invoiceForm.value)
      const matched = res.data?.matchStatus === 'matched'
      proxy.$modal.msgSuccess(matched ? '发票登记成功，三单匹配通过' : '发票登记成功，检测到匹配差异')
      invoiceOpen.value = false
      activeTab.value = 'invoice'
      invoiceQuery.value.invoiceStatus = 'pending'
      await refreshAll()
    } finally { submitLoading.value = false }
  })
}
async function showInvoiceItems(row) { selectedInvoice.value = row; const res = await listSupplierInvoiceItems(row.invoiceNo); invoiceItemRows.value = res.data || []; invoiceItemsOpen.value = true }
function approveInvoice(row) {
  if (row.matchStatus === 'exception') {
    exceptionForm.value = { invoiceNo: row.invoiceNo, supplierInvoiceNo: row.supplierInvoiceNo, exceptionReason: '' }
    exceptionOpen.value = true
    return
  }
  proxy.$modal.confirm(`确认发票 ${row.supplierInvoiceNo} 并生成应付和会计凭证？`).then(() => approveSupplierInvoice(row.invoiceNo, { overrideMatch: false })).then(async () => { proxy.$modal.msgSuccess('应付确认成功'); await refreshAll() }).catch(() => {})
}
function submitExceptionApproval() {
  proxy.$refs.exceptionFormRef.validate(valid => {
    if (!valid) return
    approveSupplierInvoice(exceptionForm.value.invoiceNo, { overrideMatch: true, exceptionReason: exceptionForm.value.exceptionReason }).then(async () => { proxy.$modal.msgSuccess('例外放行并生成应付成功'); exceptionOpen.value = false; await refreshAll() })
  })
}

function openPaymentRequest(row) { paymentRequestForm.value = { payableNo: row.payableNo, counterpartyName: row.counterpartyName, availableAmount: Number(row.availableAmount || 0), paymentAmount: Number(row.availableAmount || 0), requestedPayDate: today(), remark: undefined }; paymentRequestOpen.value = true }
function submitPaymentRequest() {
  proxy.$refs.paymentRequestFormRef.validate(valid => {
    if (!valid) return
    addPaymentRequest(paymentRequestForm.value.payableNo, paymentRequestForm.value).then(async () => { proxy.$modal.msgSuccess('付款申请已提交'); paymentRequestOpen.value = false; activeTab.value = 'payment'; await refreshAll() })
  })
}
function approvePayment(row) { proxy.$modal.confirm(`确认审批付款申请 ${row.paymentNo}，金额 ${money(row.paymentAmount)}？`).then(() => approvePaymentRequest(row.paymentNo)).then(async () => { proxy.$modal.msgSuccess('付款申请审批通过'); await refreshAll() }).catch(() => {}) }
async function rejectPayment(row) {
  try {
    const result = await ElMessageBox.prompt(`驳回付款申请 ${row.paymentNo} 后，本次申请金额会释放，可重新发起付款申请。`, '驳回付款申请', {
      confirmButtonText: '确认驳回', cancelButtonText: '取消', inputPlaceholder: '请输入驳回原因（至少5个字）',
      inputValidator: value => String(value || '').trim().length >= 5 || '驳回原因至少5个字'
    })
    await rejectPaymentRequest(row.paymentNo, { rejectReason: String(result.value).trim() })
    proxy.$modal.msgSuccess('付款申请已驳回')
    await refreshAll()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}
async function openExecute(row) {
  if (!bankOptions.value.length) { const res = await listPaymentBankOptions(); bankOptions.value = res.data || [] }
  const requestRemain = Number(row.remainAmount || 0)
  executeForm.value = { paymentNo: row.paymentNo, requestRemain, bankAccountId: bankOptions.value[0]?.bankAccountId, executeAmount: requestRemain, executeTime: now(), bankReference: undefined, remark: undefined }
  executeOpen.value = true
}
function submitExecute() {
  proxy.$refs.executeFormRef.validate(valid => {
    if (!valid) return
    proxy.$modal.confirm(`确认从银行账户支付 ${money(executeForm.value.executeAmount)}？系统将同步生成流水、核销应付和付款凭证。`).then(() => executePaymentRequest(executeForm.value.paymentNo, executeForm.value)).then(async () => { proxy.$modal.msgSuccess('付款执行并自动核销成功'); executeOpen.value = false; await refreshAll() }).catch(() => {})
  })
}

async function openWriteoff(row) {
  writeoffForm.value = { payableNo: row.payableNo, cashFlowNo: undefined, writeoffAmount: Number(row.remainAmount || 0), remark: undefined }
  writeoffOpen.value = true
  const response = await listCashFlow({ pageNum: 1, pageSize: 100, flowType: 'out', entryStatus: 'posted' })
  writeoffFlowOptions.value = (response.rows || []).filter(item => Number(item.availableAmount || 0) > 0)
}
function handleWriteoffFlowChange(flowNo) {
  const row = writeoffFlowOptions.value.find(item => item.flowNo === flowNo)
  if (row) writeoffForm.value.writeoffAmount = Math.min(Number(writeoffForm.value.writeoffAmount || 0), Number(row.availableAmount || 0))
}
function submitWriteoff() {
  proxy.$refs.writeoffFormRef.validate(valid => {
    if (!valid) return
    writeoffPayable(writeoffForm.value.payableNo, writeoffForm.value).then(async () => { proxy.$modal.msgSuccess('应付核销成功'); writeoffOpen.value = false; await refreshAll() })
  })
}

onMounted(refreshAll)
</script>

<style scoped>
.payable-center { color: #20243a; }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: 24px; padding: 20px 24px; margin-bottom: 14px; border: 1px solid rgba(108, 92, 231, .12); border-radius: 14px; background: linear-gradient(110deg, #fff 58%, #f0eeff); box-shadow: 0 10px 28px rgba(67, 62, 118, .07); }
.eyebrow { display: inline-block; margin-bottom: 5px; color: #6c5ce7; font-size: 11px; font-weight: 800; letter-spacing: .08em; }
.page-heading h1 { margin: 0; font-size: 24px; line-height: 1.25; }
.page-heading p { margin: 7px 0 0; color: #7b849f; font-size: 13px; }
.heading-actions { display: flex; flex: none; gap: 8px; }
.metric-grid { display: grid; grid-template-columns: repeat(5, minmax(160px, 1fr)); gap: 12px; margin-bottom: 12px; }
.metric-card { min-width: 0; padding: 16px 18px; text-align: left; color: inherit; border: 1px solid #eceef7; border-radius: 12px; background: #fff; box-shadow: 0 8px 22px rgba(45, 50, 80, .05); cursor: pointer; transition: transform .18s ease, box-shadow .18s ease; }
.metric-card:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(75, 68, 142, .11); }
.metric-card::before { content: ''; display: block; width: 28px; height: 3px; margin-bottom: 12px; border-radius: 3px; background: #6c5ce7; }
.metric-card.danger::before { background: #ef6b75; }.metric-card.warning::before { background: #f0a94b; }.metric-card.purple::before { background: #9b72e8; }.metric-card.teal::before { background: #14b8a6; }
.metric-label { display: block; color: #7c849c; font-size: 12px; }
.metric-card strong { display: block; margin: 6px 0 5px; font-size: 22px; line-height: 1.1; }
.metric-card > span:last-child { color: #9299ad; font-size: 11px; }
.aging-strip { display: grid; grid-template-columns: 170px repeat(5, minmax(130px, 1fr)); align-items: center; gap: 18px; padding: 13px 18px; margin-bottom: 12px; border: 1px solid #eceef7; border-radius: 12px; background: #fff; }
.aging-title { display: flex; flex-direction: column; }.aging-title strong { font-size: 14px; }.aging-title span { margin-top: 3px; color: #9299ad; font-size: 11px; }
.aging-meta { display: flex; justify-content: space-between; gap: 8px; margin-bottom: 6px; color: #6e768f; font-size: 11px; }.aging-meta b { color: #353b54; font-weight: 650; }
.aging-track { height: 5px; overflow: hidden; border-radius: 4px; background: #eff1f7; }.aging-track i { display: block; height: 100%; border-radius: inherit; background: linear-gradient(90deg, #6c5ce7, #a29bfe); }
.workbench { overflow: hidden; border: 1px solid #e9ebf4; border-radius: 14px; background: #fff; box-shadow: 0 12px 30px rgba(43, 48, 76, .06); }
.tabs-row { padding: 0 20px; border-bottom: 1px solid #eff0f5; }.tabs-row :deep(.el-tabs__header) { margin: 0; }.tabs-row :deep(.el-tabs__nav-wrap::after) { display: none; }.tabs-row :deep(.el-tabs__item) { height: 48px; font-weight: 650; }
.search-form { display: flex; align-items: center; gap: 2px; padding: 14px 18px 2px; background: #fbfbfe; border-bottom: 1px solid #eff0f5; }
.search-form :deep(.el-form-item) { margin-right: 12px; margin-bottom: 12px; }.search-form :deep(.el-input) { width: 175px; }.search-form :deep(.el-select) { width: 140px; }.search-form :deep(.el-date-editor) { width: 142px; }
.search-actions { margin-left: auto; }
.workbench :deep(.el-table) { --el-table-header-bg-color: #f7f8fc; }.workbench :deep(.el-table th.el-table__cell) { color: #525c75; font-weight: 700; }.workbench :deep(.el-table .cell) { white-space: nowrap; }
.cell-sub { display: block; max-width: 100%; margin-top: 3px; overflow: hidden; color: #969daf; font-size: 11px; font-weight: 400; text-overflow: ellipsis; white-space: nowrap; }
.overdue-text { color: #e24d5c !important; }.mb16 { margin-bottom: 16px; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 14px; }.form-grid .span-2 { grid-column: span 2; }
.purchase-option { display: flex; justify-content: space-between; gap: 16px; }.purchase-option small { color: #9299ad; }
.invoice-context { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; padding: 12px 14px; margin: 0 0 14px 110px; border-radius: 10px; background: #f7f7fd; }.invoice-context div { min-width: 0; }.invoice-context span { display: block; color: #9299ad; font-size: 11px; }.invoice-context b { display: block; margin-top: 4px; overflow: hidden; font-size: 13px; text-overflow: ellipsis; white-space: nowrap; }
.invoice-lines { margin-bottom: 14px; }.invoice-lines :deep(.el-input-number) { width: 116px; }
.invoice-footer-grid { display: grid; grid-template-columns: 1fr 220px; gap: 16px; margin: 0 0 14px 110px; }.tolerance-box { display: flex; align-items: center; gap: 12px; padding: 10px 12px; border-radius: 10px; background: #f8f9fc; }.tolerance-box > span { color: #7b849f; font-size: 12px; font-weight: 700; }.tolerance-box label { display: flex; align-items: center; gap: 5px; color: #6f778f; font-size: 11px; }.tolerance-box :deep(.el-input-number) { width: 82px; }
.invoice-total { display: flex; flex-direction: column; align-items: flex-end; padding: 10px 14px; border-radius: 10px; background: #f0eeff; }.invoice-total span { color: #7b749f; font-size: 11px; }.invoice-total strong { margin: 2px 0; color: #5f50da; font-size: 19px; }.invoice-total small { color: #858da4; }
.dialog-form { margin-top: 18px; }
@media (max-width: 1400px) { .metric-grid { grid-template-columns: repeat(3, 1fr); }.aging-strip { grid-template-columns: 150px repeat(3, 1fr); }.aging-item:nth-last-child(-n+2) { display: none; } }
@media (max-width: 900px) { .page-heading { align-items: flex-start; flex-direction: column; }.metric-grid { grid-template-columns: repeat(2, 1fr); }.aging-strip { grid-template-columns: 1fr 1fr; }.aging-title { grid-column: 1 / -1; }.search-form { align-items: stretch; flex-direction: column; }.search-actions { margin-left: 0; }.form-grid { grid-template-columns: 1fr; }.form-grid .span-2 { grid-column: span 1; }.invoice-context, .invoice-footer-grid { grid-template-columns: 1fr; margin-left: 0; } }
</style>
