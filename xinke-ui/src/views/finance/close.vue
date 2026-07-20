<template>
  <div class="app-container">
    <el-alert class="close-guide" type="warning" :closable="false" show-icon title="月结会冻结该期间的结算、费用、流水和凭证修改。必须先执行月结检查，所有检查项通过后才能关账。" />
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
      <el-table-column label="范围" align="center"><template #default="{ row }">{{ row.closeScope === 'company' ? '全公司' : row.closeScope }}</template></el-table-column>
      <el-table-column label="检查状态" align="center"><template #default="{ row }"><el-tag round :type="checkStatusType(row.checkStatus)">{{ checkStatusLabel(row.checkStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="结账状态" align="center"><template #default="{ row }"><el-tag round :type="row.closeStatus === 'closed' ? 'success' : 'warning'">{{ row.closeStatus === 'closed' ? '已关账' : '未关账' }}</el-tag></template></el-table-column>
      <el-table-column label="订单" align="center"><template #default="{ row }">{{ completeLabel(row.orderComplete) }}</template></el-table-column>
      <el-table-column label="结算" align="center"><template #default="{ row }">{{ completeLabel(row.settlementComplete) }}</template></el-table-column>
      <el-table-column label="资金" align="center"><template #default="{ row }">{{ completeLabel(row.bankComplete) }}</template></el-table-column>
      <el-table-column label="凭证" align="center"><template #default="{ row }">{{ completeLabel(row.voucherComplete) }}</template></el-table-column>
      <el-table-column label="结账人" prop="closeBy" align="center" />
      <el-table-column label="结账时间" prop="closeTime" align="center" width="170" />
      <el-table-column label="操作" align="center" width="210" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.closeStatus !== 'closed'" link type="primary" @click="handleCheck(scope.row)" v-hasPermi="['finance:close:check']">重新检查</el-button>
          <el-button v-if="scope.row.closeStatus !== 'closed' && scope.row.checkStatus === 'passed'" link type="danger" @click="handleClose(scope.row)" v-hasPermi="['finance:close:close']">确认关账</el-button>
          <el-button v-if="scope.row.closeStatus === 'closed'" link type="warning" @click="handleReopen(scope.row)" v-hasPermi="['finance:close:reopen']">反关账</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="checkOpen" title="月结检查结果" width="620px" append-to-body>
      <el-result :icon="checkResult.checkStatus === 'passed' ? 'success' : 'warning'" :title="checkResult.checkStatus === 'passed' ? '检查通过，可以执行关账' : '检查未通过，请先处理以下事项'" :sub-title="checkResult.remark || ''" />
      <div class="check-grid">
        <div><span>草稿结算单</span><strong>{{ checkResult.draftSettlementCount || 0 }}</strong></div>
        <div><span>未完成对账</span><strong>{{ checkResult.unmatchedSettlementCount || 0 }}</strong></div>
        <div><span>未匹配流水</span><strong>{{ checkResult.unmatchedCashCount || 0 }}</strong></div>
        <div><span>未入账流水</span><strong>{{ checkResult.unpostedCashCount || 0 }}</strong></div>
        <div><span>未过账凭证</span><strong>{{ checkResult.draftVoucherCount || 0 }}</strong></div>
        <div><span>未结应收/应付</span><strong>{{ Number(checkResult.openReceivableCount || 0) + Number(checkResult.openPayableCount || 0) }}</strong></div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceClose">
import { ElMessageBox } from 'element-plus'
import { listPeriodClose, checkPeriodClose, closePeriod, reopenPeriod } from '@/api/finance/close'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const checkOpen = ref(false)
const checkResult = ref({})
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
    checkResult.value = res.data || {}
    checkOpen.value = true
    getList()
  })
}
function handleClose(row) {
  const payload = periodPayload(row)
  proxy.$modal.confirm(`确认关闭 ${payload.periodCode} 会计期间？关账后该期间业务单据、流水和凭证不能继续修改。`).then(() => closePeriod(payload)).then(() => {
    proxy.$modal.msgSuccess('关账成功')
    getList()
  }).catch(() => {})
}
async function handleReopen(row) {
  const payload = periodPayload(row)
  try {
    const result = await ElMessageBox.prompt(`反关账会重新开放 ${payload.periodCode} 的业务修改，请填写审批依据。`, '反关账', {
      confirmButtonText: '确认反关账', cancelButtonText: '取消', inputPlaceholder: '至少填写5个字',
      inputValidator: value => String(value || '').trim().length >= 5 || '反关账原因至少5个字'
    })
    payload.reason = String(result.value).trim()
    await reopenPeriod(payload)
    proxy.$modal.msgSuccess('反关账成功')
    await getList()
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}
function completeLabel(value) { return String(value) === '1' ? '已完成' : '待处理' }
function checkStatusLabel(value) { return ({ unchecked: '未检查', passed: '检查通过', failed: '存在阻断项' })[value] || value || '-' }
function checkStatusType(value) { return ({ unchecked: 'info', passed: 'success', failed: 'danger' })[value] || 'info' }
getList()
</script>

<style scoped>
.close-guide { margin-bottom:16px; }.check-grid { display:grid; grid-template-columns:repeat(3, 1fr); gap:10px; }.check-grid div { padding:13px; border:1px solid #ececf4; border-radius:10px; background:#fafbfe; }.check-grid span,.check-grid strong { display:block; }.check-grid span { color:#7c849a; font-size:12px; }.check-grid strong { margin-top:6px; font-size:20px; }
</style>
