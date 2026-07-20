<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="凭证号" prop="voucherNo">
        <el-input v-model="queryParams.voucherNo" placeholder="请输入凭证号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="期间" prop="periodCode">
        <el-input v-model="queryParams.periodCode" placeholder="如 2026-06" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="来源单号" prop="sourceNo">
        <el-input v-model="queryParams.sourceNo" placeholder="请输入来源单号" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="凭证号" prop="voucherNo" align="center" min-width="170" />
      <el-table-column label="期间" prop="periodCode" align="center" />
      <el-table-column label="凭证日期" prop="voucherDate" align="center" />
      <el-table-column label="来源" align="center"><template #default="{ row }">{{ sourceLabel(row.sourceType) }}</template></el-table-column>
      <el-table-column label="来源单号" prop="sourceNo" align="center" min-width="150" />
      <el-table-column label="摘要" prop="summary" align="center" min-width="180" show-overflow-tooltip />
      <el-table-column label="借方" align="right"><template #default="{ row }">{{ money(row.debitAmount) }}</template></el-table-column>
      <el-table-column label="贷方" align="right"><template #default="{ row }">{{ money(row.creditAmount) }}</template></el-table-column>
      <el-table-column label="状态" align="center"><template #default="{ row }"><el-tag round :type="voucherStatusType(row.voucherStatus)">{{ voucherStatusLabel(row.voucherStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" align="center" width="170" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleEntries(scope.row)" v-hasPermi="['finance:voucher:query']">分录</el-button>
          <el-button v-if="scope.row.voucherStatus === 'draft'" link type="primary" :loading="actionNo === scope.row.voucherNo" @click="handleAudit(scope.row)" v-hasPermi="['finance:voucher:audit']">检查并过账</el-button>
          <span v-else-if="scope.row.voucherStatus === 'posted'" class="done-text">已过账</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="entryTitle" v-model="entryOpen" width="760px" append-to-body>
      <el-alert class="entry-summary" :closable="false" :type="entryBalanced ? 'success' : 'error'" show-icon :title="`借方 ${money(entryDebit)}，贷方 ${money(entryCredit)}，${entryBalanced ? '借贷平衡' : '借贷不平衡，禁止过账'}`" />
      <el-table :data="entryList">
        <el-table-column label="序号" prop="entrySeq" align="center" width="80" />
        <el-table-column label="科目编码" prop="subjectCode" align="center" width="110" />
        <el-table-column label="科目名称" prop="subjectName" align="center" />
        <el-table-column label="方向" align="center" width="90"><template #default="{ row }">{{ row.direction === 'debit' ? '借' : '贷' }}</template></el-table-column>
        <el-table-column label="金额" align="right" width="120"><template #default="{ row }">{{ money(row.amount) }}</template></el-table-column>
        <el-table-column label="摘要" prop="summary" align="center" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceVoucher">
import { listVoucher, listVoucherEntry, auditVoucher } from '@/api/finance/voucher'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const entryList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const entryOpen = ref(false)
const entryTitle = ref('')
const actionNo = ref('')
const queryParams = ref({ pageNum: 1, pageSize: 10, voucherNo: undefined, periodCode: undefined, sourceNo: undefined })
const entryDebit = computed(() => entryList.value.filter(row => row.direction === 'debit').reduce((sum, row) => sum + Number(row.amount || 0), 0))
const entryCredit = computed(() => entryList.value.filter(row => row.direction === 'credit').reduce((sum, row) => sum + Number(row.amount || 0), 0))
const entryBalanced = computed(() => entryList.value.length > 0 && Math.abs(entryDebit.value - entryCredit.value) < 0.01)

function getList() {
  loading.value = true
  listVoucher(queryParams.value).then(res => { dataList.value = res.rows; total.value = res.total }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleEntries(row) {
  listVoucherEntry(row.voucherNo).then(res => {
    entryList.value = res.data || []
    entryTitle.value = `凭证分录：${row.voucherNo}`
    entryOpen.value = true
  })
}
async function handleAudit(row) {
  const response = await listVoucherEntry(row.voucherNo)
  const entries = response.data || []
  const debit = entries.filter(item => item.direction === 'debit').reduce((sum, item) => sum + Number(item.amount || 0), 0)
  const credit = entries.filter(item => item.direction === 'credit').reduce((sum, item) => sum + Number(item.amount || 0), 0)
  if (!entries.length) return proxy.$modal.msgError('凭证没有会计分录，不能过账')
  if (Math.abs(debit - credit) >= 0.01) return proxy.$modal.msgError(`借贷不平衡：借方 ${money(debit)}，贷方 ${money(credit)}`)
  proxy.$modal.confirm(`凭证 ${row.voucherNo} 共 ${entries.length} 条分录，借贷均为 ${money(debit)}。确认科目无误并过账到正式账簿？`).then(async () => {
    actionNo.value = row.voucherNo
    try { await auditVoucher(row.voucherNo); proxy.$modal.msgSuccess('凭证已过账，正式进入账簿'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function sourceLabel(value) { return ({ settlement: '平台结算', receivable: '应收账款', payable: '应付账款', expense: '费用单', cash_flow: '资金流水', supplier_invoice: '供应商发票', payment: '付款执行' })[value] || value || '-' }
function voucherStatusLabel(value) { return ({ draft: '待过账', posted: '已过账', voided: '已作废' })[value] || value || '-' }
function voucherStatusType(value) { return ({ draft: 'warning', posted: 'success', voided: 'info' })[value] || 'info' }
getList()
</script>

<style scoped>.entry-summary { margin-bottom:12px; }.done-text { color:#00a879; font-size:13px; }</style>
