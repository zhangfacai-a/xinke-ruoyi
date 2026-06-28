<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="SKU编码" prop="skuCode">
        <el-input v-model="queryParams.skuCode" placeholder="请输入SKU编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="SKU名称" prop="skuName">
        <el-input v-model="queryParams.skuName" placeholder="请输入SKU名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:sku:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:sku:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:sku:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="skuList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="SKU ID" align="center" prop="skuId" width="90" />
      <el-table-column label="商品" align="center" prop="productName" min-width="140" />
      <el-table-column label="SKU编码" align="center" prop="skuCode" />
      <el-table-column label="SKU名称" align="center" prop="skuName" min-width="150" />
      <el-table-column label="成本价" align="right" prop="costPrice" />
      <el-table-column label="销售价" align="right" prop="salePrice" />
      <el-table-column label="库存" align="right" prop="stockQty" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:sku:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:sku:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="560px" append-to-body>
      <el-form ref="skuRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="商品ID" prop="productId">
          <el-input-number v-model="form.productId" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="SKU编码" prop="skuCode">
          <el-input v-model="form.skuCode" placeholder="请输入SKU编码" />
        </el-form-item>
        <el-form-item label="SKU名称" prop="skuName">
          <el-input v-model="form.skuName" placeholder="请输入SKU名称" />
        </el-form-item>
        <el-form-item label="成本价">
          <el-input-number v-model="form.costPrice" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="销售价">
          <el-input-number v-model="form.salePrice" :min="0" :precision="2" controls-position="right" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stockQty" :min="0" controls-position="right" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="0">启用</el-radio>
            <el-radio value="1">停用</el-radio>
          </el-radio-group>
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

<script setup name="ErpSku">
import { listSku, getSku, addSku, updateSku, delSku } from '@/api/erp/sku'

const { proxy } = getCurrentInstance()
const skuList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, skuCode: undefined, skuName: undefined },
  rules: {
    productId: [{ required: true, message: '商品ID不能为空', trigger: 'blur' }],
    skuCode: [{ required: true, message: 'SKU编码不能为空', trigger: 'blur' }],
    skuName: [{ required: true, message: 'SKU名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listSku(queryParams.value).then(response => {
    skuList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}

function reset() {
  form.value = { skuId: undefined, productId: 1, skuCode: undefined, skuName: undefined, costPrice: 0, salePrice: 0, stockQty: 0, status: '0', remark: undefined }
  proxy.resetForm('skuRef')
}
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.skuId); single.value = selection.length !== 1; multiple.value = !selection.length }
function handleAdd() { reset(); open.value = true; title.value = '新增SKU' }
function handleUpdate(row) {
  reset()
  const skuId = row.skuId || ids.value
  getSku(skuId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改SKU'
  })
}
function submitForm() {
  proxy.$refs.skuRef.validate(valid => {
    if (!valid) return
    const request = form.value.skuId ? updateSku(form.value) : addSku(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.skuId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}
function handleDelete(row) {
  const skuIds = row.skuId || ids.value
  proxy.$modal.confirm('确认删除SKU编号为 "' + skuIds + '" 的数据项？').then(() => delSku(skuIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
