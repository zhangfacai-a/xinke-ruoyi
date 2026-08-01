<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="min(760px, calc(100vw - 24px))"
    append-to-body
    :close-on-click-modal="false"
    :before-close="beforeClose"
  >
    <el-alert
      v-if="pollWarning"
      title="暂时无法刷新任务状态，任务仍可能在后台执行"
      :description="pollWarning"
      type="warning"
      :closable="false"
      show-icon
      class="poll-alert"
    />

    <el-result :icon="resultIcon" :title="resultTitle" :sub-title="job.errorMessage || job.phase || ''">
      <template #extra>
        <div class="job-progress">
          <el-progress
            v-if="!terminalStatus"
            :percentage="progress"
            :status="progressStatus"
          />
          <div class="job-stats">
            <span>状态：{{ statusLabel(job.status) }}</span>
            <span v-if="job.totalRows != null">总行数：{{ formatCount(job.totalRows) }}</span>
            <span v-if="job.successRows != null">成功：{{ formatCount(job.successRows) }}</span>
            <span v-if="job.failedRows">失败：{{ formatCount(job.failedRows) }}</span>
          </div>
          <el-button v-if="pollWarning && !terminalStatus" icon="Refresh" :loading="refreshing" @click="pollNow">
            立即刷新
          </el-button>
        </div>
      </template>
    </el-result>

    <div v-if="errorsLoading || errors.length" class="job-errors">
      <div class="job-errors-heading">
        <strong>失败明细</strong>
        <span>最多显示 1000 条</span>
      </div>
      <el-table v-loading="errorsLoading" :data="errors" max-height="300" empty-text="暂无错误明细">
        <el-table-column label="源行" prop="sourceRowNo" width="82" align="right" />
        <el-table-column label="字段" prop="sourceColumnName" min-width="130" show-overflow-tooltip />
        <el-table-column label="原始值" prop="rawValue" min-width="160" show-overflow-tooltip />
        <el-table-column label="原因" prop="errorMessage" min-width="220" show-overflow-tooltip />
      </el-table>
    </div>

    <template #footer>
      <el-button v-if="!terminalStatus" @click="requestClose">关闭，后台继续</el-button>
      <el-button v-else type="primary" @click="visible = false">完成</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { getMutationErrors, getMutationJob } from '@/api/datahub/mutation'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  title: { type: String, default: '数据任务' },
  initialJob: { type: Object, default: () => ({}) },
  successTitle: { type: String, default: '操作成功' }
})

const emit = defineEmits(['update:modelValue', 'completed'])
const { proxy } = getCurrentInstance()
const job = reactive({})
const errors = ref([])
const errorsLoading = ref(false)
const pollWarning = ref('')
const refreshing = ref(false)
let pollTimer
let pollFailures = 0
let pollRequestId = 0
let completedEmitted = false
let forceClosing = false

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const terminalStatus = computed(() => ['SUCCESS', 'FAILED', 'VALIDATION_FAILED', 'MANUAL_REQUIRED'].includes(job.status))

const resultIcon = computed(() => {
  if (job.status === 'SUCCESS') return 'success'
  if (terminalStatus.value) return 'error'
  return 'info'
})

const resultTitle = computed(() => {
  if (job.status === 'SUCCESS') return props.successTitle
  if (job.status === 'VALIDATION_FAILED') return '数据校验未通过'
  if (job.status === 'MANUAL_REQUIRED') return '任务需要人工处理'
  if (job.status === 'FAILED') return '操作失败'
  return '任务正在执行'
})

const progress = computed(() => {
  const explicit = Number(job.progressPercent ?? job.progress)
  if (Number.isFinite(explicit)) return Math.max(0, Math.min(100, Math.round(explicit)))
  const total = Number(job.totalRows)
  const processed = Number(job.processedRows)
  if (total > 0 && Number.isFinite(processed)) return Math.min(100, Math.round((processed * 100) / total))
  return { QUEUED: 8, VALIDATING: 30, STAGING: 65, COMMITTING: 90, RECOVERING: 92, SUCCESS: 100 }[job.status] || 5
})

const progressStatus = computed(() => job.status === 'SUCCESS' ? 'success' : terminalStatus.value ? 'exception' : '')

watch(() => props.modelValue, value => {
  if (value) start()
  else if (terminalStatus.value) stopPolling()
})

onBeforeUnmount(stopPolling)

function normalizeResponse(response) {
  return response?.data ?? response ?? {}
}

function start() {
  stopPolling()
  Object.assign(job, props.initialJob || {})
  errors.value = []
  pollWarning.value = ''
  pollFailures = 0
  completedEmitted = false
  forceClosing = false
  if (terminalStatus.value) return finishTerminal()
  pollNow()
}

async function pollNow() {
  clearPolling()
  if (!job.previewId || terminalStatus.value) return
  if (refreshing.value) return
  const requestId = ++pollRequestId
  refreshing.value = true
  try {
    const response = await getMutationJob(job.previewId)
    if (requestId !== pollRequestId) return
    Object.assign(job, normalizeResponse(response))
    pollFailures = 0
    pollWarning.value = ''
    if (terminalStatus.value) return finishTerminal()
  } catch (error) {
    if (requestId !== pollRequestId) return
    pollFailures += 1
    pollWarning.value = error?.message || '请稍后重试或到导入记录中查看最终结果'
  } finally {
    if (requestId === pollRequestId) refreshing.value = false
  }
  if (requestId !== pollRequestId) return
  if (terminalStatus.value) return
  const delay = Math.min(10000, 1500 * Math.max(1, pollFailures + 1))
  pollTimer = window.setTimeout(pollNow, delay)
}

async function finishTerminal() {
  clearPolling()
  if (job.status !== 'SUCCESS') await loadErrors().catch(() => {})
  if (!completedEmitted) {
    completedEmitted = true
    emit('completed', { ...job })
  }
}

async function loadErrors() {
  if (!job.previewId || errorsLoading.value || errors.value.length) return
  errorsLoading.value = true
  try {
    const response = await getMutationErrors(job.previewId)
    const payload = normalizeResponse(response)
    errors.value = Array.isArray(payload) ? payload : payload.rows || payload.errors || []
  } finally {
    errorsLoading.value = false
  }
}

function clearPolling() {
  if (pollTimer) window.clearTimeout(pollTimer)
  pollTimer = undefined
}

function stopPolling() {
  clearPolling()
  pollRequestId += 1
  refreshing.value = false
}

function statusLabel(status) {
  return {
    PARSING: '解析中', PENDING_CONFIRM: '等待确认', QUEUED: '排队中', STAGING: '写入中',
    VALIDATING: '校验中', COMMITTING: '发布中', SUCCESS: '成功', VALIDATION_FAILED: '校验失败',
    FAILED: '失败', RECOVERING: '恢复中', MANUAL_REQUIRED: '需要人工处理'
  }[status] || status || '等待处理'
}

function formatCount(value) {
  const count = Number(value)
  return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '0'
}

function requestClose() {
  beforeClose(() => { visible.value = false })
}

function beforeClose(done) {
  if (forceClosing || terminalStatus.value) return done()
  proxy.$modal.confirm('任务仍在后台执行，关闭窗口不会取消任务。确认关闭吗？').then(() => {
    forceClosing = true
    done()
  }).catch(() => {})
}
</script>

<style scoped>
.poll-alert {
  margin-bottom: 12px;
}

.job-progress {
  width: min(580px, 72vw);
}

.job-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 18px;
  margin: 12px 0;
  color: #606266;
  font-size: 13px;
}

.job-errors {
  margin-top: 8px;
}

.job-errors-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 8px;
}

.job-errors-heading span {
  color: #909399;
  font-size: 12px;
}
</style>
