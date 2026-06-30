<template>
  <div class="home-dashboard">
    <section class="hero-card">
      <div class="hero-copy">
        <span class="hero-kicker">今日经营概览</span>
        <h1>首页工作台</h1>
        <p>集中查看订单、库存、采购、应付、费用和利润，快速发现需要运营和财务处理的问题。</p>
      </div>

      <div class="hero-visual" aria-hidden="true">
        <div class="cube-stack">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="float-card revenue">
          <small>本月净利润</small>
          <strong>¥284,910</strong>
          <em>较上月 +12.6%</em>
        </div>
        <div class="float-card warning">
          <small>库存预警</small>
          <strong>18 SKU</strong>
          <em>需要复核</em>
        </div>
      </div>
    </section>

    <section class="metrics-grid">
      <article v-for="item in kpis" :key="item.label" class="metric-card" :class="item.tone">
        <div class="metric-icon">
          <component :is="item.icon" />
        </div>
        <div class="metric-body">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <em :class="{ down: item.down }">{{ item.change }}</em>
        </div>
      </article>
    </section>

    <section class="content-grid">
      <article class="panel chart-panel span-2">
        <div class="panel-head">
          <div>
            <h2>销售额与利润趋势</h2>
            <p>最近 7 天销售额、净利润、费用走势</p>
          </div>
          <el-segmented v-model="trendRange" :options="['7天', '30天', '季度']" />
        </div>
        <div ref="lineChartRef" class="chart-box"></div>
      </article>

      <article class="panel channel-panel">
        <div class="panel-head">
          <div>
            <h2>渠道销售排行</h2>
            <p>按平台汇总销售额</p>
          </div>
        </div>
        <div ref="barChartRef" class="chart-box"></div>
      </article>




      <article class="panel notes-panel span-2">
        <div class="panel-head compact">
          <div>
            <h2>经营提醒</h2>
            <p>运营、财务、管理层共用的复盘摘要</p>
          </div>
          <el-button plain :icon="Plus">新增</el-button>
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
import { Calendar, Goods, Money, Plus, ShoppingCart, Tickets, TrendCharts, Wallet } from '@element-plus/icons-vue'

const trendRange = ref('7天')
const lineChartRef = ref(null)
const barChartRef = ref(null)
let lineChart
let barChart

const kpis = [
  { label: '销售额', value: '¥1,286,420', change: '+18.2%', icon: Money, tone: 'purple' },
  { label: '净利润', value: '¥284,910', change: '+12.6%', icon: Wallet, tone: 'blue' },
  { label: '订单数', value: '18,936', change: '+9.4%', icon: ShoppingCart, tone: 'green' },
  { label: '售后成本', value: '¥36,210', change: '-4.1%', icon: Tickets, tone: 'orange', down: true },
  { label: '库存预警', value: '18 SKU', change: '需处理', icon: Goods, tone: 'red' }
]

const tasks = [
  { title: '确认抖音 6 月结算单', desc: '2 个店铺待审核，差异集中在平台扣点', label: '今天', type: 'danger', status: 'urgent' },
  { title: '银行流水核销', desc: '13 条收款待匹配，需要补齐业务单据', label: '财务', type: 'warning', status: 'running' },
  { title: '费用单生成凭证', desc: '推广费与物流费待过账', label: '凭证', type: 'primary', status: 'running' },
  { title: '库存成本复核', desc: 'TOP SKU 成本波动超过阈值', label: '运营', type: 'info', status: 'idle' }
]

const weekDays = ['一', '二', '三', '四', '五', '六', '日']
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
  { time: '14:30', title: '投放 ROI 复盘' },
  { time: '16:00', title: '月结检查' }
]

const notes = [
  { tag: '利润', title: '利润率提升 2.3%', content: '佣金费率下降，广告 ROI 回升，净利率维持在 22% 以上。' },
  { tag: '库存', title: '3 个 SKU 进入预警', content: '建议优先处理周转天数超过 45 天的滞销 SKU，降低仓储占用。' },
  { tag: '财务', title: '对账差异收敛', content: '差异集中在平台扣点与支付手续费，需要补充费用映射规则。' }
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
      { name: '销售额', type: 'line', smooth: true, symbolSize: 8, areaStyle: { opacity: 0.12 }, data: [120, 148, 132, 176, 188, 214, 236] },
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
    setTimeout(resizeCharts, 320)
  })
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeCharts)
  lineChart?.dispose()
  barChart?.dispose()
})
</script>

<style scoped lang="scss">
.home-dashboard {
  width: 100%;
  min-width: 0;
  min-height: 100%;
  padding: 18px;
  overflow-x: hidden;
}

.hero-card,
.panel,
.metric-card {
  border: 1px solid rgba(255, 255, 255, 0.74);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 48px rgba(55, 62, 97, 0.10);
  backdrop-filter: blur(18px);
}

.hero-card {
  min-width: 0;
  min-height: 216px;
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(280px, 420px);
  gap: 20px;
  align-items: center;
  padding: 28px;
  border-radius: 24px;
  overflow: hidden;
  background:
    radial-gradient(circle at 82% 20%, rgba(162, 155, 254, 0.34), transparent 34%),
    linear-gradient(135deg, rgba(255, 255, 255, 0.94), rgba(255, 255, 255, 0.70));
}

.hero-copy {
  min-width: 0;
  position: relative;
  z-index: 1;
}

.hero-kicker {
  display: inline-flex;
  padding: 7px 12px;
  border-radius: 999px;
  color: #6c5ce7;
  background: rgba(108, 92, 231, 0.09);
  font-size: 12px;
  font-weight: 850;
}

h1 {
  margin: 16px 0 0;
  color: #202437;
  font-size: 36px;
  line-height: 1.15;
  font-weight: 900;
}

.hero-copy p {
  max-width: 650px;
  margin: 14px 0 0;
  color: #737d94;
  font-size: 15px;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.hero-visual {
  position: relative;
  z-index: 1;
  min-height: 190px;
}

.cube-stack {
  position: absolute;
  top: 42px;
  left: 120px;
  width: 150px;
  height: 130px;
  transform: rotate(-12deg);

  span {
    position: absolute;
    width: 88px;
    height: 88px;
    border-radius: 18px;
    background: linear-gradient(135deg, #6c5ce7, #a29bfe);
    box-shadow: 0 24px 46px rgba(108, 92, 231, 0.22);
    transform: rotate(45deg) skew(-12deg, -12deg);
  }

  span:nth-child(1) {
    left: 38px;
    top: 0;
  }

  span:nth-child(2) {
    left: 0;
    top: 54px;
    opacity: 0.78;
  }

  span:nth-child(3) {
    left: 76px;
    top: 54px;
    opacity: 0.58;
  }
}

.float-card {
  position: absolute;
  width: 156px;
  padding: 14px;
  border: 1px solid rgba(255, 255, 255, 0.78);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 18px 40px rgba(55, 62, 97, 0.12);
  backdrop-filter: blur(18px);

  small,
  em {
    display: block;
    color: #8a91a6;
    font-size: 12px;
    font-style: normal;
  }

  strong {
    display: block;
    margin: 7px 0;
    color: #202437;
    font-size: 20px;
    font-weight: 900;
  }

  em {
    color: #00a783;
    font-weight: 800;
  }
}

.float-card.revenue {
  top: 4px;
  right: 12px;
}

.float-card.warning {
  left: 0;
  bottom: 4px;

  em {
    color: #e65d6c;
  }
}

.metrics-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
  margin-top: 16px;
}

.metric-card {
  min-width: 0;
  display: flex;
  gap: 13px;
  align-items: center;
  padding: 17px;
  border-radius: 20px;
  transition: transform 0.22s ease, box-shadow 0.22s ease;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 24px 54px rgba(55, 62, 97, 0.13);
  }
}

.metric-body {
  min-width: 0;

  span,
  em {
    display: block;
    color: #8a91a6;
    font-size: 12px;
    font-style: normal;
  }

  strong {
    display: block;
    margin: 5px 0;
    overflow: hidden;
    color: #202437;
    font-size: 21px;
    font-weight: 900;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  em {
    color: #00a783;
    font-weight: 800;
  }

  em.down {
    color: #6c5ce7;
  }
}

.metric-icon {
  width: 46px;
  height: 46px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 46px;
  border-radius: 16px;
  color: #ffffff;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  box-shadow: 0 14px 26px rgba(108, 92, 231, 0.22);

  svg {
    width: 21px;
    height: 21px;
  }
}

.metric-card.blue .metric-icon {
  background: linear-gradient(135deg, #4c8dff, #8fb8ff);
}

.metric-card.green .metric-icon {
  background: linear-gradient(135deg, #00b894, #55efc4);
}

.metric-card.orange .metric-icon {
  background: linear-gradient(135deg, #fdcb6e, #ffeaa7);
  color: #7a4d00;
}

.metric-card.red .metric-icon {
  background: linear-gradient(135deg, #ff7675, #ffaaa5);
}

.content-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-top: 16px;
}

.span-2 {
  grid-column: span 2;
}

.panel {
  min-width: 0;
  padding: 20px;
  border-radius: 20px;
}

.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;

  h2 {
    margin: 0;
    color: #202437;
    font-size: 16px;
    font-weight: 900;
  }

  p {
    margin: 6px 0 0;
    color: #8a91a6;
    font-size: 13px;
  }
}

.panel-head.compact {
  margin-bottom: 14px;
}

.chart-box {
  height: 310px;
}

.task-list,
.schedule-list {
  display: grid;
  gap: 12px;
}

.task-item {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border: 1px solid rgba(112, 115, 145, 0.10);
  border-radius: 15px;
  background: #fbfcff;
}

.task-dot {
  width: 9px;
  height: 34px;
  flex: 0 0 9px;
  border-radius: 999px;
  background: #dfe3ee;
}

.task-dot.urgent {
  background: #ff7675;
}

.task-dot.running {
  background: #6c5ce7;
}

.task-main {
  flex: 1;
  min-width: 0;

  strong {
    display: block;
    overflow: hidden;
    color: #252a3d;
    font-size: 14px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    display: block;
    margin-top: 4px;
    overflow: hidden;
    color: #8a91a6;
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
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
  font-weight: 800;
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
  margin-top: 16px;
}

.schedule-item {
  display: flex;
  align-items: center;
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
  min-height: 132px;
  padding: 16px;
  border: 1px solid rgba(112, 115, 145, 0.10);
  border-radius: 15px;
  background: linear-gradient(180deg, #fbfcff, #ffffff);

  span {
    display: inline-flex;
    padding: 4px 9px;
    border-radius: 999px;
    color: #6c5ce7;
    background: rgba(108, 92, 231, 0.10);
    font-size: 12px;
    font-weight: 800;
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

@media (max-width: 1500px) {
  .metrics-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .content-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .hero-card {
    grid-template-columns: 1fr;
  }

  .hero-visual {
    display: none;
  }
}

@media (max-width: 900px) {
  .home-dashboard {
    padding: 14px;
  }

  .hero-card {
    padding: 22px;
    border-radius: 20px;
  }

  h1 {
    font-size: 30px;
  }

  .metrics-grid,
  .content-grid,
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
