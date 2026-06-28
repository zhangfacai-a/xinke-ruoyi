<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="订单号" prop="orderId">
        <el-input v-model="queryParams.orderId" placeholder="请输入订单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="店铺" prop="shopName">
        <el-input v-model="queryParams.shopName" placeholder="请输入店铺" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="商品" prop="productName">
        <el-input v-model="queryParams.productName" placeholder="请输入商品" clearable style="width: 200px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="orderList">
      <el-table-column label="订单号" align="center" prop="orderId" width="150" />
      <el-table-column label="店铺" align="center" prop="shopName" width="140" />
      <el-table-column label="商品" align="center" prop="productName" min-width="160" />
      <el-table-column label="SKU" align="center" prop="skuName" min-width="140" />
      <el-table-column label="数量" align="right" prop="quantity" width="80" />
      <el-table-column label="支付金额" align="right" prop="payAmount" width="110" />
      <el-table-column label="商品成本" align="right" prop="productCost" width="110" />
      <el-table-column label="平台扣点" align="right" prop="platformFee" width="110" />
      <el-table-column label="推广费" align="right" prop="adCost" width="100" />
      <el-table-column label="运费" align="right" prop="freightFee" width="90" />
      <el-table-column label="售后成本" align="right" prop="refundCost" width="110" />
      <el-table-column label="利润" align="right" prop="profitAmount" width="110" />
      <el-table-column label="支付时间" align="center" prop="payTime" width="170" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="ErpOrder">
import { listOrder } from '@/api/erp/order'

const { proxy } = getCurrentInstance()
const orderList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, orderId: undefined, shopName: undefined, productName: undefined })

function getList() {
  loading.value = true
  listOrder(queryParams.value).then(response => {
    orderList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

getList()
</script>
