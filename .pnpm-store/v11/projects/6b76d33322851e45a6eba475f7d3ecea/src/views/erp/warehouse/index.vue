<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="仓库编码" prop="warehouseCode">
        <el-input v-model="queryParams.warehouseCode" placeholder="请输入仓库编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库名称" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入仓库名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库类型" prop="warehouseType">
        <el-select v-model="queryParams.warehouseType" placeholder="全部" clearable style="width: 140px">
          <el-option label="自营仓" value="self" />
          <el-option label="三方仓" value="third_party" />
          <el-option label="退货仓" value="return" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:warehouse:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:warehouse:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:warehouse:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="warehouseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="仓库ID" align="center" prop="warehouseId" width="90" />
      <el-table-column label="仓库编码" align="center" prop="warehouseCode" />
      <el-table-column label="仓库名称" align="center" prop="warehouseName" min-width="160" />
      <el-table-column label="类型" align="center" prop="warehouseType" />
      <el-table-column label="联系人" align="center" prop="contactName" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" />
      <el-table-column label="地址" align="center" prop="address" min-width="180" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:warehouse:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:warehouse:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="620px" append-to-body>
      <el-form ref="warehouseRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="仓库编码" prop="warehouseCode">
          <el-input v-model="form.warehouseCode" placeholder="请输入仓库编码" />
        </el-form-item>
        <el-form-item label="仓库名称" prop="warehouseName">
          <el-input v-model="form.warehouseName" placeholder="请输入仓库名称" />
        </el-form-item>
        <el-form-item label="仓库类型">
          <el-select v-model="form.warehouseType" style="width: 100%">
            <el-option label="自营仓" value="self" />
            <el-option label="三方仓" value="third_party" />
            <el-option label="退货仓" value="return" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入地址" />
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

<script setup name="ErpWarehouse">
import { listWarehouse, getWarehouse, addWarehouse, updateWarehouse, delWarehouse } from '@/api/erp/warehouse'

const { proxy } = getCurrentInstance()
const warehouseList = ref([])
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
  queryParams: { pageNum: 1, pageSize: 10, warehouseCode: undefined, warehouseName: undefined, warehouseType: undefined },
  rules: {
    warehouseCode: [{ required: true, message: '仓库编码不能为空', trigger: 'blur' }],
    warehouseName: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listWarehouse(queryParams.value).then(response => {
    warehouseList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function reset() {
  form.value = { warehouseId: undefined, warehouseCode: undefined, warehouseName: undefined, warehouseType: 'self', contactName: undefined, contactPhone: undefined, address: undefined, status: '0', remark: undefined }
  proxy.resetForm('warehouseRef')
}
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.warehouseId); single.value = selection.length !== 1; multiple.value = !selection.length }
function handleAdd() { reset(); open.value = true; title.value = '新增仓库' }
function handleUpdate(row) {
  reset()
  const warehouseId = row.warehouseId || ids.value
  getWarehouse(warehouseId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改仓库'
  })
}
function submitForm() {
  proxy.$refs.warehouseRef.validate(valid => {
    if (!valid) return
    const request = form.value.warehouseId ? updateWarehouse(form.value) : addWarehouse(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.warehouseId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}
function handleDelete(row) {
  const warehouseIds = row.warehouseId || ids.value
  proxy.$modal.confirm('确认删除仓库编号为 "' + warehouseIds + '" 的数据项？').then(() => delWarehouse(warehouseIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
