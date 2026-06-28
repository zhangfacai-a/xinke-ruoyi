<template>
  <div class="app-container bi-dashboard">
    <el-row :gutter="16">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric"><span>GMV</span><strong>{{ summary.gmv || 0 }}</strong></div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric"><span>净成交额</span><strong>{{ summary.netAmount || 0 }}</strong></div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric"><span>利润</span><strong>{{ summary.profitAmount || 0 }}</strong></div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric"><span>ROI</span><strong>{{ summary.roi || 0 }}</strong></div>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="mt16">
      <el-col :xs="24" :lg="12"><div ref="gmvChartRef" class="chart"></div></el-col>
      <el-col :xs="24" :lg="12"><div ref="profitChartRef" class="chart"></div></el-col>
    </el-row>
    <el-row :gutter="16" class="mt16">
      <el-col :span="24"><div ref="rankChartRef" class="chart chart-wide"></div></el-col>
    </el-row>
  </div>
</template>

<script setup name="BiDashboard">
import * as echarts from 'echarts'
import { getProfitSummary, getGmvTrend, getProfitTrend, getProductRank } from '@/api/bi/dashboard'

const summary = ref({})
const gmvChartRef = ref()
const profitChartRef = ref()
const rankChartRef = ref()
let gmvChart
let profitChart
let rankChart

function lineOption(title, data) {
  return {
    title: { text: title, left: 12, top: 10, textStyle: { fontSize: 15 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 56, bottom: 36 },
    xAxis: { type: 'category', data: data.map(item => item.name) },
    yAxis: { type: 'value' },
    series: [{ type: 'line', smooth: true, data: data.map(item => item.value), areaStyle: {} }]
  }
}

function barOption(data) {
  return {
    title: { text: '商品TOP10', left: 12, top: 10, textStyle: { fontSize: 15 } },
    tooltip: { trigger: 'axis' },
    grid: { left: 96, right: 24, top: 56, bottom: 24 },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(item => item.name).reverse() },
    series: [{ type: 'bar', data: data.map(item => item.value).reverse() }]
  }
}

function resizeCharts() {
  gmvChart && gmvChart.resize()
  profitChart && profitChart.resize()
  rankChart && rankChart.resize()
}

function loadData() {
  Promise.all([getProfitSummary(), getGmvTrend(), getProfitTrend(), getProductRank()]).then(([summaryRes, gmvRes, profitRes, rankRes]) => {
    summary.value = summaryRes.data || {}
    gmvChart.setOption(lineOption('GMV趋势', gmvRes.data || []))
    profitChart.setOption(lineOption('利润趋势', profitRes.data || []))
    rankChart.setOption(barOption(rankRes.data || []))
  })
}

onMounted(() => {
  gmvChart = echarts.init(gmvChartRef.value)
  profitChart = echarts.init(profitChartRef.value)
  rankChart = echarts.init(rankChartRef.value)
  window.addEventListener('resize', resizeCharts)
  loadData()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  gmvChart && gmvChart.dispose()
  profitChart && profitChart.dispose()
  rankChart && rankChart.dispose()
})
</script>

<style scoped>
.bi-dashboard .metric {
  min-height: 92px;
  padding: 18px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.bi-dashboard .metric span {
  display: block;
  color: #606266;
  font-size: 13px;
}
.bi-dashboard .metric strong {
  display: block;
  margin-top: 10px;
  font-size: 26px;
  color: #111827;
}
.mt16 {
  margin-top: 16px;
}
.chart {
  height: 340px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}
.chart-wide {
  height: 420px;
}
</style>
