<template>
  <section class="assignment-rules">
    <header>
      <div><h4>进客与分配规则</h4><span>先判断哪些观众值得跟进，再决定是否自动分给领取人</span></div>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增规则</el-button>
    </header>

    <div v-loading="loading" class="rule-list">
      <article v-for="rule in rules" :key="rule.roomId">
        <div class="rule-room"><strong>{{ rule.roomName || `直播间 ${rule.roomId}` }}</strong><span :class="{ enabled: rule.enabled }"><i></i>{{ rule.enabled ? '自动分配中' : '手动领取' }}</span></div>
        <div class="rule-qualification"><span>进入待办</span><strong>{{ qualificationSummary(rule) }}</strong></div>
        <div class="rule-members"><span>领取人</span><strong>{{ memberNames(rule) }}</strong></div>
        <div class="rule-policy"><span>每人最多 {{ rule.maxActivePerOwner }} 位进行中客户</span><span>{{ rule.reclaimHours }} 小时未联系自动回收</span></div>
        <div class="rule-actions"><el-button link @click="runRule(rule)">立即分配</el-button><el-button link type="primary" @click="openEdit(rule)">编辑</el-button></div>
      </article>
      <el-empty v-if="!loading && !rules.length" description="还没有智能分配规则" :image-size="56" />
    </div>

    <el-dialog v-model="visible" title="进客与分配规则" width="640px" append-to-body destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="直播间" required><el-select v-model="form.roomId" filterable :disabled="Boolean(form.ruleId)" placeholder="选择直播间"><el-option v-for="room in availableRooms" :key="room.roomId" :label="roomLabel(room)" :value="Number(room.roomId)" /></el-select></el-form-item>
        <div class="form-section"><div class="form-section-head"><div><strong>哪些观众进入待办</strong><small>下面任意一项命中即可，填写 0 表示不使用该条件</small></div><el-switch v-model="form.qualificationEnabled" /></div>
          <div v-if="form.qualificationEnabled" class="qualification-grid">
            <el-form-item label="评论榜前 N 名"><el-input-number v-model="form.commentRankThreshold" :min="0" :max="500" controls-position="right" /></el-form-item>
            <el-form-item label="观看榜前 N 名"><el-input-number v-model="form.watchRankThreshold" :min="0" :max="500" controls-position="right" /></el-form-item>
            <el-form-item label="最低消费等级"><el-input-number v-model="form.minPayLevel" :min="0" :max="500" controls-position="right" /></el-form-item>
            <el-form-item label="累计到访天数"><el-input-number v-model="form.minVisitDays" :min="0" :max="500" controls-position="right" /></el-form-item>
          </div>
          <div v-if="form.qualificationEnabled" class="relation-switches"><el-checkbox v-model="form.followerQualifies">粉丝直接进入待办</el-checkbox><el-checkbox v-model="form.followingQualifies">已回关直接进入待办</el-checkbox></div>
        </div>
        <div class="form-section"><div class="form-section-head"><div><strong>如何分配</strong><small>关闭自动分配时，符合条件的客户进入待领取池</small></div><el-switch v-model="form.enabled" /></div>
        <el-form-item v-if="form.enabled" label="参与分配的领取人" required><el-select v-model="form.memberUserIds" multiple filterable collapse-tags :max-collapse-tags="3" placeholder="可选择多个账号"><el-option v-for="person in ownerOptions" :key="personId(person)" :label="personLabel(person)" :value="personId(person)" /></el-select></el-form-item>
        <div class="policy-grid">
          <el-form-item label="每人最大进行中客户"><el-input-number v-model="form.maxActivePerOwner" :min="1" :max="10000" controls-position="right" /></el-form-item>
          <el-form-item label="未联系自动回收"><el-input-number v-model="form.reclaimHours" :min="1" :max="720" controls-position="right" /><small>小时</small></el-form-item>
        </div>
        </div>
      </el-form>
      <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存规则</el-button></template>
    </el-dialog>
  </section>
</template>

<script setup>
import { computed, getCurrentInstance, onMounted, ref } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { autoAssignAudienceFollowups, listAudienceAssignmentRules, saveAudienceAssignmentRule } from '@/api/live/audienceRank'

const props = defineProps({ roomOptions: { type: Array, default: () => [] }, ownerOptions: { type: Array, default: () => [] } })
const emit = defineEmits(['changed'])
const { proxy } = getCurrentInstance()
const loading = ref(false), saving = ref(false), visible = ref(false), rules = ref([])
const blank = () => ({ ruleId: null, roomId: null, enabled: false, memberUserIds: [], maxActivePerOwner: 100, reclaimHours: 24, qualificationEnabled: true, commentRankThreshold: 30, watchRankThreshold: 30, minPayLevel: 10, minVisitDays: 2, followerQualifies: false, followingQualifies: false })
const form = ref(blank())
const availableRooms = computed(() => props.roomOptions.filter(room => form.value.roomId === Number(room.roomId) || !rules.value.some(rule => Number(rule.roomId) === Number(room.roomId))))

onMounted(load)
async function load() { loading.value = true; try { rules.value = (await listAudienceAssignmentRules())?.data || [] } finally { loading.value = false } }
function openCreate() { form.value = blank(); visible.value = true }
function openEdit(rule) { form.value = { ...blank(), ...rule, memberUserIds: [...(rule.memberUserIds || [])].map(Number) }; visible.value = true }
async function save() {
  if (!form.value.roomId) return proxy.$modal.msgWarning('请选择直播间')
  if (form.value.enabled && !form.value.memberUserIds.length) return proxy.$modal.msgWarning('请至少选择一位领取人')
  saving.value = true
  try { await saveAudienceAssignmentRule(form.value); proxy.$modal.msgSuccess('进客与分配规则已保存'); visible.value = false; await load(); emit('changed') } finally { saving.value = false }
}
async function runRule(rule) { const result = (await autoAssignAudienceFollowups(rule.roomId))?.data || {}; proxy.$modal.msgSuccess(`已分配 ${result.assignedCount || 0} 位，回收 ${result.reclaimedCount || 0} 位`); emit('changed') }
function roomLabel(room) { return room.roomName || room.liveAccount || room.roomCode || `直播间 ${room.roomId}` }
function personId(person) { return Number(person.userId ?? person.user_id ?? person.id) }
function personLabel(person) { const name = person.userName || person.nickName || person.name || person.account || '未命名账号'; const account = person.account || person.mobile || ''; return account && account !== name ? `${name}（${account}）` : name }
function memberNames(rule) { const names = (rule.members || []).map(personLabel); return names.length ? names.join('、') : '未选择领取人' }
function qualificationSummary(rule) { if (rule.qualificationEnabled === false) return '仅人工加入'; const items = []; if (Number(rule.commentRankThreshold) > 0) items.push(`评论前 ${rule.commentRankThreshold}`); if (Number(rule.watchRankThreshold) > 0) items.push(`观看前 ${rule.watchRankThreshold}`); if (Number(rule.minPayLevel) > 0) items.push(`消费 ${rule.minPayLevel} 级`); if (Number(rule.minVisitDays) > 0) items.push(`到访 ${rule.minVisitDays} 天`); if (rule.followerQualifies) items.push('粉丝'); if (rule.followingQualifies) items.push('已回关'); return items.length ? items.join(' / ') : '仅人工加入' }
defineExpose({ load })
</script>

<style scoped lang="scss">
.assignment-rules { margin-top: 24px; border-top: 1px solid #e4e7ec; }
.assignment-rules > header { display: flex; min-height: 70px; align-items: center; justify-content: space-between; gap: 18px; }
h4 { margin: 0; font-size: 15px; letter-spacing: 0; } header span { display: block; margin-top: 5px; color: #7b8492; font-size: 12px; }
.rule-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.rule-list article { min-width: 0; padding: 15px; border: 1px solid #e2e6eb; border-radius: 5px; background: #fff; }
.rule-room { display: flex; align-items: center; justify-content: space-between; gap: 8px; }.rule-room > strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.rule-room span { display: inline-flex; flex: none; align-items: center; gap: 5px; color: #8a5c14; font-size: 11px; }.rule-room span i { width: 6px; height: 6px; border-radius: 50%; background: #d49324; }.rule-room span.enabled { color: #18764b; }.rule-room span.enabled i { background: #2b9b65; }
.rule-qualification, .rule-members { margin-top: 13px; }.rule-qualification span, .rule-members span, .rule-policy span { color: #7b8492; font-size: 11px; }.rule-qualification strong, .rule-members strong { display: block; margin-top: 4px; font-size: 12px; line-height: 18px; }.rule-members strong { min-height: 36px; }
.rule-policy { display: flex; flex-direction: column; gap: 3px; padding-top: 10px; border-top: 1px solid #edf0f3; }.rule-actions { display: flex; justify-content: flex-end; margin-top: 8px; }
.form-section { margin-top: 14px; padding-top: 14px; border-top: 1px solid #e7eaee; }.form-section-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; margin-bottom: 12px; }.form-section-head strong,.form-section-head small { display: block; }.form-section-head small { margin-top: 4px; color: #7b8492; font-size: 11px; }.qualification-grid,.policy-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 12px; }.qualification-grid :deep(.el-input-number),.policy-grid :deep(.el-input-number), :deep(.el-select) { width: 100%; }.policy-grid small { margin-left: 7px; color: #7b8492; }.relation-switches { display: flex; flex-wrap: wrap; gap: 18px; margin-bottom: 6px; }
@media (max-width: 1000px) { .rule-list { grid-template-columns: 1fr 1fr; } }
@media (max-width: 620px) { .assignment-rules > header { align-items: flex-start; flex-direction: column; padding: 14px 0; }.rule-list, .policy-grid { grid-template-columns: 1fr; } }
</style>
