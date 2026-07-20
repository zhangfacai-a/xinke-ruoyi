<template>
  <div class="app-container">
    <el-tabs v-model="activeName" @tab-change="getList">
      <el-tab-pane label="对账任务" name="task" />
      <el-tab-pane label="对账差异" name="diff" />
    </el-tabs>
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="期间" prop="periodCode" v-if="activeName === 'task'">
        <el-input v-model="queryParams.periodCode" placeholder="如 2026-06" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="单号" prop="keyword">
        <el-input v-model="queryParams.keyword" placeholder="任务号/差异号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item v-if="activeName === 'diff'" label="状态" prop="diffStatus">
        <el-select v-model="queryParams.diffStatus" placeholder="全部" clearable style="width: 120px">
          <el-option label="待处理" value="open" />
          <el-option label="已处理" value="resolved" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Refresh" @click="handleRun" v-hasPermi="['finance:reconcile:run']">运行对账</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-if="activeName === 'task'" v-loading="loading" :data="dataList">
      <el-table-column label="任务号" prop="taskNo" align="center" min-width="170" />
      <el-table-column label="类型" align="center"><template #default="{ row }">{{ reconcileTypeLabel(row.reconcileType) }}</template></el-table-column>
      <el-table-column label="期间" prop="periodCode" align="center" />
      <el-table-column label="店铺" prop="shopName" align="center" />
      <el-table-column label="状态" align="center"><template #default="{ row }"><el-tag round :type="row.taskStatus === 'completed' ? 'success' : 'warning'">{{ row.taskStatus === 'completed' ? '已完成' : '运行中' }}</el-tag></template></el-table-column>
      <el-table-column label="总数" prop="totalCount" align="right" />
      <el-table-column label="差异数" prop="diffCount" align="right" />
      <el-table-column label="开始时间" prop="startTime" align="center" width="170" />
      <el-table-column label="完成时间" prop="finishTime" align="center" width="170" />
    </el-table>
    <el-table v-else v-loading="loading" :data="dataList">
      <el-table-column label="差异号" prop="diffNo" align="center" min-width="170" />
      <el-table-column label="差异类型" align="center"><template #default="{ row }">{{ diffTypeLabel(row.diffType) }}</template></el-table-column>
      <el-table-column label="来源" align="center"><template #default="{ row }">{{ row.sourceType === 'settlement' ? '平台结算' : row.sourceType }}</template></el-table-column>
      <el-table-column label="来源单号" prop="sourceNo" align="center" min-width="150" />
      <el-table-column label="应有金额" align="right"><template #default="{ row }">{{ money(row.expectedAmount) }}</template></el-table-column>
      <el-table-column label="实际金额" align="right"><template #default="{ row }">{{ money(row.actualAmount) }}</template></el-table-column>
      <el-table-column label="差异金额" align="right"><template #default="{ row }"><strong class="diff-amount">{{ money(row.diffAmount) }}</strong></template></el-table-column>
      <el-table-column label="状态" align="center"><template #default="{ row }"><el-tag round :type="row.diffStatus === 'resolved' ? 'success' : 'danger'">{{ row.diffStatus === 'resolved' ? '已处理' : '待处理' }}</el-tag></template></el-table-column>
      <el-table-column label="处理结果" prop="handleResult" align="center" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="110" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.diffStatus === 'open'" link type="primary" @click="handleResolve(row)" v-hasPermi="['finance:reconcile:handle']">填写处理结果</el-button>
          <span v-else class="resolved-text">已完成</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="运行财务对账" v-model="open" width="520px" append-to-body>
      <el-form ref="runRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="期间" prop="periodCode">
          <el-input v-model="form.periodCode" placeholder="如 2026-06" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.reconcileType" style="width: 100%">
            <el-option label="平台结算 vs 收款流水" value="settlement_cash" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺ID">
          <el-input v-model="form.shopId" placeholder="可选，空则全部店铺" />
        </el-form-item>
        <el-form-item label="店铺名称">
          <el-input v-model="form.shopName" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitRun">确定</el-button>
          <el-button @click="open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog title="处理对账差异" v-model="resolveOpen" width="540px" append-to-body>
      <el-form ref="resolveRef" :model="resolveForm" :rules="resolveRules" label-width="100px">
        <el-form-item label="差异号"><el-input v-model="resolveForm.diffNo" disabled /></el-form-item>
        <el-form-item label="来源单号"><el-input v-model="resolveForm.sourceNo" disabled /></el-form-item>
        <el-form-item label="差异金额"><el-input :model-value="money(resolveForm.diffAmount)" disabled /></el-form-item>
        <el-form-item label="处理说明" prop="handleResult"><el-input v-model="resolveForm.handleResult" type="textarea" :rows="4" placeholder="说明原因、调整方式或核对结论，至少5个字" /></el-form-item>
        <el-alert type="warning" :closable="false" show-icon title="标记处理只关闭差异事项，不会自动修改流水或结算金额。需要调整数据时请先完成调整再填写结论。" />
      </el-form>
      <template #footer><el-button @click="resolveOpen = false">取消</el-button><el-button type="primary" @click="submitResolve">确认已处理</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceReconcile">
import { listReconcileTask, listReconcileDiff, runReconcile, resolveReconcileDiff } from '@/api/finance/reconcile'

const { proxy } = getCurrentInstance()
const activeName = ref('task')
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const resolveOpen = ref(false)
const resolveForm = ref({})
const resolveRules = { handleResult: [{ required: true, min: 5, message: '处理说明至少填写5个字', trigger: 'blur' }] }
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, periodCode: undefined, keyword: undefined, diffStatus: undefined },
  rules: {
    periodCode: [{ required: true, message: '期间不能为空', trigger: 'blur' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

function currentPeriod() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
function buildParams() {
  const params = { pageNum: queryParams.value.pageNum, pageSize: queryParams.value.pageSize, periodCode: queryParams.value.periodCode }
  if (activeName.value === 'task') params.taskNo = queryParams.value.keyword
  else { params.diffNo = queryParams.value.keyword; params.diffStatus = queryParams.value.diffStatus }
  return params
}
function getList() {
  loading.value = true
  const request = activeName.value === 'task' ? listReconcileTask : listReconcileDiff
  request(buildParams()).then(res => { dataList.value = res.rows; total.value = res.total }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleRun() {
  form.value = { periodCode: queryParams.value.periodCode || currentPeriod(), reconcileType: 'settlement_cash', shopId: undefined, shopName: undefined }
  open.value = true
}
function submitRun() {
  proxy.$refs.runRef.validate(valid => {
    if (!valid) return
    runReconcile(form.value).then(res => {
      proxy.$modal.msgSuccess(`对账完成，总数 ${res.data.totalCount}，差异 ${res.data.diffCount}`)
      open.value = false
      activeName.value = 'task'
      getList()
    })
  })
}
function handleResolve(row) {
  resolveForm.value = { diffNo: row.diffNo, sourceNo: row.sourceNo, diffAmount: row.diffAmount, handleResult: '' }
  resolveOpen.value = true
}
function submitResolve() {
  proxy.$refs.resolveRef.validate(async valid => {
    if (!valid) return
    await resolveReconcileDiff(resolveForm.value.diffNo, { handleResult: resolveForm.value.handleResult })
    proxy.$modal.msgSuccess('差异已标记处理完成')
    resolveOpen.value = false
    await getList()
  })
}
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function reconcileTypeLabel(value) { return value === 'settlement_cash' ? '平台结算与收款流水' : value || '-' }
function diffTypeLabel(value) { return ({ missing_cash: '缺少收款流水', amount_diff: '到账金额差异' })[value] || value || '-' }
getList()
</script>

<style scoped>
.diff-amount { color:#e34d65; }.resolved-text { color:#00a879; font-size:13px; }
</style>
