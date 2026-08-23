<template>
  <div class="app-container gift-ledger">
    <header class="page-head">
      <div>
        <h2>订单备注 / 礼品记录</h2>
        <p>订单号到手即可记录主播、场控、到返、售后、服务标记和礼品。</p>
      </div>
      <div class="head-actions">
        <el-button v-hasPermi="['live:gift:export']" icon="Download" @click="handleExport">导出</el-button>
      </div>
    </header>

    <section class="room-context"><div class="room-context-copy"><span>当前直播间</span><strong>{{ currentRoom?.roomName || '全部直播间' }}</strong><small>{{ currentRoom ? `新订单默认归入这里 · 列表仅显示这里 · 平台ID ${currentRoom.roomCode}` : '新订单不指定直播间 · 列表显示全部记录' }}</small></div><el-select v-model="currentRoomId" clearable filterable placeholder="全部直播间（可不选）" @change="changeCurrentRoom"><el-option v-for="item in rooms" :key="item.roomId" :label="`${item.roomName} · ${item.roomCode}`" :value="item.roomId" /></el-select><div class="room-people"><span>主播</span><strong>{{ currentRoomPeople.anchor || '未配置' }}</strong></div><div class="room-people"><span>场控</span><strong>{{ currentRoomPeople.controller || '未配置' }}</strong></div></section>

    <section v-hasPermi="['live:gift:entry']" class="entry-workbench">
      <div class="entry-mode-row">
        <div><strong>快速录入</strong><span>录入人：{{ currentUser }}</span></div>
        <el-segmented v-model="entryMode" :options="[{ label: '单个订单', value: 'single' }, { label: '批量订单', value: 'batch' }]" />
      </div>
      <div v-if="entryMode === 'single'" class="single-entry-row">
        <el-input ref="quickOrderInput" v-model.trim="quickOrderNo" size="large" clearable maxlength="64" placeholder="输入平台订单号，按回车开始" @keyup.enter="openEntry(quickOrderNo)">
          <template #prefix><el-icon><Tickets /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" :disabled="!quickOrderNo" @click="openEntry(quickOrderNo)">录入</el-button>
      </div>
      <div v-else class="batch-entry-row">
        <el-input v-model="batchOrderText" type="textarea" :rows="3" resize="none" placeholder="粘贴订单号：支持 Excel 一列、换行、逗号或空格，自动去空和去重" />
        <div class="batch-entry-side">
          <span>识别 <strong>{{ parsedBatchOrders.length }}</strong> 个订单</span>
          <small v-if="batchDuplicateCount">已去重 {{ batchDuplicateCount }} 个</small>
          <el-button type="primary" size="large" :disabled="!parsedBatchOrders.length || parsedBatchOrders.length > 500" @click="openBatchEntry">统一配置</el-button>
        </div>
      </div>
      <el-alert v-if="entryMode === 'batch' && parsedBatchOrders.length > 500" title="单次最多录入 500 个订单，请分批处理" type="warning" :closable="false" show-icon />
    </section>

    <section class="filter-bar">
      <el-input v-model.trim="query.orderNo" clearable placeholder="搜索订单号" class="order-filter" @keyup.enter="load">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-date-picker v-model="dates" type="daterange" value-format="YYYY-MM-DD" start-placeholder="操作开始日" end-placeholder="操作结束日" @change="load" />
      <el-button type="primary" icon="Search" @click="load">查询</el-button>
      <el-button icon="Refresh" @click="resetQuery">重置</el-button>
    </section>

    <el-table v-loading="loading" :data="rows" stripe @row-dblclick="editRecord">
      <el-table-column prop="orderNo" label="订单号" min-width="200">
        <template #default="{ row }"><span class="order-no">{{ row.orderNo }}</span></template>
      </el-table-column>
      <el-table-column prop="entryDate" label="录入日期" width="112" />
      <el-table-column prop="roomNameSnapshot" label="直播间" min-width="140" show-overflow-tooltip><template #default="{row}">{{ row.roomNameSnapshot || '未指定' }}</template></el-table-column>
      <el-table-column label="主播 / 场控" min-width="150" show-overflow-tooltip>
        <template #default="{ row }"><span>{{ [row.anchorName, row.controllerName].filter(Boolean).join(' / ') || '-' }}</span></template>
      </el-table-column>
      <el-table-column prop="giftText" label="礼品" min-width="220" show-overflow-tooltip>
        <template #default="{ row }"><span :class="{ muted: !row.giftText }">{{ row.giftText || statusMeta(row.processStatus).label }}</span></template>
      </el-table-column>
      <el-table-column label="到返金额" width="105" align="right"><template #default="{ row }">{{ row.refundAmount == null ? '-' : `¥${money(row.refundAmount)}` }}</template></el-table-column>
      <el-table-column label="服务项" min-width="150"><template #default="{ row }"><div class="service-tags"><el-tag v-for="item in serviceTags(row)" :key="item" size="small" type="warning" effect="light">{{ item }}</el-tag><span v-if="!serviceTags(row).length" class="muted">-</span></div></template></el-table-column>
      <el-table-column label="成本" width="110" align="right">
        <template #default="{ row }"><strong>¥{{ money(row.giftCost) }}</strong></template>
      </el-table-column>
      <el-table-column label="状态" width="112">
        <template #default="{ row }"><el-tag :type="statusMeta(row.processStatus).type" effect="light">{{ statusMeta(row.processStatus).label }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createBy" label="录入人" width="110" />
      <el-table-column prop="operateTime" label="最后操作" width="165" />
      <el-table-column v-hasPermi="['live:gift:entry']" label="操作" width="82" fixed="right">
        <template #default="{ row }"><el-button link type="primary" icon="Edit" @click="editRecord(row)">修改</el-button></template>
      </el-table-column>
      <template #empty><el-empty description="还没有礼品记录" /></template>
    </el-table>

    <el-drawer v-model="entryVisible" :title="entryTitle" size="min(960px, 96vw)" destroy-on-close @opened="focusEntryInput">
      <div class="drawer-body">
        <div v-if="activeEntryMode === 'single'" class="order-lookup">
          <el-input
            ref="entryOrderInput"
            v-model.trim="entryOrderNo"
            size="large"
            clearable
            maxlength="64"
            placeholder="输入订单号后按回车"
            @keyup.enter="findOrder"
          >
            <template #prepend>订单号</template>
          </el-input>
          <el-button type="primary" size="large" :loading="orderLoading" :disabled="!entryOrderNo" @click="findOrder">确认</el-button>
        </div>

        <el-empty v-if="!order" description="先输入订单号" :image-size="82" />
        <template v-else>
          <div class="entry-identity">
            <div>
              <span>{{ activeEntryMode === 'batch' ? '本次批量' : '当前订单' }}</span>
              <strong>{{ activeEntryMode === 'batch' ? `${batchOrderNos.length} 个订单` : order.orderNo }}</strong>
              <el-tag v-if="activeEntryMode === 'single'" :type="isEditing ? 'warning' : 'success'" effect="light">{{ isEditing ? '已有记录 · 修改' : '首次录入 · 新增' }}</el-tag>
            </div>
          <div><span>录入人</span><strong>{{ currentUser }}</strong></div><div><span>直播间</span><strong>{{ entryForm.roomNameSnapshot || '未指定' }}</strong></div>
          </div>
          <div v-if="activeEntryMode === 'batch'" class="batch-policy">
            <div><strong>订单号已清洗</strong><span>{{ batchOrderNos.slice(0, 5).join('、') }}{{ batchOrderNos.length > 5 ? ` 等 ${batchOrderNos.length} 个` : '' }}</span></div>
            <el-checkbox v-model="overwriteExisting">覆盖已有记录</el-checkbox>
            <small>{{ overwriteExisting ? '已有订单会替换原记录并重新计算库存' : '已有记录默认跳过，避免误覆盖' }}</small>
          </div>

          <section class="entry-section template-section">
            <div class="section-title"><div><strong>快捷模板</strong><span>点一下填入整套订单信息，之后仍可修改</span></div></div>
            <div class="template-chips">
              <button type="button" :class="{ active: activeTemplateId == null }" @click="useManualMode"><el-icon><EditPen /></el-icon>自定义</button>
              <button v-for="item in enabledTemplates" :key="item.templateId" type="button" :class="{ active: activeTemplateId === item.templateId }" @click="applyTemplate(item)"><el-icon><MagicStick /></el-icon>{{ item.templateName }}</button>
              <span v-if="!enabledTemplates.length" class="template-empty">当前账号还没有启用的模板</span>
            </div>
          </section>

          <section class="entry-section service-section">
            <div class="section-title"><div><strong>订单服务信息</strong><span>空白项不计入预览和统计</span></div></div>
            <div class="service-grid">
              <el-form-item label="主播"><el-select v-model="entryForm.anchorUserId" filterable clearable placeholder="选择主播" @change="syncStaffSnapshot"><el-option v-for="item in anchorOptions" :key="item.userId" :label="item.userName" :value="item.userId" /></el-select></el-form-item>
              <el-form-item label="场控"><el-select v-model="entryForm.controllerUserId" filterable clearable placeholder="选择场控" @change="syncStaffSnapshot"><el-option v-for="item in controllerOptions" :key="item.userId" :label="item.userName" :value="item.userId" /></el-select></el-form-item>
              <el-form-item label="到返金额"><el-input-number v-model="entryForm.refundAmount" :min="0" :precision="2" :step="1" :value-on-clear="null" controls-position="right" /></el-form-item>
              <el-form-item label="服务标记"><el-input v-model="entryForm.serviceMark" clearable placeholder="例如：重点跟进" /></el-form-item>
              <el-form-item label="到返理由" class="span-2"><el-input v-model="entryForm.refundReason" clearable placeholder="填写到返原因" /></el-form-item>
              <el-form-item label="售后补偿" class="span-2"><el-input v-model="entryForm.afterSaleCompensation" clearable placeholder="填写售后补偿内容" /></el-form-item>
              <el-form-item label="其他备注" class="span-4"><el-input v-model="entryForm.otherRemark" type="textarea" :rows="2" maxlength="500" placeholder="补充说明" /></el-form-item>
            </div>
            <div class="boolean-grid">
              <el-checkbox v-model="entryForm.extendedWarranty">是否延保</el-checkbox>
              <el-checkbox v-model="entryForm.priceProtection">是否价保</el-checkbox>
              <el-checkbox v-model="entryForm.delayed">是否延迟</el-checkbox>
              <el-checkbox v-model="entryForm.followUp">是否追单</el-checkbox>
              <el-checkbox v-model="entryForm.urgent">是否加急</el-checkbox>
            </div>
          </section>

          <div class="gift-workspace">
              <section class="gift-catalog">
                <div class="section-title">
                  <div><strong>选择礼品</strong><span>支持名称、编码、别名和拼音简写</span></div>
                  <el-input v-model.trim="giftKeyword" clearable placeholder="搜索礼品" />
                </div>
                <div class="gift-grid">
                  <button v-for="item in filteredGifts" :key="item.giftId" type="button" :disabled="item.currentCost == null" @click="chooseGift(item)">
                    <strong>{{ item.giftName }}</strong>
                    <span>{{ item.giftCode }} · 库存 {{ item.stockQty ?? 0 }}</span>
                    <b v-if="item.currentCost != null">¥{{ money(item.currentCost) }}</b>
                    <em v-else>未设置成本</em>
                  </button>
                  <el-empty v-if="!filteredGifts.length" description="没有匹配的礼品" :image-size="58" />
                </div>
              </section>

              <section ref="selectedListRef" class="selected-gifts">
                <div class="section-title">
                  <div><strong>本单礼品</strong><span>共 {{ giftCount }} 件</span></div>
                </div>
                  <el-empty v-if="!(entryForm.gifts || []).length" description="从左侧点选礼品，或套用快捷模板" :image-size="64" />
                  <div v-for="item in (entryForm.gifts || [])" :key="item.giftId" class="selected-row">
                  <div class="selected-name"><strong>{{ item.giftName }}</strong><span>¥{{ money(item.currentCost) }}/{{ item.unit || '件' }}</span></div>
                  <el-input-number v-model="item.quantity" :min="1" :max="10" @change="handleQuantityChange(item)" />
                  <strong class="line-cost">¥{{ money(Number(item.currentCost || 0) * item.quantity) }}</strong>
                  <el-button link type="danger" icon="Delete" title="移除" @click="removeGift(item.giftId)" />
                </div>
                <div class="gift-total"><span>预计礼品成本</span><strong>¥{{ money(giftTotal) }}</strong></div>
              </section>
          </div>

          <section class="entry-section note-section">
            <div class="section-title">
              <div><strong>结构化预览</strong><span>保存时会同时保存结构化字段和可解析文本</span></div>
              <span class="note-count">{{ (previewText || '').length }}/2000</span>
            </div>
            <el-input :model-value="previewText" type="textarea" :rows="4" readonly />
          </section>
        </template>
      </div>
      <template #footer>
        <div class="drawer-actions">
          <el-button @click="entryVisible = false">关闭</el-button>
          <div>
            <el-button v-if="activeEntryMode === 'single'" :disabled="!order" :loading="saving" @click="saveEntry(false)">保存</el-button>
            <el-button type="primary" :disabled="!order" :loading="saving" @click="saveEntry(activeEntryMode === 'single')">{{ activeEntryMode === 'batch' ? `确认录入 ${batchOrderNos.length} 单` : '保存并录下一单' }}</el-button>
          </div>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup name="GiftLedger">
import { batchSaveOrderGifts, getGiftOrder, getRoomPreference, getTemplate, listGiftLedger, listGifts, listMappings, listRooms, listStaff, listTemplates, saveOrderGifts, saveRoomPreference } from '@/api/live/gift'
import useUserStore from '@/store/modules/user'
import { booleanValue, formatLiveTemplate } from '@/utils/liveTemplateCodec'
import { useRoute, useRouter } from 'vue-router'

const { proxy } = getCurrentInstance()
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const statuses = [
  { value: 'selected', label: '已选礼品', type: 'success' },
  { value: 'customer_declined', label: '顾客未选', type: 'info' },
  { value: 'not_applicable', label: '本单不送', type: 'info' },
  { value: 'pending', label: '稍后确认', type: 'warning' }
]

const rows = ref([])
const gifts = ref([])
const templates = ref([])
const staffOptions = ref([])
const mappings = ref([])
const rooms = ref([])
const currentRoomId = ref(null)
const dates = ref([])
const loading = ref(false)
const quickOrderNo = ref('')
const quickOrderInput = ref()
const entryMode = ref('single')
const activeEntryMode = ref('single')
const batchOrderText = ref('')
const batchOrderNos = ref([])
const overwriteExisting = ref(false)
const entryVisible = ref(false)
const entryOrderInput = ref()
const selectedListRef = ref()
const entryOrderNo = ref('')
const order = ref()
const orderLoading = ref(false)
const saving = ref(false)
const giftKeyword = ref('')
const activeTemplateId = ref(null)
const activeTemplateFields = ref([])
const query = ref({ orderNo: '', giftName: '' })
const entryForm = ref(emptyEntryForm())

const currentUser = computed(() => userStore.nickName || userStore.name || '当前用户')
const currentRoom = computed(() => rooms.value.find(item => Number(item.roomId) === Number(currentRoomId.value)))
const currentRoomMapping = computed(() => mappings.value.find(item => item.subjectType === 'ROOM' && Number(item.subjectId) === Number(currentRoomId.value)))
const currentRoomPeople = computed(() => ({ anchor: currentRoomMapping.value?.anchorNames || '', controller: currentRoomMapping.value?.controllerNames || '' }))
const isEditing = computed(() => order.value?.entryMode === 'edit')
const entryTitle = computed(() => activeEntryMode.value === 'batch' ? '批量录入订单信息' : !order.value ? '录入订单信息' : isEditing.value ? '修改订单记录' : '新增订单记录')
const rawBatchOrders = computed(() => String(batchOrderText.value || '').split(/[\s,，;；]+/).map(item => item.trim()).filter(Boolean))
const parsedBatchOrders = computed(() => [...new Set(rawBatchOrders.value)].filter(item => item.length <= 64))
const batchDuplicateCount = computed(() => rawBatchOrders.value.length - parsedBatchOrders.value.length)
const enabledTemplates = computed(() => templates.value.filter(item => item.status === '0'))
const anchorOptions = computed(() => roleOptions('anchor'))
const controllerOptions = computed(() => roleOptions('controller'))
const previewText = computed(() => formatLiveTemplate({ fields: previewFields.value, gifts: entryForm.value.gifts || [] }, gifts.value) || '')
const previewFields = computed(() => [
  { key: 'anchor', label: '主播', type: 'text', enabled: !!entryForm.value.anchorNameSnapshot, value: entryForm.value.anchorNameSnapshot },
  { key: 'controller', label: '场控', type: 'text', enabled: !!entryForm.value.controllerNameSnapshot, value: entryForm.value.controllerNameSnapshot },
  { key: 'refundAmount', label: '到返金额', type: 'number', enabled: entryForm.value.refundAmount != null, value: entryForm.value.refundAmount },
  { key: 'otherRemark', label: '其他备注', type: 'text', enabled: !!entryForm.value.otherRemark, value: entryForm.value.otherRemark },
  { key: 'refundReason', label: '到返理由', type: 'text', enabled: !!entryForm.value.refundReason, value: entryForm.value.refundReason },
  { key: 'afterSaleCompensation', label: '售后补偿', type: 'text', enabled: !!entryForm.value.afterSaleCompensation, value: entryForm.value.afterSaleCompensation },
  { key: 'serviceMark', label: '服务标记', type: 'text', enabled: !!entryForm.value.serviceMark, value: entryForm.value.serviceMark },
  { key: 'extendedWarranty', label: '是否延保', type: 'boolean', enabled: !!entryForm.value.extendedWarranty, value: entryForm.value.extendedWarranty },
  { key: 'priceProtection', label: '是否价保', type: 'boolean', enabled: !!entryForm.value.priceProtection, value: entryForm.value.priceProtection },
  { key: 'delayed', label: '是否延迟', type: 'boolean', enabled: !!entryForm.value.delayed, value: entryForm.value.delayed },
  { key: 'followUp', label: '是否追单', type: 'boolean', enabled: !!entryForm.value.followUp, value: entryForm.value.followUp },
  { key: 'urgent', label: '是否加急', type: 'boolean', enabled: !!entryForm.value.urgent, value: entryForm.value.urgent }
])
const isOn = value => value === true || value === 1 || value === '1' || value === 'true' || value === '是'
const filteredGifts = computed(() => {
  const keyword = giftKeyword.value.toLocaleLowerCase()
  return gifts.value
    .filter(item => !isOn(item.personalHidden))
    .filter(item => !keyword || [item.giftName, item.giftCode, item.shortName, item.aliases]
      .some(value => String(value || '').toLocaleLowerCase().includes(keyword)))
    .sort((a, b) => Number(isOn(b.personalPinned)) - Number(isOn(a.personalPinned)))
})
const giftCount = computed(() => entryForm.value.gifts.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const giftTotal = computed(() => entryForm.value.gifts.reduce((sum, item) => sum + Number(item.currentCost || 0) * Number(item.quantity || 0), 0))

function emptyEntryForm() {
  return { processStatus: 'selected', gifts: [], operatorNote: '', roomId: null, roomCodeSnapshot: '', roomNameSnapshot: '', anchorUserId: null, anchorNameSnapshot: '', controllerUserId: null, controllerNameSnapshot: '', refundAmount: null, refundReason: '', otherRemark: '', afterSaleCompensation: '', serviceMark: '', extendedWarranty: false, priceProtection: false, delayed: false, followUp: false, urgent: false, templateId: null, templateNameSnapshot: '', parsedText: '' }
}

function money(value) {
  return Number(value || 0).toFixed(2)
}

function statusMeta(value) {
  return statuses.find(item => item.value === value) || { label: value || '-', type: 'info' }
}

function ids(value) { return String(value || '').split(',').filter(Boolean).map(Number) }
function names(value) { return String(value || '').split('、').filter(Boolean) }
function roleOptions(role) {
  const idKey = `${role}Ids`, nameKey = `${role}Names`, result = new Map()
  mappings.value.filter(item => item.status === '0').forEach(item => {
    const roleIds = ids(item[idKey]), roleNames = names(item[nameKey])
    roleIds.forEach((id, index) => {
      const staff = staffOptions.value.find(option => Number(option.userId) === id)
      result.set(id, { userId: id, userName: staff?.userName || roleNames[index] || String(id) })
    })
  })
  return [...result.values()].sort((a, b) => a.userName.localeCompare(b.userName, 'zh-CN'))
}
function syncStaffSnapshot() {
  entryForm.value.anchorNameSnapshot = anchorOptions.value.find(item => Number(item.userId) === Number(entryForm.value.anchorUserId))?.userName || ''
  entryForm.value.controllerNameSnapshot = controllerOptions.value.find(item => Number(item.userId) === Number(entryForm.value.controllerUserId))?.userName || ''
}
async function changeCurrentRoom() {
  await saveRoomPreference({ roomId: currentRoomId.value || null })
  proxy.$modal.msgSuccess(currentRoom.value ? `已切换到 ${currentRoom.value.roomName}` : '已清除默认直播间')
  await load()
}
function applyCurrentRoomPeople(form) {
  form.roomId = currentRoom.value?.roomId || null
  form.roomCodeSnapshot = currentRoom.value?.roomCode || ''
  form.roomNameSnapshot = currentRoom.value?.roomName || ''
  const mapping = currentRoomMapping.value
  if (!mapping) return
  const anchorId = ids(mapping.anchorIds)[0], controllerId = ids(mapping.controllerIds)[0]
  if (anchorId) { form.anchorUserId = anchorId; form.anchorNameSnapshot = names(mapping.anchorNames)[0] || staffOptions.value.find(item => Number(item.userId) === anchorId)?.userName || '' }
  if (controllerId) { form.controllerUserId = controllerId; form.controllerNameSnapshot = names(mapping.controllerNames)[0] || staffOptions.value.find(item => Number(item.userId) === controllerId)?.userName || '' }
}
function serviceTags(row) {
  return [{ value: row.extendedWarranty, label: '延保' }, { value: row.priceProtection, label: '价保' }, { value: row.delayed, label: '延迟' }, { value: row.followUp, label: '追单' }, { value: row.urgent, label: '加急' }].filter(item => item.value).map(item => item.label)
}

function queryPayload() {
  return { ...query.value, roomId: currentRoomId.value || null, beginDate: dates.value?.[0], endDate: dates.value?.[1] }
}

async function load() {
  loading.value = true
  try {
    rows.value = (await listGiftLedger(queryPayload())).data || []
  } finally {
    loading.value = false
  }
}

function resetQuery() {
  query.value = { orderNo: '', giftName: '' }
  dates.value = []
  router.replace({ query: {} })
  load()
}

function resetEntry(keepOrderNo = false) {
  order.value = null
  if (!keepOrderNo) entryOrderNo.value = ''
  entryForm.value = emptyEntryForm()
  applyCurrentRoomPeople(entryForm.value)
  activeTemplateId.value = null
  activeTemplateFields.value = []
  giftKeyword.value = ''
}

function openEntry(orderNo = '') {
  const normalized = String(orderNo || '').trim()
  activeEntryMode.value = 'single'
  entryVisible.value = true
  resetEntry()
  entryOrderNo.value = normalized
  if (normalized) nextTick(findOrder)
}

function openBatchEntry() {
  if (!parsedBatchOrders.value.length) return proxy.$modal.msgWarning('请先粘贴订单号')
  if (parsedBatchOrders.value.length > 500) return proxy.$modal.msgWarning('单次最多录入500个订单')
  activeEntryMode.value = 'batch'
  batchOrderNos.value = [...parsedBatchOrders.value]
  overwriteExisting.value = false
  resetEntry()
  order.value = { orderNo: '', entryMode: 'batch' }
  entryVisible.value = true
}

function focusEntryInput() {
  nextTick(() => entryOrderInput.value?.focus())
}

async function findOrder() {
  if (!entryOrderNo.value) return proxy.$modal.msgWarning('请输入订单号')
  orderLoading.value = true
  try {
    const response = await getGiftOrder(entryOrderNo.value)
    order.value = response.data
    entryOrderNo.value = response.data.orderNo
    gifts.value = (await listGifts({ status: '0', includeHidden: '1', costDate: response.data.orderDate })).data || []
    entryForm.value = {
      processStatus: response.data.giftStatus?.processStatus || 'selected',
      operatorNote: response.data.giftStatus?.operatorNote || '',
      roomId: response.data.giftStatus?.roomId || currentRoom.value?.roomId || null,
      roomCodeSnapshot: response.data.giftStatus?.roomCodeSnapshot || currentRoom.value?.roomCode || '',
      roomNameSnapshot: response.data.giftStatus?.roomNameSnapshot || currentRoom.value?.roomName || '',
      anchorUserId: response.data.giftStatus?.anchorUserId || null,
      anchorNameSnapshot: response.data.giftStatus?.anchorNameSnapshot || '',
      controllerUserId: response.data.giftStatus?.controllerUserId || null,
      controllerNameSnapshot: response.data.giftStatus?.controllerNameSnapshot || '',
      refundAmount: response.data.giftStatus?.refundAmount == null ? null : Number(response.data.giftStatus.refundAmount),
      refundReason: response.data.giftStatus?.refundReason || '',
      otherRemark: response.data.giftStatus?.otherRemark || response.data.giftStatus?.operatorNote || '',
      afterSaleCompensation: response.data.giftStatus?.afterSaleCompensation || '',
      serviceMark: response.data.giftStatus?.serviceMark || '',
      extendedWarranty: booleanValue(response.data.giftStatus?.extendedWarranty),
      priceProtection: booleanValue(response.data.giftStatus?.priceProtection),
      delayed: booleanValue(response.data.giftStatus?.delayed),
      followUp: booleanValue(response.data.giftStatus?.followUp),
      urgent: booleanValue(response.data.giftStatus?.urgent),
      templateId: response.data.giftStatus?.templateId || null,
      templateNameSnapshot: response.data.giftStatus?.templateNameSnapshot || '',
      parsedText: response.data.giftStatus?.parsedText || '',
      gifts: (response.data.gifts || []).map(item => ({ ...item, currentCost: item.unitCost }))
    }
    if (!response.data.giftStatus) applyCurrentRoomPeople(entryForm.value)
    activeTemplateId.value = null
    activeTemplateFields.value = []
  } finally {
    orderLoading.value = false
  }
}

function parseTemplateContent(value) {
  try {
    return typeof value === 'string' ? JSON.parse(value) : value || {}
  } catch {
    return {}
  }
}

async function applyTemplate(item) {
  if (activeTemplateId.value !== item.templateId && hasEntryContent()) {
    try { await proxy.$modal.confirm(`使用“${item.templateName}”会替换当前已填写内容，是否继续？`) } catch { return }
  }
  const detail = (await getTemplate(item.templateId)).data
  const content = parseTemplateContent(detail.contentJson)
  const selected = []
  const unavailable = []
  ;(content.gifts || []).forEach(saved => {
    const gift = gifts.value.find(option => Number(option.giftId) === Number(saved.giftId))
    if (!gift || gift.currentCost == null) {
      unavailable.push(saved.giftName || '未命名礼品')
      return
    }
    const existing = selected.find(row => row.giftId === gift.giftId)
    const quantity = Math.min(10, Math.max(1, Number(saved.quantity) || 1))
    if (existing) existing.quantity = Math.min(10, existing.quantity + quantity)
    else selected.push({ ...gift, quantity })
  })
  const fieldValue = key => (content.fields || []).find(field => field.key === key)?.value
  entryForm.value.processStatus = 'selected'
  entryForm.value.anchorNameSnapshot = fieldValue('anchor') || ''
  entryForm.value.controllerNameSnapshot = fieldValue('controller') || ''
  entryForm.value.refundAmount = fieldValue('refundAmount') == null || fieldValue('refundAmount') === '' ? null : Number(fieldValue('refundAmount'))
  entryForm.value.otherRemark = fieldValue('otherRemark') || ''
  entryForm.value.refundReason = fieldValue('refundReason') || ''
  entryForm.value.afterSaleCompensation = fieldValue('afterSaleCompensation') || ''
  entryForm.value.serviceMark = fieldValue('serviceMark') || ''
  entryForm.value.extendedWarranty = booleanValue(fieldValue('extendedWarranty'))
  entryForm.value.priceProtection = booleanValue(fieldValue('priceProtection'))
  entryForm.value.delayed = booleanValue(fieldValue('delayed'))
  entryForm.value.followUp = booleanValue(fieldValue('followUp'))
  entryForm.value.urgent = booleanValue(fieldValue('urgent'))
  entryForm.value.anchorUserId = anchorOptions.value.find(option => option.userName === entryForm.value.anchorNameSnapshot)?.userId || null
  entryForm.value.controllerUserId = controllerOptions.value.find(option => option.userName === entryForm.value.controllerNameSnapshot)?.userId || null
  entryForm.value.gifts = selected
  activeTemplateId.value = item.templateId
  activeTemplateFields.value = Array.isArray(content.fields) ? content.fields : []
  entryForm.value.templateId = item.templateId
  entryForm.value.templateNameSnapshot = item.templateName
  entryForm.value.parsedText = previewText.value
  if (unavailable.length) proxy.$modal.msgWarning(`${unavailable.join('、')} 已停用或没有成本，未填入`)
}

function hasEntryContent() {
  const form = entryForm.value
  return form.gifts.length > 0 || !!(form.anchorNameSnapshot || form.controllerNameSnapshot || form.refundAmount != null || form.refundReason || form.otherRemark || form.afterSaleCompensation || form.serviceMark || form.extendedWarranty || form.priceProtection || form.delayed || form.followUp || form.urgent)
}

function useManualMode() {
  activeTemplateId.value = null
  activeTemplateFields.value = []
  entryForm.value.templateId = null
  entryForm.value.templateNameSnapshot = ''
}

function syncTemplateNote() {
  if (activeTemplateId.value != null) entryForm.value.parsedText = previewText.value
}

function detachTemplate() {
  activeTemplateId.value = null
  activeTemplateFields.value = []
}

function chooseGift(item) {
  const selected = entryForm.value.gifts.find(gift => gift.giftId === item.giftId)
  if (selected) selected.quantity = Math.min(10, Number(selected.quantity || 0) + 1)
  else entryForm.value.gifts.push({ ...item, quantity: 1 })
  syncTemplateNote()
  nextTick(() => selectedListRef.value?.querySelector('.selected-row:last-of-type')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' }))
}

function handleQuantityChange(item) {
  item.quantity = Math.min(10, Math.max(1, Number(item.quantity) || 1))
  syncTemplateNote()
}

function removeGift(giftId) {
  entryForm.value.gifts = entryForm.value.gifts.filter(item => item.giftId !== giftId)
  syncTemplateNote()
}

function handleStatusChange(value) {
  if (value !== 'selected') {
    entryForm.value.gifts = []
    activeTemplateId.value = null
    activeTemplateFields.value = []
  }
}

async function saveEntry(next) {
  if (!hasEntryContent()) return proxy.$modal.msgWarning('请至少填写一项订单信息或选择礼品')
  if (previewText.value.length > 2000) return proxy.$modal.msgWarning('预览内容不能超过2000字')
  const editing = isEditing.value
  saving.value = true
  try {
    const payload = {
      processStatus: entryForm.value.gifts.length ? 'selected' : 'not_applicable',
      operatorNote: entryForm.value.otherRemark,
      roomId: entryForm.value.roomId,
      roomCodeSnapshot: entryForm.value.roomCodeSnapshot,
      roomNameSnapshot: entryForm.value.roomNameSnapshot,
      anchorUserId: entryForm.value.anchorUserId,
      anchorNameSnapshot: entryForm.value.anchorNameSnapshot,
      controllerUserId: entryForm.value.controllerUserId,
      controllerNameSnapshot: entryForm.value.controllerNameSnapshot,
      refundAmount: entryForm.value.refundAmount,
      refundReason: entryForm.value.refundReason,
      otherRemark: entryForm.value.otherRemark,
      afterSaleCompensation: entryForm.value.afterSaleCompensation,
      serviceMark: entryForm.value.serviceMark,
      extendedWarranty: entryForm.value.extendedWarranty,
      priceProtection: entryForm.value.priceProtection,
      delayed: entryForm.value.delayed,
      followUp: entryForm.value.followUp,
      urgent: entryForm.value.urgent,
      templateId: entryForm.value.templateId,
      templateNameSnapshot: entryForm.value.templateNameSnapshot,
      parsedText: previewText.value,
      gifts: entryForm.value.gifts.map(item => ({ giftId: item.giftId, quantity: item.quantity }))
    }
    if (activeEntryMode.value === 'batch') {
      const response = await batchSaveOrderGifts({ ...payload, orderNos: batchOrderNos.value, overwriteExisting: overwriteExisting.value })
      const result = response.data || {}
      proxy.$modal.msgSuccess(`批量录入完成：成功 ${result.success || 0} 单${result.skipped ? `，跳过已有 ${result.skipped} 单` : ''}`)
      batchOrderText.value = ''
    } else {
      await saveOrderGifts({ ...payload, orderNo: order.value.orderNo })
      proxy.$modal.msgSuccess(editing ? '订单记录已修改' : '订单记录已新增')
    }
    quickOrderNo.value = ''
    await load()
    if (next && activeEntryMode.value === 'single') {
      resetEntry()
      focusEntryInput()
    } else {
      entryVisible.value = false
    }
  } finally {
    saving.value = false
  }
}

function editRecord(row) {
  openEntry(row.orderNo)
}

function handleExport() {
  proxy.download('live/gift/ledger/export', queryPayload(), `订单礼品记录_${new Date().getTime()}.xlsx`)
}

onMounted(async () => {
  query.value.orderNo = String(route.query.orderNo || '')
  query.value.giftName = String(route.query.giftName || '')
  if (route.query.beginDate && route.query.endDate) dates.value = [String(route.query.beginDate), String(route.query.endDate)]
  const [giftRes, templateRes, staffRes, mappingRes, roomRes, preferenceRes] = await Promise.all([listGifts({ status: '0', includeHidden: '1' }), listTemplates(), listStaff({ status: '0' }), listMappings({}), listRooms({ status: '0' }), getRoomPreference()])
  gifts.value = giftRes.data || []
  templates.value = templateRes.data || []
  staffOptions.value = staffRes.data || []
  mappings.value = mappingRes.data || []
  rooms.value = roomRes.data || []
  currentRoomId.value = preferenceRes.data?.roomId || null
  await load()
  nextTick(() => quickOrderInput.value?.focus())
})
</script>

<style scoped>
.room-context{display:grid;grid-template-columns:minmax(210px,1fr) minmax(280px,1.4fr) minmax(120px,.6fr) minmax(120px,.6fr);align-items:center;gap:16px;margin-bottom:12px;padding:13px 16px;border:1px solid #dfe3e8;background:#fff}.room-context-copy,.room-people{display:flex;min-width:0;flex-direction:column;gap:3px}.room-context-copy span,.room-people span{color:#6b7280;font-size:11px}.room-context-copy strong,.room-people strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.room-context-copy small{color:#909399}.room-context>.el-select{width:100%}
.gift-ledger{max-width:1600px}.page-head,.head-actions,.filter-bar,.section-title,.drawer-actions,.entry-identity>div{display:flex;align-items:center}.page-head{justify-content:space-between;gap:16px;margin-bottom:14px}.page-head h2{margin:0 0 4px;font-size:22px}.page-head p{margin:0;color:#6b7280}.head-actions{gap:8px}.entry-workbench{margin-bottom:14px;padding:14px 16px;border:1px solid #e2e8f0;background:#fff}.entry-mode-row{display:flex;align-items:center;justify-content:space-between;gap:16px;margin-bottom:12px}.entry-mode-row>div{display:flex;align-items:baseline;gap:12px}.entry-mode-row>div span{color:#6b7280;font-size:12px}.single-entry-row{display:grid;grid-template-columns:minmax(0,640px) 90px;gap:8px}.batch-entry-row{display:grid;grid-template-columns:minmax(0,1fr) 150px;gap:12px}.batch-entry-side{display:flex;align-items:stretch;flex-direction:column;justify-content:center;gap:5px}.batch-entry-side span,.batch-entry-side small{color:#6b7280}.batch-entry-side strong{color:#c2410c;font-size:18px}.entry-workbench>.el-alert{margin-top:10px}.filter-bar{flex-wrap:wrap;gap:8px;padding:10px 0;border-bottom:1px solid #ebeef5}.order-filter{width:260px}.filter-bar .el-select{width:150px}.order-no{font-family:Consolas,monospace}.muted{color:#909399}.drawer-body{padding:0 4px 18px}.order-lookup{display:grid;grid-template-columns:minmax(0,1fr) 90px;gap:8px}.entry-identity{display:flex;justify-content:space-between;gap:12px;margin:14px 0;padding:12px 14px;border:1px solid #dfe3e8;background:#f8fafc}.entry-identity>div{gap:10px;min-width:0}.entry-identity span{color:#6b7280;font-size:13px}.entry-identity strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.batch-policy{display:flex;align-items:center;gap:14px;padding:10px 14px;border:1px solid #fed7aa;background:#fff7ed}.batch-policy>div{display:flex;min-width:0;flex:1;flex-direction:column}.batch-policy>div span{overflow:hidden;color:#7c2d12;text-overflow:ellipsis;white-space:nowrap}.batch-policy small{color:#9a3412}.entry-section{padding:15px 0;border-bottom:1px solid #ebeef5}.section-title{justify-content:space-between;gap:12px;margin-bottom:11px}.section-title>div{display:flex;flex-direction:column;gap:2px}.section-title span,.selected-name span{color:#6b7280;font-size:12px}.template-section{padding-bottom:12px}.template-chips{display:flex;gap:8px;overflow-x:auto;padding-bottom:3px}.template-chips button{display:inline-flex;align-items:center;flex:none;gap:5px;padding:9px 13px;border:1px solid #d8dde3;border-radius:5px;background:#fff;color:#4b5563;cursor:pointer}.template-chips button:hover,.template-chips button.active{border-color:#f26b21;background:#fff7f2;color:#d85209}.template-empty{align-self:center;color:#909399;font-size:13px}.service-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:2px 14px}.service-grid :deep(.el-form-item){margin-bottom:12px}.service-grid :deep(.el-select),.service-grid :deep(.el-input),.service-grid :deep(.el-input-number){width:100%}.service-grid .span-2{grid-column:span 2}.service-grid .span-4{grid-column:1/-1}.boolean-grid{display:flex;flex-wrap:wrap;gap:10px 18px;padding:4px 0 2px}.boolean-grid :deep(.el-checkbox){margin-right:0}.service-tags{display:flex;flex-wrap:wrap;gap:4px}.gift-workspace{display:grid;grid-template-columns:minmax(0,1.08fr) minmax(350px,.92fr);gap:14px;padding:15px 0}.gift-catalog,.selected-gifts{min-height:370px;padding:14px;border:1px solid #dfe3e8;background:#fff}.gift-catalog .section-title .el-input{width:220px}.gift-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px;max-height:410px;overflow:auto}.gift-grid>button{display:flex;min-width:0;min-height:88px;flex-direction:column;gap:5px;padding:10px;border:1px solid #dfe3e8;border-radius:5px;background:#fff;text-align:left;cursor:pointer}.gift-grid>button:hover:not(:disabled){border-color:#f26b21;background:#fff7ed}.gift-grid>button:disabled{cursor:not-allowed;opacity:.55}.gift-grid>button strong,.gift-grid>button span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.gift-grid>button span{color:#6b7280;font:12px Consolas,monospace}.gift-grid>button b{color:#c2410c}.gift-grid>button em{color:#dc2626;font-size:12px;font-style:normal}.gift-grid>.el-empty{grid-column:1/-1}.selected-gifts{max-height:500px;overflow:auto}.selected-row{display:grid;grid-template-columns:minmax(0,1fr) 105px 76px 30px;align-items:center;gap:7px;padding:10px 0;border-bottom:1px solid #eee;scroll-margin:70px}.selected-name{display:flex;min-width:0;flex-direction:column}.selected-name strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.selected-row :deep(.el-input-number){width:105px}.line-cost{text-align:right}.gift-total{display:flex;justify-content:space-between;align-items:center;padding-top:16px}.gift-total strong{color:#c2410c;font-size:22px}.note-section{border-bottom:0}.note-count{color:#909399!important}.drawer-actions{justify-content:space-between;width:100%}.drawer-actions>div{display:flex;gap:8px}
@media(max-width:900px){.page-head{align-items:flex-start;flex-direction:column}.head-actions{align-self:stretch;justify-content:flex-end}.quick-title{display:none}.current-user{display:none}.service-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.service-grid .span-4{grid-column:1/-1}.gift-workspace{grid-template-columns:1fr}.gift-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.selected-gifts{max-height:none}.entry-identity{align-items:flex-start;flex-direction:column}.status-buttons{display:flex;flex-wrap:wrap}}
@media(max-width:560px){.entry-mode-row{align-items:stretch;flex-direction:column}.single-entry-row,.batch-entry-row{grid-template-columns:1fr}.batch-entry-side{align-items:stretch}.batch-policy{align-items:flex-start;flex-direction:column}.service-grid{grid-template-columns:1fr}.service-grid .span-2,.service-grid .span-4{grid-column:auto}.gift-catalog .section-title{align-items:stretch;flex-direction:column}.gift-catalog .section-title .el-input{width:100%}.gift-grid{grid-template-columns:1fr}.selected-row{grid-template-columns:minmax(0,1fr) 100px 30px}.line-cost{display:none}}
</style>
