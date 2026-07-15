<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="运营" prop="operatorName" v-if="isOperatorReport">
        <el-input v-model="queryParams.operatorName" placeholder="请输入运营姓名" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="渠道" prop="channelCode" v-if="isOperatorReport">
        <el-input v-model="queryParams.channelCode" placeholder="如 douyin_ad" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="店铺ID" prop="shopId">
        <el-input v-model="queryParams.shopId" placeholder="店铺只作为归属上下文" clearable @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-radio-group v-model="reportType" @change="getList">
        <el-radio-button value="operatorDaily">运营日报</el-radio-button>
        <el-radio-button value="operatorMonthly">运营月报</el-radio-button>
        <el-radio-button value="shopDaily">店铺日报</el-radio-button>
        <el-radio-button value="shopMonthly">店铺月报</el-radio-button>
      </el-radio-group>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="profitList">
      <el-table-column label="日期" align="center" prop="dt" width="110" />
      <el-table-column v-if="isOperatorReport" label="运营" align="center" prop="operatorName" min-width="110" />
      <el-table-column v-if="isOperatorReport" label="渠道" align="center" prop="channelName" min-width="120">
        <template #default="scope">
          {{ scope.row.channelName || scope.row.channelCode || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="店铺" align="center" prop="shopName" min-width="130" />
      <el-table-column label="GMV" align="right" prop="gmv" />
      <el-table-column label="净成交额" align="right" prop="netAmount" />
      <el-table-column label="商品成本" align="right" prop="productCost" />
      <el-table-column label="平台扣点" align="right" prop="platformFee" />
      <el-table-column label="推广费用" align="right" prop="adCost" />
      <el-table-column label="运费" align="right" prop="freightFee" />
      <el-table-column label="售后成本" align="right" prop="afterSaleCost" />
      <el-table-column label="利润" align="right" prop="profitAmount" />
      <el-table-column v-if="isOperatorReport" label="应分润" align="right" prop="shareProfitAmount" />
      <el-table-column label="ROI" align="right" prop="roi" width="90" />
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="FinanceProfit">
import {
  listDailyProfit,
  listMonthlyProfit,
  listOperatorDailyProfit,
  listOperatorMonthlyProfit
} from '@/api/finance/profit'

const { proxy } = getCurrentInstance()
const profitList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const reportType = ref('operatorDaily')
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  shopId: undefined,
  operatorName: undefined,
  channelCode: undefined
})

const isOperatorReport = computed(() => reportType.value.startsWith('operator'))

const requestMap = {
  operatorDaily: listOperatorDailyProfit,
  operatorMonthly: listOperatorMonthlyProfit,
  shopDaily: listDailyProfit,
  shopMonthly: listMonthlyProfit
}

function getList() {
  loading.value = true
  requestMap[reportType.value](queryParams.value).then(response => {
    profitList.value = response.rows || []
    total.value = response.total || 0
    loading.value = false
  }).catch(() => {
    loading.value = false
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

getList()
</script>
