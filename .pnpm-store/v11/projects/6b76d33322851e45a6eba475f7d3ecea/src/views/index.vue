<template>
  <div class="analytics-home">
    <section class="hero-panel soft-card">
      <div>
        <p class="eyebrow">Ecommerce ERP + BI Command Center</p>
        <h1>电商经营数据中心</h1>
        <p class="hero-copy">统一查看销售、利润、库存、财务结算与待办进度，支撑运营、财务和管理层的日常决策。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" icon="TrendCharts">查看BI看板</el-button>
        <el-button plain icon="Download">导出日报</el-button>
      </div>
    </section>

    <section class="kpi-grid">
      <article v-for="item in kpis" :key="item.label" class="kpi-card soft-card">
        <div class="kpi-top">
          <span class="kpi-icon" :class="item.tone">
            <component :is="item.icon" />
          </span>
          <span class="kpi-change" :class="{ down: item.down }">{{ item.change }}</span>
        </div>
        <div class="kpi-value">{{ item.value }}</div>
        <div class="kpi-label">{{ item.label }}</div>
        <div class="mini-track">
          <span :style="{ width: item.progress + '%' }"></span>
        </div>
      </article>
    </section>

    <section class="dashboard-grid">
      <article class="chart-card soft-card span-2">
        <div class="card-header">
          <div>
            <h2>GMV 与利润趋势</h2>
            <p>近 7 日销售额、净利润与费用走势</p>
          </div>
          <el-segmented v-model="trendRange" :options="['7日', '30日', '季度']" />
        </div>
        <div ref="lineChartRef" class="chart-box"></div>
      </article>

      <article class="chart-card soft-card">
        <div class="card-header">
          <div>
            <h2>渠道销售排行</h2>
            <p>按平台/店铺聚合</p>
          </div>
        </div>
        <div ref="barChartRef" class="chart-box"></div>
      </article>

      <article class="task-panel soft-card">
        <div class="card-header compact">
          <div>
            <h2>财务待办</h2>
            <p>本周需要处理的关键事项</p>
          </div>
          <el-tag type="primary" effect="light">8项</el-tag>
        </div>
        <div class="task-list">
          <div v-for="task in tasks" :key="task.title" class="task-item">
            <div class="task-check" :class="task.status"></div>
            <div class="task-copy">
              <strong>{{ task.title }}</strong>
              <span>{{ task.desc }}</span>
            </div>
            <el-tag :type="task.type" effect="plain">{{ task.label }}</el-tag>
          </div>
        </div>
      </article>

      <article class="calendar-panel soft-card">
        <div class="card-header compact">
          <div>
            <h2>结算日程</h2>
            <p>6月关键节点</p>
          </div>
          <el-button circle plain icon="Calendar" />
        </div>
        <div class="calendar-grid">
          <span v-for="day in weekDays" :key="day" class="week-day">{{ day }}</span>
          <button
            v-for="day in calendarDays"
            :key="day.day"
            class="calendar-day"
            :class="{ active: day.active, muted: day.muted, event: day.event }"
          >
            {{ day.label }}
          </button>
        </div>
        <div class="schedule-list">
          <div v-for="item in schedules" :key="item.time" class="schedule-item">
            <span>{{ item.time }}</span>
            <strong>{{ item.title }}</strong>
          </div>
        </div>
      </article>

      <article class="notes-panel soft-card span-2">
        <div class="card-header compact">
          <div>
            <h2>经营笔记</h2>
            <p>运营、财务与管理层共享摘要</p>
          </div>
          <el-button plain icon="Plus">新增</el-button>
        </div>
        <div class="notes-grid">
          <div v-for="note in notes" :key="note.title" class="note-card">
            <span>{{ note.tag }}</span>
            <h3>{{ note.title }}</h3>
            <p>{{ note.content }}</p>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup name="Index">
import * as echarts from 'echarts'
import { Money, TrendCharts, ShoppingCart, Wallet, Goods, Tickets } from '@element-plus/icons-vue'

const trendRange = ref('7日')
const lineChartRef = ref(null)
const barChartRef = ref(null)
let lineChart
let barChart

const kpis = [
  { label: 'GMV', value: '¥ 1,286,420', change: '+18.2%', progress: 84, icon: Money, tone: 'purple' },
  { label: '净利润', value: '¥ 284,910', change: '+12.6%', progress: 72, icon: Wallet, tone: 'blue' },
  { label: '订单数', value: '18,936', change: '+9.4%', progress: 66, icon: ShoppingCart, tone: 'green' },
  { label: '售后成本', value: '¥ 36,210', change: '-4.1%', progress: 38, icon: Tickets, tone: 'orange', down: true }
]

const tasks = [
  { title: '确认抖音 6 月结算单', desc: '2 个店铺待审核', label: '今天', type: 'danger', status: 'urgent' },
  { title: '银行流水核销', desc: '13 条收款待匹配', label: '财务', type: 'warning', status: 'running' },
  { title: '费用单生成凭证', desc: '推广费与物流费待过账', label: '凭证', type: 'primary', status: 'running' },
  { title: '库存成本复核', desc: 'TOP SKU 成本波动超过阈值', label: '运营', type: 'info', status: 'idle' }
]

const weekDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun']
const calendarDays = Array.from({ length: 35 }, (_, index) => {
  const label = index < 1 ? 31 : index
  return {
    day: index,
    label,
    muted: index < 1 || index > 30,
    active: index === 27,
    event: [4, 11, 18, 27].includes(index)
  }
})

const schedules = [
  { time: '10:00', title: '平台结算复核' },
  { time: '14:30', title: 'ROI 复盘会议' },
  { time: '16:00', title: '月结检查' }
]

const notes = [
  { tag: '利润', title: '利润率提升 2.3%', content: '本周佣金费率下降，广告 ROI 回升，净利率维持在 22% 以上。' },
  { tag: '库存', title: '3 个 SKU 进入预警', content: '建议优先处理周转天数超过 45 天的滞销 SKU，降低仓储占用。' },
  { tag: '财务', title: '对账差异收敛', content: '差异金额集中在平台扣点与支付手续费口径，需要补充费用映射规则。' }
]

function buildLineChart() {
  lineChart = echarts.init(lineChartRef.value)
  lineChart.setOption({
    color: ['#6C5CE7', '#00B894', '#FDCB6E'],
    tooltip: { trigger: 'axis' },
    grid: { top: 34, right: 20, bottom: 28, left: 42 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: ['06-22', '06-23', '06-24', '06-25', '06-26', '06-27', '06-28'],
      axisLine: { lineStyle: { color: '#E5E8F1' } },
      axisLabel: { color: '#8A91A6' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#8A91A6' },
      splitLine: { lineStyle: { color: '#EEF1F7' } }
    },
    series: [
      { name: 'GMV', type: 'line', smooth: true, symbolSize: 8, areaStyle: { opacity: 0.12 }, data: [120, 148, 132, 176, 188, 214, 236] },
      { name: '净利润', type: 'line', smooth: true, symbolSize: 8, areaStyle: { opacity: 0.08 }, data: [34, 42, 39, 51, 56, 63, 69] },
      { name: '费用', type: 'line', smooth: true, symbolSize: 8, data: [22, 24, 28, 27, 31, 30, 33] }
    ]
  })
}

function buildBarChart() {
  barChart = echarts.init(barChartRef.value)
  barChart.setOption({
    color: ['#6C5CE7'],
    tooltip: { trigger: 'axis' },
    grid: { top: 28, right: 16, bottom: 26, left: 46 },
    xAxis: {
      type: 'category',
      data: ['抖音', '天猫', '京东', '拼多多', '小红书'],
      axisTick: { show: false },
      axisLine: { lineStyle: { color: '#E5E8F1' } },
      axisLabel: { color: '#8A91A6' }
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#8A91A6' },
      splitLine: { lineStyle: { color: '#EEF1F7' } }
    },
    series: [
      {
        name: '销售额',
        type: 'bar',
        barWidth: 22,
        data: [386, 312, 246, 198, 126],
        itemStyle: {
          borderRadius: [8, 8, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#A29BFE' },
            { offset: 1, color: '#6C5CE7' }
          ])
        }
      }
    ]
  })
}

function resizeCharts() {
  lineChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  nextTick(() => {
    buildLineChart()
    buildBarChart()
    window.addEventListener('resize', resizeCharts)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  lineChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped lang="scss">
.analytics-home {
  min-height: 100%;
  padding: 24px;
}

.hero-panel {
  min-height: 160px;
  padding: 28px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.92), rgba(255, 255, 255, 0.72)),
    radial-gradient(circle at 90% 20%, rgba(162, 155, 254, 0.34), transparent 34%) !important;
  overflow: hidden;
}

.eyebrow {
  margin: 0 0 10px;
  color: #6c5ce7;
  font-size: 12px;
  font-weight: 750;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #1f2433;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 800;
}

.hero-copy {
  max-width: 680px;
  margin: 12px 0 0;
  color: #778095;
  font-size: 14px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.kpi-card {
  padding: 20px;
}

.kpi-top,
.card-header,
.task-item,
.schedule-item {
  display: flex;
  align-items: center;
}

.kpi-top,
.card-header {
  justify-content: space-between;
  gap: 16px;
}

.kpi-icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  color: #ffffff;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  box-shadow: 0 12px 24px rgba(108, 92, 231, 0.22);

  svg {
    width: 20px;
    height: 20px;
  }
}

.kpi-icon.blue { background: linear-gradient(135deg, #4c8dff, #8fb8ff); }
.kpi-icon.green { background: linear-gradient(135deg, #00b894, #55efc4); }
.kpi-icon.orange { background: linear-gradient(135deg, #fdcb6e, #ffeaa7); color: #7a4d00; }

.kpi-change {
  padding: 5px 10px;
  border-radius: 999px;
  color: #00a37a;
  background: rgba(0, 184, 148, 0.10);
  font-size: 12px;
  font-weight: 750;
}

.kpi-change.down {
  color: #6c5ce7;
  background: rgba(108, 92, 231, 0.10);
}

.kpi-value {
  margin-top: 18px;
  color: #202437;
  font-size: 25px;
  font-weight: 800;
}

.kpi-label {
  margin-top: 6px;
  color: #7c8498;
  font-size: 13px;
}

.mini-track {
  height: 7px;
  margin-top: 18px;
  border-radius: 999px;
  background: #eef0f7;
  overflow: hidden;

  span {
    display: block;
    height: 100%;
    border-radius: inherit;
    background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  }
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
  margin-top: 18px;
}

.span-2 {
  grid-column: span 2;
}

.chart-card,
.task-panel,
.calendar-panel,
.notes-panel {
  padding: 22px;
}

.card-header {
  margin-bottom: 16px;

  h2 {
    margin: 0;
    color: #202437;
    font-size: 16px;
    font-weight: 800;
  }

  p {
    margin: 6px 0 0;
    color: #8a91a6;
    font-size: 13px;
  }
}

.card-header.compact {
  margin-bottom: 14px;
}

.chart-box {
  height: 310px;
}

.task-list {
  display: grid;
  gap: 12px;
}

.task-item {
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(112, 115, 145, 0.10);
  border-radius: 14px;
  background: #fbfcff;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 12px 24px rgba(55, 62, 97, 0.08);
  }
}

.task-check {
  width: 10px;
  height: 34px;
  border-radius: 999px;
  background: #dfe3ee;
}

.task-check.urgent { background: #ff7675; }
.task-check.running { background: #6c5ce7; }

.task-copy {
  flex: 1;
  min-width: 0;

  strong {
    display: block;
    color: #252a3d;
    font-size: 14px;
  }

  span {
    display: block;
    margin-top: 4px;
    color: #8a91a6;
    font-size: 12px;
  }
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 7px;
}

.week-day {
  color: #9aa2b5;
  font-size: 11px;
  font-weight: 750;
  text-align: center;
}

.calendar-day {
  height: 34px;
  border: 0;
  border-radius: 11px;
  color: #596176;
  background: #f5f7fc;
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover {
    color: #6c5ce7;
    background: #f0edff;
    transform: translateY(-1px);
  }
}

.calendar-day.muted {
  color: #c1c7d5;
}

.calendar-day.event {
  box-shadow: inset 0 -3px 0 rgba(108, 92, 231, 0.30);
}

.calendar-day.active {
  color: #ffffff;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  box-shadow: 0 10px 20px rgba(108, 92, 231, 0.26);
}

.schedule-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.schedule-item {
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 12px;
  background: #fbfcff;

  span {
    color: #8a91a6;
    font-size: 12px;
  }

  strong {
    color: #33384c;
    font-size: 13px;
  }
}

.notes-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.note-card {
  min-height: 142px;
  padding: 16px;
  border-radius: 14px;
  background: linear-gradient(180deg, #fbfcff, #ffffff);
  border: 1px solid rgba(112, 115, 145, 0.10);

  span {
    display: inline-flex;
    padding: 4px 9px;
    border-radius: 999px;
    color: #6c5ce7;
    background: rgba(108, 92, 231, 0.10);
    font-size: 12px;
    font-weight: 750;
  }

  h3 {
    margin: 14px 0 8px;
    color: #252a3d;
    font-size: 15px;
  }

  p {
    margin: 0;
    color: #7c8498;
    font-size: 13px;
    line-height: 1.7;
  }
}

@media (max-width: 1280px) {
  .kpi-grid,
  .dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .span-2 {
    grid-column: span 2;
  }
}

@media (max-width: 768px) {
  .analytics-home {
    padding: 16px;
  }

  .hero-panel,
  .card-header,
  .kpi-top {
    align-items: flex-start;
    flex-direction: column;
  }

  .kpi-grid,
  .dashboard-grid,
  .notes-grid {
    grid-template-columns: 1fr;
  }

  .span-2 {
    grid-column: span 1;
  }

  .chart-box {
    height: 260px;
  }
}
</style>
