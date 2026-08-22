export const LIVE_TEMPLATE_FIELDS = [
  { key: 'anchor', label: '主播', type: 'text' },
  { key: 'controller', label: '场控', type: 'text' },
  { key: 'refundAmount', label: '到返金额', type: 'number' },
  { key: 'otherRemark', label: '其他备注', type: 'text' },
  { key: 'refundReason', label: '到返理由', type: 'text' },
  { key: 'afterSaleCompensation', label: '售后补偿', type: 'text' },
  { key: 'serviceMark', label: '服务标记', type: 'text' },
  { key: 'extendedWarranty', label: '是否延保', type: 'boolean' },
  { key: 'priceProtection', label: '是否价保', type: 'boolean' },
  { key: 'delayed', label: '是否延迟', type: 'boolean' },
  { key: 'followUp', label: '是否追单', type: 'boolean' },
  { key: 'urgent', label: '是否加急', type: 'boolean' }
]

const FIELD_BY_LABEL = new Map(LIVE_TEMPLATE_FIELDS.map(item => [item.label, item]))
const BOOLEAN_FIELD_BY_LABEL = new Map(
  LIVE_TEMPLATE_FIELDS.filter(item => item.type === 'boolean')
    .flatMap(item => [[item.label, item], [item.label.replace(/^是否/, ''), item]])
)
const oneLine = value => String(value ?? '').replace(/[\r\n]+/g, ' ').replace(/\s*\+\s*/g, '＋').trim()

export function booleanValue(value) {
  return value === true || value === 1 || value === '1' || value === 'true' || value === '是'
}

export function formatLiveTemplate(content, giftOptions = []) {
  const parts = (content?.fields || []).filter(item => item.enabled).flatMap(item => {
    const definition = LIVE_TEMPLATE_FIELDS.find(field => field.key === item.key) || item
    if (definition.type === 'boolean') return booleanValue(item.value) ? [`${definition.label}：是`] : []
    if (definition.type === 'number') {
      if (item.value === '' || item.value == null || !Number.isFinite(Number(item.value))) return []
      return [`${definition.label}：${Number(item.value)}`]
    }
    const value = oneLine(item.value)
    return value ? [`${definition.label}：${value}`] : []
  })
  ;(content?.gifts || []).filter(item => item.giftId).forEach(item => {
    const gift = giftOptions.find(option => option.giftId === item.giftId) || {}
    parts.push(`礼品：${oneLine(item.giftName || gift.giftName)}｜数量：${Number(item.quantity) || 1}｜编码：${oneLine(item.giftCode || gift.giftCode)}`)
  })
  return parts.join(' + ')
}

export function parseLiveTemplate(text) {
  const result = { version: 1, fields: {}, fieldOrder: [], gifts: [] }
  String(text || '').split(/\r?\n|\s*\+\s*/).map(part => part.trim()).filter(Boolean).forEach(part => {
    if (/^礼品\s*[：:]/.test(part)) {
      const match = part.match(/^礼品\s*[：:]\s*(.*?)\s*[｜|]\s*数量\s*[：:]\s*(\d+)(?:\s*[｜|]\s*编码\s*[：:]\s*(.*))?$/)
      if (!match || !match[1].trim()) throw new Error(`无法解析礼品：${part}`)
      const quantity = Number(match[2])
      if (!Number.isInteger(quantity) || quantity < 1 || quantity > 10) throw new Error(`礼品数量应为 1 到 10：${part}`)
      result.gifts.push({ giftName: match[1].trim(), quantity, giftCode: (match[3] || '').trim() })
      return
    }
    const separator = part.search(/[：:]/)
    const label = (separator < 0 ? part : part.slice(0, separator)).trim()
    const definition = FIELD_BY_LABEL.get(label) || BOOLEAN_FIELD_BY_LABEL.get(label)
    if (!definition) throw new Error(`未知模板字段：${label}`)
    const rawValue = separator < 0 ? '' : part.slice(separator + 1).trim()
    if (definition.type === 'boolean') {
      if (rawValue && !['是', '否'].includes(rawValue)) throw new Error(`${definition.label}只能是“是”或“否”`)
      result.fields[definition.key] = rawValue !== '否'
      if (rawValue !== '否' && !result.fieldOrder.includes(definition.key)) result.fieldOrder.push(definition.key)
      return
    }
    if (definition.type === 'number') {
      if (rawValue && !Number.isFinite(Number(rawValue))) throw new Error(`${definition.label}必须是数字`)
      result.fields[definition.key] = rawValue === '' ? null : Number(rawValue)
    } else result.fields[definition.key] = rawValue
    if (!result.fieldOrder.includes(definition.key)) result.fieldOrder.push(definition.key)
  })
  return result
}

export function assertLiveTemplateParsable(content, giftOptions = []) {
  const text = formatLiveTemplate(content, giftOptions)
  return { text, parsed: parseLiveTemplate(text) }
}
