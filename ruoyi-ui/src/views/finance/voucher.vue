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
      <el-table-column label="来源" prop="sourceType" align="center" />
      <el-table-column label="来源单号" prop="sourceNo" align="center" min-width="150" />
      <el-table-column label="摘要" prop="summary" align="center" min-width="180" show-overflow-tooltip />
      <el-table-column label="借方" prop="debitAmount" align="right" />
      <el-table-column label="贷方" prop="creditAmount" align="right" />
      <el-table-column label="状态" prop="voucherStatus" align="center" />
      <el-table-column label="操作" align="center" width="170" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleEntries(scope.row)" v-hasPermi="['finance:voucher:query']">分录</el-button>
          <el-button link type="primary" @click="handleAudit(scope.row)" v-hasPermi="['finance:voucher:audit']">过账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="entryTitle" v-model="entryOpen" width="760px" append-to-body>
      <el-table :data="entryList">
        <el-table-column label="序号" prop="entrySeq" align="center" width="80" />
        <el-table-column label="科目编码" prop="subjectCode" align="center" width="110" />
        <el-table-column label="科目名称" prop="subjectName" align="center" />
        <el-table-column label="方向" prop="direction" align="center" width="90" />
        <el-table-column label="金额" prop="amount" align="right" width="120" />
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
const queryParams = ref({ pageNum: 1, pageSize: 10, voucherNo: undefined, periodCode: undefined, sourceNo: undefined })

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
function handleAudit(row) {
  proxy.$modal.confirm(`确认过账凭证 ${row.voucherNo}？`).then(() => auditVoucher(row.voucherNo)).then(() => {
    proxy.$modal.msgSuccess('过账成功')
    getList()
  }).catch(() => {})
}
getList()
</script>
