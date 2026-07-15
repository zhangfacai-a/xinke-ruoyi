<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="店铺编码" prop="shopCode">
        <el-input v-model="queryParams.shopCode" placeholder="请输入店铺编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="店铺名称" prop="shopName">
        <el-input v-model="queryParams.shopName" placeholder="请输入店铺名称" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="平台" prop="platformName">
        <el-input v-model="queryParams.platformName" placeholder="请输入平台名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:shop:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:shop:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:shop:remove']">删除</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="shopList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="店铺ID" align="center" prop="shopId" width="90" />
      <el-table-column label="店铺编码" align="center" prop="shopCode" />
      <el-table-column label="店铺名称" align="center" prop="shopName" min-width="160" />
      <el-table-column label="平台编码" align="center" prop="platformCode" />
      <el-table-column label="平台名称" align="center" prop="platformName" />
      <el-table-column label="负责人" align="center" prop="ownerName" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" align="center" class-name="small-padding fixed-width">
        <template #default="scope">
          <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['erp:shop:edit']">修改</el-button>
          <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['erp:shop:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-form ref="shopRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="店铺编码" prop="shopCode">
          <el-input v-model="form.shopCode" placeholder="请输入店铺编码" />
        </el-form-item>
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="平台编码">
          <el-input v-model="form.platformCode" placeholder="如 douyin / tmall / jd" />
        </el-form-item>
        <el-form-item label="平台名称">
          <el-input v-model="form.platformName" placeholder="请输入平台名称" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.ownerName" placeholder="请输入负责人" />
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

<script setup name="ErpShop">
import { listShop, getShop, addShop, updateShop, delShop } from '@/api/erp/shop'

const { proxy } = getCurrentInstance()
const shopList = ref([])
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
  queryParams: { pageNum: 1, pageSize: 10, shopCode: undefined, shopName: undefined, platformName: undefined },
  rules: {
    shopCode: [{ required: true, message: '店铺编码不能为空', trigger: 'blur' }],
    shopName: [{ required: true, message: '店铺名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function getList() {
  loading.value = true
  listShop(queryParams.value).then(response => {
    shopList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function reset() {
  form.value = { shopId: undefined, shopCode: undefined, shopName: undefined, platformCode: undefined, platformName: undefined, ownerName: undefined, status: '0', remark: undefined }
  proxy.resetForm('shopRef')
}
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.shopId); single.value = selection.length !== 1; multiple.value = !selection.length }
function handleAdd() { reset(); open.value = true; title.value = '新增店铺' }
function handleUpdate(row) {
  reset()
  const shopId = row.shopId || ids.value
  getShop(shopId).then(response => {
    form.value = response.data
    open.value = true
    title.value = '修改店铺'
  })
}
function submitForm() {
  proxy.$refs.shopRef.validate(valid => {
    if (!valid) return
    const request = form.value.shopId ? updateShop(form.value) : addShop(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(form.value.shopId ? '修改成功' : '新增成功')
      open.value = false
      getList()
    })
  })
}
function handleDelete(row) {
  const shopIds = row.shopId || ids.value
  proxy.$modal.confirm('确认删除店铺编号为 "' + shopIds + '" 的数据项？').then(() => delShop(shopIds)).then(() => {
    getList()
    proxy.$modal.msgSuccess('删除成功')
  }).catch(() => {})
}

getList()
</script>
