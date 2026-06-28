<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="期间" prop="periodCode">
        <el-input v-model="queryParams.periodCode" placeholder="如 2026-06" clearable style="width: 160px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="结账状态" prop="closeStatus">
        <el-select v-model="queryParams.closeStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="打开" value="open" />
          <el-option label="已关闭" value="closed" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="CircleCheck" @click="handleCheck()" v-hasPermi="['finance:close:check']">月结检查</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="期间" prop="periodCode" align="center" />
      <el-table-column label="范围" prop="closeScope" align="center" />
      <el-table-column label="检查状态" prop="checkStatus" align="center" />
      <el-table-column label="结账状态" prop="closeStatus" align="center" />
      <el-table-column label="订单完整" prop="orderComplete" align="center" />
      <el-table-column label="结算完整" prop="settlementComplete" align="center" />
      <el-table-column label="银行完整" prop="bankComplete" align="center" />
      <el-table-column label="凭证完整" prop="voucherComplete" align="center" />
      <el-table-column label="结账人" prop="closeBy" align="center" />
      <el-table-column label="结账时间" prop="closeTime" align="center" width="170" />
      <el-table-column label="操作" align="center" width="210" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleCheck(scope.row)" v-hasPermi="['finance:close:check']">检查</el-button>
          <el-button link type="primary" @click="handleClose(scope.row)" v-hasPermi="['finance:close:close']">关账</el-button>
          <el-button link type="primary" @click="handleReopen(scope.row)" v-hasPermi="['finance:close:reopen']">反关账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="FinanceClose">
import { listPeriodClose, checkPeriodClose, closePeriod, reopenPeriod } from '@/api/finance/close'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, periodCode: undefined, closeStatus: undefined })

function currentPeriod() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
function periodPayload(row) {
  return { periodCode: row?.periodCode || queryParams.value.periodCode || currentPeriod(), closeScope: row?.closeScope || 'company' }
}
function getList() {
  loading.value = true
  listPeriodClose(queryParams.value).then(res => { dataList.value = res.rows; total.value = res.total }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleCheck(row) {
  const payload = periodPayload(row)
  checkPeriodClose(payload).then(res => {
    proxy.$modal.msgSuccess(`检查完成：${res.data.checkStatus}`)
    getList()
  })
}
function handleClose(row) {
  const payload = periodPayload(row)
  proxy.$modal.confirm(`确认关闭期间 ${payload.periodCode}？`).then(() => closePeriod(payload)).then(() => {
    proxy.$modal.msgSuccess('关账成功')
    getList()
  }).catch(() => {})
}
function handleReopen(row) {
  const payload = periodPayload(row)
  proxy.$modal.confirm(`确认反关账期间 ${payload.periodCode}？`).then(() => reopenPeriod(payload)).then(() => {
    proxy.$modal.msgSuccess('反关账成功')
    getList()
  }).catch(() => {})
}
getList()
</script>
