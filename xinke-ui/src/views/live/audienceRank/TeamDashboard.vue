<template>
  <section class="team-page" v-loading="loading">
    <header class="team-head">
      <div><h3>管理中心</h3><p>{{ managerTab === 'execution' ? '先处理团队积压和同步异常，再查看转化表现' : '按直播间设置进客、分配和回收规则' }}</p></div>
      <div v-if="managerTab === 'execution'" class="filters"><el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" @change="load" /><el-select v-model="query.roomId" clearable filterable placeholder="全部直播间" @change="load"><el-option v-for="room in roomOptions" :key="room.roomId" :label="roomLabel(room)" :value="Number(room.roomId)" /></el-select><el-select v-model="query.ownerUserId" clearable filterable placeholder="全部领取人" @change="load"><el-option v-for="person in ownerOptions" :key="personId(person)" :label="personLabel(person)" :value="personId(person)" /></el-select></div>
    </header>

    <nav class="manager-tabs"><button :class="{ active: managerTab === 'execution' }" type="button" @click="managerTab = 'execution'">执行看板</button><button :class="{ active: managerTab === 'rules' }" type="button" @click="managerTab = 'rules'">进客与分配规则</button></nav>

    <template v-if="managerTab === 'execution'">

    <div class="metric-row">
      <button :class="{ danger: Number(overview.unassignedCustomers) > 0 }" type="button" @click="drill({ stage: 'UNASSIGNED' })"><span>待领取</span><strong>{{ integer(overview.unassignedCustomers) }}</strong><small>尚未分配负责人</small></button>
      <button :class="{ danger: Number(overview.overdueCustomers) > 0 }" type="button" @click="drill({ overdue: true })"><span>已逾期</span><strong>{{ integer(overview.overdueCustomers) }}</strong><small>超过计划时间</small></button>
      <button type="button" @click="drill({ contacted: true })"><span>今日已完成</span><strong>{{ integer(overview.todayContactedCustomers) }}</strong><small>{{ integer(overview.todayDueCustomers) }} 位仍待联系</small></button>
      <button class="success" type="button" @click="drill({ stage: 'ORDERED' })"><span>下单转化</span><strong>{{ percent(overview.orderedCustomers, overview.qualifiedCustomers) }}</strong><small>{{ integer(overview.orderedCustomers) }} 位已下单</small></button>
    </div>

    <section class="panel attention-panel">
      <header><div><h4>需要处理</h4><span>只显示需要管理员介入的问题</span></div></header>
      <div class="issue-list"><button v-for="item in issueRows" :key="item.key" :class="item.tone" type="button" @click="handleIssue(item)"><span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.hint }}</small><el-icon><ArrowRight /></el-icon></button></div>
    </section>

    <div class="manager-grid">
      <section class="panel owner-panel">
        <header><div><h4>人员负载</h4><span>优先关注逾期多、今日任务堆积的领取人</span></div></header>
        <el-table :data="ownerRows" row-key="ownerUserId" empty-text="暂无领取人数据">
          <el-table-column label="领取人" min-width="130"><template #default="{ row }"><strong>{{ row.ownerName }}</strong></template></el-table-column>
          <el-table-column label="负责" width="75" align="right"><template #default="{ row }"><el-button link @click="drill({ ownerUserId: row.ownerUserId })">{{ integer(row.assignedCount) }}</el-button></template></el-table-column>
          <el-table-column label="今日" width="75" align="right"><template #default="{ row }"><el-button link @click="drill({ ownerUserId: row.ownerUserId, todayDue: true })">{{ integer(row.todayDueCount) }}</el-button></template></el-table-column>
          <el-table-column label="高意向" width="85" align="right" prop="qualifiedCount" />
          <el-table-column label="下单" width="70" align="right" prop="orderedCount" />
          <el-table-column label="逾期" width="70" align="right"><template #default="{ row }"><el-button v-if="Number(row.overdueCount)" link type="danger" @click="drill({ ownerUserId: row.ownerUserId, overdue: true })">{{ integer(row.overdueCount) }}</el-button><span v-else class="healthy">0</span></template></el-table-column>
        </el-table>
      </section>

      <section class="panel funnel-panel">
        <header><div><h4>客户转化</h4><span>从进入待办开始计算，不包含 {{ integer(overview.observingCustomers) }} 位观察客户</span></div></header>
        <div class="funnel-list"><button v-for="item in funnelRows" :key="item.stageCode" type="button" @click="drill(funnelFilter(item.stageCode))"><span>{{ funnelLabel(item.stageCode) }}</span><i><b :style="{ width: `${funnelWidth(item.totalCount)}%` }"></b></i><strong>{{ integer(item.totalCount) }}</strong><small>{{ funnelRate(item.totalCount) }}</small></button></div>
      </section>
    </div>

    <section class="panel room-panel">
      <header><div><h4>直播间客户质量</h4><span>比较客户沉淀、重复到访和下单表现</span></div></header>
      <el-table :data="roomRows" row-key="roomId" empty-text="暂无直播间数据">
        <el-table-column label="直播间" min-width="220"><template #default="{ row }"><strong>{{ row.roomName || '未匹配直播间' }}</strong></template></el-table-column>
        <el-table-column label="沉淀客户" min-width="105" align="right"><template #default="{ row }"><el-button link @click="drill({ roomId: row.roomId })">{{ integer(row.customerCount) }}</el-button></template></el-table-column>
        <el-table-column label="重复到访" min-width="105" align="right"><template #default="{ row }"><el-button link @click="drill({ roomId: row.roomId, repeatVisit: true })">{{ integer(row.repeatVisitCount) }}</el-button></template></el-table-column>
        <el-table-column label="高意向" min-width="95" align="right" prop="highIntentCount" /><el-table-column label="已下单" min-width="95" align="right" prop="orderedCount" />
        <el-table-column label="下单转化" min-width="110" align="right"><template #default="{ row }">{{ percent(row.orderedCount, row.customerCount) }}</template></el-table-column>
      </el-table>
    </section>
    </template>

    <AssignmentRules v-else ref="rulesRef" class="rules-panel" :room-options="roomOptions" :owner-options="ownerOptions" @changed="load" />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ArrowRight } from '@element-plus/icons-vue'
import { getAudienceTeamDashboard } from '@/api/live/audienceRank'
import AssignmentRules from './AssignmentRules.vue'

const props = defineProps({ roomOptions: { type: Array, default: () => [] }, ownerOptions: { type: Array, default: () => [] }, syncSummary: { type: Object, default: () => ({}) } })
const emit = defineEmits(['drilldown', 'open-sync'])
const loading = ref(false), managerTab = ref('execution'), rulesRef = ref(null), dateRange = ref([]), overview = ref({}), funnelRows = ref([]), ownerRows = ref([]), roomRows = ref([])
const query = ref({ roomId: null, ownerUserId: null })
const qualifiedCount = computed(() => Number(funnelRows.value.find(item => item.stageCode === 'QUALIFIED')?.totalCount || 0))
const funnelMax = computed(() => Math.max(1, ...funnelRows.value.map(item => Number(item.totalCount || 0))))
const issueRows = computed(() => {
  const rows = []
  if (Number(overview.value.overdueCustomers || 0) > 0) rows.push({ key: 'overdue', label: '逾期客户', value: `${integer(overview.value.overdueCustomers)} 位`, hint: '已经超过计划联系时间', filter: { overdue: true }, tone: 'danger' })
  if (Number(overview.value.unassignedCustomers || 0) > 0) rows.push({ key: 'unassigned', label: '等待领取', value: `${integer(overview.value.unassignedCustomers)} 位`, hint: '尚未分配负责人', filter: { stage: 'UNASSIGNED' }, tone: 'warning' })
  if (Number(props.syncSummary?.unmatchedBatchCount || 0) > 0) rows.push({ key: 'sync', label: '同步异常', value: `${integer(props.syncSummary.unmatchedBatchCount)} 批`, hint: '需要确认直播间归属', action: 'sync', tone: 'warning' })
  return rows.length ? rows : [{ key: 'healthy', label: '当前无需处理', value: '运行正常', hint: '没有逾期、待领取或同步异常', tone: 'healthy' }]
})
onMounted(load)
async function load() { loading.value = true; try { const params = { ...query.value }; if (dateRange.value?.length === 2) [params.beginDate, params.endDate] = dateRange.value; const data = (await getAudienceTeamDashboard(params))?.data || {}; overview.value = data.overview || {}; funnelRows.value = data.funnel || []; ownerRows.value = data.owners || []; roomRows.value = data.rooms || []; if (!dateRange.value.length && data.beginDate && data.endDate) dateRange.value = [data.beginDate, data.endDate] } finally { loading.value = false } }
function drill(filters) { emit('drilldown', { ...filters, beginDate: dateRange.value?.[0], endDate: dateRange.value?.[1] }) }
function handleIssue(item) { if (item.action === 'sync') emit('open-sync'); else if (item.filter) drill(item.filter) }
function integer(value) { return Number(value || 0).toLocaleString('zh-CN') }
function percent(value, total) { return `${total ? (Number(value || 0) * 100 / Number(total)).toFixed(1) : '0.0'}%` }
function funnelWidth(value) { return Math.max(4, Number(value || 0) * 100 / funnelMax.value) }
function funnelRate(value) { return percent(value, overview.value.qualifiedCustomers) }
function funnelLabel(code) { return ({ NEW: '进入待办', CLAIMED: '已领取', CONTACTED: '已联系', QUALIFIED: '有意向', ORDERED: '已下单' })[code] || code }
function funnelFilter(code) { return ({ NEW: { qualified: true }, CLAIMED: { claimed: true }, CONTACTED: { contacted: true }, QUALIFIED: { intentLevel: 'HIGH' }, ORDERED: { stage: 'ORDERED' } })[code] || {} }
function roomLabel(room) { return room.roomName || room.liveAccount || room.roomCode || `直播间 ${room.roomId}` }
function personId(person) { return Number(person.userId ?? person.user_id ?? person.id) }
function personLabel(person) { const name = person.userName || person.nickName || person.name || person.account || '未命名账号'; const account = person.account || person.mobile || ''; return account && account !== name ? `${name}（${account}）` : name }
defineExpose({ load })
</script>

<style scoped lang="scss">
.team-page {
  padding: 0 17px 28px;
  border: 1px solid #e3e7eb;
  color: #202631;
  background: #fff;
}
.team-head {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid #e7eaee;
}
.team-head h3,
.panel h4 { margin: 0; letter-spacing: 0; }
.team-head h3 { font-size: 17px; font-weight: 650; }
.team-head p { margin: 5px 0 0; color: #747e8c; font-size: 12px; }
.filters { display: flex; gap: 8px; }
.filters :deep(.el-date-editor) { width: 250px; }
.filters :deep(.el-select) { width: 150px; }
.manager-tabs { display: flex; gap: 24px; border-bottom: 1px solid #e7eaee; }
.manager-tabs button { position: relative; height: 46px; padding: 0 1px; border: 0; color: #697586; background: transparent; cursor: pointer; }
.manager-tabs button.active { color: #202631; font-weight: 650; }
.manager-tabs button.active::after { position: absolute; right: 0; bottom: -1px; left: 0; height: 2px; background: #ed6a2c; content: ''; }
.rules-panel { margin-top: 0; border-top: 0; }
.metric-row { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin: 14px 0 20px; border: 1px solid #e4e7ec; border-radius: 5px; }
.metric-row button { min-width: 0; padding: 14px 16px; border: 0; border-right: 1px solid #edf0f3; background: #fff; text-align: left; cursor: pointer; }
.metric-row button:last-child { border-right: 0; }
.metric-row button:hover { background: #fafbfc; }
.metric-row span,
.metric-row small { display: block; overflow: hidden; color: #747e8c; text-overflow: ellipsis; white-space: nowrap; }
.metric-row span { font-size: 12px; }
.metric-row strong { display: block; margin: 5px 0 3px; font-size: 22px; font-weight: 650; }
.metric-row small { font-size: 10px; }
.metric-row .danger strong { color: #b42318; }
.metric-row .success strong { color: #16794b; }
.panel { min-width: 0; }
.panel > header { display: flex; min-height: 49px; align-items: center; border-bottom: 1px solid #e7eaee; }
.panel h4 { font-size: 14px; }
.panel header span { display: block; margin-top: 4px; color: #8992a0; font-size: 11px; }
.panel :deep(.el-table__header th) { height: 42px; color: #596474; background: #f7f8fa; font-weight: 600; }
.panel :deep(.el-table__row td) { padding: 9px 0; }
.panel :deep(.el-button.is-link) { padding: 0; }
.healthy { color: #27845a; }
.attention-panel { margin-bottom: 20px; }
.attention-panel > header { min-height: 45px; }
.issue-list { display: grid; grid-template-columns: repeat(auto-fit, minmax(260px, 1fr)); border-bottom: 1px solid #e7eaee; }
.issue-list > button { display: grid; min-width: 0; min-height: 62px; grid-template-columns: minmax(0, 1fr) auto 18px; align-items: center; gap: 4px 8px; padding: 9px 14px; border: 0; border-right: 1px solid #edf0f3; color: #344054; background: #fff; cursor: pointer; text-align: left; }
.issue-list > button:first-child { padding-left: 2px; }
.issue-list > button:last-child { border-right: 0; }
.issue-list > button:hover { background: #fafbfd; }
.issue-list > button span,
.issue-list > button small { grid-column: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.issue-list > button strong { grid-row: 1 / span 2; grid-column: 2; font-size: 13px; }
.issue-list > button small { color: #8992a0; font-size: 10px; }
.issue-list > button :deep(.el-icon) { grid-row: 1 / span 2; grid-column: 3; color: #98a2b3; }
.issue-list > button.danger strong { color: #b42318; }
.issue-list > button.warning strong { color: #9a620f; }
.issue-list > button.healthy { cursor: default; }
.issue-list > button.healthy strong { color: #16794b; }
.issue-list > button.healthy :deep(.el-icon) { display: none; }
.manager-grid { display: grid; grid-template-columns: minmax(520px, 1.15fr) minmax(340px, .85fr); gap: 28px; }
.funnel-list button { display: grid; width: 100%; min-height: 51px; grid-template-columns: 72px minmax(80px, 1fr) 48px 46px; align-items: center; gap: 9px; padding: 0 2px; border: 0; border-bottom: 1px solid #edf0f3; background: #fff; cursor: pointer; text-align: left; }
.funnel-list button:hover { background: #fafbfd; }
.funnel-list button > span { color: #596575; font-size: 12px; }
.funnel-list i { height: 7px; border-radius: 3px; background: #edf1f5; }
.funnel-list i b { display: block; height: 100%; border-radius: 3px; background: #4c7da9; }
.funnel-list strong,
.funnel-list small { text-align: right; }
.funnel-list small { color: #8992a0; }
.room-panel { margin-top: 24px; }
@media (max-width: 1100px) {
  .team-head { align-items: flex-start; flex-direction: column; padding: 14px 0; }
  .filters { width: 100%; flex-wrap: wrap; }
  .manager-grid { grid-template-columns: 1fr; gap: 22px; }
}
@media (max-width: 760px) {
  .team-page { padding: 0 10px 20px; }
  .filters :deep(.el-date-editor),
  .filters :deep(.el-select) { width: 100%; }
  .metric-row { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .metric-row button:nth-child(2) { border-right: 0; }
  .metric-row button:nth-child(-n+2) { border-bottom: 1px solid #edf0f3; }
  .metric-row button { padding: 13px 11px; }
  .issue-list { grid-template-columns: 1fr; }
  .issue-list > button,
  .issue-list > button:first-child { padding: 9px 4px; border-right: 0; border-bottom: 1px solid #edf0f3; }
  .issue-list > button:last-child { border-bottom: 0; }
  .owner-panel { overflow-x: auto; }
}
</style>
