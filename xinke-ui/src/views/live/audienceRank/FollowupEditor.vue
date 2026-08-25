<template>
  <div class="followup-editor" v-loading="loading">
    <div v-if="!modelValue?.followupId" class="editor-empty">
      <div class="empty-mark"></div>
      <strong>选择一位客户开始处理</strong>
      <span>客户资料、本次跟进和历史记录会显示在这里</span>
    </div>

    <template v-else>
      <header class="customer-head">
        <div class="customer-identity">
          <div class="identity-line">
            <h3>{{ modelValue.nicknameSnapshot || '未命名客户' }}</h3>
            <span :class="['stage-chip', stageTone]">{{ stageLabel }}</span>
            <span v-if="modelValue.priority" class="priority-label">重点</span>
          </div>
          <p>{{ modelValue.roomNameSnapshot || '未匹配直播间' }}<span>最近出现 {{ shortTime(modelValue.lastSeenAt) }}</span></p>
        </div>
        <div v-if="profileUrl" class="profile-actions">
          <el-tooltip content="打开抖音主页"><el-button :icon="LinkIcon" circle aria-label="打开抖音主页" @click="openProfile" /></el-tooltip>
          <el-tooltip content="复制用户标识"><el-button :icon="CopyDocument" circle aria-label="复制用户标识" @click="copyUid" /></el-tooltip>
        </div>
        <el-button v-if="modelValue.reactivationPending && canEdit" type="primary" plain :loading="saving" @click="$emit('reactivate')">重新激活</el-button>
      </header>

      <section class="customer-signals">
        <div><span>最近评论榜</span><strong>{{ rankLabel(modelValue.commentRank) }}</strong><small>最佳 {{ rankLabel(modelValue.bestCommentRank) }}</small></div>
        <div><span>最近观看榜</span><strong>{{ rankLabel(modelValue.watchRank) }}</strong><small>最佳 {{ rankLabel(modelValue.bestWatchRank) }}</small></div>
        <div><span>累计到访</span><strong>{{ integer(modelValue.appearanceDays) }} 天</strong><small>{{ consecutiveLabel }}</small></div>
        <div><span>客户关系</span><strong>{{ modelValue.isFollower ? '粉丝' : '非粉丝' }}</strong><small>{{ relationDetail }}</small></div>
      </section>

      <section v-if="modelValue.reactivationPending" class="reactivation-note"><strong>客户再次到访</strong><span>上一条商机已结束，重新激活后会保留历史订单和沟通记录。</span></section>

      <section v-if="contactReasons.length" class="contact-reasons" aria-label="优先联系原因">
        <strong>建议优先联系</strong>
        <div><span v-for="reason in contactReasons" :key="reason">{{ reason }}</span></div>
      </section>

      <section v-if="['OBSERVING', 'UNASSIGNED'].includes(modelValue.status)" class="claim-section">
        <div><strong>{{ modelValue.status === 'OBSERVING' ? '观察中的观众' : '待领取客户' }}</strong><span>{{ modelValue.status === 'OBSERVING' ? '当前未命中自动进客规则，可人工加入自己的跟进' : '领取后自动安排明天 10:00 跟进' }}</span></div>
        <el-button v-if="canClaim" type="primary" :loading="saving" @click="$emit('claim')">{{ modelValue.status === 'OBSERVING' ? '加入我的跟进' : '领取客户' }}</el-button>
      </section>

      <template v-else-if="mode !== 'view'">
        <section v-if="mode === 'edit' && canEdit" class="action-section">
          <div class="section-heading"><h4>本次沟通</h4><span>{{ modelValue.lastContactAt ? `上次 ${shortTime(modelValue.lastContactAt)}` : '首次联系' }}</span></div>

          <label class="field-label required">沟通结果</label>
          <div class="result-grid">
            <button v-for="item in resultOptions" :key="item.value" :class="[{ active: modelValue.followResultCode === item.value }, item.tone]" type="button" @click="chooseResult(item)">
              <component :is="item.icon" />
              <span>{{ item.label }}</span>
            </button>
          </div>
          <div v-if="resultHint" class="result-hint"><el-icon><Clock /></el-icon><span>{{ resultHint }}</span></div>

          <div v-if="showIntent" class="intent-row">
            <label>意向程度</label>
            <el-radio-group :model-value="modelValue.intentLevel || 'UNKNOWN'" @change="patch('intentLevel', $event)">
              <el-radio-button v-for="item in intentOptions" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button>
            </el-radio-group>
          </div>

          <div class="business-grid">
            <label><span>咨询型号</span><el-input :model-value="modelValue.consultModel" maxlength="256" clearable placeholder="例如：W3" @input="patch('consultModel', $event)" /></label>
            <label v-if="resultRequiresOrder"><span class="required">本次订单号</span><el-input :model-value="modelValue.orderNo" maxlength="64" clearable placeholder="粘贴平台订单号" @input="changeOrderNo" /></label>
          </div>

          <label class="field-label">本次沟通记录</label>
          <el-input :model-value="modelValue.lastFollowResult" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="记录客户需求、异议和下一步" @input="patch('lastFollowResult', $event)" />

          <template v-if="modelValue.followResultCode === 'INVALID'">
            <label class="field-label required">无效原因</label>
            <div class="reason-row">
              <el-select :model-value="modelValue.closeReasonCode" placeholder="选择原因" @change="patch('closeReasonCode', $event)">
                <el-option v-for="item in closeReasonOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <el-input :model-value="modelValue.closeReason" maxlength="500" clearable placeholder="补充说明（选填）" @input="patch('closeReason', $event)" />
            </div>
          </template>

          <template v-if="!terminal">
            <label class="field-label required">下次跟进</label>
            <div class="due-row">
              <el-date-picker :model-value="modelValue.nextFollowAt" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" clearable placeholder="选择时间" @update:model-value="patch('nextFollowAt', $event)" />
              <div class="due-shortcuts">
                <button type="button" @click="setDue('later')">2 小时后</button>
                <button type="button" @click="setDue('tomorrow')">明天</button>
                <button type="button" @click="setDue('threeDays')">3 天后</button>
                <button type="button" @click="setDue('week')">7 天后</button>
              </div>
            </div>
          </template>
        </section>

        <el-collapse class="detail-collapse">
          <el-collapse-item name="profile">
            <template #title><div class="collapse-title"><span>更多资料</span><small>{{ modelValue.ownerNameSnapshot || '待领取' }} · {{ modelValue.anchorNameSnapshot || '未分配主播' }}</small></div></template>
            <div class="collapse-body">
              <label class="field-label">长期备注</label>
              <el-input :model-value="modelValue.remark" type="textarea" :rows="2" maxlength="1000" placeholder="偏好、长期需求或交接信息" @input="patch('remark', $event)" />
              <div class="contact-grid">
                <label><span>联系电话</span><el-input :model-value="modelValue.contactPhone" maxlength="64" clearable placeholder="客户电话（选填）" @input="patch('contactPhone', $event)" /></label>
                <label><span>微信号</span><el-input :model-value="modelValue.contactWechat" maxlength="128" clearable placeholder="微信号（选填）" @input="patch('contactWechat', $event)" /></label>
              </div>
              <el-checkbox :model-value="Boolean(modelValue.priority)" class="priority-check" @change="patch('priority', $event)">设为重点客户</el-checkbox>

              <div v-if="canAssign" class="assignment-fields">
                <label><span>负责人（领取人）</span><el-select :model-value="modelValue.ownerUserId" filterable clearable placeholder="待领取" @change="patch('ownerUserId', $event)"><el-option v-for="person in ownerOptions" :key="personId(person)" :label="personLabel(person)" :value="personId(person)" /></el-select></label>
              </div>
              <div class="role-readonly"><span>来源直播间人员</span><strong>主播 {{ modelValue.anchorNameSnapshot || '未设置' }} · 场控 {{ modelValue.controllerNameSnapshot || '未设置' }}</strong><small>主播和场控由直播间资料自动带出</small></div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </template>

      <section v-else class="read-section">
        <dl>
          <dt>客户阶段</dt><dd>{{ stageLabel }}</dd><dt>最近结果</dt><dd>{{ resultLabel(modelValue.followResultCode) }}</dd>
          <dt>意向程度</dt><dd>{{ intentLabel(modelValue.intentLevel) }}</dd><dt>下次跟进</dt><dd>{{ modelValue.nextFollowAt || '未安排' }}</dd>
          <dt>咨询型号</dt><dd>{{ modelValue.consultModel || '未填写' }}</dd><dt>订单号</dt><dd>{{ modelValue.orderNo || '未填写' }}</dd>
          <dt>领取人</dt><dd>{{ modelValue.ownerNameSnapshot || '待领取' }}</dd><dt>主播 / 场控</dt><dd>{{ modelValue.anchorNameSnapshot || '-' }} / {{ modelValue.controllerNameSnapshot || '-' }}</dd>
          <dt>沟通记录</dt><dd class="wide">{{ modelValue.lastFollowResult || '暂无' }}</dd><dt>长期备注</dt><dd class="wide">{{ modelValue.remark || '暂无' }}</dd>
        </dl>
      </section>

      <el-collapse class="history-collapse">
        <el-collapse-item v-if="canViewHistory" name="timeline">
          <template #title><div class="collapse-title"><span>客户时间线</span><small>{{ timeline.length }} 条到访和沟通记录</small></div></template>
          <section class="history-section compact-history">
            <div v-if="timeline.length" class="timeline-list">
              <article v-for="item in timeline" :key="item.key" :class="item.type"><i></i><time>{{ item.time }}</time><div><strong>{{ item.title }}</strong><p>{{ item.content }}</p><small v-if="item.meta">{{ item.meta }}</small></div></article>
            </div>
            <el-empty v-else description="还没有到访或跟进记录" :image-size="48" />
          </section>
        </el-collapse-item>
        <el-collapse-item name="orders">
          <template #title><div class="collapse-title"><span>订单记录</span><small>{{ (modelValue.orders || []).length }} 笔订单</small></div></template>
          <section class="orders-section compact-orders">
            <div v-if="modelValue.orders?.length" class="order-list"><div v-for="order in modelValue.orders" :key="order.customerOrderId" class="order-item"><strong>{{ order.orderNo }}</strong><span>{{ order.productModel || '未填写型号' }}</span><small>{{ orderStatusLabel(order.orderStatus) }} · {{ shortTime(order.orderedAt) }}</small></div></div>
            <div v-else class="no-orders">还没有订单记录</div>
            <div v-if="mode === 'edit' && canEdit && modelValue.orders?.length" class="order-add"><el-input v-model="newOrderNo" clearable maxlength="64" placeholder="添加另一笔订单" @keyup.enter="submitOrder" /><el-button type="primary" plain :disabled="!newOrderNo.trim()" @click="submitOrder">添加订单</el-button></div>
          </section>
        </el-collapse-item>
      </el-collapse>

      <footer v-if="!['OBSERVING', 'UNASSIGNED'].includes(modelValue.status) && mode !== 'view'" class="editor-footer">
        <span :class="{ dirty }">{{ dirty ? '有未保存修改' : '内容已保存' }}</span>
        <div>
          <el-button :loading="saving" @click="$emit('save')">保存</el-button>
          <el-button v-if="mode === 'edit'" type="primary" :loading="saving" @click="$emit('save-next')">保存并下一位</el-button>
          <el-button v-else type="primary" :loading="saving" @click="$emit('save')">保存人员归属</el-button>
        </div>
      </footer>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ChatDotRound, CloseBold, CopyDocument, Finished, Link as LinkIcon, Phone, Star } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: Object, default: () => ({}) }, mode: { type: String, default: 'view' }, loading: Boolean,
  saving: Boolean, dirty: Boolean, canEdit: Boolean, canAssign: Boolean, canClaim: Boolean, canViewHistory: Boolean,
  logs: { type: Array, default: () => [] }, visits: { type: Array, default: () => [] }, ownerOptions: { type: Array, default: () => [] },
  anchorOptions: { type: Array, default: () => [] }, controllerOptions: { type: Array, default: () => [] }
})
const emit = defineEmits(['update:modelValue', 'save', 'save-next', 'claim', 'reactivate', 'save-order'])
const newOrderNo = ref('')
const resultOptions = [
  { value: 'NO_RESPONSE', label: '未联系上', status: 'PENDING', icon: Phone },
  { value: 'CONTACTED', label: '已沟通', status: 'CONTACTED', icon: ChatDotRound },
  { value: 'CONSIDERING', label: '有意向', status: 'QUALIFIED', icon: Star },
  { value: 'ORDERED', label: '已下单', status: 'ORDERED', icon: Finished, tone: 'success' },
  { value: 'INVALID', label: '无效客户', status: 'INVALID', icon: CloseBold, tone: 'danger' }
]
const intentOptions = [{ value: 'HIGH', label: '高' }, { value: 'MEDIUM', label: '中' }, { value: 'LOW', label: '低' }, { value: 'UNKNOWN', label: '未知' }]
const closeReasonOptions = [{ value: 'NO_NEED', label: '暂无需求' }, { value: 'PRICE', label: '价格原因' }, { value: 'NO_RESPONSE', label: '长期联系不上' }, { value: 'DUPLICATE', label: '重复客户' }, { value: 'OTHER', label: '其他' }]
const terminal = computed(() => ['CLOSED', 'INVALID', 'ORDERED'].includes(props.modelValue?.status))
const resultRequiresOrder = computed(() => props.modelValue?.followResultCode === 'ORDERED' || ['ORDERED', 'CLOSED'].includes(props.modelValue?.status))
const showIntent = computed(() => Boolean(props.modelValue?.followResultCode) && !['NO_RESPONSE', 'INVALID'].includes(props.modelValue?.followResultCode))
const profileUrl = computed(() => { const value = String(props.modelValue?.secUid || '').trim(); return value ? `https://www.douyin.com/user/${encodeURIComponent(value)}` : '' })
const stageLabel = computed(() => ({ OBSERVING: '观察中', UNASSIGNED: '待领取', FOLLOWING: '跟进中', DEAL_PENDING: '待成交', ORDERED: '已下单', ENDED: '已结束' })[stageCode(props.modelValue?.status)] || '跟进中')
const stageTone = computed(() => ({ OBSERVING: 'muted', UNASSIGNED: 'waiting', FOLLOWING: 'active', DEAL_PENDING: 'intent', ORDERED: 'success', ENDED: 'muted' })[stageCode(props.modelValue?.status)])
const relationDetail = computed(() => `${props.modelValue?.isFollowing ? '已回关' : '未回关'}${props.modelValue?.payLevel == null ? '' : ` · 消费 ${props.modelValue.payLevel} 级`}`)
const consecutiveLabel = computed(() => { const days = Number(props.modelValue?.consecutiveDays || 0), total = Number(props.modelValue?.appearanceDays || 0); return days > 1 ? `连续 ${days} 天` : (total > 1 ? '非连续到访' : '首次到访') })
const contactReasons = computed(() => {
  const row = props.modelValue || {}, reasons = []
  if (row.qualificationReason) reasons.push(row.qualificationReason)
  if (row.priority) reasons.push('人工重点客户')
  if (Number(row.consecutiveDays || 0) >= 2) reasons.push(`连续 ${integer(row.consecutiveDays)} 天到访`)
  else if (Number(row.appearanceDays || 0) >= 2) reasons.push(`累计到访 ${integer(row.appearanceDays)} 天`)
  if (Number(row.bestWatchRank || 0) > 0 && Number(row.bestWatchRank) <= 10) reasons.push(`观看榜 TOP ${row.bestWatchRank}`)
  if (Number(row.bestCommentRank || 0) > 0 && Number(row.bestCommentRank) <= 10) reasons.push(`评论榜 TOP ${row.bestCommentRank}`)
  if (Number(row.payLevel || 0) >= 10) reasons.push(`消费等级 ${row.payLevel}`)
  if (row.intentLevel === 'HIGH' && !terminal.value) reasons.push('高意向待推进')
  return [...new Set(reasons)].slice(0, 4)
})
const resultHint = computed(() => ({
  NO_RESPONSE: '已自动安排 2 小时后再次联系',
  CONTACTED: '请记录客户需求并安排下次联系',
  CONSIDERING: '默认安排 3 天后回访',
  ORDERED: '填写订单号后结束跟进任务',
  INVALID: '保存后将不再生成跟进任务'
})[props.modelValue?.followResultCode] || '')
const timeline = computed(() => {
  const visitItems = props.visits.map((item, index) => ({
    key: `visit-${item.batchId || index}-${item.visitDate}`,
    type: 'visit',
    time: item.visitDate,
    title: `进入${item.roomName || '未知直播间'}`,
    content: `评论 ${rankLabel(item.commentRank)} · 观看 ${rankLabel(item.watchRank)}`,
    meta: '榜单到访'
  }))
  const logItems = props.logs.map(log => ({
    key: `log-${log.logId}`,
    type: 'followup',
    time: log.createTime,
    title: actionLabel(log.actionType),
    content: log.content || resultLabel(log.result) || logSummary(log),
    meta: log.operatorNameSnapshot || '系统'
  }))
  return [...visitItems, ...logItems].sort((a, b) => String(b.time || '').localeCompare(String(a.time || '')))
})

function patch(key, value) { emit('update:modelValue', { ...props.modelValue, [key]: value }) }
function submitOrder() { const orderNo = newOrderNo.value.trim(); if (!orderNo) return; emit('save-order', { orderNo }); newOrderNo.value = '' }
function chooseResult(item) {
  const next = { ...props.modelValue, followResultCode: item.value, status: item.status }
  if (item.value === 'CONSIDERING' && (!next.intentLevel || next.intentLevel === 'UNKNOWN')) next.intentLevel = 'MEDIUM'
  if (item.value !== 'INVALID') { next.closeReasonCode = null; next.closeReason = '' }
  if (['ORDERED', 'INVALID'].includes(item.value)) next.nextFollowAt = null
  else if (!next.nextFollowAt) next.nextFollowAt = defaultDue(item.value)
  emit('update:modelValue', next)
}
function changeOrderNo(value) { const next = { ...props.modelValue, orderNo: value }; if (String(value || '').trim()) { next.followResultCode = 'ORDERED'; next.status = 'ORDERED'; next.nextFollowAt = null } emit('update:modelValue', next) }
function pad(value) { return String(value).padStart(2, '0') }
function dateTime(date) { return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:00` }
function defaultDue(result) { const date = new Date(); if (result === 'NO_RESPONSE') date.setHours(date.getHours() + 2, 0, 0, 0); else { date.setDate(date.getDate() + (result === 'CONSIDERING' ? 3 : result === 'PAUSED' ? 7 : 1)); date.setHours(10, 0, 0, 0) } return dateTime(date) }
function setDue(type) { const date = new Date(); if (type === 'later') { date.setHours(date.getHours() + 2, 0, 0, 0) } else { date.setDate(date.getDate() + (type === 'tomorrow' ? 1 : type === 'threeDays' ? 3 : 7)); date.setHours(10, 0, 0, 0) } patch('nextFollowAt', dateTime(date)) }
function stageCode(status) { if (status === 'OBSERVING') return 'OBSERVING'; if (status === 'UNASSIGNED') return 'UNASSIGNED'; if (['PENDING', 'CONTACTED'].includes(status)) return 'FOLLOWING'; if (['QUALIFIED', 'QUOTED', 'ORDER_PENDING', 'PAUSED'].includes(status)) return 'DEAL_PENDING'; if (status === 'ORDERED') return 'ORDERED'; return ['CLOSED', 'INVALID'].includes(status) ? 'ENDED' : 'FOLLOWING' }
function resultLabel(value) { return resultOptions.find(item => item.value === value)?.label || ({ QUOTED: '已报价', ORDER_PENDING: '待下单', PAUSED: '暂缓' })[value] || '' }
function orderStatusLabel(value) { return ({ ORDERED: '已下单', COMPLETED: '已完成', CANCELLED: '已取消', REFUNDED: '已退款' })[value] || '订单' }
function intentLabel(value) { return intentOptions.find(item => item.value === value)?.label || '未知' }
function integer(value) { return Number(value || 0).toLocaleString('zh-CN') }
function rankLabel(value) { return value == null ? '-' : `第 ${value} 名` }
function shortTime(value) { return value ? String(value).replace(/^\d{4}-/, '') : '-' }
function personId(person) { return Number(person.userId ?? person.user_id ?? person.staffId ?? person.id) }
function personLabel(person) { const name = person.userName || person.user_name || person.nickName || person.name || person.account || '未命名账号'; const account = person.account || person.mobile || ''; return account && account !== name ? `${name}（${account}）` : name }
function actionLabel(action) { return ({ CREATE: '自动建档', ASSIGN: '领取客户', AUTO_ASSIGN: '系统智能分配', RECLAIM: '超时回收', CONTACT: '完成一次沟通', STATUS: '阶段变更', UPDATE: '资料更新' })[action] || '客户资料更新' }
function logSummary(log) { return log.statusAfter && log.statusAfter !== log.statusBefore ? `${log.statusBefore || '-'} → ${log.statusAfter}` : '资料已更新' }
function openProfile() { if (profileUrl.value) window.open(profileUrl.value, '_blank', 'noopener,noreferrer') }
async function copyUid() { const value = String(props.modelValue?.secUid || '').trim(); if (!value) return; try { await navigator.clipboard.writeText(value) } catch { const input = document.createElement('textarea'); input.value = value; input.style.position = 'fixed'; input.style.opacity = '0'; document.body.append(input); input.select(); document.execCommand('copy'); input.remove() } }
</script>

<style scoped lang="scss">
.followup-editor { min-height: 100%; color: #172033; background: #fff; }
.editor-empty { display: grid; min-height: 500px; place-content: center; justify-items: center; color: #7d8795; text-align: center; }.empty-mark { width: 38px; height: 38px; margin-bottom: 13px; border: 1px solid #d8dde4; border-radius: 50%; box-shadow: inset 0 0 0 9px #f5f7f9; }.editor-empty strong { color: #344054; font-size: 14px; }.editor-empty span { margin-top: 5px; font-size: 11px; }
.customer-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; padding: 1px 0 14px; border-bottom: 1px solid #e6e9ee; }.customer-identity { min-width: 0; }.identity-line { display: flex; min-width: 0; align-items: center; gap: 6px; }.identity-line h3 { overflow: hidden; margin: 0; font-size: 18px; font-weight: 650; letter-spacing: 0; text-overflow: ellipsis; white-space: nowrap; }.customer-identity p { display: flex; flex-wrap: wrap; gap: 8px; margin: 6px 0 0; color: #737d8c; font-size: 11px; }.customer-identity p span::before { margin-right: 8px; color: #c4cad2; content: '·'; }
.stage-chip, .priority-label { flex: none; padding: 2px 6px; border-radius: 3px; font-size: 10px; }.stage-chip.waiting { color: #8a5a12; background: #fff6df; }.stage-chip.active { color: #245f9f; background: #edf5fd; }.stage-chip.intent { color: #7b4e05; background: #fff2d2; }.stage-chip.success { color: #16764a; background: #eaf8f1; }.stage-chip.muted { color: #697386; background: #f0f2f5; }.priority-label { color: #8a5200; border: 1px solid #ebc273; background: #fff9ec; }
.profile-actions { display: flex; flex: none; gap: 5px; }.profile-actions :deep(.el-button) { width: 31px; height: 31px; margin: 0; border-radius: 4px; }
.reactivation-note { display: flex; align-items: center; gap: 9px; margin: -5px 0 16px; padding: 10px 12px; border: 1px solid #f0d4a8; border-radius: 5px; color: #8a5a12; background: #fffaf0; font-size: 11px; }.reactivation-note strong { flex: none; font-size: 12px; }.reactivation-note span { color: #876d42; }
.customer-signals { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); margin: 14px 0 18px; border: 1px solid #e6e9ee; border-radius: 5px; background: #f8fafc; }.customer-signals > div { min-width: 0; padding: 10px; border-right: 1px solid #e6e9ee; }.customer-signals > div:last-child { border-right: 0; }.customer-signals span, .customer-signals small, .customer-signals strong { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }.customer-signals span { color: #7c8695; font-size: 10px; }.customer-signals strong { margin: 5px 0 3px; font-size: 13px; }.customer-signals small { color: #8992a0; font-size: 10px; }
.claim-section { display: flex; align-items: center; justify-content: space-between; gap: 16px; padding: 19px 2px; border-bottom: 1px solid #e6e9ee; }.claim-section div { display: flex; flex-direction: column; gap: 5px; }.claim-section strong { font-size: 14px; }.claim-section span { color: #727d8d; font-size: 11px; }
.action-section { padding-bottom: 17px; }.section-heading { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 10px; }.section-heading h4 { margin: 0; font-size: 14px; letter-spacing: 0; }.section-heading span { color: #8992a0; font-size: 10px; }.field-label, .business-grid label > span, .assignment-fields label > span { display: block; margin: 12px 0 5px; color: #596575; font-size: 11px; }.required::after { margin-left: 2px; color: #b42318; content: '*'; }
.result-grid { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 6px; }.result-grid button { display: flex; min-width: 0; height: 40px; align-items: center; justify-content: center; gap: 5px; padding: 0 5px; border: 1px solid #dce1e7; border-radius: 4px; color: #536174; background: #fff; cursor: pointer; }.result-grid button svg { width: 14px; height: 14px; flex: none; }.result-grid button:hover { border-color: #9db7d3; background: #f8fbfe; }.result-grid button.active { color: #1f5f9e; border-color: #76a2ce; background: #edf5fd; box-shadow: inset 0 0 0 1px #a9c7e4; }.result-grid button.success.active { color: #16764a; border-color: #74bd96; background: #eff9f4; box-shadow: inset 0 0 0 1px #a9d9bf; }.result-grid button.danger.active { color: #a33029; border-color: #d99590; background: #fff4f3; box-shadow: inset 0 0 0 1px #efb9b5; }
.intent-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-top: 12px; }.intent-row > label { color: #596575; font-size: 11px; }.intent-row :deep(.el-radio-button__inner) { min-width: 52px; padding: 8px 12px; }
.business-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }.reason-row { display: grid; grid-template-columns: 145px minmax(0, 1fr); gap: 8px; }.due-row { display: grid; grid-template-columns: minmax(190px, 1fr) auto; gap: 8px; }.due-row :deep(.el-date-editor) { width: 100%; }.due-shortcuts { display: flex; align-items: center; gap: 4px; }.due-shortcuts button { min-height: 32px; padding: 0 8px; border: 1px solid #dce1e7; border-radius: 4px; color: #596575; background: #fff; cursor: pointer; }.due-shortcuts button:hover { color: #245f9f; border-color: #9db7d3; background: #f7fafe; }
.contact-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }.contact-grid label > span { display: block; margin: 12px 0 5px; color: #596575; font-size: 11px; }
.detail-collapse { border-top: 1px solid #e6e9ee; border-bottom: 0; }.detail-collapse :deep(.el-collapse-item__header) { height: 47px; border-bottom-color: #e6e9ee; }.collapse-title { display: flex; min-width: 0; flex: 1; align-items: center; justify-content: space-between; gap: 10px; padding-right: 8px; }.collapse-title span { font-size: 13px; font-weight: 600; }.collapse-title small { overflow: hidden; color: #8992a0; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.collapse-body { padding: 0 1px 12px; }.priority-check { margin-top: 10px; }.assignment-fields { display: grid; grid-template-columns: minmax(220px, 1fr); gap: 8px; }.assignment-fields :deep(.el-select) { width: 100%; }.role-readonly { display: flex; flex-direction: column; gap: 4px; margin-top: 13px; padding-top: 11px; border-top: 1px solid #edf0f3; }.role-readonly span, .role-readonly small { color: #8992a0; font-size: 10px; }.role-readonly strong { font-size: 12px; font-weight: 600; }
.read-section { padding: 4px 0 16px; }.read-section dl { display: grid; grid-template-columns: 80px minmax(0, 1fr) 80px minmax(0, 1fr); margin: 0; border-top: 1px solid #e6e9ee; }.read-section dt, .read-section dd { margin: 0; padding: 9px 5px; border-bottom: 1px solid #e9ecf0; font-size: 11px; }.read-section dt { color: #7a8493; }.read-section dd { overflow-wrap: anywhere; }.read-section dd.wide { grid-column: span 3; }
.history-collapse { margin-top: 4px; border-top: 0; }.history-collapse :deep(.el-collapse-item__header) { height: 48px; }.history-section { padding: 16px 0 5px; border-top: 1px solid #e6e9ee; }.compact-history,.compact-orders { padding-top: 0; border-top: 0; }.timeline-list { margin-top: 7px; }.timeline-list article { position: relative; display: grid; min-height: 56px; grid-template-columns: 9px 84px minmax(0, 1fr); gap: 10px; padding: 9px 0; }.timeline-list article::before { position: absolute; top: 20px; bottom: -11px; left: 4px; width: 1px; background: #dfe4e9; content: ''; }.timeline-list article:last-child::before { display: none; }.timeline-list article > i { position: relative; z-index: 1; width: 9px; height: 9px; margin-top: 3px; border: 2px solid #668fb8; border-radius: 50%; background: #fff; }.timeline-list article.visit > i { border-color: #d18427; }.timeline-list time { color: #7b8492; font-size: 10px; }.timeline-list strong { font-size: 11px; }.timeline-list p { margin: 3px 0; color: #536174; font-size: 11px; line-height: 1.5; }.timeline-list small { color: #929aa7; font-size: 10px; }
.orders-section { padding: 16px 0 5px; border-top: 1px solid #e6e9ee; }.order-list { display: grid; gap: 6px; }.order-item { display: grid; grid-template-columns: 1fr 1fr auto; gap: 8px; align-items: center; padding: 9px 10px; border: 1px solid #e6e9ee; border-radius: 4px; background: #fafbfc; }.order-item strong { font-size: 12px; }.order-item span, .order-item small { overflow: hidden; color: #667085; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.order-item small { text-align: right; }.no-orders { padding: 10px; color: #98a2b3; background: #fafbfc; font-size: 11px; }
.order-add { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 7px; margin-top: 8px; }
.editor-footer { position: sticky; z-index: 3; bottom: -15px; display: flex; min-height: 61px; align-items: center; justify-content: space-between; gap: 12px; margin: 7px -15px -15px; padding: 10px 15px; border-top: 1px solid #dfe4e9; background: rgb(255 255 255 / 96%); box-shadow: 0 -6px 14px rgb(16 24 40 / 4%); }.editor-footer > span { color: #98a2b3; font-size: 10px; }.editor-footer > span.dirty { color: #9a620f; }.editor-footer > div { display: flex; gap: 7px; }.editor-footer :deep(.el-button) { margin: 0; }
@media (max-width: 620px) { .customer-signals { grid-template-columns: repeat(2, minmax(0, 1fr)); }.customer-signals > div:nth-child(2) { border-right: 0; }.customer-signals > div:nth-child(-n+2) { border-bottom: 1px solid #e6e9ee; }.result-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }.business-grid, .contact-grid, .reason-row, .assignment-fields, .due-row { grid-template-columns: 1fr; }.intent-row { align-items: flex-start; flex-direction: column; }.intent-row :deep(.el-radio-group) { display: grid; width: 100%; grid-template-columns: repeat(4, minmax(0, 1fr)); }.intent-row :deep(.el-radio-button__inner) { width: 100%; min-width: 0; }.due-shortcuts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); }.due-shortcuts button { padding: 0 3px; }.read-section dl { grid-template-columns: 75px minmax(0, 1fr); }.read-section dd.wide { grid-column: auto; }.timeline-list article { grid-template-columns: 9px minmax(0, 1fr); }.timeline-list time { grid-column: 2; }.timeline-list article > div { grid-column: 2; }.order-item { grid-template-columns: 1fr; }.order-item small { text-align: left; }.editor-footer { align-items: stretch; flex-direction: column; }.editor-footer > div { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>

<style scoped lang="scss">
.followup-editor { color: #202631; }
.customer-head { padding-bottom: 15px; }
.identity-line h3 { font-size: 19px; }
.customer-identity p { font-size: 12px; }
.stage-chip, .priority-label { padding: 3px 7px; font-size: 11px; }
.customer-signals { margin: 14px 0 20px; }
.customer-signals > div { padding: 11px 12px; }
.customer-signals span, .customer-signals small { font-size: 11px; }
.customer-signals strong { font-size: 14px; }
.contact-reasons { display: flex; align-items: center; gap: 10px; margin: -7px 0 18px; padding: 9px 11px; border-left: 3px solid #ed6a2c; color: #5f482f; background: #fff8f3; }
.contact-reasons > strong { flex: none; font-size: 12px; }
.contact-reasons > div { display: flex; min-width: 0; flex-wrap: wrap; gap: 5px; }
.contact-reasons span { padding: 2px 6px; border: 1px solid #f0d2bd; border-radius: 3px; color: #8a4b23; background: #fff; font-size: 10px; }
.section-heading h4 { font-size: 15px; }
.section-heading span { color: #7c8694; font-size: 11px; }
.field-label, .business-grid label > span, .assignment-fields label > span { font-size: 12px; }
.result-grid button { min-height: 48px; font-size: 12px; }
.result-grid button.active { color: #b64d1f; border-color: #ed6a2c; background: #fff5ef; }
.result-grid button.success.active { color: #16764a; border-color: #58ae82; background: #eef9f3; }
.result-grid button.danger.active { color: #b42318; border-color: #e58b84; background: #fff2f1; }
.result-hint { display: flex; align-items: center; gap: 6px; min-height: 32px; margin-top: 7px; padding: 0 9px; color: #536174; background: #f5f7f9; font-size: 11px; }
.due-shortcuts button { min-height: 32px; }
.editor-footer :deep(.el-button--primary) { border-color: #ed6a2c; background: #ed6a2c; }
.editor-footer :deep(.el-button--primary:hover) { border-color: #dc5c20; background: #dc5c20; }
</style>
