<template>
  <div class="app-container freight-page">
    <header class="page-head">
      <div><h2>运费征收明细</h2><span>共 {{ total }} 条 · 运费合计 ¥ {{ money(summaryAmount) }}</span></div>
      <div class="head-actions">
        <el-button v-if="capabilities.import" :icon="Upload" :loading="importing" @click="fileInput?.click()">导入 Excel</el-button>
        <input ref="fileInput" type="file" accept=".xlsx,.xls" hidden @change="importFile" />
        <el-button v-if="capabilities.export" :icon="Download" :loading="exporting" :disabled="!total" @click="exportRows">导出 CSV</el-button>
        <el-button v-if="capabilities.create" type="primary" :icon="Plus" @click="openCreate">新增明细</el-button>
      </div>
    </header>

    <el-tabs v-model="tab" class="view-tabs" @tab-change="search"><el-tab-pane label="明细" name="detail" /><el-tab-pane v-if="capabilities.dashboard" label="看板" name="dashboard" /></el-tabs>

    <el-form class="filters" inline @submit.prevent="search">
      <el-form-item label="录入日期"><el-date-picker v-model="filters.dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" :clearable="false" /></el-form-item>
      <el-form-item label="店铺"><el-select v-model="filters.shopId" filterable clearable placeholder="全部店铺"><el-option v-for="shop in shops" :key="shop.shopId" :label="shop.shopName" :value="shop.shopId" /></el-select></el-form-item>
      <el-form-item label="订单状态"><el-select v-model="filters.deliveryStatus" clearable placeholder="全部状态"><el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="划扣方式"><el-select v-model="filters.deductionMethod" clearable placeholder="全部方式"><el-option v-for="item in methods" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item><el-input v-model.trim="filters.orderNo" clearable maxlength="64" placeholder="搜索订单号" @keyup.enter="search" /></el-form-item>
      <el-form-item><el-input v-model.trim="filters.creatorName" clearable maxlength="100" placeholder="搜索录入人" @keyup.enter="search" /></el-form-item>
      <el-form-item><el-button type="primary" :icon="Search" :loading="loading" @click="search">查询</el-button><el-button :icon="Refresh" @click="resetFilters">重置</el-button></el-form-item>
    </el-form>

    <div v-if="tab === 'dashboard'" v-loading="loading" class="dashboard">
      <div class="metrics">
        <div><span>征收订单</span><strong>{{ Number(dashboard.total || 0).toLocaleString() }}</strong></div>
        <div><span>运费合计</span><strong>¥ {{ money(dashboard.freightAmount) }}</strong></div>
        <div><span>已签收</span><strong>{{ Number(dashboard.signedCount || 0).toLocaleString() }}</strong></div>
        <div><span>已发货未签收</span><strong>{{ Number(dashboard.unsignedCount || 0).toLocaleString() }}</strong></div>
      </div>
      <section class="dashboard-section"><div class="section-head"><h3>每日征收</h3><span>按录入日期</span></div><div v-if="Number(dashboard.total)" ref="trendChart" class="trend-chart" /><el-empty v-else description="当前筛选范围没有明细" :image-size="64" /></section>
      <section class="dashboard-section"><div class="section-head"><h3>划扣方式</h3><span>按运费金额</span></div>
        <div class="method-lines"><div v-for="item in methodStats" :key="item.label" class="method-line"><span>{{ item.label }}</span><strong>¥ {{ money(item.amount) }}</strong><small>{{ item.count }} 条</small><div class="method-track"><div :style="{ width: item.percent + '%' }" /></div></div></div>
      </section>
    </div>
    <template v-else>
    <el-table v-loading="loading" :data="rows" row-key="id" border class="detail-table" empty-text="暂无运费征收明细">
      <el-table-column prop="createdAt" label="录入时间" width="170" :formatter="(_,__,value) => time(value)" />
      <el-table-column prop="shopName" label="店铺" min-width="160" show-overflow-tooltip />
      <el-table-column prop="orderNo" label="订单号" min-width="190" show-overflow-tooltip />
      <el-table-column label="订单状态" width="145"><template #default="{ row }"><el-tag :type="row.deliveryStatus === 'SIGNED' ? 'success' : 'warning'" effect="plain">{{ statusLabel(row.deliveryStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="运费金额" width="130" align="right"><template #default="{ row }">¥ {{ money(row.freightAmount) }}</template></el-table-column>
      <el-table-column label="划扣方式" min-width="170"><template #default="{ row }">{{ methodLabel(row.deductionMethod) }}</template></el-table-column>
      <el-table-column prop="createdByName" label="录入人" width="115" />
      <el-table-column label="征收时间" width="170"><template #default="{ row }">{{ time(row.collectedAt) }}</template></el-table-column>
      <el-table-column prop="remark" label="备注" min-width="170" show-overflow-tooltip />
      <el-table-column v-if="capabilities.edit" label="操作" width="95" fixed="right"><template #default="{ row }"><el-button link type="primary" :icon="Edit" :disabled="row.editable === false" @click="openEdit(row)">编辑</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" v-model:page-size="pageSize" class="pager" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @size-change="load" @current-change="load" />
    </template>

    <el-dialog v-model="visible" :title="form.id ? '编辑运费明细' : '新增运费明细'" width="min(600px, 96vw)" :close-on-click-modal="false" destroy-on-close @opened="focusOrderNo">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="saving">
        <div class="form-grid">
          <el-form-item label="店铺" prop="shopId"><el-select v-model="form.shopId" filterable placeholder="选择店铺"><el-option v-for="shop in operateShops" :key="shop.shopId" :label="shop.shopName" :value="shop.shopId" /></el-select></el-form-item>
          <el-form-item label="订单号" prop="orderNo"><el-input ref="orderNoInput" v-model.trim="form.orderNo" maxlength="64" placeholder="输入订单号" /></el-form-item>
          <el-form-item label="订单状态" prop="deliveryStatus"><el-select v-model="form.deliveryStatus" placeholder="选择状态"><el-option v-for="item in statuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="运费金额（元）" prop="freightAmount"><el-input-number v-model="form.freightAmount" :min="0.01" :max="999999999999.99" :precision="2" :controls="false" /></el-form-item>
          <el-form-item label="划扣方式" prop="deductionMethod"><el-select v-model="form.deductionMethod" placeholder="选择方式"><el-option v-for="item in methods" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item label="征收时间"><el-date-picker v-model="form.collectedAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable placeholder="可选" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model.trim="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>
        <div class="operator-line">录入人：{{ form.id ? form.createdByName : currentName }}<template v-if="form.id"> · 最近修改 {{ time(form.updatedAt) }}</template></div>
      </el-form>
      <template #footer>
        <el-button :disabled="saving" @click="visible=false">取消</el-button>
        <el-button v-if="!form.id" :disabled="saving" @click="save(false)">保存并关闭</el-button>
        <el-button type="primary" :loading="saving" @click="save(true)">{{ form.id ? '保存' : '保存并继续' }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FreightCollection">
import { Download, Edit, Plus, Refresh, Search, Upload } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { saveAs } from 'file-saver'
import * as echarts from 'echarts'
import useUserStore from '@/store/modules/user'

import { freightCapabilities, freightShops, freightList, freightDashboard, freightCreate, freightUpdate, freightImport, freightExport } from '@/api/live/freight'

const user = useUserStore()
const currentName = computed(() => user.nickName || user.name)
const capabilities = ref({})
const operateShops = computed(() => !capabilities.value.managed || capabilities.value.operateScope === 'ALL' ? shops.value : shops.value.filter(s => capabilities.value.operateShopIds?.map(String).includes(String(s.shopId))))
const statuses = [{ value: 'SIGNED', label: '已签收' }, { value: 'SHIPPED_UNSIGNED', label: '已发货未签收' }]
const methods = [{ value: 'ALIPAY_PERSONAL', label: '支付宝个人账户9%' }, { value: 'ORDER_PAYMENT', label: '订单打款7%' }]
const statusLabel = value => statuses.find(item => item.value === value)?.label || value
const methodLabel = value => methods.find(item => item.value === value)?.label || value
const today = () => new Date().toLocaleDateString('en-CA')
const monthAgo = () => { const d = new Date(); d.setDate(d.getDate() - 29); return d.toLocaleDateString('en-CA') }
const initialFilters = () => ({ dates: [monthAgo(), today()], shopId: null, deliveryStatus: '', deductionMethod: '', orderNo: '', creatorName: '' })
const filters = ref(initialFilters()), rows = ref([]), shops = ref([]), total = ref(0), summaryAmount = ref(0)
const tab = ref('detail'), dashboard = ref({}), trendChart = ref()
let trendInstance
const methodStats = computed(() => {
  const totalAmount = Number(dashboard.value.freightAmount || 0)
  return [
    { label: '支付宝个人账户9%', count: Number(dashboard.value.alipayCount || 0), amount: Number(dashboard.value.alipayAmount || 0) },
    { label: '订单打款7%', count: Number(dashboard.value.orderPaymentCount || 0), amount: Number(dashboard.value.orderPaymentAmount || 0) }
  ].map(item => ({ ...item, percent: totalAmount ? Math.round(item.amount / totalAmount * 100) : 0 }))
})
const page = ref(1), pageSize = ref(20), loading = ref(false), saving = ref(false), importing = ref(false), exporting = ref(false)
const fileInput = ref(), formRef = ref(), orderNoInput = ref(), visible = ref(false)
const lastCreate = ref({ shopId: null, deliveryStatus: '' })
const nowLocal = () => {
  const date = new Date()
  const pad = value => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}
const createForm = (shopId = null, deliveryStatus = '', deductionMethod = 'ORDER_PAYMENT') => ({
  id: null, shopId, orderNo: '', deliveryStatus, freightAmount: null,
  deductionMethod, collectedAt: nowLocal(), remark: '', version: 0
})
const form = ref(createForm())
const rules = {
  shopId: [{ required: true, message: '请选择店铺', trigger: 'change' }],
  orderNo: [{ required: true, message: '请输入订单号', trigger: 'blur' }],
  deliveryStatus: [{ required: true, message: '请选择订单状态', trigger: 'change' }],
  freightAmount: [{ required: true, type: 'number', min: 0.01, message: '请输入大于0的运费金额', trigger: 'change' }],
  deductionMethod: [{ required: true, message: '请选择划扣方式', trigger: 'change' }]
}
const money = value => Number(value || 0).toFixed(2)
const time = value => value ? String(value).replace('T', ' ').slice(0, 19) : '—'
const params = () => ({ beginDate: filters.value.dates?.[0], endDate: filters.value.dates?.[1], shopId: filters.value.shopId || undefined, deliveryStatus: filters.value.deliveryStatus || undefined, deductionMethod: filters.value.deductionMethod || undefined, orderNo: filters.value.orderNo || undefined, creatorName: filters.value.creatorName || undefined })

async function load() {
  loading.value = true
  try {
    if (tab.value === 'dashboard') {
      dashboard.value = (await freightDashboard(params())).data || {}
      total.value = Number(dashboard.value.total || 0)
      summaryAmount.value = dashboard.value.freightAmount || 0
      await nextTick()
      renderTrend()
      return
    }
    const result = (await freightList({ ...params(), pageNum: page.value, pageSize: pageSize.value })).data
    rows.value = result.rows || []
    total.value = Number(result.total || 0)
    summaryAmount.value = result.freightAmount || 0
  } finally { loading.value = false }
}
function renderTrend() {
  if (!trendChart.value) return
  if (trendInstance && trendInstance.getDom() !== trendChart.value) { trendInstance.dispose(); trendInstance = null }
  if (!trendInstance) trendInstance = echarts.init(trendChart.value)
  const daily = dashboard.value.daily || []
  trendInstance.setOption({
    grid: { left: 58, right: 22, top: 28, bottom: 46 },
    tooltip: { trigger: 'axis', valueFormatter: value => `¥ ${money(value)}` },
    xAxis: { type: 'category', name: '录入日期', data: daily.map(item => item.day), axisLabel: { hideOverlap: true } },
    yAxis: { type: 'value', name: '运费金额（元）' },
    series: [{ name: '运费金额', type: 'line', smooth: false, symbolSize: 7, data: daily.map(item => Number(item.freightAmount || 0)), areaStyle: { opacity: 0.08 }, lineStyle: { width: 2 } }]
  }, true)
  trendInstance.resize()
}
function resizeTrend() { trendInstance?.resize() }
function search() { page.value = 1; load() }
function resetFilters() { filters.value = initialFilters(); search() }
function focusOrderNo() { orderNoInput.value?.focus() }
function openCreate() {
  const rememberedShop = operateShops.value.find(shop => shop.shopId === lastCreate.value.shopId)
  const shopId = rememberedShop?.shopId ?? (operateShops.value.length === 1 ? operateShops.value[0].shopId : null)
  form.value = createForm(shopId, lastCreate.value.deliveryStatus)
  visible.value = true
}
function openEdit(row) { form.value = { ...row, freightAmount: Number(row.freightAmount), collectedAt: row.collectedAt ? time(row.collectedAt) : '' }; visible.value = true }
async function save(continueCreating) {
  await formRef.value.validate()
  saving.value = true
  try {
    const editing = Boolean(form.value.id)
    if (editing) await freightUpdate(form.value.id, form.value)
    else await freightCreate(form.value)
    if (!editing) lastCreate.value = { shopId: form.value.shopId, deliveryStatus: form.value.deliveryStatus }
    ElMessage.success('保存成功')
    if (continueCreating && !editing) {
      form.value = createForm(form.value.shopId, form.value.deliveryStatus, form.value.deductionMethod)
      await nextTick()
      formRef.value?.clearValidate()
      focusOrderNo()
    } else visible.value = false
    try { await load() } catch (error) { ElMessage.error(error?.message || '列表刷新失败，请手动查询') }
  } catch (error) {
    const message = error?.message || '保存失败'
    if (message.includes('订单号已存在')) await ElMessageBox.alert('该订单号已经录入，请核对后修改。', '重复订单', { type: 'warning', confirmButtonText: '知道了' })
    else ElMessage.error(message)
  } finally { saving.value = false }
}
async function importFile(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  importing.value = true
  try {
    const result = (await freightImport(file)).data
    ElMessage.success(`已导入 ${result.imported} 条明细`)
    await load()
  } catch (error) {
    await ElMessageBox.alert(error?.message || '导入失败', '导入未完成', { type: 'warning', confirmButtonText: '知道了' })
  } finally { importing.value = false }
}
async function exportRows() {
  exporting.value = true
  try {
    const blob = await freightExport(params())
    if (blob.type?.includes('json')) throw new Error(JSON.parse(await blob.text()).msg || '导出失败')
    saveAs(blob, `运费征收明细_${filters.value.dates[0]}_${filters.value.dates[1]}.csv`)
  } catch (error) { ElMessage.error(error?.message || '导出失败') }
  finally { exporting.value = false }
}
onMounted(async () => { window.addEventListener('resize', resizeTrend); try { capabilities.value=(await freightCapabilities()).data || {}; shops.value = (await freightShops()).data || []; await load() } catch (error) { ElMessage.error(error?.message || '加载失败') } })
onBeforeUnmount(() => { window.removeEventListener('resize', resizeTrend); trendInstance?.dispose(); trendInstance = null })
</script>

<style scoped>
.freight-page { color: var(--el-text-color-primary) }
.page-head { display:flex; align-items:center; justify-content:space-between; gap:16px; margin-bottom:20px }
.page-head h2 { margin:0 0 5px; font-size:20px; font-weight:650 }
.page-head span, .operator-line { color:var(--el-text-color-secondary); font-size:13px }
.head-actions { display:flex; align-items:center; gap:8px; flex-wrap:wrap }
.view-tabs { margin-bottom:6px }
.filters { padding:14px 0 2px; border-top:1px solid var(--el-border-color-light) }
.filters :deep(.el-form-item) { margin-right:12px; margin-bottom:12px }
.filters :deep(.el-select) { width:180px }
.filters :deep(.el-input) { width:170px }
.detail-table { width:100% }
.pager { justify-content:flex-end; margin-top:18px }
.form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:0 16px }
.form-grid :deep(.el-select), .form-grid :deep(.el-input-number), .form-grid :deep(.el-date-editor) { width:100% }
.operator-line { padding:4px 0 8px }
.dashboard { min-height:340px }
.metrics { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:0; border-bottom:1px solid var(--el-border-color-light); padding:22px 0 }
.metrics>div { padding:0 24px; border-left:1px solid var(--el-border-color-light) }
.metrics>div:first-child { padding-left:0; border-left:0 }
.metrics span,.section-head span { display:block; font-size:13px; color:var(--el-text-color-secondary) }
.metrics strong { display:block; margin-top:8px; font-size:24px; line-height:1.2; font-weight:650; overflow-wrap:anywhere }
.dashboard-section { padding:24px 0; border-bottom:1px solid var(--el-border-color-light) }
.section-head { display:flex; align-items:baseline; gap:12px; margin-bottom:16px }
.section-head h3 { margin:0; font-size:16px; font-weight:650 }
.trend-chart { width:100%; height:300px }
.method-lines { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:32px }
.method-line { display:grid; grid-template-columns:minmax(0,1fr) auto; gap:7px 12px; align-items:baseline }
.method-line>span { font-size:14px; min-width:0; overflow-wrap:anywhere }
.method-line>strong { font-size:20px }
.method-line>small { color:var(--el-text-color-secondary); font-size:12px }
.method-track { grid-column:1/-1; height:7px; background:var(--el-fill-color); border-radius:3px; overflow:hidden }
.method-track>div { height:100%; background:var(--el-color-success) }
.method-line:nth-child(2) .method-track>div { background:var(--el-color-warning) }
@media (max-width:700px) { .page-head { align-items:flex-start; flex-direction:column } .head-actions { width:100% } .form-grid { grid-template-columns:1fr } .pager { justify-content:flex-start; overflow-x:auto } }
@media (max-width:700px) { .metrics { grid-template-columns:repeat(2,minmax(0,1fr)); row-gap:20px } .metrics>div { padding:0 12px } .metrics>div:nth-child(odd) { border-left:0; padding-left:0 } .method-lines { grid-template-columns:1fr; gap:24px } }
</style>
