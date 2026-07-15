<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="流水号" prop="flowNo">
        <el-input v-model="queryParams.flowNo" placeholder="请输入流水号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="flowType">
        <el-select v-model="queryParams.flowType" placeholder="全部" clearable style="width: 120px">
          <el-option label="收入" value="in" />
          <el-option label="支出" value="out" />
        </el-select>
      </el-form-item>
      <el-form-item label="往来方" prop="counterpartyName">
        <el-input v-model="queryParams.counterpartyName" placeholder="请输入往来方" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['finance:cashflow:add']">新增流水</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="流水号" prop="flowNo" align="center" min-width="170" />
      <el-table-column label="类型" prop="flowType" align="center" />
      <el-table-column label="往来方" prop="counterpartyName" align="center" min-width="150" />
      <el-table-column label="来源" prop="sourceType" align="center" />
      <el-table-column label="来源单号" prop="sourceNo" align="center" min-width="150" />
      <el-table-column label="金额" prop="amount" align="right" />
      <el-table-column label="手续费" prop="feeAmount" align="right" />
      <el-table-column label="流水时间" prop="flowTime" align="center" width="170" />
      <el-table-column label="业务日期" prop="businessDate" align="center" />
      <el-table-column label="匹配状态" prop="matchStatus" align="center" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新增收付款流水" v-model="open" width="620px" append-to-body>
      <el-form ref="cashFlowRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="流水号">
          <el-input v-model="form.flowNo" placeholder="不填则自动生成" />
        </el-form-item>
        <el-form-item label="类型" prop="flowType">
          <el-radio-group v-model="form.flowType">
            <el-radio value="in">收入</el-radio>
            <el-radio value="out">支出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :precision="2" :step="100" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="手续费">
          <el-input-number v-model="form.feeAmount" :precision="2" :step="10" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="往来方">
          <el-input v-model="form.counterpartyName" placeholder="请输入往来方" />
        </el-form-item>
        <el-form-item label="流水时间">
          <el-date-picker v-model="form.flowTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="选择时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="业务日期">
          <el-date-picker v-model="form.businessDate" type="date" value-format="YYYY-MM-DD" placeholder="不填则取流水日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="来源类型">
          <el-input v-model="form.sourceType" placeholder="如 settlement / receivable / payable，可选" />
        </el-form-item>
        <el-form-item label="来源单号">
          <el-input v-model="form.sourceNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceCashFlow">
import { listCashFlow, addCashFlow } from '@/api/finance/cashflow'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, flowNo: undefined, flowType: undefined, counterpartyName: undefined },
  rules: {
    flowType: [{ required: true, message: '流水类型不能为空', trigger: 'change' }],
    amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

function reset() {
  form.value = {
    flowNo: undefined,
    flowType: 'in',
    amount: 0,
    feeAmount: 0,
    counterpartyName: undefined,
    flowTime: undefined,
    businessDate: undefined,
    sourceType: undefined,
    sourceNo: undefined,
    remark: undefined
  }
  proxy.resetForm('cashFlowRef')
}
function getList() {
  loading.value = true
  listCashFlow(queryParams.value).then(res => { dataList.value = res.rows; total.value = res.total }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { reset(); open.value = true }
function submitForm() {
  proxy.$refs.cashFlowRef.validate(valid => {
    if (!valid) return
    addCashFlow(form.value).then(() => {
      proxy.$modal.msgSuccess('新增流水成功')
      open.value = false
      getList()
    })
  })
}
getList()
</script>
