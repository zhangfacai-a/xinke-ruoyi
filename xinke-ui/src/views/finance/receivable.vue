<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="应收单号" prop="receivableNo">
        <el-input v-model="queryParams.receivableNo" placeholder="请输入应收单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="期间" prop="periodCode">
        <el-input v-model="queryParams.periodCode" placeholder="如 2026-06" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="往来方" prop="counterpartyName">
        <el-input v-model="queryParams.counterpartyName" placeholder="请输入往来方" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8"><right-toolbar v-model:showSearch="showSearch" @queryTable="getList" /></el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="应收单号" prop="receivableNo" align="center" min-width="170" />
      <el-table-column label="来源" prop="sourceType" align="center" />
      <el-table-column label="来源单号" prop="sourceNo" align="center" min-width="150" />
      <el-table-column label="往来方" prop="counterpartyName" align="center" min-width="150" />
      <el-table-column label="店铺" prop="shopName" align="center" />
      <el-table-column label="期间" prop="periodCode" align="center" />
      <el-table-column label="应收金额" align="right"><template #default="{ row }">{{ money(row.billAmount) }}</template></el-table-column>
      <el-table-column label="已核销" align="right"><template #default="{ row }">{{ money(row.writeoffAmount) }}</template></el-table-column>
      <el-table-column label="未核销" align="right"><template #default="{ row }"><strong>{{ money(row.remainAmount) }}</strong></template></el-table-column>
      <el-table-column label="到期日" prop="dueDate" align="center" />
      <el-table-column label="状态" align="center"><template #default="{ row }"><el-tag round :type="billStatusType(row.billStatus)">{{ billStatusLabel(row.billStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="操作" align="center" width="120" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.billStatus !== 'settled' && Number(scope.row.remainAmount || 0) > 0" link type="primary" @click="handleWriteoff(scope.row)" v-hasPermi="['finance:receivable:writeoff']">选择收款核销</el-button>
          <span v-else class="done-text">已结清</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="应收核销" v-model="open" width="520px" append-to-body>
      <el-form ref="writeoffRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="应收单号">
          <el-input v-model="form.receivableNo" disabled />
        </el-form-item>
        <el-form-item label="流水号" prop="cashFlowNo">
          <el-select v-model="form.cashFlowNo" placeholder="选择已入账且未完全核销的收款" filterable style="width:100%" @change="handleCashFlowChange">
            <el-option v-for="item in cashFlowOptions" :key="item.flowNo" :label="`${item.flowNo} · ${item.counterpartyName || '未填写往来方'} · 可用 ${money(item.availableAmount)}`" :value="item.flowNo" />
          </el-select>
        </el-form-item>
        <el-form-item label="核销金额" prop="writeoffAmount">
          <el-input-number v-model="form.writeoffAmount" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
        <el-alert :closable="false" type="info" show-icon title="核销会同时减少应收余额和流水可用余额；一条收款流水可以分次核销多张应收单。" />
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitWriteoff">确定</el-button>
          <el-button @click="open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceReceivable">
import { listReceivable, writeoffReceivable } from '@/api/finance/receivable'
import { listCashFlow } from '@/api/finance/cashflow'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const cashFlowOptions = ref([])
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, receivableNo: undefined, periodCode: undefined, counterpartyName: undefined },
  rules: {
    cashFlowNo: [{ required: true, message: '流水号不能为空', trigger: 'blur' }],
    writeoffAmount: [{ required: true, message: '核销金额不能为空', trigger: 'blur' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

function getList() {
  loading.value = true
  listReceivable(queryParams.value).then(res => { dataList.value = res.rows; total.value = res.total }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
async function handleWriteoff(row) {
  form.value = { receivableNo: row.receivableNo, cashFlowNo: undefined, writeoffAmount: Number(row.remainAmount || 0), remark: undefined }
  open.value = true
  const response = await listCashFlow({ pageNum: 1, pageSize: 100, flowType: 'in', entryStatus: 'posted' })
  cashFlowOptions.value = (response.rows || []).filter(item => Number(item.availableAmount || 0) > 0)
}
function handleCashFlowChange(flowNo) { const row = cashFlowOptions.value.find(item => item.flowNo === flowNo); if (row) form.value.writeoffAmount = Math.min(Number(form.value.writeoffAmount || 0), Number(row.availableAmount || 0)) }
function submitWriteoff() {
  proxy.$refs.writeoffRef.validate(valid => {
    if (!valid) return
    writeoffReceivable(form.value.receivableNo, form.value).then(() => {
      proxy.$modal.msgSuccess('核销成功')
      open.value = false
      getList()
    })
  })
}
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function billStatusLabel(value) { return ({ open: '待收款', partial: '部分收款', settled: '已结清' })[value] || value || '-' }
function billStatusType(value) { return ({ open: 'warning', partial: 'primary', settled: 'success' })[value] || 'info' }
getList()
</script>

<style scoped>.done-text { color:#00a879; font-size:13px; }</style>
