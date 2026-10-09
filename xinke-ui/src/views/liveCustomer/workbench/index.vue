<template>
  <div class="customer-workbench">
    <section class="workspace-bar">
      <div class="workspace-title"><span class="title-mark"><el-icon><Monitor /></el-icon></span><span>直播客户工作台</span><el-tag size="small" type="success" effect="plain">V2</el-tag></div>
      <div class="workspace-meta">{{ todayLabel }}</div>
    </section>

    <el-tabs v-model="active" class="workbench-tabs" @tab-change="loadActive">
      <el-tab-pane name="realtime"><template #label><el-icon><ChatDotRound /></el-icon>实时互动</template>
        <div class="metric-row"><div class="metric"><span>本场互动</span><b>{{ summary.interactions }}</b></div><div class="metric"><span>待登记</span><b class="warning">{{ summary.unregistered }}</b></div><div class="metric"><span>已加粉</span><b>{{ summary.followed }}</b></div><div class="metric"><span>咨询未下单</span><b>{{ summary.consulting }}</b></div></div>
        <div class="toolbar"><el-select v-model="selectedRoomId" clearable filterable placeholder="当前直播间" @change="handleRoomChange"><el-option v-for="item in rooms" :key="item.roomId" :label="item.roomName + (item.sessionId ? '' : ' · 今日未开播')" :value="item.roomId" /></el-select><el-input v-model="query.keyword" clearable placeholder="搜索昵称或评论" :prefix-icon="Search" @keyup.enter="loadActive" /><el-select v-model="query.stage" clearable placeholder="客户阶段" @change="loadActive"><el-option label="未咨询" value="NOT_CONSULTED"/><el-option label="咨询未下单" value="CONSULTING"/><el-option label="高意向待推进" value="HIGH_INTENT"/><el-option label="已下单" value="ORDERED"/></el-select><el-button :icon="Refresh" @click="loadActive">刷新</el-button><el-button type="primary" @click="openRegistration({})">新增客户</el-button></div>
        <el-alert v-if="loadError" :title="loadError" type="error" show-icon :closable="false" class="load-alert"><template #default><el-button link type="danger" @click="loadActive">重新加载</el-button></template></el-alert>
        <div class="quick-hint"><el-icon><InfoFilled /></el-icon><span>先处理有评论的客户；咨询未下单客户建议安排明日跟进，连续 3 天未成交会进入释放池。</span></div>
        <el-table v-loading="loading" :data="rows" stripe class="data-table" empty-text="暂无互动数据"><el-table-column label="客户" min-width="180"><template #default="{row}"><div class="customer-cell"><strong>{{ row.nickname || '未命名客户' }}</strong><el-tag v-if="row.sourceType" size="small" effect="plain" :type="sourceType(row.sourceType)">{{ sourceLabel(row.sourceType) }}</el-tag></div><div class="muted">{{ row.ownerName || '待领取' }}</div></template></el-table-column><el-table-column label="最近评论" min-width="280" prop="lastComment" show-overflow-tooltip/><el-table-column label="互动" width="90"><template #default="{row}">{{ row.interactionCount || 0 }} 次</template></el-table-column><el-table-column label="加粉" width="90"><template #default="{row}"><span :class="row.fanStatus === 'MUTUAL' ? 'text-success' : ''">{{ fanLabel(row.fanStatus) }}</span></template></el-table-column><el-table-column label="客户阶段" width="125"><template #default="{row}"><el-tag size="small" :type="stageType(row.stage)">{{ stageLabel(row.stage) }}</el-tag></template></el-table-column><el-table-column label="最近互动" width="170" prop="lastInteractionAt"/><el-table-column label="操作" fixed="right" width="220"><template #default="{row}"><el-button link type="primary" @click="openRegistration(row)">快速登记</el-button><el-button link type="warning" @click="quickConsult(row)">咨询未下单</el-button><el-button link @click="showCustomer(row)">详情</el-button></template></el-table-column></el-table>
        <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadActive" />
      </el-tab-pane>

      <el-tab-pane name="tasks"><template #label><el-icon><List /></el-icon>今日待跟进</template>
        <div class="task-summary"><div v-for="item in taskCards" :key="item.key" class="task-card" :class="item.tone"><span>{{ item.label }}</span><b>{{ item.value }}</b></div></div>
        <div class="quick-hint"><el-icon><InfoFilled /></el-icon><span>连续 3 天未成交，或场控没有特殊标记，系统会创建电话客服任务；原主播、场控和跟进记录仍会保留。</span><el-button size="small" type="primary" plain :loading="handoffRunning" @click="runHandoff">立即检查转交</el-button></div>
        <el-table v-loading="loading" :data="tasks" stripe class="data-table" empty-text="今天没有待跟进任务"><el-table-column label="客户" min-width="180" prop="nickname"/><el-table-column label="任务类型" width="120"><template #default="{row}"><el-tag size="small" :type="row.taskType === 'PHONE_SERVICE' ? 'danger' : 'info'">{{ row.taskType === 'PHONE_SERVICE' ? '电话客服' : '直播跟进' }}</el-tag></template></el-table-column><el-table-column label="转交原因" min-width="150" show-overflow-tooltip><template #default="{row}">{{ row.handoffReason === 'THREE_DAYS_NO_ORDER' ? '三天未成交' : row.handoffReason === 'NO_CONTROLLER_MARK' ? '场控未标记' : '—' }}</template></el-table-column><el-table-column label="任务日期" width="120" prop="taskDate"/><el-table-column label="跟进状态" width="120"><template #default="{row}"><el-tag size="small" :type="taskType(row.status)">{{ taskLabel(row.status) }}</el-tag></template></el-table-column><el-table-column label="咨询型号" min-width="150" prop="consultModel" show-overflow-tooltip/><el-table-column label="负责人" width="110"><template #default="{row}">{{ row.taskType === 'PHONE_SERVICE' ? (row.assigneeName || '待领取') : (row.ownerName || '待领取') }}</template></el-table-column><el-table-column label="操作" fixed="right" width="250"><template #default="{row}"><el-button link type="primary" @click="claimTask(row)">{{ row.taskType === 'PHONE_SERVICE' ? '领取电话任务' : '领取' }}</el-button><el-button link type="success" @click="completeTask(row)">完成</el-button><el-button link type="warning" @click="nextTask(row)">下次跟进</el-button><el-button link type="info" @click="releaseTask(row)">释放</el-button></template></el-table-column></el-table>
        <pagination v-show="taskTotal > 0" :total="taskTotal" v-model:page="taskQuery.pageNum" v-model:limit="taskQuery.pageSize" @pagination="loadActive" />
      </el-tab-pane>

      <el-tab-pane name="customers"><template #label><el-icon><User /></el-icon>客户库</template>
        <div class="toolbar"><el-input v-model="customerQuery.keyword" clearable placeholder="按昵称筛选" :prefix-icon="Search" @keyup.enter="loadActive" /><el-select v-model="customerQuery.stage" clearable placeholder="客户阶段" @change="loadActive"><el-option label="未咨询" value="NOT_CONSULTED"/><el-option label="咨询未下单" value="CONSULTING"/><el-option label="已下单" value="ORDERED"/></el-select><el-button :icon="Refresh" @click="loadActive">刷新</el-button></div>
        <el-table v-loading="loading" :data="customers" stripe class="data-table" empty-text="暂无客户"><el-table-column label="客户" min-width="190"><template #default="{row}"><strong>{{ row.nickname }}</strong><div class="muted">{{ sourceLabel(row.sourceType) }}</div></template></el-table-column><el-table-column label="公屏互动" width="100" prop="commentCount"/><el-table-column label="加粉关系" width="110"><template #default="{row}">{{ fanLabel(row.fanStatus) }}</template></el-table-column><el-table-column label="咨询阶段" width="130"><template #default="{row}"><el-tag size="small" :type="stageType(row.stage)">{{ stageLabel(row.stage) }}</el-tag></template></el-table-column><el-table-column label="领取人" width="120" prop="ownerName"/><el-table-column label="最近互动" width="170" prop="lastInteractionAt"/><el-table-column label="操作" fixed="right" width="150"><template #default="{row}"><el-button link type="primary" @click="openRegistration(row)">补充登记</el-button><el-button link @click="showCustomer(row)">详情</el-button></template></el-table-column></el-table>
        <pagination v-show="customerTotal > 0" :total="customerTotal" v-model:page="customerQuery.pageNum" v-model:limit="customerQuery.pageSize" @pagination="loadActive" />
      </el-tab-pane>

      <el-tab-pane name="orders"><template #label><el-icon><Goods /></el-icon>订单跟踪</template>
        <div class="order-flow"><span>未下单</span><i>›</i><span>已下单待补号</span><i>›</i><span>已录入待同步</span><i>›</i><span>处理中</span><i>›</i><span>已完成</span></div>
        <el-table v-loading="loading" :data="orders" stripe class="data-table" empty-text="暂无订单记录"><el-table-column label="客户" min-width="180" prop="nickname"/><el-table-column label="订单号" min-width="170" prop="orderNo"/><el-table-column label="型号" min-width="150" prop="productModel"/><el-table-column label="订单状态" width="150"><template #default="{row}"><el-tag size="small" :type="orderType(row.orderStatus)">{{ orderLabel(row.orderStatus) }}</el-tag></template></el-table-column><el-table-column label="金额" width="110" prop="payAmount"/><el-table-column label="更新时间" width="170" prop="updatedAt"/><el-table-column label="操作" fixed="right" width="90"><template #default="{row}"><el-button link type="primary" @click="editOrder(row)">更新</el-button></template></el-table-column></el-table>
        <div v-if="!orders.length && !loading" class="empty-hint"><el-icon><Goods /></el-icon><span>登记客户时填写订单号，订单会在这里持续跟踪</span></div>
      </el-tab-pane>

      <el-tab-pane name="uploads"><template #label><el-icon><Upload /></el-icon>上传记录</template>
        <el-table v-loading="loading" :data="batches" stripe class="data-table" empty-text="暂无上传记录"><el-table-column label="批次" prop="batch_id" min-width="220"/><el-table-column label="直播间" prop="room_name" min-width="180"/><el-table-column label="直播日期" prop="live_date" width="110"/><el-table-column label="接收" prop="accepted_count" width="80"/><el-table-column label="新增客户" prop="new_customer_count" width="90"/><el-table-column label="更新客户" prop="updated_customer_count" width="90"/><el-table-column label="重复" prop="duplicate_count" width="80"/><el-table-column label="失败" prop="rejected_count" width="80"/><el-table-column label="状态" width="110"><template #default="{row}"><el-tag size="small" :type="row.status === 'ACCEPTED' ? 'success' : row.status === 'SHADOW' ? 'warning' : 'info'">{{ row.status === 'ACCEPTED' ? '已入库' : row.status === 'SHADOW' ? '影子记录' : row.status || '处理中' }}</el-tag></template></el-table-column><el-table-column label="操作" fixed="right" width="80"><template #default="{row}"><el-button link @click="showBatch(row)">明细</el-button></template></el-table-column><el-table-column label="上传时间" width="170" prop="created_at"/></el-table>
        <pagination v-show="batchTotal > 0" :total="batchTotal" v-model:page="batchQuery.pageNum" v-model:limit="batchQuery.pageSize" @pagination="loadActive" />
      </el-tab-pane>
    </el-tabs>

    <el-drawer v-model="drawerVisible" title="快速登记" size="460px" :destroy-on-close="true"><div class="drawer-tip"><el-icon><InfoFilled /></el-icon><span>只需补充本次互动，不会重复创建客户</span></div><el-form ref="formRef" :model="form" :rules="rules" label-position="top"><el-form-item label="客户昵称" prop="nickname"><el-input v-model="form.nickname" :disabled="!!form.customerId" placeholder="输入客户昵称" /></el-form-item><el-form-item label="当前直播间"><el-select v-model="form.sessionId" clearable filterable placeholder="可不选" style="width:100%"><el-option v-for="item in rooms" :key="item.sessionId || item.roomId" :label="item.roomName" :value="item.sessionId || item.roomId" /></el-select></el-form-item><el-form-item label="领取人"><el-select v-model="form.ownerUserId" filterable clearable style="width:100%"><el-option v-for="item in users" :key="item.userId" :label="item.userName + ' · ' + item.account" :value="item.userId" /></el-select></el-form-item><el-form-item label="主播"><el-select v-model="form.anchorUserId" filterable clearable style="width:100%"><el-option v-for="item in staffFor('anchor')" :key="item.userId" :label="item.userName" :value="item.userId" /></el-select></el-form-item><el-form-item label="场控"><el-select v-model="form.controllerUserId" filterable clearable style="width:100%"><el-option v-for="item in staffFor('controller')" :key="item.userId" :label="item.userName" :value="item.userId" /></el-select></el-form-item><el-form-item label="场控特殊标记"><el-input v-model="form.specialMark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="有特殊情况、继续跟进原因等再填写；留空会在规则扫描时转交电话客服" /></el-form-item><el-form-item label="互动类型" prop="interactionType"><el-select v-model="form.interactionType" placeholder="请选择"><el-option label="公屏评论" value="COMMENT"/><el-option label="表达购买意向" value="BUY_INTENT"/><el-option label="咨询产品" value="CONSULT"/></el-select></el-form-item><el-form-item label="加粉关系"><el-select v-model="form.addFanStatus" clearable placeholder="请选择"><el-option label="未关注" value="UNFOLLOWED"/><el-option label="已关注" value="FOLLOWED"/><el-option label="已互关" value="MUTUAL"/></el-select></el-form-item><el-form-item label="咨询阶段" prop="stage"><el-select v-model="form.stage" placeholder="请选择"><el-option label="未咨询" value="NOT_CONSULTED"/><el-option label="咨询未下单" value="CONSULTING"/><el-option label="高意向待推进" value="HIGH_INTENT"/><el-option label="已下单" value="ORDERED"/></el-select></el-form-item><el-form-item label="咨询型号"><el-input v-model="form.consultModel" placeholder="选填" /></el-form-item><el-form-item label="顾虑 / 备注"><el-input v-model="form.note" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="选填" /></el-form-item><el-form-item label="订单号"><el-input v-model="form.orderNo" placeholder="已下单可先填，没有可留空" /></el-form-item><el-form-item label="截图凭证"><el-upload v-model:file-list="attachmentFiles" :auto-upload="false" :limit="5" accept="image/jpeg,image/png,image/webp" list-type="text"><el-button :icon="Picture">选择截图</el-button><template #tip><div class="upload-tip">支持 JPG、PNG、WEBP，单张不超过 10MB；上传失败不影响登记</div></template></el-upload></el-form-item></el-form><template #footer><div class="drawer-footer"><el-button @click="drawerVisible=false">取消</el-button><el-button :loading="saving" @click="submitRegistration(true)">保存并下一位</el-button><el-button type="primary" :loading="saving" @click="submitRegistration(false)">保存并关闭</el-button></div></template></el-drawer>
    <el-dialog v-model="detailVisible" title="客户详情" width="760px"><div v-loading="detailLoading" class="detail"><template v-if="detail"><div class="detail-head"><strong>{{ detail.nickname }}</strong><el-tag>{{ stageLabel(detail.stage) }}</el-tag><span class="muted">领取人：{{ detail.ownerName || '待领取' }}</span></div><el-descriptions :column="3" border><el-descriptions-item label="互动">{{ detail.interactions?.length || 0 }} 条</el-descriptions-item><el-descriptions-item label="身份">{{ detail.identities?.length || 0 }} 个</el-descriptions-item><el-descriptions-item label="订单">{{ detail.orders?.length || 0 }} 个</el-descriptions-item></el-descriptions><div v-if="detail.attachments?.length" class="attachment-list"><div class="attachment-title">截图凭证</div><div v-for="item in detail.attachments" :key="item.id" class="attachment-row"><span>{{ item.originalName }}</span><el-button link type="primary" @click="openAttachment(item)">查看</el-button></div></div><div v-for="item in (detail.opportunities || []).filter(item => item.handoffReason)" :key="item.id" class="handoff-note"><el-tag type="danger" size="small">已转电话客服</el-tag><span>{{ item.handoffReason === 'THREE_DAYS_NO_ORDER' ? '三天未成交' : '场控未标记' }} · {{ item.handoffAt || '' }}</span><span>原跟进：{{ item.ownerName || '未分配' }} / 主播：{{ item.anchorName || '未设置' }} / 场控：{{ item.controllerName || '未设置' }}</span></div><el-divider content-position="left">最近跟进</el-divider><el-timeline><el-timeline-item v-for="item in (detail.registrations || []).slice(0,8)" :key="item.id" :timestamp="item.createdAt">{{ item.registeredName }}：{{ item.note || item.consultModel || '已登记' }}<span v-if="item.specialMark" class="muted"> · 特殊标记：{{ item.specialMark }}</span></el-timeline-item></el-timeline></template></div></el-dialog>
    <el-dialog v-model="batchVisible" title="上传批次明细" width="860px"><el-table :data="batchDetail?.events || []" max-height="500" empty-text="该批次没有事件明细"><el-table-column prop="eventType" label="类型" width="100"/><el-table-column prop="nickname" label="昵称" width="160"/><el-table-column prop="secUid" label="平台身份" min-width="180" show-overflow-tooltip/><el-table-column prop="content" label="评论内容" min-width="240" show-overflow-tooltip/><el-table-column prop="occurredAt" label="发生时间" width="170"/></el-table></el-dialog>
    <el-dialog v-model="orderVisible" title="更新订单" width="430px"><el-form label-position="top"><el-form-item label="订单号"><el-input v-model="orderForm.orderNo" /></el-form-item><el-form-item label="型号"><el-input v-model="orderForm.productModel" /></el-form-item><el-form-item label="金额"><el-input-number v-model="orderForm.payAmount" :min="0" :precision="2" /></el-form-item><el-form-item label="状态"><el-select v-model="orderForm.orderStatus" style="width:100%"><el-option v-for="item in orderStatuses" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item></el-form><template #footer><el-button @click="orderVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveOrder">保存</el-button></template></el-dialog>
    <el-dialog v-model="previewVisible" title="查看截图" width="720px" destroy-on-close><div class="attachment-preview"><img :src="previewUrl" alt="客户截图" /></div></el-dialog>
  </div>
</template>

<script setup name="LiveCustomerWorkbench">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ChatDotRound, Goods, InfoFilled, List, Monitor, Picture, Refresh, Search, Upload, User } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { actionLiveTask, ensureLiveSession, getLiveAttachmentUrl, getLiveCustomer, getLiveOptions, getLiveUploadBatch, getLiveWorkbenchSummary, listLiveCustomers, listLiveOrders, listLiveTasks, listLiveUploadBatches, runLivePhoneHandoff, saveLiveRegistration, updateLiveOrder, uploadLiveAttachment } from '@/api/liveCustomer'

const active = ref('realtime'); const loading = ref(false); const loadError = ref(''); const saving = ref(false); const handoffRunning = ref(false); const drawerVisible = ref(false); const formRef = ref(); const attachmentFiles = ref([]); const previewVisible = ref(false); const previewUrl = ref('')
const rows = ref([]); const customers = ref([]); const tasks = ref([]); const orders = ref([]); const batches = ref([])
const rooms = ref([]); const users = ref([]); const selectedRoomId = ref(null); const selectedSession = ref(null); const detailVisible = ref(false); const detailLoading = ref(false); const detail = ref(null); const batchVisible = ref(false); const batchDetail = ref(null); const orderVisible = ref(false); const orderForm = reactive({ id: null, orderNo: '', productModel: '', payAmount: null, orderStatus: 'ORDERED_PENDING_NO' })
const total = ref(0); const customerTotal = ref(0); const taskTotal = ref(0); const batchTotal = ref(0)
const query = reactive({ keyword: '', stage: '', pageNum: 1, pageSize: 20 }); const customerQuery = reactive({ keyword: '', stage: '', pageNum: 1, pageSize: 20 }); const taskQuery = reactive({ pageNum: 1, pageSize: 20 }); const batchQuery = reactive({ pageNum: 1, pageSize: 20 })
const form = reactive({ customerId: null, nickname: '', secUid: '', sessionId: null, ownerUserId: null, anchorUserId: null, controllerUserId: null, interactionType: 'COMMENT', addFanStatus: '', stage: 'NOT_CONSULTED', consultModel: '', concern: '', specialMark: '', note: '', orderNo: '' })
const rules = { nickname: [{ required: true, message: '请输入客户昵称', trigger: 'blur' }], interactionType: [{ required: true, message: '请选择互动类型', trigger: 'change' }], stage: [{ required: true, message: '请选择咨询阶段', trigger: 'change' }] }
const orderStatuses = [{ value: 'ORDERED_PENDING_NO', label: '已下单待补号' }, { value: 'ENTERED_PENDING_SYNC', label: '已录入待同步' }, { value: 'PROCESSING', label: '支付 / 处理中' }, { value: 'SHIPPED', label: '已发货' }, { value: 'COMPLETED', label: '已完成' }, { value: 'REFUNDED', label: '已退款' }, { value: 'CLOSED', label: '已关闭' }]
const concernOptions = ['价格问题', '产品顾虑', '留言未回', '信息未读', '售后顾虑', '考虑中', '已拍待补订单号']
const sessionRooms = computed(() => rooms.value.filter(item => item.sessionId))
const staffFor = role => { const room = rooms.value.find(item => Number(item.sessionId || item.roomId) === Number(form.sessionId)); const ids = String(role === 'anchor' ? room?.anchorIds || '' : room?.controllerIds || '').split(',').filter(Boolean).map(Number); return users.value.filter(item => ids.includes(Number(item.userId))) }
const todayLabel = computed(() => new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', weekday: 'short' }))
const summary = reactive({ interactions: 0, unregistered: 0, followed: 0, consulting: 0 })
const taskCards = computed(() => [{ key: 'overdue', label: '已逾期', value: tasks.value.filter(r => r.status === 'OVERDUE').length, tone: 'danger' }, { key: 'today', label: '今日待处理', value: tasks.value.filter(r => ['PENDING', 'TODAY'].includes(r.status) && r.taskType !== 'PHONE_SERVICE').length, tone: 'primary' }, { key: 'phone', label: '待转电话客服', value: tasks.value.filter(r => r.taskType === 'PHONE_SERVICE').length, tone: 'danger' }, { key: 'consulting', label: '咨询未下单', value: tasks.value.filter(r => r.stage === 'CONSULTING').length, tone: 'warning' }])
const stageLabel = value => ({ NOT_CONSULTED: '未咨询', UNCONTACTED: '未咨询', CONSULTING: '咨询未下单', HIGH_INTENT: '高意向', ORDERED: '已下单' }[value] || '未咨询')
const stageType = value => value === 'ORDERED' ? 'success' : value === 'CONSULTING' || value === 'HIGH_INTENT' ? 'warning' : 'info'
const fanLabel = value => ({ UNKNOWN: '未知', UNFOLLOWED: '未关注', FOLLOWED: '已关注', MUTUAL: '已互关' }[value] || '未知')
const sourceLabel = value => ({ PLUGIN: '插件发现', MANUAL: '人工登记', PLUGIN_MANUAL: '插件发现', PLUGIN_REALTIME: '插件发现' }[value] || '待确认')
const sourceType = value => value === 'MANUAL' ? 'warning' : 'info'
const taskLabel = value => ({ PENDING: '待处理', TODAY: '今日待跟进', OVERDUE: '已逾期', IN_PROGRESS: '跟进中', DONE: '已完成' }[value] || '待处理')
const taskType = value => value === 'OVERDUE' ? 'danger' : value === 'DONE' ? 'success' : 'warning'
const orderLabel = value => ({ ORDERED_PENDING_NO: '已下单待补号', ENTERED_PENDING_SYNC: '已录入待同步', PROCESSING: '支付 / 处理中', SHIPPED: '已发货', COMPLETED: '已完成', REFUNDED: '已退款', CLOSED: '已关闭' }[value] || '未下单')
const orderType = value => value === 'COMPLETED' ? 'success' : value === 'REFUNDED' || value === 'CLOSED' ? 'info' : 'warning'
async function loadActive () { loading.value = true; loadError.value = ''; try { if (active.value === 'uploads') await loadBatches(); else if (active.value === 'tasks') await loadTasks(); else if (active.value === 'customers') await loadCustomers(); else if (active.value === 'orders') await loadOrders(); else await loadRealtime() } catch (error) { loadError.value = error?.message || '数据加载失败，请检查服务后重试' } finally { loading.value = false } }
async function loadRealtime () { const params = { ...query, sessionId: selectedSession.value || undefined }; const result = await listLiveCustomers(params); rows.value = result.rows || []; total.value = result.total || 0; try { const summaryResult = await getLiveWorkbenchSummary({ sessionId: selectedSession.value || undefined }); Object.assign(summary, summaryResult.data || {}) } catch (e) { const list = rows.value; Object.assign(summary, { interactions: list.reduce((n, r) => n + Number(r.interactionCount || 0), 0), unregistered: list.filter(r => !r.sourceType || r.sourceType === 'PLUGIN').length, followed: list.filter(r => ['FOLLOWED', 'MUTUAL'].includes(r.fanStatus)).length, consulting: list.filter(r => r.stage === 'CONSULTING').length }) } }
async function loadCustomers () { const result = await listLiveCustomers(customerQuery); customers.value = result.rows || []; customerTotal.value = result.total || 0 }
async function loadTasks () { const result = await listLiveTasks(taskQuery); tasks.value = result.rows || []; taskTotal.value = result.total || 0 }
async function runHandoff () { handoffRunning.value = true; try { const result = await runLivePhoneHandoff(); ElMessage.success(`已检查转交规则，新增 ${result.data?.created || 0} 条电话客服任务`); await loadTasks() } finally { handoffRunning.value = false } }
async function loadOrders () { const result = await listLiveOrders({ pageNum: 1, pageSize: 100 }); orders.value = result.rows || [] }
async function loadBatches () { const result = await listLiveUploadBatches(batchQuery); batches.value = result.rows || []; batchTotal.value = result.total || 0 }
async function handleRoomChange (roomId) {
  if (!roomId) { selectedSession.value = null; await loadActive(); return }
  const room = rooms.value.find(item => Number(item.roomId) === Number(roomId));
  if (!room) return
  if (!room.sessionId) {
    try {
      const result = await ensureLiveSession({ roomName: room.roomName, platformRoomId: room.roomCode, liveDate: new Date().toISOString().slice(0, 10) })
      const session = result.data || {}
      room.sessionId = session.sessionId || session.id
      if (!room.sessionId) throw new Error('场次创建失败')
      ElMessage.success('已自动创建今日直播场次')
    } catch (e) { selectedRoomId.value = null; selectedSession.value = null; ElMessage.error(e?.message || '直播场次创建失败'); return }
  }
  selectedSession.value = Number(room.sessionId)
  await loadActive()
}
function openRegistration (row = {}, preset = {}) { attachmentFiles.value = []; Object.assign(form, { customerId: row.customerId || null, nickname: row.nickname || '', secUid: row.secUid || '', sessionId: row.sessionId || selectedSession.value || null, ownerUserId: row.ownerUserId || null, anchorUserId: null, controllerUserId: null, interactionType: preset.interactionType || 'COMMENT', addFanStatus: preset.addFanStatus || row.fanStatus || '', stage: preset.stage || row.stage || 'NOT_CONSULTED', consultModel: row.consultModel || '', concern: preset.concern || '', specialMark: row.specialMark || '', note: '', orderNo: row.orderNo || '' }); drawerVisible.value = true }
function quickConsult (row) { openRegistration(row, { interactionType: 'CONSULT', stage: 'CONSULTING' }); ElMessage.info('已带入“咨询未下单”，补充型号或顾虑后保存') }
async function showCustomer (row) { detailVisible.value = true; detailLoading.value = true; try { detail.value = (await getLiveCustomer(row.customerId)).data } finally { detailLoading.value = false } }
async function showBatch (row) { batchVisible.value = true; batchDetail.value = (await getLiveUploadBatch(row.id)).data }
async function runTask (row, action, extra = {}) { await actionLiveTask(row.id, { action, ...extra }); ElMessage.success('任务已更新'); await loadTasks() }
const claimTask = row => runTask(row, 'CLAIM'); const completeTask = row => runTask(row, 'COMPLETE'); const releaseTask = row => runTask(row, 'RELEASE')
async function nextTask (row) { const nextTaskDate = window.prompt('请输入下次跟进日期（YYYY-MM-DD）', new Date(Date.now() + 86400000).toISOString().slice(0, 10)); if (nextTaskDate) await runTask(row, 'NEXT', { nextTaskDate }) }
async function ensureFormSession () {
  if (!form.sessionId) return true
  const room = rooms.value.find(item => Number(item.roomId) === Number(form.sessionId) && !item.sessionId)
  if (!room) return true
  try {
    const result = await ensureLiveSession({ roomName: room.roomName, platformRoomId: room.roomCode, liveDate: new Date().toISOString().slice(0, 10) })
    const session = result.data || {}
    room.sessionId = session.sessionId || session.id
    form.sessionId = room.sessionId ? Number(room.sessionId) : null
    return !!form.sessionId
  } catch (e) { ElMessage.error(e?.message || '直播场次创建失败'); return false }
}
function editOrder (row) { Object.assign(orderForm, { id: row.id, orderNo: row.orderNo || '', productModel: row.productModel || '', payAmount: row.payAmount == null ? null : Number(row.payAmount), orderStatus: row.orderStatus || 'ORDERED_PENDING_NO' }); orderVisible.value = true }
async function saveOrder () { saving.value = true; try { await updateLiveOrder(orderForm.id, orderForm); ElMessage.success('订单已更新'); orderVisible.value = false; await loadOrders() } finally { saving.value = false } }
async function submitRegistration (next = false) { if (!formRef.value) return; const valid = await formRef.value.validate().catch(() => false); if (!valid || !(await ensureFormSession())) return; saving.value = true; try { const saved = await saveLiveRegistration({ ...form, fanStatus: form.addFanStatus, consultStage: form.stage }); const savedData = saved.data || {}; if (attachmentFiles.value.length) await uploadAttachments(savedData.customerId || savedData.id || form.customerId, savedData.opportunityId); ElMessage.success('已保存，本次互动已追加'); if (next) { attachmentFiles.value = []; Object.assign(form, { customerId: null, nickname: '', secUid: '', interactionType: 'COMMENT', addFanStatus: '', stage: 'NOT_CONSULTED', consultModel: '', concern: '', specialMark: '', note: '', orderNo: '' }) } else drawerVisible.value = false; await loadActive() } finally { saving.value = false } }
async function uploadAttachments (customerId, opportunityId) { if (!customerId) return; for (const item of attachmentFiles.value) { const source = item.raw || item; const file = await compressImage(source); try { await uploadLiveAttachment(file, { customerId, opportunityId }) } catch (error) { ElMessage.warning(`客户资料已保存；${source.name} 上传失败，可稍后补传`) } } }
async function compressImage (file) {
  if (!file || !file.type?.startsWith('image/') || file.size < 200 * 1024 || typeof createImageBitmap !== 'function') return file
  try {
    const bitmap = await createImageBitmap(file)
    const maxDimension = 6000
    const scale = Math.min(1, maxDimension / bitmap.width, maxDimension / bitmap.height)
    const canvas = document.createElement('canvas')
    canvas.width = Math.max(1, Math.round(bitmap.width * scale)); canvas.height = Math.max(1, Math.round(bitmap.height * scale))
    const context = canvas.getContext('2d', { alpha: true }); context.imageSmoothingEnabled = true; context.imageSmoothingQuality = 'high'
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height); bitmap.close()
    const blob = await new Promise(resolve => canvas.toBlob(resolve, file.type, file.type === 'image/png' ? undefined : 0.98))
    if (!blob || blob.size >= file.size) return file
    ElMessage.info(`${file.name} 已优化 ${(file.size / 1024 / 1024).toFixed(1)}MB → ${(blob.size / 1024 / 1024).toFixed(1)}MB`)
    return new File([blob], file.name, { type: file.type, lastModified: file.lastModified || Date.now() })
  } catch (error) { return file }
}
async function openAttachment (item) { try { const result = await getLiveAttachmentUrl(item.id); const url = result.data?.url; if (!url) throw new Error('查看地址为空'); previewUrl.value = url; previewVisible.value = true } catch (error) { ElMessage.error(error?.message || '无法打开截图') } }
async function loadOptions () { const result = (await getLiveOptions()).data || {}; rooms.value = result.rooms || []; users.value = result.users || [] }
onMounted(async () => { await loadOptions(); loadActive() })
watch(() => form.sessionId, value => { if (value) ensureFormSession() })
</script>

<style scoped>
.customer-workbench{min-height:calc(100vh - 84px);padding:16px 22px 28px;background:#f5f7fa;color:#1f2937}.workspace-bar{max-width:1480px;margin:0 auto 8px;display:flex;align-items:center;justify-content:space-between;height:38px}.workspace-title{display:flex;align-items:center;gap:9px;font-size:18px;font-weight:650}.title-mark{display:inline-flex;align-items:center;justify-content:center;width:28px;height:28px;border-radius:7px;background:#e9f7f1;color:#159570}.workspace-title .el-tag{margin-left:2px}.workspace-meta{font-size:12px;color:#8b96a5}.workbench-tabs{max-width:1480px;margin:auto;background:#fff;border:1px solid #e5e9ef;border-radius:8px;padding:0 18px 20px;box-shadow:0 1px 2px rgba(16,24,40,.03)}.workbench-tabs :deep(.el-tabs__header){margin-bottom:8px}.workbench-tabs :deep(.el-tabs__item){height:46px;font-size:14px;gap:5px}.workbench-tabs :deep(.el-tabs__item .el-icon){vertical-align:-2px}.metric-row{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;padding:6px 0 14px}.metric{border:1px solid #edf0f3;border-radius:7px;padding:10px 14px;display:flex;align-items:center;justify-content:space-between}.metric span{font-size:12px;color:#7c8795}.metric b{font-size:20px;color:#22324a}.metric b.warning{color:#e68a26}.toolbar{display:flex;gap:10px;padding:3px 0 14px}.toolbar .el-input{width:280px}.toolbar .el-select{width:160px}.data-table{width:100%}.customer-cell{display:flex;align-items:center;gap:7px}.muted{margin-top:4px;color:#98a2b3;font-size:12px}.text-success{color:#159570}.task-summary{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;padding:7px 0 15px}.task-card{border-left:3px solid #7b8ba8;background:#f8fafc;border-radius:6px;padding:10px 13px;display:flex;justify-content:space-between;align-items:center}.task-card span{font-size:12px;color:#667085}.task-card b{font-size:20px;color:#344054}.task-card.warning{border-color:#e6a23c}.task-card.warning b{color:#b7791f}.task-card.danger{border-color:#e55b5b}.task-card.danger b{color:#c53c3c}.task-card.primary{border-color:#4a90e2}.task-card.primary b{color:#3575bb}.quick-hint{display:flex;align-items:center;gap:7px;margin:-3px 0 12px;padding:8px 10px;background:#f8fafc;border:1px solid #edf0f3;border-radius:6px;color:#667085;font-size:12px}.load-alert{margin:0 0 12px}.order-flow{display:flex;align-items:center;gap:8px;color:#667085;font-size:12px;padding:11px 2px 18px;flex-wrap:wrap}.order-flow span{padding:6px 10px;border:1px solid #e4e7ec;border-radius:5px;background:#fafbfc}.order-flow i{font-size:18px;color:#b2bac5;font-style:normal}.empty-hint,.drawer-tip,.cos-tip{display:flex;align-items:center;gap:7px;color:#8a95a3;font-size:12px}.empty-hint{justify-content:center;padding:45px}.drawer-tip{padding:9px 10px;background:#f0f8f4;color:#3d8068;border-radius:6px;margin-bottom:16px}.cos-tip{padding:9px 10px;background:#f8f9fb;border-radius:6px;margin-top:3px}.drawer-footer{display:flex;justify-content:flex-end;gap:9px}@media(max-width:700px){.customer-workbench{padding:10px 8px 18px}.workspace-title{font-size:16px}.workspace-meta{display:none}.metric-row,.task-summary{grid-template-columns:repeat(2,1fr)}.toolbar{flex-wrap:wrap}.toolbar .el-input,.toolbar .el-select{width:100%}.workbench-tabs{padding:0 10px 12px}.order-flow{gap:5px}.quick-hint{align-items:flex-start}}
.upload-tip{color:#98a2b3;font-size:12px;line-height:1.5}
.attachment-list{margin-top:16px}
.attachment-title{margin-bottom:8px;font-weight:600;color:#344054}
.attachment-row{display:flex;align-items:center;justify-content:space-between;padding:7px 10px;border-bottom:1px solid #f0f2f5;color:#606266;font-size:13px}
.attachment-preview{display:flex;align-items:center;justify-content:center;min-height:260px;background:#f5f7fa}
.attachment-preview img{display:block;max-width:100%;max-height:68vh;object-fit:contain}
</style>
<style scoped>
.handoff-note{display:flex;align-items:center;gap:9px;margin-top:14px;padding:9px 11px;border:1px solid #f5c2c2;background:#fff7f7;border-radius:6px;color:#8b4b4b;font-size:12px;flex-wrap:wrap}
</style>
<style scoped>
/* Workbench visual layer: compact hierarchy with one clear action per row. */
.customer-workbench{background:#f6f7f9;padding:18px 24px 32px;color:#202938}
.workspace-bar{max-width:1480px;height:42px;margin:0 auto 12px}
.workspace-title{font-size:19px;letter-spacing:0;color:#172235}
.title-mark{width:30px;height:30px;border-radius:8px;background:#e7f5ef;color:#159570}
.workspace-title .el-tag{border:0;background:#edf3ff;color:#3767b1}
.workspace-meta{color:#8b95a3}
.workbench-tabs{max-width:1480px;border:1px solid #e4e8ee;border-radius:10px;background:#fff;box-shadow:0 4px 16px rgba(31,43,61,.04);padding:0 20px 22px}
.workbench-tabs :deep(.el-tabs__header){margin:0 0 14px;border-bottom:1px solid #eef1f4}
.workbench-tabs :deep(.el-tabs__nav-wrap::after){height:1px;background:#eef1f4}
.workbench-tabs :deep(.el-tabs__item){height:50px;padding:0 17px;font-size:14px;color:#667085;transition:color .18s ease}
.workbench-tabs :deep(.el-tabs__item.is-active){color:#1f7f65;font-weight:600}
.workbench-tabs :deep(.el-tabs__active-bar){height:2px;background:#1f9a78}
.metric-row{grid-template-columns:repeat(4,minmax(0,1fr));gap:0;padding:0 0 16px;border-bottom:1px solid #f0f2f5}
.metric{position:relative;border:0;border-right:1px solid #edf0f3;border-radius:0;padding:5px 18px 5px 4px;min-height:48px}
.metric:last-child{border-right:0}
.metric span{font-size:12px;color:#8a94a3}
.metric b{font-size:22px;line-height:1;color:#26364d;font-weight:650}
.metric b.warning{color:#d98429}
.toolbar{align-items:center;gap:8px;padding:16px 0 13px}
.toolbar .el-input{width:280px}.toolbar .el-select{width:156px}
.toolbar .el-button{height:34px;border-radius:6px}
.toolbar .el-button[type=button]{font-weight:500}
.data-table{--el-table-border-color:#edf0f3;--el-table-header-bg-color:#fafbfc;--el-table-row-hover-bg-color:#f5faf8;border-radius:7px;overflow:hidden}
.data-table :deep(th.el-table__cell){height:42px;color:#7a8594;font-size:12px;font-weight:600;background:#fafbfc}
.data-table :deep(td.el-table__cell){height:52px;color:#344054;font-size:13px}
.data-table :deep(.el-button.is-link){padding:3px 4px;font-size:12px}
.customer-cell{gap:8px}.customer-cell strong{color:#26364d;font-weight:600}
.muted{font-size:11px;color:#98a2b3}
.quick-hint{gap:7px;margin:0 0 12px;padding:8px 10px;border:0;background:#f7faf9;color:#6b7b75;font-size:12px}
.quick-hint .el-button{margin-left:auto}
.task-summary{grid-template-columns:repeat(4,minmax(0,1fr));gap:8px;padding:3px 0 15px}
.task-card{border:1px solid #edf0f3;border-left:3px solid #8b9ab0;background:#fff;border-radius:7px;padding:10px 13px}
.task-card.warning{border-left-color:#dfa14b;background:#fffdfa}.task-card.danger{border-left-color:#e46d6d;background:#fffafa}.task-card.primary{border-left-color:#5a96df;background:#fbfdff}
.task-card span{font-size:12px;color:#7d8794}.task-card b{font-size:21px;color:#30415a}
.order-flow{padding:8px 2px 17px;color:#7b8795}.order-flow span{padding:5px 9px;border-color:#e8ebef;background:#fafbfc}.order-flow i{color:#c3cad2}
.empty-hint{padding:42px;color:#9aa4b2}
.drawer-tip{margin:-2px 0 14px;padding:9px 10px;background:#f3faf6;color:#437963}
.drawer-footer{gap:8px}.drawer-footer .el-button{min-width:82px;height:34px;border-radius:6px}
.upload-tip{margin-top:5px;color:#98a2b3;font-size:11px;line-height:1.5}
.attachment-list{margin-top:18px;padding-top:14px;border-top:1px solid #eef1f4}.attachment-title{font-size:13px}
.attachment-row{padding:8px 10px;background:#fafbfc;border-bottom:1px solid #eef1f4}
.attachment-preview{min-height:300px;border-radius:7px;overflow:hidden;background:#f4f6f8}
.attachment-preview img{max-height:70vh}
.detail-head{display:flex;align-items:center;gap:10px;padding:2px 0 16px}.detail-head strong{font-size:20px;color:#26364d}
.load-alert{margin:0 0 12px}
@media (max-width:900px){.customer-workbench{padding:12px 10px 22px}.workbench-tabs{padding:0 12px 16px}.metric-row,.task-summary{grid-template-columns:repeat(2,minmax(0,1fr));gap:0}.metric{border-bottom:1px solid #edf0f3}.metric:nth-child(2){border-right:0}.toolbar{flex-wrap:wrap}.toolbar .el-input,.toolbar .el-select{width:calc(50% - 4px)}.toolbar .el-button{flex:1}.workspace-meta{display:none}}
@media (max-width:560px){.workbench-tabs :deep(.el-tabs__item){padding:0 9px;font-size:12px}.toolbar .el-input,.toolbar .el-select{width:100%}.toolbar .el-button{flex:none}.task-summary{gap:6px}.task-card{padding:9px}.drawer-footer{flex-wrap:wrap}.drawer-footer .el-button{flex:1;min-width:0}}
</style>

