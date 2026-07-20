<template>
  <div class="app-container inventory-page">
    <section class="inventory-summary">
      <div class="summary-title">
        <span>INVENTORY CONTROL</span>
        <h2>库存控制台</h2>
        <p>ERP 流水是账面库存，云仓快照用于对账，不直接覆盖账面数量。</p>
      </div>
      <div class="summary-card physical"><span>实体仓可用</span><strong>{{ number(summary.physicalAvailableQty) }}</strong><small>ERP 实时记账</small></div>
      <div class="summary-card cloud"><span>云仓账面</span><strong>{{ number(summary.cloudLedgerAvailableQty) }}</strong><small>出入库流水余额</small></div>
      <div class="summary-card cloud-external"><span>云仓实存</span><strong>{{ number(summary.cloudExternalAvailableQty) }}</strong><small>服务商最近快照</small></div>
      <button class="summary-card warning" @click="filterException('low_stock')"><span>低库存 SKU</span><strong>{{ number(summary.lowStockSkuCount) }}</strong><small>可用数不高于安全库存</small></button>
      <button class="summary-card danger" @click="filterException('sync_diff')"><span>云仓差异 SKU</span><strong>{{ number(summary.cloudDiffSkuCount) }}</strong><small>需要核对库存流水</small></button>
    </section>

    <section class="filter-panel" v-show="showSearch">
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="仓库" prop="warehouseName"><el-input v-model="queryParams.warehouseName" placeholder="搜索仓库" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="仓型" prop="warehouseType"><el-select v-model="queryParams.warehouseType" placeholder="全部仓型" clearable><el-option label="实体仓" value="physical" /><el-option label="云仓" value="cloud" /></el-select></el-form-item>
        <el-form-item label="SKU" prop="skuCode"><el-input v-model="queryParams.skuCode" placeholder="SKU编码" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="商品" prop="skuName"><el-input v-model="queryParams.skuName" placeholder="SKU名称" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="异常" prop="onlyWarning"><el-select v-model="queryParams.onlyWarning" placeholder="全部库存" clearable><el-option label="低库存" value="low_stock" /><el-option label="云仓有差异" value="sync_diff" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <div class="table-toolbar">
        <div class="toolbar-copy"><strong>库存明细</strong><span>库存金额 ¥{{ money(summary.inventoryAmount) }} · 锁定 {{ number(summary.lockedQty) }} · 在途 {{ number(summary.inboundQty) }}</span></div>
        <div class="toolbar-actions">
          <el-button type="primary" icon="Upload" @click="openCloudSync" v-hasPermi="['erp:inventory:sync']">同步云仓库存</el-button>
          <el-button icon="Clock" @click="openSyncLog">同步记录</el-button>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
        </div>
      </div>

      <el-table v-loading="loading" :data="inventoryList" row-key="balanceId">
        <el-table-column label="仓库" min-width="190" fixed="left">
          <template #default="{ row }"><div class="warehouse-cell"><el-tag :type="isCloud(row) ? 'primary' : 'success'" effect="light">{{ isCloud(row) ? '云仓' : '实体' }}</el-tag><div><strong>{{ row.warehouseName }}</strong><span>{{ isCloud(row) ? (row.providerName || '未配置服务商') : '自有库存' }}</span></div></div></template>
        </el-table-column>
        <el-table-column label="SKU" min-width="220">
          <template #default="{ row }"><div class="sku-cell"><strong>{{ row.skuName }}</strong><span>{{ row.skuCode }}</span></div></template>
        </el-table-column>
        <el-table-column label="ERP可用" width="105" align="right">
          <template #default="{ row }"><strong :class="{ 'low-stock': row.availableQty <= row.safetyQty }">{{ number(row.availableQty) }}</strong></template>
        </el-table-column>
        <el-table-column label="锁定" prop="lockedQty" width="82" align="right" />
        <el-table-column label="在途" prop="inboundQty" width="82" align="right" />
        <el-table-column label="残次" prop="defectiveQty" width="82" align="right" />
        <el-table-column label="安全库存" prop="safetyQty" width="100" align="right" />
        <el-table-column label="云仓实存" width="105" align="right">
          <template #default="{ row }"><span v-if="isCloud(row)">{{ row.externalAvailableQty ?? '-' }}</span><span v-else class="muted">-</span></template>
        </el-table-column>
        <el-table-column label="差异" width="100" align="right">
          <template #default="{ row }"><el-tag v-if="isCloud(row)" :type="Number(row.syncDiffQty || 0) === 0 ? 'success' : 'danger'" effect="plain">{{ signed(row.syncDiffQty) }}</el-tag><span v-else class="muted">-</span></template>
        </el-table-column>
        <el-table-column label="对账状态" width="115" align="center">
          <template #default="{ row }"><template v-if="isCloud(row)"><span class="sync-dot" :class="row.syncStatus"></span>{{ syncLabel(row.syncStatus) }}</template><span v-else class="muted">实时记账</span></template>
        </el-table-column>
        <el-table-column label="成本价" width="100" align="right"><template #default="{ row }">¥{{ money(row.costPrice) }}</template></el-table-column>
        <el-table-column label="库存金额" width="120" align="right"><template #default="{ row }">¥{{ money(Number(row.availableQty || 0) * Number(row.costPrice || 0)) }}</template></el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="165" />
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog title="同步云仓库存" v-model="syncOpen" width="700px" append-to-body destroy-on-close>
      <div class="sync-notice"><el-icon><InfoFilled /></el-icon><div><strong>先对账，后处理差异</strong><span>本操作保存云仓快照并计算“云仓实存 - ERP可用”，不会直接修改 ERP 库存。</span></div></div>
      <el-form label-position="top">
        <div class="sync-form-grid">
          <el-form-item label="选择云仓"><el-select v-model="syncForm.warehouseId" placeholder="请选择" style="width:100%"><el-option v-for="item in cloudWarehouses" :key="item.warehouseId" :label="`${item.warehouseName} · ${item.providerName || '未配置服务商'}`" :value="item.warehouseId" /></el-select></el-form-item>
          <el-form-item label="数据来源"><el-select v-model="syncForm.source" style="width:100%"><el-option label="手工粘贴" value="manual" /><el-option label="服务商文件" value="file" /><el-option label="API接口" value="api" /></el-select></el-form-item>
        </div>
        <el-form-item label="库存明细">
          <el-input v-model="syncForm.text" type="textarea" :rows="11" placeholder="每行一个SKU：SKU编码, 可用库存, 锁定库存（锁定可不填）&#10;例如：&#10;DEMO-SKU-001, 120, 5&#10;DEMO-SKU-002, 86, 0" />
        </el-form-item>
      </el-form>
      <div class="parse-result" v-if="syncForm.text"><span>已识别 {{ parsedSyncItems.length }} 个 SKU</span><span v-if="parseError" class="error">{{ parseError }}</span></div>
      <template #footer><el-button @click="syncOpen=false">取消</el-button><el-button type="primary" :loading="syncing" @click="submitCloudSync">开始对账</el-button></template>
    </el-dialog>

    <el-dialog title="云仓同步记录" v-model="logOpen" width="900px" append-to-body>
      <div class="log-filter"><span>云仓</span><el-select v-model="logWarehouseId" @change="loadSyncLogs" style="width:260px"><el-option v-for="item in cloudWarehouses" :key="item.warehouseId" :label="item.warehouseName" :value="item.warehouseId" /></el-select></div>
      <el-table v-loading="logLoading" :data="syncLogs" max-height="480">
        <el-table-column label="批次号" prop="syncBatchNo" min-width="190" />
        <el-table-column label="来源" prop="sourceType" width="90" />
        <el-table-column label="状态" width="90"><template #default="{ row }"><el-tag :type="row.syncStatus === 'success' ? 'success' : 'warning'">{{ row.syncStatus === 'success' ? '一致' : '有差异' }}</el-tag></template></el-table-column>
        <el-table-column label="上报SKU" prop="totalSkuCount" width="90" align="right" />
        <el-table-column label="已匹配" prop="matchedSkuCount" width="90" align="right" />
        <el-table-column label="未建档" prop="unmatchedSkuCount" width="90" align="right" />
        <el-table-column label="差异SKU" prop="differenceSkuCount" width="90" align="right" />
        <el-table-column label="同步时间" prop="finishedTime" width="165" />
        <el-table-column label="说明" prop="message" min-width="190" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="ErpInventory">
import { getInventorySummary, listCloudSyncLog, listInventory, syncCloudInventory } from '@/api/erp/inventory'
import { listWarehouse } from '@/api/erp/warehouse'

const { proxy } = getCurrentInstance()
const inventoryList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const total = ref(0)
const queryParams = reactive({ pageNum: 1, pageSize: 10, warehouseName: undefined, warehouseType: undefined, skuCode: undefined, skuName: undefined, syncStatus: undefined, onlyWarning: undefined })
const summary = reactive({ physicalAvailableQty: 0, cloudLedgerAvailableQty: 0, cloudExternalAvailableQty: 0, lockedQty: 0, inboundQty: 0, defectiveQty: 0, lowStockSkuCount: 0, cloudDiffSkuCount: 0, inventoryAmount: 0 })
const cloudWarehouses = ref([])
const syncOpen = ref(false)
const syncing = ref(false)
const syncForm = reactive({ warehouseId: undefined, source: 'manual', text: '' })
const logOpen = ref(false)
const logWarehouseId = ref()
const logLoading = ref(false)
const syncLogs = ref([])

function number(value) { return Number(value || 0).toLocaleString('zh-CN') }
function money(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function signed(value) { const num = Number(value || 0); return num > 0 ? `+${num}` : String(num) }
function isCloud(row) { return ['cloud', 'third_party'].includes(row.warehouseType) }
function syncLabel(value) { return ({ never: '未同步', matched: '一致', difference: '有差异', failed: '失败' })[value] || '未同步' }

const parsedResult = computed(() => {
  const items = []
  let error = ''
  const lines = syncForm.text.split(/\r?\n/).map(line => line.trim()).filter(Boolean)
  for (let index = 0; index < lines.length; index++) {
    const parts = lines[index].split(/[,，\t]/).map(item => item.trim())
    const availableQty = Number(parts[1])
    const lockedQty = parts[2] === undefined || parts[2] === '' ? 0 : Number(parts[2])
    if (!parts[0] || !Number.isInteger(availableQty) || availableQty < 0 || !Number.isInteger(lockedQty) || lockedQty < 0) {
      error = `第 ${index + 1} 行格式不正确`
      break
    }
    items.push({ skuCode: parts[0], availableQty, lockedQty })
  }
  return { items, error }
})
const parsedSyncItems = computed(() => parsedResult.value.items)
const parseError = computed(() => parsedResult.value.error)

async function getList() {
  loading.value = true
  try {
    const [listResponse, summaryResponse] = await Promise.all([listInventory(queryParams), getInventorySummary(queryParams)])
    inventoryList.value = listResponse.rows || []
    total.value = listResponse.total || 0
    Object.assign(summary, summaryResponse.data || {})
  } finally { loading.value = false }
}
async function loadCloudWarehouses() { const response = await listWarehouse({ pageNum: 1, pageSize: 200, warehouseType: 'cloud', status: '0' }); cloudWarehouses.value = response.rows || [] }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function filterException(type) { queryParams.onlyWarning = type; queryParams.warehouseType = type === 'sync_diff' ? 'cloud' : undefined; handleQuery() }

async function openCloudSync() {
  await loadCloudWarehouses()
  if (!cloudWarehouses.value.length) { proxy.$modal.msgWarning('请先在仓库管理中创建并启用云仓'); return }
  syncForm.warehouseId = syncForm.warehouseId || cloudWarehouses.value[0].warehouseId
  syncOpen.value = true
}
async function submitCloudSync() {
  if (!syncForm.warehouseId) { proxy.$modal.msgWarning('请选择云仓'); return }
  if (!syncForm.text.trim()) { proxy.$modal.msgWarning('请粘贴库存明细'); return }
  if (parseError.value) { proxy.$modal.msgWarning(parseError.value); return }
  syncing.value = true
  try {
    const response = await syncCloudInventory({ warehouseId: syncForm.warehouseId, source: syncForm.source, items: parsedSyncItems.value })
    const result = response.data
    proxy.$modal.msgSuccess(`同步完成：匹配 ${result.matchedCount}，未建档 ${result.unmatchedCount}，当前差异 ${result.differenceCount}`)
    syncOpen.value = false
    syncForm.text = ''
    getList()
  } finally { syncing.value = false }
}
async function openSyncLog() {
  await loadCloudWarehouses()
  if (!cloudWarehouses.value.length) { proxy.$modal.msgWarning('暂无云仓'); return }
  logWarehouseId.value = logWarehouseId.value || cloudWarehouses.value[0].warehouseId
  logOpen.value = true
  loadSyncLogs()
}
async function loadSyncLogs() { if (!logWarehouseId.value) return; logLoading.value = true; try { const response = await listCloudSyncLog(logWarehouseId.value); syncLogs.value = response.data || [] } finally { logLoading.value = false } }

getList()
</script>

<style scoped>
.inventory-page { display: flex; flex-direction: column; gap: 16px; }
.inventory-summary, .filter-panel, .table-panel { border: 1px solid rgba(108,92,231,.1); background: rgba(255,255,255,.93); box-shadow: 0 12px 32px rgba(51,46,112,.07); }
.inventory-summary { display: grid; grid-template-columns: minmax(230px, 1.1fr) repeat(5, minmax(130px, .8fr)); gap: 10px; padding: 18px; border-radius: 16px; }
.summary-title { padding: 4px 10px; }
.summary-title > span { color: #6c5ce7; font-size: 11px; font-weight: 800; letter-spacing: .08em; }
.summary-title h2 { margin: 5px 0 3px; color: #24283b; font-size: 23px; }
.summary-title p { margin: 0; color: #9299ad; font-size: 12px; line-height: 1.5; }
.summary-card { display: flex; flex-direction: column; align-items: flex-start; justify-content: center; min-width: 0; padding: 12px 14px; border: 1px solid #ececf5; border-radius: 12px; background: #fafafe; color: #737a91; text-align: left; }
button.summary-card { cursor: pointer; transition: .2s ease; } button.summary-card:hover { transform: translateY(-2px); border-color: #c2bbff; box-shadow: 0 8px 18px rgba(108,92,231,.1); }
.summary-card strong { margin: 3px 0 1px; color: #292e43; font-size: 22px; }.summary-card small { overflow: hidden; width: 100%; color: #a0a6b8; text-overflow: ellipsis; white-space: nowrap; }
.summary-card.physical strong { color: #00a88f; }.summary-card.cloud strong { color: #6c5ce7; }.summary-card.cloud-external strong { color: #3586ed; }.summary-card.warning strong { color: #e58a26; }.summary-card.danger strong { color: #e25564; }
.filter-panel { padding: 14px 18px 2px; border-radius: 14px; }.filter-panel :deep(.el-form-item) { margin-right: 16px; }.filter-panel :deep(.el-input), .filter-panel :deep(.el-select) { width: 170px; }
.table-panel { overflow: hidden; border-radius: 16px; }.table-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 14px 16px; border-bottom: 1px solid #eceef5; }.toolbar-copy strong, .toolbar-copy span { display: block; }.toolbar-copy span { margin-top: 3px; color: #969db0; font-size: 12px; }.toolbar-actions { display: flex; align-items: center; gap: 8px; }
.warehouse-cell { display: flex; align-items: center; gap: 9px; }.warehouse-cell strong, .warehouse-cell span, .sku-cell strong, .sku-cell span { display: block; }.warehouse-cell span, .sku-cell span { margin-top: 2px; color: #969db0; font-size: 12px; }.sku-cell strong { color: #34394d; }
.low-stock { color: #e36a39; }.muted { color: #b0b5c4; }.sync-dot { display: inline-block; width: 7px; height: 7px; margin-right: 6px; border-radius: 50%; background: #aeb3c3; }.sync-dot.matched { background: #00b894; }.sync-dot.difference { background: #f39c3d; }.sync-dot.failed { background: #ea5a68; }
.sync-notice { display: flex; gap: 11px; margin-bottom: 16px; padding: 13px 15px; border-radius: 10px; background: #f0eeff; color: #6c5ce7; }.sync-notice strong, .sync-notice span { display: block; }.sync-notice span { margin-top: 3px; color: #727a94; font-size: 12px; }.sync-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 16px; }.parse-result { display: flex; justify-content: space-between; color: #6f768d; font-size: 12px; }.parse-result .error { color: #e25564; }.log-filter { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
@media (max-width: 1350px) { .inventory-summary { grid-template-columns: repeat(3, 1fr); }.summary-title { grid-column: 1 / -1; } }
@media (max-width: 760px) { .inventory-summary { grid-template-columns: repeat(2, 1fr); }.table-toolbar { align-items: flex-start; flex-direction: column; }.sync-form-grid { grid-template-columns: 1fr; } }
</style>
