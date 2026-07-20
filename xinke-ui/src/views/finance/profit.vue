<template>
  <div class="finance-center" v-loading="overviewLoading">
    <section class="finance-hero">
      <div>
        <span class="eyebrow">FINANCE OPERATIONS</span>
        <h1>财务运营中心</h1>
        <p>先处理资金和单据待办，再核对利润与关账。每个数字都可以进入对应业务页面。</p>
      </div>
      <div class="hero-actions">
        <el-tag round effect="plain">会计期间 {{ summary.currentPeriod || '-' }}</el-tag>
        <el-button icon="Refresh" :loading="overviewLoading" @click="loadOverview">刷新数据</el-button>
      </div>
    </section>

    <el-tabs v-model="activeView" class="finance-tabs" @tab-change="handleViewChange">
      <el-tab-pane label="财务工作台" name="overview">
        <section class="metric-grid">
          <button class="metric-card receivable" @click="go('/finance/receivable')">
            <span>待收款</span><strong>{{ money(summary.openReceivableAmount) }}</strong>
            <small :class="{ danger: number(summary.overdueReceivableAmount) > 0 }">逾期 {{ money(summary.overdueReceivableAmount) }}</small>
          </button>
          <button class="metric-card payable" @click="go('/finance/payable')">
            <span>待付款</span><strong>{{ money(summary.openPayableAmount) }}</strong>
            <small :class="{ danger: number(summary.overduePayableAmount) > 0 }">逾期 {{ money(summary.overduePayableAmount) }}</small>
          </button>
          <button class="metric-card inflow" @click="go('/finance/cashflow')">
            <span>本月流入</span><strong>{{ money(summary.monthInflowAmount) }}</strong>
            <small>以已入账资金流水为准</small>
          </button>
          <button class="metric-card outflow" @click="go('/finance/cashflow')">
            <span>本月流出</span><strong>{{ money(summary.monthOutflowAmount) }}</strong>
            <small>净现金 {{ money(monthNetCash) }}</small>
          </button>
        </section>

        <section class="workspace-grid">
          <article class="panel cash-panel">
            <div class="panel-heading">
              <div><h2>资金趋势</h2><p>最近六个月已入账的实际收付款</p></div>
              <el-button link type="primary" @click="go('/finance/cashflow')">查看资金流水</el-button>
            </div>
            <div ref="cashChartRef" class="cash-chart"></div>
          </article>

          <article class="panel todo-panel">
            <div class="panel-heading"><div><h2>本期待办</h2><p>按财务风险和处理顺序排列</p></div></div>
            <button v-for="item in todoItems" :key="item.key" class="todo-row" @click="go(item.path)">
              <span class="todo-icon" :class="item.level"><el-icon><component :is="item.icon" /></el-icon></span>
              <span class="todo-content"><strong>{{ item.title }}</strong><small>{{ item.description }}</small></span>
              <b :class="item.level">{{ item.value }}</b>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </article>
        </section>

        <section class="workflow-panel panel">
          <div class="panel-heading"><div><h2>财务处理顺序</h2><p>单据必须按顺序流转，系统会阻止跳过审核、核销或过账</p></div></div>
          <div class="workflow-grid">
            <button v-for="(step, index) in workflowSteps" :key="step.path" class="workflow-step" @click="go(step.path)">
              <span class="step-index">{{ index + 1 }}</span>
              <div><strong>{{ step.title }}</strong><small>{{ step.description }}</small></div>
              <el-icon><ArrowRight /></el-icon>
            </button>
          </div>
        </section>

        <section class="bottom-grid">
          <article class="panel module-panel">
            <div class="panel-heading"><div><h2>业务模块</h2><p>按工作目的进入，不需要记财务术语</p></div></div>
            <div class="module-grid">
              <button v-for="module in moduleCards" :key="module.path" class="module-card" @click="go(module.path)">
                <el-icon><component :is="module.icon" /></el-icon>
                <span><strong>{{ module.title }}</strong><small>{{ module.description }}</small></span>
              </button>
            </div>
          </article>

          <article class="panel activity-panel">
            <div class="panel-heading"><div><h2>最近财务动作</h2><p>快速确认刚才的操作是否已经落账</p></div></div>
            <el-empty v-if="!activities.length" description="暂无财务操作" :image-size="64" />
            <button v-for="row in activities" :key="`${row.activityType}-${row.bizNo}`" class="activity-row" @click="go(activityPath(row.activityType))">
              <span><strong>{{ row.title }}</strong><small>{{ row.bizNo }} · {{ formatTime(row.businessTime) }}</small></span>
              <span class="activity-value"><b>{{ money(row.amount) }}</b><el-tag size="small" round :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag></span>
            </button>
          </article>
        </section>
      </el-tab-pane>

      <el-tab-pane label="经营利润" name="profit">
        <section class="panel report-panel">
          <div class="report-toolbar">
            <el-radio-group v-model="reportType" @change="getProfitList">
              <el-radio-button value="operatorDaily">运营日报</el-radio-button>
              <el-radio-button value="operatorMonthly">运营月报</el-radio-button>
              <el-radio-button value="shopDaily">店铺日报</el-radio-button>
              <el-radio-button value="shopMonthly">店铺月报</el-radio-button>
            </el-radio-group>
            <el-form :model="queryParams" ref="queryRef" :inline="true">
              <el-form-item v-if="isOperatorReport" prop="operatorName"><el-input v-model="queryParams.operatorName" placeholder="运营姓名" clearable /></el-form-item>
              <el-form-item v-if="isOperatorReport" prop="channelCode"><el-input v-model="queryParams.channelCode" placeholder="流量渠道" clearable /></el-form-item>
              <el-form-item prop="shopId"><el-input v-model="queryParams.shopId" placeholder="店铺ID" clearable /></el-form-item>
              <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
            </el-form>
          </div>
          <el-table v-loading="profitLoading" :data="profitList">
            <el-table-column label="日期" prop="dt" width="110" />
            <el-table-column v-if="isOperatorReport" label="运营" prop="operatorName" min-width="110" />
            <el-table-column v-if="isOperatorReport" label="渠道" min-width="120"><template #default="{ row }">{{ row.channelName || row.channelCode || '-' }}</template></el-table-column>
            <el-table-column label="店铺" prop="shopName" min-width="130" show-overflow-tooltip />
            <el-table-column label="GMV" align="right"><template #default="{ row }">{{ money(row.gmv) }}</template></el-table-column>
            <el-table-column label="净成交额" align="right"><template #default="{ row }">{{ money(row.netAmount) }}</template></el-table-column>
            <el-table-column label="商品成本" align="right"><template #default="{ row }">{{ money(row.productCost) }}</template></el-table-column>
            <el-table-column label="平台费用" align="right"><template #default="{ row }">{{ money(row.platformFee) }}</template></el-table-column>
            <el-table-column label="推广费" align="right"><template #default="{ row }">{{ money(row.adCost) }}</template></el-table-column>
            <el-table-column label="售后成本" align="right"><template #default="{ row }">{{ money(row.afterSaleCost) }}</template></el-table-column>
            <el-table-column label="利润" align="right"><template #default="{ row }"><strong :class="{ danger: number(row.profitAmount) < 0 }">{{ money(row.profitAmount) }}</strong></template></el-table-column>
            <el-table-column label="ROI" prop="roi" width="90" align="right" />
          </el-table>
          <pagination v-show="profitTotal > 0" :total="profitTotal" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getProfitList" />
        </section>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="FinanceProfit">
import * as echarts from 'echarts'
import { getFinanceDashboardOverview, listDailyProfit, listMonthlyProfit, listOperatorDailyProfit, listOperatorMonthlyProfit } from '@/api/finance/profit'

const { proxy } = getCurrentInstance()
const router = useRouter()
const activeView = ref('overview')
const overviewLoading = ref(false)
const summary = ref({})
const cashTrend = ref([])
const activities = ref([])
const cashChartRef = ref()
let cashChart

const profitLoading = ref(false)
const profitList = ref([])
const profitTotal = ref(0)
const reportType = ref('operatorDaily')
const queryParams = ref({ pageNum: 1, pageSize: 10, shopId: undefined, operatorName: undefined, channelCode: undefined })
const isOperatorReport = computed(() => reportType.value.startsWith('operator'))
const monthNetCash = computed(() => number(summary.value.monthInflowAmount) - number(summary.value.monthOutflowAmount))

const requestMap = { operatorDaily: listOperatorDailyProfit, operatorMonthly: listOperatorMonthlyProfit, shopDaily: listDailyProfit, shopMonthly: listMonthlyProfit }
const workflowSteps = [
  { title: '确认业务单据', description: '平台结算、供应商发票和费用先审核', path: '/finance/settlement' },
  { title: '确认应收应付', description: '检查账期、金额和往来方', path: '/finance/payable' },
  { title: '登记资金流水', description: '银行或平台收付款先入账', path: '/finance/cashflow' },
  { title: '核销与对账', description: '用真实流水冲减应收应付', path: '/finance/reconcile' },
  { title: '凭证与月结', description: '凭证过账后执行月结检查', path: '/finance/voucher' }
]
const moduleCards = [
  { title: '平台结算', description: '核对平台收入、退款和扣费', path: '/finance/settlement', icon: 'DocumentChecked' },
  { title: '应收账款', description: '查看平台还欠公司多少钱', path: '/finance/receivable', icon: 'Wallet' },
  { title: '应付与付款', description: '供应商发票、付款审批与核销', path: '/finance/payable', icon: 'CreditCard' },
  { title: '资金流水', description: '登记银行和平台实际收付款', path: '/finance/cashflow', icon: 'Money' },
  { title: '费用管理', description: '推广、物流和日常费用归集', path: '/finance/expense', icon: 'Tickets' },
  { title: '凭证管理', description: '检查借贷分录并执行过账', path: '/finance/voucher', icon: 'Notebook' },
  { title: '账簿中心', description: '查询明细账并检查试算平衡', path: '/finance/ledger', icon: 'Collection' },
  { title: '对账差异', description: '找出单据金额与实际到账差异', path: '/finance/reconcile', icon: 'ScaleToOriginal' },
  { title: '月结管理', description: '检查未处理事项并关闭期间', path: '/finance/close', icon: 'Lock' }
]
const todoItems = computed(() => [
  { key: 'payments', title: '付款申请待处理', description: `待审批或待执行金额 ${money(summary.value.pendingPaymentAmount)}`, value: `${number(summary.value.pendingPaymentCount)} 笔`, path: '/finance/payable', icon: 'CreditCard', level: number(summary.value.pendingPaymentCount) ? 'danger' : 'normal' },
  { key: 'cash', title: '资金流水待匹配', description: '已入账但尚未核销到应收应付', value: `${number(summary.value.unmatchedCashCount)} 笔`, path: '/finance/cashflow', icon: 'Link', level: number(summary.value.unmatchedCashCount) ? 'warning' : 'normal' },
  { key: 'settlement', title: '平台结算待确认', description: '确认后生成应收账款', value: `${number(summary.value.draftSettlementCount)} 笔`, path: '/finance/settlement', icon: 'DocumentChecked', level: number(summary.value.draftSettlementCount) ? 'warning' : 'normal' },
  { key: 'expense', title: '费用单待审核', description: '审核后形成应付并允许生成凭证', value: `${number(summary.value.draftExpenseCount)} 笔`, path: '/finance/expense', icon: 'Tickets', level: number(summary.value.draftExpenseCount) ? 'warning' : 'normal' },
  { key: 'voucher', title: '会计凭证待过账', description: '过账前不会进入正式账簿', value: `${number(summary.value.draftVoucherCount)} 张`, path: '/finance/voucher', icon: 'Notebook', level: number(summary.value.draftVoucherCount) ? 'danger' : 'normal' }
])

async function loadOverview() {
  overviewLoading.value = true
  try {
    const response = await getFinanceDashboardOverview()
    summary.value = response.data?.summary || {}
    cashTrend.value = response.data?.cashTrend || []
    activities.value = response.data?.recentActivities || []
    await nextTick()
    renderCashChart()
  } finally { overviewLoading.value = false }
}

function renderCashChart() {
  if (!cashChartRef.value) return
  cashChart ||= echarts.init(cashChartRef.value)
  cashChart.setOption({
    tooltip: { trigger: 'axis', valueFormatter: value => money(value) },
    legend: { right: 6, top: 0, data: ['资金流入', '资金流出'] },
    grid: { left: 12, right: 12, top: 42, bottom: 8, containLabel: true },
    xAxis: { type: 'category', data: cashTrend.value.map(item => item.periodCode), axisLine: { lineStyle: { color: '#dfe3f1' } }, axisTick: { show: false } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#eef0f7' } }, axisLabel: { formatter: value => value >= 10000 ? `${value / 10000}万` : value } },
    series: [
      { name: '资金流入', type: 'bar', barMaxWidth: 30, data: cashTrend.value.map(item => item.inflowAmount), itemStyle: { color: '#28c99a', borderRadius: [5, 5, 0, 0] } },
      { name: '资金流出', type: 'bar', barMaxWidth: 30, data: cashTrend.value.map(item => item.outflowAmount), itemStyle: { color: '#7a6cf5', borderRadius: [5, 5, 0, 0] } }
    ]
  }, true)
}

function handleViewChange(name) { if (name === 'profit' && !profitList.value.length) getProfitList() }
function getProfitList() {
  profitLoading.value = true
  requestMap[reportType.value](queryParams.value).then(response => { profitList.value = response.rows || []; profitTotal.value = response.total || 0 }).finally(() => { profitLoading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getProfitList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function go(path) { router.push(path) }
function number(value) { return Number(value || 0) }
function money(value) { return `¥${number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-' }
function activityPath(type) { return ({ cash_flow: '/finance/cashflow', expense: '/finance/expense', payment: '/finance/payable', voucher: '/finance/voucher' })[type] || '/finance/profit' }
function statusLabel(value) { return ({ draft: '草稿', submitted: '待审批', approved: '已审核', partially_paid: '部分付款', paid: '已完成', posted: '已入账', reversed: '已冲销', voided: '已作废', rejected: '已驳回', completed: '已完成' })[value] || value || '未知' }
function statusType(value) { return ({ draft: 'info', submitted: 'warning', approved: 'primary', partially_paid: 'warning', paid: 'success', posted: 'success', reversed: 'warning', voided: 'info', rejected: 'danger', completed: 'success' })[value] || 'info' }
function resizeChart() { cashChart?.resize() }

onMounted(() => { loadOverview(); window.addEventListener('resize', resizeChart) })
onBeforeUnmount(() => { window.removeEventListener('resize', resizeChart); cashChart?.dispose() })
</script>

<style scoped>
.finance-center { min-height: calc(100vh - 84px); padding: 20px; background: #f4f6fb; color: #20263a; }
.finance-hero { display: flex; align-items: center; justify-content: space-between; gap: 20px; padding: 26px 30px; background: linear-gradient(120deg, #fff 55%, #eeebff); border: 1px solid #ebeaf6; border-radius: 16px; }
.eyebrow { display: inline-block; padding: 5px 10px; color: #6657e8; background: #f0edff; border-radius: 999px; font-size: 12px; font-weight: 700; }
.finance-hero h1 { margin: 10px 0 4px; font-size: 28px; }
.finance-hero p, .panel-heading p { margin: 0; color: #7b849d; }
.hero-actions { display: flex; align-items: center; gap: 10px; }
.finance-tabs { margin-top: 14px; }
.finance-tabs :deep(.el-tabs__header) { margin-bottom: 14px; padding: 0 4px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; }
.metric-card { min-width: 0; padding: 18px 20px; text-align: left; border: 1px solid #e8eaf3; border-radius: 14px; background: #fff; cursor: pointer; transition: transform .2s, box-shadow .2s; }
.metric-card:hover, .module-card:hover, .workflow-step:hover { transform: translateY(-2px); box-shadow: 0 12px 28px rgba(44, 50, 80, .09); }
.metric-card span, .metric-card small { display: block; color: #7b849d; }
.metric-card strong { display: block; margin: 8px 0 5px; font-size: 25px; }
.metric-card.receivable { border-top: 3px solid #28c99a; }.metric-card.payable { border-top: 3px solid #ff8d72; }.metric-card.inflow { border-top: 3px solid #5b9cff; }.metric-card.outflow { border-top: 3px solid #7a6cf5; }
.panel { border: 1px solid #e8eaf3; border-radius: 14px; background: #fff; }
.workspace-grid { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(320px, .75fr); gap: 14px; margin-top: 14px; }
.panel-heading { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 18px 20px 10px; }
.panel-heading h2 { margin: 0 0 4px; font-size: 17px; }.panel-heading p { font-size: 13px; }
.cash-chart { height: 300px; padding: 0 12px 10px; }
.todo-panel { padding-bottom: 10px; }
.todo-row, .activity-row { width: calc(100% - 24px); margin: 0 12px; padding: 12px 8px; display: flex; align-items: center; gap: 10px; text-align: left; border: 0; border-bottom: 1px solid #f0f1f6; background: transparent; cursor: pointer; }
.todo-row:last-child, .activity-row:last-child { border-bottom: 0; }
.todo-icon { width: 34px; height: 34px; display: grid; place-items: center; color: #6657e8; background: #f0edff; border-radius: 9px; }.todo-icon.warning { color: #d98916; background: #fff4dc; }.todo-icon.danger { color: #e95e71; background: #ffedf0; }
.todo-content, .activity-row > span:first-child { flex: 1; min-width: 0; }.todo-content strong, .todo-content small, .activity-row strong, .activity-row small { display: block; }.todo-content small, .activity-row small { margin-top: 3px; color: #8a92a8; font-size: 12px; }
.todo-row b { font-size: 14px; color: #596177; }.todo-row b.warning { color: #d98916; }.todo-row b.danger, .danger { color: #e24c62 !important; }
.workflow-panel { margin-top: 14px; padding-bottom: 18px; }.workflow-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 10px; padding: 4px 18px; }
.workflow-step { display: flex; align-items: center; gap: 10px; min-width: 0; padding: 14px; text-align: left; border: 1px solid #ececf4; border-radius: 10px; background: #fbfbfe; cursor: pointer; transition: .2s; }.workflow-step div { flex: 1; min-width: 0; }.workflow-step strong, .workflow-step small { display: block; }.workflow-step small { margin-top: 4px; color: #8991a6; font-size: 12px; line-height: 1.45; }.step-index { flex: 0 0 26px; height: 26px; display: grid; place-items: center; color: #fff; background: #7567ef; border-radius: 8px; font-weight: 700; }
.bottom-grid { display: grid; grid-template-columns: minmax(0, 1.25fr) minmax(360px, .75fr); gap: 14px; margin-top: 14px; }.module-panel, .activity-panel { padding-bottom: 14px; }
.module-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 10px; padding: 4px 18px; }.module-card { display: flex; gap: 10px; align-items: flex-start; min-width: 0; padding: 13px; text-align: left; border: 1px solid #ececf4; border-radius: 10px; background: #fff; cursor: pointer; transition: .2s; }.module-card > .el-icon { flex: 0 0 auto; margin-top: 2px; color: #6c5ce7; font-size: 20px; }.module-card strong, .module-card small { display: block; }.module-card small { margin-top: 4px; color: #8a92a8; font-size: 12px; line-height: 1.4; }
.activity-value { flex: 0 0 auto; text-align: right; }.activity-value b { display: block; margin-bottom: 5px; }
.report-panel { padding: 18px; }.report-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 14px; }.report-toolbar .el-form-item { margin-bottom: 0; }
@media (max-width: 1300px) { .workflow-grid { grid-template-columns: repeat(3, 1fr); }.module-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 1000px) { .metric-grid { grid-template-columns: repeat(2, 1fr); }.workspace-grid, .bottom-grid { grid-template-columns: 1fr; }.report-toolbar { align-items: stretch; flex-direction: column; } }
@media (max-width: 640px) { .finance-center { padding: 12px; }.finance-hero { align-items: flex-start; flex-direction: column; padding: 20px; }.metric-grid, .workflow-grid, .module-grid { grid-template-columns: 1fr; }.hero-actions { width: 100%; justify-content: space-between; } }
</style>
