<template>
  <div :class="['gift-inventory', { 'app-container': !embedded }]">
    <header v-if="!embedded" class="page-head"><div><h2>礼品库存</h2><p>维护礼品可用库存。库存为零时，订单录入会阻止扣减并提示先入库。</p></div><el-button icon="Refresh" :loading="loading" title="刷新" @click="load" /></header>
    <div class="summary-row"><article><span>礼品档案</span><strong>{{ integer(summary.giftCount) }}</strong></article><article><span>库存总件数</span><strong>{{ integer(summary.totalStock) }}</strong></article><article class="warning"><span>低库存</span><strong>{{ integer(summary.lowCount) }}</strong></article><article class="danger"><span>零库存</span><strong>{{ integer(summary.zeroCount) }}</strong></article></div>
    <section class="toolbar"><el-input v-model.trim="keyword" clearable placeholder="搜索礼品名称或编码" @keyup.enter="load" /><el-segmented v-model="stockStatus" :options="statusOptions" @change="load" /><el-button type="primary" icon="Search" @click="load">查询</el-button><el-button icon="Refresh" @click="reset">重置</el-button></section>
    <el-table v-loading="loading" :data="rows" stripe>
      <el-table-column prop="giftName" label="礼品" min-width="230" show-overflow-tooltip><template #default="{ row }"><div class="gift-name"><strong>{{ row.giftName }}</strong><span>{{ row.giftCode }}</span></div></template></el-table-column>
      <el-table-column label="库存" width="160" align="right"><template #default="{ row }"><strong :class="stockClass(row)">{{ row.stockQty }}</strong><span class="unit">件</span></template></el-table-column>
      <el-table-column prop="safetyQty" label="安全库存" width="110" align="right" />
      <el-table-column label="库存状态" width="150"><template #default="{ row }"><el-tag :type="row.stockStatus === 'zero' ? 'danger' : row.stockStatus === 'low' ? 'warning' : 'success'" effect="light">{{ statusLabel(row.stockStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="使用件数" width="110" align="right" prop="usedQty" />
      <el-table-column label="库存比例" min-width="150"><template #default="{ row }"><el-progress :percentage="stockPercent(row)" :status="row.stockStatus === 'zero' ? 'exception' : undefined" :show-text="false" /></template></el-table-column>
      <el-table-column label="操作" width="276" fixed="right"><template #default="{ row }"><div class="inventory-actions"><el-button link type="primary" icon="Plus" @click="openAdjust(row, 'in')">入库</el-button><el-button link type="warning" icon="Minus" @click="openAdjust(row, 'out')">出库</el-button><el-button link icon="Edit" @click="openAdjust(row, 'set')">盘点</el-button><el-button link icon="List" @click="openMovements(row)">流水</el-button></div></template></el-table-column>
      <template #empty><el-empty description="暂无礼品库存数据" /></template>
    </el-table>
    <el-dialog v-model="adjustVisible" :title="adjustTitle" width="520px" destroy-on-close>
      <div class="adjust-gift"><strong>{{ adjustForm.giftName }}</strong><span>当前库存 {{ adjustForm.currentQty }} 件</span></div>
      <el-form label-width="90px"><el-form-item label="操作类型"><el-radio-group v-model="adjustForm.movementType"><el-radio-button label="in">入库</el-radio-button><el-radio-button label="out">出库</el-radio-button><el-radio-button label="set">盘点设置</el-radio-button></el-radio-group></el-form-item><el-form-item :label="adjustForm.movementType === 'set' ? '盘点数量' : '数量'"><el-input-number v-model="adjustForm.quantity" :min="0" :max="999999" controls-position="right" /></el-form-item><el-form-item label="安全库存"><el-input-number v-model="adjustForm.safetyQty" :min="0" :max="999999" controls-position="right" /></el-form-item><el-form-item label="备注"><el-input v-model.trim="adjustForm.remark" type="textarea" :rows="3" maxlength="500" placeholder="例如：采购入库、盘点修正、损耗出库" /></el-form-item></el-form>
      <template #footer><el-button @click="adjustVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveAdjust">保存</el-button></template>
    </el-dialog>
    <el-drawer v-model="movementVisible" :title="`${movementGift.giftName || ''} · 库存流水`" size="620px"><el-table v-loading="movementLoading" :data="movements" size="small"><el-table-column prop="createTime" label="时间" width="160" /><el-table-column label="变动" width="80" align="right"><template #default="{ row }"><strong :class="row.quantity > 0 ? 'in' : 'out'">{{ row.quantity > 0 ? '+' : '' }}{{ row.quantity }}</strong></template></el-table-column><el-table-column prop="beforeQty" label="变动前" width="75" align="right" /><el-table-column prop="afterQty" label="变动后" width="75" align="right" /><el-table-column prop="movementType" label="类型" width="110" /><el-table-column prop="sourceNo" label="来源" width="130" show-overflow-tooltip /><el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip /></el-table></el-drawer>
  </div>
</template>

<script setup name="GiftInventory">
import { adjustGiftInventory, getGiftInventorySummary, listGiftInventory, listGiftInventoryMovements } from '@/api/live/gift'
const props = defineProps({ embedded: { type: Boolean, default: false } })
const { proxy } = getCurrentInstance()
const rows = ref([]), summary = ref({}), movements = ref([]), movementGift = ref({}), keyword = ref(''), stockStatus = ref(''), loading = ref(false), saving = ref(false), movementLoading = ref(false), adjustVisible = ref(false), movementVisible = ref(false), adjustForm = ref({})
const statusOptions = [{ label: '全部', value: '' }, { label: '低库存', value: 'low' }, { label: '零库存', value: 'zero' }]
const integer = value => Number(value || 0).toLocaleString('zh-CN')
const adjustTitle = computed(() => adjustForm.value.movementType === 'in' ? '礼品入库' : adjustForm.value.movementType === 'out' ? '礼品出库' : '库存盘点')
function statusLabel(value) { return value === 'zero' ? '零库存' : value === 'low' ? '低库存' : '库存正常' }
function stockClass(row) { return row.stockStatus === 'zero' ? 'danger' : row.stockStatus === 'low' ? 'warning' : 'normal' }
function stockPercent(row) { const max = Math.max(Number(row.stockQty || 0), Number(row.safetyQty || 0), 1); return Math.min(100, Math.round(Number(row.stockQty || 0) * 100 / max)) }
async function load() { loading.value = true; try { const [list, stats] = await Promise.all([listGiftInventory({ keyword: keyword.value, status: '0', stockStatus: stockStatus.value }), getGiftInventorySummary({})]); rows.value = list.data || []; summary.value = stats.data || {} } finally { loading.value = false } }
function reset() { keyword.value = ''; stockStatus.value = ''; load() }
function openAdjust(row, type) { adjustForm.value = { giftId: row.giftId, giftName: row.giftName, currentQty: Number(row.stockQty || 0), movementType: type, quantity: type === 'set' ? Number(row.stockQty || 0) : 1, safetyQty: Number(row.safetyQty || 0), remark: '' }; adjustVisible.value = true }
async function saveAdjust() { if (adjustForm.value.quantity == null || adjustForm.value.quantity < 0) return proxy.$modal.msgWarning('请输入有效数量'); if (adjustForm.value.movementType !== 'set' && adjustForm.value.quantity === 0) return proxy.$modal.msgWarning('数量必须大于0'); saving.value = true; try { await adjustGiftInventory(adjustForm.value); proxy.$modal.msgSuccess('库存已更新'); adjustVisible.value = false; await load() } finally { saving.value = false } }
async function openMovements(row) { movementGift.value = row; movementVisible.value = true; movementLoading.value = true; try { movements.value = (await listGiftInventoryMovements({ giftId: row.giftId })).data || [] } finally { movementLoading.value = false } }
onMounted(load)
</script>

<style scoped>
.gift-inventory{max-width:1600px}.page-head,.toolbar,.gift-name,.adjust-gift{display:flex;align-items:center}.page-head{justify-content:space-between}.page-head h2{margin:0 0 4px;font-size:22px}.page-head p{margin:0;color:#6b7280;font-size:13px}.summary-row{display:grid;grid-template-columns:repeat(4,1fr);gap:1px;margin-top:18px;border:1px solid #e5e7eb;background:#e5e7eb}.summary-row article{display:flex;flex-direction:column;gap:7px;padding:16px;background:#fff}.summary-row span{color:#6b7280;font-size:13px}.summary-row strong{font-size:25px}.summary-row .warning strong,.warning{color:#d97706}.summary-row .danger strong,.danger{color:#dc2626}.toolbar{gap:10px;padding:16px 0}.toolbar>.el-input{width:300px}.gift-name{align-items:flex-start;flex-direction:column;gap:3px}.gift-name span{color:#8a949f;font:11px Consolas,monospace}.unit{margin-left:4px;color:#6b7280;font-size:12px}.normal{color:#16a34a}.adjust-gift{justify-content:space-between;margin-bottom:18px;padding:12px 14px;background:#f8fafc}.adjust-gift span{color:#6b7280}.in{color:#16a34a}.out{color:#dc2626}.inventory-actions{display:flex;align-items:center;gap:12px;white-space:nowrap}.inventory-actions :deep(.el-button){flex:none;margin-left:0!important;padding-inline:2px}@media(max-width:800px){.summary-row{grid-template-columns:repeat(2,1fr)}.toolbar{align-items:stretch;flex-wrap:wrap}.toolbar>*{width:100%!important}}
</style>
