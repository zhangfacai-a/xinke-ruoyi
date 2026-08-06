<template>
  <div class="app-container viewer-page">
    <section class="viewer-hero">
      <div>
        <p class="eyebrow">直播运营 / 客户追单</p>
        <h2>直播观众追单池</h2>
        <p class="hero-desc">按直播间和观众去重建档；有评论的客户自动标为高意向，运营优先跟进。</p>
      </div>
      <div class="hero-status">
        <span>最近同步</span>
        <strong>{{ formatDateTime(summary.lastUpdateTime || summary.last_update_time) }}</strong>
      </div>
    </section>

    <el-row :gutter="16" class="metric-row">
      <el-col v-for="item in metrics" :key="item.label" :xs="12" :sm="12" :md="6" :lg="4">
        <div class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </el-col>
    </el-row>

    <section class="search-panel">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" class="search-form">
        <el-form-item label="日期" prop="dateRange">
          <el-date-picker v-model="queryParams.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 260px" />
        </el-form-item>
        <el-form-item label="直播间" prop="roomName">
          <el-autocomplete v-model="queryParams.roomName" :fetch-suggestions="queryRoomSearch" clearable
            placeholder="搜索直播间" style="width: 180px" @select="handleQuery" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="客户" prop="nickname">
          <el-input v-model="queryParams.nickname" placeholder="搜索昵称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="负责人" prop="ownerName">
          <el-autocomplete v-model="queryParams.ownerName" :fetch-suggestions="queryOwnerSearch" clearable
            placeholder="搜索负责人" style="width: 150px" @select="handleQuery" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="评论" prop="hasComment">
          <el-select v-model="queryParams.hasComment" clearable placeholder="全部" style="width: 120px">
            <el-option label="有评论" value="1" />
            <el-option label="无评论" value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态" style="width: 130px">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="追单池" prop="excludeOrdered">
          <el-switch v-model="queryParams.excludeOrdered" active-value="1" inactive-value="0" active-text="隐藏已下单"
            @change="handleQuery" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button type="warning" plain icon="Download" @click="handleExport">导出明细</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Refresh" @click="getList">刷新线索</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table v-loading="loading" :data="leadList" row-key="lead_id" height="560" border stripe>
        <el-table-column label="客户" min-width="200" fixed show-overflow-tooltip>
          <template #default="scope">
            <div class="customer-cell">
              <strong>{{ scope.row.nickname || '未知观众' }}</strong>
              <el-button link type="primary" icon="CopyDocument" @click="copyProfile(scope.row)">主页</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="日期" prop="lead_date" width="112" align="center" show-overflow-tooltip />

        <el-table-column label="直播间" prop="live_room_name" min-width="170" show-overflow-tooltip>
          <template #default="scope">{{ roomName(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="评论" min-width="220" show-overflow-tooltip>
          <template #default="scope">
            <div class="comment-cell">
              <el-tag v-if="Number(scope.row.has_comment) === 1" type="success" effect="light">
                {{ Number(scope.row.comment_count || 0) }}条
              </el-tag>
              <el-tag v-else type="info" effect="plain">无</el-tag>
              <span>{{ scope.row.last_comment_content || '暂无评论' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="意向" width="100" align="center">
          <template #default="scope">
            <el-tag :type="intentTag(scope.row.intent)" effect="light">{{ intentLabel(scope.row.intent) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="scope">
            <el-tag :type="statusTag(scope.row.status)" effect="light">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责人" prop="owner_name" width="110" show-overflow-tooltip />


        <el-table-column label="订单号" prop="order_no" min-width="140" show-overflow-tooltip />
        <el-table-column label="来访" width="96" align="center" show-overflow-tooltip>
          <template #default="scope">
            {{ scope.row.visit_count || 1 }} 天
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="update_time" width="168" align="center" show-overflow-tooltip>
          <template #default="scope">{{ formatDateTime(scope.row.update_time) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="openDetail(scope.row)"
              v-hasPermi="['live:viewer:query']">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="openFollow(scope.row)"
              v-hasPermi="['live:viewer:follow']">跟进</el-button>
            <el-button v-if="!isSoldStatus(scope.row.status)" link type="success" icon="CircleCheck"
              @click="openMarkPreOrdered(scope.row)" v-hasPermi="['live:viewer:follow']">追前已购</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-drawer v-model="detailOpen" size="680px" title="客户追单详情" append-to-body>
      <div v-if="detail.lead" class="detail-wrap">
        <div class="detail-head">
          <div class="name-badge">{{ shortName(detail.lead.nickname) }}</div>
          <div>
            <h3>{{ detail.lead.nickname || '未知观众' }}</h3>
            <p>{{ detailRoomName }} · {{ detail.lead.lead_date }}</p>
          </div>
        </div>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">{{ statusLabel(detail.lead.status) }}</el-descriptions-item>
          <el-descriptions-item label="意向">{{ intentLabel(detail.lead.intent) }}</el-descriptions-item>
          <el-descriptions-item label="负责人">{{ detail.lead.owner_name || '-' }}</el-descriptions-item>
          <el-descriptions-item label="订单号">{{ detail.lead.order_no || '-' }}</el-descriptions-item>
          <el-descriptions-item label="主页" :span="2">
            <el-button link type="primary" icon="CopyDocument" @click="copyProfile(detail.lead)">复制抖音主页链接</el-button>
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.lead.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h4>评论记录</h4>
        <el-empty v-if="!detail.comments || detail.comments.length === 0" description="暂无评论" />
        <el-timeline v-else>
          <el-timeline-item v-for="item in detail.comments" :key="item.comment_id"
            :timestamp="formatDateTime(item.captured_at)">
            {{ item.content }}
          </el-timeline-item>
        </el-timeline>

        <h4>来访记录</h4>
        <el-empty v-if="!detail.visits || detail.visits.length === 0" description="暂无来访" />
        <el-timeline v-else>
          <el-timeline-item v-for="item in detail.visits" :key="item.lead_id" :timestamp="item.lead_date">
            <strong>{{ item.live_room_name || '-' }}</strong>
            <span class="visit-meta">
              评论 {{ item.comment_count || 0 }} 条
              <template v-if="item.last_comment_content">，最近：{{ item.last_comment_content }}</template>
            </span>
          </el-timeline-item>
        </el-timeline>

        <h4>跟进记录</h4>
        <el-empty v-if="!detail.followRecords || detail.followRecords.length === 0" description="暂无跟进" />
        <el-timeline v-else>
          <el-timeline-item v-for="item in detail.followRecords" :key="item.record_id"
            :timestamp="formatDateTime(item.create_time)">
            <strong>{{ item.display_owner_name || item.owner_name || '未记录负责人' }}</strong>：{{ item.follow_content }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <el-dialog v-model="followOpen" title="记录跟进" width="560px" append-to-body>
      <el-alert v-if="ownerTip" :title="ownerTip" type="warning" show-icon :closable="false" class="mb12" />
      <el-form :model="followForm" label-width="96px">
        <el-form-item label="负责人">
          <el-input v-model="followForm.ownerName" :disabled="ownerInputDisabled" placeholder="例如：张三" />
        </el-form-item>
        <el-form-item label="订单号">
          <el-input v-model="followForm.orderNo" placeholder="填写后自动标记为已下单" />
        </el-form-item>
        <el-form-item label="跟进内容">
          <el-input v-model="followForm.followContent" type="textarea" :rows="4" placeholder="记录客户需求、型号、预算、是否已下单等" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="followForm.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="followOpen = false">取消</el-button>
        <el-button type="primary" @click="submitFollow">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="orderedOpen" title="标记为追单前已下单" width="520px" append-to-body>
      <el-alert title="用于处理进追单池前已经成交的客户。标记后默认从追单池隐藏，但可通过状态筛选找回。" type="success" show-icon :closable="false" class="mb12" />
      <el-form :model="orderedForm" label-width="96px">
        <el-form-item label="客户">
          <span>{{ orderedLead?.nickname || '-' }}</span>
        </el-form-item>
        <el-form-item label="订单号" required>
          <el-input v-model="orderedForm.orderNo" placeholder="请输入已成交订单号" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderedForm.remark" type="textarea" :rows="3" placeholder="例如：直播前已下单、客服已确认等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="orderedOpen = false">取消</el-button>
        <el-button type="success" @click="submitMarkPreOrdered">确认追前已购</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LiveViewerLead">
import {
  addViewerFollow,
  getViewerLead,
  getViewerSummary,
  listViewerOwnerSuggestions,
  listViewerLeads,
  listViewerRoomSuggestions,
  updateViewerLead
} from '@/api/live/viewer'

const { proxy } = getCurrentInstance()
const DOUYIN_USER_PREFIX = 'https://www.douyin.com/user/'

const today = formatLocalDate(new Date())
const weekStart = formatLocalDate(addDays(new Date(), -6))
const defaultDateRange = () => [weekStart, today]

const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const leadList = ref([])
const summary = ref({})
const detailOpen = ref(false)
const followOpen = ref(false)
const orderedOpen = ref(false)
const currentLead = ref(null)
const orderedLead = ref(null)
const detail = ref({ lead: null, comments: [], visits: [], followRecords: [] })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  dateRange: defaultDateRange(),
  roomName: undefined,
  nickname: undefined,
  ownerName: undefined,
  hasComment: undefined,
  status: undefined,
  excludeOrdered: '1'
})

const followForm = reactive({
  ownerName: undefined,
  orderNo: undefined,
  followContent: undefined,
  remark: undefined
})

const orderedForm = reactive({
  orderNo: undefined,
  remark: undefined
})

const statusOptions = [
  { label: '新线索', value: 'new' },
  { label: '跟进中', value: 'following' },
  { label: '追单前已下单', value: 'pre_ordered' },
  { label: '追单后已下单', value: 'ordered' },
  { label: '无效', value: 'invalid' }
]

const intentOptions = [
  { label: '未知', value: 'unknown' },
  { label: '低意向', value: 'low' },
  { label: '中意向', value: 'medium' },
  { label: '高意向', value: 'high' }
]

const metrics = computed(() => [
  { label: '区间观众', value: valueOf(summary.value, 'totalCount'), desc: '去重后人数' },
  { label: '有评论', value: valueOf(summary.value, 'commentLeadCount'), desc: '自动高意向' },
  { label: '新线索', value: valueOf(summary.value, 'newCount'), desc: '未分配/未处理' },
  { label: '跟进中', value: valueOf(summary.value, 'followingCount'), desc: '需要运营推进' },
  { label: '追前已购', value: valueOf(summary.value, 'preOrderedCount'), desc: '无需继续追问' },
  { label: '追后成交', value: valueOf(summary.value, 'orderedCount'), desc: '运营转化复盘' }
])

const detailRoomName = computed(() => {
  const lead = detail.value.lead || {}
  return lead.display_room_name || lead.live_room_name || '-'
})

const ownerInputDisabled = computed(() => {
  if (!currentLead.value) return false
  return hasOrderNo(currentLead.value) || hasOwner(currentLead.value)
})

const ownerTip = computed(() => {
  if (!currentLead.value) return ''
  if (hasOrderNo(currentLead.value)) {
    return '该线索上次已保存为已下单，负责人不能再修改。'
  }
  if (hasOwner(currentLead.value)) {
    return '该线索已有负责人，负责人不可修改；超过 24 小时且未下单会自动释放。'
  }
  return ''
})

function valueOf(row, key) {
  return row?.[key] ?? row?.[toSnake(key)] ?? 0
}

function addDays(date, days) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function formatLocalDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDateTime(value) {
  if (value === undefined || value === null || value === '') return '-'
  return String(value).replace('T', ' ').replace(/\.\d+$/, '').slice(0, 19)
}

function toSnake(key) {
  return key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`)
}

function buildQuery() {
  const [beginLeadDate, endLeadDate] = queryParams.dateRange || []
  const excludeOrdered = queryParams.status ? undefined : queryParams.excludeOrdered
  return cleanQuery({
    pageNum: queryParams.pageNum,
    pageSize: queryParams.pageSize,
    beginLeadDate,
    endLeadDate,
    roomName: queryParams.roomName,
    nickname: queryParams.nickname,
    ownerName: queryParams.ownerName,
    hasComment: queryParams.hasComment,
    status: queryParams.status,
    excludeOrdered
  })
}

function cleanQuery(query, keepPage = true) {
  const data = {}
  Object.keys(query).forEach((key) => {
    if (!keepPage && (key === 'pageNum' || key === 'pageSize')) return
    const value = query[key]
    if (value === undefined || value === null || value === '') return
    if (typeof value === 'string' && value.trim() === '') return
    data[key] = typeof value === 'string' ? value.trim() : value
  })
  return data
}

function statusLabel(value) {
  return statusOptions.find((item) => item.value === value)?.label || '新线索'
}

function statusTag(value) {
  return ({ new: 'info', following: 'warning', pre_ordered: 'success', ordered: 'success', invalid: 'danger' })[value] || 'info'
}

function intentLabel(value) {
  return intentOptions.find((item) => item.value === value)?.label || '未知'
}

function intentTag(value) {
  return ({ low: 'info', medium: 'warning', high: 'danger', unknown: '' })[value] || ''
}

function shortName(name) {
  return name ? String(name).slice(0, 1) : '客'
}

function roomName(row) {
  return row.live_room_name || row.display_room_name || '-'
}

function profileUrl(row) {
  return row?.sec_uid ? `${DOUYIN_USER_PREFIX}${row.sec_uid}` : ''
}

async function copyText(text) {
  if (!text) {
    proxy.$modal.msgWarning('没有可复制的主页链接')
    return
  }
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text)
    } else {
      const input = document.createElement('textarea')
      input.value = text
      input.style.position = 'fixed'
      input.style.opacity = '0'
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    proxy.$modal.msgSuccess('已复制抖音主页链接')
  } catch (error) {
    proxy.$modal.msgError('复制失败，请手动复制')
  }
}

function copyProfile(row) {
  copyText(profileUrl(row))
}

function toAutocompleteRows(list) {
  return (list || []).filter(Boolean).map((item) => ({ value: item }))
}

function queryRoomSearch(keyword, cb) {
  listViewerRoomSuggestions({ keyword }).then((res) => {
    cb(toAutocompleteRows(res.data))
  }).catch(() => cb([]))
}

function queryOwnerSearch(keyword, cb) {
  listViewerOwnerSuggestions({ keyword }).then((res) => {
    cb(toAutocompleteRows(res.data))
  }).catch(() => cb([]))
}

function hasOwner(row) {
  return Boolean(String(row?.owner_name || '').trim())
}

function hasOrderNo(row) {
  return Boolean(String(row?.order_no || '').trim())
}

function isSoldStatus(status) {
  return status === 'ordered' || status === 'pre_ordered'
}

function getList() {
  loading.value = true
  listViewerLeads(buildQuery())
    .then((res) => {
      leadList.value = res.rows || []
      total.value = res.total || 0
    })
    .catch(() => {
      leadList.value = []
      total.value = 0
    })
    .finally(() => {
      loading.value = false
    })
  refreshSummary()
}

function refreshSummary() {
  const query = buildQuery()
  getViewerSummary({
    beginLeadDate: query.beginLeadDate,
    endLeadDate: query.endLeadDate,
    roomName: query.roomName,
    nickname: query.nickname,
    ownerName: query.ownerName,
    hasComment: query.hasComment,
    status: query.status,
    excludeOrdered: query.excludeOrdered
  }).then((res) => {
    summary.value = res.data || {}
  }).catch(() => {
    summary.value = {}
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.dateRange = undefined
  queryParams.roomName = undefined
  queryParams.ownerName = undefined
  queryParams.excludeOrdered = '1'
  getList()
}

function handleExport() {
  const query = cleanQuery(buildQuery(), false)
  proxy.download('live/viewer/lead/export', query, `直播追单明细_${new Date().getTime()}.xlsx`)
}

function openDetail(row) {
  getViewerLead(row.lead_id).then((res) => {
    detail.value = res.data || { lead: row, comments: [], visits: [], followRecords: [] }
    detailOpen.value = true
  })
}

function openFollow(row) {
  currentLead.value = row
  followForm.ownerName = row.owner_name
  followForm.orderNo = row.order_no
  followForm.followContent = undefined
  followForm.remark = row.remark
  followOpen.value = true
}

function openMarkPreOrdered(row) {
  orderedLead.value = row
  orderedForm.orderNo = row.order_no || undefined
  orderedForm.remark = row.remark
  orderedOpen.value = true
}

function submitMarkPreOrdered() {
  if (!orderedLead.value) return
  if (!String(orderedForm.orderNo || '').trim()) {
    proxy.$modal.msgWarning('请先填写订单号，避免误标记')
    return
  }
  updateViewerLead(orderedLead.value.lead_id, {
    status: 'pre_ordered',
    orderNo: orderedForm.orderNo,
    remark: orderedForm.remark
  }).then(() => {
    proxy.$modal.msgSuccess('已标记为追单前已下单')
    orderedOpen.value = false
    getList()
  })
}

function submitFollow() {
  if (!currentLead.value) return
  if (ownerInputDisabled.value && followForm.ownerName !== currentLead.value.owner_name) {
    proxy.$modal.msgWarning(ownerTip.value)
    return
  }

  const leadId = currentLead.value.lead_id
  const payload = {
    ownerName: ownerInputDisabled.value ? currentLead.value.owner_name : followForm.ownerName,
    orderNo: followForm.orderNo,
    remark: followForm.remark
  }

  updateViewerLead(leadId, payload).then(() => {
    if (!String(followForm.followContent || '').trim()) {
      return null
    }
    return addViewerFollow(leadId, {
      followContent: followForm.followContent
    })
  }).then(() => {
    proxy.$modal.msgSuccess('已保存')
    followOpen.value = false
    getList()
  })
}

getList()
</script>

<style scoped lang="scss">
.viewer-page {
  padding: 22px 24px;
  background: #fff;
}

.viewer-hero {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 0 18px;
  border-bottom: 1px solid #e5e7eb;

  h2 {
    margin: 5px 0 7px;
    color: #1f2933;
    font-size: 22px;
    font-weight: 650;
  }
}

.eyebrow {
  margin: 0;
  color: #f26b21;
  font-size: 12px;
  font-weight: 600;
}

.hero-desc {
  margin: 0;
  color: #6b7280;
  font-size: 13px;
}

.hero-status {
  min-width: 170px;
  padding-left: 16px;
  border-left: 2px solid #f7a06f;

  span,
  strong {
    display: block;
  }

  span {
    color: #8a949f;
    margin-bottom: 4px;
    font-size: 12px;
  }

  strong {
    color: #374151;
    font-size: 13px;
    font-weight: 600;
  }
}

.metric-row {
  margin: 0 !important;
  padding: 14px 0;
  border-bottom: 1px solid #e5e7eb;
  background: #fafafa;

  :deep(.el-col) { padding: 0 !important; }
}

.metric-card {
  min-height: 76px;
  padding: 8px 18px;
  border-right: 1px solid #e5e7eb;

  span,
  small {
    display: block;
    color: #77818c;
    font-size: 12px;
  }

  strong {
    display: block;
    margin: 5px 0 3px;
    color: #1f2933;
    font-size: 22px;
    font-weight: 650;
  }
}

.search-panel {
  padding: 16px 0 4px;
  border-bottom: 1px solid #e5e7eb;
}

.table-panel {
  padding: 16px 0 0;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
}

.customer-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  strong {
    overflow: hidden;
    color: #1f2933;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.comment-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;

  span {
    overflow: hidden;
    color: #4b5563;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.detail-wrap {
  h4 {
    margin: 24px 0 12px;
    color: #1f2933;
  }
}

.detail-head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;

  h3 {
    margin: 0 0 4px;
  }

  p {
    margin: 0;
    color: #77818c;
  }
}

.visit-meta {
  display: block;
  max-width: 540px;
  margin-top: 4px;
  overflow: hidden;
  color: #77818c;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.name-badge {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 4px;
  color: #fff;
  background: #f26b21;
  font-size: 20px;
  font-weight: 650;
  box-shadow: none;
}

.mb12 {
  margin-bottom: 12px;
}

@media (max-width: 768px) {
  .viewer-page { padding: 14px 12px; }
  .viewer-hero { align-items: flex-start; gap: 14px; }
  .hero-status { display: none; }
  .metric-card { border-bottom: 1px solid #e5e7eb; }
}
</style>
