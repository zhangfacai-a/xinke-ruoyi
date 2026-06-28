<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="店铺ID" prop="shopId">
        <el-input v-model="queryParams.shopId" placeholder="请输入店铺ID" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-radio-group v-model="reportType" @change="getList">
        <el-radio-button value="daily">日报</el-radio-button>
        <el-radio-button value="monthly">月报</el-radio-button>
      </el-radio-group>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="profitList">
      <el-table-column label="日期" align="center" prop="dt" width="120" />
      <el-table-column label="店铺" align="center" prop="shopName" min-width="140" />
      <el-table-column label="GMV" align="right" prop="gmv" />
      <el-table-column label="净成交额" align="right" prop="netAmount" />
      <el-table-column label="商品成本" align="right" prop="productCost" />
      <el-table-column label="平台扣点" align="right" prop="platformFee" />
      <el-table-column label="推广费用" align="right" prop="adCost" />
      <el-table-column label="运费" align="right" prop="freightFee" />
      <el-table-column label="售后成本" align="right" prop="afterSaleCost" />
      <el-table-column label="利润" align="right" prop="profitAmount" />
      <el-table-column label="ROI" align="right" prop="roi" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="FinanceProfit">
import { listDailyProfit, listMonthlyProfit } from '@/api/finance/profit'

const { proxy } = getCurrentInstance()
const profitList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const reportType = ref('daily')
const queryParams = ref({ pageNum: 1, pageSize: 10, shopId: undefined })

function getList() {
  loading.value = true
  const request = reportType.value === 'daily' ? listDailyProfit : listMonthlyProfit
  request(queryParams.value).then(response => {
    profitList.value = response.rows
    total.value = response.total
    loading.value = false
  })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }

getList()
</script>
