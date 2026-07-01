<template>
  <div class="app-container viewer-page">
    <section class="viewer-hero">
      <div>
        <p class="eyebrow">Douyin Live CRM</p>
        <h2>直播观众追单池</h2>
        <p class="hero-desc">按每天、直播间、观众去重建档；评论会自动挂到同一个客户，运营只处理需要跟进和可转化的人。</p>
      </div>
      <div class="hero-status">
        <span>最近同步</span>
        <strong>{{ summary.lastUpdateTime || summary.last_update_time || '-' }}</strong>
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
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
        <el-form-item label="日期" prop="leadDate">
          <el-date-picker v-model="queryParams.leadDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="直播间" prop="roomName">
          <el-input v-model="queryParams.roomName" placeholder="请输入直播间名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="客户" prop="nickname">
          <el-input v-model="queryParams.nickname" placeholder="搜索昵称" clearable @keyup.enter="handleQuery" />
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
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

      <el-table v-loading="loading" :data="leadList" row-key="lead_id" height="560" border>
        <el-table-column label="客户" min-width="170" fixed show-overflow-tooltip>
          <template #default="scope">
            <div class="customer-cell">
              <strong>{{ scope.row.nickname || '未知观众' }}</strong>
              <el-button link type="primary" icon="CopyDocument" @click="copyProfile(scope.row)">复制主页</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="日期" prop="lead_date" width="110" align="center" show-overflow-tooltip />
        <el-table-column label="直播间" prop="live_room_name" min-width="150" show-overflow-tooltip>
          <template #default="scope">
            {{ roomName(scope.row) }}
          </template>
        </el-table-column>
        <el-table-column label="评论" min-width="260" show-overflow-tooltip>
          <template #default="scope">
            <div class="comment-cell">
              <el-tag v-if="Number(scope.row.has_comment) === 1" type="success" effect="light">
                {{ scope.row.comment_count || 1 }}条
              </el-tag>
              <el-tag v-else type="info" effect="plain">无</el-tag>
              <span>{{ scope.row.last_comment_content || '暂无评论' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="意向" width="110" align="center">
          <template #default="scope">
            <el-tag :type="intentTag(scope.row.intent)" effect="light">{{ intentLabel(scope.row.intent) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="statusTag(scope.row.status)" effect="light">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="负责人" prop="owner_name" width="120" show-overflow-tooltip />
        <el-table-column label="订单号" prop="order_no" min-width="150" show-overflow-tooltip />
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="更新时间" prop="update_time" width="170" align="center" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="openDetail(scope.row)" v-hasPermi="['live:viewer:query']">详情</el-button>
            <el-button link type="primary" icon="Edit" @click="openFollow(scope.row)" v-hasPermi="['live:viewer:follow']">跟进</el-button>
            <el-button link type="primary" icon="CopyDocument" @click="copyProfile(scope.row)">复制</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
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
          <el-timeline-item v-for="item in detail.comments" :key="item.comment_id" :timestamp="item.captured_at">
            {{ item.content }}
          </el-timeline-item>
        </el-timeline>

        <h4>跟进记录</h4>
        <el-empty v-if="!detail.followRecords || detail.followRecords.length === 0" description="暂无跟进" />
        <el-timeline v-else>
          <el-timeline-item v-for="item in detail.followRecords" :key="item.record_id" :timestamp="item.create_time">
            <strong>{{ item.operator_name || '系统' }}</strong>：{{ item.follow_content }}
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-drawer>

    <el-dialog v-model="followOpen" title="记录跟进" width="560px" append-to-body>
      <el-alert v-if="ownerTip" :title="ownerTip" type="warning" show-icon :closable="false" class="mb12" />
      <el-form :model="followForm" label-width="96px">
        <el-form-item label="状态">
          <el-select v-model="followForm.status" style="width: 100%">
            <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="意向">
          <el-select v-model="followForm.intent" style="width: 100%">
            <el-option v-for="item in intentOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="followForm.ownerName" :disabled="ownerInputDisabled" placeholder="例如：张三" />
        </el-form-item>
        <el-form-item label="订单号" :required="followForm.status === 'ordered'">
          <el-input v-model="followForm.orderNo" placeholder="已下单必须填写订单号" />
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
  </div>
</template>

<script setup name="LiveViewerLead">
import {
  addViewerFollow,
  getViewerLead,
  getViewerSummary,
  listViewerLeads,
  updateViewerLead
} from '@/api/live/viewer'

const { proxy } = getCurrentInstance()
const DOUYIN_USER_PREFIX = 'https://www.douyin.com/user/'

const today = formatLocalDate(new Date())
const showSearch = ref(true)
const loading = ref(false)
const total = ref(0)
const leadList = ref([])
const summary = ref({})
const detailOpen = ref(false)
const followOpen = ref(false)
const currentLead = ref(null)
const detail = ref({ lead: null, comments: [], followRecords: [] })

const queryParams = reactive({
  pageNum: 1,
  pageSize: 20,
  leadDate: today,
  roomName: undefined,
  nickname: undefined,
  hasComment: undefined,
  status: undefined
})

const followForm = reactive({
  status: 'following',
  intent: 'unknown',
  ownerName: undefined,
  orderNo: undefined,
  followContent: undefined,
  remark: undefined
})

const statusOptions = [
  { label: '新线索', value: 'new' },
  { label: '跟进中', value: 'following' },
  { label: '已下单', value: 'ordered' },
  { label: '无效', value: 'invalid' }
]

const intentOptions = [
  { label: '未知', value: 'unknown' },
  { label: '低意向', value: 'low' },
  { label: '中意向', value: 'medium' },
  { label: '高意向', value: 'high' }
]

const metrics = computed(() => [
  { label: '今日观众', value: valueOf(summary.value, 'totalCount'), desc: '去重后人数' },
  { label: '有评论', value: valueOf(summary.value, 'commentLeadCount'), desc: '优先处理' },
  { label: '新线索', value: valueOf(summary.value, 'newCount'), desc: '未分配/未处理' },
  { label: '跟进中', value: valueOf(summary.value, 'followingCount'), desc: '需要运营推进' },
  { label: '已下单', value: valueOf(summary.value, 'orderedCount'), desc: '可做转化复盘' }
])

const detailRoomName = computed(() => {
  const lead = detail.value.lead || {}
  return lead.display_room_name || lead.live_room_name || '-'
})

const ownerInputDisabled = computed(() => {
  if (!currentLead.value) return false
  return currentLead.value.status === 'ordered' || followForm.status === 'ordered' || isOwnerLocked(currentLead.value)
})

const ownerTip = computed(() => {
  if (!currentLead.value) return ''
  if (currentLead.value.status === 'ordered' || followForm.status === 'ordered') {
    return '已下单线索不能再填写或变更负责人。'
  }
  if (isOwnerLocked(currentLead.value)) {
    return '负责人填写后 24 小时内锁定，超过 24 小时且未下单会自动释放。'
  }
  return ''
})

function valueOf(row, key) {
  return row?.[key] ?? row?.[toSnake(key)] ?? 0
}

function formatLocalDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function toSnake(key) {
  return key.replace(/[A-Z]/g, (letter) => `_${letter.toLowerCase()}`)
}

function statusLabel(value) {
  return statusOptions.find((item) => item.value === value)?.label || '新线索'
}

function statusTag(value) {
  return ({ new: 'info', following: 'warning', ordered: 'success', invalid: 'danger' })[value] || 'info'
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

function isOwnerLocked(row) {
  if (!row?.owner_name || !row?.owner_assigned_time || row.status === 'ordered') return false
  const assigned = new Date(String(row.owner_assigned_time).replace(/-/g, '/')).getTime()
  return Number.isFinite(assigned) && Date.now() - assigned < 24 * 60 * 60 * 1000
}

function getList() {
  loading.value = true
  listViewerLeads(queryParams).then((res) => {
    leadList.value = res.rows || []
    total.value = res.total || 0
  }).finally(() => {
    loading.value = false
  })
  refreshSummary()
}

function refreshSummary() {
  getViewerSummary({
    leadDate: queryParams.leadDate,
    roomName: queryParams.roomName
  }).then((res) => {
    summary.value = res.data || {}
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  queryParams.pageNum = 1
  queryParams.leadDate = today
  queryParams.roomName = undefined
  getList()
}

function openDetail(row) {
  getViewerLead(row.lead_id).then((res) => {
    detail.value = res.data || { lead: row, comments: [], followRecords: [] }
    detailOpen.value = true
  })
}

function openFollow(row) {
  currentLead.value = row
  followForm.status = row.status || 'following'
  followForm.intent = row.intent || 'unknown'
  followForm.ownerName = row.owner_name
  followForm.orderNo = row.order_no
  followForm.followContent = undefined
  followForm.remark = row.remark
  followOpen.value = true
}

function submitFollow() {
  if (!currentLead.value) return
  if (followForm.status === 'ordered' && !String(followForm.orderNo || '').trim()) {
    proxy.$modal.msgWarning('已下单必须填写订单号')
    return
  }
  if (ownerInputDisabled.value && followForm.ownerName !== currentLead.value.owner_name) {
    proxy.$modal.msgWarning(ownerTip.value)
    return
  }

  const leadId = currentLead.value.lead_id
  const payload = {
    status: followForm.status,
    intent: followForm.intent,
    ownerName: ownerInputDisabled.value ? currentLead.value.owner_name : followForm.ownerName,
    orderNo: followForm.orderNo,
    remark: followForm.remark
  }

  updateViewerLead(leadId, payload).then(() => {
    if (!String(followForm.followContent || '').trim()) {
      return null
    }
    return addViewerFollow(leadId, {
      status: followForm.status,
      followContent: followForm.followContent,
      followResult: followForm.intent
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
  background:
    radial-gradient(circle at 10% 10%, rgba(108, 92, 231, 0.14), transparent 24rem),
    radial-gradient(circle at 92% 8%, rgba(162, 155, 254, 0.22), transparent 22rem);
}

.viewer-hero,
.search-panel,
.table-panel,
.metric-card {
  border: 1px solid rgba(132, 119, 255, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 45px rgba(74, 72, 128, 0.08);
  backdrop-filter: blur(16px);
}

.viewer-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 26px 30px;
  margin-bottom: 16px;

  h2 {
    margin: 8px 0 10px;
    color: #20233a;
    font-size: 30px;
    font-weight: 800;
  }
}

.eyebrow {
  display: inline-flex;
  margin: 0;
  padding: 6px 12px;
  border-radius: 999px;
  color: #6c5ce7;
  background: rgba(108, 92, 231, 0.1);
  font-weight: 700;
}

.hero-desc {
  margin: 0;
  color: #707893;
}

.hero-status {
  min-width: 180px;
  padding: 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(108, 92, 231, 0.1), rgba(162, 155, 254, 0.16));

  span,
  strong {
    display: block;
  }

  span {
    color: #7a8198;
    margin-bottom: 8px;
  }

  strong {
    color: #20233a;
  }
}

.metric-row {
  margin-bottom: 16px;
}

.metric-card {
  padding: 18px;
  min-height: 110px;
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 22px 52px rgba(108, 92, 231, 0.14);
  }

  span,
  small {
    display: block;
    color: #7c849c;
  }

  strong {
    display: block;
    margin: 10px 0 6px;
    color: #20233a;
    font-size: 26px;
  }
}

.search-panel,
.table-panel {
  padding: 18px;
  margin-bottom: 16px;
}

.customer-cell {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;

  strong {
    overflow: hidden;
    color: #20233a;
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
    color: #4f566b;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.detail-wrap {
  h4 {
    margin: 24px 0 12px;
    color: #20233a;
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
    color: #7c849c;
  }
}

.name-badge {
  display: grid;
  place-items: center;
  width: 52px;
  height: 52px;
  border-radius: 16px;
  color: #fff;
  background: linear-gradient(135deg, #6c5ce7, #a29bfe);
  font-size: 20px;
  font-weight: 800;
  box-shadow: 0 16px 34px rgba(108, 92, 231, 0.24);
}

.mb12 {
  margin-bottom: 12px;
}
</style>
