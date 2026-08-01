<template>
  <div class="app-container datahub-create-page">
    <div class="page-toolbar">
      <el-button circle icon="ArrowLeft" aria-label="返回" @click="handleBack" />
      <div class="page-heading">
        <h2>创建数据表</h2>
        <span v-if="preview.fileName">{{ preview.fileName }}</span>
      </div>
    </div>

    <el-steps :active="activeStep" finish-status="success" simple class="create-steps">
      <el-step title="选择文件" icon="Upload" />
      <el-step title="配置结构" icon="Operation" />
      <el-step title="创建数据表" icon="CircleCheck" />
    </el-steps>

    <section v-if="activeStep === 0" class="upload-stage">
      <el-upload
        ref="uploadRef"
        drag
        :auto-upload="false"
        :limit="1"
        :disabled="uploading"
        accept=".xls,.xlsx,.csv"
        :on-change="handleFileChange"
        :on-remove="handleFileRemove"
        :on-exceed="handleFileExceed"
      >
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 或 CSV 文件拖到此处，或<em>点击选择</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xls、.xlsx、.csv，每次上传一个文件</div>
        </template>
      </el-upload>

      <div v-if="uploading || uploadMessage" class="upload-progress">
        <el-progress :percentage="uploadPercent" :status="uploadProgressStatus" />
        <span>{{ uploadMessage }}</span>
      </div>
    </section>

    <template v-else-if="activeStep === 1">
      <section class="config-section">
        <div class="section-title-row">
          <div>
            <h3>数据表信息</h3>
            <span v-if="preview.expiresAt">预览有效期至 {{ parseTime(preview.expiresAt) }}</span>
          </div>
          <el-button icon="Refresh" :disabled="sheetLoading" @click="restart">重新选择文件</el-button>
        </div>

        <el-form ref="configRef" :model="preview" label-width="96px" class="dataset-form">
          <el-row :gutter="18">
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="数据表名称" prop="displayName" required>
                <el-input v-model="preview.displayName" maxlength="255" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="英文表名" prop="physicalName" required>
                <el-input v-model="preview.physicalName" maxlength="64" class="mono-input" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="8">
              <el-form-item label="Sheet">
                <el-select
                  v-model="preview.sheetName"
                  :loading="sheetLoading"
                  :disabled="preview.sheetNames.length <= 1"
                  style="width: 100%"
                  @change="handleSheetChange"
                >
                  <el-option v-for="name in preview.sheetNames" :key="name" :label="name" :value="name" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div v-if="preview.warnings.length" class="warning-list">
          <el-alert
            v-for="(warning, index) in preview.warnings"
            :key="index"
            :title="String(warning)"
            type="warning"
            :closable="false"
            show-icon
          />
        </div>
      </section>

      <section class="config-section">
        <div class="section-title-row">
          <div>
            <h3>字段配置</h3>
            <span>共 {{ preview.columns.length }} 个字段</span>
          </div>
          <el-tag v-if="reviewCount" type="warning">{{ reviewCount }} 个字段待确认</el-tag>
        </div>

        <el-table :data="preview.columns" row-key="sourceIndex" max-height="440">
          <el-table-column label="#" width="58" align="center">
            <template #default="scope">{{ Number(scope.row.sourceIndex) + 1 }}</template>
          </el-table-column>
          <el-table-column label="原始列名" prop="sourceName" min-width="150" show-overflow-tooltip />
          <el-table-column label="显示名称" min-width="170">
            <template #default="scope">
              <el-input v-model="scope.row.displayName" maxlength="255" @input="markReviewed(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="英文字段名" min-width="190">
            <template #default="scope">
              <el-input v-model="scope.row.physicalName" maxlength="64" class="mono-input" @input="markReviewed(scope.row)" />
            </template>
          </el-table-column>
          <el-table-column label="字段类型" width="145">
            <template #default="scope">
              <el-select v-model="scope.row.dataType" style="width: 100%" @change="handleTypeChange(scope.row)">
                <el-option v-for="item in dataTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="长度/精度" width="190">
            <template #default="scope">
              <el-input-number
                v-if="scope.row.dataType === 'VARCHAR'"
                v-model="scope.row.length"
                :min="1"
                :max="1000"
                controls-position="right"
                style="width: 100%"
              />
              <div v-else-if="scope.row.dataType === 'DECIMAL'" class="decimal-fields">
                <el-input-number v-model="scope.row.precision" :min="1" :max="38" controls-position="right" />
                <span>,</span>
                <el-input-number v-model="scope.row.scale" :min="0" :max="38" controls-position="right" />
              </div>
              <span v-else class="muted">-</span>
            </template>
          </el-table-column>
          <el-table-column label="允许为空" width="95" align="center">
            <template #default="scope"><el-switch v-model="scope.row.nullable" /></template>
          </el-table-column>
          <el-table-column label="示例值" min-width="190">
            <template #default="scope">
              <span class="sample-values">{{ formatSamples(scope.row.samples) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="92" align="center" fixed="right">
            <template #default="scope">
              <el-tag :type="scope.row.needsReview ? 'warning' : 'success'" effect="light">
                {{ scope.row.needsReview ? '待确认' : '已确认' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section class="config-section sample-section">
        <div class="section-title-row">
          <div>
            <h3>数据预览</h3>
            <span>仅展示系统抽取的样例数据</span>
          </div>
        </div>
        <el-table :data="preview.sampleRows" max-height="340" empty-text="暂无样例数据">
          <el-table-column
            v-for="column in preview.columns"
            :key="column.sourceIndex"
            :label="column.displayName || column.sourceName"
            :min-width="columnMinWidth(column)"
          >
            <template #default="scope">{{ sampleCell(scope.row, column) }}</template>
          </el-table-column>
        </el-table>
      </section>

      <div class="page-actions">
        <el-button @click="handleBack">取消</el-button>
        <el-button type="primary" icon="CircleCheck" :loading="confirming" @click="handleConfirm">确认创建</el-button>
      </div>
    </template>

    <section v-else class="job-stage">
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
        :sub-title="resultMessage"
      >
        <template #extra>
          <div class="job-progress">
            <el-progress v-if="!terminalStatus" :percentage="jobProgress" :status="jobProgressStatus" />
            <div class="job-stats">
              <span>状态：{{ jobStatusLabel(job.status) }}</span>
              <span v-if="job.totalRows != null">总行数：{{ formatCount(job.totalRows) }}</span>
              <span v-if="job.successRows != null">成功：{{ formatCount(job.successRows) }}</span>
              <span v-if="job.failedRows">失败：{{ formatCount(job.failedRows) }}</span>
            </div>
          </div>
          <div class="result-actions">
            <el-button v-if="job.status === 'SUCCESS' && job.datasetId" type="primary" icon="View" @click="openDataset">查看数据</el-button>
            <el-button v-if="terminalStatus && job.status !== 'SUCCESS' && preview.previewId" type="primary" icon="Edit" @click="editConfiguration">返回修改</el-button>
            <el-button v-if="terminalStatus && job.status !== 'SUCCESS'" icon="Refresh" @click="restart">重新上传</el-button>
            <el-button @click="handleBack">返回列表</el-button>
          </div>
        </template>
      </el-result>

      <div v-if="errorLoading || importErrors.length" class="import-errors">
        <div class="section-title-row">
          <div>
            <h3>失败明细</h3>
            <span>最多显示 1000 条校验错误</span>
          </div>
        </div>
        <el-table v-loading="errorLoading" :data="importErrors" max-height="360" empty-text="暂无错误明细">
          <el-table-column label="源行" prop="sourceRowNo" width="82" align="right" />
          <el-table-column label="字段" prop="sourceColumnName" min-width="140" show-overflow-tooltip />
          <el-table-column label="原始值" prop="rawValue" min-width="180" show-overflow-tooltip />
          <el-table-column label="原因" prop="errorMessage" min-width="260" show-overflow-tooltip />
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup name="DataHubDatasetCreate">
import { confirmImport, getImportErrors, getImportJob, previewImport, updatePreviewSheet } from '@/api/datahub/dataset'

const { proxy } = getCurrentInstance()
const route = useRoute()
const uploadRef = ref(null)
const activeStep = ref(0)
const uploading = ref(false)
const uploadPercent = ref(0)
const uploadProgressStatus = ref('')
const uploadMessage = ref('')
const sheetLoading = ref(false)
const confirming = ref(false)
const job = ref({})
const errorLoading = ref(false)
const importErrors = ref([])
let errorsLoaded = false
let pollTimer
let confirmedSheetName = ''

const targetFolderId = computed(() => {
  const value = Number(route.query.folderId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})

const emptyPreview = () => ({
  previewId: undefined,
  fileName: '',
  sheetNames: [],
  sheetName: '',
  displayName: '',
  physicalName: '',
  columns: [],
  sampleRows: [],
  warnings: [],
  expiresAt: undefined
})

const preview = reactive(emptyPreview())

const dataTypeOptions = [
  { label: '文本', value: 'VARCHAR' },
  { label: '长文本', value: 'TEXT' },
  { label: '整数', value: 'BIGINT' },
  { label: '小数', value: 'DECIMAL' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '布尔值', value: 'BOOLEAN' }
]

const terminalStatus = computed(() => ['SUCCESS', 'FAILED', 'VALIDATION_FAILED', 'MANUAL_REQUIRED'].includes(job.value.status))
const reviewCount = computed(() => preview.columns.filter(column => column.needsReview).length)

const resultIcon = computed(() => {
  if (job.value.status === 'SUCCESS') return 'success'
  if (terminalStatus.value) return 'error'
  return 'info'
})

const resultTitle = computed(() => {
  if (job.value.status === 'SUCCESS') return '数据表创建成功'
  if (job.value.status === 'VALIDATION_FAILED') return '数据校验未通过'
  if (['FAILED', 'MANUAL_REQUIRED'].includes(job.value.status)) return '数据表创建失败'
  return '正在创建数据表'
})

const resultMessage = computed(() => {
  return job.value.errorMessage || job.value.message || (terminalStatus.value ? '' : '文件已提交，系统正在校验并写入数据')
})

const progressByStatus = {
  QUEUED: 10,
  VALIDATING: 30,
  STAGING: 65,
  COMMITTING: 90,
  RECOVERING: 90,
  SUCCESS: 100,
  VALIDATION_FAILED: 100,
  FAILED: 100,
  MANUAL_REQUIRED: 100
}

const jobProgress = computed(() => {
  const progress = Number(job.value.progressPercent ?? job.value.progress)
  if (Number.isFinite(progress)) return Math.max(0, Math.min(100, Math.round(progress)))
  return progressByStatus[job.value.status] || 5
})

const jobProgressStatus = computed(() => {
  if (job.value.status === 'SUCCESS') return 'success'
  if (terminalStatus.value) return 'exception'
  return ''
})

const statusLabels = {
  PARSING: '正在解析',
  PENDING_CONFIRM: '等待确认',
  QUEUED: '等待处理',
  STAGING: '正在写入暂存表',
  VALIDATING: '正在校验',
  COMMITTING: '正在发布',
  SUCCESS: '成功',
  VALIDATION_FAILED: '校验失败',
  FAILED: '失败',
  RECOVERING: '正在恢复',
  MANUAL_REQUIRED: '需要人工处理'
}

function jobStatusLabel(status) {
  return statusLabels[status] || status || '等待处理'
}

function normalizeResponseData(response) {
  return response?.data ?? response ?? {}
}

function applyPreview(data) {
  const next = data || {}
  Object.assign(preview, emptyPreview(), next)
  preview.sheetNames = Array.isArray(next.sheetNames) && next.sheetNames.length
    ? next.sheetNames.map(String)
    : [next.sheetName || '数据']
  preview.sheetName = next.sheetName || preview.sheetNames[0]
  confirmedSheetName = preview.sheetName
  preview.columns = (next.columns || []).map((column, index) => normalizeColumn(column, index))
  preview.sampleRows = Array.isArray(next.sampleRows) ? next.sampleRows : []
  preview.warnings = Array.isArray(next.warnings) ? next.warnings : next.warnings ? [next.warnings] : []
  activeStep.value = 1
}

function normalizeColumn(column, index) {
  const dataType = String(column.dataType || 'VARCHAR').toUpperCase()
  return {
    ...column,
    sourceIndex: column.sourceIndex ?? index,
    sourceName: column.sourceName || `第 ${index + 1} 列`,
    displayName: column.displayName || column.sourceName || `字段 ${index + 1}`,
    physicalName: column.physicalName || `column_${String(index + 1).padStart(3, '0')}`,
    dataType,
    length: dataType === 'VARCHAR' ? Number(column.length) || 255 : column.length,
    precision: dataType === 'DECIMAL' ? Number(column.precision) || 18 : column.precision,
    scale: dataType === 'DECIMAL' && column.scale != null ? Number(column.scale) : 2,
    nullable: column.nullable !== false,
    needsReview: Boolean(column.needsReview),
    samples: Array.isArray(column.samples) ? column.samples : []
  }
}

function handleFileChange(file) {
  if (!file?.raw || uploading.value) return
  const fileName = file.name || ''
  if (!/\.(xls|xlsx|csv)$/i.test(fileName)) {
    proxy.$modal.msgError('请选择 .xls、.xlsx 或 .csv 文件')
    uploadRef.value?.clearFiles()
    return
  }
  uploadFile(file.raw)
}

function handleFileRemove() {
  if (!uploading.value && activeStep.value === 0) resetUploadState()
}

function handleFileExceed() {
  proxy.$modal.msgWarning('每次只能选择一个文件')
}

async function uploadFile(file) {
  uploading.value = true
  uploadPercent.value = 0
  uploadProgressStatus.value = ''
  uploadMessage.value = '正在上传文件...'
  const formData = new FormData()
  formData.append('file', file)
  try {
    const response = await previewImport(formData, handleUploadProgress)
    uploadPercent.value = 100
    uploadProgressStatus.value = 'success'
    uploadMessage.value = '文件解析完成'
    applyPreview(normalizeResponseData(response))
  } catch (error) {
    uploadProgressStatus.value = 'exception'
    uploadMessage.value = error?.message || '文件解析失败'
  } finally {
    uploading.value = false
  }
}

function handleUploadProgress(event) {
  if (!event.total) return
  const percent = Math.round((event.loaded * 100) / event.total)
  uploadPercent.value = Math.min(percent, 95)
  if (percent >= 100) uploadMessage.value = '文件已上传，正在解析表头和样例数据...'
}

async function handleSheetChange(sheetName) {
  const previousSheet = confirmedSheetName
  sheetLoading.value = true
  try {
    const response = await updatePreviewSheet(preview.previewId, { sheetName })
    applyPreview(normalizeResponseData(response))
  } catch (error) {
    preview.sheetName = previousSheet
  } finally {
    sheetLoading.value = false
  }
}

function markReviewed(column) {
  column.needsReview = false
}

function handleTypeChange(column) {
  if (column.dataType === 'VARCHAR' && !column.length) column.length = 255
  if (column.dataType === 'DECIMAL') {
    if (!column.precision) column.precision = 18
    if (column.scale == null) column.scale = 2
  }
  column.needsReview = false
}

function formatSamples(samples) {
  if (!Array.isArray(samples) || !samples.length) return '-'
  return samples.filter(value => value !== null && value !== undefined && value !== '').slice(0, 3).join('、') || '-'
}

function sampleCell(row, column) {
  if (Array.isArray(row)) return formatCell(row[column.sourceIndex])
  if (!row || typeof row !== 'object') return formatCell(row)
  const keys = [column.physicalName, column.sourceName, String(column.sourceIndex)]
  for (const key of keys) {
    if (key != null && Object.prototype.hasOwnProperty.call(row, key)) return formatCell(row[key])
  }
  return '-'
}

function formatCell(value) {
  if (value === null || value === undefined || value === '') return '-'
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function columnMinWidth(column) {
  const label = column.displayName || column.sourceName || ''
  return Math.max(120, Math.min(240, label.length * 16 + 38))
}

function validateConfiguration() {
  const displayName = preview.displayName.trim()
  const physicalName = preview.physicalName.trim()
  if (!displayName) return '请输入数据表名称'
  if (displayName.length > 255) return '数据表名称不能超过 255 个字符'
  if (!/^[a-z][a-z0-9_]{0,63}$/.test(physicalName)) return '英文表名必须以小写字母开头，且只能包含小写字母、数字和下划线'
  if (!preview.columns.length) return '文件中没有可创建的字段'

  const physicalNames = new Set()
  for (let index = 0; index < preview.columns.length; index += 1) {
    const column = preview.columns[index]
    const rowNo = index + 1
    column.displayName = String(column.displayName || '').trim()
    column.physicalName = String(column.physicalName || '').trim()
    if (!column.displayName) return `第 ${rowNo} 个字段缺少显示名称`
    if (column.displayName.length > 255) return `字段“${column.displayName.slice(0, 20)}...”的显示名称不能超过 255 个字符`
    if (!/^[a-z][a-z0-9_]{0,63}$/.test(column.physicalName)) return `字段“${column.displayName}”的英文名格式不正确`
    if (physicalNames.has(column.physicalName)) return `英文字段名“${column.physicalName}”重复`
    physicalNames.add(column.physicalName)
    if (column.dataType === 'VARCHAR' && (!column.length || column.length < 1 || column.length > 1000)) return `字段“${column.displayName}”的文本长度应为 1 至 1000`
    if (column.dataType === 'DECIMAL' && (column.precision < 1 || column.precision > 38 || column.scale < 0 || column.scale > column.precision)) return `字段“${column.displayName}”的小数精度配置不正确`
  }
  preview.displayName = displayName
  preview.physicalName = physicalName
  return ''
}

function confirmPayload() {
  return {
    displayName: preview.displayName,
    physicalName: preview.physicalName,
    targetFolderId: targetFolderId.value,
    columns: preview.columns.map(column => ({
      sourceIndex: column.sourceIndex,
      sourceName: column.sourceName,
      displayName: column.displayName,
      physicalName: column.physicalName,
      dataType: column.dataType,
      length: column.dataType === 'VARCHAR' ? column.length : null,
      precision: column.dataType === 'DECIMAL' ? column.precision : null,
      scale: column.dataType === 'DECIMAL' ? column.scale : null,
      nullable: column.nullable,
      needsReview: false,
      translationSource: column.translationSource,
      samples: column.samples
    }))
  }
}

async function handleConfirm() {
  const validationMessage = validateConfiguration()
  if (validationMessage) return proxy.$modal.msgError(validationMessage)
  try {
    await proxy.$modal.confirm(`确认创建数据表“${preview.displayName}”吗？`)
  } catch (error) {
    return
  }

  confirming.value = true
  try {
    importErrors.value = []
    errorsLoaded = false
    const response = await confirmImport(preview.previewId, confirmPayload())
    job.value = { ...normalizeResponseData(response) }
    activeStep.value = 2
    startPolling()
  } finally {
    confirming.value = false
  }
}

function startPolling() {
  clearPolling()
  pollJob()
}

async function pollJob() {
  try {
    const response = await getImportJob(preview.previewId)
    job.value = { ...job.value, ...normalizeResponseData(response) }
    if (terminalStatus.value) {
      if (job.value.status === 'SUCCESS') activeStep.value = 3
      else await loadImportErrors().catch(() => {})
      return
    }
  } catch (error) {
    if (activeStep.value < 2) return
  }
  pollTimer = window.setTimeout(pollJob, 1500)
}

async function loadImportErrors() {
  if (errorsLoaded || !preview.previewId) return
  errorLoading.value = true
  try {
    const response = await getImportErrors(preview.previewId)
    const data = normalizeResponseData(response)
    importErrors.value = Array.isArray(data) ? data : []
    errorsLoaded = true
  } finally {
    errorLoading.value = false
  }
}

function clearPolling() {
  if (pollTimer) window.clearTimeout(pollTimer)
  pollTimer = undefined
}

function resetUploadState() {
  uploadPercent.value = 0
  uploadProgressStatus.value = ''
  uploadMessage.value = ''
}

function restart() {
  clearPolling()
  Object.assign(preview, emptyPreview())
  confirmedSheetName = ''
  job.value = {}
  importErrors.value = []
  errorsLoaded = false
  resetUploadState()
  activeStep.value = 0
  nextTick(() => uploadRef.value?.clearFiles())
}

function editConfiguration() {
  clearPolling()
  job.value = {}
  importErrors.value = []
  errorsLoaded = false
  activeStep.value = 1
}

function handleBack() {
  clearPolling()
  proxy.$tab.closeOpenPage({ path: '/datahub/dataset' })
}

function openDataset() {
  const title = preview.displayName || '数据表详情'
  proxy.$tab.closeOpenPage({ path: `/datahub/dataset-detail/index/${job.value.datasetId}`, meta: { title } })
}

function formatCount(value) {
  const count = Number(value)
  return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '0'
}

onBeforeUnmount(clearPolling)
</script>

<style scoped>
.datahub-create-page {
  min-width: 0;
}

.import-errors {
  width: min(1100px, 100%);
  margin: 0 auto 24px;
}

.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.page-heading {
  min-width: 0;
}

.page-heading h2,
.section-title-row h3 {
  margin: 0;
  letter-spacing: 0;
}

.page-heading h2 {
  font-size: 20px;
  line-height: 28px;
}

.page-heading span,
.section-title-row span {
  display: block;
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.create-steps {
  margin-bottom: 18px;
}

.upload-stage {
  max-width: 780px;
  margin: 50px auto 0;
}

.upload-stage :deep(.el-upload-dragger) {
  min-height: 240px;
  padding-top: 52px;
}

.upload-progress {
  margin-top: 20px;
}

.upload-progress > span {
  display: block;
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
}

.config-section {
  padding: 16px 0 20px;
  border-bottom: 1px solid #ebeef5;
}

.section-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
}

.section-title-row h3 {
  font-size: 15px;
  line-height: 22px;
}

.dataset-form {
  padding-top: 4px;
}

.warning-list {
  display: grid;
  gap: 8px;
}

.mono-input :deep(.el-input__inner) {
  font-family: Consolas, 'SFMono-Regular', monospace;
}

.decimal-fields {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 12px minmax(0, 1fr);
  align-items: center;
  gap: 4px;
}

.decimal-fields :deep(.el-input-number) {
  width: 100%;
}

.muted {
  color: #c0c4cc;
}

.sample-values {
  color: #606266;
  font-size: 12px;
  word-break: break-all;
}

.sample-section {
  border-bottom: 0;
}

.page-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 18px 0 4px;
}

.job-stage {
  max-width: 720px;
  margin: 36px auto 0;
}

.job-progress {
  width: min(600px, 75vw);
  margin-bottom: 22px;
}

.job-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px 20px;
  margin-top: 12px;
  color: #606266;
  font-size: 13px;
}

.result-actions {
  display: flex;
  justify-content: center;
  flex-wrap: wrap;
  gap: 8px;
}

@media (max-width: 768px) {
  .datahub-create-page {
    padding: 12px;
  }

  .upload-stage {
    margin-top: 24px;
  }

  .section-title-row {
    align-items: center;
  }

  .job-progress {
    width: 100%;
  }
}
</style>
