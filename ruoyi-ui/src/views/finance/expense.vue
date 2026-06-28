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
      <el-table-column label="费用金额" prop="expenseAmount" align="right" />
      <el-table-column label="税额" prop="taxAmount" align="right" />
      <el-table-column label="价税合计" prop="totalAmount" align="right" />
      <el-table-column label="分摊维度" prop="allocationDimension" align="center" />
      <el-table-column label="状态" prop="expenseStatus" align="center" />
      <el-table-column label="发生日期" prop="occurredDate" align="center" />
      <el-table-column label="操作" align="center" width="190" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleApprove(scope.row)" v-hasPermi="['finance:expense:audit']">审核</el-button>
          <el-button link type="primary" @click="handleVoucher(scope.row)" v-hasPermi="['finance:voucher:add']">生成凭证</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新增费用单" v-model="open" width="680px" append-to-body>
      <el-form ref="expenseRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="费用单号">
              <el-input v-model="form.expenseNo" placeholder="不填则自动生成" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用类型" prop="feeTypeCode">
              <el-input v-model="form.feeTypeCode" placeholder="如 ad_fee / freight_fee" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="费用金额" prop="expenseAmount">
              <el-input-number v-model="form.expenseAmount" :precision="2" :step="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="税额">
              <el-input-number v-model="form.taxAmount" :precision="2" :step="10" controls-position="right" style="width: 100%" />
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
import { listExpense, addExpense, approveExpense, createExpenseVoucher } from '@/api/finance/expense'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
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
  return new Date().toISOString().slice(0, 10)
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
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { reset(); open.value = true }
function submitForm() {
  proxy.$refs.expenseRef.validate(valid => {
    if (!valid) return
    addExpense(form.value).then(() => {
      proxy.$modal.msgSuccess('新增费用单成功')
      open.value = false
      getList()
    })
  })
}
function handleApprove(row) {
  proxy.$modal.confirm(`确认审核费用单 ${row.expenseNo} 并生成应付单？`).then(() => approveExpense(row.expenseNo)).then(() => {
    proxy.$modal.msgSuccess('审核成功')
    getList()
  }).catch(() => {})
}
function handleVoucher(row) {
  proxy.$modal.confirm(`确认根据费用单 ${row.expenseNo} 生成会计凭证？`).then(() => createExpenseVoucher(row.expenseNo)).then(() => {
    proxy.$modal.msgSuccess('凭证生成成功')
    getList()
  }).catch(() => {})
}
getList()
</script>
