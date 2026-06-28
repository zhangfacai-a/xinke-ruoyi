<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="仓库" prop="warehouseName">
        <el-input v-model="queryParams.warehouseName" placeholder="请输入仓库名称" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
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
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="inventoryList">
      <el-table-column label="库存ID" align="center" prop="balanceId" width="90" />
      <el-table-column label="仓库" align="center" prop="warehouseName" min-width="140" />
      <el-table-column label="SKU编码" align="center" prop="skuCode" min-width="150" />
      <el-table-column label="SKU名称" align="center" prop="skuName" min-width="160" />
      <el-table-column label="可用库存" align="right" prop="availableQty" />
      <el-table-column label="锁定库存" align="right" prop="lockedQty" />
      <el-table-column label="在途库存" align="right" prop="inboundQty" />
      <el-table-column label="残次库存" align="right" prop="defectiveQty" />
      <el-table-column label="安全库存" align="right" prop="safetyQty" />
      <el-table-column label="成本价" align="right" prop="costPrice" />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="170" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ErpInventory">
import { listInventory } from '@/api/erp/inventory'

const { proxy } = getCurrentInstance()
const inventoryList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, warehouseName: undefined, skuCode: undefined, skuName: undefined })

function getList() {
  loading.value = true
  listInventory(queryParams.value).then(response => {
    inventoryList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

getList()
</script>
