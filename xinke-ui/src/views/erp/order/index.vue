<template>
  <div class="app-container fulfillment-page">
    <section class="page-hero">
      <div>
        <div class="eyebrow">SALES FULFILLMENT</div>
        <h1>销售履约中心</h1>
        <p>按主订单管理支付，按子订单管理商品，用包裹连接仓库、物流、售后与利润。</p>
      </div>
      <div class="hero-actions">
        <el-button icon="Refresh" @click="refreshAll">刷新</el-button>
        <el-button v-hasPermi="['erp:order:sync']" type="primary" icon="Connection" :loading="syncing" @click="handleSync">同步订单</el-button>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card total"><span>主订单</span><strong>{{ number(summary.orderCount) }}</strong><small>当前筛选范围</small></article>
      <article class="metric-card amount"><span>成交金额</span><strong>¥{{ money(summary.payAmount) }}</strong><small>主订单支付合计</small></article>
      <article class="metric-card pending"><span>待发商品</span><strong>{{ number(summary.pendingQuantity) }}</strong><small>{{ number(summary.pendingCount) }} 个待处理订单</small></article>
      <article class="metric-card shipping"><span>部分发货</span><strong>{{ number(summary.partialShippedCount) }}</strong><small>仍需继续拆包发货</small></article>
      <article class="metric-card after-sale"><span>售后订单</span><strong>{{ number(summary.afterSaleCount) }}</strong><small>含部分售后与全额退款</small></article>
    </section>

    <section class="work-panel">
      <el-form ref="queryRef" :model="queryParams" :inline="true" class="filter-form">
        <el-form-item label="主订单" prop="mainOrderNo">
          <el-input v-model="queryParams.mainOrderNo" placeholder="搜索主订单号" clearable style="width: 190px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="子订单" prop="subOrderNo">
          <el-input v-model="queryParams.subOrderNo" placeholder="搜索子订单号" clearable style="width: 190px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="店铺" prop="shopName">
          <el-input v-model="queryParams.shopName" placeholder="店铺名称" clearable style="width: 160px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="商品" prop="productName">
          <el-input v-model="queryParams.productName" placeholder="商品、SKU或编码" clearable style="width: 190px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="履约状态" prop="fulfillmentStatus">
          <el-select v-model="queryParams.fulfillmentStatus" placeholder="全部状态" clearable style="width: 150px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="loading" :data="orderList" row-key="salesId" class="order-table" @row-dblclick="openDetail">
        <el-table-column label="主订单" min-width="190" fixed="left">
          <template #default="{ row }">
            <button class="order-link" @click="openDetail(row)">{{ row.mainOrderNo }}</button>
            <div class="cell-meta">{{ formatDate(row.payTime) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="店铺" prop="shopName" min-width="140" show-overflow-tooltip />
        <el-table-column label="商品结构" min-width="135">
          <template #default="{ row }">
            <strong>{{ number(row.itemCount) }} 个子订单</strong>
            <div class="cell-meta">共 {{ number(row.totalQuantity) }} 件</div>
          </template>
        </el-table-column>
        <el-table-column label="成交/退款" min-width="150" align="right">
          <template #default="{ row }">
            <strong>¥{{ money(row.payAmount) }}</strong>
            <div class="cell-meta refund-text">退款 ¥{{ money(row.refundAmount) }}</div>
          </template>
        </el-table-column>
        <el-table-column label="履约进度" min-width="210">
          <template #default="{ row }">
            <el-progress :percentage="progress(row)" :stroke-width="7" :show-text="false" />
            <div class="progress-meta">
              <span>已发 {{ number(row.shippedQuantity) }}</span>
              <span>售后 {{ number(row.refundedQuantity) }}</span>
              <span>待处理 {{ pendingQuantity(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.fulfillmentStatus)" effect="light" round>{{ statusLabel(row.fulfillmentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="105" fixed="right" align="center">
          <template #default="{ row }">
            <el-button link type="primary" icon="View" @click="openDetail(row)">履约详情</el-button>
          </template>
        </el-table-column>
        <template #empty><el-empty description="暂无销售订单，点击右上角同步订单" :image-size="80" /></template>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-drawer v-model="detailVisible" size="88%" :with-header="false" destroy-on-close class="fulfillment-drawer">
      <div v-loading="detailLoading" class="drawer-content">
        <header class="drawer-header">
          <div>
            <div class="drawer-kicker">主订单 {{ detail.order?.mainOrderNo || '-' }}</div>
            <h2>{{ detail.order?.shopName || '销售履约详情' }}</h2>
            <div class="drawer-meta">
              <el-tag :type="statusType(detail.order?.fulfillmentStatus)" round>{{ statusLabel(detail.order?.fulfillmentStatus) }}</el-tag>
              <span>支付时间 {{ formatDate(detail.order?.payTime) }}</span>
              <span>成交 ¥{{ money(detail.order?.payAmount) }}</span>
            </div>
          </div>
          <div class="drawer-actions">
            <el-button v-hasPermi="['erp:order:after-sale']" icon="RefreshLeft" :disabled="!detail.items?.length" @click="openAfterSale">发起售后</el-button>
            <el-button v-hasPermi="['erp:order:shipment']" type="primary" icon="Van" :disabled="!hasShippableItem" @click="openShipment">创建包裹</el-button>
            <el-button circle icon="Close" @click="detailVisible = false" />
          </div>
        </header>

        <div class="detail-metrics">
          <div><span>商品件数</span><strong>{{ number(detail.order?.totalQuantity) }}</strong></div>
          <div><span>已发货</span><strong>{{ number(detail.order?.shippedQuantity) }}</strong></div>
          <div><span>已售后</span><strong>{{ number(detail.order?.refundedQuantity) }}</strong></div>
          <div><span>待处理</span><strong>{{ detailPendingQuantity }}</strong></div>
        </div>

        <el-tabs v-model="activeTab" class="detail-tabs">
          <el-tab-pane :label="`商品明细 ${detail.items?.length || 0}`" name="items">
            <el-table :data="detail.items" row-key="salesItemId" class="detail-table">
              <el-table-column label="子订单号" prop="subOrderNo" min-width="190" show-overflow-tooltip />
              <el-table-column label="商品/SKU" min-width="220">
                <template #default="{ row }"><strong>{{ row.productName || row.skuName }}</strong><div class="cell-meta">{{ row.skuCode || '-' }} · {{ row.skuName }}</div></template>
              </el-table-column>
              <el-table-column label="购买" prop="quantity" width="72" align="right" />
              <el-table-column label="已发" prop="shippedQuantity" width="72" align="right" />
              <el-table-column label="售后" prop="refundedQuantity" width="72" align="right" />
              <el-table-column label="待处理" width="82" align="right"><template #default="{ row }"><strong>{{ row.pendingQuantity }}</strong></template></el-table-column>
              <el-table-column label="成交金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.payAmount) }}</template></el-table-column>
              <el-table-column label="状态" width="115" align="center"><template #default="{ row }"><el-tag :type="itemStatusType(row.itemStatus)" round>{{ itemStatusLabel(row.itemStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="90" align="center"><template #default="{ row }"><el-button v-hasPermi="['erp:order:after-sale']" link type="primary" :disabled="afterSaleMax(row, 'refund_only') <= 0" @click="openAfterSale(row)">售后</el-button></template></el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`发货包裹 ${detail.shipments?.length || 0}`" name="shipments">
            <div class="tab-toolbar"><el-button v-hasPermi="['erp:order:shipment']" type="primary" plain icon="Plus" :disabled="!hasShippableItem" @click="openShipment">创建发货包裹</el-button></div>
            <el-table :data="detail.shipments" row-key="shipmentId" class="detail-table">
              <el-table-column label="包裹/发货单" min-width="190"><template #default="{ row }"><strong>{{ row.shipmentNo }}</strong><div class="cell-meta">{{ formatDate(row.createTime) }}</div></template></el-table-column>
              <el-table-column label="发货仓" prop="warehouseName" min-width="130" />
              <el-table-column label="商品" min-width="230" show-overflow-tooltip><template #default="{ row }">{{ shipmentItemsText(row.items) }}</template></el-table-column>
              <el-table-column label="物流" min-width="190"><template #default="{ row }"><span>{{ row.logisticsCompany || '-' }}</span><div class="cell-meta">{{ row.logisticsNo || '待填写' }}</div></template></el-table-column>
              <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="shipmentStatusType(row.shipmentStatus)" round>{{ shipmentStatusLabel(row.shipmentStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="操作" min-width="170" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.shipmentStatus === 'draft'" v-hasPermi="['erp:order:shipment']" link type="primary" @click="handleShip(row)">确认发货</el-button>
                  <el-button v-if="row.shipmentStatus === 'draft'" v-hasPermi="['erp:order:shipment']" link type="danger" @click="handleCancelShipment(row)">取消</el-button>
                  <el-button v-if="row.shipmentStatus === 'shipped'" v-hasPermi="['erp:order:shipment']" link type="success" @click="handleSign(row)">确认签收</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`售后记录 ${detail.afterSales?.length || 0}`" name="afterSales">
            <el-table :data="detail.afterSales" row-key="afterSaleId" class="detail-table">
              <el-table-column label="售后单" min-width="190"><template #default="{ row }"><strong>{{ row.afterSaleNo }}</strong><div class="cell-meta">{{ row.subOrderNo }}</div></template></el-table-column>
              <el-table-column label="商品" prop="skuName" min-width="170" show-overflow-tooltip />
              <el-table-column label="类型" width="110"><template #default="{ row }">{{ afterSaleTypeLabel(row.afterSaleType) }}</template></el-table-column>
              <el-table-column label="数量" prop="quantity" width="75" align="right" />
              <el-table-column label="退款金额" width="120" align="right"><template #default="{ row }">¥{{ money(row.refundAmount) }}</template></el-table-column>
              <el-table-column label="原因" prop="reason" min-width="190" show-overflow-tooltip />
              <el-table-column label="状态" width="100" align="center"><template #default="{ row }"><el-tag :type="afterSaleStatusType(row.afterSaleStatus)" round>{{ afterSaleStatusLabel(row.afterSaleStatus) }}</el-tag></template></el-table-column>
              <el-table-column label="操作" width="150" align="center">
                <template #default="{ row }">
                  <el-button v-if="row.afterSaleStatus === 'pending'" v-hasPermi="['erp:order:after-sale']" link type="primary" @click="handleApproveAfterSale(row)">审核通过</el-button>
                  <el-button v-if="row.afterSaleStatus === 'pending'" v-hasPermi="['erp:order:after-sale']" link type="danger" @click="handleRejectAfterSale(row)">驳回</el-button>
                  <el-button v-if="row.afterSaleStatus === 'approved'" v-hasPermi="['erp:order:after-sale']" link type="success" @click="handleCompleteAfterSale(row)">完成售后</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane :label="`操作日志 ${detail.logs?.length || 0}`" name="logs">
            <el-timeline class="log-timeline">
              <el-timeline-item v-for="log in detail.logs" :key="log.logId" :timestamp="formatDate(log.actionTime)" placement="top">
                <div class="log-title">{{ actionLabel(log.actionType) }} · {{ log.bizNo }}</div>
                <div class="cell-meta">{{ log.operatorName || '系统' }} · {{ log.detail || `${log.fromStatus || '-'} → ${log.toStatus || '-'}` }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="!detail.logs?.length" description="暂无操作日志" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-drawer>

    <el-dialog v-model="shipmentVisible" title="创建发货包裹" width="760px" append-to-body destroy-on-close>
      <el-form ref="shipmentFormRef" :model="shipmentForm" :rules="shipmentRules" label-width="92px">
        <div class="dialog-grid">
          <el-form-item label="发货仓库" prop="warehouseId">
            <el-select v-model="shipmentForm.warehouseId" placeholder="选择实际扣库存的仓库" style="width:100%">
              <el-option v-for="item in detail.warehouses" :key="item.warehouseId" :label="warehouseLabel(item)" :value="item.warehouseId" />
            </el-select>
          </el-form-item>
          <el-form-item label="物流公司" prop="logisticsCompany"><el-input v-model="shipmentForm.logisticsCompany" placeholder="例如 顺丰速运" /></el-form-item>
          <el-form-item label="物流单号" prop="logisticsNo"><el-input v-model="shipmentForm.logisticsNo" placeholder="确认发货前必须填写" /></el-form-item>
          <el-form-item label="备注"><el-input v-model="shipmentForm.remark" placeholder="拆包、加急等说明" /></el-form-item>
        </div>
        <div class="dialog-section-title">选择本包裹商品</div>
        <el-table :data="shipmentForm.items" size="small">
          <el-table-column label="子订单" prop="subOrderNo" min-width="180" />
          <el-table-column label="商品" prop="skuName" min-width="170" />
          <el-table-column label="可发" prop="pendingQuantity" width="70" align="right" />
          <el-table-column label="本次发货" width="150" align="center">
            <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :max="Number(row.pendingQuantity || 0)" controls-position="right" /></template>
          </el-table-column>
        </el-table>
      </el-form>
      <template #footer><el-button @click="shipmentVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitShipment">保存草稿</el-button></template>
    </el-dialog>

    <el-dialog v-model="afterSaleVisible" title="发起订单售后" width="620px" append-to-body destroy-on-close>
      <el-form ref="afterSaleFormRef" :model="afterSaleForm" :rules="afterSaleRules" label-width="100px">
        <el-form-item label="子订单" prop="salesItemId">
          <el-select v-model="afterSaleForm.salesItemId" placeholder="选择售后商品" style="width:100%" @change="handleAfterSaleItemChange">
            <el-option v-for="item in detail.items" :key="item.salesItemId" :value="item.salesItemId" :label="`${item.subOrderNo} · ${item.skuName}`" />
          </el-select>
        </el-form-item>
        <el-form-item label="售后类型" prop="afterSaleType">
          <el-radio-group v-model="afterSaleForm.afterSaleType" @change="normalizeAfterSaleQuantity">
            <el-radio-button value="refund_only">仅退款</el-radio-button>
            <el-radio-button value="return_refund">退货退款</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="售后数量" prop="quantity">
          <el-input-number v-model="afterSaleForm.quantity" :min="1" :max="currentAfterSaleMax" controls-position="right" />
          <span class="form-help">最多 {{ currentAfterSaleMax }} 件</span>
        </el-form-item>
        <el-form-item label="退款金额" prop="refundAmount"><el-input-number v-model="afterSaleForm.refundAmount" :min="0" :precision="2" :step="10" controls-position="right" /></el-form-item>
        <el-form-item v-if="afterSaleForm.afterSaleType === 'return_refund'" label="退货仓库" prop="returnWarehouseId">
          <el-select v-model="afterSaleForm.returnWarehouseId" placeholder="验收入库仓库" style="width:100%">
            <el-option v-for="item in detail.warehouses" :key="item.warehouseId" :label="warehouseLabel(item)" :value="item.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="售后原因" prop="reason"><el-input v-model="afterSaleForm.reason" type="textarea" :rows="3" maxlength="255" show-word-limit placeholder="说明退款或退货原因" /></el-form-item>
        <el-form-item label="内部备注"><el-input v-model="afterSaleForm.remark" placeholder="仅内部人员可见" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="afterSaleVisible = false">取消</el-button><el-button type="primary" :loading="submitting" @click="submitAfterSale">提交审核</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpOrder">
import { ElMessageBox } from 'element-plus'
import {
  approveAfterSale, cancelShipment, completeAfterSale, createAfterSale, createShipment, rejectAfterSale,
  getFulfillmentDetail, getFulfillmentSummary, listFulfillmentOrders,
  shipShipment, signShipment, syncFulfillmentOrders
} from '@/api/erp/order'

const { proxy } = getCurrentInstance()
const loading = ref(false)
const syncing = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const orderList = ref([])
const total = ref(0)
const summary = ref({})
const detailVisible = ref(false)
const shipmentVisible = ref(false)
const afterSaleVisible = ref(false)
const activeTab = ref('items')
const detail = ref({ order: {}, items: [], shipments: [], afterSales: [], warehouses: [], logs: [] })
const shipmentForm = ref({ items: [] })
const afterSaleForm = ref({})
const queryParams = ref({ pageNum: 1, pageSize: 10, mainOrderNo: undefined, subOrderNo: undefined, shopName: undefined, productName: undefined, fulfillmentStatus: undefined })

const statusOptions = [
  { label: '待发货', value: 'pending' }, { label: '部分发货', value: 'partial_shipped' },
  { label: '已发货', value: 'shipped' }, { label: '售后处理中', value: 'after_sale' },
  { label: '已退款', value: 'refunded' }
]
const shipmentRules = {
  warehouseId: [{ required: true, message: '请选择发货仓库', trigger: 'change' }],
  logisticsNo: [{ required: true, message: '请输入物流单号', trigger: 'blur' }]
}
const afterSaleRules = {
  salesItemId: [{ required: true, message: '请选择售后商品', trigger: 'change' }],
  afterSaleType: [{ required: true, message: '请选择售后类型', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入售后数量', trigger: 'blur' }],
  refundAmount: [{ required: true, message: '请输入退款金额', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入售后原因', trigger: 'blur' }]
}

const hasShippableItem = computed(() => detail.value.items?.some(item => Number(item.pendingQuantity || 0) > 0))
const detailPendingQuantity = computed(() => Math.max(0, Number(detail.value.order?.totalQuantity || 0) - Number(detail.value.order?.shippedQuantity || 0) - Number(detail.value.order?.refundedQuantity || 0)))
const selectedAfterSaleItem = computed(() => detail.value.items?.find(item => item.salesItemId === afterSaleForm.value.salesItemId))
const currentAfterSaleMax = computed(() => Math.max(0, afterSaleMax(selectedAfterSaleItem.value, afterSaleForm.value.afterSaleType)))

async function getList() {
  loading.value = true
  try {
    const response = await listFulfillmentOrders(queryParams.value)
    orderList.value = response.rows || []
    total.value = response.total || 0
  } finally { loading.value = false }
}

async function loadSummary() {
  const response = await getFulfillmentSummary(queryParams.value)
  summary.value = response.data || {}
}

async function refreshAll() { await Promise.all([getList(), loadSummary()]) }
function handleQuery() { queryParams.value.pageNum = 1; refreshAll() }
function resetQuery() { proxy.resetForm('queryRef'); queryParams.value.pageNum = 1; refreshAll() }

async function handleSync() {
  syncing.value = true
  try {
    const response = await syncFulfillmentOrders()
    proxy.$modal.msgSuccess(`同步完成：订单 ${response.data?.orderRows || 0}，商品 ${response.data?.itemRows || 0}`)
    await refreshAll()
  } finally { syncing.value = false }
}

async function openDetail(row, tab = 'items') {
  detailVisible.value = true
  detailLoading.value = true
  activeTab.value = tab
  try {
    const response = await getFulfillmentDetail(row.salesId)
    detail.value = response.data || { order: {}, items: [], shipments: [], afterSales: [], warehouses: [], logs: [] }
  } finally { detailLoading.value = false }
}

async function reloadDetail(tab = activeTab.value) {
  const salesId = detail.value.order?.salesId
  if (!salesId) return
  await openDetail({ salesId }, tab)
  await refreshAll()
}

function openShipment() {
  shipmentForm.value = {
    salesId: detail.value.order.salesId,
    warehouseId: detail.value.warehouses?.[0]?.warehouseId,
    logisticsCompany: '', logisticsNo: '', remark: '',
    idempotencyKey: clientKey('shipment'),
    items: detail.value.items.filter(item => Number(item.pendingQuantity || 0) > 0).map(item => ({
      salesItemId: item.salesItemId, subOrderNo: item.subOrderNo, skuName: item.skuName,
      pendingQuantity: Number(item.pendingQuantity || 0), quantity: Number(item.pendingQuantity || 0)
    }))
  }
  shipmentVisible.value = true
}

function submitShipment() {
  proxy.$refs.shipmentFormRef.validate(async valid => {
    if (!valid) return
    const items = shipmentForm.value.items.filter(item => Number(item.quantity || 0) > 0).map(item => ({ salesItemId: item.salesItemId, quantity: item.quantity }))
    if (!items.length) return proxy.$modal.msgError('至少选择一个发货商品')
    submitting.value = true
    try {
      const response = await createShipment({ ...shipmentForm.value, items })
      proxy.$modal.msgSuccess(`发货单 ${response.data} 已保存为草稿`)
      shipmentVisible.value = false
      await reloadDetail('shipments')
    } finally { submitting.value = false }
  })
}

function handleShip(row) {
  proxy.$modal.confirm(`确认包裹 ${row.shipmentNo} 发货？确认后将扣减 ${row.warehouseName} 的库存。`).then(async () => {
    await shipShipment(row.shipmentNo)
    proxy.$modal.msgSuccess('发货成功，库存已扣减')
    await reloadDetail('shipments')
  })
}

function handleSign(row) {
  proxy.$modal.confirm(`确认包裹 ${row.shipmentNo} 已签收？`).then(async () => {
    await signShipment(row.shipmentNo)
    proxy.$modal.msgSuccess('包裹已签收')
    await reloadDetail('shipments')
  })
}

async function handleCancelShipment(row) {
  try {
    const result = await ElMessageBox.prompt(`取消草稿包裹 ${row.shipmentNo}，不会扣减库存。`, '取消发货单', {
      confirmButtonText: '确认取消', cancelButtonText: '返回', inputPlaceholder: '请输入取消原因',
      inputValidator: value => String(value || '').trim().length >= 2 || '取消原因至少2个字'
    })
    await cancelShipment(row.shipmentNo, result.value)
    proxy.$modal.msgSuccess('发货单已取消')
    await reloadDetail('shipments')
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

function openAfterSale(row) {
  const item = row?.salesItemId ? row : detail.value.items.find(candidate => afterSaleMax(candidate, 'refund_only') > 0)
  if (!item) return proxy.$modal.msgError('当前订单没有可售后商品')
  afterSaleForm.value = {
    salesItemId: item.salesItemId, afterSaleType: 'refund_only', quantity: 1,
    refundAmount: proportionalRefund(item, 1), returnWarehouseId: undefined,
    reason: '', remark: '', idempotencyKey: clientKey('after-sale')
  }
  afterSaleVisible.value = true
}

function handleAfterSaleItemChange() {
  afterSaleForm.value.quantity = 1
  afterSaleForm.value.refundAmount = proportionalRefund(selectedAfterSaleItem.value, 1)
}

function normalizeAfterSaleQuantity() {
  afterSaleForm.value.quantity = Math.min(Math.max(1, Number(afterSaleForm.value.quantity || 1)), currentAfterSaleMax.value || 1)
  afterSaleForm.value.refundAmount = proportionalRefund(selectedAfterSaleItem.value, afterSaleForm.value.quantity)
}

function submitAfterSale() {
  proxy.$refs.afterSaleFormRef.validate(async valid => {
    if (!valid) return
    if (currentAfterSaleMax.value <= 0 || afterSaleForm.value.quantity > currentAfterSaleMax.value) return proxy.$modal.msgError('售后数量超过可操作数量')
    if (afterSaleForm.value.afterSaleType === 'return_refund' && !afterSaleForm.value.returnWarehouseId) return proxy.$modal.msgError('退货退款必须选择退货仓库')
    submitting.value = true
    try {
      const response = await createAfterSale(afterSaleForm.value)
      proxy.$modal.msgSuccess(`售后单 ${response.data} 已提交审核`)
      afterSaleVisible.value = false
      await reloadDetail('afterSales')
    } finally { submitting.value = false }
  })
}

function handleApproveAfterSale(row) {
  proxy.$modal.confirm(`确认售后单 ${row.afterSaleNo} 审核通过？`).then(async () => {
    await approveAfterSale(row.afterSaleNo)
    proxy.$modal.msgSuccess('售后单已审核')
    await reloadDetail('afterSales')
  })
}

async function handleRejectAfterSale(row) {
  try {
    const result = await ElMessageBox.prompt(`驳回售后单 ${row.afterSaleNo}，该申请将释放占用的可售后数量和金额。`, '驳回售后单', {
      confirmButtonText: '确认驳回', cancelButtonText: '返回', inputPlaceholder: '请输入驳回原因',
      inputType: 'textarea', inputValidator: value => String(value || '').trim().length >= 2 || '驳回原因至少2个字'
    })
    await rejectAfterSale(row.afterSaleNo, result.value)
    proxy.$modal.msgSuccess('售后单已驳回')
    await reloadDetail('afterSales')
  } catch (error) { if (error !== 'cancel' && error !== 'close') throw error }
}

function handleCompleteAfterSale(row) {
  const inventoryTip = row.afterSaleType === 'return_refund' ? `，并回库至 ${row.returnWarehouseName}` : ''
  proxy.$modal.confirm(`确认完成售后单 ${row.afterSaleNo}${inventoryTip}？完成后将更新退款成本。`).then(async () => {
    await completeAfterSale(row.afterSaleNo)
    proxy.$modal.msgSuccess('售后已完成')
    await reloadDetail('afterSales')
  })
}

function number(value) { return Number(value || 0).toLocaleString('zh-CN') }
function money(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function formatDate(value) { return value ? String(value).replace('T', ' ').slice(0, 19) : '-' }
function pendingQuantity(row) { return Math.max(0, Number(row.totalQuantity || 0) - Number(row.shippedQuantity || 0) - Number(row.refundedQuantity || 0)) }
function progress(row) { return Math.min(100, Math.max(0, Number(row.processRate || 0))) }
function statusLabel(value) { return ({ pending: '待发货', partial_shipped: '部分发货', shipped: '已发货', after_sale: '售后处理中', refunded: '已退款' })[value] || value || '未知' }
function statusType(value) { return ({ pending: 'warning', partial_shipped: 'primary', shipped: 'success', after_sale: 'danger', refunded: 'info' })[value] || 'info' }
function itemStatusLabel(value) { return ({ paid: '待发货', partial_shipped: '部分发货', shipped: '已发货', partial_refunded: '部分售后', refunded: '已退款' })[value] || value || '未知' }
function itemStatusType(value) { return ({ paid: 'warning', partial_shipped: 'primary', shipped: 'success', partial_refunded: 'danger', refunded: 'info' })[value] || 'info' }
function shipmentStatusLabel(value) { return ({ draft: '草稿', shipped: '已发货', signed: '已签收', canceled: '已取消' })[value] || value || '-' }
function shipmentStatusType(value) { return ({ draft: 'info', shipped: 'primary', signed: 'success', canceled: 'danger' })[value] || 'info' }
function afterSaleTypeLabel(value) { return ({ refund_only: '仅退款', return_refund: '退货退款' })[value] || value || '-' }
function afterSaleStatusLabel(value) { return ({ pending: '待审核', approved: '已审核', completed: '已完成', rejected: '已驳回', canceled: '已取消' })[value] || value || '-' }
function afterSaleStatusType(value) { return ({ pending: 'warning', approved: 'primary', completed: 'success', rejected: 'danger', canceled: 'info' })[value] || 'info' }
function actionLabel(value) { return ({ create: '创建单据', ship: '确认发货', sign: '确认签收', cancel: '取消单据', approve: '审核通过', reject: '审核驳回', complete: '完成售后' })[value] || value || '操作' }
function shipmentItemsText(items = []) { return items.map(item => `${item.skuName || item.subOrderNo} ×${item.quantity}`).join('；') || '-' }
function warehouseLabel(item) { return `${item.warehouseName} · ${['cloud', 'third_party'].includes(item.warehouseType) ? '云仓' : '实体仓'}` }
function afterSaleMax(item, type) { if (!item) return 0; return type === 'return_refund' ? Number(item.shippedQuantity || 0) - Number(item.refundedQuantity || 0) : Number(item.quantity || 0) - Number(item.refundedQuantity || 0) }
function proportionalRefund(item, quantity) { if (!item || !item.quantity) return 0; return Number((Number(item.payAmount || 0) * Number(quantity || 0) / Number(item.quantity)).toFixed(2)) }
function clientKey(prefix) { return `${prefix}-${Date.now()}-${Math.random().toString(36).slice(2, 9)}` }

refreshAll()
</script>

<style scoped>
.fulfillment-page { min-width: 0; }
.page-hero, .work-panel { background: rgba(255,255,255,.94); border: 1px solid rgba(108,92,231,.11); border-radius: 14px; box-shadow: 0 10px 30px rgba(57,48,125,.07); }
.page-hero { display:flex; align-items:center; justify-content:space-between; gap:24px; padding:22px 26px; margin-bottom:14px; }
.eyebrow { color:#6c5ce7; font-size:12px; font-weight:800; letter-spacing:.06em; }
.page-hero h1 { margin:5px 0 4px; color:#252a3d; font-size:25px; }
.page-hero p { margin:0; color:#7c869e; }
.hero-actions { display:flex; flex:none; gap:10px; }
.metric-grid { display:grid; grid-template-columns:repeat(5,minmax(0,1fr)); gap:12px; margin-bottom:14px; }
.metric-card { position:relative; min-width:0; padding:15px 17px; overflow:hidden; background:#fff; border:1px solid #eceefa; border-radius:12px; }
.metric-card::before { position:absolute; inset:0 auto 0 0; width:4px; content:''; background:#6c5ce7; }
.metric-card.amount::before { background:#00b894; }.metric-card.pending::before { background:#fdcb6e; }.metric-card.shipping::before { background:#74b9ff; }.metric-card.after-sale::before { background:#ff7675; }
.metric-card span,.metric-card small { display:block; color:#8490a8; }.metric-card strong { display:block; margin:6px 0 4px; overflow:hidden; color:#252a3d; font-size:23px; white-space:nowrap; text-overflow:ellipsis; }
.work-panel { padding:16px; }.filter-form { padding:2px 2px 6px; }.filter-form :deep(.el-form-item) { margin-bottom:10px; }
.order-table :deep(.el-table__cell),.detail-table :deep(.el-table__cell) { padding:9px 0; }
.order-link { max-width:100%; padding:0; overflow:hidden; color:#5f55e7; font:inherit; font-weight:700; white-space:nowrap; text-overflow:ellipsis; cursor:pointer; background:none; border:0; }
.cell-meta { margin-top:3px; color:#929bb0; font-size:12px; }.refund-text { color:#e66a77; }
.progress-meta { display:flex; justify-content:space-between; gap:8px; margin-top:6px; color:#7f899f; font-size:11px; }
.drawer-content { min-height:100%; background:#f6f7fc; }.drawer-header { display:flex; align-items:flex-start; justify-content:space-between; gap:20px; padding:22px 26px; background:#fff; border-bottom:1px solid #e9ebf5; }
.drawer-kicker { color:#6c5ce7; font-size:12px; font-weight:750; }.drawer-header h2 { margin:5px 0 8px; color:#252a3d; font-size:23px; }.drawer-meta { display:flex; align-items:center; flex-wrap:wrap; gap:12px; color:#7e879e; font-size:13px; }.drawer-actions { display:flex; gap:9px; }
.detail-metrics { display:grid; grid-template-columns:repeat(4,1fr); gap:12px; padding:16px 26px 0; }.detail-metrics div { padding:13px 16px; background:#fff; border:1px solid #e9ebf5; border-radius:10px; }.detail-metrics span { display:block; color:#8992a9; font-size:12px; }.detail-metrics strong { display:block; margin-top:4px; color:#2a3043; font-size:20px; }
.detail-tabs { margin:16px 26px 26px; padding:0 16px 18px; background:#fff; border:1px solid #e9ebf5; border-radius:12px; }.tab-toolbar { display:flex; justify-content:flex-end; padding:0 0 10px; }
.log-timeline { max-width:820px; padding:12px 20px; }.log-title { color:#34394d; font-weight:650; }
.dialog-grid { display:grid; grid-template-columns:1fr 1fr; column-gap:18px; }.dialog-section-title { margin:4px 0 10px; color:#454b60; font-weight:700; }.form-help { margin-left:10px; color:#8d96aa; font-size:12px; }
@media (max-width:1200px) { .metric-grid { grid-template-columns:repeat(3,minmax(0,1fr)); }.drawer-actions { flex-wrap:wrap; justify-content:flex-end; } }
@media (max-width:768px) { .page-hero,.drawer-header { align-items:flex-start; flex-direction:column; }.hero-actions,.drawer-actions { width:100%; }.metric-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }.detail-metrics { grid-template-columns:repeat(2,1fr); padding-inline:14px; }.detail-tabs { margin-inline:14px; }.dialog-grid { grid-template-columns:1fr; } }
</style>
