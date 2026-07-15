<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="流水号" prop="movementNo">
        <el-input v-model="queryParams.movementNo" placeholder="请输入流水号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="仓库" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入仓库名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="SKU编码" prop="skuCode">
        <el-input v-model="queryParams.skuCode" placeholder="请输入SKU编码" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="类型" prop="movementType">
        <el-select v-model="queryParams.movementType" placeholder="全部" clearable style="width: 130px">
          <el-option label="入库" value="IN" />
          <el-option label="出库" value="OUT" />
          <el-option label="锁定" value="LOCK" />
          <el-option label="解锁" value="UNLOCK" />
          <el-option label="调整" value="ADJUST" />
          <el-option label="调拨" value="TRANSFER" />
        </el-select>
      </el-form-item>
      <el-form-item label="来源单号" prop="sourceNo">
        <el-input v-model="queryParams.sourceNo" placeholder="请输入来源单号" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="movementList">
      <el-table-column label="流水ID" align="center" prop="movementId" width="90" />
      <el-table-column label="流水号" align="center" prop="movementNo" min-width="170" />
      <el-table-column label="仓库" align="center" prop="warehouseName" min-width="130" />
      <el-table-column label="SKU编码" align="center" prop="skuCode" min-width="150" />
      <el-table-column label="SKU名称" align="center" prop="skuName" min-width="150" />
      <el-table-column label="类型" align="center" prop="movementType" width="90" />
      <el-table-column label="来源类型" align="center" prop="sourceType" width="110" />
      <el-table-column label="来源单号" align="center" prop="sourceNo" min-width="150" />
      <el-table-column label="数量" align="right" prop="quantity" width="90" />
      <el-table-column label="变动前" align="right" prop="beforeQty" width="90" />
      <el-table-column label="变动后" align="right" prop="afterQty" width="90" />
      <el-table-column label="成本价" align="right" prop="costPrice" width="100" />
      <el-table-column label="操作人" align="center" prop="operatorName" width="100" />
      <el-table-column label="流水时间" align="center" prop="movementTime" width="170" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ErpMovement">
import { listMovement } from '@/api/erp/movement'

const { proxy } = getCurrentInstance()
const movementList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, movementNo: undefined, warehouseName: undefined, skuCode: undefined, movementType: undefined, sourceNo: undefined })

function getList() {
  loading.value = true
  listMovement(queryParams.value).then(response => {
    movementList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

getList()
</script>
