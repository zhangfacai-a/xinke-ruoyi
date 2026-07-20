<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="费用单号" prop="expenseNo">
        <el-input v-model="queryParams.expenseNo" placeholder="请输入费用单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="期间" prop="periodCode">
        <el-input v-model="queryParams.periodCode" placeholder="如 2026-06" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="店铺" prop="shopName">
        <el-input v-model="queryParams.shopName" placeholder="请输入店铺" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['finance:expense:add']">新增费用</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="费用单号" prop="expenseNo" align="center" min-width="170" />
      <el-table-column label="费用类型" prop="feeTypeName" align="center" />
      <el-table-column label="店铺" prop="shopName" align="center" />
      <el-table-column label="SKU" prop="skuCode" align="center" />
      <el-table-column label="订单号" prop="orderNo" align="center" min-width="150" />
      <el-table-column label="供应商" prop="supplierName" align="center" />
      <el-table-column label="期间" prop="periodCode" align="center" />
      <el-table-column label="费用金额" align="right"><template #default="{ row }">{{ money(row.expenseAmount) }}</template></el-table-column>
      <el-table-column label="税额" align="right"><template #default="{ row }">{{ money(row.taxAmount) }}</template></el-table-column>
      <el-table-column label="价税合计" align="right"><template #default="{ row }"><strong>{{ money(row.totalAmount) }}</strong></template></el-table-column>
      <el-table-column label="分摊维度" prop="allocationDimension" align="center" />
      <el-table-column label="状态" align="center"><template #default="{ row }"><el-tag round :type="expenseStatusType(row.expenseStatus)">{{ expenseStatusLabel(row.expenseStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="后续单据" min-width="170">
        <template #default="{ row }"><div class="linked-bills"><span v-if="row.payableNo">应付 {{ row.payableNo }}</span><span v-if="row.voucherNo">凭证 {{ row.voucherNo }}</span><em v-if="!row.payableNo && !row.voucherNo">审核后自动生成</em></div></template>
      </el-table-column>
      <el-table-column label="发生日期" prop="occurredDate" align="center" />
      <el-table-column label="操作" align="center" width="285" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.expenseStatus === 'draft'" link type="primary" @click="handleEdit(scope.row)" v-hasPermi="['finance:expense:edit']">修改</el-button>
          <el-button v-if="scope.row.expenseStatus === 'draft'" link type="primary" :loading="actionNo === scope.row.expenseNo" @click="handleApprove(scope.row)" v-hasPermi="['finance:expense:audit']">审核并生成应付</el-button>
          <el-button v-if="scope.row.expenseStatus === 'draft'" link type="danger" :loading="actionNo === scope.row.expenseNo" @click="handleVoid(scope.row)" v-hasPermi="['finance:expense:edit']">作废</el-button>
          <el-button v-if="scope.row.expenseStatus === 'approved' && !scope.row.voucherNo" link type="primary" :loading="actionNo === scope.row.expenseNo" @click="handleVoucher(scope.row)" v-hasPermi="['finance:voucher:add']">生成凭证</el-button>
          <span v-if="scope.row.expenseStatus === 'approved' && scope.row.voucherNo" class="done-text">处理完成</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="editMode ? '修改费用单' : '新增费用单'" v-model="open" width="680px" append-to-body>
      <el-form ref="expenseRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="费用单号">
              <el-input v-model="form.expenseNo" placeholder="不填则自动生成" :disabled="editMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用类型" prop="feeTypeCode">
              <el-select v-model="form.feeTypeCode" placeholder="请选择费用类型" filterable style="width:100%">
                <el-option v-for="item in feeTypeOptions" :key="item.feeCode" :label="item.feeName" :value="item.feeCode"><span>{{ item.feeName }}</span><small class="option-code">{{ item.feeCode }}</small></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用金额" prop="expenseAmount">
              <el-input-number v-model="form.expenseAmount" :precision="2" :step="100" :min="0.01" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税额">
              <el-input-number v-model="form.taxAmount" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发生日期" prop="occurredDate">
              <el-date-picker v-model="form.occurredDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期间">
              <el-input v-model="form.periodCode" placeholder="不填则取发生日期月份" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="店铺ID">
              <el-input v-model="form.shopId" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="店铺名称">
              <el-input v-model="form.shopName" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商ID">
              <el-input v-model="form.supplierId" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商名称">
              <el-input v-model="form.supplierName" placeholder="请输入供应商" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="订单号">
              <el-input v-model="form.orderNo" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="SKU编码">
              <el-input v-model="form.skuCode" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="分摊维度">
              <el-select v-model="form.allocationDimension" style="width: 100%">
                <el-option label="店铺月度" value="shop_month" />
                <el-option label="店铺日期" value="shop_day" />
                <el-option label="订单" value="order" />
                <el-option label="SKU" value="sku" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
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

<script setup name="FinanceExpense">
import { listExpense, addExpense, updateExpense, voidExpense, approveExpense, createExpenseVoucher } from '@/api/finance/expense'
import { listFeeType } from '@/api/finance/config'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const editMode = ref(false)
const actionNo = ref('')
const feeTypeOptions = ref([])
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, expenseNo: undefined, periodCode: undefined, shopName: undefined },
  rules: {
    feeTypeCode: [{ required: true, message: '费用类型不能为空', trigger: 'blur' }],
    expenseAmount: [{ required: true, message: '费用金额不能为空', trigger: 'blur' }],
    occurredDate: [{ required: true, message: '发生日期不能为空', trigger: 'change' }]
  }
})
const { form, queryParams, rules } = toRefs(data)

function currentDate() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}
function reset() {
  form.value = {
    expenseNo: undefined,
    feeTypeCode: 'ad_fee',
    expenseAmount: 0,
    taxAmount: 0,
    occurredDate: currentDate(),
    periodCode: undefined,
    shopId: undefined,
    shopName: undefined,
    supplierId: undefined,
    supplierName: undefined,
    orderNo: undefined,
    skuCode: undefined,
    allocationDimension: 'shop_month',
    remark: undefined
  }
  proxy.resetForm('expenseRef')
}
function getList() {
  loading.value = true
  listExpense(queryParams.value).then(res => { dataList.value = res.rows; total.value = res.total }).finally(() => { loading.value = false })
}
async function loadFeeTypes() { const response = await listFeeType({ pageNum: 1, pageSize: 100, status: '0' }); feeTypeOptions.value = response.rows || [] }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { reset(); editMode.value = false; open.value = true }
function handleEdit(row) {
  reset()
  editMode.value = true
  form.value = { ...row }
  open.value = true
}
function submitForm() {
  proxy.$refs.expenseRef.validate(valid => {
    if (!valid) return
    const request = editMode.value ? updateExpense(form.value.expenseNo, form.value) : addExpense(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(editMode.value ? '费用单修改成功' : '新增费用单成功')
      open.value = false
      getList()
    })
  })
}
function handleVoid(row) {
  proxy.$modal.confirm(`确认作废草稿费用单 ${row.expenseNo}？作废后不会生成应付或凭证。`).then(async () => {
    actionNo.value = row.expenseNo
    try { await voidExpense(row.expenseNo); proxy.$modal.msgSuccess('费用单已作废'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function handleApprove(row) {
  proxy.$modal.confirm(`确认费用类型和金额无误？审核后将自动生成应付单，不能再修改本单。`).then(async () => {
    actionNo.value = row.expenseNo
    try { await approveExpense(row.expenseNo); proxy.$modal.msgSuccess('审核完成，应付单已生成'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function handleVoucher(row) {
  proxy.$modal.confirm(`根据费用单 ${row.expenseNo} 生成借贷平衡的草稿凭证？`).then(async () => {
    actionNo.value = row.expenseNo
    try { await createExpenseVoucher(row.expenseNo); proxy.$modal.msgSuccess('草稿凭证已生成，请到凭证管理检查并过账'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function expenseStatusLabel(value) { return ({ draft: '待审核', approved: '已审核', rejected: '已驳回', voided: '已作废' })[value] || value || '-' }
function expenseStatusType(value) { return ({ draft: 'warning', approved: 'success', rejected: 'danger', voided: 'info' })[value] || 'info' }
Promise.all([getList(), loadFeeTypes()])
</script>

<style scoped>
.linked-bills span, .linked-bills em { display:block; line-height:1.55; font-size:12px; }.linked-bills span { color:#6254df; }.linked-bills em { color:#9aa1b4; font-style:normal; }.done-text { color:#00a879; font-size:13px; }.option-code { float:right; margin-left:20px; color:#9aa1b4; }
</style>
