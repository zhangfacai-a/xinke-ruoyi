<template>
  <el-dialog
    v-model="visible"
    :title="`${operationText}数据`"
    width="min(1120px, calc(100vw - 24px))"
    :fullscreen="isMobile"
    append-to-body
    :close-on-click-modal="false"
    :before-close="beforeClose"
    class="data-import-wizard"
  >
    <el-steps :active="step" finish-status="success" simple class="wizard-steps">
      <el-step title="选择文件" icon="Upload" />
      <el-step title="字段映射" icon="Connection" />
      <el-step title="确认影响" icon="Warning" />
      <el-step title="执行结果" icon="CircleCheck" />
    </el-steps>

    <section v-if="step === 0" class="upload-stage">
      <div class="target-summary">
        <span>目标数据表</span>
        <strong>{{ dataset.displayName }}</strong>
        <small>当前版本 v{{ dataset.currentVersionNo || dataset.currentSchemaVersion || 1 }} · {{ formatCount(dataset.rowCount) }} 行</small>
      </div>

      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :disabled="uploading"
        accept=".xls,.xlsx,.csv"
        :on-change="handleFileChange"
        :on-exceed="() => proxy.$modal.msgWarning('每次只能选择一个文件')"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 或 CSV 文件拖到此处，或<em>点击选择</em></div>
        <template #tip><div class="el-upload__tip">支持 .xls、.xlsx、.csv，每次上传一个文件</div></template>
      </el-upload>

      <div v-if="uploading || uploadMessage" class="upload-progress">
        <el-progress :percentage="uploadPercent" :status="uploadStatus" />
        <span>{{ uploadMessage }}</span>
      </div>
    </section>

    <template v-else-if="step === 1">
      <div class="mapping-toolbar">
        <div>
          <strong>{{ preview.fileName || '已上传文件' }}</strong>
          <span>共 {{ formatCount(preview.totalRows) }} 行，{{ sourceColumns.length }} 个源字段</span>
        </div>
        <el-select
          v-model="preview.sheetName"
          :disabled="preview.sheetNames.length <= 1 || sheetLoading"
          :loading="sheetLoading"
          style="width: 220px"
          @change="changeSheet"
        >
          <el-option v-for="name in preview.sheetNames" :key="name" :label="name" :value="name" />
        </el-select>
      </div>

      <div v-if="preview.warnings.length" class="warning-list">
        <el-alert v-for="(warning, index) in preview.warnings" :key="index" :title="String(warning)" type="warning" :closable="false" show-icon />
      </div>

      <el-alert
        v-if="mappingIssues.length"
        :title="`字段映射还有 ${mappingIssues.length} 个问题`"
        :description="mappingIssues.slice(0, 4).join('；')"
        type="error"
        :closable="false"
        show-icon
        class="mapping-alert"
      />

      <el-table :data="sourceColumns" row-key="sourceIndex" max-height="470">
        <el-table-column label="源字段" min-width="180" fixed="left">
          <template #default="scope">
            <strong>{{ scope.row.sourceName }}</strong>
            <small class="source-meta">{{ scope.row.dataType || 'UNKNOWN' }}</small>
          </template>
        </el-table-column>
        <el-table-column label="样例值" min-width="210" show-overflow-tooltip>
          <template #default="scope">{{ samplesText(scope.row.samples) }}</template>
        </el-table-column>
        <el-table-column label="映射到当前字段" min-width="250">
          <template #default="scope">
            <el-select v-model="scope.row.targetColumnId" filterable style="width: 100%">
              <el-option
                v-for="target in columns"
                :key="target.columnId"
                :label="`${target.displayName} (${target.physicalName})`"
                :value="target.columnId"
                :disabled="targetUsedByOther(target.columnId, scope.row.sourceIndex)"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="目标类型" width="160">
          <template #default="scope">{{ targetTypeText(scope.row.targetColumnId) }}</template>
        </el-table-column>
        <el-table-column label="校验" width="110" align="center" fixed="right">
          <template #default="scope">
            <el-tag :type="mappingStatus(scope.row).type" effect="light">{{ mappingStatus(scope.row).label }}</el-tag>
          </template>
        </el-table-column>
      </el-table>

      <div class="wizard-actions">
        <el-button @click="resetWizard">重新选择文件</el-button>
        <el-button type="primary" icon="ArrowRight" @click="goToConfirmation">检查并继续</el-button>
      </div>
    </template>

    <section v-else-if="step === 2" class="impact-stage">
      <el-alert
        :title="operation === 'APPEND' ? '追加只会增加数据，不改变当前字段结构' : '覆盖成功后，新版本将替代当前可见数据'"
        :description="operation === 'REPLACE' ? '任务失败时当前版本保持不变；旧版本是否可回滚以服务端保留策略为准。' : '任务会严格按已确认的字段映射校验，任一非法行都会使本次导入失败。'"
        :type="operation === 'REPLACE' ? 'warning' : 'info'"
        :closable="false"
        show-icon
      />

      <div class="impact-grid">
        <div><span>当前版本</span><strong>v{{ dataset.currentVersionNo || dataset.currentSchemaVersion || 1 }}</strong></div>
        <div><span>当前数据量</span><strong>{{ formatCount(dataset.rowCount) }}</strong></div>
        <div><span>文件数据量</span><strong>{{ formatCount(preview.totalRows) }}</strong></div>
        <div>
          <span>完成后预计</span>
          <strong>{{ formatCount(estimatedRows) }}</strong>
        </div>
      </div>

      <div class="mapping-review">
        <strong>字段映射</strong>
        <el-tag v-for="source in sourceColumns" :key="source.sourceIndex" effect="plain">
          {{ source.sourceName }} → {{ targetColumn(source.targetColumnId)?.displayName }}
        </el-tag>
      </div>

      <el-form v-if="operation === 'REPLACE'" label-position="top" class="replace-confirmation">
        <el-form-item :label="`请输入数据表名称“${dataset.displayName}”确认覆盖`">
          <el-input v-model="confirmationName" autocomplete="off" />
        </el-form-item>
      </el-form>

      <div class="wizard-actions">
        <el-button @click="step = 1">返回映射</el-button>
        <el-button
          :type="operation === 'REPLACE' ? 'danger' : 'primary'"
          :icon="operation === 'REPLACE' ? 'Refresh' : 'Upload'"
          :loading="confirming"
          :disabled="operation === 'REPLACE' && confirmationName !== dataset.displayName"
          @click="confirmImport"
        >确认{{ operationText }}</el-button>
      </div>
    </section>

    <section v-else class="result-stage">
      <el-alert
        v-if="pollWarning"
        title="暂时无法刷新任务状态，任务仍可能在后台执行"
        :description="pollWarning"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-result :icon="resultIcon" :title="resultTitle" :sub-title="job.errorMessage || job.phase || ''">
        <template #extra>
          <div class="job-progress">
            <el-progress v-if="!terminalStatus" :percentage="jobProgress" />
            <div class="job-stats">
              <span>状态：{{ statusLabel(job.status) }}</span>
              <span v-if="job.totalRows != null">总行数：{{ formatCount(job.totalRows) }}</span>
              <span v-if="job.successRows != null">成功：{{ formatCount(job.successRows) }}</span>
              <span v-if="job.failedRows">失败：{{ formatCount(job.failedRows) }}</span>
            </div>
            <el-button v-if="pollWarning && !terminalStatus" icon="Refresh" :loading="polling" @click="pollJob">立即刷新</el-button>
          </div>
        </template>
      </el-result>

      <div v-if="errorsLoading || importErrors.length" class="import-errors">
        <div class="errors-heading"><strong>失败明细</strong><span>最多显示 1000 条</span></div>
        <el-table v-loading="errorsLoading" :data="importErrors" max-height="320">
          <el-table-column label="源行" prop="sourceRowNo" width="82" align="right" />
          <el-table-column label="字段" prop="sourceColumnName" min-width="140" show-overflow-tooltip />
          <el-table-column label="原始值" prop="rawValue" min-width="180" show-overflow-tooltip />
          <el-table-column label="原因" prop="errorMessage" min-width="260" show-overflow-tooltip />
        </el-table>
      </div>

      <div class="wizard-actions">
        <el-button v-if="retryableStatus" @click="returnToMapping">返回映射</el-button>
        <el-button v-if="retryableStatus" icon="Refresh" @click="resetWizard">重新上传</el-button>
        <el-button v-if="terminalStatus" type="primary" @click="visible = false">完成</el-button>
        <el-button v-else @click="requestClose">关闭，后台继续</el-button>
      </div>
    </section>
  </el-dialog>
</template>

<script setup>
import {
  confirmDatasetImport,
  getMutationErrors,
  getMutationJob,
  previewDatasetImport,
  updateDatasetImportSheet
} from '@/api/datahub/mutation'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  datasetId: { type: [Number, String], required: true },
  operation: { type: String, required: true },
  dataset: { type: Object, default: () => ({}) },
  columns: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'submitted', 'completed', 'conflict'])
const { proxy } = getCurrentInstance()
const uploadRef = ref(null)
const step = ref(0)
const uploading = ref(false)
const uploadPercent = ref(0)
const uploadStatus = ref('')
const uploadMessage = ref('')
const sheetLoading = ref(false)
const confirming = ref(false)
const sourceColumns = ref([])
const confirmationName = ref('')
const job = reactive({})
const importErrors = ref([])
const errorsLoading = ref(false)
const pollWarning = ref('')
const polling = ref(false)
const baseVersionId = ref(null)
let pollTimer
let pollRequestId = 0
let pollFailures = 0
let terminalEmitted = false
let forceClosing = false

const preview = reactive(emptyPreview())
const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})
const operation = computed(() => String(props.operation || '').toUpperCase())
const operationText = computed(() => operation.value === 'REPLACE' ? '覆盖' : '追加')
const isMobile = computed(() => window.innerWidth <= 768)
const estimatedRows = computed(() => operation.value === 'APPEND'
  ? Number(props.dataset.rowCount || 0) + Number(preview.totalRows || 0)
  : Number(preview.totalRows || 0))
const terminalStatus = computed(() => ['SUCCESS', 'FAILED', 'VALIDATION_FAILED', 'MANUAL_REQUIRED'].includes(job.status))
const retryableStatus = computed(() => ['FAILED', 'VALIDATION_FAILED'].includes(job.status))
const resultIcon = computed(() => job.status === 'SUCCESS' ? 'success' : terminalStatus.value ? 'error' : 'info')
const resultTitle = computed(() => {
  if (job.status === 'SUCCESS') return `${operationText.value}成功`
  if (job.status === 'VALIDATION_FAILED') return '数据校验未通过'
  if (job.status === 'MANUAL_REQUIRED') return '任务需要人工处理'
  if (job.status === 'FAILED') return `${operationText.value}失败`
  return `正在${operationText.value}数据`
})
const jobProgress = computed(() => {
  const explicit = Number(job.progressPercent ?? job.progress)
  if (Number.isFinite(explicit)) return Math.max(0, Math.min(100, Math.round(explicit)))
  const total = Number(job.totalRows)
  const processed = Number(job.processedRows)
  if (total > 0 && Number.isFinite(processed)) return Math.min(100, Math.round((processed * 100) / total))
  return { QUEUED: 8, VALIDATING: 30, STAGING: 65, COMMITTING: 90, RECOVERING: 92, SUCCESS: 100 }[job.status] || 5
})
const mappingIssues = computed(validateMappings)

watch(() => props.modelValue, value => {
  if (value) initialize()
  else if (step.value !== 3 || terminalStatus.value) stopPolling()
})

onBeforeUnmount(stopPolling)

function emptyPreview() {
  return { previewId: undefined, fileName: '', sheetNames: [], sheetName: '', totalRows: 0, warnings: [], sampleRows: [] }
}

function normalizeResponse(response) {
  return response?.data ?? response ?? {}
}

function initialize() {
  resetWizard()
  baseVersionId.value = props.dataset.currentVersionId
  forceClosing = false
}

function resetWizard() {
  stopPolling()
  step.value = 0
  uploading.value = false
  uploadPercent.value = 0
  uploadStatus.value = ''
  uploadMessage.value = ''
  sourceColumns.value = []
  confirmationName.value = ''
  Object.assign(preview, emptyPreview())
  for (const key of Object.keys(job)) delete job[key]
  importErrors.value = []
  pollWarning.value = ''
  pollFailures = 0
  terminalEmitted = false
  nextTick(() => uploadRef.value?.clearFiles())
}

function handleFileChange(file) {
  if (!file?.raw || uploading.value) return
  if (!/\.(xls|xlsx|csv)$/i.test(file.name || '')) {
    proxy.$modal.msgError('请选择 .xls、.xlsx 或 .csv 文件')
    uploadRef.value?.clearFiles()
    return
  }
  uploadFile(file.raw)
}

async function uploadFile(file) {
  uploading.value = true
  uploadPercent.value = 0
  uploadStatus.value = ''
  uploadMessage.value = '正在上传文件...'
  const formData = new FormData()
  formData.append('file', file)
  formData.append('operation', operation.value)
  if (baseVersionId.value != null) formData.append('baseVersionId', String(baseVersionId.value))
  try {
    const response = await previewDatasetImport(props.datasetId, formData, handleUploadProgress)
    uploadPercent.value = 100
    uploadStatus.value = 'success'
    uploadMessage.value = '文件解析完成'
    applyPreview(normalizeResponse(response))
  } catch (error) {
    uploadStatus.value = 'exception'
    uploadMessage.value = error?.message || '文件解析失败'
    handleConflict(error)
  } finally {
    uploading.value = false
  }
}

function handleUploadProgress(event) {
  if (!event.total) return
  const percent = Math.round((event.loaded * 100) / event.total)
  uploadPercent.value = Math.min(percent, 95)
  if (percent >= 100) uploadMessage.value = '文件已上传，正在解析字段和样例数据...'
}

function applyPreview(data) {
  Object.assign(preview, emptyPreview(), data)
  preview.sheetNames = Array.isArray(data.sheetNames) && data.sheetNames.length ? data.sheetNames.map(String) : [data.sheetName || '数据']
  preview.sheetName = data.sheetName || preview.sheetNames[0]
  preview.warnings = Array.isArray(data.warnings) ? data.warnings : data.warnings ? [data.warnings] : []
  const suggestions = new Map((data.suggestedMappings || data.mappings || [])
    .map(item => [String(item.sourceIndex), item.targetColumnId]))
  const usedTargets = new Set()
  sourceColumns.value = (data.sourceColumns || data.columns || []).map((column, index) => {
    const source = {
      ...column,
      sourceIndex: column.sourceIndex ?? index,
      sourceName: column.sourceName || column.displayName || `第 ${index + 1} 列`,
      dataType: String(column.inferredType || column.dataType || '').toUpperCase(),
      samples: Array.isArray(column.samples) ? column.samples : [],
      targetColumnId: column.targetColumnId ?? column.mappedColumnId ?? suggestions.get(String(column.sourceIndex ?? index))
    }
    if (source.targetColumnId == null) source.targetColumnId = autoTarget(source, usedTargets)?.columnId
    if (source.targetColumnId != null) usedTargets.add(String(source.targetColumnId))
    return source
  })
  step.value = 1
}

function autoTarget(source, usedTargets) {
  const name = normalizeName(source.sourceName)
  return props.columns.find(column => {
    if (usedTargets.has(String(column.columnId))) return false
    return [column.displayName, column.sourceName, column.physicalName].some(candidate => normalizeName(candidate) === name)
  })
}

function normalizeName(value) {
  return String(value || '').trim().toLowerCase().replace(/[\s_-]+/g, '')
}

async function changeSheet(sheetName) {
  sheetLoading.value = true
  try {
    const response = await updateDatasetImportSheet(props.datasetId, preview.previewId, {
      sheetName,
      baseVersionId: baseVersionId.value
    })
    applyPreview(normalizeResponse(response))
  } catch (error) {
    handleConflict(error)
  } finally {
    sheetLoading.value = false
  }
}

function targetColumn(columnId) {
  return props.columns.find(column => String(column.columnId) === String(columnId))
}

function targetUsedByOther(columnId, sourceIndex) {
  return sourceColumns.value.some(source => source.sourceIndex !== sourceIndex && String(source.targetColumnId) === String(columnId))
}

function compatibleTypes(sourceType, targetType) {
  if (!sourceType || !targetType) return false
  if (['VARCHAR', 'TEXT'].includes(targetType)) return true
  if (sourceType === targetType) return true
  if (sourceType === 'BIGINT' && targetType === 'DECIMAL') return true
  if (sourceType === 'DATE' && targetType === 'DATETIME') return true
  return false
}

function mappingStatus(source) {
  const target = targetColumn(source.targetColumnId)
  if (!target) return { type: 'danger', label: '未映射' }
  if (!compatibleTypes(source.dataType, target.dataType)) return { type: 'danger', label: '类型冲突' }
  return { type: 'success', label: '可导入' }
}

function validateMappings() {
  const issues = []
  const targetIds = new Set()
  if (sourceColumns.value.length !== props.columns.length) {
    issues.push(`源字段数（${sourceColumns.value.length}）与目标字段数（${props.columns.length}）不一致`)
  }
  for (const source of sourceColumns.value) {
    const target = targetColumn(source.targetColumnId)
    if (!target) issues.push(`源字段“${source.sourceName}”尚未映射`)
    else if (!compatibleTypes(source.dataType, target.dataType)) issues.push(`“${source.sourceName}”与“${target.displayName}”类型不兼容`)
    else if (targetIds.has(String(target.columnId))) issues.push(`目标字段“${target.displayName}”被重复映射`)
    else targetIds.add(String(target.columnId))
  }
  for (const target of props.columns) {
    if (!targetIds.has(String(target.columnId))) issues.push(`目标字段“${target.displayName}”没有来源`)
  }
  return issues
}

function goToConfirmation() {
  if (mappingIssues.value.length) return proxy.$modal.msgError(mappingIssues.value[0])
  confirmationName.value = ''
  step.value = 2
}

async function confirmImport() {
  if (mappingIssues.value.length) return proxy.$modal.msgError(mappingIssues.value[0])
  confirming.value = true
  try {
    const response = await confirmDatasetImport(props.datasetId, preview.previewId, {
      baseVersionId: baseVersionId.value,
      sheetName: preview.sheetName,
      confirmationName: operation.value === 'REPLACE' ? confirmationName.value : null,
      mappings: sourceColumns.value.map(source => ({
        sourceIndex: source.sourceIndex,
        targetColumnId: source.targetColumnId
      }))
    })
    Object.assign(job, normalizeResponse(response))
    step.value = 3
    emit('submitted', { ...job })
    pollFailures = 0
    pollWarning.value = ''
    pollJob()
  } catch (error) {
    handleConflict(error)
  } finally {
    confirming.value = false
  }
}

async function pollJob() {
  clearPolling()
  if (!job.previewId || terminalStatus.value) return finishTerminal()
  if (polling.value) return
  const requestId = ++pollRequestId
  polling.value = true
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
    if (requestId === pollRequestId) polling.value = false
  }
  if (requestId !== pollRequestId) return
  if (terminalStatus.value) return
  const delay = Math.min(10000, 1500 * Math.max(1, pollFailures + 1))
  pollTimer = window.setTimeout(pollJob, delay)
}

async function finishTerminal() {
  clearPolling()
  if (job.status !== 'SUCCESS') await loadErrors().catch(() => {})
  if (!terminalEmitted) {
    terminalEmitted = true
    emit('completed', { ...job })
  }
}

async function loadErrors() {
  if (!job.previewId || errorsLoading.value) return
  errorsLoading.value = true
  try {
    const response = await getMutationErrors(job.previewId)
    const payload = normalizeResponse(response)
    importErrors.value = Array.isArray(payload) ? payload : payload.rows || payload.errors || []
  } finally {
    errorsLoading.value = false
  }
}

function returnToMapping() {
  stopPolling()
  for (const key of Object.keys(job)) delete job[key]
  importErrors.value = []
  pollWarning.value = ''
  step.value = 1
}

function handleConflict(error) {
  const status = error?.response?.status
  const code = error?.response?.data?.code || error?.code
  const message = String(error?.message || error?.response?.data?.msg || '')
  if (Number(status) === 409 || Number(code) === 409
    || code === 'DATA_VERSION_CONFLICT' || /版本.*(变化|冲突|过期)/.test(message)) {
    emit('conflict', error)
  }
}

function targetTypeText(columnId) {
  const column = targetColumn(columnId)
  if (!column) return '-'
  if (column.dataType === 'VARCHAR') return `VARCHAR(${column.columnLength || column.length || 255})`
  if (column.dataType === 'DECIMAL') return `DECIMAL(${column.numericPrecision || column.precision || 18},${column.numericScale ?? column.scale ?? 0})`
  return column.dataType || '-'
}

function samplesText(samples) {
  return Array.isArray(samples) ? samples.filter(value => value !== null && value !== '').slice(0, 3).join('、') || '-' : '-'
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
  const running = step.value === 3 && !terminalStatus.value
  if (forceClosing || !running) return done()
  proxy.$modal.confirm('任务仍在后台执行，关闭窗口不会取消任务。确认关闭吗？').then(() => {
    forceClosing = true
    done()
  }).catch(() => {})
}

function clearPolling() {
  if (pollTimer) window.clearTimeout(pollTimer)
  pollTimer = undefined
}

function stopPolling() {
  clearPolling()
  pollRequestId += 1
  polling.value = false
}
</script>

<style scoped>
.wizard-steps {
  margin-bottom: 18px;
}

.upload-stage {
  max-width: 760px;
  margin: 24px auto 0;
}

.target-summary {
  display: grid;
  margin-bottom: 16px;
  text-align: center;
}

.target-summary span,
.target-summary small,
.mapping-toolbar span {
  color: #909399;
  font-size: 12px;
}

.target-summary strong {
  margin: 4px 0;
  font-size: 18px;
}

.upload-stage :deep(.el-upload-dragger) {
  min-height: 210px;
  padding-top: 42px;
}

.upload-progress {
  margin-top: 16px;
}

.upload-progress span {
  display: block;
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.mapping-toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.mapping-toolbar strong,
.mapping-toolbar span,
.source-meta {
  display: block;
}

.warning-list {
  display: grid;
  gap: 6px;
  margin-bottom: 10px;
}

.mapping-alert {
  margin-bottom: 10px;
}

.source-meta {
  color: #a8abb2;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 10px;
  margin-top: 2px;
}

.wizard-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 18px;
}

.impact-stage {
  max-width: 860px;
  margin: 28px auto 0;
}

.impact-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 20px 0;
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
}

.impact-grid > div {
  padding: 14px 18px;
  border-right: 1px solid #ebeef5;
}

.impact-grid > div:last-child {
  border-right: 0;
}

.impact-grid span,
.impact-grid strong {
  display: block;
}

.impact-grid span {
  color: #909399;
  font-size: 12px;
}

.impact-grid strong {
  margin-top: 5px;
  font-size: 18px;
}

.mapping-review {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.mapping-review strong {
  width: 100%;
  margin-bottom: 2px;
}

.replace-confirmation {
  margin-top: 20px;
}

.result-stage {
  max-width: 900px;
  margin: 10px auto 0;
}

.job-progress {
  width: min(600px, 72vw);
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

.errors-heading {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.errors-heading span {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 768px) {
  .mapping-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .mapping-toolbar :deep(.el-select) {
    width: 100% !important;
  }

  .impact-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .impact-grid > div:nth-child(2) {
    border-right: 0;
  }

  .impact-grid > div:nth-child(-n + 2) {
    border-bottom: 1px solid #ebeef5;
  }
}
</style>
