<template>
  <div class="customer-center-v3">
    <header class="command-bar">
      <nav class="view-tabs" aria-label="客户中心页面">
        <button v-for="item in tabs" :key="item.key" type="button" :class="{ active: activeTab === item.key }" @click="switchTab(item.key)">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </button>
      </nav>
      <div v-if="!['sync', 'download'].includes(activeTab)" class="context-tools">
        <el-date-picker v-model="dateRange" class="range-picker" type="daterange" value-format="YYYY-MM-DD" unlink-panels range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" :disabled-date="disableFutureDate" @change="changeRange" />
        <el-select v-model="selectedRoomId" class="room-select" clearable filterable placeholder="全部直播间" @change="changeRoom">
          <el-option v-for="room in rooms" :key="room.roomId" :label="room.roomName" :value="room.roomId" />
        </el-select>
      </div>
    </header>

    <section v-if="activeTab === 'inbox' || activeTab === 'followup'" class="content-panel">
      <div class="filter-bar">
        <el-input v-model="query.keyword" class="search-input" clearable :prefix-icon="Search" placeholder="搜索昵称、评论或联系方式" @keyup.enter="search" @clear="search" />
        <el-select v-model="query.stage" clearable placeholder="全部阶段" @change="search">
          <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-checkbox v-if="activeTab === 'inbox'" v-model="query.commentedOnly" border @change="search">只看有评论</el-checkbox>
        <span class="result-count">{{ total }} 位客户</span>
        <el-button type="primary" :icon="Plus" @click="openRegistration(null, true)">手动登记</el-button>
      </div>

      <el-table v-loading="loading" :data="rows" row-key="customerId" class="customer-table" empty-text="当前条件下没有客户">
        <el-table-column label="客户" min-width="220">
          <template #default="{ row }">
            <div class="customer-name-row">
              <button class="customer-link" type="button" @click="openDetail(row)">{{ row.nickname }}</button>
              <el-tooltip :content="profileActionLabel(row)"><el-button link class="profile-link" :icon="Link" :aria-label="profileActionLabel(row)" @click.stop="openProfile(row)" /></el-tooltip>
            </div>
            <div class="subline customer-context"><span v-if="row.roomName" :title="row.roomName">{{ row.roomName }}</span><span v-if="hasContact(row)" :title="contactText(row)">{{ compactContact(row) }}</span></div>
          </template>
        </el-table-column>
        <el-table-column label="客户动态" min-width="330">
          <template #default="{ row }">
            <button v-if="row.commentCount" class="comment-link" type="button" @click="openDetail(row)">{{ row.lastComment || '查看评论内容' }}</button>
            <span v-else class="viewer-copy">有效观看 {{ watchText(row.watchSeconds) }}</span>
            <div class="subline interaction-meta"><span>{{ formatTime(row.lastInteractionAt) }}</span><span>{{ row.visitCount || 0 }} 次来访</span><span>{{ watchText(row.watchSeconds) }}</span><span v-if="row.commentCount">{{ row.commentCount }} 条评论</span></div>
          </template>
        </el-table-column>
        <el-table-column v-if="activeTab === 'followup'" label="首次跟进" width="160">
          <template #default="{ row }">
            <span>{{ row.controllerFirstFollowupAt ? formatTime(row.controllerFirstFollowupAt) : '-' }}</span>
            <div v-if="row.controllerFirstFollowupName" class="subline">{{ row.controllerFirstFollowupName }}</div>
          </template>
        </el-table-column>
        <el-table-column label="阶段" width="116"><template #default="{ row }"><el-tag size="small" effect="plain" :type="stageType(row.stage) || undefined">{{ stageLabel(row.stage) }}</el-tag></template></el-table-column>
        <el-table-column label="负责人" width="130"><template #default="{ row }"><button class="owner-button" type="button" @click="openOwnerDialog(row)"><span>{{ row.ownerName || '待领取' }}</span><el-icon><EditPen /></el-icon></button></template></el-table-column>
        <el-table-column label="操作" fixed="right" width="126">
          <template #default="{ row }">
            <div class="row-actions">
              <el-tooltip v-if="activeTab === 'inbox' && row.taskId && !row.ownerUserId" content="领取客户"><el-button circle type="primary" :icon="User" @click="claim(row)" /></el-tooltip>
              <el-tooltip content="登记或修改跟进"><el-button circle :icon="EditPen" @click="openRegistration(row, false)" /></el-tooltip>
              <el-tooltip content="查看客户档案"><el-button circle :icon="View" @click="openDetail(row)" /></el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="mobile-list" v-loading="loading">
        <article v-for="row in rows" :key="row.customerId" class="mobile-customer">
          <div class="mobile-head"><div class="customer-name-row"><button class="customer-link" @click="openDetail(row)">{{ row.nickname }}</button><el-button link class="profile-link" :icon="Link" :aria-label="profileActionLabel(row)" @click.stop="openProfile(row)" /></div><el-tag size="small" effect="plain" :type="stageType(row.stage) || undefined">{{ stageLabel(row.stage) }}</el-tag></div>
          <div class="subline customer-context"><span v-if="row.roomName">{{ row.roomName }}</span><span v-if="hasContact(row)">{{ compactContact(row) }}</span></div>
          <button v-if="row.commentCount" class="comment-link" @click="openDetail(row)">{{ row.lastComment }}</button><div v-else class="viewer-copy">有效观看 {{ watchText(row.watchSeconds) }}</div>
          <div class="mobile-meta"><span>{{ row.visitCount || 0 }} 次来访</span><span>{{ watchText(row.watchSeconds) }}</span><span>{{ row.ownerName || '待领取' }}</span><span>{{ formatShortTime(row.lastInteractionAt) }}</span></div>
          <div class="mobile-actions"><el-button size="small" :icon="User" @click="openOwnerDialog(row)">负责人</el-button><el-button size="small" @click="openDetail(row)">查看档案</el-button><el-button size="small" type="primary" @click="openRegistration(row, false)">登记跟进</el-button></div>
        </article>
        <el-empty v-if="!loading && !rows.length" description="当前条件下没有客户" :image-size="70" />
      </div>

      <el-pagination v-if="total > query.pageSize" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="desktop-pagination" background layout="total, prev, pager, next" :total="total" @current-change="loadCustomers" />
      <el-pagination v-if="total > query.pageSize" v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" class="mobile-pagination" size="small" background layout="prev, pager, next" :pager-count="5" :total="total" @current-change="loadCustomers" />
    </section>

    <section v-else-if="activeTab === 'dashboard'" class="dashboard" v-loading="loading">
      <div class="stat-grid">
        <div v-for="card in dashboardCards" :key="card.label" class="stat-item"><span>{{ card.label }}</span><strong>{{ card.value }}</strong><small>{{ card.note }}</small></div>
      </div>
      <div class="ranking-panel">
        <div class="section-title"><strong>直播间评论客户</strong><span>{{ rangeLabel }}</span></div>
        <el-table :data="dashboard.roomStats || []" empty-text="所选时间没有评论客户上报">
          <el-table-column prop="roomName" label="直播间" min-width="220" />
          <el-table-column prop="reportedCustomers" label="上报客户" width="110" />
          <el-table-column prop="uploadedComments" label="评论数" width="100" />
          <el-table-column prop="claimedCustomers" label="已领取" width="100" />
          <el-table-column prop="unclaimedCustomers" label="待领取" width="100" />
          <el-table-column label="领取率" width="110"><template #default="{ row }"><strong class="orange-text">{{ rate(row.claimedCustomers, row.reportedCustomers) }}%</strong></template></el-table-column>
          <el-table-column label="最近上报" width="150"><template #default="{ row }">{{ formatTime(row.lastReportedAt) }}</template></el-table-column>
        </el-table>
      </div>
      <div class="ranking-panel">
        <div class="section-title"><strong>负责人跟进情况</strong><span>{{ selectedRoom?.roomName || '全部直播间' }} · {{ rangeLabel }}</span></div>
        <el-table :data="dashboard.ranking || []" empty-text="暂无跟进数据">
          <el-table-column type="index" label="排名" width="70" /><el-table-column prop="ownerName" label="负责人" min-width="140" /><el-table-column prop="customerCount" label="客户数" width="100" /><el-table-column prop="commentedCount" label="有评论" width="100" /><el-table-column prop="consultingCount" label="咨询中" width="100" /><el-table-column prop="orderedCount" label="已下单" width="100" /><el-table-column label="转化率" width="110"><template #default="{ row }"><strong class="orange-text">{{ conversion(row) }}%</strong></template></el-table-column>
        </el-table>
      </div>
    </section>

    <section v-else-if="activeTab === 'download'" class="content-panel download-panel">
      <div class="download-filters">
        <div class="quick-ranges" aria-label="快捷日期">
          <button type="button" :class="{ active: isDownloadRange(0, 0) }" @click="setDownloadRange(0, 0)">今天</button>
          <button type="button" :class="{ active: isDownloadRange(-1, -1) }" @click="setDownloadRange(-1, -1)">昨天</button>
          <button type="button" :class="{ active: isDownloadRange(-6, 0) }" @click="setDownloadRange(-6, 0)">近 7 天</button>
          <button type="button" :class="{ active: isDownloadRange(-29, 0) }" @click="setDownloadRange(-29, 0)">近 30 天</button>
        </div>
        <el-date-picker v-model="dateRange" class="download-range" type="daterange" value-format="YYYY-MM-DD" unlink-panels range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" :disabled-date="disableFutureDate" @change="changeRange" />
        <el-select v-model="selectedRoomId" class="download-room" clearable filterable placeholder="全部直播间" @change="changeRoom">
          <el-option v-for="room in rooms" :key="room.roomId" :label="room.roomName" :value="room.roomId" />
        </el-select>
        <el-select v-model="downloadQuery.stage" clearable placeholder="全部阶段" @change="changeDownloadFilter">
          <el-option v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="downloadQuery.queue" placeholder="全部领取状态" @change="changeDownloadFilter">
          <el-option label="全部领取状态" value="ALL" />
          <el-option label="待领取" value="INBOX" />
          <el-option label="已领取" value="FOLLOWUP" />
        </el-select>
        <el-checkbox v-model="downloadCommentedOnly" border @change="changeDownloadFilter">只看有评论</el-checkbox>
      </div>
      <div class="download-summary">
        <div class="download-summary-main">
          <span>可下载</span><strong>{{ downloadTotal.toLocaleString() }}</strong><span>条客户明细</span>
          <el-tag v-if="downloadTotal > 10000" type="warning" effect="plain">本次导出前 10,000 条</el-tag>
        </div>
        <div class="download-scope">{{ rangeLabel }}<span>·</span>{{ selectedRoom?.roomName || '全部直播间' }}<span>·</span>{{ downloadScopeText }}</div>
        <el-button type="primary" :icon="Download" :loading="exporting" :disabled="!downloadTotal" @click="downloadAll">{{ downloadButtonText }}</el-button>
      </div>
      <div class="download-table-wrap">
        <el-table v-loading="loading" :data="downloadRows" class="download-table" empty-text="所选条件没有可下载的客户">
          <el-table-column label="日期 / 直播间" min-width="210">
            <template #default="{ row }"><strong>{{ row.liveDate }}</strong><div class="subline ellipsis-line" :title="row.roomName">{{ row.roomName || '-' }}</div></template>
          </el-table-column>
          <el-table-column label="客户 / 联系方式" min-width="180">
            <template #default="{ row }"><strong>{{ row.nickname || '-' }}</strong><div class="subline ellipsis-line" :title="contactText(row)">{{ contactText(row) }}</div></template>
          </el-table-column>
          <el-table-column label="最近互动" min-width="240" show-overflow-tooltip>
            <template #default="{ row }"><span>{{ row.lastComment || (row.commentCount ? '有评论' : '有效观看') }}</span><div class="subline">{{ formatTime(row.lastInteractionAt) }}</div></template>
          </el-table-column>
          <el-table-column label="订单 / 截图" min-width="200"><template #default="{ row }"><span>{{ row.orderNos || '无订单号' }}</span><div class="subline">{{ Number(row.screenshotCount || 0) }} 张截图</div></template></el-table-column>
          <el-table-column label="直播表现" min-width="185"><template #default="{ row }">{{ row.visitCount || 0 }} 次 · {{ watchText(row.watchSeconds) }}<span v-if="row.commentCount"> · {{ row.commentCount }} 条评论</span></template></el-table-column>
          <el-table-column label="阶段 / 负责人" min-width="145"><template #default="{ row }"><el-tag size="small" effect="plain" :type="stageType(row.stage) || undefined">{{ stageLabel(row.stage) }}</el-tag><div class="subline">{{ row.ownerName || '待领取' }}</div></template></el-table-column>
          <el-table-column label="场控首次跟进" min-width="165"><template #default="{ row }"><span>{{ row.controllerFirstFollowupAt ? formatTime(row.controllerFirstFollowupAt) : '尚未跟进' }}</span><div v-if="row.controllerFirstFollowupName" class="subline">{{ row.controllerFirstFollowupName }}</div></template></el-table-column>
        </el-table>
      </div>
      <el-pagination v-if="downloadTotal > downloadQuery.pageSize" v-model:current-page="downloadQuery.pageNum" v-model:page-size="downloadQuery.pageSize" :page-sizes="[20, 50, 100]" background layout="total, sizes, prev, pager, next" :total="downloadTotal" @current-change="loadDownloadPreview" @size-change="changeDownloadPageSize" />
    </section>

    <section v-else class="content-panel sync-panel">
      <div class="section-title"><strong>直播间同步</strong><span>超过 24 小时未上报会标红</span></div>
      <el-table v-loading="loading" :data="syncRooms" empty-text="暂无同步记录">
        <el-table-column prop="roomName" label="直播间" min-width="220" /><el-table-column label="最近一天上报" width="150"><template #default="{ row }">{{ row.accepted }} 条</template></el-table-column><el-table-column label="最近上报时间" width="190"><template #default="{ row }">{{ formatTime(row.lastUpload) || '-' }}</template></el-table-column><el-table-column label="状态" width="150"><template #default="{ row }"><el-tag :type="row.statusType" effect="plain">{{ row.statusLabel }}</el-tag></template></el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="registrationVisible" :before-close="closeRegistration" size="min(560px, 100%)" class="registration-drawer" @paste="handleImagePaste">
      <template #header><div><strong>{{ form.customerId ? '登记客户跟进' : '手动登记客户' }}</strong><div class="drawer-subtitle">{{ formRoomName || '请先选择直播间' }}</div></div></template>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="registration-form">
        <div class="form-grid two"><el-form-item label="客户昵称" prop="nickname"><el-autocomplete v-model="form.nickname" :fetch-suggestions="suggestCustomers" value-key="nickname" clearable placeholder="输入昵称自动匹配历史档案" @select="selectCustomer" /></el-form-item><el-form-item label="负责人"><el-select v-model="form.ownerUserId" filterable placeholder="默认当前登录人"><el-option v-for="user in users" :key="user.userId" :label="user.userName" :value="user.userId" /></el-select></el-form-item></div>
        <div class="contact-strip"><el-form-item label="抖音号"><el-input v-model="form.douyinAccount" clearable /></el-form-item><el-form-item label="手机号"><el-input v-model="form.phone" clearable /></el-form-item><el-form-item label="微信号"><el-input v-model="form.wechat" clearable /></el-form-item></div>
        <el-form-item label="本次结果" prop="stage"><el-radio-group v-model="form.stage" class="stage-picker"><el-radio-button v-for="item in stageOptions.filter(x => x.value !== 'NOT_CONSULTED')" :key="item.value" :value="item.value">{{ item.label }}</el-radio-button></el-radio-group></el-form-item>
        <div class="form-grid two"><el-form-item label="主播"><el-select v-model="form.anchorUserId" clearable filterable placeholder="自动匹配"><el-option v-for="item in roomAnchors" :key="item.userId" :label="item.userName" :value="item.userId" /></el-select></el-form-item><el-form-item label="场控"><el-select v-model="form.controllerUserId" clearable filterable placeholder="自动匹配"><el-option v-for="item in roomControllers" :key="item.userId" :label="item.userName" :value="item.userId" /></el-select></el-form-item></div>
        <div v-if="['CONSULTING','HIGH_INTENT','ORDERED'].includes(form.stage)" class="form-grid two"><el-form-item label="咨询型号"><el-input v-model="form.consultModel" clearable /></el-form-item><el-form-item label="客户顾虑"><el-input v-model="form.concern" clearable /></el-form-item></div>
        <div class="form-grid two"><el-form-item v-if="form.stage === 'ORDERED'" label="订单号"><el-input v-model="form.orderNo" clearable placeholder="没有可稍后补录" /></el-form-item><el-form-item label="下次跟进"><el-date-picker v-model="form.nextTaskDate" type="date" value-format="YYYY-MM-DD" clearable placeholder="无需跟进可不选" /></el-form-item></div>
        <el-form-item label="跟进记录"><el-input v-model="form.note" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="记录客户需求或处理结果" /></el-form-item>
        <el-form-item label="凭证图片"><div class="paste-upload-area" tabindex="0"><el-upload v-model:file-list="files" action="#" multiple accept="image/*" :auto-upload="false" list-type="picture-card"><el-icon><Plus /></el-icon></el-upload><small>当前窗口可直接按 Ctrl+V 粘贴截图，也可以选择图片</small></div></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="registrationVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="saveRegistration">保存</el-button></div></template>
    </el-drawer>

    <el-drawer v-model="detailVisible" size="min(720px, 100%)" class="detail-drawer">
      <template #header><div class="detail-header"><div><strong>{{ detail?.nickname || '客户档案' }}</strong><div class="drawer-subtitle">{{ detailContact }}</div></div><el-button v-if="detail" :icon="Link" @click="openProfile(detail)">{{ profileActionLabel(detail) }}</el-button></div></template>
      <div v-loading="detailLoading" class="detail-body">
        <div class="profile-strip"><div><span>来访</span><strong>{{ detailVisitCount }} 次</strong></div><div><span>有效观看</span><strong>{{ watchText(detailWatchSeconds) }}</strong></div><div><span>评论</span><strong>{{ detailComments.length }} 条</strong></div><div><span>当前阶段</span><strong>{{ stageLabel(detail?.stage) }}</strong></div></div>
        <div class="first-followup"><span>场控首次跟进</span><strong>{{ detailFirstFollowup ? formatTime(detailFirstFollowup.at) : '尚未跟进' }}</strong><small v-if="detailFirstFollowup?.name">{{ detailFirstFollowup.name }}</small></div>
        <section class="detail-section"><div class="section-title"><strong>评论记录</strong><span>{{ detailComments.length }} 条</span></div><div v-if="detailComments.length" class="timeline"><div v-for="item in detailComments" :key="item.id" class="timeline-item"><p>{{ item.content }}</p><span>{{ item.roomName }} · {{ formatTime(item.occurredAt) }}</span></div></div><el-empty v-else description="没有评论记录" :image-size="64" /></section>
        <section v-if="detail?.registrations?.length" class="detail-section"><div class="section-title"><strong>跟进记录</strong></div><div class="timeline"><div v-for="item in detail.registrations" :key="item.id" class="timeline-item"><p><el-tag v-if="item.interactionType === 'OWNER_CHANGE'" size="small" effect="plain" class="record-tag">负责人变更</el-tag>{{ item.note || item.concern || stageLabel(item.stage) }}</p><span>{{ item.registeredName || '-' }} · {{ formatTime(item.createdAt) }}</span></div></div></section>
        <section v-if="detailImages.length" class="detail-section"><div class="section-title"><strong>凭证图片</strong></div><div class="image-grid"><el-image v-for="item in detailImages" :key="item.id" :src="item.url" :preview-src-list="detailImages.map(x => x.url)" fit="cover" preview-teleported /></div></section>
      </div>
      <template #footer><div class="drawer-footer"><el-button @click="detailVisible=false">关闭</el-button><el-button :icon="User" @click="openOwnerDialog(detail)">修改负责人</el-button><el-button type="primary" :icon="EditPen" @click="editFromDetail">登记跟进</el-button></div></template>
    </el-drawer>

    <el-dialog v-model="ownerDialogVisible" title="修改负责人" width="min(440px, 92vw)" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="客户"><el-input :model-value="ownerForm.nickname" disabled /></el-form-item>
        <el-form-item label="负责人" required><el-select v-model="ownerForm.ownerUserId" filterable placeholder="请选择负责人" style="width:100%"><el-option v-for="user in users" :key="user.userId" :label="user.userName" :value="user.userId" /></el-select></el-form-item>
        <el-form-item label="修改原因"><el-input v-model="ownerForm.reason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="选填，例如：客户跨直播间、运营交接" /></el-form-item>
      </el-form>
      <template #footer><div class="drawer-footer"><el-button @click="ownerDialogVisible=false">取消</el-button><el-button type="primary" :loading="ownerSaving" @click="saveOwnerChange">确认修改</el-button></div></template>
    </el-dialog>
  </div>
</template>

<script setup name="LiveCustomerCenter">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, Download, EditPen, Link, List, Plus, Refresh, Search, User, UserFilled, View } from '@element-plus/icons-vue'
import { saveAs } from 'file-saver'
import useUserStore from '@/store/modules/user'
import { actionLiveTask, changeLiveCustomerOwner, exportCustomerCenter, getCustomerCenterDashboard, getLiveAttachmentUrl, getLiveCustomer, getLiveOptions, listCustomerCenter, listLiveUploadBatches, saveLiveRegistration, searchLiveCustomerSuggestions, uploadLiveAttachment } from '@/api/liveCustomer'

const route = useRoute(), router = useRouter(), userStore = useUserStore()
const tabs = [{ key: 'inbox', label: '待处理', icon: List }, { key: 'followup', label: '跟进中', icon: UserFilled }, { key: 'dashboard', label: '数据看板', icon: DataAnalysis }, { key: 'sync', label: '同步记录', icon: Refresh }, { key: 'download', label: '下载明细', icon: Download }]
const activeTab = ref(tabs.some(x => x.key === route.query.tab) ? route.query.tab : 'inbox')
const loading = ref(false), exporting = ref(false), saving = ref(false)
const rows = ref([]), total = ref(0), rooms = ref([]), users = ref([]), batches = ref([])
const downloadRows = ref([]), downloadTotal = ref(0), downloadCommentedOnly = ref(false)
const dashboard = reactive({}), selectedRoomId = ref(readRoom())
const query = reactive({ keyword: '', stage: '', commentedOnly: false, pageNum: 1, pageSize: 20 })
const downloadQuery = reactive({ stage: '', queue: 'ALL', pageNum: 1, pageSize: 20 })
const defaultRanges = { inbox: [dateOffset(0), dateOffset(0)], followup: [dateOffset(-1), dateOffset(0)], dashboard: [dateOffset(-1), dateOffset(-1)], download: [dateOffset(-1), dateOffset(0)] }
const tabRanges = reactive({ inbox: [...defaultRanges.inbox], followup: [...defaultRanges.followup], dashboard: [...defaultRanges.dashboard], download: [...defaultRanges.download] })
if (route.query.beginDate && route.query.endDate && tabRanges[activeTab.value]) tabRanges[activeTab.value] = [String(route.query.beginDate), String(route.query.endDate)]
const dateRange = computed({ get: () => tabRanges[activeTab.value] || [], set: value => { if (tabRanges[activeTab.value]) tabRanges[activeTab.value] = value } })
const stageOptions = [{ value: 'NOT_CONSULTED', label: '未沟通' }, { value: 'CONTACTED', label: '已沟通' }, { value: 'CONSULTING', label: '咨询中' }, { value: 'HIGH_INTENT', label: '高意向' }, { value: 'ORDERED', label: '已下单' }, { value: 'INVALID', label: '无效' }]
const selectedRoom = computed(() => rooms.value.find(x => Number(x.roomId) === Number(selectedRoomId.value)) || null)
const rangeLabel = computed(() => { const [begin, end] = dateRange.value || []; return begin === end ? begin : `${begin} 至 ${end}` })
const downloadScopeText = computed(() => [downloadQuery.stage && stageLabel(downloadQuery.stage), downloadQuery.queue === 'INBOX' ? '待领取' : downloadQuery.queue === 'FOLLOWUP' ? '已领取' : '全部领取状态', downloadCommentedOnly.value && '有评论'].filter(Boolean).join(' · '))
const downloadButtonText = computed(() => downloadTotal.value > 10000 ? '下载前 10,000 条' : `下载 ${downloadTotal.value.toLocaleString()} 条`)
const dashboardCards = computed(() => [{ label: '有效客户', value: Number(dashboard.validCustomers || 0), note: '评论或观看满 5 分钟' }, { label: '评论客户上报', value: Number(dashboard.commentedCustomers || 0), note: '按客户去重' }, { label: '评论已领取', value: Number(dashboard.claimedCommented || 0), note: `领取率 ${rate(dashboard.claimedCommented, dashboard.commentedCustomers)}%` }, { label: '评论待领取', value: Number(dashboard.unclaimedCommented || 0), note: '保留到领取或处理' }, { label: '已下单', value: Number(dashboard.ordered || 0), note: `转化率 ${rate(dashboard.ordered, dashboard.validCustomers)}%` }])
const syncRooms = computed(() => { const map = new Map(); for (const item of batches.value) { const roomName = item.room_name || item.roomName || '未命名直播间', time = new Date(item.created_at || item.createdAt || 0).getTime(), current = map.get(roomName) || { roomName, accepted: 0, lastUpload: null, lastTime: 0 }; if (time > current.lastTime) { current.lastTime = time; current.lastUpload = item.created_at || item.createdAt; current.accepted = Number(item.dayAccepted ?? item.accepted_count ?? item.acceptedCount ?? 0) } map.set(roomName, current) } return [...map.values()].sort((a, b) => b.lastTime - a.lastTime).map(item => { const age = item.lastTime ? Date.now() - item.lastTime : Infinity; return { ...item, statusType: age > 86400000 ? 'danger' : age > 43200000 ? 'warning' : 'success', statusLabel: age > 86400000 ? '超过 1 天未上报' : age > 43200000 ? '超过 12 小时' : '正常' } }) })

const registrationVisible = ref(false), formRef = ref(), files = ref([]), manualRegistration = ref(false), form = reactive(emptyForm())
const rules = { nickname: [{ required: true, message: '请输入客户昵称', trigger: 'blur' }], stage: [{ required: true, message: '请选择本次结果', trigger: 'change' }] }
const formRoomName = computed(() => form.roomName || selectedRoom.value?.roomName || ''), formRoom = computed(() => rooms.value.find(x => x.roomName === formRoomName.value) || selectedRoom.value)
const roomAnchors = computed(() => staffForRoom('anchor')), roomControllers = computed(() => staffForRoom('controller'))
const detailVisible = ref(false), detailLoading = ref(false), detail = ref(null), detailImages = ref([])
const ownerDialogVisible = ref(false), ownerSaving = ref(false), ownerForm = reactive({ customerId: null, nickname: '', ownerUserId: null, reason: '' })
const detailComments = computed(() => { const seen = new Set(); return (detail.value?.interactions || []).filter(x => x.eventType === 'COMMENT' && x.content).filter(x => { const key = `${x.sessionId}|${String(x.content).normalize('NFKC').trim().toLowerCase()}`; if (seen.has(key)) return false; seen.add(key); return true }) })
const detailVisitCount = computed(() => new Set((detail.value?.interactions || []).map(x => x.sessionId).filter(Boolean)).size)
const detailWatchSeconds = computed(() => { const values = new Map(); for (const item of (detail.value?.interactions || [])) values.set(item.sessionId || item.liveDate || item.id, Math.max(Number(values.get(item.sessionId) || 0), Number(item.watchSeconds || 0))); return [...values.values()].reduce((sum, value) => sum + value, 0) })
const detailFirstFollowup = computed(() => { const item = (detail.value?.opportunities || []).find(x => x.controllerFirstFollowupAt || x.firstFollowedAt); return item ? { at: item.controllerFirstFollowupAt || item.firstFollowedAt, name: item.controllerFirstFollowupName || item.controllerName || item.ownerName || '' } : null })
const detailContact = computed(() => [detail.value?.douyinAccount && `抖音号 ${detail.value.douyinAccount}`, detail.value?.phone, detail.value?.wechat].filter(Boolean).join(' · ') || '暂无联系方式')

function emptyForm() { return { customerId: null, nickname: '', secUid: '', douyinAccount: '', phone: '', wechat: '', roomName: '', liveDate: '', sessionId: null, ownerUserId: Number(userStore.id) || null, anchorUserId: null, controllerUserId: null, interactionType: 'COMMENT', stage: 'CONTACTED', addFanStatus: 'UNKNOWN', consultModel: '', concern: '', specialMark: '', note: '', orderNo: '', nextTaskDate: '' } }
function storageKey() { return `liveCustomerCenter.v3.room.${userStore.id || 'anonymous'}` }
function readRoom() { try { const value = Number(localStorage.getItem(storageKey())); return value > 0 ? value : null } catch { return null } }
function rememberRoom() { try { selectedRoomId.value ? localStorage.setItem(storageKey(), selectedRoomId.value) : localStorage.removeItem(storageKey()) } catch {} }
function switchTab(value) { activeTab.value = value; query.pageNum = 1; downloadQuery.pageNum = 1; syncRoute(); reload() }
function changeRange(value) { if (!value?.[0] || !value?.[1]) tabRanges[activeTab.value] = [...defaultRanges[activeTab.value]]; const [begin, end] = tabRanges[activeTab.value]; const days = Math.floor((new Date(`${end}T00:00:00`) - new Date(`${begin}T00:00:00`)) / 86400000) + 1; if (days > 31) { const start = new Date(`${end}T00:00:00`); start.setDate(start.getDate() - 30); tabRanges[activeTab.value] = [localDate(start), end]; ElMessage.warning('单次最多查看 31 天，已自动调整开始日期') } query.pageNum = 1; downloadQuery.pageNum = 1; syncRoute(); reload() }
function changeRoom() { rememberRoom(); query.pageNum = 1; reload() }
function changeDownloadFilter() { downloadQuery.pageNum = 1; loadDownloadPreview() }
function changeDownloadPageSize() { downloadQuery.pageNum = 1; loadDownloadPreview() }
function setDownloadRange(beginOffset, endOffset) { tabRanges.download = [dateOffset(beginOffset), dateOffset(endOffset)]; changeRange(tabRanges.download) }
function isDownloadRange(beginOffset, endOffset) { return dateRange.value?.[0] === dateOffset(beginOffset) && dateRange.value?.[1] === dateOffset(endOffset) }
function search() { query.pageNum = 1; loadCustomers() }
function syncRoute() { const range = tabRanges[activeTab.value]; router.replace({ query: range ? { tab: activeTab.value, beginDate: range[0], endDate: range[1] } : { tab: activeTab.value } }) }
function dateParams() { return { beginDate: dateRange.value?.[0], endDate: dateRange.value?.[1] } }
function reload() { if (['inbox', 'followup'].includes(activeTab.value)) loadCustomers(); else if (activeTab.value === 'dashboard') loadDashboard(); else if (activeTab.value === 'download') loadDownloadPreview(); else loadSync() }
async function loadOptions() { const data = (await getLiveOptions()).data || {}; rooms.value = data.rooms || []; users.value = data.users || []; if (selectedRoomId.value && !rooms.value.some(x => Number(x.roomId) === Number(selectedRoomId.value))) selectedRoomId.value = null }
async function loadCustomers() { loading.value = true; try { const data = (await listCustomerCenter({ ...dateParams(), roomName: selectedRoom.value?.roomName, queue: activeTab.value === 'followup' ? 'FOLLOWUP' : 'INBOX', ...query })).data || {}; rows.value = data.rows || []; total.value = Number(data.total || 0) } catch { rows.value = []; total.value = 0 } finally { loading.value = false } }
async function loadDashboard() { loading.value = true; try { const data = (await getCustomerCenterDashboard({ ...dateParams(), roomName: selectedRoom.value?.roomName })).data || {}; Object.keys(dashboard).forEach(key => delete dashboard[key]); Object.assign(dashboard, data) } finally { loading.value = false } }
async function loadSync() { loading.value = true; try { const response = await listLiveUploadBatches({ pageNum: 1, pageSize: 200 }); batches.value = response.rows || [] } finally { loading.value = false } }
async function loadDownloadPreview() { loading.value = true; try { const data = (await listCustomerCenter({ ...dateParams(), roomName: selectedRoom.value?.roomName, queue: downloadQuery.queue, stage: downloadQuery.stage, commentedOnly: downloadCommentedOnly.value, pageNum: downloadQuery.pageNum, pageSize: downloadQuery.pageSize })).data || {}; downloadRows.value = data.rows || []; downloadTotal.value = Number(data.total || 0) } catch { downloadRows.value = []; downloadTotal.value = 0 } finally { loading.value = false } }
async function downloadAll() { exporting.value = true; try { const params = { ...dateParams(), roomName: selectedRoom.value?.roomName, queue: downloadQuery.queue, stage: downloadQuery.stage, commentedOnly: downloadCommentedOnly.value }; saveAs(await exportCustomerCenter(params), `直播客户明细-${dateRange.value[0]}至${dateRange.value[1]}.csv`); ElMessage.success(downloadTotal.value > 10000 ? '已下载前 10,000 条明细' : `已下载 ${downloadTotal.value.toLocaleString()} 条客户明细`) } catch (error) { ElMessage.error(error?.message || '下载失败，请稍后重试') } finally { exporting.value = false } }
async function claim(row) { try { await actionLiveTask(row.taskId, { action: 'CLAIM' }); ElMessage.success('客户已领取'); await loadCustomers() } catch (error) { ElMessage.error(error?.message || '领取失败') } }
function openOwnerDialog(row) { if (!row?.customerId) return; Object.assign(ownerForm, { customerId: row.customerId, nickname: row.nickname || '未命名客户', ownerUserId: row.ownerUserId ?? (Number(userStore.id) || null), reason: '' }); ownerDialogVisible.value = true }
async function saveOwnerChange() { if (!ownerForm.ownerUserId) return ElMessage.warning('请选择负责人'); ownerSaving.value = true; try { const response = await changeLiveCustomerOwner(ownerForm.customerId, { ownerUserId: ownerForm.ownerUserId, reason: ownerForm.reason?.trim() || undefined }); const result = response.data || {}; ownerDialogVisible.value = false; ElMessage.success(result.changed === false ? '负责人没有变化' : '负责人已修改并记录'); if (form.customerId === ownerForm.customerId) form.ownerUserId = result.ownerUserId; if (detail.value?.customerId === ownerForm.customerId) detail.value = { ...detail.value, ownerUserId: result.ownerUserId, ownerName: result.ownerName }; await reload() } catch (error) { ElMessage.error(error?.message || '负责人修改失败') } finally { ownerSaving.value = false } }

function openRegistration(row, manual) { manualRegistration.value = manual; files.value = []; const currentUserId = Number(userStore.id) || null; Object.assign(form, emptyForm(), { customerId: row?.customerId || null, nickname: row?.nickname || '', douyinAccount: row?.douyinAccount || '', phone: row?.phone || '', wechat: row?.wechat || '', roomName: row?.roomName || selectedRoom.value?.roomName || '', liveDate: row?.liveDate || dateOffset(0), sessionId: row?.sessionId || null, ownerUserId: row?.customerId ? (row?.ownerUserId ?? currentUserId) : currentUserId, anchorUserId: row?.anchorUserId || null, controllerUserId: row?.controllerUserId || null, stage: normalizeStage(row?.stage), specialMark: row?.specialMark || '', orderNo: row?.orderNo || '' }); registrationVisible.value = true; if (row?.customerId) hydrateForm(row.customerId) }
async function hydrateForm(customerId) { try { const customer = (await getLiveCustomer(customerId)).data || {}; if (form.customerId !== customerId) return; const opportunity = customer.opportunities?.find(item => item.ownerUserId) || customer.opportunities?.[0] || {}, registration = customer.registrations?.[0] || {}, order = customer.orders?.[0] || {}; Object.assign(form, { douyinAccount: customer.douyinAccount || form.douyinAccount, phone: customer.phone || form.phone, wechat: customer.wechat || form.wechat, ownerUserId: customer.ownerUserId ?? opportunity.ownerUserId ?? form.ownerUserId ?? (Number(userStore.id) || null), anchorUserId: opportunity.anchorUserId || form.anchorUserId, controllerUserId: opportunity.controllerUserId || form.controllerUserId, stage: normalizeStage(opportunity.stage || registration.stage || customer.stage), consultModel: opportunity.consultModel || registration.consultModel || '', concern: opportunity.concern || registration.concern || '', specialMark: opportunity.specialMark || registration.specialMark || '', note: registration.note || '', orderNo: order.orderNo || registration.orderNo || '' }) } catch { ElMessage.warning('历史档案读取失败，请刷新后重试') } }
function normalizeStage(value) { return stageOptions.some(x => x.value === value) && value !== 'NOT_CONSULTED' ? value : 'CONTACTED' }
function staffForRoom(role) { const ids = String(formRoom.value?.[`${role}Ids`] || '').split(',').filter(Boolean).map(Number); return users.value.filter(user => ids.includes(Number(user.userId))) }
function suggestCustomers(keyword, callback) { if (!keyword?.trim()) return callback([]); searchLiveCustomerSuggestions({ keyword: keyword.trim(), limit: 8 }).then(response => callback(response.data || [])).catch(() => callback([])) }
function selectCustomer(item) { Object.assign(form, { customerId: item.customerId, nickname: item.nickname, douyinAccount: item.douyinAccount || '', phone: item.phone || '', wechat: item.wechat || '' }); hydrateForm(item.customerId) }
function handleImagePaste(event) {
  const items = Array.from(event.clipboardData?.items || [])
  const allowedTypes = new Set(['image/jpeg', 'image/png', 'image/webp'])
  const fileItems = items.filter(item => item.kind === 'file' && item.type.startsWith('image/'))
  const imageItems = fileItems.filter(item => allowedTypes.has(item.type.toLowerCase()))
  if (!fileItems.length) return
  event.preventDefault()
  if (!imageItems.length) return ElMessage.warning('图片格式不支持，仅支持 JPG、PNG、WEBP')
  imageItems.forEach((item, index) => {
    const blob = item.getAsFile()
    if (!blob) return
    const extension = (blob.type.split('/')[1] || 'png').replace('jpeg', 'jpg')
    const file = new File([blob], `clipboard-${Date.now()}-${index}.${extension}`, { type: blob.type })
    files.value.push({ name: file.name, raw: file, size: file.size, status: 'ready', uid: `${Date.now()}-${index}`, url: URL.createObjectURL(file) })
  })
  if (fileItems.length > imageItems.length) ElMessage.warning('部分图片格式不支持，仅支持 JPG、PNG、WEBP')
  ElMessage.success(`已添加 ${imageItems.length} 张剪贴板图片`)
}
async function saveRegistration() { if (!(await formRef.value?.validate().catch(() => false))) return; if (!formRoomName.value) return ElMessage.warning('请先选择直播间'); if (manualRegistration.value && !form.douyinAccount && !form.phone && !form.wechat) return ElMessage.warning('手动登记至少填写一种联系方式'); saving.value = true; try { const room = formRoom.value, response = await saveLiveRegistration({ ...form, roomName: formRoomName.value, liveDate: form.liveDate || dateOffset(0), sessionId: form.sessionId || room?.sessionId || undefined, interactionType: ['HIGH_INTENT', 'ORDERED'].includes(form.stage) ? 'BUY_INTENT' : form.stage === 'CONSULTING' ? 'CONSULT' : 'COMMENT' }), saved = response.data || {}, savedCustomerId = saved.customerId || saved.id || form.customerId; let failed = 0; if (files.value.length && !savedCustomerId) throw new Error('客户已保存，但未返回客户编号，请刷新后重试上传图片'); for (const item of files.value) { try { await uploadLiveAttachment(item.raw || item, { customerId: savedCustomerId, opportunityId: saved.opportunityId }) } catch { failed += 1 } } ElMessage[failed ? 'warning' : 'success'](failed ? `客户资料已保存；${failed} 张图片上传失败，可稍后补传` : '客户资料已保存'); registrationVisible.value = false; await reload() } catch (error) { ElMessage.error(error?.message || '保存失败，请稍后重试') } finally { saving.value = false } }
function closeRegistration(done) { done() }

async function openDetail(row) { detailVisible.value = true; detailLoading.value = true; detail.value = null; detailImages.value = []; try { detail.value = (await getLiveCustomer(row.customerId)).data || {}; detailImages.value = (await Promise.all((detail.value.attachments || []).map(async item => { try { const response = await getLiveAttachmentUrl(item.id); return { ...item, url: response.data?.url } } catch { return null } }))).filter(x => x?.url) } finally { detailLoading.value = false } }
function editFromDetail() { const row = detail.value; detailVisible.value = false; openRegistration(row, false) }
function profileSecUid(row) {
  const values = [row?.secUid, ...(row?.identities || []).map(item => item.secUid)]
  for (const value of values) {
    const normalized = normalizeProfileIdentity(value)
    if (normalized) return normalized
  }
  return ''
}
function normalizeProfileIdentity(value) {
  let text = String(value || '').trim()
  if (!text || text.startsWith('dom:')) return ''
  try { text = decodeURIComponent(text) } catch {}
  if (/^https?:\/\//i.test(text)) {
    try {
      const url = new URL(text)
      if (!/(^|\.)douyin\.com$/i.test(url.hostname)) return ''
      const match = url.pathname.match(/\/user\/([^/?#]+)/i)
      return match ? decodeURIComponent(match[1]) : url.href
    } catch { return '' }
  }
  const queryMatch = text.match(/(?:^|[?&])sec_uid=([^&#]+)/i)
  if (queryMatch) return decodeURIComponent(queryMatch[1])
  const pathMatch = text.match(/(?:^|\/)user\/([^/?#]+)/i)
  if (pathMatch) return decodeURIComponent(pathMatch[1])
  return (text.startsWith('MS4wLjAB') || text.length >= 32) && !/[\s/:?#&=]/.test(text) ? text : ''
}
function profileActionLabel(row) { return profileSecUid(row) ? '打开抖音主页' : '在抖音搜索该客户' }
function openProfile(row) {
  const identity = profileSecUid(row)
  const nickname = String(row?.nickname || '').trim()
  if (!identity && !nickname) return ElMessage.warning('没有可用于查找客户的信息')
  const url = /^https?:\/\//i.test(identity) ? identity : identity ? `https://www.douyin.com/user/${encodeURIComponent(identity)}` : `https://www.douyin.com/search/${encodeURIComponent(nickname)}?type=user`
  window.open(url, '_blank', 'noopener,noreferrer')
}
function stageLabel(value) { return stageOptions.find(x => x.value === value)?.label || '未沟通' }
function stageType(value) { return ({ ORDERED: 'success', HIGH_INTENT: 'warning', CONSULTING: 'primary', INVALID: 'info' }[value] || '') }
function hasContact(row) { return Boolean(row?.douyinAccount || row?.phone || row?.wechat) }
function contactText(row) { return [row.douyinAccount && `抖音号 ${row.douyinAccount}`, row.phone, row.wechat].filter(Boolean).join(' · ') || '暂无联系方式' }
function compactContact(row) { return row.douyinAccount ? `抖音号 ${row.douyinAccount}` : row.phone ? `手机 ${row.phone}` : `微信 ${row.wechat}` }
function watchText(seconds) { const value = Number(seconds || 0); if (value < 60) return '不足 1 分钟'; const minutes = Math.round(value / 60); return minutes >= 60 ? `${Math.floor(minutes / 60)} 小时 ${minutes % 60} 分钟` : `${minutes} 分钟` }
function formatTime(value) { if (!value) return ''; return new Date(value).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).replaceAll('/', '-') }
function formatShortTime(value) { if (!value) return ''; return new Date(value).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false }) }
function localDate(date) { return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` }
function dateOffset(offset) { const date = new Date(); date.setDate(date.getDate() + offset); return localDate(date) }
function disableFutureDate(value) { const end = new Date(); end.setHours(23, 59, 59, 999); return value > end }
function rate(value, base) { return Number(base) ? (Number(value || 0) * 100 / Number(base)).toFixed(1) : '0.0' }
function conversion(row) { return rate(row.orderedCount, row.customerCount) }
watch(() => route.query.tab, value => { if (tabs.some(x => x.key === value) && value !== activeTab.value) { activeTab.value = value; if (route.query.beginDate && route.query.endDate && tabRanges[value]) tabRanges[value] = [String(route.query.beginDate), String(route.query.endDate)]; reload() } })
onMounted(async () => { await loadOptions(); syncRoute(); reload() })
</script>

<style scoped>
.owner-button{display:inline-flex;align-items:center;gap:5px;max-width:100%;padding:3px 0;border:0;background:transparent;color:#4c5666;cursor:pointer}.owner-button span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.owner-button .el-icon{flex:0 0 auto;color:#9aa2ad}.owner-button:hover,.owner-button:hover .el-icon{color:#e75e1b}
.record-tag{margin-right:7px;vertical-align:1px}
.customer-center-v3{min-height:calc(100vh - 84px);padding:12px;background:#f5f6f8;color:#273142}.command-bar{display:flex;align-items:center;justify-content:space-between;gap:16px;min-height:54px;padding:7px 10px;background:#fff;border:1px solid #e8ebef;border-radius:7px}.view-tabs{display:flex;align-items:center;gap:3px}.view-tabs button{display:flex;align-items:center;gap:6px;height:36px;padding:0 14px;border:0;border-radius:5px;background:transparent;color:#626d7c;cursor:pointer;white-space:nowrap}.view-tabs button:hover{background:#f6f7f9;color:#303846}.view-tabs button.active{background:#fff3ec;color:#e75e1b;font-weight:650}.context-tools,.filter-bar{display:flex;align-items:center;gap:8px}.range-picker{width:250px}.room-select{width:210px}.content-panel,.dashboard{margin-top:10px;padding:12px;background:#fff;border:1px solid #e8ebef;border-radius:7px}.filter-bar{margin-bottom:10px}.search-input{width:min(360px,32vw)}.filter-bar>.el-select{width:150px}.result-count{margin-left:auto;color:#7b8491;font-size:13px}.customer-table{width:100%}.customer-table :deep(th.el-table__cell){height:40px;background:#f8f9fa;color:#596474;font-weight:600}.customer-table :deep(td.el-table__cell){padding:9px 0}.customer-name-row{display:flex;align-items:center;gap:3px;min-width:0}.customer-link,.comment-link{max-width:100%;padding:0;border:0;background:none;text-align:left;cursor:pointer}.customer-link{overflow:hidden;color:#273142;font-weight:650;text-overflow:ellipsis;white-space:nowrap}.customer-link:hover,.comment-link:hover{color:#e75e1b}.profile-link{flex:0 0 auto;padding:2px!important;color:#8c96a3!important}.profile-link:hover{color:#e75e1b!important}.comment-link{display:block;overflow:hidden;color:#4c5666;text-overflow:ellipsis;white-space:nowrap}.viewer-copy{color:#596474}.subline{display:flex;gap:8px;margin-top:4px;color:#9299a3;font-size:12px}.customer-context,.interaction-meta{overflow:hidden;white-space:nowrap}.customer-context span,.interaction-meta span{overflow:hidden;text-overflow:ellipsis}.customer-context span+span:before,.interaction-meta span+span:before{margin-right:8px;color:#c3c8ce;content:'·'}.ellipsis-line{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.row-actions{display:flex;gap:6px}.row-actions .el-button+.el-button{margin-left:0}.el-pagination{justify-content:flex-end;margin-top:12px}.mobile-list{display:none}.stat-grid{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:1px;overflow:hidden;border:1px solid #e8ebef;border-radius:6px;background:#e8ebef}.stat-item{min-height:100px;padding:15px;background:#fff}.stat-item span,.stat-item small{display:block;color:#7b8491}.stat-item strong{display:block;margin:8px 0 5px;font-size:25px;letter-spacing:0}.stat-item small{font-size:12px}.ranking-panel{margin-top:10px;border:1px solid #e8ebef;border-radius:6px}.section-title{display:flex;align-items:center;justify-content:space-between;min-height:46px;padding:0 14px;border-bottom:1px solid #edf0f2}.section-title span{color:#89919d;font-size:12px}.orange-text{color:#e75e1b}.sync-panel{padding:0}.download-panel{padding:0;overflow:hidden}.download-filters{display:flex;align-items:center;gap:8px;flex-wrap:wrap;padding:10px 12px;border-bottom:1px solid #edf0f2;background:#fafbfc}.download-filters>.el-select{width:150px}.download-range{width:250px}.download-room{width:220px!important}.quick-ranges{display:flex;overflow:hidden;border:1px solid #dcdfe6;border-radius:4px;background:#fff}.quick-ranges button{height:32px;padding:0 11px;border:0;border-right:1px solid #e4e7ed;background:#fff;color:#606266;cursor:pointer;white-space:nowrap}.quick-ranges button:last-child{border-right:0}.quick-ranges button:hover,.quick-ranges button.active{background:#fff3ec;color:#e75e1b}.quick-ranges button.active{font-weight:600}.download-summary{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:14px;min-height:58px;padding:8px 12px;border-bottom:1px solid #edf0f2}.download-summary-main{display:flex;align-items:baseline;gap:6px;white-space:nowrap}.download-summary-main strong{color:#e75e1b;font-size:24px;letter-spacing:0}.download-summary-main span,.download-scope{color:#7b8491;font-size:13px}.download-scope{overflow:hidden;text-align:right;text-overflow:ellipsis;white-space:nowrap}.download-scope span{margin:0 6px;color:#c0c4cc}.download-table-wrap{overflow-x:auto}.download-table{min-width:1000px}.download-table :deep(th.el-table__cell){height:40px;background:#f8f9fa;color:#596474;font-weight:600}.download-table :deep(td.el-table__cell){padding:8px 0}.registration-form{padding-right:4px}.paste-upload-area{width:100%;padding:10px;border:1px dashed #d6dbe2;border-radius:6px;outline:none}.paste-upload-area:focus-within,.paste-upload-area:focus{border-color:#e75e1b;background:#fffaf7}.paste-upload-area>small{display:block;margin-top:7px;color:#8a93a0;font-size:12px}.drawer-subtitle{margin-top:4px;color:#8a93a0;font-size:12px}.form-grid{display:grid;gap:12px}.form-grid.two{grid-template-columns:1fr 1fr}.contact-strip{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;padding:10px 12px;margin-bottom:18px;background:#f7f8fa;border-radius:6px}.contact-strip .el-form-item{margin-bottom:0}.stage-picker{display:flex;width:100%}.stage-picker :deep(.el-radio-button){flex:1}.stage-picker :deep(.el-radio-button__inner){width:100%;padding:9px 5px}.drawer-footer{display:flex;justify-content:flex-end;gap:8px}.detail-header{display:flex;align-items:center;justify-content:space-between;gap:16px;width:100%;padding-right:10px}.detail-body{padding-right:4px}.profile-strip{display:grid;grid-template-columns:repeat(4,1fr);border:1px solid #e8ebef;border-radius:6px}.profile-strip>div{padding:12px}.profile-strip>div+div{border-left:1px solid #e8ebef}.profile-strip span,.profile-strip strong{display:block}.profile-strip span{margin-bottom:5px;color:#87909c;font-size:12px}.first-followup{display:flex;align-items:center;gap:14px;padding:11px 13px;margin-top:10px;background:#fff7f1;border-left:3px solid #e75e1b;border-radius:4px}.first-followup span{color:#7b8491}.first-followup small{margin-left:auto;color:#7b8491}.detail-section{margin-top:16px;border:1px solid #e8ebef;border-radius:6px}.timeline{padding:2px 14px}.timeline-item{padding:11px 0;border-bottom:1px solid #eef0f2}.timeline-item:last-child{border-bottom:0}.timeline-item p{margin:0 0 5px;line-height:1.55}.timeline-item span{color:#8b939e;font-size:12px}.image-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:8px;padding:12px}.image-grid .el-image{width:100%;aspect-ratio:1;border-radius:5px;background:#f2f3f5}.customer-center-v3 :deep(.el-button--primary){--el-button-bg-color:#e75e1b;--el-button-border-color:#e75e1b;--el-button-hover-bg-color:#f1783d;--el-button-hover-border-color:#f1783d}.customer-center-v3 :deep(.el-radio-button__original-radio:checked+.el-radio-button__inner){background:#e75e1b;border-color:#e75e1b;box-shadow:-1px 0 0 0 #e75e1b}.customer-center-v3 :deep(.el-checkbox.is-bordered.is-checked){border-color:#e75e1b}.customer-center-v3 :deep(.el-checkbox__input.is-checked .el-checkbox__inner){background:#e75e1b;border-color:#e75e1b}
@media(max-width:900px){.customer-center-v3{padding:8px}.command-bar{align-items:stretch;flex-direction:column}.view-tabs{display:grid;grid-template-columns:repeat(5,1fr)}.view-tabs button{justify-content:center;padding:0 6px}.context-tools{display:grid;grid-template-columns:1fr 1fr}.range-picker,.room-select{width:100%}.content-panel,.dashboard{padding:9px}.filter-bar{display:grid;grid-template-columns:1fr auto}.search-input{grid-column:1/-1;width:100%}.filter-bar>.el-select{width:100%}.result-count{margin-left:0}.customer-table{display:none}.mobile-list{display:block}.mobile-customer{padding:12px 2px;border-bottom:1px solid #eceff2}.mobile-head,.mobile-meta,.mobile-actions{display:flex;align-items:center;justify-content:space-between;gap:8px}.mobile-customer>.comment-link,.mobile-customer>.viewer-copy{margin-top:9px}.mobile-meta{justify-content:flex-start;margin-top:8px;color:#89919d;font-size:12px}.mobile-meta span+span:before{margin-right:8px;content:'·'}.mobile-actions{justify-content:flex-end;margin-top:10px}.stat-grid{grid-template-columns:repeat(2,1fr)}.stat-item:last-child{grid-column:1/-1}.download-panel{padding:0}.download-filters{display:grid;grid-template-columns:1fr 1fr}.quick-ranges{grid-column:1/-1;width:max-content;max-width:100%}.download-range,.download-room,.download-filters>.el-select{width:100%!important}.download-summary{grid-template-columns:1fr auto;gap:6px 12px}.download-scope{grid-column:1/-1;grid-row:2;text-align:left}.form-grid.two,.contact-strip{grid-template-columns:1fr}.profile-strip{grid-template-columns:repeat(2,1fr)}.profile-strip>div:nth-child(3){border-left:0;border-top:1px solid #e8ebef}.profile-strip>div:nth-child(4){border-top:1px solid #e8ebef}.image-grid{grid-template-columns:repeat(3,1fr)}}
@media(max-width:520px){.view-tabs{grid-template-columns:repeat(3,1fr)}.view-tabs button{font-size:12px}.view-tabs button .el-icon{display:none}.context-tools{grid-template-columns:1fr}.context-tools>*{grid-column:1!important}.filter-bar{grid-template-columns:1fr}.filter-bar>*{grid-column:1!important}.filter-bar>.el-select{width:100%}.download-filters{grid-template-columns:1fr}.download-filters>*{grid-column:1!important}.quick-ranges{display:grid;grid-template-columns:repeat(4,1fr);width:100%}.quick-ranges button{padding:0 4px}.download-summary{grid-template-columns:1fr}.download-summary-main{flex-wrap:wrap;white-space:normal}.download-summary>.el-button{grid-row:3;width:100%}.download-scope{overflow:visible;white-space:normal}.stage-picker{display:grid;grid-template-columns:repeat(2,1fr)}.stage-picker :deep(.el-radio-button__inner){border:1px solid #dcdfe6!important;box-shadow:none!important}.first-followup{align-items:flex-start;flex-direction:column;gap:4px}.first-followup small{margin-left:0}.image-grid{grid-template-columns:repeat(2,1fr)}}
.mobile-pagination{display:none}
@media(max-width:900px){.desktop-pagination{display:none}.mobile-pagination{display:flex;justify-content:center}}
</style>
