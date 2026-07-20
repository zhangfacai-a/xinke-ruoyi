<template>
  <div class="app-container warehouse-page">
    <section class="warehouse-overview">
      <div class="overview-copy">
        <span class="overview-label">WAREHOUSE NETWORK</span>
        <h2>仓网管理</h2>
        <p>实体仓管库位和作业，云仓管服务商库存与账实差异。</p>
      </div>
      <div class="metric-grid">
        <button class="metric-item" :class="{ active: !queryParams.warehouseType }" @click="filterByType(undefined)">
          <span>全部仓库</span><strong>{{ summary.totalCount }}</strong><small>{{ summary.enabledCount }} 个启用</small>
        </button>
        <button class="metric-item physical" :class="{ active: queryParams.warehouseType === 'physical' }" @click="filterByType('physical')">
          <span>实体仓</span><strong>{{ summary.physicalCount }}</strong><small>库位、盘点、批次</small>
        </button>
        <button class="metric-item cloud" :class="{ active: queryParams.warehouseType === 'cloud' }" @click="filterByType('cloud')">
          <span>云仓</span><strong>{{ summary.cloudCount }}</strong><small>外部库存对账</small>
        </button>
        <button class="metric-item warning" :class="{ active: queryParams.syncStatus === 'warning' }" @click="filterSyncWarning">
          <span>同步异常</span><strong>{{ summary.syncAbnormalCount }}</strong><small>待核对差异</small>
        </button>
      </div>
    </section>

    <section class="filter-panel" v-show="showSearch">
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="仓库" prop="warehouseName">
          <el-input v-model="queryParams.warehouseName" placeholder="名称或关键字" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="编码" prop="warehouseCode">
          <el-input v-model="queryParams.warehouseCode" placeholder="仓库编码" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="形态" prop="warehouseType">
          <el-select v-model="queryParams.warehouseType" placeholder="全部形态" clearable>
            <el-option label="实体仓" value="physical" />
            <el-option label="云仓" value="cloud" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item class="filter-actions">
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <div class="table-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['erp:warehouse:add']">新增仓库</el-button>
          <el-button icon="Edit" :disabled="single" @click="handleUpdate" v-hasPermi="['erp:warehouse:edit']">修改</el-button>
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete" v-hasPermi="['erp:warehouse:remove']">删除</el-button>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>

      <el-table v-loading="loading" :data="warehouseList" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column label="仓库" min-width="220" fixed="left">
          <template #default="{ row }">
            <div class="warehouse-cell">
              <span class="warehouse-icon" :class="warehouseType(row)">
                <el-icon><House v-if="warehouseType(row) === 'physical'" /><Cloudy v-else /></el-icon>
              </span>
              <div>
                <strong>{{ row.warehouseName }}</strong>
                <span>{{ row.warehouseCode }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="形态 / 用途" min-width="150">
          <template #default="{ row }">
            <el-tag :type="warehouseType(row) === 'cloud' ? 'primary' : 'success'" effect="light">
              {{ warehouseType(row) === 'cloud' ? '云仓' : '实体仓' }}
            </el-tag>
            <span class="usage-text">{{ usageLabel(row.warehouseUsage) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="经营信息" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">
            <template v-if="warehouseType(row) === 'cloud'">
              <strong class="cell-main">{{ row.providerName || '-' }}</strong>
              <span class="cell-sub">外部编码：{{ row.externalWarehouseCode || '-' }}</span>
            </template>
            <template v-else>
              <strong class="cell-main">{{ row.ownershipType === 'outsourced' ? '外包管理' : '自营管理' }}</strong>
              <span class="cell-sub">{{ row.locationCount || 0 }} 个库位</span>
            </template>
          </template>
        </el-table-column>
        <el-table-column label="SKU" prop="skuCount" width="82" align="right" />
        <el-table-column label="同步状态" min-width="145">
          <template #default="{ row }">
            <template v-if="warehouseType(row) === 'cloud'">
              <el-tag :type="syncTagType(row.syncStatus)" effect="plain">{{ syncLabel(row.syncStatus) }}</el-tag>
              <span class="cell-sub">{{ row.lastSyncTime || '尚未同步' }}</span>
            </template>
            <span v-else class="muted">实体仓实时记账</span>
          </template>
        </el-table-column>
        <el-table-column label="负责人" min-width="150">
          <template #default="{ row }">
            <span class="cell-main">{{ row.contactName || '-' }}</span>
            <span class="cell-sub">{{ row.contactPhone || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="地址" prop="address" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="82" align="center">
          <template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right" align="center">
          <template #default="{ row }">
            <el-button v-if="warehouseType(row) === 'physical'" link type="primary" icon="Grid" @click="openLocations(row)" v-hasPermi="['erp:warehouse:query']">库位</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['erp:warehouse:edit']">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['erp:warehouse:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog :title="title" v-model="open" width="760px" append-to-body destroy-on-close>
      <el-form ref="warehouseRef" :model="form" :rules="rules" label-position="top" class="warehouse-form">
        <div class="form-section-title">基础信息</div>
        <div class="form-grid">
          <el-form-item label="仓库编码" prop="warehouseCode"><el-input v-model="form.warehouseCode" placeholder="例如 WH-HZ-001" /></el-form-item>
          <el-form-item label="仓库名称" prop="warehouseName"><el-input v-model="form.warehouseName" placeholder="便于业务识别的名称" /></el-form-item>
          <el-form-item label="仓库形态" prop="warehouseType">
            <el-segmented v-model="form.warehouseType" :options="warehouseTypeOptions" block @change="handleWarehouseTypeChange" />
          </el-form-item>
          <el-form-item label="仓库用途" prop="warehouseUsage">
            <el-select v-model="form.warehouseUsage" style="width:100%">
              <el-option label="正品仓" value="normal" /><el-option label="退货仓" value="return" />
              <el-option label="残次仓" value="defective" /><el-option label="中转仓" value="transit" />
            </el-select>
          </el-form-item>
          <el-form-item label="经营方式" prop="ownershipType">
            <el-select v-model="form.ownershipType" style="width:100%"><el-option label="自营" value="self_operated" /><el-option label="外包" value="outsourced" /></el-select>
          </el-form-item>
          <el-form-item label="出库优先级"><el-input-number v-model="form.priority" :min="1" :max="9999" controls-position="right" style="width:100%" /></el-form-item>
        </div>

        <template v-if="form.warehouseType === 'cloud'">
          <div class="form-section-title">云仓接入</div>
          <div class="form-grid cloud-config">
            <el-form-item label="云仓服务商" prop="providerName"><el-input v-model="form.providerName" placeholder="例如 京东物流、菜鸟云仓" /></el-form-item>
            <el-form-item label="外部仓编码" prop="externalWarehouseCode"><el-input v-model="form.externalWarehouseCode" placeholder="服务商系统中的仓库编码" /></el-form-item>
            <el-form-item label="货主编码"><el-input v-model="form.ownerCode" placeholder="服务商分配的货主编码" /></el-form-item>
            <el-form-item label="同步方式"><el-select v-model="form.syncMode" style="width:100%"><el-option label="手工同步" value="manual" /><el-option label="API接口" value="api" /><el-option label="文件导入" value="file" /></el-select></el-form-item>
          </div>
        </template>
        <template v-else>
          <div class="form-section-title">实体仓规则</div>
          <div class="switch-row">
            <div><strong>启用库位管理</strong><span>收货、拣货和盘点按库位执行</span></div><el-switch v-model="form.enableLocation" active-value="1" inactive-value="0" />
          </div>
        </template>

        <div class="form-section-title">联系与控制</div>
        <div class="form-grid">
          <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
          <el-form-item label="仓库地址" class="span-2"><el-input v-model="form.address" /></el-form-item>
          <el-form-item label="允许负库存"><el-switch v-model="form.allowNegativeStock" active-value="1" inactive-value="0" /><span class="switch-help">建议保持关闭</span></el-form-item>
          <el-form-item label="启用状态"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
          <el-form-item label="备注" class="span-2"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
        </div>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" @click="submitForm">保存仓库</el-button></template>
    </el-dialog>

    <el-drawer v-model="locationOpen" :title="`${activeWarehouse.warehouseName || ''} · 库位管理`" size="760px" append-to-body>
      <div class="location-toolbar">
        <div><strong>实体仓库位</strong><span>编码在仓内唯一，可用于打印条码和盘点。</span></div>
        <el-button type="primary" icon="Plus" @click="editLocation()" v-hasPermi="['erp:warehouse:add']">新增库位</el-button>
      </div>
      <el-table v-loading="locationLoading" :data="locationList">
        <el-table-column label="库位编码" prop="locationCode" min-width="120" />
        <el-table-column label="库位名称" prop="locationName" min-width="140" />
        <el-table-column label="库区" prop="zoneCode" width="100" />
        <el-table-column label="用途" width="100"><template #default="{ row }"><el-tag effect="plain">{{ locationUsageLabel(row.usageType) }}</el-tag></template></el-table-column>
        <el-table-column label="条码" prop="barcode" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" width="80"><template #default="{ row }">{{ row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
        <el-table-column label="操作" width="130" align="right"><template #default="{ row }"><el-button link type="primary" @click="editLocation(row)">修改</el-button><el-button link type="danger" @click="removeLocation(row)">删除</el-button></template></el-table-column>
      </el-table>
    </el-drawer>

    <el-dialog :title="locationForm.locationId ? '修改库位' : '新增库位'" v-model="locationEditOpen" width="560px" append-to-body>
      <el-form ref="locationRef" :model="locationForm" :rules="locationRules" label-position="top" class="form-grid">
        <el-form-item label="库位编码" prop="locationCode"><el-input v-model="locationForm.locationCode" placeholder="例如 A-01-01" /></el-form-item>
        <el-form-item label="库位名称" prop="locationName"><el-input v-model="locationForm.locationName" /></el-form-item>
        <el-form-item label="库区"><el-input v-model="locationForm.zoneCode" placeholder="例如 A区" /></el-form-item>
        <el-form-item label="用途"><el-select v-model="locationForm.usageType" style="width:100%"><el-option label="正品" value="normal" /><el-option label="退货" value="return" /><el-option label="残次" value="defective" /><el-option label="暂存" value="staging" /></el-select></el-form-item>
        <el-form-item label="条码"><el-input v-model="locationForm.barcode" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="locationForm.sortOrder" :min="1" :max="9999" style="width:100%" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="locationForm.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="locationForm.remark" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="locationEditOpen=false">取消</el-button><el-button type="primary" @click="submitLocation">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpWarehouse">
import {
  addWarehouse, addWarehouseLocation, delWarehouse, delWarehouseLocation, getWarehouse,
  getWarehouseSummary, listWarehouse, listWarehouseLocation, updateWarehouse, updateWarehouseLocation
} from '@/api/erp/warehouse'

const { proxy } = getCurrentInstance()
const warehouseList = ref([])
const loading = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const warehouseRef = ref()
const locationRef = ref()
const summary = reactive({ totalCount: 0, physicalCount: 0, cloudCount: 0, enabledCount: 0, syncAbnormalCount: 0 })
const queryParams = reactive({ pageNum: 1, pageSize: 10, warehouseCode: undefined, warehouseName: undefined, warehouseType: undefined, status: undefined, syncStatus: undefined })
const form = ref({})
const warehouseTypeOptions = [{ label: '实体仓', value: 'physical' }, { label: '云仓', value: 'cloud' }]

const validateCloudField = (label) => (_rule, value, callback) => {
  if (form.value.warehouseType === 'cloud' && !String(value || '').trim()) callback(new Error(`云仓${label}不能为空`))
  else callback()
}
const rules = {
  warehouseCode: [{ required: true, message: '仓库编码不能为空', trigger: 'blur' }],
  warehouseName: [{ required: true, message: '仓库名称不能为空', trigger: 'blur' }],
  warehouseType: [{ required: true, message: '请选择仓库形态', trigger: 'change' }],
  providerName: [{ validator: validateCloudField('服务商'), trigger: 'blur' }],
  externalWarehouseCode: [{ validator: validateCloudField('外部仓编码'), trigger: 'blur' }]
}

const locationOpen = ref(false)
const locationEditOpen = ref(false)
const locationLoading = ref(false)
const locationList = ref([])
const activeWarehouse = ref({})
const locationForm = ref({})
const locationRules = {
  locationCode: [{ required: true, message: '库位编码不能为空', trigger: 'blur' }],
  locationName: [{ required: true, message: '库位名称不能为空', trigger: 'blur' }]
}

function warehouseType(row) { return ['cloud', 'third_party'].includes(row.warehouseType) ? 'cloud' : 'physical' }
function usageLabel(value) { return ({ normal: '正品', return: '退货', defective: '残次', transit: '中转' })[value] || '正品' }
function locationUsageLabel(value) { return ({ normal: '正品', return: '退货', defective: '残次', staging: '暂存' })[value] || value }
function syncLabel(value) { return ({ never: '未同步', success: '正常', warning: '有差异', failed: '失败' })[value] || '未同步' }
function syncTagType(value) { return ({ success: 'success', warning: 'warning', failed: 'danger', never: 'info' })[value] || 'info' }

async function getList() {
  loading.value = true
  try {
    const [listResponse, summaryResponse] = await Promise.all([listWarehouse(queryParams), getWarehouseSummary()])
    warehouseList.value = listResponse.rows || []
    total.value = listResponse.total || 0
    Object.assign(summary, summaryResponse.data || {})
  } finally { loading.value = false }
}
function reset() {
  form.value = { warehouseId: undefined, warehouseCode: '', warehouseName: '', warehouseType: 'physical', warehouseUsage: 'normal', ownershipType: 'self_operated', providerName: '', externalWarehouseCode: '', ownerCode: '', syncMode: 'manual', syncStatus: 'not_applicable', enableLocation: '1', allowNegativeStock: '0', priority: 100, contactName: '', contactPhone: '', address: '', status: '0', remark: '' }
  nextTick(() => warehouseRef.value?.clearValidate())
}
function handleWarehouseTypeChange(value) {
  if (value === 'cloud') { form.value.ownershipType = 'outsourced'; form.value.enableLocation = '0'; form.value.syncStatus = form.value.syncStatus === 'not_applicable' ? 'never' : form.value.syncStatus }
  else { form.value.ownershipType = 'self_operated'; form.value.enableLocation = '1'; form.value.syncStatus = 'not_applicable' }
}
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); queryParams.syncStatus = undefined; handleQuery() }
function filterByType(type) { queryParams.warehouseType = type; queryParams.syncStatus = undefined; handleQuery() }
function filterSyncWarning() { queryParams.warehouseType = 'cloud'; queryParams.syncStatus = 'warning'; handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.warehouseId); single.value = selection.length !== 1; multiple.value = !selection.length }
function handleAdd() { reset(); open.value = true; title.value = '新增仓库' }
async function handleUpdate(row) {
  reset()
  const warehouseId = row.warehouseId || ids.value[0]
  const response = await getWarehouse(warehouseId)
  form.value = { ...form.value, ...response.data, warehouseType: warehouseType(response.data) }
  open.value = true
  title.value = '修改仓库'
}
function submitForm() {
  warehouseRef.value.validate(async valid => {
    if (!valid) return
    const isEdit = !!form.value.warehouseId
    await (isEdit ? updateWarehouse(form.value) : addWarehouse(form.value))
    proxy.$modal.msgSuccess(isEdit ? '仓库已更新' : '仓库已创建')
    open.value = false
    getList()
  })
}
function handleDelete(row) {
  const warehouseIds = row.warehouseId || ids.value
  proxy.$modal.confirm('删除后无法恢复。已有库存或业务历史的仓库应改为停用，是否继续？').then(() => delWarehouse(warehouseIds)).then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {})
}

async function openLocations(row) { activeWarehouse.value = row; locationOpen.value = true; await loadLocations() }
async function loadLocations() { locationLoading.value = true; try { const response = await listWarehouseLocation(activeWarehouse.value.warehouseId); locationList.value = response.data || [] } finally { locationLoading.value = false } }
function editLocation(row) { locationForm.value = row ? { ...row } : { warehouseId: activeWarehouse.value.warehouseId, locationCode: '', locationName: '', zoneCode: '', usageType: 'normal', barcode: '', sortOrder: 100, status: '0', remark: '' }; locationEditOpen.value = true; nextTick(() => locationRef.value?.clearValidate()) }
function submitLocation() { locationRef.value.validate(async valid => { if (!valid) return; const isEdit = !!locationForm.value.locationId; await (isEdit ? updateWarehouseLocation(locationForm.value) : addWarehouseLocation(locationForm.value)); proxy.$modal.msgSuccess(isEdit ? '库位已更新' : '库位已创建'); locationEditOpen.value = false; loadLocations(); getList() }) }
function removeLocation(row) { proxy.$modal.confirm(`确认删除库位 ${row.locationCode}？`).then(() => delWarehouseLocation(row.locationId)).then(() => { proxy.$modal.msgSuccess('删除成功'); loadLocations(); getList() }).catch(() => {}) }

getList()
</script>

<style scoped>
.warehouse-page { display: flex; flex-direction: column; gap: 16px; }
.warehouse-overview, .filter-panel, .table-panel { border: 1px solid rgba(108, 92, 231, .1); background: rgba(255,255,255,.92); box-shadow: 0 12px 32px rgba(51, 46, 112, .07); }
.warehouse-overview { display: grid; grid-template-columns: minmax(250px, .8fr) minmax(640px, 2fr); gap: 24px; padding: 22px 24px; border-radius: 16px; }
.overview-label { color: #6c5ce7; font-size: 11px; font-weight: 800; letter-spacing: .08em; }
.overview-copy h2 { margin: 6px 0 4px; color: #22263a; font-size: 25px; }
.overview-copy p { margin: 0; color: #7b829c; font-size: 13px; }
.metric-grid { display: grid; grid-template-columns: repeat(4, minmax(120px, 1fr)); gap: 10px; }
.metric-item { display: flex; flex-direction: column; align-items: flex-start; min-width: 0; padding: 13px 15px; border: 1px solid #ececf6; border-radius: 12px; background: #fafafe; color: #6b7288; cursor: pointer; text-align: left; transition: .2s ease; }
.metric-item:hover, .metric-item.active { transform: translateY(-2px); border-color: #b9b1ff; box-shadow: 0 9px 20px rgba(108,92,231,.12); }
.metric-item strong { margin: 3px 0 1px; color: #23283c; font-size: 23px; }
.metric-item small { overflow: hidden; width: 100%; color: #9aa0b4; text-overflow: ellipsis; white-space: nowrap; }
.metric-item.physical strong { color: #00a88f; } .metric-item.cloud strong { color: #6c5ce7; } .metric-item.warning strong { color: #ef8b2c; }
.filter-panel { padding: 14px 18px 2px; border-radius: 14px; }
.filter-panel :deep(.el-form-item) { margin-right: 18px; }
.filter-panel :deep(.el-input), .filter-panel :deep(.el-select) { width: 190px; }
.table-panel { overflow: hidden; border-radius: 16px; }
.table-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; border-bottom: 1px solid #eceef5; }
.toolbar-actions { display: flex; gap: 8px; }
.warehouse-cell { display: flex; align-items: center; gap: 11px; }
.warehouse-icon { display: grid; flex: 0 0 38px; width: 38px; height: 38px; place-items: center; border-radius: 10px; background: #e9fbf6; color: #00a88f; }
.warehouse-icon.cloud { background: #eeebff; color: #6c5ce7; }
.warehouse-cell strong, .warehouse-cell span, .cell-main, .cell-sub { display: block; }
.warehouse-cell strong, .cell-main { color: #30354b; font-size: 13px; }
.warehouse-cell div > span, .cell-sub { margin-top: 3px; color: #969db0; font-size: 12px; }
.usage-text { margin-left: 7px; color: #73798d; font-size: 12px; }
.muted { color: #a0a6b8; font-size: 12px; }
.warehouse-form { max-height: 65vh; overflow-y: auto; padding: 0 4px; }
.form-section-title { margin: 4px 0 12px; padding-left: 9px; border-left: 3px solid #6c5ce7; color: #34394e; font-weight: 700; }
.form-section-title:not(:first-child) { margin-top: 18px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 0 16px; }
.span-2 { grid-column: 1 / -1; }
.switch-row, .location-toolbar { display: flex; align-items: center; justify-content: space-between; padding: 14px 16px; border: 1px solid #ececf5; border-radius: 10px; background: #fafafe; }
.switch-row span, .location-toolbar span { display: block; margin-top: 3px; color: #9299ad; font-size: 12px; }
.switch-help { margin-left: 10px; color: #a0a6b8; font-size: 12px; }
.location-toolbar { margin-bottom: 16px; }
@media (max-width: 1200px) { .warehouse-overview { grid-template-columns: 1fr; } }
@media (max-width: 760px) { .metric-grid { grid-template-columns: repeat(2, 1fr); } .form-grid { grid-template-columns: 1fr; } .span-2 { grid-column: auto; } }
</style>
