<template>
  <div class="app-container live-bi-page">
    <div class="bi-board">
      <section class="bi-hero">
        <div>
          <span class="eyebrow">Live Operations BI</span>
          <h2>直播追单经营看板</h2>
          <p>把直播间流量、评论意向、负责人跟进和订单转化放在同一张运营桌面里。</p>
        </div>
        <div class="hero-actions">
          <el-segmented v-model="quickRange" :options="rangeOptions" @change="changeQuickRange" />
          <el-button type="primary" icon="Refresh" :loading="loading" @click="loadData">刷新</el-button>
        </div>
      </section>

      <section class="filter-card">
        <el-form :inline="true" :model="queryParams" class="filter-form">
          <el-form-item label="日期">
            <el-date-picker
              v-model="queryParams.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :clearable="false"
            />
          </el-form-item>
          <el-form-item label="直播间">
            <el-autocomplete
              v-model="queryParams.roomName"
              :fetch-suggestions="queryRoomSearch"
              clearable
              placeholder="搜索直播间"
              style="width: 220px"
            />
          </el-form-item>
          <el-form-item label="负责人">
            <el-autocomplete
              v-model="queryParams.ownerName"
              :fetch-suggestions="queryOwnerSearch"
              clearable
              placeholder="搜索负责人"
              style="width: 180px"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="loadData">分析</el-button>
            <el-button icon="RefreshLeft" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section v-if="errorMessage" class="bi-error-card">
        {{ errorMessage }}
      </section>

      <section class="metric-grid" v-loading="loading">
        <article v-for="item in metrics" :key="item.key" class="metric-card">
          <span class="metric-label">{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em :class="item.good ? 'good' : ''">{{ item.desc }}</em>
        </article>
      </section>

      <section class="main-grid">
        <article class="chart-card trend-card">
          <div class="card-head">
            <div>
              <h3>线索趋势</h3>
              <p>观众、评论客户、追后成交的日趋势</p>
            </div>
          </div>
          <div ref="trendChartRef" class="chart chart-lg"></div>
        </article>

        <article class="chart-card">
          <div class="card-head">
            <div>
              <h3>追单漏斗</h3>
              <p>从进入追单池到追后成交</p>
            </div>
          </div>
          <div ref="funnelChartRef" class="chart chart-lg"></div>
        </article>
      </section>

      <section class="shop-grid">
        <article class="chart-card">
        <div class="card-head">
          <div>
            <h3>店铺追单效率</h3>
            <p>复合评分 = 高意向率、分配率、跟进率、追后成交率综合表现</p>
          </div>
        </div>
        <div ref="shopChartRef" class="chart"></div>
      </article>

      <article class="data-card shop-rank-card">
        <div class="card-head">
          <div>
            <h3>店铺经营排名</h3>
            <p>看哪些店铺值得加人、复盘或优先承接</p>
          </div>
        </div>
        <div class="shop-top" v-if="bestShop">
          <span>当前最佳店铺</span>
          <strong>{{ bestShop.shopName }}</strong>
          <small>评分 {{ number(bestShop.shopScore) }} · 追后成交 {{ number(bestShop.orderedCount) }} 单</small>
        </div>
        <el-table :data="shopRank" height="232">
          <el-table-column label="店铺" prop="shopName" min-width="150" show-overflow-tooltip />
          <el-table-column label="高意向" prop="highIntentCount" width="82" align="right" />
          <el-table-column label="未分配" prop="unassignedHighIntentCount" width="82" align="right" />
          <el-table-column label="成交" prop="orderedCount" width="70" align="right" />
          <el-table-column label="评分" width="74" align="right">
            <template #default="scope">{{ number(scope.row.shopScore) }}</template>
          </el-table-column>
        </el-table>
      </article>
      </section>

      <section class="insight-grid">
      <article class="chart-card">
        <div class="card-head">
          <div>
            <h3>评论热力</h3>
            <p>按小时看客户主动提问强度</p>
          </div>
        </div>
        <div ref="hourChartRef" class="chart"></div>
      </article>

      <article class="chart-card">
        <div class="card-head">
          <div>
            <h3>成交信号词云</h3>
            <p>按业务权重识别更接近成交的评论信号</p>
          </div>
        </div>
        <div class="word-cloud" v-if="signalWords.length">
          <span
            v-for="item in signalWords"
            :key="`${item.category}-${item.word}`"
            class="word-chip"
            :style="wordStyle(item)"
          >
            {{ item.word }}
            <small>{{ item.category }} · {{ number(item.hitCount) }}</small>
          </span>
        </div>
        <el-empty v-else description="暂无成交信号" :image-size="84" />
      </article>
      </section>

      <section class="table-grid">
      <article class="data-card">
        <div class="card-head">
          <div>
            <h3>直播间质量排行</h3>
            <p>优先看高评论率和追后成交</p>
          </div>
        </div>
        <el-table :data="roomRank" height="330">
          <el-table-column label="直播间" prop="roomName" min-width="180" show-overflow-tooltip />
          <el-table-column label="观众" prop="viewerCount" width="82" align="right" />
          <el-table-column label="有评论" prop="commentLeadCount" width="82" align="right" />
          <el-table-column label="未分配" prop="unassignedHighIntentCount" width="82" align="right" />
          <el-table-column label="追后成交" prop="orderedCount" width="92" align="right" />
          <el-table-column label="评论率" width="92" align="right">
            <template #default="scope">{{ percent(scope.row.commentRate) }}</template>
          </el-table-column>
        </el-table>
      </article>

      <article class="data-card">
        <div class="card-head">
          <div>
            <h3>负责人绩效</h3>
            <p>跟进覆盖和追后成交能力</p>
          </div>
        </div>
        <div class="owner-podium" v-if="ownerPodium.length">
          <div v-for="(item, index) in ownerPodium" :key="item.ownerName">
            <span>TOP {{ index + 1 }}</span>
            <strong>{{ item.ownerName }}</strong>
            <small>追后成交 {{ number(item.orderedCount) }} 单 · 跟进率 {{ percent(item.followRate) }}</small>
          </div>
        </div>
        <el-table :data="ownerRank" height="232">
          <el-table-column label="负责人" prop="ownerName" min-width="120" show-overflow-tooltip />
          <el-table-column label="高意向" prop="commentLeadCount" width="82" align="right" />
          <el-table-column label="已跟进" prop="followedViewerCount" width="82" align="right" />
          <el-table-column label="追后成交" prop="orderedCount" width="92" align="right" />
          <el-table-column label="成交率" width="86" align="right">
            <template #default="scope">{{ percent(scope.row.orderRate) }}</template>
          </el-table-column>
        </el-table>
      </article>

      <article class="data-card action-card">
        <div class="card-head">
          <div>
            <h3>运营动作建议</h3>
            <p>按当前数据自动生成</p>
          </div>
        </div>
        <ul class="action-list">
          <li v-for="item in actionTips" :key="item.title" :class="item.level">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </li>
        </ul>
      </article>

      <article class="data-card quality-card">
        <div class="card-head">
          <div>
            <h3>运营缺口</h3>
            <p>只展示可处理的问题</p>
          </div>
        </div>
        <div class="quality-list">
          <div v-for="item in qualityRows" :key="item.issueType">
            <span>{{ issueLabel(item.issueType) }}</span>
            <strong>{{ number(item.issueCount) }}</strong>
          </div>
        </div>
      </article>
      </section>
    </div>
  </div>
</template>

<script setup>
import * as echarts from 'echarts'
import { useRoute } from 'vue-router'
import { getViewerBi, listViewerOwnerSuggestions, listViewerRoomSuggestions } from '@/api/live/viewer'

const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const quickRange = ref(String(route.query.days || '7'))
const dashboard = ref({})
const trendChartRef = ref(null)
const funnelChartRef = ref(null)
const hourChartRef = ref(null)
const shopChartRef = ref(null)
let trendChart
let funnelChart
let hourChart
let shopChart

const rangeOptions = [
  { label: '7天', value: '7' },
  { label: '30天', value: '30' },
  { label: '今天', value: '1' }
]

const queryParams = reactive({
  dateRange: initialDateRange(),
  roomName: route.query.roomName ? String(route.query.roomName) : undefined,
  ownerName: route.query.ownerName ? String(route.query.ownerName) : undefined
})

const overview = computed(() => dashboard.value.overview || {})
const trend = computed(() => dashboard.value.trend || [])
const roomRank = computed(() => dashboard.value.roomRank || [])
const shopRank = computed(() => dashboard.value.shopRank || [])
const ownerRank = computed(() => dashboard.value.ownerRank || [])
const hourHeat = computed(() => dashboard.value.hourHeat || [])
const signalWords = computed(() => dashboard.value.signalWords || [])
const qualityRows = computed(() => dashboard.value.quality || [])
const ownerPodium = computed(() => ownerRank.value.filter((item) => item.ownerName && item.ownerName !== '未分配').slice(0, 3))
const bestShop = computed(() => shopRank.value[0])

const metrics = computed(() => [
  { key: 'shop', label: '覆盖店铺', value: number(overview.value.shopCount), desc: '区间有线索店铺' },
  { key: 'viewer', label: '去重观众', value: number(overview.value.viewerCount), desc: '区间客户池规模' },
  { key: 'comment', label: '高意向客户', value: number(overview.value.commentLeadCount), desc: `${percent(overview.value.commentRate)} 评论率`, good: true },
  { key: 'unassigned', label: '未分配高意向', value: number(overview.value.unassignedHighIntentCount), desc: '需要立即分配' },
  { key: 'pending', label: '待跟进客户', value: number(overview.value.pendingFollowCount), desc: `${percent(overview.value.followRate)} 跟进覆盖率` },
  { key: 'ordered', label: '追后成交', value: number(overview.value.orderedCount), desc: `${percent(overview.value.orderRate)} 有效转化率`, good: true }
])

const actionTips = computed(() => {
  const list = []
  const unassigned = num(overview.value.unassignedHighIntentCount)
  const pending = num(overview.value.pendingFollowCount)
  const orderRate = num(overview.value.orderRate)
  const assignRate = num(overview.value.assignRate)
  const followRate = num(overview.value.followRate)
  const bestRoom = roomRank.value[0]
  const weakShop = shopRank.value.find((item) => num(item.highIntentCount) >= 5 && num(item.unassignedHighIntentCount) > 0)

  if (unassigned > 0) {
    list.push({ level: 'warn', title: `还有 ${unassigned} 个高意向客户未分配`, desc: '先分给运营，避免评论客户冷掉。' })
  }
  if (pending > 0) {
    list.push({ level: 'warn', title: `${pending} 个已分配客户还没跟进`, desc: '负责人已填但没有跟进记录，建议今天优先清掉。' })
  }
  if (assignRate < 70 && num(overview.value.commentLeadCount) >= 20) {
    list.push({ level: 'risk', title: '高意向分配率偏低', desc: '直播后 30 分钟内未分配，会明显降低追单效率。' })
  }
  if (followRate < 60 && num(overview.value.assignedViewerCount) >= 10) {
    list.push({ level: 'risk', title: '负责人跟进覆盖不足', desc: '建议按负责人拉清单，先处理有评论的客户。' })
  }
  if (orderRate < 8 && num(overview.value.commentLeadCount) >= 20) {
    list.push({ level: 'risk', title: '追后成交率偏低', desc: '建议复盘话术、价格异议和赠品承诺。' })
  }
  if (weakShop) {
    list.push({ level: 'warn', title: `${weakShop.shopName} 有高意向未承接`, desc: `未分配 ${number(weakShop.unassignedHighIntentCount)} 人，建议先补运营承接。` })
  }
  if (bestRoom) {
    list.push({ level: 'good', title: `优先复盘 ${bestRoom.roomName}`, desc: `高意向 ${number(bestRoom.commentLeadCount)} 人，追后成交 ${number(bestRoom.orderedCount)} 单。` })
  }
  if (!list.length) {
    list.push({ level: 'good', title: '当前追单链路健康', desc: '继续保持评论客户优先分配和及时跟进。' })
  }
  return list
})

function defaultDateRange(days) {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - Math.max(days - 1, 0))
  return [formatDate(start), formatDate(end)]
}

function initialDateRange() {
  const begin = route.query.beginLeadDate || route.query.beginDate
  const end = route.query.endLeadDate || route.query.endDate
  if (begin && end) {
    return [String(begin), String(end)]
  }
  return defaultDateRange(Number(route.query.days || 7))
}

function formatDate(date) {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

function num(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

function number(value) {
  return num(value).toLocaleString()
}

function percent(value) {
  const n = num(value)
  return n ? `${n.toFixed(2)}%` : '0.00%'
}

function issueLabel(type) {
  return ({
    empty_nickname: '空昵称观众',
    empty_room_name: '无直播间名称',
    unassigned_high_intent: '未分配高意向'
  })[type] || type
}

const wordColors = {
  购买信号: '#6c5ce7',
  价格敏感: '#e17055',
  型号对比: '#0984e3',
  赠品活动: '#d68910',
  安装售后: '#00a884',
  强需求: '#d63031',
  使用场景: '#636e72'
}

function wordStyle(item) {
  const maxScore = Math.max(...signalWords.value.map((row) => num(row.score)), 1)
  const ratio = Math.max(num(item.score) / maxScore, 0.18)
  return {
    '--word-color': wordColors[item.category] || '#6c5ce7',
    fontSize: `${13 + ratio * 18}px`,
    opacity: 0.72 + ratio * 0.28
  }
}

function buildQuery() {
  const [beginLeadDate, endLeadDate] = queryParams.dateRange || []
  return {
    beginLeadDate,
    endLeadDate,
    roomName: queryParams.roomName,
    ownerName: queryParams.ownerName
  }
}

function toAutocompleteRows(list) {
  return (list || []).filter(Boolean).map((item) => ({ value: item }))
}

function queryRoomSearch(keyword, cb) {
  listViewerRoomSuggestions({ keyword }).then((res) => cb(toAutocompleteRows(res.data))).catch(() => cb([]))
}

function queryOwnerSearch(keyword, cb) {
  listViewerOwnerSuggestions({ keyword }).then((res) => cb(toAutocompleteRows(res.data))).catch(() => cb([]))
}

function changeQuickRange(value) {
  queryParams.dateRange = defaultDateRange(Number(value))
  loadData()
}

function resetQuery() {
  quickRange.value = '7'
  queryParams.dateRange = defaultDateRange(7)
  queryParams.roomName = undefined
  queryParams.ownerName = undefined
  loadData()
}

function loadData() {
  loading.value = true
  errorMessage.value = ''
  getViewerBi(buildQuery()).then((res) => {
    dashboard.value = res.data || {}
    nextTick(renderCharts)
  }).catch((error) => {
    dashboard.value = {}
    errorMessage.value = error?.msg || error?.message || 'BI接口加载失败，请检查后端是否已部署最新版本。'
  }).finally(() => {
    loading.value = false
  })
}

function ensureCharts() {
  trendChart ||= echarts.init(trendChartRef.value)
  funnelChart ||= echarts.init(funnelChartRef.value)
  hourChart ||= echarts.init(hourChartRef.value)
  shopChart ||= echarts.init(shopChartRef.value)
}

function renderCharts() {
  if (!trendChartRef.value) return
  ensureCharts()
  renderTrend()
  renderFunnel()
  renderHour()
  renderShop()
}

function renderTrend() {
  const dates = trend.value.map((item) => item.statDate)
  trendChart.setOption({
    color: ['#6c5ce7', '#00b894', '#fdcb6e', '#0984e3'],
    tooltip: { trigger: 'axis' },
    legend: { top: 0, right: 8, data: ['观众', '高意向', '未分配', '追后成交'] },
    grid: { left: 34, right: 20, top: 42, bottom: 28 },
    xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e7e9f4' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf0f8' } } },
    series: [
      { name: '观众', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: trend.value.map((item) => num(item.viewerCount)) },
      { name: '高意向', type: 'line', smooth: true, areaStyle: { opacity: 0.08 }, data: trend.value.map((item) => num(item.commentLeadCount)) },
      { name: '未分配', type: 'line', smooth: true, data: trend.value.map((item) => num(item.unassignedHighIntentCount)) },
      { name: '追后成交', type: 'bar', barWidth: 16, data: trend.value.map((item) => num(item.orderedCount)) }
    ]
  })
}

function renderFunnel() {
  funnelChart.setOption({
    color: ['#6c5ce7', '#74b9ff', '#55efc4', '#fdcb6e', '#00b894'],
    tooltip: { trigger: 'item' },
    series: [{
      type: 'funnel',
      left: 20,
      right: 20,
      top: 16,
      bottom: 12,
      label: { formatter: '{b}: {c}' },
      data: [
        { name: '观众', value: num(overview.value.viewerCount) },
        { name: '高意向', value: num(overview.value.commentLeadCount) },
        { name: '已分配', value: num(overview.value.assignedViewerCount) },
        { name: '已跟进', value: num(overview.value.followedViewerCount) },
        { name: '追后成交', value: num(overview.value.orderedCount) }
      ]
    }]
  })
}

function renderHour() {
  const map = new Map(hourHeat.value.map((item) => [Number(item.statHour), num(item.commentCount)]))
  const hours = Array.from({ length: 24 }, (_, index) => index)
  hourChart.setOption({
    color: ['#a29bfe'],
    tooltip: { trigger: 'axis' },
    grid: { left: 34, right: 12, top: 24, bottom: 28 },
    xAxis: { type: 'category', data: hours.map((h) => `${h}:00`) },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf0f8' } } },
    series: [{ type: 'bar', barWidth: 12, data: hours.map((h) => map.get(h) || 0) }]
  })
}

function renderShop() {
  const rows = shopRank.value.slice(0, 8).reverse()
  shopChart.setOption({
    color: ['#6c5ce7', '#00b894', '#fdcb6e'],
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { top: 0, right: 8, data: ['评分', '高意向', '追后成交'] },
    grid: { left: 92, right: 24, top: 42, bottom: 24 },
    xAxis: { type: 'value', splitLine: { lineStyle: { color: '#edf0f8' } } },
    yAxis: {
      type: 'category',
      data: rows.map((item) => item.shopName),
      axisLabel: { width: 84, overflow: 'truncate' }
    },
    series: [
      { name: '评分', type: 'bar', barWidth: 12, data: rows.map((item) => num(item.shopScore)) },
      { name: '高意向', type: 'bar', barWidth: 12, data: rows.map((item) => num(item.highIntentCount)) },
      { name: '追后成交', type: 'bar', barWidth: 12, data: rows.map((item) => num(item.orderedCount)) }
    ]
  })
}

function resizeCharts() {
  trendChart?.resize()
  funnelChart?.resize()
  hourChart?.resize()
  shopChart?.resize()
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', resizeCharts)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  trendChart?.dispose()
  funnelChart?.dispose()
  hourChart?.dispose()
  shopChart?.dispose()
})
</script>

<style scoped lang="scss">
.live-bi-page {
  min-height: calc(100vh - 84px);
  overflow-x: hidden;
  padding: clamp(10px, 1.1vw, 18px);
  background:
    radial-gradient(circle at 16% 0%, rgba(108, 92, 231, .13), transparent 26%),
    linear-gradient(180deg, #f7f8ff 0%, #f4f6fb 100%);
  color: #22263a;
}

.bi-board {
  display: grid;
  gap: clamp(10px, 1vw, 14px);
  min-width: 0;
  width: 100%;
  max-width: 1600px;
  margin: 0 auto;
}

.bi-hero,
.filter-card,
.bi-error-card,
.metric-card,
.chart-card,
.data-card {
  border: 1px solid rgba(122, 118, 255, .12);
  border-radius: 18px;
  background: rgba(255, 255, 255, .86);
  box-shadow: 0 18px 42px rgba(80, 86, 160, .10);
  backdrop-filter: blur(14px);
}

.bi-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-width: 0;
  padding: clamp(16px, 1.6vw, 22px);

  h2 {
    margin: 7px 0;
    font-size: clamp(22px, 2vw, 28px);
    line-height: 1.18;
  }

  p {
    margin: 0;
    color: #717a96;
  }
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(108, 92, 231, .10);
  color: #6c5ce7;
  font-weight: 700;
}

.hero-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.filter-card {
  min-width: 0;
  padding: 14px 18px 0;
}

.bi-error-card {
  padding: 14px 18px;
  color: #d63031;
  font-weight: 700;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 0 10px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 12px;
}

.metric-card {
  min-width: 0;
  min-height: clamp(88px, 6vw, 104px);
  padding: clamp(13px, 1vw, 16px);

  .metric-label {
    display: block;
    color: #7b849d;
    font-size: 13px;
  }

  strong {
    display: block;
    margin-top: 8px;
    font-size: clamp(21px, 1.8vw, 25px);
    line-height: 1;
  }

  em {
    display: block;
    margin-top: 10px;
    color: #8b92a8;
    font-style: normal;
    font-size: 12px;

    &.good {
      color: #00a884;
    }
  }
}

.main-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(0, .9fr);
  gap: 14px;
}

.insight-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 14px;
}

.shop-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, .9fr);
  gap: 14px;
}

.chart-card,
.data-card {
  overflow: hidden;
  min-width: 0;
  padding: clamp(14px, 1.2vw, 18px);
}

.chart-card.wide {
  grid-column: span 2;
}

.card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 10px;

  h3 {
    margin: 0;
    font-size: 17px;
  }

  p {
    margin: 6px 0 0;
    color: #8790aa;
    font-size: 13px;
  }
}

.chart {
  height: clamp(220px, 18vw, 270px);
}

.chart-lg {
  height: clamp(260px, 25vw, 330px);
}

.word-cloud {
  display: flex;
  align-content: center;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  min-height: clamp(220px, 18vw, 270px);
  padding: 12px 4px;
}

.word-chip {
  display: inline-flex;
  align-items: baseline;
  gap: 6px;
  padding: 8px 12px;
  border: 1px solid color-mix(in srgb, var(--word-color) 22%, transparent);
  border-radius: 999px;
  background: color-mix(in srgb, var(--word-color) 10%, #fff);
  color: var(--word-color);
  font-weight: 800;
  line-height: 1;
  box-shadow: 0 10px 24px color-mix(in srgb, var(--word-color) 13%, transparent);

  small {
    color: #8790aa;
    font-size: 11px;
    font-weight: 600;
    white-space: nowrap;
  }
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.shop-rank-card {
  .shop-top {
    margin-bottom: 12px;
    padding: 14px;
    border: 1px solid rgba(108, 92, 231, .16);
    border-radius: 16px;
    background:
      radial-gradient(circle at 85% 0%, rgba(162, 155, 254, .28), transparent 42%),
      linear-gradient(135deg, rgba(108, 92, 231, .10), rgba(255, 255, 255, .92));

    span,
    strong,
    small {
      display: block;
    }

    span {
      color: #6c5ce7;
      font-size: 12px;
      font-weight: 800;
    }

    strong {
      overflow: hidden;
      margin-top: 8px;
      color: #22263a;
      font-size: 20px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    small {
      margin-top: 8px;
      color: #7b849d;
      font-size: 12px;
    }
  }
}

.owner-podium {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 12px;

  div {
    min-width: 0;
    padding: 12px;
    border: 1px solid rgba(108, 92, 231, .14);
    border-radius: 14px;
    background: linear-gradient(135deg, rgba(108, 92, 231, .10), rgba(255, 255, 255, .86));
  }

  span,
  strong,
  small {
    display: block;
  }

  span {
    color: #6c5ce7;
    font-size: 11px;
    font-weight: 800;
  }

  strong {
    overflow: hidden;
    margin-top: 6px;
    color: #22263a;
    font-size: 16px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    margin-top: 6px;
    color: #8790aa;
    font-size: 11px;
    line-height: 1.4;
  }
}

.action-list {
  display: grid;
  gap: 10px;
  padding: 0;
  margin: 0;
  list-style: none;

  li {
    padding: 12px;
    border-radius: 14px;
    background: #f7f8ff;
    border: 1px solid #edf0ff;

    &.risk {
      background: #fff5f5;
      border-color: #ffe1e1;
    }

    &.warn {
      background: #fff9ed;
      border-color: #ffe4b3;
    }

    &.good {
      background: #effcf7;
      border-color: #cbf5e5;
    }

    strong,
    span {
      display: block;
    }

    strong {
      font-size: 14px;
    }

    span {
      margin-top: 5px;
      color: #7b849d;
      font-size: 12px;
      line-height: 1.5;
    }
  }
}

.quality-list {
  display: grid;
  gap: 12px;

  div {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 13px 14px;
    border-radius: 14px;
    background: #f7f8ff;
    color: #737c96;
  }

  strong {
    color: #22263a;
    font-size: 20px;
  }
}

@media (max-width: 1440px) {
  .main-grid,
  .insight-grid,
  .shop-grid,
  .table-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .bi-hero,
  .hero-actions {
    align-items: flex-start;
    flex-direction: column;
  }

  .metric-grid,
  .main-grid,
  .insight-grid,
  .shop-grid,
  .table-grid {
    grid-template-columns: 1fr;
  }

  .chart-card.wide {
    grid-column: auto;
  }

  .filter-form :deep(.el-form-item) {
    width: 100%;
  }

  .filter-form :deep(.el-date-editor),
  .filter-form :deep(.el-autocomplete) {
    width: 100% !important;
  }
}
</style>
