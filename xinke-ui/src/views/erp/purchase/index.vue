<template>
  <div class="app-container purchase-page">
    <section class="page-intro">
      <div>
        <span class="eyebrow">PURCHASE CONTROL</span>
        <h2>采购订单工作台</h2>
        <p>统一管理供应商、采购明细、审批与到货进度，订单金额由 SKU 明细自动计算。</p>
      </div>
      <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['erp:purchase:add']">新建采购单</el-button>
    </section>

    <section class="summary-grid">
      <div class="summary-card"><span>采购单总数</span><strong>{{ summary.totalCount || 0 }}</strong><small>全部采购任务</small></div>
      <div class="summary-card warning"><span>待审批</span><strong>{{ summary.pendingApprovalCount || 0 }}</strong><small>需要及时处理</small></div>
      <div class="summary-card danger"><span>逾期未完成</span><strong>{{ summary.overdueCount || 0 }}</strong><small>已超过预计到货</small></div>
      <div class="summary-card money"><span>未结采购额</span><strong>¥{{ formatMoney(summary.openAmount) }}</strong><small>未完成订单价税合计</small></div>
    </section>

    <section class="filter-panel" v-show="showSearch">
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="采购单号" prop="purchaseNo">
          <el-input v-model="queryParams.purchaseNo" placeholder="搜索采购单号" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="采购日期">
          <el-date-picker v-model="purchaseDateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" style="width:250px" />
        </el-form-item>
        <el-form-item label="供应商" prop="supplierName">
          <el-input v-model="queryParams.supplierName" placeholder="搜索供应商" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="入库仓" prop="warehouseId">
          <el-select v-model="queryParams.warehouseId" placeholder="全部仓库" clearable>
            <el-option v-for="item in warehouseOptions" :key="item.warehouseId" :label="item.warehouseName" :value="item.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="purchaseStatus">
          <el-select v-model="queryParams.purchaseStatus" placeholder="全部状态" clearable>
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <div class="table-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:purchase:add']">新增</el-button>
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['erp:purchase:edit']">修改</el-button>
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['erp:purchase:remove']">删除</el-button>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="refreshPage" />
      </div>

      <el-table v-loading="loading" :data="purchaseList" row-key="purchaseId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column label="采购单" min-width="178" show-overflow-tooltip>
          <template #default="{ row }">
            <button class="order-link" @click="handleView(row)">{{ row.purchaseNo }}</button>
            <div class="cell-meta">{{ row.itemCount || 0 }} 种 SKU · {{ row.totalQuantity || 0 }} 件</div>
          </template>
        </el-table-column>
        <el-table-column label="采购日期" prop="purchaseDate" width="112" align="center" />
        <el-table-column label="供应商" prop="supplierName" min-width="170" show-overflow-tooltip />
        <el-table-column label="入库仓" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag :type="isCloudWarehouse(row) ? 'primary' : 'success'" effect="plain" size="small">{{ isCloudWarehouse(row) ? '云仓' : '实体仓' }}</el-tag>
            <span class="warehouse-name">{{ row.warehouseName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="价税合计" min-width="130" align="right">
          <template #default="{ row }"><strong>¥{{ formatMoney(row.totalWithTax ?? row.totalAmount) }}</strong><div class="cell-meta">税额 ¥{{ formatMoney(row.taxAmount) }}</div></template>
        </el-table-column>
        <el-table-column label="到货进度" min-width="160">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.receiveProgress || 0)" :stroke-width="7" />
            <div class="cell-meta">{{ row.receivedQuantity || 0 }} / {{ row.totalQuantity || 0 }} 件</div>
          </template>
        </el-table-column>
        <el-table-column label="预计到货" prop="expectedTime" width="168">
          <template #default="{ row }"><span :class="{ 'overdue-text': row.overdue }">{{ row.expectedTime || '-' }}</span><el-tag v-if="row.overdue" type="danger" size="small" effect="plain" class="overdue-tag">逾期</el-tag></template>
        </el-table-column>
        <el-table-column label="状态" width="104" align="center">
          <template #default="{ row }"><el-tag :type="statusType(row.purchaseStatus)" effect="light">{{ formatStatus(row.purchaseStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="198" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="handleView(row)">详情</el-button>
            <el-button v-if="row.purchaseStatus === 'draft'" link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['erp:purchase:edit']">修改</el-button>
            <el-button v-if="canReceive(row)" link type="success" icon="Box" @click="handleReceive(row)" v-hasPermi="['erp:purchase:edit']">到货</el-button>
            <el-dropdown v-if="availableActions(row).length" trigger="click" @command="target => handleStatus(row, target)">
              <el-button link type="primary">处理<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown><el-dropdown-menu><el-dropdown-item v-for="action in availableActions(row)" :key="action.value" :command="action.value" :class="{ 'danger-action': action.value === 'closed' }">{{ action.label }}</el-dropdown-item></el-dropdown-menu></template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog :title="title" v-model="open" width="1080px" append-to-body destroy-on-close>
      <el-form ref="purchaseRef" :model="form" :rules="rules" label-width="96px" :disabled="dialogMode === 'view'">
        <div class="form-grid">
          <el-form-item label="采购单号" prop="purchaseNo"><el-input v-model="form.purchaseNo" :disabled="Boolean(form.purchaseId)" /></el-form-item>
          <el-form-item label="采购日期" prop="purchaseDate"><el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          <el-form-item label="供应商" prop="supplierId">
            <el-select v-model="form.supplierId" filterable placeholder="选择启用供应商" style="width:100%" @change="handleSupplierChange">
              <el-option v-for="item in supplierOptions" :key="item.supplierId" :value="item.supplierId" :label="item.supplierName"><span>{{ item.supplierName }}</span><span class="option-meta">{{ item.supplierLevel }}级 · {{ item.leadTimeDays }}天</span></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="入库仓" prop="warehouseId">
            <el-select v-model="form.warehouseId" placeholder="选择启用仓库" style="width:100%">
              <el-option v-for="item in receivingWarehouses" :key="item.warehouseId" :value="item.warehouseId" :label="`${item.warehouseName} · ${isCloudWarehouse(item) ? '云仓' : '实体仓'}`" />
            </el-select>
          </el-form-item>
          <el-form-item label="预计到货" prop="expectedTime"><el-date-picker v-model="form.expectedTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit /></el-form-item>

        <div class="line-header">
          <div><h3>采购明细</h3><span>采购价为未税单价，价税合计自动计算</span></div>
          <el-button v-if="dialogMode !== 'view'" type="primary" plain icon="Plus" @click="addItem">添加 SKU</el-button>
        </div>
        <el-table :data="form.items" border class="item-table" empty-text="请添加采购商品">
          <el-table-column type="index" label="#" width="48" align="center" />
          <el-table-column label="SKU" min-width="270">
            <template #default="{ row }">
              <el-select v-model="row.skuId" filterable placeholder="搜索 SKU" style="width:100%" @change="handleSkuChange(row)">
                <el-option v-for="sku in skuOptions" :key="sku.skuId" :value="sku.skuId" :label="`${sku.skuCode} · ${sku.skuName}`"><span>{{ sku.skuCode }} · {{ sku.skuName }}</span><span class="option-meta">成本 ¥{{ formatMoney(sku.costPrice) }}</span></el-option>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="126" align="center"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="1" :max="999999" controls-position="right" /></template></el-table-column>
          <el-table-column label="未税单价" width="145" align="center"><template #default="{ row }"><el-input-number v-model="row.purchasePrice" :min="0" :precision="2" :step="1" controls-position="right" /></template></el-table-column>
          <el-table-column label="税率" width="110" align="center"><template #default="{ row }"><el-select v-model="row.taxRate"><el-option v-for="rate in taxRates" :key="rate.value" :value="rate.value" :label="rate.label" /></el-select></template></el-table-column>
          <el-table-column label="税额" width="118" align="right"><template #default="{ row }">¥{{ formatMoney(lineTax(row)) }}</template></el-table-column>
          <el-table-column label="价税合计" width="132" align="right"><template #default="{ row }"><strong>¥{{ formatMoney(lineTotal(row)) }}</strong></template></el-table-column>
          <el-table-column label="历史" width="70" align="center"><template #default="{ row }"><el-button v-if="row.skuId" link type="primary" icon="Clock" @click="showPriceHistory(row)">价格</el-button></template></el-table-column>
          <el-table-column v-if="dialogMode === 'view'" label="已到货" width="90" align="center" prop="receivedQty" />
          <el-table-column v-else label="" width="54" align="center"><template #default="{ $index }"><el-button link type="danger" icon="Delete" @click="removeItem($index)" /></template></el-table-column>
        </el-table>

        <div class="order-total">
          <span>未税金额 <b>¥{{ formatMoney(orderTotals.amount) }}</b></span>
          <span>税额 <b>¥{{ formatMoney(orderTotals.tax) }}</b></span>
          <span class="grand-total">价税合计 <b>¥{{ formatMoney(orderTotals.total) }}</b></span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="cancel">{{ dialogMode === 'view' ? '关闭' : '取消' }}</el-button>
        <el-button v-if="dialogMode !== 'view'" type="primary" :loading="saving" @click="submitForm">保存草稿</el-button>
      </template>
    </el-dialog>

    <el-dialog title="登记采购到货" v-model="receiveOpen" width="820px" append-to-body destroy-on-close>
      <div class="receive-order-info">
        <div><span>采购单</span><strong>{{ receivingOrder.purchaseNo }}</strong></div>
        <div><span>供应商</span><strong>{{ receivingOrder.supplierName }}</strong></div>
        <div><span>入库仓</span><strong>{{ receivingOrder.warehouseName }}</strong></div>
      </div>
      <el-table :data="receiveForm.items" border empty-text="该订单已无待收商品">
        <el-table-column label="SKU" min-width="220"><template #default="{ row }"><strong>{{ row.skuCode }}</strong><div class="cell-meta">{{ row.skuName }}</div></template></el-table-column>
        <el-table-column label="采购" width="72" prop="quantity" align="center" />
        <el-table-column label="已到" width="72" prop="receivedQty" align="center" />
        <el-table-column label="未到" width="72" align="center"><template #default="{ row }">{{ row.remainingQty }}</template></el-table-column>
        <el-table-column label="本次到货" width="142" align="center"><template #default="{ row }"><el-input-number v-model="row.receiveQty" :min="0" :max="row.remainingQty" controls-position="right" /></template></el-table-column>
        <el-table-column label="批次号" min-width="160"><template #default="{ row }"><el-input v-model="row.batchNo" placeholder="不填则自动生成" /></template></el-table-column>
      </el-table>
      <el-input v-model="receiveForm.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="本次到货备注" class="receive-remark" />
      <template #footer><el-button @click="receiveOpen = false">取消</el-button><el-button type="primary" :loading="receiveSaving" @click="submitReceive">确认入库</el-button></template>
    </el-dialog>

    <el-dialog :title="`${historySku.skuCode || ''} 历史采购价格`" v-model="historyOpen" width="860px" append-to-body destroy-on-close>
      <div class="history-tip">每次采购单价按订单快照保存，不会覆盖以前批次；库存成本在收货时按移动加权平均重新计算。</div>
      <el-table v-loading="historyLoading" :data="historyRows" empty-text="暂无历史采购记录" max-height="480">
        <el-table-column label="采购日期" prop="purchaseDate" width="112" />
        <el-table-column label="采购单号" prop="purchaseNo" min-width="170" show-overflow-tooltip />
        <el-table-column label="供应商" prop="supplierName" min-width="180" show-overflow-tooltip />
        <el-table-column label="数量" prop="quantity" width="82" align="right" />
        <el-table-column label="未税单价" width="110" align="right"><template #default="{ row }">¥{{ formatMoney(row.purchasePrice) }}</template></el-table-column>
        <el-table-column label="税率" width="76" align="right"><template #default="{ row }">{{ Number(row.taxRate || 0) * 100 }}%</template></el-table-column>
        <el-table-column label="状态" width="92" align="center"><template #default="{ row }"><el-tag :type="statusType(row.purchaseStatus)" size="small">{{ formatStatus(row.purchaseStatus) }}</el-tag></template></el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="ErpPurchaseOrder">
import { listPurchase, getPurchase, getPurchaseSummary, getSkuPurchaseHistory, addPurchase, updatePurchase, updatePurchaseStatus, receivePurchase, delPurchase } from '@/api/erp/purchase'
import { listWarehouse } from '@/api/erp/warehouse'
import { listSupplier } from '@/api/erp/supplier'
import { listSku } from '@/api/erp/sku'

const { proxy } = getCurrentInstance()
const purchaseList = ref([])
const warehouseOptions = ref([])
const supplierOptions = ref([])
const skuOptions = ref([])
const loading = ref(false)
const saving = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const dialogMode = ref('edit')
const summary = ref({})
const receiveOpen = ref(false)
const receiveSaving = ref(false)
const receivingOrder = ref({})
const receiveForm = reactive({ items: [], remark: undefined })
const historyOpen = ref(false)
const historyLoading = ref(false)
const historyRows = ref([])
const historySku = ref({})
const purchaseDateRange = ref([])

const statusOptions = [
  { label: '草稿', value: 'draft' }, { label: '待审批', value: 'submitted' },
  { label: '已审批', value: 'approved' }, { label: '收货中', value: 'receiving' },
  { label: '已完成', value: 'completed' }, { label: '已关闭', value: 'closed' }
]
const taxRates = [
  { label: '免税', value: 0 }, { label: '1%', value: 0.01 }, { label: '3%', value: 0.03 },
  { label: '6%', value: 0.06 }, { label: '9%', value: 0.09 }, { label: '13%', value: 0.13 }
]
const statusActions = {
  draft: [{ label: '提交审批', value: 'submitted' }, { label: '关闭订单', value: 'closed' }],
  submitted: [{ label: '审批通过', value: 'approved' }, { label: '撤回草稿', value: 'draft' }, { label: '关闭订单', value: 'closed' }],
  approved: [{ label: '关闭订单', value: 'closed' }],
  processing: [{ label: '关闭订单', value: 'closed' }],
  receiving: [{ label: '关闭订单', value: 'closed' }],
  done: [{ label: '关闭订单', value: 'closed' }]
}

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, purchaseNo: undefined, supplierName: undefined, warehouseId: undefined, purchaseStatus: undefined },
  rules: {
    purchaseNo: [{ required: true, message: '采购单号不能为空', trigger: 'blur' }],
    purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'change' }],
    supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
    warehouseId: [{ required: true, message: '请选择入库仓', trigger: 'change' }],
    expectedTime: [{ required: true, message: '请选择预计到货时间', trigger: 'change' }]
  }
})
const { queryParams, form, rules } = toRefs(data)
const receivingWarehouses = computed(() => warehouseOptions.value.filter(item => !['return', 'defective'].includes(item.warehouseUsage)))
const orderTotals = computed(() => (form.value.items || []).reduce((result, row) => {
  result.amount += lineAmount(row); result.tax += lineTax(row); result.total += lineTotal(row); return result
}, { amount: 0, tax: 0, total: 0 }))

function formatMoney(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function formatStatus(value) { return ({ processing: '处理中', done: '已完成' }[value] || statusOptions.find(item => item.value === value)?.label || value || '-') }
function statusType(value) { return ({ draft: 'info', submitted: 'warning', approved: 'primary', processing: 'primary', receiving: 'primary', completed: 'success', done: 'success', closed: 'info' })[value] || 'info' }
function isCloudWarehouse(row) { return ['cloud', 'third_party'].includes(row.warehouseType) }
function availableActions(row) { return statusActions[row.purchaseStatus] || [] }
function canReceive(row) { return ['approved', 'processing', 'receiving'].includes(row.purchaseStatus) && Number(row.receivedQuantity || 0) < Number(row.totalQuantity || 0) }
function lineAmount(row) { return Number(row.quantity || 0) * Number(row.purchasePrice || 0) }
function lineTax(row) { return lineAmount(row) * Number(row.taxRate || 0) }
function lineTotal(row) { return lineAmount(row) + lineTax(row) }
function generatePurchaseNo() { const now = new Date(); const digits = [now.getFullYear(), now.getMonth() + 1, now.getDate(), now.getHours(), now.getMinutes(), now.getSeconds()].map((v, i) => i === 0 ? v : String(v).padStart(2, '0')).join(''); return `PO${digits}${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}` }
function formatDateTime(date) { const p = v => String(v).padStart(2, '0'); return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())} ${p(date.getHours())}:${p(date.getMinutes())}:${p(date.getSeconds())}` }
function formatDate(date) { const p = v => String(v).padStart(2, '0'); return `${date.getFullYear()}-${p(date.getMonth() + 1)}-${p(date.getDate())}` }

async function loadOptions() {
  const [warehouseRes, supplierRes, skuRes] = await Promise.all([
    listWarehouse({ pageNum: 1, pageSize: 500, status: '0' }),
    listSupplier({ pageNum: 1, pageSize: 500, status: '0' }),
    listSku({ pageNum: 1, pageSize: 1000, status: '0' })
  ])
  warehouseOptions.value = warehouseRes.rows || []; supplierOptions.value = supplierRes.rows || []; skuOptions.value = skuRes.rows || []
}
async function getList() { loading.value = true; try { const response = await listPurchase(proxy.addDateRange(queryParams.value, purchaseDateRange.value, 'PurchaseDate')); purchaseList.value = response.rows || []; total.value = response.total || 0 } finally { loading.value = false } }
async function loadSummary() { const response = await getPurchaseSummary(); summary.value = response.data || {} }
function refreshPage() { getList(); loadSummary() }
function reset() {
  const warehouse = receivingWarehouses.value[0]
  form.value = { purchaseId: undefined, purchaseNo: generatePurchaseNo(), purchaseDate: formatDate(new Date()), supplierId: undefined, warehouseId: warehouse?.warehouseId, expectedTime: undefined, remark: undefined, items: [] }
  proxy.resetForm('purchaseRef')
}
function handleSupplierChange(supplierId) { const supplier = supplierOptions.value.find(item => item.supplierId === supplierId); if (supplier && !form.value.expectedTime) { const date = new Date(); date.setDate(date.getDate() + Number(supplier.leadTimeDays || 7)); form.value.expectedTime = formatDateTime(date) } }
function addItem() { form.value.items.push({ skuId: undefined, quantity: 1, receivedQty: 0, purchasePrice: 0, taxRate: 0.13 }) }
function removeItem(index) { form.value.items.splice(index, 1) }
function handleSkuChange(row) { const sku = skuOptions.value.find(item => item.skuId === row.skuId); if (sku) { row.skuCode = sku.skuCode; row.skuName = sku.skuName; row.purchasePrice = Number(sku.costPrice || 0) } }
async function showPriceHistory(row) {
  historySku.value = row
  historyRows.value = []
  historyOpen.value = true
  historyLoading.value = true
  try { const response = await getSkuPurchaseHistory(row.skuId, 30); historyRows.value = response.data || [] } finally { historyLoading.value = false }
}
function handleAdd() { reset(); addItem(); dialogMode.value = 'edit'; title.value = '新建采购订单'; open.value = true }
async function loadOrder(row, mode) { reset(); const purchaseId = row?.purchaseId || ids.value[0]; const response = await getPurchase(purchaseId); form.value = { ...response.data, items: response.data.items || [] }; dialogMode.value = mode; title.value = mode === 'view' ? '采购订单详情' : '修改采购订单'; open.value = true }
function handleView(row) { loadOrder(row, 'view') }
function handleUpdate(row) { loadOrder(row, 'edit') }
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; refreshPage() }
function resetQuery() { purchaseDateRange.value = []; proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.purchaseId); single.value = selection.length !== 1 || (selection[0] && selection[0].purchaseStatus !== 'draft'); multiple.value = !selection.length || selection.some(item => item.purchaseStatus !== 'draft') }
async function submitForm() {
  await proxy.$refs.purchaseRef.validate()
  if (!form.value.items?.length || form.value.items.some(item => !item.skuId)) { proxy.$modal.msgError('请完善采购商品明细'); return }
  saving.value = true
  try { const request = form.value.purchaseId ? updatePurchase(form.value) : addPurchase(form.value); await request; proxy.$modal.msgSuccess(form.value.purchaseId ? '采购单已修改' : '采购单已创建'); open.value = false; refreshPage() } finally { saving.value = false }
}
function handleDelete(row) { const purchaseIds = row?.purchaseId || ids.value; proxy.$modal.confirm('确认删除选中的草稿采购单吗？').then(() => delPurchase(purchaseIds)).then(() => { proxy.$modal.msgSuccess('删除成功'); refreshPage() }).catch(() => {}) }
function handleStatus(row, targetStatus) { const label = statusActions[row.purchaseStatus]?.find(item => item.value === targetStatus)?.label || '更新状态'; proxy.$modal.confirm(`确认对采购单 ${row.purchaseNo} 执行“${label}”吗？`).then(() => updatePurchaseStatus(row.purchaseId, targetStatus)).then(() => { proxy.$modal.msgSuccess('状态已更新'); refreshPage() }).catch(() => {}) }
async function handleReceive(row) {
  const response = await getPurchase(row.purchaseId)
  receivingOrder.value = response.data
  receiveForm.items = (response.data.items || []).filter(item => Number(item.receivedQty || 0) < Number(item.quantity || 0)).map(item => ({ ...item, remainingQty: Number(item.quantity || 0) - Number(item.receivedQty || 0), receiveQty: 0, batchNo: undefined }))
  receiveForm.remark = undefined
  receiveOpen.value = true
}
async function submitReceive() {
  const items = receiveForm.items.filter(item => Number(item.receiveQty || 0) > 0).map(item => ({ itemId: item.itemId, quantity: item.receiveQty, batchNo: item.batchNo }))
  if (!items.length) { proxy.$modal.msgError('请至少填写一条本次到货数量'); return }
  receiveSaving.value = true
  try { await receivePurchase(receivingOrder.value.purchaseId, { items, remark: receiveForm.remark }); proxy.$modal.msgSuccess('采购到货已入库'); receiveOpen.value = false; refreshPage() } finally { receiveSaving.value = false }
}

loadOptions().then(() => reset())
refreshPage()
</script>

<style scoped>
.purchase-page { --accent: #6c5ce7; }
.page-intro, .filter-panel, .table-panel { background: rgba(255,255,255,.92); border: 1px solid rgba(108,92,231,.09); box-shadow: 0 12px 30px rgba(42,39,90,.06); }
.page-intro { display:flex; align-items:center; justify-content:space-between; padding:20px 24px; border-radius:16px; margin-bottom:14px; }
.eyebrow { color:var(--accent); font-size:12px; font-weight:800; letter-spacing:.08em; }
.page-intro h2 { margin:4px 0 2px; font-size:24px; color:#24283a; }
.page-intro p { margin:0; color:#7b849e; }
.summary-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:12px; margin-bottom:14px; }
.summary-card { padding:16px 18px; border-radius:14px; background:#fff; border:1px solid #eceefa; box-shadow:0 8px 24px rgba(42,39,90,.05); }
.summary-card span,.summary-card small { display:block; color:#858da5; }
.summary-card strong { display:block; margin:6px 0 3px; font-size:24px; color:#252a3d; }
.summary-card.warning strong { color:#d98a18; }.summary-card.danger strong{color:#e85d75}.summary-card.money strong{color:#00a889}
.filter-panel { border-radius:14px; padding:15px 18px 0; margin-bottom:14px; }
.filter-panel :deep(.el-input),.filter-panel :deep(.el-select){width:190px}
.table-panel { border-radius:16px; padding:14px 14px 4px; }
.table-toolbar,.toolbar-actions,.line-header,.order-total { display:flex; align-items:center; }
.table-toolbar { justify-content:space-between; margin-bottom:12px; }.toolbar-actions{gap:8px}
.order-link { border:0; padding:0; background:transparent; color:var(--accent); font-weight:700; cursor:pointer; }
.cell-meta { margin-top:4px; color:#959db2; font-size:12px; }.warehouse-name{margin-left:7px}.overdue-text{color:#df4f67}.overdue-tag{margin-left:6px}
.form-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:0 18px; }
.line-header { justify-content:space-between; margin:12px 0 10px; }.line-header h3{display:inline;margin:0 12px 0 0;font-size:16px}.line-header span{font-size:12px;color:#8a92a9}
.item-table :deep(.el-input-number){width:100%}.option-meta{float:right;margin-left:24px;color:#9aa2b8;font-size:12px}
.order-total { justify-content:flex-end; gap:28px; padding:16px 8px 0; color:#737c95; }.order-total b{margin-left:8px;color:#31364b}.grand-total b{font-size:21px;color:var(--accent)}
.receive-order-info{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-bottom:14px}.receive-order-info div{padding:12px 14px;border-radius:10px;background:#f7f7fd}.receive-order-info span{display:block;margin-bottom:5px;color:#9199ae;font-size:12px}.receive-order-info strong{color:#34394d}.receive-remark{margin-top:14px}
.history-tip{margin-bottom:12px;padding:10px 12px;border-radius:9px;background:#f5f3ff;color:#686f86;font-size:13px}
:deep(.danger-action){color:#e85d75}
@media (max-width:1100px){.summary-grid{grid-template-columns:repeat(2,1fr)}.form-grid{grid-template-columns:1fr}.page-intro{align-items:flex-start}.page-intro p{max-width:580px}}
@media (max-width:700px){.summary-grid{grid-template-columns:1fr}.page-intro{gap:16px}.page-intro p{display:none}}
</style>
