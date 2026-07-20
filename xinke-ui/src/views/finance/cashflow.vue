<template>
  <div class="app-container cash-ledger-page">
    <section class="ledger-hero">
      <div>
        <span class="eyebrow">CASH LEDGER</span>
        <h1>资金出入账中心</h1>
        <p>统一记录银行与平台账户流水，控制草稿、入账、核销、凭证和冲销。</p>
      </div>
      <div class="hero-actions">
        <el-button icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['finance:cashflow:add']">新增流水</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card income"><span>本期流入</span><strong>{{ money(summary.inflowAmount) }}</strong><small>实际到账净额</small></article>
      <article class="metric-card expense"><span>本期流出</span><strong>{{ money(summary.outflowAmount) }}</strong><small>实际付出净额</small></article>
      <article class="metric-card net"><span>现金净流入</span><strong>{{ money(summary.netCashAmount) }}</strong><small>流入减流出</small></article>
      <article class="metric-card pending"><span>待分配金额</span><strong>{{ money(summary.unallocatedAmount) }}</strong><small>{{ summary.unmatchedCount || 0 }} 笔未完全核销</small></article>
      <article class="metric-card draft"><span>待入账</span><strong>{{ summary.draftCount || 0 }}</strong><small>草稿流水</small></article>
    </section>

    <section class="ledger-panel">
      <el-form ref="queryRef" :model="queryParams" :inline="true" class="filter-bar">
        <el-form-item label="业务日期">
          <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
        </el-form-item>
        <el-form-item label="流水">
          <el-input v-model="queryParams.flowNo" placeholder="流水号/交易参考号" clearable style="width: 190px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="方向">
          <el-select v-model="queryParams.flowType" placeholder="全部" clearable style="width: 110px">
            <el-option label="收入" value="in" /><el-option label="支出" value="out" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.entryStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="草稿" value="draft" /><el-option label="已入账" value="posted" /><el-option label="已冲销" value="reversed" /><el-option label="已作废" value="voided" />
          </el-select>
        </el-form-item>
        <el-form-item label="核销">
          <el-select v-model="queryParams.matchStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="未核销" value="unmatched" /><el-option label="部分核销" value="partial" /><el-option label="已核销" value="matched" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="dataList" row-key="cashFlowId" class="ledger-table">
        <el-table-column label="流水信息" min-width="190" fixed="left">
          <template #default="{ row }">
            <div class="primary-cell">{{ row.flowNo }}</div>
            <div class="secondary-cell">{{ row.externalReference || row.sourceNo || '无外部参考号' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="收支" width="120">
          <template #default="{ row }">
            <el-tag :type="row.flowType === 'in' ? 'success' : 'danger'" effect="light">{{ row.flowType === 'in' ? '收入' : '支出' }}</el-tag>
            <div class="secondary-cell">{{ categoryLabel(row.flowCategory) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="资金账户" min-width="170" show-overflow-tooltip>
          <template #default="{ row }"><div class="primary-cell">{{ row.accountName || '-' }}</div><div class="secondary-cell">{{ row.accountInstitution || '-' }}</div></template>
        </el-table-column>
        <el-table-column label="往来方" prop="counterpartyName" min-width="145" show-overflow-tooltip />
        <el-table-column label="业务金额" width="125" align="right"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="手续费" width="105" align="right"><template #default="{ row }">{{ money(row.feeAmount) }}</template></el-table-column>
        <el-table-column label="账户净额" width="125" align="right"><template #default="{ row }"><strong>{{ money(row.netAmount) }}</strong></template></el-table-column>
        <el-table-column label="核销进度" min-width="145">
          <template #default="{ row }">
            <div>{{ money(row.settledAmount) }} / {{ money(row.amount) }}</div>
            <div class="secondary-cell">可用 {{ money(row.availableAmount) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="业务日期" prop="businessDate" width="115" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="entryStatusType(row.entryStatus)" effect="plain">{{ entryStatusLabel(row.entryStatus) }}</el-tag>
            <div class="secondary-cell">{{ matchStatusLabel(row.matchStatus) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="凭证" min-width="150" show-overflow-tooltip><template #default="{ row }">{{ row.voucherNo || '未生成' }}</template></el-table-column>
        <el-table-column label="操作" width="175" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.entryStatus === 'draft'" link type="primary" @click="handlePost(row)" v-hasPermi="['finance:cashflow:post']">入账</el-button>
            <el-button v-if="row.entryStatus === 'draft'" link type="danger" @click="handleVoid(row)" v-hasPermi="['finance:cashflow:edit']">作废</el-button>
            <el-button v-if="row.entryStatus === 'posted' && Number(row.settledAmount || 0) === 0" link type="danger" @click="handleReverse(row)" v-hasPermi="['finance:cashflow:reverse']">冲销</el-button>
            <span v-if="row.entryStatus === 'reversed'" class="secondary-cell">{{ row.reversalFlowNo }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog v-model="open" title="登记资金流水" width="760px" append-to-body destroy-on-close>
      <el-alert title="业务金额用于应收应付核销；账户净额会根据收支方向和手续费自动计算。" type="info" :closable="false" class="mb16" />
      <el-form ref="cashFlowRef" :model="form" :rules="rules" label-width="105px">
        <el-row :gutter="18">
          <el-col :span="12"><el-form-item label="收支方向" prop="flowType"><el-radio-group v-model="form.flowType"><el-radio-button value="in">收入</el-radio-button><el-radio-button value="out">支出</el-radio-button></el-radio-group></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="业务分类" prop="flowCategory"><el-select v-model="form.flowCategory" style="width:100%"><el-option v-for="item in categoryOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="资金账户" prop="accountKey"><el-select v-model="form.accountKey" filterable style="width:100%" placeholder="选择银行账户或平台账户"><el-option v-for="item in accountOptions" :key="`${item.accountType}:${item.accountId}`" :label="accountOptionLabel(item)" :value="`${item.accountType}:${item.accountId}`" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="往来方类型"><el-select v-model="form.counterpartyType" style="width:100%"><el-option label="平台" value="platform" /><el-option label="供应商" value="supplier" /><el-option label="客户" value="customer" /><el-option label="员工" value="employee" /><el-option label="其他" value="other" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="往来方"><el-input v-model="form.counterpartyName" placeholder="平台、供应商、员工等" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="业务金额" prop="amount"><el-input-number v-model="form.amount" :precision="2" :min="0.01" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="手续费"><el-input-number v-model="form.feeAmount" :precision="2" :min="0" controls-position="right" style="width:100%" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="账户净额"><el-input :model-value="money(calculatedNetAmount)" disabled /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="流水时间" prop="flowTime"><el-date-picker v-model="form.flowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="业务日期" prop="businessDate"><el-date-picker v-model="form.businessDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="交易参考号"><el-input v-model="form.externalReference" placeholder="银行流水号/平台交易号" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="来源单号"><el-input v-model="form.sourceNo" placeholder="结算单、应收单或应付单" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="保存状态"><el-radio-group v-model="form.entryStatus"><el-radio value="draft">保存草稿</el-radio><el-radio value="posted">立即入账</el-radio></el-radio-group><el-checkbox v-if="form.entryStatus === 'posted'" v-model="form.generateVoucher" class="voucher-check">同时生成会计凭证</el-checkbox></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" @click="submitForm">保存流水</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceCashFlow">
import { ElMessageBox } from 'element-plus'
import { addCashFlow, getCashFlowAccountOptions, getCashFlowSummary, listCashFlow, postCashFlow, voidCashFlow, reverseCashFlow } from '@/api/finance/cashflow'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const open = ref(false)
const dataList = ref([])
const accountOptions = ref([])
const total = ref(0)
const summary = ref({})
const dateRange = ref(defaultDateRange())
const queryParams = ref({ pageNum: 1, pageSize: 10, flowNo: undefined, flowType: undefined, flowCategory: undefined, entryStatus: undefined, matchStatus: undefined })
const form = ref({})
const rules = {
  flowType: [{ required: true, message: '请选择收支方向', trigger: 'change' }],
  flowCategory: [{ required: true, message: '请选择业务分类', trigger: 'change' }],
  accountKey: [{ required: true, message: '请选择资金账户', trigger: 'change' }],
  amount: [{ required: true, message: '请输入业务金额', trigger: 'blur' }],
  flowTime: [{ required: true, message: '请选择流水时间', trigger: 'change' }],
  businessDate: [{ required: true, message: '请选择业务日期', trigger: 'change' }]
}
const categoryOptions = [
  { label: '平台/客户收款', value: 'receivable' }, { label: '销售回款', value: 'sales_receipt' },
  { label: '供应商付款', value: 'supplier_payment' }, { label: '费用付款', value: 'expense_payment' },
  { label: '客户退款', value: 'refund' }, { label: '税费支付', value: 'tax_payment' },
  { label: '工资支付', value: 'payroll' }, { label: '资本金', value: 'capital' }, { label: '其他', value: 'other' }
]
const calculatedNetAmount = computed(() => form.value.flowType === 'in'
  ? Number(form.value.amount || 0) - Number(form.value.feeAmount || 0)
  : Number(form.value.amount || 0) + Number(form.value.feeAmount || 0))

function localDate(value = new Date()) { return new Date(value.getTime() - value.getTimezoneOffset() * 60000).toISOString().slice(0, 10) }
function localDateTime() { return new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 19).replace('T', ' ') }
function defaultDateRange() { const now = new Date(); return [`${localDate(now).slice(0, 7)}-01`, localDate(now)] }
function queryWithDate() { return { ...queryParams.value, beginBusinessDate: dateRange.value?.[0], endBusinessDate: dateRange.value?.[1] } }
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function categoryLabel(value) { return categoryOptions.find(item => item.value === value)?.label || value || '其他' }
function entryStatusLabel(value) { return ({ draft: '草稿', posted: '已入账', reversed: '已冲销', voided: '已作废' })[value] || value || '-' }
function entryStatusType(value) { return ({ draft: 'info', posted: 'success', reversed: 'warning', voided: 'info' })[value] || 'info' }
function matchStatusLabel(value) { return ({ unmatched: '未核销', partial: '部分核销', matched: '已核销' })[value] || value || '-' }
function maskAccount(value) { const text = String(value || ''); return text.length > 8 ? `${text.slice(0, 4)}…${text.slice(-4)}` : text }
function accountOptionLabel(item) { return `${item.accountName} · ${item.institutionName || ''} · ${maskAccount(item.accountNo)}` }

async function getList() {
  loading.value = true
  try { const res = await listCashFlow(queryWithDate()); dataList.value = res.rows || []; total.value = res.total || 0 }
  finally { loading.value = false }
}
async function loadSummary() { const res = await getCashFlowSummary(queryWithDate()); summary.value = res.data || {} }
async function loadAccounts() { const res = await getCashFlowAccountOptions(); accountOptions.value = res.data || [] }
async function refreshAll() { await Promise.all([getList(), loadSummary(), loadAccounts()]) }
function handleQuery() { queryParams.value.pageNum = 1; refreshAll() }
function resetQuery() { queryParams.value = { pageNum: 1, pageSize: 10 }; dateRange.value = defaultDateRange(); refreshAll() }
function handleAdd() {
  const now = localDateTime()
  form.value = { flowType: 'in', flowCategory: 'receivable', counterpartyType: 'platform', amount: 0.01, feeAmount: 0, currency: 'CNY', flowTime: now, businessDate: now.slice(0, 10), entryStatus: 'posted', generateVoucher: true }
  open.value = true
}
function submitForm() {
  proxy.$refs.cashFlowRef.validate(async valid => {
    if (!valid) return
    if (calculatedNetAmount.value <= 0) return proxy.$modal.msgError('账户净额必须大于0')
    const [accountType, accountId] = form.value.accountKey.split(':')
    const payload = { ...form.value, bankAccountId: accountType === 'bank' ? Number(accountId) : undefined, platformAccountId: accountType === 'platform' ? Number(accountId) : undefined }
    delete payload.accountKey
    await addCashFlow(payload)
    proxy.$modal.msgSuccess(form.value.entryStatus === 'posted' ? '流水已入账' : '草稿保存成功')
    open.value = false
    await refreshAll()
  })
}
function handlePost(row) {
  proxy.$modal.confirm(`确认将流水 ${row.flowNo} 入账并生成会计凭证？`).then(async () => { await postCashFlow(row.flowNo, { generateVoucher: true }); proxy.$modal.msgSuccess('入账成功'); await refreshAll() })
}
function handleVoid(row) {
  proxy.$modal.confirm(`确认作废草稿流水 ${row.flowNo}？作废后不会影响资金余额。`).then(async () => {
    await voidCashFlow(row.flowNo)
    proxy.$modal.msgSuccess('草稿流水已作废')
    await refreshAll()
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
async function handleReverse(row) {
  try {
    const result = await ElMessageBox.prompt(`冲销后将生成一条反向流水，原流水 ${row.flowNo} 会保留。`, '冲销资金流水', { confirmButtonText: '确认冲销', cancelButtonText: '取消', inputPlaceholder: '请输入冲销原因（至少4个字）', inputValidator: value => String(value || '').trim().length >= 4 || '冲销原因至少4个字' })
    await reverseCashFlow(row.flowNo, { reason: result.value })
    proxy.$modal.msgSuccess('冲销成功')
    await refreshAll()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

refreshAll()
</script>

<style scoped>
.cash-ledger-page { min-width: 0; }
.ledger-hero, .ledger-panel { background: rgba(255,255,255,.92); border: 1px solid rgba(108,92,231,.10); border-radius: 14px; box-shadow: 0 10px 30px rgba(57,48,125,.07); }
.ledger-hero { display:flex; justify-content:space-between; align-items:center; padding:22px 26px; margin-bottom:16px; }
.eyebrow { color:#6c5ce7; font-size:12px; font-weight:750; letter-spacing:.04em; }
.ledger-hero h1 { margin:5px 0 4px; font-size:25px; color:#252a3d; }
.ledger-hero p { margin:0; color:#7b849e; }
.hero-actions { display:flex; gap:10px; }
.metric-grid { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:12px; margin-bottom:16px; }
.metric-card { position:relative; min-width:0; padding:16px 18px; background:#fff; border:1px solid #eceefa; border-radius:12px; overflow:hidden; }
.metric-card::before { content:''; position:absolute; left:0; top:0; width:4px; height:100%; background:#6c5ce7; }
.metric-card.income::before { background:#00b894; }.metric-card.expense::before { background:#ff7675; }.metric-card.net::before { background:#6c5ce7; }.metric-card.pending::before { background:#fdcb6e; }.metric-card.draft::before { background:#74b9ff; }
.metric-card span,.metric-card small { display:block; color:#7d879f; }.metric-card strong { display:block; margin:7px 0 5px; font-size:23px; color:#252a3d; white-space:nowrap; overflow:hidden; text-overflow:ellipsis; }
.ledger-panel { padding:16px; }.filter-bar { padding:2px 4px 8px; }.filter-bar :deep(.el-form-item) { margin-bottom:10px; }
.primary-cell { color:#30364b; font-weight:600; }.secondary-cell { margin-top:3px; color:#929bb0; font-size:12px; }.voucher-check { margin-left:20px; }.mb16 { margin-bottom:16px; }
.ledger-table :deep(.el-table__cell) { padding:9px 0; }
@media (max-width:1200px) { .metric-grid { grid-template-columns:repeat(3,minmax(0,1fr)); } }
@media (max-width:768px) { .ledger-hero { align-items:flex-start; gap:14px; flex-direction:column; }.metric-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }.hero-actions { width:100%; }.hero-actions .el-button { flex:1; } }
</style>
