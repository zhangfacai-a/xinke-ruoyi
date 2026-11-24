<template>
  <div class="app-container jd-page">
    <div class="page-head">
      <div><h2>京东客户信息</h2><span>客户联系方式按订单归档</span></div>
      <div class="actions">
        <el-button :icon="Upload" :loading="importing" @click="fileInput?.click()">导入 RPA 文件</el-button>
        <el-button @click="mergeVisible = true">合并客户档案</el-button>
        <input ref="fileInput" type="file" accept=".xlsx,.csv" hidden @change="upload" />
      </div>
    </div>
    <el-tabs v-model="tab" @tab-change="load"><el-tab-pane label="接待记录" name="records" /><el-tab-pane label="客户档案" name="customers" /><el-tab-pane label="导入批次" name="batches" /></el-tabs>
    <div v-if="tab !== 'batches'" class="filters">
      <el-input v-model="search" clearable :placeholder="tab === 'records' ? '客户昵称、客服昵称或订单号' : '客户昵称、手机号、微信号或订单号'" @keyup.enter="resetSearch" />
      <el-select v-if="tab === 'records'" v-model="status" clearable placeholder="全部匹配状态"><el-option label="已关联" value="MATCHED" /><el-option label="无订单" value="NO_ORDER" /><el-option label="归属冲突" value="CONFLICT" /></el-select>
      <el-button type="primary" :icon="Search" @click="resetSearch">查询</el-button>
    </div>
    <el-table v-if="tab === 'records'" v-loading="loading" :data="rows" border row-key="recordId" empty-text="暂无接待记录">
      <el-table-column prop="occurredAt" label="时间" width="175" />
      <el-table-column prop="serviceNickname" label="客服昵称" min-width="110" />
      <el-table-column prop="customerNickname" label="客户昵称" min-width="170" />
      <el-table-column prop="rawOrderNos" label="订单号" min-width="230" show-overflow-tooltip />
      <el-table-column label="匹配" width="100"><template #default="{ row }"><el-tag :type="row.matchStatus === 'MATCHED' ? 'success' : row.matchStatus === 'CONFLICT' ? 'danger' : 'info'">{{ statusLabel(row.matchStatus) }}</el-tag></template></el-table-column>
      <el-table-column prop="fileName" label="来源文件" min-width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right"><template #default="{ row }"><el-button v-if="row.customerId" link type="primary" @click="openCustomer(row.customerId)">客户档案</el-button><el-button v-else-if="row.matchStatus === 'CONFLICT'" link type="primary" @click="openConflict(row.recordId)">查看归属</el-button></template></el-table-column>
    </el-table>
    <el-table v-else-if="tab === 'customers'" v-loading="loading" :data="rows" border row-key="customerId" empty-text="暂无客户档案">
      <el-table-column prop="customerId" label="客户 ID" width="100" />
      <el-table-column prop="displayNickname" label="客户昵称" min-width="180" />
      <el-table-column prop="orderCount" label="订单数" width="90" />
      <el-table-column prop="contactPhone" label="手机号" min-width="160" />
      <el-table-column prop="contactWechat" label="微信号" min-width="160" />
      <el-table-column prop="lastSeenAt" label="最近接待" width="175" />
      <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button link type="primary" @click="openCustomer(row.customerId)">查看 / 编辑</el-button></template></el-table-column>
    </el-table>
    <el-table v-else v-loading="loading" :data="rows" border row-key="batchId" empty-text="暂无导入记录">
      <el-table-column prop="fileName" label="文件名" min-width="230" /><el-table-column prop="importedAt" label="导入时间" width="175" /><el-table-column prop="sourceRowCount" label="文件行数" width="100" /><el-table-column prop="newRecordCount" label="新增记录" width="100" /><el-table-column prop="duplicateRecordCount" label="跳过重复" width="100" />
    </el-table>
    <pagination v-if="tab !== 'batches'" :total="total" v-model:page="pageNum" v-model:limit="pageSize" @pagination="load" />

    <el-drawer v-model="drawer" title="客户档案" size="min(540px, 100%)">
      <template v-if="profile">
        <div class="profile-title">{{ profile.displayNickname }} <small>#{{ profile.customerId }}</small></div>
        <el-form label-position="top" @submit.prevent="saveContact">
          <el-form-item label="手机号"><el-input v-model.trim="contact.phone" maxlength="32" clearable /></el-form-item>
          <el-form-item label="微信号"><el-input v-model.trim="contact.wechat" maxlength="100" clearable /></el-form-item>
          <el-button type="primary" :loading="saving" @click="saveContact">保存联系方式</el-button>
        </el-form>
        <h3>关联订单</h3>
        <el-table :data="profile.orders" size="small" max-height="360"><el-table-column prop="orderNo" label="订单号" /><el-table-column prop="lastSeenAt" label="最近出现" width="170" /></el-table>
      </template>
    </el-drawer>
    <el-dialog v-model="linkVisible" title="关联已有客户" width="480px">
      <el-form label-position="top"><el-form-item label="订单号"><el-input v-model.trim="link.orderNo" maxlength="64" /></el-form-item><el-form-item label="目标客户 ID"><el-input v-model.trim="link.customerId" placeholder="从客户档案中查看 ID" /></el-form-item></el-form>
      <template #footer><el-button @click="linkVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="saveLink">确认关联</el-button></template>
    </el-dialog>
    <el-dialog v-model="mergeVisible" title="合并客户档案" width="480px">
      <el-form label-position="top"><el-form-item label="来源客户 ID"><el-input v-model.trim="merge.sourceId" /></el-form-item><el-form-item label="保留的客户 ID"><el-input v-model.trim="merge.targetId" /></el-form-item></el-form>
      <template #footer><el-button @click="mergeVisible = false">取消</el-button><el-button type="danger" :loading="saving" @click="saveMerge">确认合并</el-button></template>
    </el-dialog>
    <el-dialog v-model="conflictVisible" title="订单归属冲突" width="560px">
      <el-table :data="conflictOrders" border><el-table-column prop="orderNo" label="订单号" min-width="180" /><el-table-column prop="customerId" label="客户 ID" width="100" /><el-table-column prop="displayNickname" label="客户昵称" min-width="120" /><el-table-column label="操作" width="90"><template #default="{ row }"><el-button v-if="!row.customerId" link type="primary" @click="conflictVisible = false; openLink(row.orderNo)">关联</el-button></template></el-table-column></el-table>
      <template #footer><el-button @click="conflictVisible = false">关闭</el-button><el-button type="primary" @click="conflictVisible = false; mergeVisible = true">合并客户档案</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Upload, Search } from '@element-plus/icons-vue'
import { jdRecords, jdRecordOrders, jdCustomers, jdCustomer, jdBatches, jdContact, jdLinkOrder, jdMerge, jdImport } from '@/api/live/jdCustomers'

const tab = ref('records'), search = ref(''), status = ref(''), rows = ref([]), total = ref(0)
const pageNum = ref(1), pageSize = ref(20), loading = ref(false), importing = ref(false), saving = ref(false)
const fileInput = ref(), drawer = ref(false), profile = ref(null), contact = ref({ phone: '', wechat: '' })
const linkVisible = ref(false), link = ref({ orderNo: '', customerId: '' })
const mergeVisible = ref(false), merge = ref({ sourceId: '', targetId: '' })
const conflictVisible = ref(false), conflictOrders = ref([])
const statusLabel = value => ({ MATCHED: '已关联', NO_ORDER: '无订单', CONFLICT: '归属冲突' }[value] || value)
const resetSearch = () => { pageNum.value = 1; load() }
async function load() {
  loading.value = true
  try {
    const result = tab.value === 'batches' ? await jdBatches() : tab.value === 'records'
      ? await jdRecords({ search: search.value, status: status.value, pageNum: pageNum.value, pageSize: pageSize.value })
      : await jdCustomers({ search: search.value, pageNum: pageNum.value, pageSize: pageSize.value })
    rows.value = tab.value === 'batches' ? result.data : result.data.rows
    total.value = tab.value === 'batches' ? rows.value.length : result.data.total
  } finally { loading.value = false }
}
async function upload(event) {
  const file = event.target.files?.[0]; event.target.value = ''; if (!file) return
  importing.value = true
  try {
    const result = await jdImport(file)
    ElMessage.success(`导入 ${result.data.imported} 条，跳过重复 ${result.data.duplicates} 条`)
    pageNum.value = 1; await load()
  } catch (error) { ElMessageBox.alert(error.message || '导入失败', '导入结果', { type: 'warning' }) }
  finally { importing.value = false }
}
async function openCustomer(id) {
  const result = await jdCustomer(id); profile.value = result.data
  contact.value = { phone: profile.value.contactPhone || '', wechat: profile.value.contactWechat || '' }
  drawer.value = true
}
async function saveContact() {
  saving.value = true
  try { await jdContact(profile.value.customerId, contact.value); ElMessage.success('联系方式已保存'); await openCustomer(profile.value.customerId); await load() }
  catch (error) { ElMessage.error(error.message || '保存失败') } finally { saving.value = false }
}
function openLink(orderNo) { link.value = { orderNo, customerId: '' }; linkVisible.value = true }
async function openConflict(id) { const result = await jdRecordOrders(id); conflictOrders.value = result.data; conflictVisible.value = true }
async function saveLink() {
  saving.value = true
  try { await jdLinkOrder(link.value); linkVisible.value = false; ElMessage.success('订单已关联'); await load() }
  catch (error) { ElMessage.error(error.message || '关联失败') } finally { saving.value = false }
}
async function saveMerge() {
  try { await ElMessageBox.confirm('来源客户的订单会转到保留客户名下，合并后无法直接撤销。请确认两个档案属于同一客户。', '确认合并', { type: 'warning' }) }
  catch { return }
  saving.value = true
  try { await jdMerge(merge.value); mergeVisible.value = false; ElMessage.success('客户档案已合并'); await load() }
  catch (error) { ElMessage.error(error.message || '合并失败') } finally { saving.value = false }
}
onMounted(load)
</script>

<style scoped>
.jd-page { color: #253343; }
.page-head { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin-bottom: 18px; }
.page-head h2 { font-size: 21px; margin: 0 0 5px; }
.page-head span { font-size: 13px; color: #667787; }
.filters { display: flex; align-items: center; gap: 10px; margin: 14px 0; }
.filters .el-input { width: min(390px, 100%); }
.filters .el-select { width: 160px; }
.profile-title { font-size: 19px; font-weight: 600; margin: 4px 0 20px; }
.profile-title small { color: #788693; font-size: 13px; font-weight: 400; }
h3 { font-size: 15px; margin: 30px 0 12px; }
@media (max-width: 700px) { .page-head, .filters { align-items: stretch; flex-direction: column; } .filters .el-input, .filters .el-select { width: 100%; } }
</style>
