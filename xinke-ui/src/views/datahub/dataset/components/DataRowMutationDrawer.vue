<template>
  <el-drawer
    v-model="visible"
    :title="mode === 'INSERT' ? '新增数据行' : '编辑数据行'"
    :size="drawerSize"
    append-to-body
    :before-close="beforeClose"
  >
    <div class="row-editor-meta">
      <span>{{ mode === 'INSERT' ? '新增行将在批量提交后写入' : `行 ID：${row?._id ?? row?.rowId ?? '-'}` }}</span>
      <span v-if="mode !== 'INSERT'">源行号：{{ row?._source_row_no ?? '-' }}</span>
    </div>

    <el-alert
      v-if="formError"
      :title="formError"
      type="error"
      :closable="false"
      show-icon
      class="form-alert"
    />

    <el-form label-position="top" class="row-editor-form">
      <div class="field-grid">
        <el-form-item
          v-for="column in columns"
          :key="column.columnId"
          :label="column.displayName"
          :error="fieldErrors[columnKey(column)]"
          :class="{ 'field-wide': column.dataType === 'TEXT' }"
        >
          <template #label>
            <div class="field-label">
              <span>{{ column.displayName }}</span>
              <small>{{ columnTypeText(column) }} · {{ column.physicalName }}</small>
            </div>
          </template>

          <div class="field-control">
            <el-radio-group
              v-if="column.dataType === 'BOOLEAN'"
              :model-value="booleanChoice(column)"
              @update:model-value="value => setBooleanChoice(column, value)"
            >
              <el-radio-button value="TRUE">是</el-radio-button>
              <el-radio-button value="FALSE">否</el-radio-button>
              <el-radio-button v-if="columnNullable(column)" value="NULL">NULL</el-radio-button>
            </el-radio-group>

            <el-date-picker
              v-else-if="column.dataType === 'DATE'"
              v-model="fieldState[columnKey(column)].value"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              :disabled="fieldState[columnKey(column)].isNull"
              style="width: 100%"
            />

            <el-date-picker
              v-else-if="column.dataType === 'DATETIME'"
              v-model="fieldState[columnKey(column)].value"
              type="datetime"
              value-format="YYYY-MM-DD HH:mm:ss"
              placeholder="选择日期时间"
              :disabled="fieldState[columnKey(column)].isNull"
              style="width: 100%"
            />

            <el-input
              v-else-if="column.dataType === 'TEXT'"
              v-model="fieldState[columnKey(column)].value"
              type="textarea"
              :rows="4"
              resize="vertical"
              placeholder="输入文本；留空表示空字符串"
              :disabled="fieldState[columnKey(column)].isNull"
            />

            <el-input
              v-else
              v-model="fieldState[columnKey(column)].value"
              :placeholder="inputPlaceholder(column)"
              :disabled="fieldState[columnKey(column)].isNull"
              clearable
            />

            <el-checkbox
              v-if="column.dataType !== 'BOOLEAN' && columnNullable(column)"
              v-model="fieldState[columnKey(column)].isNull"
              class="null-checkbox"
            >设为 NULL</el-checkbox>
          </div>
        </el-form-item>
      </div>
    </el-form>

    <template #footer>
      <div class="drawer-footer">
        <span>此处保存只会加入待提交清单</span>
        <div>
          <el-button @click="requestClose">取消</el-button>
          <el-button type="primary" icon="Check" @click="saveDraft">加入待提交清单</el-button>
        </div>
      </div>
    </template>
  </el-drawer>
</template>

<script setup>
const props = defineProps({
  modelValue: { type: Boolean, default: false },
  mode: { type: String, default: 'UPDATE' },
  columns: { type: Array, default: () => [] },
  row: { type: Object, default: () => ({}) }
})

const emit = defineEmits(['update:modelValue', 'save'])
const { proxy } = getCurrentInstance()
const fieldState = reactive({})
const fieldErrors = reactive({})
const formError = ref('')
const baseline = ref('')
let forceClosing = false

const visible = computed({
  get: () => props.modelValue,
  set: value => emit('update:modelValue', value)
})

const drawerSize = computed(() => window.innerWidth <= 768 ? '100%' : '680px')

watch(() => props.modelValue, value => {
  if (value) initialize()
})

function columnKey(column) {
  return String(column.columnId)
}

function columnNullable(column) {
  return column.nullable !== false && column.isNullable !== false
}

function initialValue(column) {
  const raw = props.row?.[column.physicalName]
  if (raw === null || raw === undefined) {
    return { value: '', isNull: columnNullable(column) }
  }
  if (column.dataType === 'BOOLEAN') {
    const bool = raw === true || raw === 1 || raw === '1' || String(raw).toLowerCase() === 'true'
    return { value: bool, isNull: false }
  }
  return { value: String(raw), isNull: false }
}

function initialize() {
  for (const key of Object.keys(fieldState)) delete fieldState[key]
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  for (const column of props.columns) fieldState[columnKey(column)] = initialValue(column)
  formError.value = ''
  baseline.value = stateSignature()
  forceClosing = false
}

function booleanChoice(column) {
  const state = fieldState[columnKey(column)]
  if (!state || state.isNull) return 'NULL'
  if (state.value === true) return 'TRUE'
  if (state.value === false) return 'FALSE'
  return ''
}

function setBooleanChoice(column, choice) {
  const state = fieldState[columnKey(column)]
  state.isNull = choice === 'NULL'
  state.value = choice === 'TRUE' ? true : choice === 'FALSE' ? false : ''
}

function inputPlaceholder(column) {
  if (column.dataType === 'BIGINT') return '输入整数；按字符串保留完整精度'
  if (column.dataType === 'DECIMAL') return '输入小数；按字符串保留完整精度'
  return '输入文本；留空表示空字符串'
}

function columnTypeText(column) {
  if (column.dataType === 'VARCHAR') return `VARCHAR(${column.columnLength || column.length || 255})`
  if (column.dataType === 'DECIMAL') return `DECIMAL(${column.numericPrecision || column.precision || 18},${column.numericScale ?? column.scale ?? 0})`
  return column.dataType || '-'
}

function stateSignature() {
  return JSON.stringify(props.columns.map(column => {
    const state = fieldState[columnKey(column)] || {}
    return [column.columnId, Boolean(state.isNull), state.value]
  }))
}

function validateDecimal(value, column) {
  if (!/^[+-]?(?:\d+\.?\d*|\d*\.\d+)$/.test(value)) return '请输入合法小数'
  const precision = Number(column.numericPrecision || column.precision || 18)
  const scale = Number(column.numericScale ?? column.scale ?? 0)
  const unsigned = value.replace(/^[+-]/, '')
  const [integerPart = '', decimalPart = ''] = unsigned.split('.')
  const integerDigits = integerPart.replace(/^0+/, '').length
  if (decimalPart.length > scale) return `小数位不能超过 ${scale} 位`
  if (integerDigits > precision - scale) return `整数位不能超过 ${precision - scale} 位`
  return ''
}

function validateFields() {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  formError.value = ''
  let firstError = ''
  for (const column of props.columns) {
    const key = columnKey(column)
    const state = fieldState[key]
    let error = ''
    if (state.isNull) {
      if (!columnNullable(column)) error = '该字段不允许为 NULL'
    } else if (column.dataType === 'BOOLEAN') {
      if (state.value !== true && state.value !== false) error = '请选择是或否'
    } else {
      const value = state.value == null ? '' : String(state.value)
      if (column.dataType === 'BIGINT' && !/^[+-]?\d+$/.test(value)) error = '请输入合法整数'
      if (column.dataType === 'DECIMAL') error = validateDecimal(value, column)
      if (['DATE', 'DATETIME'].includes(column.dataType) && !value) error = '请选择日期'
      const maxLength = Number(column.columnLength || column.length)
      if (!error && column.dataType === 'VARCHAR' && maxLength > 0 && value.length > maxLength) {
        error = `文本不能超过 ${maxLength} 个字符`
      }
    }
    if (error) {
      fieldErrors[key] = error
      if (!firstError) firstError = `${column.displayName}：${error}`
    }
  }
  formError.value = firstError
  return !firstError
}

function mutationValues() {
  return props.columns.filter(column => {
    if (props.mode === 'INSERT') return true
    const current = fieldState[columnKey(column)]
    const initial = initialValue(column)
    if (Boolean(current.isNull) !== Boolean(initial.isNull)) return true
    if (current.isNull) return false
    return current.value !== initial.value
  }).map(column => {
    const state = fieldState[columnKey(column)]
    let value = null
    if (!state.isNull) {
      value = column.dataType === 'BOOLEAN' ? state.value : String(state.value ?? '')
    }
    return {
      columnId: column.columnId,
      value,
      isNull: Boolean(state.isNull)
    }
  })
}

function saveDraft() {
  if (!validateFields()) return
  const values = mutationValues()
  if (props.mode !== 'INSERT' && values.length === 0) {
    formError.value = '没有需要保存的字段变更'
    return
  }
  forceClosing = true
  emit('save', {
    operation: props.mode,
    values
  })
  visible.value = false
}

function requestClose() {
  beforeClose(() => { visible.value = false })
}

function beforeClose(done) {
  if (forceClosing || stateSignature() === baseline.value) return done()
  proxy.$modal.confirm('当前填写内容尚未加入待提交清单，确认关闭吗？').then(() => {
    forceClosing = true
    done()
  }).catch(() => {})
}
</script>

<style scoped>
.row-editor-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px 18px;
  margin-bottom: 14px;
  color: #606266;
  font-size: 13px;
}

.form-alert {
  margin-bottom: 14px;
}

.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 18px;
}

.field-wide {
  grid-column: 1 / -1;
}

.field-label span,
.field-label small {
  display: block;
  letter-spacing: 0;
}

.field-label small {
  color: #a8abb2;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 10px;
  line-height: 16px;
}

.field-control {
  width: 100%;
}

.null-checkbox {
  display: block;
  height: 24px;
  margin-top: 3px;
}

.drawer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.drawer-footer > span {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 768px) {
  .field-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .field-wide {
    grid-column: auto;
  }

  .drawer-footer {
    align-items: flex-end;
    flex-direction: column;
  }
}
</style>
