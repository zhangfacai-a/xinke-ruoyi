<template>
  <div class="app-container retention-center">
    <div class="page-heading"><h2>挽单与转推中心</h2><div v-if="canCreate" class="heading-actions"><el-button v-if="tab !== 'dashboard' && tab !== 'download'" :disabled="!selectedRows.length" @click="openBatchAssign">批量分配<span v-if="selectedRows.length">（{{ selectedRows.length }}）</span></el-button><el-button type="primary" @click="openCreate">新增订单</el-button></div></div>
    <el-tabs v-model="tab" @tab-change="switchTab">
      <el-tab-pane label="待处理" name="PENDING" />
      <el-tab-pane label="跟进中" name="FOLLOWING" />
      <el-tab-pane label="已完成" name="COMPLETED" />
      <el-tab-pane label="看板" name="dashboard" />
      <el-tab-pane label="下载" name="download" />
    </el-tabs>
    <el-form inline class="filters" @submit.prevent="search">
      <el-form-item label="创建日期"><el-date-picker v-model="currentFilter.dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" :clearable="false" /></el-form-item>
      <el-form-item label="店铺"><el-select v-model="currentFilter.shopId" clearable filterable placeholder="全部店铺"><el-option v-for="s in shops" :key="s.shopId" :label="s.shopName" :value="s.shopId" /></el-select></el-form-item>
      <el-form-item v-if="!ownOnly" label="负责人"><el-select v-model="currentFilter.ownerId" clearable filterable placeholder="全部人员"><el-option v-for="s in filterStaff" :key="s.userId" :label="s.userName" :value="s.userId" /></el-select></el-form-item>
      <el-form-item label="结果"><el-select v-model="currentFilter.resultType" clearable placeholder="全部结果"><el-option v-for="item in resultFilterOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
      <el-form-item label="电话地址"><el-select v-model="currentFilter.addressStatus" clearable placeholder="全部"><el-option label="待补充" value="MISSING" /><el-option label="已补充" value="FILLED" /></el-select></el-form-item>
      <el-form-item v-if="tab !== 'dashboard'"><el-input v-model="currentFilter.keyword" placeholder="订单号 / 电话" maxlength="64" clearable @keyup.enter="search" /></el-form-item>
      <el-form-item v-if="tab === 'download'" label="进度"><el-select v-model="currentFilter.progress" clearable placeholder="全部"><el-option v-for="(v,k) in progressLabels" :key="k" :label="v" :value="k" /></el-select></el-form-item>
      <el-form-item><el-button :loading="loading" @click="search">查询</el-button></el-form-item>
    </el-form>

    <template v-if="tab === 'dashboard'">
      <div v-loading="loading" class="dashboard">
        <div v-for="card in cards" :key="card.key" class="metric"><span>{{ card.label }}</span><strong>{{ card.money ? money(stats[card.key]) : card.duration ? duration(stats[card.key]) : (stats[card.key] || 0) }}</strong></div>
      </div>
    </template>
    <template v-else>
      <div v-if="tab === 'download'" class="download-bar">
        <span>订单明细 · {{ total }} 条 · 单次最多31天、5000条</span>
        <el-button type="primary" :loading="downloading" :disabled="loading || !total" @click="download">下载 CSV</el-button>
      </div>
      <el-table v-loading="loading" :data="rows" row-key="id" border @selection-change="selectedRows = $event">
        <el-table-column v-if="canCreate && tab !== 'download'" type="selection" width="46" :selectable="canBatchSelect" />
        <el-table-column prop="createdAt" label="创建时间" width="170" :formatter="(_,__,v) => time(v)" />
        <el-table-column prop="shopName" label="店铺" min-width="160" show-overflow-tooltip />
        <el-table-column prop="orderNo" label="原订单号" width="195" />
        <el-table-column label="电话地址" min-width="220" show-overflow-tooltip><template #default="{row}">{{ contact(row) }}</template></el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="110"><template #default="{row}">{{ row.ownerName || '未分配' }}</template></el-table-column>
        <el-table-column label="结果" min-width="150"><template #default="{row}"><div>{{ resultLabels[row.retentionResult] }}</div><div v-if="row.referralSuccess == 1">转推成功</div><div v-if="row.freightCollected == 1">已征收运费</div></template></el-table-column>
        <el-table-column prop="latestNote" label="最新跟进" min-width="220" show-overflow-tooltip />
        <el-table-column prop="firstFollowedAt" label="首次跟进" width="170" :formatter="(_,__,v) => time(v)" />
        <el-table-column v-if="tab === 'download'" label="金额（元）" min-width="190"><template #default="{row}">挽单 {{ money(row.retentionAmount) }}<br>转推 {{ money(row.referralAmount) }}<br>运费 {{ money(row.freightAmount) }}</template></el-table-column>
        <el-table-column label="操作" width="135" fixed="right"><template #default="{row}">
          <el-tooltip content="查看订单档案"><el-button circle :icon="View" :disabled="opening" aria-label="查看订单档案" @click="openDetail(row.id)" /></el-tooltip>
          <el-button v-if="canDelete(row)" type="danger" link :disabled="deleting || saving" @click="removeOrder(row)">删除</el-button>
        </template></el-table-column>
      </el-table>
      <el-pagination class="pagination" v-model:current-page="page" v-model:page-size="pageSize" :total="total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next" @size-change="search" @current-change="load" />
    </template>

    <el-drawer v-model="visible" size="min(720px, 100%)" class="order-drawer" :close-on-click-modal="false" :before-close="closeDialog" destroy-on-close>
      <template #header><div class="detail-header"><strong>{{ form.id ? (editing ? '登记订单跟进' : '订单档案') : '新增订单' }}</strong><div class="drawer-subtitle">{{ form.shopName || '选择店铺并登记订单' }}<span v-if="form.id"> · {{ form.orderNo }}</span></div></div></template>
      <div v-if="form.id && !editing" class="detail-body">
        <div class="profile-strip">
          <div><span>负责人</span><strong>{{ form.ownerName || '未分配' }}</strong></div>
          <div><span>处理进度</span><strong>{{ progressLabels[form.progress] }}</strong></div>
          <div><span>挽单结果</span><strong>{{ resultLabels[form.retentionResult] }}</strong></div>
        </div>
        <div class="first-followup"><span>首次跟进</span><strong>{{ time(form.firstFollowedAt) }}</strong></div>
        <section class="detail-section"><div class="section-title"><strong>订单信息</strong><span>{{ time(form.createdAt) }}</span></div><dl class="order-info"><dt>店铺</dt><dd>{{ form.shopName }}</dd><dt>原订单号</dt><dd>{{ form.orderNo }}</dd><dt>电话地址</dt><dd>{{ contact(form) }}</dd></dl></section>
        <section class="detail-section"><div class="section-title"><strong>处理结果</strong></div><dl class="order-info"><dt>挽单销售额</dt><dd>{{ form.retentionResult === 'SUCCESS' ? `¥ ${money(form.retentionAmount)}` : '—' }}</dd><dt>转推</dt><dd>{{ form.referralSuccess ? '转推成功' : '未转推' }}<template v-if="form.referralSuccess"><br>{{ form.referralShopName }}<br>{{ form.referralOrderNo }} · ¥ {{ money(form.referralAmount) }}</template></dd><dt>运费征收</dt><dd>{{ form.freightCollected ? `¥ ${money(form.freightAmount)}` : '未征收' }}</dd><dt>完成时间</dt><dd>{{ time(form.completedAt) }}</dd></dl></section>
      </div>
      <el-form v-if="!form.id || editing" ref="formRef" :model="form" :rules="rules" label-position="top" :disabled="saving" class="registration-form">
        <div class="form-grid">
          <el-form-item label="店铺" prop="shopId"><el-select v-model="form.shopId" :disabled="!!form.id && !form.canEditOrder" @change="form.ownerId = null" filterable placeholder="选择店铺"><el-option v-for="s in editableShops" :key="s.shopId" :label="s.shopName" :value="s.shopId" /></el-select></el-form-item>
          <el-form-item v-if="form.id" label="原订单号" prop="orderNo"><el-input v-model="form.orderNo" :disabled="!form.canEditOrder" maxlength="64" /></el-form-item>
          <el-form-item label="电话客服"><el-select v-model="form.ownerId" :disabled="!!form.id && !form.canEditOrder" clearable filterable placeholder="未分配"><el-option v-for="s in assignedStaff" :key="s.userId" :label="s.userName" :value="s.userId" /></el-select></el-form-item>
        </div>
        <el-form-item v-if="!form.id" label="原订单号（每行一条）" prop="orderNoText"><el-input v-model="form.orderNoText" type="textarea" :rows="8" maxlength="13000" placeholder="粘贴订单号，按回车拆分&#10;6955000000000000001&#10;6955000000000000002" /></el-form-item>
        <div v-if="!form.id && parsedOrders.total" class="import-summary"><strong>识别 {{ parsedOrders.unique.length }} 条</strong><span v-if="parsedOrders.duplicates">输入内有 {{ parsedOrders.duplicates }} 条重复内容</span><span v-if="parsedOrders.invalid.length" class="invalid">{{ parsedOrders.invalid.length }} 条超过 64 个字符</span><span v-if="parsedOrders.valid.length > 200" class="invalid">单次最多检查 200 行</span></div>
        <el-form-item label="电话地址（选填）"><el-input v-model="form.address" :disabled="!!form.id && !form.canEditAddress" type="textarea" :rows="2" maxlength="500" placeholder="可稍后由电话客服补充" /></el-form-item>
        <template v-if="form.id">
          <div class="timestamps">创建 {{ time(form.createdAt) }}　首次跟进 {{ time(form.firstFollowedAt) }}</div>
          <div class="form-grid">
            <el-form-item label="处理进度"><el-radio-group v-model="form.progress" class="stage-picker"><el-radio-button v-for="(v,k) in progressLabels" :key="k" :value="k">{{ v }}</el-radio-button></el-radio-group></el-form-item>
            <el-form-item label="挽单结果"><el-radio-group v-model="form.retentionResult" class="stage-picker"><el-radio-button v-for="(v,k) in resultLabels" :key="k" :value="k">{{ v }}</el-radio-button></el-radio-group></el-form-item>
            <el-form-item v-if="form.retentionResult === 'SUCCESS'" label="挽单销售额（元）" prop="retentionAmount"><el-input-number v-model="form.retentionAmount" :min="0" :max="999999999999.99" :precision="2" :controls="false" /></el-form-item>
          </div>
          <el-checkbox v-model="form.referralSuccess">转推成功</el-checkbox>
          <div v-if="form.referralSuccess" class="form-grid outcome">
            <el-form-item label="转推店铺" prop="referralShopId"><el-select v-model="form.referralShopId" filterable><el-option v-for="s in shops" :key="s.shopId" :label="s.shopName" :value="s.shopId" /></el-select></el-form-item>
            <el-form-item label="转推订单号" prop="referralOrderNo"><el-input v-model="form.referralOrderNo" maxlength="64" /></el-form-item>
            <el-form-item label="转推金额（元）" prop="referralAmount"><el-input-number v-model="form.referralAmount" :min="0" :max="999999999999.99" :precision="2" :controls="false" /></el-form-item>
          </div>
          <el-form-item v-if="form.retentionResult !== 'SUCCESS'" label="征收运费金额（元）" prop="freightAmount"><el-input-number v-model="form.freightAmount" :min="0" :max="999999999999.99" :precision="2" :controls="false" /></el-form-item>
          <el-form-item label="本次跟进记录 / 修改原因"><el-input v-model="form.note" type="textarea" :rows="3" maxlength="2000" show-word-limit placeholder="选择“本次为客户跟进”时必须填写" /></el-form-item>
          <el-checkbox v-model="form.isFollowup">本次为客户跟进</el-checkbox>
          <el-form-item v-if="needsEvidence" label="结果凭证图片" class="evidence-upload"><el-upload v-model:file-list="evidenceFiles" action="#" multiple accept="image/jpeg,image/png,image/webp" :auto-upload="false" :limit="9" list-type="picture-card"><el-icon><Plus /></el-icon></el-upload><small>支持 JPG、PNG、WEBP，单张不超过 10MB；可在保存时一并上传。</small></el-form-item>
        </template>
      </el-form>
      <section v-if="form.id && attachments.length" class="detail-section"><div class="section-title"><strong>结果凭证</strong><span>{{ attachments.length }} 张</span></div><div class="evidence-grid"><div v-for="item in attachments" :key="item.id" class="evidence-item"><el-image :src="item.url" :preview-src-list="attachments.map(x => x.url)" fit="cover" preview-teleported /><el-button v-if="editing" type="danger" text :icon="Delete" aria-label="删除凭证" @click="removeAttachment(item)" /></div></div></section>
      <template v-if="form.id">
        <section class="detail-section"><div class="section-title"><strong>跟进及修改记录</strong><span>{{ history.length }} 条</span></div>
          <div class="timeline"><div v-for="h in history" :key="h.id" class="timeline-item">
            <p class="note">{{ h.note }}</p><div v-if="h.action === 'OWNER_CHANGE'" class="change">{{ ownerLabel(h.oldValue) }} → {{ ownerLabel(h.newValue) }}</div>
            <span>{{ h.operatorName }} · {{ time(h.createdAt) }} · {{ actionLabels[h.action] || h.action }}</span>
          </div><div v-if="!history.length" class="empty-history">暂无跟进记录</div></div>
        </section>
      </template>
      <template #footer><div class="drawer-footer"><el-button v-if="form.id && canDelete(form)" type="danger" plain :disabled="deleting || saving" @click="removeOrder(form)">删除订单</el-button><span class="footer-spacer" /><el-button :disabled="saving || deleting" @click="visible = false">关闭</el-button><el-button v-if="form.id && !editing" type="primary" :icon="EditPen" :disabled="deleting" @click="editing = true">登记跟进 / 修改</el-button><el-button v-else type="primary" :loading="saving" :disabled="deleting || (!form.id && (!parsedOrders.unique.length || parsedOrders.valid.length > 200))" @click="save">{{ form.id ? '保存' : `检查并创建 ${parsedOrders.unique.length} 条` }}</el-button></div></template>
    </el-drawer>

    <el-dialog v-model="batchVisible" title="批量分配电话客服" width="min(480px, 94%)" :close-on-click-modal="false">
      <div class="batch-summary">已选择 <strong>{{ selectedRows.length }}</strong> 条订单，涉及 <strong>{{ selectedShopNames.join('、') }}</strong></div>
      <el-alert v-if="!batchStaff.length" title="所选店铺没有共同的电话客服，请按店铺分别分配" type="warning" :closable="false" show-icon />
      <el-form label-position="top" class="batch-form"><el-form-item label="分配给"><el-select v-model="batchOwnerId" filterable placeholder="选择电话客服" style="width:100%"><el-option v-for="s in batchStaff" :key="s.userId" :label="s.userName" :value="s.userId" /></el-select></el-form-item></el-form>
      <template #footer><el-button @click="batchVisible=false">取消</el-button><el-button type="primary" :loading="batchSaving" :disabled="!batchOwnerId || !batchStaff.length" @click="confirmBatchAssign">确认分配</el-button></template>
    </el-dialog>

    <el-dialog v-model="duplicateVisible" title="发现重复订单" width="min(620px, 94%)" :close-on-click-modal="false">
      <el-alert :title="`重复或无效订单不会创建，其余 ${duplicateCheck.validOrderNos.length} 条可以正常新增`" type="warning" :closable="false" show-icon />
      <div v-if="duplicateCheck.existingOrderNos.length" class="duplicate-group"><strong>系统已存在（{{ duplicateCheck.existingOrderNos.length }}）</strong><div>{{ duplicateCheck.existingOrderNos.join('\n') }}</div></div>
      <div v-if="duplicateCheck.duplicateInputOrderNos.length" class="duplicate-group"><strong>输入内容重复（{{ duplicateCheck.duplicateInputOrderNos.length }}）</strong><div>{{ duplicateCheck.duplicateInputOrderNos.join('\n') }}</div></div>
      <div v-if="duplicateCheck.invalidOrderNos.length" class="duplicate-group"><strong>格式不正确（{{ duplicateCheck.invalidOrderNos.length }}）</strong><div>{{ duplicateCheck.invalidOrderNos.join('\n') }}</div></div>
      <template #footer><el-button @click="duplicateVisible=false">返回修改</el-button><el-button v-if="duplicateCheck.validOrderNos.length" type="primary" :loading="saving" @click="confirmDuplicateCreate">跳过重复并新增 {{ duplicateCheck.validOrderNos.length }} 条</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="RetentionCenter">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View, EditPen, Plus, Delete } from '@element-plus/icons-vue'
import { saveAs } from 'file-saver'
import { retentionOptions, retentionList, retentionDashboard, retentionDetail, retentionBatchCheck, retentionBatchCreate, retentionBatchOwner, retentionUpdate, retentionDelete, retentionUploadAttachment, retentionDeleteAttachment, retentionExport } from '@/api/retention'

const progressLabels = { PENDING: '待处理', FOLLOWING: '跟进中', COMPLETED: '已完成' }
const resultLabels = { UNKNOWN: '未确定', SUCCESS: '挽单成功', FAILED: '挽单失败' }
const resultFilterOptions = [
  { value: 'UNKNOWN', label: '未确定' },
  { value: 'SUCCESS', label: '挽单成功' },
  { value: 'FAILED', label: '挽单失败' },
  { value: 'REFERRAL_SUCCESS', label: '转推成功' },
  { value: 'FREIGHT_COLLECTED', label: '已征收运费' }
]
const actionLabels = { CREATE: '创建', EDIT: '修改', FOLLOWUP: '跟进', OWNER_CHANGE: '负责人变更' }
function date(days = 0) { const d = new Date(); d.setDate(d.getDate() + days); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}` }
function filter() { return { dates: [date(-1), date()], shopId: '', ownerId: '', resultType: '', addressStatus: '', keyword: '', progress: '' } }
const filters = reactive({ list: filter(), dashboard: filter(), download: filter() })
const tab = ref('PENDING'), currentFilter = computed(() => filters[tab.value === 'dashboard' || tab.value === 'download' ? tab.value : 'list'])
const shops = ref([]), staff = ref([]), rows = ref([]), stats = ref({}), total = ref(0), page = ref(1), pageSize = ref(20)
const ownOnly = ref(true), canCreate = ref(false), adminShopIds = ref([])
const editableShops = computed(() => form.value.id && !form.value.canEditOrder ? shops.value : shops.value.filter(s => adminShopIds.value.some(id => String(id) === String(s.shopId))))
const assignedStaff = computed(() => staff.value.filter(s => String(s.shopId) === String(form.value.shopId)))
const filterStaff = computed(() => [...new Map(staff.value.map(s => [s.userId, s])).values()])
const editing = ref(false), selectedRows = ref([]), batchVisible = ref(false), batchSaving = ref(false), batchOwnerId = ref(null)
const duplicateVisible = ref(false), duplicateCheck = ref({validOrderNos:[],existingOrderNos:[],duplicateInputOrderNos:[],invalidOrderNos:[]}), pendingCreate = ref(null)
const deleting = ref(false)
const loading = ref(false), downloading = ref(false), saving = ref(false), opening = ref(false), visible = ref(false), formRef = ref(), form = ref({}), history = ref([]), attachments = ref([]), evidenceFiles = ref([])
const needsEvidence = computed(() => form.value.retentionResult === 'SUCCESS' || form.value.referralSuccess || form.value.freightAmount != null)
const parsedOrders = computed(() => { const lines=String(form.value.orderNoText || '').split(/\r?\n/).map(v=>v.trim()).filter(Boolean), seen=new Set(), unique=[], valid=[], invalid=[], duplicateOrderNos=[]; for(const value of lines){if(value.length>64){invalid.push(value);continue}valid.push(value);if(seen.has(value)){if(!duplicateOrderNos.includes(value))duplicateOrderNos.push(value);continue}seen.add(value);unique.push(value)}return {total:lines.length,valid,unique,invalid,duplicateOrderNos,duplicates:valid.length-unique.length} })
const selectedShopIds = computed(() => [...new Set(selectedRows.value.map(row => String(row.shopId)))])
const selectedShopNames = computed(() => selectedShopIds.value.map(id => shops.value.find(s => String(s.shopId)===id)?.shopName || id))
const batchStaff = computed(() => { const ids=selectedShopIds.value; if(!ids.length)return []; const byUser=new Map(); for(const item of staff.value){const key=String(item.userId);if(!byUser.has(key))byUser.set(key,{...item,shops:new Set()});byUser.get(key).shops.add(String(item.shopId))}return [...byUser.values()].filter(item=>ids.every(id=>item.shops.has(id))) })
const cards = [{key:'total',label:'订单数'},{key:'pending',label:'待处理'},{key:'following',label:'跟进中'},{key:'completed',label:'已完成'},{key:'avgCompletionSeconds',label:'平均每条完成用时',duration:true},{key:'retentionCount',label:'挽单成功'},{key:'failedCount',label:'挽单失败'},{key:'referralCount',label:'转推成功'},{key:'freightCount',label:'征收运费订单'},{key:'retentionAmount',label:'挽单销售额',money:true},{key:'referralAmount',label:'转推金额',money:true},{key:'freightAmount',label:'征收运费',money:true}]
const required = { required: true, message: '请填写此项', trigger: 'blur' }
const rules = { shopId:[required], orderNo:[required], orderNoText:[required], referralShopId:[required], referralOrderNo:[required], retentionAmount:[required], referralAmount:[required] }
let requestId = 0
function params() { const f = currentFilter.value; if (!f.dates || f.dates.length !== 2) throw new Error('请选择日期范围'); return { beginDate: f.dates[0], endDate: f.dates[1], shopId: f.shopId || undefined, ownerId: f.ownerId || undefined, resultType: f.resultType || undefined, addressStatus: f.addressStatus || undefined, keyword: f.keyword || undefined, progress: tab.value === 'download' ? f.progress || undefined : progressLabels[tab.value] ? tab.value : undefined, pageNum: page.value, pageSize: pageSize.value } }
async function load() {
  const id = ++requestId; loading.value = true
  try { const q = params(); const res = await (tab.value === 'dashboard' ? retentionDashboard(q) : retentionList(q)); if(id !== requestId) return; if(tab.value === 'dashboard') stats.value = res.data || {}; else { rows.value = res.data.rows; total.value = Number(res.data.total) } }
  catch (e) { if(id === requestId) { rows.value = []; total.value = 0; stats.value = {} } }
  finally { if(id === requestId) loading.value = false }
}
function search() { page.value = 1; load() }
function switchTab() { rows.value = []; selectedRows.value=[]; total.value = 0; search() }
function money(v) { return Number(v || 0).toLocaleString('zh-CN', {minimumFractionDigits:2,maximumFractionDigits:2}) }
function time(v) { return v ? String(v).replace('T',' ').slice(0,19) : '—' }
function duration(v) { if(v === null || v === undefined || v === '') return '—'; const seconds=Math.max(0,Number(v)); if(!Number.isFinite(seconds)) return '—'; const minutes=Math.round(seconds/60), days=Math.floor(minutes/1440), hours=Math.floor((minutes%1440)/60), mins=minutes%60; return [days?`${days}天`:'',hours?`${hours}小时`:'',mins||(!days&&!hours)?`${mins}分钟`:''].filter(Boolean).join('') }
function contact(v) { return v.address || '待补充' }
function canDelete(row) { return canCreate.value && adminShopIds.value.some(id => String(id) === String(row.shopId)) }
function canBatchSelect(row) { return row.progress !== 'COMPLETED' && canDelete(row) }
async function removeOrder(row) {
  if(deleting.value || saving.value) return
  deleting.value = true
  try {
    await ElMessageBox.confirm(`删除订单 ${row.orderNo}？对应跟进记录也会删除。`, '删除订单', { type:'warning', confirmButtonText:'删除', cancelButtonText:'取消' })
    await retentionDelete(row.id, row.version)
    if(form.value.id === row.id) visible.value = false
    ElMessage.success('订单已删除')
    if(rows.value.length === 1 && page.value > 1) page.value--
    await load()
  } catch { /* Cancellation leaves data unchanged; request helper displays server errors. */ }
  finally { deleting.value = false }
}
function openCreate() { editing.value=false; form.value = {shopId:null,orderNoText:'',address:'',ownerId:null}; history.value=[]; attachments.value=[]; evidenceFiles.value=[]; visible.value=true }
function openBatchAssign() { if(!selectedRows.value.length)return; batchOwnerId.value=null; batchVisible.value=true }
async function confirmBatchAssign() {
  if(batchSaving.value || !batchOwnerId.value)return
  const person=batchStaff.value.find(s=>String(s.userId)===String(batchOwnerId.value))
  try { await ElMessageBox.confirm(`将 ${selectedRows.value.length} 条订单分配给 ${person?.userName || ''}？`, '确认批量分配', {type:'warning',confirmButtonText:'确认分配',cancelButtonText:'取消'}); batchSaving.value=true
    const res=await retentionBatchOwner({ownerId:batchOwnerId.value,orders:selectedRows.value.map(row=>({id:row.id,version:row.version}))}); const data=res.data || {}; ElMessage.success(`已分配 ${data.assignedCount || 0} 条${data.skippedCount ? `，跳过同负责人 ${data.skippedCount} 条` : ''}`);batchVisible.value=false;selectedRows.value=[];await load()
  } catch { /* Cancellation or request errors keep the dialog available. */ } finally { batchSaving.value=false }
}
async function openDetail(id) {
  if(opening.value) return; opening.value=true; editing.value=false
  try { const res = await retentionDetail(id); const v=res.data; history.value=v.history || []; attachments.value=v.attachments || []; evidenceFiles.value=[]; form.value={ ...v, ownerId:v.ownerId || null, retentionAmount:Number(v.retentionAmount || 0), referralAmount:Number(v.referralAmount || 0), freightAmount:v.freightCollected == 1 ? Number(v.freightAmount) : null, referralSuccess:v.referralSuccess == 1, freightCollected:v.freightCollected == 1, note:'', isFollowup:false }; visible.value=true }
  catch { /* Request helper displays server errors. */ }
  finally { opening.value=false }
}
function closeDialog(done) { if(!saving.value) done() }
async function save() {
  if(saving.value || !await formRef.value.validate().catch(() => false)) return
  const v = form.value
  if(!v.id && parsedOrders.value.invalid.length) return ElMessage.warning('存在超过 64 个字符的订单号，请修正后再创建')
  if(!v.id && parsedOrders.value.valid.length > 200) return ElMessage.warning('单次最多检查 200 行订单号')
  if(v.id && v.isFollowup && !String(v.note || '').trim()) return ElMessage.warning('请填写本次跟进记录')
  if(v.id && v.isFollowup && !v.ownerId) return ElMessage.warning('请先分配负责人')
  if(v.id && v.progress === 'COMPLETED' && v.retentionResult === 'UNKNOWN' && !v.referralSuccess && (v.freightAmount == null || v.retentionResult === 'SUCCESS')) return ElMessage.warning('请选择业务结果后完成')
  const pending=evidenceFiles.value.map(x => x.raw).filter(Boolean)
  if(pending.some(file => file.size > 10*1024*1024)) return ElMessage.warning('单张图片不能超过 10MB')
  if(!v.id) {
    saving.value=true
    try {
      const payload={shopId:v.shopId,ownerId:v.ownerId,address:v.address}
      const checked=(await retentionBatchCheck({shopId:v.shopId,orderNos:parsedOrders.value.valid})).data || {}
      duplicateCheck.value={validOrderNos:checked.validOrderNos||[],existingOrderNos:checked.existingOrderNos||[],duplicateInputOrderNos:checked.duplicateInputOrderNos||[],invalidOrderNos:checked.invalidOrderNos||[]}
      pendingCreate.value=payload
      if(duplicateCheck.value.existingOrderNos.length||duplicateCheck.value.duplicateInputOrderNos.length||duplicateCheck.value.invalidOrderNos.length){duplicateVisible.value=true;return}
      await createCheckedOrders(payload,duplicateCheck.value.validOrderNos)
    } finally { saving.value=false }
    return
  }
  saving.value = true
  try { const data={...v}; delete data.history;
    if(v.id && !v.canEditOrder) for(const key of ['shopId','orderNo','ownerId']) delete data[key];
    if(v.retentionResult === 'SUCCESS') data.freightAmount=null; let id=v.id
    if(id) { for(const file of pending) await retentionUploadAttachment(id,file); await retentionUpdate(id,data) }
    if(id) ElMessage.success(pending.length ? '已保存并上传凭证' : '已保存'); visible.value=false; await load() }
  catch { /* Keep the form open so input can be corrected. */ }
  finally { saving.value=false }
}
async function createCheckedOrders(payload,orderNos) {
  const res=await retentionBatchCreate({...payload,orderNos}),result=res.data||{}
  if(result.skippedCount){await ElMessageBox.alert(`成功创建 ${result.createdCount||0} 条。另有 ${result.skippedCount} 条在提交时已被其他人创建，系统已自动跳过：\n${(result.skippedOrderNos||[]).join('\n')}`,'创建完成',{type:'warning',confirmButtonText:'知道了'})}
  else ElMessage.success(`成功创建 ${result.createdCount||0} 条订单`)
  duplicateVisible.value=false;visible.value=false;pendingCreate.value=null;await load()
}
async function confirmDuplicateCreate() {
  if(saving.value||!pendingCreate.value||!duplicateCheck.value.validOrderNos.length)return
  saving.value=true
  try{await createCheckedOrders(pendingCreate.value,duplicateCheck.value.validOrderNos)}finally{saving.value=false}
}
async function removeAttachment(item) { try { await ElMessageBox.confirm('删除这张结果凭证？','删除凭证',{type:'warning',confirmButtonText:'删除',cancelButtonText:'取消'}); await retentionDeleteAttachment(form.value.id,item.id); attachments.value=attachments.value.filter(x => x.id !== item.id); ElMessage.success('凭证已删除') } catch { /* cancel */ } }
function ownerLabel(raw) { try { const v=JSON.parse(raw || '{}'); return v.ownerName || staff.value.find(s => String(s.userId) === String(v.ownerId))?.userName || '未分配' } catch { return '未分配' } }
async function download() {
  if(downloading.value) return
  const q=params(), days=(new Date(q.endDate)-new Date(q.beginDate))/86400000
  if(days<0 || days>30) return ElMessage.warning('单次下载最多31天')
  if(total.value>5000) return ElMessage.warning('最多下载5000条，请缩短范围')
  downloading.value=true
  try { const blob=await retentionExport(q); if(blob.type?.includes('json')) { const e=JSON.parse(await blob.text()); throw new Error(e.msg || '下载失败') } saveAs(blob,`挽单转推_${q.beginDate}_${q.endDate}.csv`) }
  catch(e) { ElMessage.error(e.message || '下载失败') }
  finally { downloading.value=false }
}
onMounted(async () => { try { const res=await retentionOptions(); shops.value=res.data.shops; staff.value=res.data.staff; ownOnly.value=!!res.data.ownOnly; canCreate.value=!!res.data.canCreate; adminShopIds.value=res.data.adminShopIds || [] } catch { /* Request helper displays server errors. */ } finally { load() } })
</script>

<style scoped>
.page-heading { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px }
.heading-actions { display:flex; align-items:center; gap:8px }
h2 { margin:0; font-size:20px; font-weight:600 }
.filters :deep(.el-select) { width:180px }
.filters :deep(.el-date-editor) { width:270px }
.download-bar { display:flex; align-items:center; justify-content:space-between; margin-bottom:14px; color:#606266 }
.pagination { justify-content:flex-end; margin-top:18px }
.form-grid { display:grid; grid-template-columns:1fr 1fr; gap:0 20px }
.form-grid :deep(.el-select), .form-grid :deep(.el-input-number) { width:100% }
.outcome { padding-top:12px }
.import-summary { display:flex; flex-wrap:wrap; align-items:center; gap:8px 14px; padding:10px 12px; margin:-8px 0 16px; background:#f7f8fa; border:1px solid #e8ebef; border-radius:6px; color:#697382; font-size:13px }
.import-summary strong { color:#303846 }
.import-summary .invalid { color:#d83b3b }
.batch-summary { padding:12px 14px; margin-bottom:16px; background:#f7f8fa; border-radius:6px; color:#697382; line-height:1.6 }
.batch-summary strong { color:#303846 }
.batch-form { margin-top:16px }
.duplicate-group { margin-top:14px }
.duplicate-group strong { display:block; margin-bottom:6px; color:#303846 }
.duplicate-group div { max-height:120px; padding:9px 11px; overflow:auto; border:1px solid #e4e7ed; border-radius:4px; background:#f7f8fa; color:#606b78; font-family:monospace; font-size:12px; line-height:1.6; white-space:pre-wrap; overflow-wrap:anywhere }
.timestamps { margin-bottom:16px; color:#909399; font-size:13px }
.order-drawer :deep(.el-drawer__header) { margin-bottom:0; padding:18px 20px; border-bottom:1px solid #e8ebef; color:#273142 }
.order-drawer :deep(.el-drawer__body) { padding:16px 20px }
.order-drawer :deep(.el-drawer__footer) { padding:12px 20px; border-top:1px solid #e8ebef }
.detail-header strong { font-size:17px; font-weight:600 }
.drawer-subtitle { margin-top:5px; color:#8a93a0; font-size:12px; overflow-wrap:anywhere }
.profile-strip { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); border:1px solid #e8ebef; border-radius:6px }
.profile-strip>div { padding:12px }
.profile-strip>div+div { border-left:1px solid #e8ebef }
.profile-strip span,.profile-strip strong { display:block }
.profile-strip span { margin-bottom:5px; color:#87909c; font-size:12px }
.profile-strip strong { font-size:14px; font-weight:600 }
.first-followup { display:flex; align-items:center; gap:14px; padding:11px 13px; margin-top:10px; background:#fff7f1; border-left:3px solid #e75e1b; border-radius:4px; font-size:13px }
.first-followup span { color:#7b8491 }
.detail-section { margin-top:16px; border:1px solid #e8ebef; border-radius:6px; overflow:hidden }
.section-title { display:flex; align-items:center; justify-content:space-between; gap:8px; min-height:42px; padding:0 14px; border-bottom:1px solid #edf0f2; font-size:14px }
.section-title span { color:#89919d; font-size:12px }
.order-info { display:grid; grid-template-columns:90px minmax(0,1fr); gap:12px; margin:0; padding:14px; font-size:13px; line-height:1.6 }
.order-info dt { color:#87909c }
.order-info dd { margin:0; white-space:pre-wrap; overflow-wrap:anywhere }
.timeline { padding:2px 14px }
.timeline-item { padding:11px 0; border-bottom:1px solid #eef0f2 }
.timeline-item:last-child { border-bottom:0 }
.timeline-item p { margin:0 0 5px; line-height:1.55 }
.timeline-item span { color:#8b939e; font-size:12px }
.empty-history { padding:16px 0; color:#8b939e; font-size:13px }
.drawer-footer { display:flex; align-items:center; gap:8px }
.footer-spacer { flex:1 }
.registration-form :deep(.el-form-item) { margin-bottom:16px }
.registration-form :deep(.el-form-item__label) { font-weight:500; padding-bottom:5px }
.stage-picker { display:flex; width:100% }
.stage-picker :deep(.el-radio-button) { flex:1 }
.stage-picker :deep(.el-radio-button__inner) { width:100%; padding:8px 4px }
.order-drawer :deep(.el-button--primary) { --el-button-bg-color:#e75e1b; --el-button-border-color:#e75e1b; --el-button-hover-bg-color:#f1783d; --el-button-hover-border-color:#f1783d }
.order-drawer :deep(.el-radio-button.is-active .el-radio-button__inner) { background:#e75e1b; border-color:#e75e1b; box-shadow:-1px 0 0 0 #e75e1b }
.note { white-space:pre-wrap; overflow-wrap:anywhere }
.change { margin-top:6px; color:#606266 }
.dashboard { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:16px }
.metric { padding:24px; border:1px solid #ebeef5; border-radius:6px }
.metric span { display:block; color:#606266; margin-bottom:12px }
.metric strong { font-size:26px; font-weight:600 }
.evidence-upload { margin-top:16px }
.evidence-upload small { display:block; width:100%; margin-top:6px; color:#909399 }
.evidence-grid { display:grid; grid-template-columns:repeat(4,minmax(0,1fr)); gap:10px; padding:14px }
.evidence-item { position:relative; aspect-ratio:1; overflow:hidden; border:1px solid #e8ebef; border-radius:6px }
.evidence-item :deep(.el-image) { width:100%; height:100% }
.evidence-item .el-button { position:absolute; top:4px; right:4px; background:rgba(255,255,255,.9) }
@media(max-width:700px) { .form-grid,.dashboard { grid-template-columns:1fr } .section-title { flex-wrap:wrap; padding-top:8px; padding-bottom:8px } .order-info { grid-template-columns:76px minmax(0,1fr) } }
</style>
