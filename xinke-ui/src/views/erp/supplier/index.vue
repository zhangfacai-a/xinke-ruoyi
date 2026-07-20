<template>
  <div class="app-container supplier-page">
    <section class="page-intro">
      <div>
        <span class="eyebrow">SUPPLIER PERFORMANCE</span>
        <h2>供应商履约档案</h2>
        <p>沉淀供货周期、起订额、账期和履约评分，为采购选商与智能补货提供依据。</p>
      </div>
      <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['erp:supplier:add']">新增供应商</el-button>
    </section>

    <section class="filter-panel" v-show="showSearch">
      <el-form :model="queryParams" ref="queryRef" :inline="true">
        <el-form-item label="供应商" prop="supplierName"><el-input v-model="queryParams.supplierName" placeholder="搜索名称" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="编码" prop="supplierCode"><el-input v-model="queryParams.supplierCode" placeholder="搜索编码" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="等级" prop="supplierLevel"><el-select v-model="queryParams.supplierLevel" placeholder="全部等级" clearable><el-option v-for="level in levels" :key="level" :label="`${level}级`" :value="level" /></el-select></el-form-item>
        <el-form-item label="优选" prop="preferredFlag"><el-select v-model="queryParams.preferredFlag" placeholder="全部" clearable><el-option label="优选供应商" value="1" /><el-option label="普通供应商" value="0" /></el-select></el-form-item>
        <el-form-item label="状态" prop="status"><el-select v-model="queryParams.status" placeholder="全部" clearable><el-option label="启用" value="0" /><el-option label="停用" value="1" /></el-select></el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <div class="table-toolbar">
        <div class="toolbar-actions">
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['erp:supplier:add']">新增</el-button>
          <el-button type="success" plain icon="Edit" :disabled="single" @click="handleUpdate()" v-hasPermi="['erp:supplier:edit']">修改</el-button>
          <el-button type="danger" plain icon="Delete" :disabled="multiple" @click="handleDelete()" v-hasPermi="['erp:supplier:remove']">删除</el-button>
        </div>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </div>
      <el-table v-loading="loading" :data="supplierList" row-key="supplierId" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column label="供应商" min-width="220" show-overflow-tooltip>
          <template #default="{ row }"><div class="supplier-name"><el-tag v-if="row.preferredFlag === '1'" type="warning" size="small" effect="plain">优选</el-tag><strong>{{ row.supplierName }}</strong></div><div class="cell-meta">{{ row.supplierCode }} · {{ row.contactName || '未维护联系人' }} {{ row.contactPhone || '' }}</div></template>
        </el-table-column>
        <el-table-column label="等级" width="82" align="center"><template #default="{ row }"><span :class="['level-badge', `level-${row.supplierLevel}`]">{{ row.supplierLevel || 'B' }}</span></template></el-table-column>
        <el-table-column label="标准交期" width="100" align="center"><template #default="{ row }"><strong>{{ row.leadTimeDays || 0 }}</strong> 天</template></el-table-column>
        <el-table-column label="综合履约" min-width="170">
          <template #default="{ row }"><div class="score-line"><el-progress :percentage="averageScore(row)" :stroke-width="7" :show-text="false" /><strong>{{ averageScore(row) }}</strong></div><div class="cell-meta">质量 {{ row.qualityScore }} · 交付 {{ row.deliveryScore }} · 服务 {{ row.serviceScore }}</div></template>
        </el-table-column>
        <el-table-column label="结算条件" min-width="126"><template #default="{ row }">{{ settlementLabel(row.settlementType) }}<div class="cell-meta">账期 {{ row.paymentDays || 0 }} 天</div></template></el-table-column>
        <el-table-column label="起订额" width="118" align="right"><template #default="{ row }">¥{{ formatMoney(row.minOrderAmount) }}</template></el-table-column>
        <el-table-column label="采购往来" min-width="132" align="right"><template #default="{ row }"><strong>{{ row.purchaseOrderCount || 0 }}</strong> 单<div class="cell-meta">在途 ¥{{ formatMoney(row.openPurchaseAmount) }}</div></template></el-table-column>
        <el-table-column label="状态" width="82" align="center"><template #default="{ row }"><el-tag :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="138" fixed="right" align="center"><template #default="{ row }"><el-button link type="primary" icon="Edit" @click="handleUpdate(row)" v-hasPermi="['erp:supplier:edit']">修改</el-button><el-button link type="danger" icon="Delete" @click="handleDelete(row)" v-hasPermi="['erp:supplier:remove']">删除</el-button></template></el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog :title="title" v-model="open" width="780px" append-to-body destroy-on-close>
      <el-form ref="supplierRef" :model="form" :rules="rules" label-width="104px">
        <div class="form-section-title">基础信息</div>
        <div class="form-grid">
          <el-form-item label="供应商编码" prop="supplierCode"><el-input v-model="form.supplierCode" placeholder="例如 SUP-0001" /></el-form-item>
          <el-form-item label="供应商名称" prop="supplierName"><el-input v-model="form.supplierName" /></el-form-item>
          <el-form-item label="联系人"><el-input v-model="form.contactName" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
          <el-form-item label="供应商等级"><el-select v-model="form.supplierLevel" style="width:100%"><el-option v-for="level in levels" :key="level" :label="`${level}级`" :value="level" /></el-select></el-form-item>
          <el-form-item label="优选供应商"><el-switch v-model="form.preferredFlag" active-value="1" inactive-value="0" inline-prompt active-text="是" inactive-text="否" /></el-form-item>
        </div>
        <div class="form-section-title">采购与结算</div>
        <div class="form-grid">
          <el-form-item label="结算方式"><el-select v-model="form.settlementType" style="width:100%"><el-option label="月结" value="monthly" /><el-option label="现结" value="cash" /><el-option label="票后结算" value="invoice" /></el-select></el-form-item>
          <el-form-item label="账期天数"><el-input-number v-model="form.paymentDays" :min="0" :max="365" controls-position="right" /></el-form-item>
          <el-form-item label="标准交期"><el-input-number v-model="form.leadTimeDays" :min="0" :max="365" controls-position="right" /><span class="input-suffix">天</span></el-form-item>
          <el-form-item label="起订金额"><el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" controls-position="right" /></el-form-item>
        </div>
        <div class="form-section-title">履约评分</div>
        <div class="score-grid">
          <el-form-item label="质量评分"><el-input-number v-model="form.qualityScore" :min="0" :max="100" :precision="1" controls-position="right" /></el-form-item>
          <el-form-item label="交付评分"><el-input-number v-model="form.deliveryScore" :min="0" :max="100" :precision="1" controls-position="right" /></el-form-item>
          <el-form-item label="服务评分"><el-input-number v-model="form.serviceScore" :min="0" :max="100" :precision="1" controls-position="right" /></el-form-item>
        </div>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="cancel">取消</el-button><el-button type="primary" :loading="saving" @click="submitForm">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="ErpSupplier">
import { listSupplier, getSupplier, addSupplier, updateSupplier, delSupplier } from '@/api/erp/supplier'

const { proxy } = getCurrentInstance()
const levels = ['A', 'B', 'C', 'D']
const supplierList = ref([])
const loading = ref(false)
const saving = ref(false)
const showSearch = ref(true)
const ids = ref([])
const single = ref(true)
const multiple = ref(true)
const total = ref(0)
const open = ref(false)
const title = ref('')
const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, supplierCode: undefined, supplierName: undefined, supplierLevel: undefined, preferredFlag: undefined, status: undefined },
  rules: {
    supplierCode: [{ required: true, message: '供应商编码不能为空', trigger: 'blur' }],
    supplierName: [{ required: true, message: '供应商名称不能为空', trigger: 'blur' }]
  }
})
const { queryParams, form, rules } = toRefs(data)

function formatMoney(value) { return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) }
function settlementLabel(value) { return ({ monthly: '月结', cash: '现结', invoice: '票后结算' })[value] || value || '-' }
function averageScore(row) { return Math.round((Number(row.qualityScore || 0) + Number(row.deliveryScore || 0) + Number(row.serviceScore || 0)) / 3) }
async function getList() { loading.value = true; try { const response = await listSupplier(queryParams.value); supplierList.value = response.rows || []; total.value = response.total || 0 } finally { loading.value = false } }
function reset() { form.value = { supplierId: undefined, supplierCode: undefined, supplierName: undefined, contactName: undefined, contactPhone: undefined, settlementType: 'monthly', paymentDays: 30, supplierLevel: 'B', leadTimeDays: 7, minOrderAmount: 0, qualityScore: 80, deliveryScore: 80, serviceScore: 80, preferredFlag: '0', status: '0', remark: undefined }; proxy.resetForm('supplierRef') }
function cancel() { open.value = false; reset() }
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleSelectionChange(selection) { ids.value = selection.map(item => item.supplierId); single.value = selection.length !== 1; multiple.value = !selection.length }
function handleAdd() { reset(); title.value = '新增供应商'; open.value = true }
async function handleUpdate(row) { reset(); const supplierId = row?.supplierId || ids.value[0]; const response = await getSupplier(supplierId); form.value = response.data; title.value = '修改供应商'; open.value = true }
async function submitForm() { await proxy.$refs.supplierRef.validate(); saving.value = true; try { const request = form.value.supplierId ? updateSupplier(form.value) : addSupplier(form.value); await request; proxy.$modal.msgSuccess(form.value.supplierId ? '修改成功' : '新增成功'); open.value = false; getList() } finally { saving.value = false } }
function handleDelete(row) { const supplierIds = row?.supplierId || ids.value; proxy.$modal.confirm('确认删除选中的供应商吗？已有采购记录的供应商只能停用。').then(() => delSupplier(supplierIds)).then(() => { proxy.$modal.msgSuccess('删除成功'); getList() }).catch(() => {}) }

getList()
</script>

<style scoped>
.supplier-page{--accent:#6c5ce7}.page-intro,.filter-panel,.table-panel{background:rgba(255,255,255,.93);border:1px solid rgba(108,92,231,.09);box-shadow:0 12px 30px rgba(42,39,90,.06)}
.page-intro{display:flex;align-items:center;justify-content:space-between;padding:20px 24px;border-radius:16px;margin-bottom:14px}.eyebrow{color:var(--accent);font-size:12px;font-weight:800;letter-spacing:.08em}.page-intro h2{margin:4px 0 2px;font-size:24px;color:#24283a}.page-intro p{margin:0;color:#7b849e}
.filter-panel{border-radius:14px;padding:15px 18px 0;margin-bottom:14px}.filter-panel :deep(.el-input),.filter-panel :deep(.el-select){width:172px}.table-panel{border-radius:16px;padding:14px 14px 4px}.table-toolbar,.toolbar-actions,.supplier-name,.score-line{display:flex;align-items:center}.table-toolbar{justify-content:space-between;margin-bottom:12px}.toolbar-actions{gap:8px}.supplier-name{gap:7px}.cell-meta{margin-top:4px;color:#959db2;font-size:12px}.score-line{gap:9px}.score-line :deep(.el-progress){flex:1}.score-line strong{color:#00a889}
.level-badge{display:inline-flex;width:30px;height:30px;align-items:center;justify-content:center;border-radius:9px;font-weight:800;background:#f0efff;color:#6c5ce7}.level-A{background:#e9faf5;color:#00a889}.level-C{background:#fff6e7;color:#cf8217}.level-D{background:#fff0f2;color:#dd5269}
.form-section-title{margin:4px 0 14px;padding-left:10px;border-left:3px solid var(--accent);font-weight:700;color:#34394d}.form-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 18px}.score-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:0 8px}.score-grid :deep(.el-form-item__label){width:82px!important}.score-grid :deep(.el-form-item__content){margin-left:82px!important}.score-grid :deep(.el-input-number),.form-grid :deep(.el-input-number){width:100%}.input-suffix{margin-left:8px;color:#8d95aa}
@media (max-width:850px){.form-grid,.score-grid{grid-template-columns:1fr}.page-intro p{display:none}}
</style>
