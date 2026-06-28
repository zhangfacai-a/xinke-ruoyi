<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="采购单号" prop="purchaseNo">
        <el-input v-model="queryParams.purchaseNo" placeholder="请输入采购单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="供应商" prop="supplierName">
        <el-input v-model="queryParams.supplierName" placeholder="请输入供应商" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="purchaseStatus">
        <el-select v-model="queryParams.purchaseStatus" placeholder="全部" clearable style="width: 150px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:purchase:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:purchase:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:purchase:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="purchaseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="采购ID" align="center" prop="purchaseId" width="90" />
      <el-table-column label="采购单号" align="center" prop="purchaseNo" min-width="160" />
      <el-table-column label="供应商" align="center" prop="supplierName" min-width="160" />
      <el-table-column label="供应商ID" align="center" prop="supplierId" width="100" />
      <el-table-column label="入库仓ID" align="center" prop="warehouseId" width="100" />
      <el-table-column label="采购金额" align="right" prop="totalAmount" />
      <el-table-column label="状态" align="center" prop="purchaseStatus" width="110">
        <template #default="scope">
          <el-tag>{{ formatStatus(scope.row.purchaseStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预计到货" align="center" prop="expectedTime" width="170" />
      <el-table-column label="操作" width="160" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:purchase:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:purchase:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="purchaseRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="采购单号" prop="purchaseNo">
          <el-input v-model="form.purchaseNo" placeholder="请输入采购单号" />
        </el-form-item>
        <el-form-item label="供应商ID" prop="supplierId">
          <el-input-number v-model="form.supplierId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="供应商名称" prop="supplierName">
          <el-input v-model="form.supplierName" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-form-item label="入库仓ID">
          <el-input-number v-model="form.warehouseId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="采购金额">
          <el-input-number v-model="form.totalAmount" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="采购状态">
          <el-select v-model="form.purchaseStatus" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计到货">
          <el-date-picker v-model="form.expectedTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择预计到货时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="cancel">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpPurchase">
import { listPurchase, getPurchase, addPurchase, updatePurchase, delPurchase } from '@/api/erp/purchase'

const { proxy } = getCurrentInstance()
const purchaseList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '已审核', value: 'approved' },
  { label: '处理中', value: 'processing' },
  { label: '已完成', value: 'done' },
  { label: '已关闭', value: 'closed' }
]

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, purchaseNo: undefined, supplierName: undefined, purchaseStatus: undefined },
  rules: {
    purchaseNo: [{ required: true, message: '采购单号不能为空', trigger: 'blur' }],
    supplierId: [{ required: true, message: '供应商ID不能为空', trigger: 'blur' }],
    supplierName: [{ required: true, message: '供应商名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function formatStatus(value) {
  return statusOptions.find(item => item.value === value)?.label || value || '-'
}
function getList() {
  loading.value = true
  listPurchase(queryParams.value).then(response => {
    purchaseList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function reset() {
  form.value = { purchaseId: undefined, purchaseNo: undefined, supplierId: 1, supplierName: undefined, warehouseId: 1, totalAmount: 0, purchaseStatus: 'draft', expectedTime: undefined, remark: undefined }
  proxy.resetForm('purchaseRef')
}
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.purchaseId); single.value = selection.length !== 1; multiple.value = !selection.length }
function handleAdd() { reset(); open.value = true; title.value = '新增采购订单' }
function handleUpdate(row) {
  reset()
  const purchaseId = row.purchaseId || ids.value
  getPurchase(purchaseId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改采购订单'
  })
}
function submitForm() {
  proxy.$refs.purchaseRef.validate(valid => {
    if (!valid) return
    const request = form.value.purchaseId ? updatePurchase(form.value) : addPurchase(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.purchaseId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}
function handleDelete(row) {
  const purchaseIds = row.purchaseId || ids.value
  proxy.$modal.confirm('确认删除采购订单编号为 "' + purchaseIds + '" 的数据项？').then(() => delPurchase(purchaseIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
