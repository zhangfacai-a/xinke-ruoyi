<template>
  <div class="app-container ledger-page">
    <el-alert
      class="ledger-guide"
      type="info"
      :closable="false"
      show-icon
      title="已过账凭证会自动写入明细账，并重算对应期间的试算平衡；草稿凭证不会进入正式账簿。"
    />

    <el-tabs v-model="activeTab" class="ledger-tabs" @tab-change="handleTabChange">
      <el-tab-pane label="明细账" name="ledger" />
      <el-tab-pane label="试算平衡" name="trial" />
    </el-tabs>

    <el-form ref="queryRef" :model="queryParams" :inline="true" class="search-form">
      <el-form-item label="会计期间" prop="periodCode">
        <el-date-picker v-model="queryParams.periodCode" type="month" value-format="YYYY-MM" placeholder="选择月份" clearable style="width: 150px" />
      </el-form-item>
      <el-form-item label="科目编码" prop="subjectCode">
        <el-input v-model="queryParams.subjectCode" placeholder="如 1002" clearable style="width: 150px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="科目名称" prop="subjectName">
        <el-input v-model="queryParams.subjectName" placeholder="搜索科目" clearable style="width: 170px" @keyup.enter="handleQuery" />
      </el-form-item>
      <template v-if="activeTab === 'ledger'">
        <el-form-item label="凭证号" prop="voucherNo">
          <el-input v-model="queryParams.voucherNo" placeholder="搜索凭证号" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="来源单号" prop="sourceNo">
          <el-input v-model="queryParams.sourceNo" placeholder="搜索业务单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
      </template>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <template v-if="activeTab === 'trial'">
      <div class="balance-summary">
        <div class="summary-item"><span>期初借方</span><strong>{{ money(trialSummary.beginDebit) }}</strong></div>
        <div class="summary-item"><span>期初贷方</span><strong>{{ money(trialSummary.beginCredit) }}</strong></div>
        <div class="summary-item"><span>本期借方</span><strong>{{ money(trialSummary.currentDebit) }}</strong></div>
        <div class="summary-item"><span>本期贷方</span><strong>{{ money(trialSummary.currentCredit) }}</strong></div>
        <div class="summary-item result" :class="isBalanced ? 'balanced' : 'unbalanced'">
          <span>本期借贷差额</span>
          <strong>{{ money(currentDifference) }}</strong>
          <el-tag size="small" round :type="isBalanced ? 'success' : 'danger'">{{ isBalanced ? '平衡' : '不平衡' }}</el-tag>
        </div>
      </div>
    </template>

    <el-table v-if="activeTab === 'ledger'" v-loading="loading" :data="dataList" stripe>
      <el-table-column label="日期" prop="voucherDate" align="center" width="110" />
      <el-table-column label="期间" prop="periodCode" align="center" width="100" />
      <el-table-column label="凭证号" prop="voucherNo" min-width="170" show-overflow-tooltip />
      <el-table-column label="科目编码" prop="subjectCode" align="center" width="110" />
      <el-table-column label="科目名称" prop="subjectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="借方金额" align="right" width="135">
        <template #default="{ row }">{{ row.direction === 'debit' ? money(row.amount) : '-' }}</template>
      </el-table-column>
      <el-table-column label="贷方金额" align="right" width="135">
        <template #default="{ row }">{{ row.direction === 'credit' ? money(row.amount) : '-' }}</template>
      </el-table-column>
      <el-table-column label="往来单位" prop="counterpartyName" min-width="150" show-overflow-tooltip />
      <el-table-column label="业务来源" align="center" width="110"><template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template></el-table-column>
      <el-table-column label="来源单号" prop="sourceNo" min-width="160" show-overflow-tooltip />
    </el-table>

    <el-table v-else v-loading="loading" :data="dataList" stripe>
      <el-table-column label="期间" prop="periodCode" align="center" width="100" />
      <el-table-column label="科目编码" prop="subjectCode" align="center" width="120" />
      <el-table-column label="科目名称" prop="subjectName" min-width="170" show-overflow-tooltip />
      <el-table-column label="期初借方" align="right" width="130"><template #default="{ row }">{{ amountOrDash(row.beginDebit) }}</template></el-table-column>
      <el-table-column label="期初贷方" align="right" width="130"><template #default="{ row }">{{ amountOrDash(row.beginCredit) }}</template></el-table-column>
      <el-table-column label="本期借方" align="right" width="130"><template #default="{ row }">{{ amountOrDash(row.currentDebit) }}</template></el-table-column>
      <el-table-column label="本期贷方" align="right" width="130"><template #default="{ row }">{{ amountOrDash(row.currentCredit) }}</template></el-table-column>
      <el-table-column label="期末借方" align="right" width="130"><template #default="{ row }">{{ amountOrDash(row.endDebit) }}</template></el-table-column>
      <el-table-column label="期末贷方" align="right" width="130"><template #default="{ row }">{{ amountOrDash(row.endCredit) }}</template></el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="FinanceLedger">
import { listLedger, listTrialBalance, getTrialBalanceSummary } from '@/api/finance/ledger'

const { proxy } = getCurrentInstance()
const activeTab = ref('ledger')
const loading = ref(false)
const dataList = ref([])
const total = ref(0)
const trialSummary = ref({})
const now = new Date()
const defaultPeriod = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  periodCode: defaultPeriod,
  subjectCode: undefined,
  subjectName: undefined,
  voucherNo: undefined,
  sourceNo: undefined
})
const currentDifference = computed(() => Number(trialSummary.value.currentDebit || 0) - Number(trialSummary.value.currentCredit || 0))
const isBalanced = computed(() => Math.abs(currentDifference.value) < 0.01)

async function getList() {
  loading.value = true
  try {
    const request = activeTab.value === 'ledger' ? listLedger : listTrialBalance
    const response = await request(queryParams.value)
    dataList.value = response.rows || []
    total.value = response.total || 0
    if (activeTab.value === 'trial') {
      const summaryResponse = await getTrialBalanceSummary(queryParams.value)
      trialSummary.value = summaryResponse.data || {}
    }
  } finally {
    loading.value = false
  }
}

function handleTabChange() { queryParams.value.pageNum = 1; getList() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.value.periodCode = defaultPeriod
  handleQuery()
}
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function amountOrDash(value) { return Number(value || 0) === 0 ? '-' : money(value) }
function sourceLabel(value) { return ({ settlement: '平台结算', expense: '费用单', cash_flow: '资金流水', supplier_invoice: '供应商发票', payment: '付款' })[value] || value || '-' }

getList()
</script>

<style scoped>
.ledger-page { min-height: calc(100vh - 84px); }
.ledger-guide { margin-bottom: 14px; }
.ledger-tabs :deep(.el-tabs__header) { margin-bottom: 14px; }
.search-form { padding: 16px 18px 0; margin-bottom: 14px; background: #fff; border: 1px solid #ececf5; border-radius: 12px; }
.balance-summary { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin-bottom: 14px; }
.summary-item { display: flex; min-height: 84px; padding: 15px 16px; flex-direction: column; justify-content: center; gap: 6px; background: #fff; border: 1px solid #ececf5; border-radius: 12px; }
.summary-item span { color: #7d869d; font-size: 13px; }
.summary-item strong { color: #242a3d; font-size: 20px; }
.summary-item.result { position: relative; }
.summary-item.result .el-tag { position: absolute; top: 12px; right: 12px; }
.summary-item.balanced { border-color: #bcecdc; background: #f4fcf9; }
.summary-item.unbalanced { border-color: #ffc9ca; background: #fff7f7; }
@media (max-width: 1100px) { .balance-summary { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
@media (max-width: 680px) { .balance-summary { grid-template-columns: 1fr; } }
</style>
