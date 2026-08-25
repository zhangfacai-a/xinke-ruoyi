<template>
  <div class="app-container audience-page">


    <nav class="section-nav" aria-label="客户跟进页面">
      <div class="section-tabs">
        <button v-if="canViewFollowups" :class="{ active: activeSection === 'workbench' }" type="button"
          @click="switchSection('workbench')">今日工作</button>
        <button v-if="canViewFollowups" :class="{ active: activeSection === 'customers' }" type="button"
          @click="switchSection('customers')">客户库</button>
        <span v-if="canAssignFollowups" class="tab-divider"></span>
        <button v-if="canAssignFollowups" :class="{ active: activeSection === 'team' }" type="button"
          @click="switchSection('team')">管理中心</button>
        <button v-if="canViewRanks" :class="{ active: activeSection === 'records' }" type="button"
          @click="switchSection('records')">上传记录</button>
      </div>
    </nav>

    <template v-if="activeSection === 'workbench'">
      <section class="work-page">
        <header class="work-head">
          <div>
            <h3>今天要处理的客户</h3>
            <p>系统已按逾期、意向和榜单信号排好顺序</p>
          </div>
          <el-button type="primary" :disabled="!workRows.length" @click="processNextCustomer">处理下一位</el-button>
        </header>
        <div class="priority-strip" v-loading="summaryLoading" aria-label="任务概览">
          <button v-for="item in taskMetrics" :key="item.key"
            :class="[{ active: taskKey === item.key, danger: item.key === 'overdue' && item.value > 0 }]" type="button"
            @click="selectTask(item.key)"><span>{{ item.label }}</span><strong>{{ integer(item.value)
            }}</strong><small>{{ item.hint }}</small></button>
        </div>
        <div class="work-toolbar">
          <el-input v-model="workQuery.keyword" :prefix-icon="Search" clearable placeholder="搜索昵称、型号、订单号"
            @input="scheduleWorkSearch" @clear="loadWorkbench(true)" @keyup.enter="loadWorkbench(true)" />
          <el-select v-model="workQuery.roomId" clearable filterable placeholder="全部直播间"
            @change="loadWorkbench(true)"><el-option v-for="room in roomOptions" :key="room.roomId"
              :label="roomLabel(room)" :value="Number(room.roomId)" /></el-select>
          <el-button v-if="hasWorkFilters" link type="primary" @click="resetWorkFilters">清除筛选</el-button>
          <span class="work-count">{{ taskLabel(taskKey) }} · {{ integer(workTotal) }} 人</span>
        </div>
        <el-table v-loading="workLoading" :data="workRows" row-key="followupId" class="work-table"
          @row-click="openWorkbenchCustomer">
          <el-table-column label="客户" min-width="190" :fixed="viewportWidth >= 1280 ? 'left' : false"><template
              #default="{ row }">
              <div class="table-customer">
                <div class="name-line"><strong>{{ row.nicknameSnapshot || '未命名客户' }}</strong><span v-if="row.priority"
                    class="priority-label">重点</span></div><small>{{ relationSummary(row) }}</small>
              </div>
            </template></el-table-column>
          <el-table-column label="为什么优先" min-width="185"><template #default="{ row }"><strong class="reason-text">{{
            primaryReason(row) }}</strong><small class="cell-sub">{{ visitSummary(row)
                }}</small></template></el-table-column>
          <el-table-column label="来源" min-width="190"><template #default="{ row }"><span>{{ row.roomNameSnapshot || '-'
          }}</span><small class="cell-sub">主播 {{ row.anchorNameSnapshot || '未设置' }} · 场控 {{
                  row.controllerNameSnapshot || '未设置' }}</small></template></el-table-column>
          <el-table-column label="当前进度" min-width="150"><template #default="{ row }"><span
                :class="['status-name', statusTone(row.status)]">{{ row.intentLevel === 'HIGH' && row.status !==
                  'UNASSIGNED' ? '高意向' : stageLabel(row.status) }}</span><small class="cell-sub">{{ row.consultModel ? `咨询
                ${row.consultModel}` : businessSummary(row) }}</small></template></el-table-column>
          <el-table-column label="下一步" min-width="150"><template #default="{ row }"><span
                :class="{ overdue: isOverdue(row) }">{{ dueText(row) }}</span><small class="cell-sub">{{
                  row.ownerNameSnapshot || '尚未分配' }}</small></template></el-table-column>
          <el-table-column label="操作" width="92" :fixed="viewportWidth >= 1280 ? 'right' : false"
            align="right"><template #default="{ row }"><el-button
                v-if="row.status === 'UNASSIGNED' && canClaimFollowups" link type="primary"
                @click.stop="claimRow(row)">领取</el-button><el-button v-else link type="primary"
                @click.stop="openWorkbenchCustomer(row)">处理</el-button></template></el-table-column>
          <template #empty>
            <div class="empty-state">
              <p>{{ hasWorkFilters ? '没有符合条件的客户' : '这一组任务已经处理完成' }}</p><el-button v-if="hasWorkFilters" link
                type="primary" @click="resetWorkFilters">清除筛选</el-button><el-button
                v-else-if="taskKey !== 'unassigned' && unassignedTotal" link type="primary"
                @click="selectTask('unassigned')">去领取新客户</el-button>
            </div>
          </template>
        </el-table>
        <pagination v-show="workTotal > 0" :total="workTotal" v-model:page="workQuery.pageNum"
          v-model:limit="workQuery.pageSize" @pagination="loadWorkbench" />
      </section>
    </template>

    <section v-else-if="activeSection === 'customers'" class="customers-page">
      <header class="library-head">
        <div>
          <h3>客户库</h3>
          <p>一个抖音用户一份档案，到访、商机和订单持续累积</p>
        </div><el-button v-if="canAssignFollowups" :type="batchMode ? 'primary' : 'default'" plain
          @click="toggleBatchMode">{{ batchMode ? '退出批量操作' : '批量操作' }}</el-button>
      </header>
      <div class="customer-presets" aria-label="客户快捷视图"><button v-for="item in customerPresets" :key="item.key"
          :class="{ active: customerPreset === item.key }" type="button" @click="applyCustomerPreset(item.key)">{{
            item.label }}<b>{{ presetCount(item.key) }}</b></button></div>
      <div class="customer-toolbar">
        <el-input v-model="customerQuery.keyword" :prefix-icon="Search" clearable placeholder="搜索昵称、电话、微信、型号或订单号"
          @input="scheduleCustomerSearch" @clear="loadCustomers(true)" @keyup.enter="loadCustomers(true)" />
        <el-select v-model="customerQuery.roomId" clearable filterable placeholder="全部直播间"
          @change="loadCustomers(true)"><el-option v-for="room in roomOptions" :key="room.roomId"
            :label="roomLabel(room)" :value="Number(room.roomId)" /></el-select>
        <el-select v-model="customerQuery.ownerUserId" clearable filterable placeholder="全部领取人"
          @change="loadCustomers(true)"><el-option v-for="person in ownerOptions" :key="personId(person)"
            :label="personLabel(person)" :value="personId(person)" /></el-select>
        <el-button :icon="Filter" :class="{ 'filter-active': customerAdvancedCount > 0 }"
          @click="showCustomerFilters = !showCustomerFilters">更多筛选<span v-if="customerAdvancedCount"> · {{
            customerAdvancedCount }}</span></el-button>
        <el-button v-if="hasCustomerFilters" link type="primary" @click="resetCustomerFilters">清除</el-button>
      </div>
      <el-collapse-transition>
        <div v-show="showCustomerFilters" class="customer-more-filters">
          <el-select v-model="customerQuery.stage" clearable placeholder="客户阶段" @change="loadCustomers(true)"><el-option
              v-for="item in stageOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select>
          <el-select v-model="customerQuery.isFollower" clearable placeholder="粉丝关系"
            @change="loadCustomers(true)"><el-option label="粉丝" :value="true" /><el-option label="非粉丝"
              :value="false" /></el-select>
          <el-select v-model="customerQuery.isFollowing" clearable placeholder="回关状态"
            @change="loadCustomers(true)"><el-option label="已回关" :value="true" /><el-option label="未回关"
              :value="false" /></el-select>
          <el-select v-model="customerQuery.priority" clearable placeholder="重点客户"
            @change="loadCustomers(true)"><el-option label="仅重点客户" :value="true" /><el-option label="普通客户"
              :value="false" /></el-select>
          <el-select v-model="customerQuery.intentLevel" clearable placeholder="意向程度"
            @change="loadCustomers(true)"><el-option label="高意向" value="HIGH" /><el-option label="中意向"
              value="MEDIUM" /><el-option label="低意向" value="LOW" /><el-option label="未知" value="UNKNOWN" /></el-select>
          <el-select v-model="customerQuery.hasOrder" clearable placeholder="下单情况"
            @change="loadCustomers(true)"><el-option label="已经下单" :value="true" /><el-option label="尚未下单"
              :value="false" /></el-select>
          <div class="level-filter"><span>最低消费等级</span><el-input-number v-model="customerQuery.minPayLevel" :min="0"
              :max="1000" controls-position="right" @change="loadCustomers(true)" /></div>
          <div class="rank-filter"><span>评论榜前</span><el-input-number v-model="customerQuery.maxCommentRank" :min="1"
              :max="500" controls-position="right" placeholder="名次" @change="loadCustomers(true)" /><span>名</span></div>
          <div class="rank-filter"><span>观看榜前</span><el-input-number v-model="customerQuery.maxWatchRank" :min="1"
              :max="500" controls-position="right" placeholder="名次" @change="loadCustomers(true)" /><span>名</span></div>
        </div>
      </el-collapse-transition>

      <div v-if="batchMode && selectedCustomerIds.length && canAssignFollowups" class="batch-bar">
        <strong>已选 {{ selectedCustomerIds.length }} 位</strong>
        <div class="batch-group"><el-select v-model="batchOwnerUserId" clearable filterable
            placeholder="选择领取人"><el-option v-for="person in ownerOptions" :key="personId(person)"
              :label="personLabel(person)" :value="personId(person)" /></el-select><el-button type="primary"
            :disabled="batchOwnerUserId == null" :loading="batchSaving" @click="batchAssignOwner">分配</el-button></div>
        <div class="batch-group"><el-date-picker v-model="batchNextFollowAt" type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss" clearable placeholder="统一安排跟进时间" /><el-button
            :disabled="!batchNextFollowAt" :loading="batchSaving" @click="batchScheduleFollowup">安排</el-button></div>
        <el-dropdown trigger="click" @command="batchSetPriority"><el-button :loading="batchSaving">重点标记<el-icon
              class="el-icon--right">
              <ArrowDown />
            </el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item
                :command="true">设为重点</el-dropdown-item><el-dropdown-item
                :command="false">取消重点</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
      </div>
      <div class="customer-count">共 <strong>{{ integer(customerTotal) }}</strong> 位客户；观察中的观众不会生成待办，命中进客规则后自动进入跟进池</div>

      <el-table v-loading="customerLoading" :data="customerRows" row-key="followupId" class="customer-table"
        @selection-change="handleCustomerSelection" @row-click="openLibraryCustomer">
        <el-table-column v-if="canAssignFollowups && batchMode" type="selection" width="46" />
        <el-table-column label="客户" width="230" :fixed="viewportWidth >= 1280 ? 'left' : false"><template
            #default="{ row }">
            <div class="table-customer">
              <div class="name-line"><strong>{{ row.nicknameSnapshot || '未命名客户' }}</strong><span v-if="row.priority"
                  class="priority-label">重点</span></div><small>{{ relationSummary(row) }}</small>
            </div>
          </template></el-table-column>
        <el-table-column label="最近到访" width="215"><template #default="{ row }"><span>{{ row.roomNameSnapshot || '-'
        }}</span><small class="cell-sub">{{ shortTime(row.lastSeenAt) }} · {{ visitSummary(row)
              }}</small></template></el-table-column>
        <el-table-column label="当前商机" min-width="175"><template #default="{ row }"><span>{{ stageLabel(row.status) }}{{
              row.consultModel ? ` · ${row.consultModel}` : '' }}</span><small class="cell-sub">{{ row.status ===
                'OBSERVING' ? '等待新的有效信号' : primaryReason(row) }}</small></template></el-table-column>
        <el-table-column label="评论榜" width="92" align="center"><template #default="{ row }"><span
              :class="['rank-value', { empty: !row.commentRank }]">{{ tableRankLabel(row.commentRank) }}</span></template></el-table-column>
        <el-table-column label="观看榜" width="92" align="center"><template #default="{ row }"><span
              :class="['rank-value', { empty: !row.watchRank }]">{{ tableRankLabel(row.watchRank) }}</span></template></el-table-column>
        <el-table-column label="订单" width="110"><template #default="{ row }"><span>{{ Number(row.orderCount ||
          row.orders?.length || (row.orderNo ? 1 : 0)) }} 笔</span><small class="cell-sub">{{ row.orderNo || '尚未下单'
              }}</small></template></el-table-column>
        <el-table-column label="负责人" width="160"><template #default="{ row }"><span>{{ row.ownerNameSnapshot || '待领取'
        }}</span><small class="cell-sub">{{ row.ownerUserId ? '负责客户跟进' : '尚未分配'
              }}</small></template></el-table-column>
        <el-table-column label="下一步" width="150" :fixed="viewportWidth >= 1280 ? 'right' : false"
          align="right"><template #default="{ row }"><small :class="['next-action', { overdue: isOverdue(row) }]">{{
            dueText(row) }}</small><el-button link type="primary" @click.stop="openLibraryCustomer(row)">{{ row.status
                === 'OBSERVING' ? '查看 / 加入跟进' : (row.status === 'UNASSIGNED' ? '领取 / 查看' : (canEditRow(row) ? '继续跟进' :
                  '查看档案')) }}</el-button></template></el-table-column>
        <template #empty>
          <div class="empty-state">
            <p>没有符合条件的客户</p><el-button v-if="hasCustomerFilters" link type="primary"
              @click="resetCustomerFilters">清除筛选</el-button>
          </div>
        </template>
      </el-table>
      <pagination v-show="customerTotal > 0" :total="customerTotal" v-model:page="customerQuery.pageNum"
        v-model:limit="customerQuery.pageSize" @pagination="loadCustomers" />
    </section>

    <TeamDashboard v-else-if="activeSection === 'team'" ref="teamDashboardRef" :room-options="roomOptions"
      :owner-options="ownerOptions" :sync-summary="syncSummary" @drilldown="openDashboardCustomers"
      @open-sync="switchSection('records')" />

    <section v-else class="records-page">
      <header class="records-head">
        <div>
          <h3>同步中心</h3>
          <p>正常数据自动进入客户库，只有异常批次需要人工处理</p>
        </div>
        <div class="records-health"><span :class="{ warning: Number(syncSummary.unmatchedBatchCount) > 0 }"><i></i>{{
          Number(syncSummary.unmatchedBatchCount) > 0 ? `${integer(syncSummary.unmatchedBatchCount)} 个异常批次` :
            '插件与系统运行正常'
            }}</span><el-button v-if="Number(syncSummary.unmatchedBatchCount) > 0" type="warning" plain
            @click="batchQuery.needsAttention = true; loadBatches(true)">处理异常</el-button></div>
      </header>
      <div class="sync-overview">
        <div><span>最近同步</span><strong>{{ shortSyncTime(syncSummary.latestCapturedAt || batchRows[0]?.capturedAt)
        }}</strong><small>{{ syncSummary.latestCapturedAt || batchRows[0]?.capturedAt || '暂无记录' }}</small></div>
        <div><span>本次去重客户</span><strong>{{ integer(batchRows[0]?.uniqueUserCount || 0)
        }}</strong><small>同一客户只保留一份档案</small>
        </div>
        <div><span>新增客户</span><strong>{{ integer(batchRows[0]?.newCustomerCount || 0) }}</strong><small>首次进入客户库</small>
        </div>
        <div><span>更新客户</span><strong>{{ integer(batchRows[0]?.updatedCustomerCount || 0)
        }}</strong><small>再次到访已更新</small>
        </div>
        <div :class="{ warning: Number(syncSummary.unmatchedBatchCount) > 0 }"><span>需要处理</span><strong>{{
          integer(syncSummary.unmatchedBatchCount) }}</strong><small>{{ Number(syncSummary.unmatchedBatchCount) ?
              '请检查直播间匹配' : '当前运行正常' }}</small></div>
      </div>
      <div class="record-detail">
        <div class="record-detail-head"><strong>上传批次明细</strong><span>共 {{ integer(batchTotal) }} 批</span></div>
        <div class="record-toolbar"><el-input v-model="batchQuery.roomName" :prefix-icon="Search" clearable
            placeholder="搜索直播间" @keyup.enter="loadBatches(true)" @clear="loadBatches(true)" /><el-segmented
            v-model="batchQuery.currentOnly" :options="versionOptions" @change="loadBatches(true)" /><el-checkbox
            v-model="batchQuery.needsAttention" @change="loadBatches(true)">只看匹配异常</el-checkbox><el-date-picker
            v-model="batchDateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="上传开始" end-placeholder="结束日期" @change="loadBatches(true)" /></div>
        <el-table v-loading="recordLoading" :data="batchRows" row-key="batchId" class="record-table">
          <el-table-column label="上传时间" prop="capturedAt" min-width="164" /><el-table-column label="直播间"
            min-width="180"><template #default="{ row }"><strong>{{ row.matchedRoomName || row.roomName || '-'
            }}</strong><small class="cell-sub">主播 {{ integer(row.anchorCount) }} · 场控 {{
                  integer(row.controllerCount) }}</small></template></el-table-column>
          <el-table-column label="数据日期" min-width="188"><template #default="{ row }"><span>评论 {{ row.commentDataDate ||
            '-' }}</span><small class="cell-sub">观看 {{ row.watchDataDate || '-'
                }}</small></template></el-table-column>
          <el-table-column label="客户处理" min-width="190"><template #default="{ row }"><span>去重 {{
            integer(row.uniqueUserCount) }} 人</span><small class="cell-sub">新增 {{ integer(row.newCustomerCount) }} ·
                更新 {{ integer(row.updatedCustomerCount) }}</small></template></el-table-column>
          <el-table-column label="版本" min-width="120"><template #default="{ row }"><span
                :class="['version-state', { history: !row.isCurrent }]">{{ row.isCurrent ? '当前有效' : '历史版本'
                }}</span></template></el-table-column>
          <el-table-column label="处理状态" min-width="160"><template #default="{ row }"><span
                :class="['match-state', matchTone(row.roomMatchStatus)]">{{ matchLabel(row.roomMatchStatus)
                }}</span><small class="cell-sub">{{ row.roomMatchStatus === 'MATCHED' ? '已自动归入直播间' : '需要管理员确认'
                }}</small></template></el-table-column>
          <el-table-column label="操作" width="116" align="right"><template #default="{ row }"><el-button
                v-if="row.roomMatchStatus !== 'MATCHED'" link type="warning"
                @click="goRoomManagement">处理</el-button><el-button link type="primary"
                @click="openBatchDetail(row)">查看</el-button></template></el-table-column>
          <template #empty>
            <div class="empty-state">
              <p>暂无上传批次</p>
            </div>
          </template>
        </el-table>
        <pagination v-show="batchTotal > 0" :total="batchTotal" v-model:page="batchQuery.pageNum"
          v-model:limit="batchQuery.pageSize" @pagination="loadBatches" />
      </div>
    </section>

    <el-drawer v-model="detailDrawerVisible" :title="drawerTitle" :size="drawerSize" :before-close="beforeDrawerClose"
      append-to-body destroy-on-close>
      <FollowupEditor v-model="selectedDetail" :mode="selectedMode" :loading="detailLoading" :saving="savingDetail"
        :dirty="detailDirty" :can-edit="canEditFollowups" :can-assign="canAssignFollowups"
        :can-claim="canClaimFollowups" :can-view-history="canViewHistory" :logs="detailLogs" :visits="detailVisits"
        :owner-options="ownerOptions" @claim="claimSelected" @reactivate="reactivateSelected"
        @save-order="addCustomerOrder" @save="saveDetail(false)" @save-next="saveDetail(true)" />
    </el-drawer>
    <el-drawer v-model="batchDrawerVisible" title="上传批次详情" :size="drawerSize" append-to-body destroy-on-close>
      <div v-loading="batchDetailLoading" class="batch-detail"><template v-if="batchDetail.batch">
          <dl class="batch-meta">
            <dt>直播间</dt>
            <dd>{{ batchDetail.batch.matchedRoomName || batchDetail.batch.roomName || '-' }}</dd>
            <dt>版本</dt>
            <dd>{{ batchDetail.batch.isCurrent ? '当前有效' : '历史版本' }}</dd>
            <dt>评论榜日期</dt>
            <dd>{{ batchDetail.batch.commentDataDate || '-' }}</dd>
            <dt>观看榜日期</dt>
            <dd>{{ batchDetail.batch.watchDataDate || '-' }}</dd>
            <dt>上传时间</dt>
            <dd>{{ batchDetail.batch.capturedAt || '-' }}</dd>
            <dt>合并人数</dt>
            <dd>{{ integer(batchDetail.batch.uniqueUserCount) }} 人</dd>
          </dl>
          <div class="drawer-section-title">
            <h3>本批客户</h3><span>{{ batchDetail.snapshots?.length || 0 }} 人</span>
          </div><el-table :data="batchDetail.snapshots || []" max-height="520"><el-table-column label="客户"
              min-width="180" prop="nickname" /><el-table-column label="评论" min-width="110"><template
                #default="{ row }">{{ rankLabel(row.commentRank) }}</template></el-table-column><el-table-column
              label="观看" min-width="110"><template #default="{ row }">{{ rankLabel(row.watchRank)
              }}</template></el-table-column><el-table-column label="操作" width="88" align="right"><template
                #default="{ row }"><el-button v-if="row.followupId" link type="primary"
                  @click="openRankCustomer(row)">客户档案</el-button></template></el-table-column></el-table>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, getCurrentInstance, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowDown, ArrowRight, CopyDocument, Download, Filter, Link as LinkIcon, Refresh, Search, UserFilled } from '@element-plus/icons-vue'
import { checkPermi } from '@/utils/permission'
import useUserStore from '@/store/modules/user'
import FollowupEditor from './FollowupEditor.vue'
import TeamDashboard from './TeamDashboard.vue'
import { batchUpdateAudienceFollowups, claimAudienceFollowup, getAudienceFollowup, getAudienceFollowupSummary, getAudienceRankBatch, getAudienceRankSummary, listAudienceFollowupAssignees, listAudienceFollowupLogs, listAudienceFollowupRooms, listAudienceFollowupVisits, listAudienceFollowups, listAudienceRankBatches, listAudienceRanks, reactivateAudienceFollowup, saveAudienceCustomerOrder, saveAudienceFollowup } from '@/api/live/audienceRank'

const { proxy } = getCurrentInstance()
const router = useRouter()
const userStore = useUserStore()
const canViewFollowups = checkPermi(['live:audienceRank:followup:list', 'live:audienceRank:followup:query'])
const canViewAllFollowups = checkPermi(['live:audienceRank:followup:assign'])
const canEditFollowups = checkPermi(['live:audienceRank:followup:edit'])
const canAssignFollowups = checkPermi(['live:audienceRank:followup:assign'])
const canClaimFollowups = checkPermi(['live:audienceRank:followup:assign', 'live:audienceRank:followup:edit'])
const canViewHistory = checkPermi(['live:audienceRank:followup:history'])
const canExportFollowups = checkPermi(['live:audienceRank:followup:export'])
const canViewRanks = checkPermi(['live:audienceRank:list'])
const currentUserId = computed(() => Number(userStore.id || userStore.userId || 0))
const stageOptions = [{ value: 'OBSERVING', label: '观察中' }, { value: 'UNASSIGNED', label: '待领取' }, { value: 'FOLLOWING', label: '跟进中' }, { value: 'DEAL_PENDING', label: '待成交' }, { value: 'ORDERED', label: '已下单' }, { value: 'ENDED', label: '已结束' }]

const activeSection = ref(canViewFollowups ? 'workbench' : (canViewRanks ? 'records' : ''))
const viewportWidth = ref(window.innerWidth)
const isCompact = computed(() => viewportWidth.value < 1180)
const roomOptions = ref([]), ownerOptions = ref([])
const taskKey = ref('today'), workLoading = ref(false), summaryLoading = ref(false), workRows = ref([]), workTotal = ref(0), summaryRows = ref([]), priorityTotal = ref(0), mineTotal = ref(0)
const workQuery = ref({ pageNum: 1, pageSize: 20, keyword: '', roomId: null, stage: '', ownerUserId: null })
const customerLoading = ref(false), customerRows = ref([]), customerTotal = ref(0), showCustomerFilters = ref(false), batchMode = ref(false), selectedCustomerIds = ref([]), batchOwnerUserId = ref(null), batchNextFollowAt = ref(null), batchSaving = ref(false)
const customerPreset = ref('all')
const customerPresets = [{ key: 'all', label: '全部' }, { key: 'pending', label: '跟进中' }, { key: 'repeat', label: '重复到访' }, { key: 'high', label: '高意向' }, { key: 'ordered', label: '已下单' }, { key: 'observing', label: '观察中' }, { key: 'ended', label: '已结束' }]
const customerQuery = ref({ pageNum: 1, pageSize: 20, keyword: '', roomId: null, stage: '', ownerUserId: null, isFollower: null, isFollowing: null, priority: null, intentLevel: '', hasOrder: null, repeatVisit: null, uncontacted: null, claimed: null, contacted: null, minPayLevel: null, maxCommentRank: null, maxWatchRank: null, qualified: null })
const selectedDetail = ref({}), detailBaseline = ref(''), detailLoading = ref(false), savingDetail = ref(false), detailLogs = ref([]), detailVisits = ref([]), detailAnchorOptions = ref([]), detailControllerOptions = ref([]), detailDrawerVisible = ref(false), detailSource = ref('workbench')
const teamDashboardRef = ref(null)
const recordMode = ref('batches'), recordLoading = ref(false), batchRows = ref([]), batchTotal = ref(0), batchDateRange = ref([]), rankRows = ref([]), rankTotal = ref(0), rankDateRange = ref([]), batchDrawerVisible = ref(false), batchDetailLoading = ref(false), batchDetail = ref({ batch: null, snapshots: [] })
const syncSummary = ref({})
const batchQuery = ref({ pageNum: 1, pageSize: 20, roomName: '', currentOnly: true, needsAttention: false })
const rankQuery = ref({ pageNum: 1, pageSize: 20, keyword: '', roomName: '', rankType: '' })
const versionOptions = [{ label: '当前版本', value: true }, { label: '全部版本', value: false }]

const sectionHint = computed(() => ({ workbench: '今天只处理真正需要联系的客户', customers: '查询客户档案、商机、订单和到访历史', team: '处理团队积压、异常和进客分配规则', records: '正常数据自动同步，只有异常需要处理' })[activeSection.value])
const drawerSize = computed(() => viewportWidth.value < 760 ? '100%' : '720px')
const activeLoading = computed(() => activeSection.value === 'workbench' ? workLoading.value : (activeSection.value === 'customers' ? customerLoading.value : (activeSection.value === 'records' ? recordLoading.value : false)))
const detailDirty = computed(() => Boolean(detailBaseline.value) && serializeDetail(selectedDetail.value) !== detailBaseline.value)
const selectedMode = computed(() => { const row = selectedDetail.value; if (!row?.followupId || ['OBSERVING', 'UNASSIGNED'].includes(row.status)) return 'view'; if (canEditFollowups && (isMine(row) || canAssignFollowups)) return 'edit'; return canAssignFollowups ? 'assign' : 'view' })
const drawerTitle = computed(() => selectedDetail.value?.nicknameSnapshot ? `客户档案 · ${selectedDetail.value.nicknameSnapshot}` : '客户档案')
const summaryMap = computed(() => { const map = new Map(); for (const row of summaryRows.value || []) map.set(String(row.status || '').toUpperCase(), row); return map })
const todayTotal = computed(() => sumSummary('todayCount', 'today_count')), overdueTotal = computed(() => sumSummary('overdueCount', 'overdue_count')), unassignedTotal = computed(() => statusTotal('UNASSIGNED')), observingTotal = computed(() => statusTotal('OBSERVING'))
const taskMetrics = computed(() => [
  { key: 'today', label: '今日待跟进', value: todayTotal.value, hint: '今天应联系的客户' },
  { key: 'overdue', label: '已逾期', value: overdueTotal.value, hint: '优先完成这些任务' },
  { key: 'priority', label: '重点客户', value: priorityTotal.value, hint: '已人工标记为重点' },
  { key: 'unassigned', label: '待领取', value: unassignedTotal.value, hint: '尚未分配负责人' }
])
const customerAdvancedCount = computed(() => ['stage', 'isFollower', 'isFollowing', 'priority', 'intentLevel', 'hasOrder', 'minPayLevel', 'maxCommentRank', 'maxWatchRank'].reduce((count, key) => count + Number(customerQuery.value[key] !== null && customerQuery.value[key] !== ''), 0))
const hasCustomerFilters = computed(() => ['keyword', 'roomId', 'stage', 'ownerUserId', 'isFollower', 'isFollowing', 'priority', 'intentLevel', 'hasOrder', 'repeatVisit', 'uncontacted', 'claimed', 'contacted', 'minPayLevel', 'maxCommentRank', 'maxWatchRank', 'overdue', 'beginDate', 'endDate'].some(key => customerQuery.value[key] !== null && customerQuery.value[key] !== ''))
const hasWorkFilters = computed(() => Boolean(workQuery.value.keyword || workQuery.value.roomId || workQuery.value.stage || workQuery.value.ownerUserId))
let workSearchTimer, customerSearchTimer

watch(recordMode, value => { if (activeSection.value === 'records') value === 'batches' ? loadBatches() : loadRanks() })
onMounted(async () => { window.addEventListener('resize', handleResize); if (canViewRanks) await loadSyncSummary(); if (canViewFollowups) { await loadSharedOptions(); await loadWorkbenchSummary(); taskKey.value = overdueTotal.value > 0 ? 'overdue' : (todayTotal.value > 0 ? 'today' : (priorityTotal.value > 0 ? 'priority' : (unassignedTotal.value > 0 ? 'unassigned' : 'today'))); await loadWorkbench() } else if (canViewRanks) await loadBatches() })
onBeforeUnmount(() => { window.removeEventListener('resize', handleResize); clearTimeout(workSearchTimer); clearTimeout(customerSearchTimer) })

function handleResize() { viewportWidth.value = window.innerWidth }
function responseData(response, fallback = []) { return response?.data ?? fallback }
function integer(value) { return Number(value || 0).toLocaleString('zh-CN') }
function rankLabel(value) { return value == null ? '-' : `第 ${value} 名` }
function shortTime(value) { return value ? String(value).replace(/^\d{4}-/, '') : '-' }
function stageCode(status) { if (status === 'OBSERVING') return 'OBSERVING'; if (status === 'UNASSIGNED') return 'UNASSIGNED'; if (['PENDING', 'CONTACTED'].includes(status)) return 'FOLLOWING'; if (['QUALIFIED', 'QUOTED', 'ORDER_PENDING', 'PAUSED'].includes(status)) return 'DEAL_PENDING'; if (status === 'ORDERED') return 'ORDERED'; return ['CLOSED', 'INVALID'].includes(status) ? 'ENDED' : 'FOLLOWING' }
function stageLabel(status) { return stageOptions.find(item => item.value === stageCode(status))?.label || '-' }
function intentLabel(value) { return ({ HIGH: '高', MEDIUM: '中', LOW: '低', UNKNOWN: '未知' })[value] || '未知' }
function statusTone(value) { return ['CLOSED', 'ORDERED'].includes(value) ? 'success' : (['OBSERVING', 'INVALID'].includes(value) ? 'muted' : (value === 'UNASSIGNED' ? 'pending' : 'active')) }
function taskLabel(value) { return taskMetrics.value.find(item => item.key === value)?.label || '当前任务' }
function shortSyncTime(value) { const text = String(value || ''); return text ? text.slice(11, 16) || text : '--:--' }
function roomLabel(room) { return room.roomName || room.liveAccount || room.roomCode || `直播间 ${room.roomId}` }
function personId(person) { return Number(person.userId ?? person.user_id ?? person.staffId ?? person.id) }
function personLabel(person) { const name = person.userName || person.user_name || person.nickName || person.name || person.account || '未命名账号'; const account = person.account || person.mobile || ''; return account && account !== name ? `${name}（${account}）` : name }
function sumSummary(...keys) { return [...summaryMap.value.values()].reduce((sum, row) => sum + Number(keys.map(key => row[key]).find(value => value != null) || 0), 0) }
function statusTotal(status) { const row = summaryMap.value.get(status) || {}; return Number(row.totalCount ?? row.total_count ?? 0) }
function relationSummary(row) { return [row.isFollower ? '粉丝' : '非粉丝', row.isFollowing ? '已回关' : '未回关', row.payLevel == null ? '' : `消费 ${row.payLevel} 级`].filter(Boolean).join(' · ') }
function visitSummary(row) { const total = Number(row.appearanceDays || 0), consecutive = Number(row.consecutiveDays || 0); return consecutive > 1 ? `累计 ${total} 天 · 连续 ${consecutive} 天` : (total > 1 ? `累计到访 ${total} 天` : '首次到访') }
function businessSummary(row) { return row.consultModel || row.orderNo ? [row.consultModel ? `咨询 ${row.consultModel}` : '', row.orderNo ? `订单 ${row.orderNo}` : '尚未下单'].filter(Boolean).join(' · ') : `评论 ${rankLabel(row.commentRank)} · 观看 ${rankLabel(row.watchRank)}` }
function tableRankLabel(value) { return Number(value) > 0 ? `第 ${Number(value)} 名` : '未上榜' }
function priorityReasons(row) {
  const reasons = []
  if (row.priority) reasons.push('人工重点')
  if (Number(row.consecutiveDays || 0) >= 2) reasons.push(`连续 ${integer(row.consecutiveDays)} 天到访`)
  else if (Number(row.appearanceDays || 0) >= 2) reasons.push(`累计 ${integer(row.appearanceDays)} 天到访`)
  if (Number(row.bestWatchRank || 0) > 0 && Number(row.bestWatchRank) <= 10) reasons.push(`观看 TOP ${row.bestWatchRank}`)
  if (Number(row.bestCommentRank || 0) > 0 && Number(row.bestCommentRank) <= 10) reasons.push(`评论 TOP ${row.bestCommentRank}`)
  if (Number(row.payLevel || 0) >= 10) reasons.push(`消费 ${row.payLevel} 级`)
  if (row.intentLevel === 'HIGH' && !['ORDERED', 'CLOSED', 'INVALID'].includes(row.status)) reasons.push('高意向待推进')
  if (isOverdue(row)) reasons.push('已逾期')
  return reasons.slice(0, 3)
}
function primaryReason(row) { if (row.reactivationPending) return '再次到访，建议重新激活'; return row.qualificationReason || priorityReasons(row)[0] || businessSummary(row) }
function presetCount(key) { if (key === 'all') return customerPreset.value === key ? integer(customerTotal.value) : ''; const map = { pending: mineTotal.value, high: priorityTotal.value, observing: observingTotal.value }; return map[key] == null ? '' : integer(map[key]) }
function isOverdue(row) { return Boolean(row?.nextFollowAt && !['ORDERED', 'CLOSED', 'INVALID'].includes(row.status) && new Date(String(row.nextFollowAt).replace(' ', 'T')).getTime() < Date.now()) }
function dueText(row) { if (row.status === 'OBSERVING') return '等待有效信号'; if (row.status === 'UNASSIGNED') return '等待领取'; if (['ORDERED', 'CLOSED', 'INVALID'].includes(row.status)) return row.status === 'ORDERED' ? '已下单' : (row.status === 'CLOSED' ? '已结束' : '不再跟进'); if (!row.nextFollowAt) return '待安排时间'; return isOverdue(row) ? `已逾期 · ${shortTime(row.nextFollowAt)}` : `下次 ${shortTime(row.nextFollowAt)}` }
function formatDuration(value) { const seconds = Number(value); if (!Number.isFinite(seconds) || seconds < 0) return '未上榜'; const hours = Math.floor(seconds / 3600), minutes = Math.floor((seconds % 3600) / 60); return hours ? `${hours} 小时 ${minutes} 分` : `${minutes} 分钟` }
function isMine(row) { return Number(row?.ownerUserId || 0) === currentUserId.value }
function canEditRow(row) { return canEditFollowups && (isMine(row) || canAssignFollowups) }
function dedupePeople(rows) { const seen = new Set(); return rows.filter(row => { const id = personId(row); if (!id || seen.has(id)) return false; seen.add(id); return true }) }

async function loadSharedOptions() { try { const [rooms, owners] = await Promise.all([listAudienceFollowupRooms(), listAudienceFollowupAssignees(null, 'owner')]); roomOptions.value = responseData(rooms, []) || []; ownerOptions.value = dedupePeople(responseData(owners, []) || []) } catch { roomOptions.value = []; ownerOptions.value = [] } }
function workParams() { const query = { ...workQuery.value, onlyMine: true, excludeTerminal: true }; if (taskKey.value === 'today') { query.todayDue = true; query.mineAssigned = true } else if (taskKey.value === 'overdue') { query.overdue = true; query.mineAssigned = true } else if (taskKey.value === 'unassigned') query.status = 'UNASSIGNED'; else if (taskKey.value === 'priority') { query.priority = true; query.mineAssigned = true } else query.mineAssigned = true; return query }
async function loadWorkbench(reset = false) { if (!canViewFollowups) return; if (reset === true) workQuery.value.pageNum = 1; workLoading.value = true; try { const response = await listAudienceFollowups(workParams()); workRows.value = response?.rows || []; workTotal.value = Number(response?.total || 0) } finally { workLoading.value = false } }
async function loadWorkbenchSummary() { summaryLoading.value = true; try { const [base, priority, mine] = await Promise.all([getAudienceFollowupSummary({ onlyMine: true }), getAudienceFollowupSummary({ onlyMine: true, mineAssigned: true, priority: true, excludeTerminal: true }), getAudienceFollowupSummary({ onlyMine: true, mineAssigned: true, excludeTerminal: true })]); summaryRows.value = responseData(base, []) || []; priorityTotal.value = totalSummary(responseData(priority, []) || []); mineTotal.value = totalSummary(responseData(mine, []) || []) } finally { summaryLoading.value = false } }
function totalSummary(rows) { return rows.reduce((sum, row) => sum + Number(row.totalCount ?? row.total_count ?? 0), 0) }
async function selectTask(key) { if (taskKey.value === key) return; if (detailDirty.value && !await confirmDiscard()) return; taskKey.value = key; clearDetail(); await loadWorkbench(true) }
function scheduleWorkSearch() { clearTimeout(workSearchTimer); workSearchTimer = setTimeout(() => loadWorkbench(true), 280) }
function resetWorkExtraFilters() { workQuery.value.stage = ''; workQuery.value.ownerUserId = null; loadWorkbench(true) }
function resetWorkFilters() { workQuery.value.keyword = ''; workQuery.value.roomId = null; resetWorkExtraFilters() }

async function openWorkbenchCustomer(row) { if (detailDirty.value && selectedDetail.value.followupId !== row.followupId && !await confirmDiscard()) return; await loadDetail(row, 'workbench', true) }
async function processNextCustomer() { if (workRows.value.length) await openWorkbenchCustomer(workRows.value[0]) }
async function openLibraryCustomer(row) { if (detailDirty.value && !await confirmDiscard()) return; await loadDetail(row, 'library', true) }
async function openRankCustomer(row) { batchDrawerVisible.value = false; if (row.followupId) await loadDetail({ followupId: row.followupId }, 'library', true) }
async function loadDetail(row, source, openDrawer) { if (!row?.followupId) return; detailSource.value = source; if (openDrawer) detailDrawerVisible.value = true; detailLoading.value = true; detailLogs.value = []; detailVisits.value = []; try { selectedDetail.value = normalizeDetail(responseData(await getAudienceFollowup(row.followupId), row)); const requests = canViewHistory ? [listAudienceFollowupLogs(row.followupId), listAudienceFollowupVisits(row.followupId)] : []; const results = await Promise.all(requests); if (canViewHistory) { detailLogs.value = responseData(results[0], []) || []; detailVisits.value = responseData(results[1], []) || [] } await nextTick(); detailBaseline.value = serializeDetail(selectedDetail.value) } catch { if (openDrawer) detailDrawerVisible.value = false } finally { detailLoading.value = false } }
function normalizeDetail(value) { return { ...value, ownerUserId: value.ownerUserId == null ? null : Number(value.ownerUserId), anchorUserId: value.anchorUserId == null ? null : Number(value.anchorUserId), controllerUserId: value.controllerUserId == null ? null : Number(value.controllerUserId), priority: Boolean(value.priority), reactivationPending: Boolean(value.reactivationPending), followResultCode: value.followResultCode || '', intentLevel: value.intentLevel || 'UNKNOWN', closeReasonCode: value.closeReasonCode || '', lastFollowResult: value.lastFollowResult || '', consultModel: value.consultModel || '', orderNo: value.orderNo || '', contactPhone: value.contactPhone || '', contactWechat: value.contactWechat || '', remark: value.remark || '', closeReason: value.closeReason || '', nextFollowAt: value.nextFollowAt || null, opportunities: value.opportunities || [], orders: value.orders || [] } }
function serializeDetail(value) { return !value?.followupId ? '' : JSON.stringify({ status: value.status, followResultCode: value.followResultCode || '', intentLevel: value.intentLevel || 'UNKNOWN', closeReasonCode: value.closeReasonCode || '', lastFollowResult: value.lastFollowResult || '', nextFollowAt: value.nextFollowAt || null, consultModel: value.consultModel || '', orderNo: value.orderNo || '', contactPhone: value.contactPhone || '', contactWechat: value.contactWechat || '', remark: value.remark || '', closeReason: value.closeReason || '', priority: Boolean(value.priority), ownerUserId: value.ownerUserId ?? null }) }
function clearDetail() { selectedDetail.value = {}; detailBaseline.value = ''; detailLogs.value = []; detailVisits.value = []; detailAnchorOptions.value = []; detailControllerOptions.value = [] }
async function confirmDiscard() { try { await proxy.$modal.confirm('当前修改还没有保存，确定放弃吗？'); return true } catch { return false } }
function validateDetail() { const value = selectedDetail.value; if (!value.followResultCode) return '请选择本次跟进结果'; if (['ORDERED', 'CLOSED'].includes(value.status) && !String(value.orderNo || '').trim()) return '已下单必须填写订单号'; if (value.status === 'INVALID' && !value.closeReasonCode && !String(value.closeReason || '').trim()) return '请选择无效原因'; if (!['ORDERED', 'CLOSED', 'INVALID'].includes(value.status) && !value.nextFollowAt) return '请安排下次跟进时间'; return '' }
async function saveDetail(openNext) {
  if (!selectedDetail.value.followupId) return
  const validation = selectedMode.value === 'assign' ? '' : validateDetail()
  if (validation) return proxy.$modal.msgWarning(validation)
  const id = selectedDetail.value.followupId
  const index = workRows.value.findIndex(row => row.followupId === id)
  const nextRow = openNext && index >= 0 ? workRows.value.slice(index + 1).find(row => row.status !== 'UNASSIGNED') : null
  savingDetail.value = true
  try {
    if (selectedMode.value === 'assign' && !canEditFollowups) {
      await batchUpdateAudienceFollowups({ followupIds: [id], changes: { ownerUserId: selectedDetail.value.ownerUserId, anchorUserId: selectedDetail.value.anchorUserId, controllerUserId: selectedDetail.value.controllerUserId } })
    } else {
      const payload = { ...selectedDetail.value }
      if (String(payload.lastFollowResult || '').trim()) payload.lastContactAt = nowText()
      await saveAudienceFollowup(payload)
    }
    proxy.$modal.msgSuccess(selectedMode.value === 'assign' ? '人员归属已保存' : '本次跟进已保存')
    detailBaseline.value = serializeDetail(selectedDetail.value)
    await Promise.all([loadWorkbenchSummary(), loadWorkbench(), activeSection.value === 'customers' ? loadCustomers() : Promise.resolve()])
    if (openNext && nextRow) await loadDetail(nextRow, 'workbench', true)
    else if (openNext) proxy.$modal.msgSuccess('当前列表已处理到最后一位')
    else await loadDetail({ followupId: id }, detailSource.value, detailDrawerVisible.value)
  } catch (error) {
    if (Number(error?.response?.status || error?.status) === 409) {
      proxy.$modal.msgWarning('这位客户刚刚被其他人更新，已为你加载最新资料')
      await loadDetail({ followupId: id }, detailSource.value, detailDrawerVisible.value)
    }
  } finally {
    savingDetail.value = false
  }
}
function nowText() { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:00` }
async function claimRow(row, source = 'workbench') { await claimAudienceFollowup(row.followupId); proxy.$modal.msgSuccess(row.status === 'OBSERVING' ? '已加入我的跟进并安排下次联系' : '已领取，系统已安排下一次跟进'); await Promise.all([loadWorkbenchSummary(), loadWorkbench(), activeSection.value === 'customers' ? loadCustomers() : Promise.resolve()]); await loadDetail(row, source, true) }
async function claimSelected() { if (selectedDetail.value.followupId) await claimRow(selectedDetail.value, detailSource.value) }
async function reactivateSelected() { if (!selectedDetail.value.followupId) return; savingDetail.value = true; try { const response = await reactivateAudienceFollowup(selectedDetail.value.followupId); selectedDetail.value = normalizeDetail(responseData(response, selectedDetail.value)); proxy.$modal.msgSuccess('已开启新的跟进商机'); await Promise.all([loadWorkbenchSummary(), loadWorkbench(), loadCustomers()]) } finally { savingDetail.value = false } }
async function addCustomerOrder(order) { if (!selectedDetail.value.followupId) return; savingDetail.value = true; try { await saveAudienceCustomerOrder(selectedDetail.value.followupId, { ...order, productModel: selectedDetail.value.consultModel || '' }); proxy.$modal.msgSuccess('订单已添加'); await loadDetail({ followupId: selectedDetail.value.followupId }, detailSource.value, detailDrawerVisible.value); await Promise.all([loadWorkbenchSummary(), loadWorkbench(), loadCustomers()]) } finally { savingDetail.value = false } }
function beforeDrawerClose(done) { if (!detailDirty.value || savingDetail.value) done(); else proxy.$modal.confirm('当前修改还没有保存，确定关闭吗？').then(done).catch(() => { }) }

async function loadCustomers(reset = false) { if (reset === true) customerQuery.value.pageNum = 1; customerLoading.value = true; try { const response = await listAudienceFollowups({ ...customerQuery.value, onlyMine: !canViewAllFollowups }); customerRows.value = response?.rows || []; customerTotal.value = Number(response?.total || 0); selectedCustomerIds.value = [] } finally { customerLoading.value = false } }
function applyCustomerPreset(key) { customerPreset.value = key; const base = { stage: '', intentLevel: '', hasOrder: null, overdue: null, repeatVisit: null, uncontacted: null, claimed: null, contacted: null, followResultCode: '', excludeTerminal: null, qualified: null }; if (key === 'pending') { base.excludeTerminal = true; base.claimed = true } else if (key === 'repeat') base.repeatVisit = true; else if (key === 'high') { base.intentLevel = 'HIGH'; base.hasOrder = false; base.excludeTerminal = true } else if (key === 'ordered') base.stage = 'ORDERED'; else if (key === 'observing') base.stage = 'OBSERVING'; else if (key === 'ended') base.stage = 'ENDED'; Object.assign(customerQuery.value, base); loadCustomers(true) }
function toggleBatchMode() { batchMode.value = !batchMode.value; selectedCustomerIds.value = []; batchOwnerUserId.value = null; batchNextFollowAt.value = null }
function scheduleCustomerSearch() { clearTimeout(customerSearchTimer); customerSearchTimer = setTimeout(() => loadCustomers(true), 280) }
function handleCustomerSelection(rows) { selectedCustomerIds.value = rows.map(row => row.followupId) }
async function batchAssignOwner() { if (!selectedCustomerIds.value.length || batchOwnerUserId.value == null) return; batchSaving.value = true; try { await batchUpdateAudienceFollowups({ followupIds: selectedCustomerIds.value, changes: { ownerUserId: batchOwnerUserId.value } }); proxy.$modal.msgSuccess('领取人已批量分配'); await Promise.all([loadCustomers(), loadWorkbenchSummary()]) } finally { batchSaving.value = false } }
async function batchScheduleFollowup() { if (!selectedCustomerIds.value.length || !batchNextFollowAt.value) return; batchSaving.value = true; try { await batchUpdateAudienceFollowups({ followupIds: selectedCustomerIds.value, changes: { nextFollowAt: batchNextFollowAt.value } }); proxy.$modal.msgSuccess('下次跟进时间已统一安排'); batchNextFollowAt.value = null; await Promise.all([loadCustomers(), loadWorkbenchSummary()]) } finally { batchSaving.value = false } }
async function batchSetPriority(priority) { if (!selectedCustomerIds.value.length) return; batchSaving.value = true; try { await batchUpdateAudienceFollowups({ followupIds: selectedCustomerIds.value, changes: { priority } }); proxy.$modal.msgSuccess(priority ? '已设为重点客户' : '已取消重点标记'); await Promise.all([loadCustomers(), loadWorkbenchSummary()]) } finally { batchSaving.value = false } }
function resetCustomerFilters() { customerPreset.value = 'all'; customerQuery.value = { ...customerQuery.value, pageNum: 1, keyword: '', roomId: null, stage: '', ownerUserId: null, isFollower: null, isFollowing: null, priority: null, intentLevel: '', hasOrder: null, repeatVisit: null, uncontacted: null, claimed: null, contacted: null, followResultCode: '', minPayLevel: null, maxCommentRank: null, maxWatchRank: null, overdue: null, excludeTerminal: null, qualified: null, beginDate: null, endDate: null }; loadCustomers(true) }

async function openDashboardCustomers(filters) { customerPreset.value = 'all'; customerQuery.value = { ...customerQuery.value, pageNum: 1, keyword: '', roomId: null, stage: '', ownerUserId: null, isFollower: null, isFollowing: null, priority: null, intentLevel: '', hasOrder: null, repeatVisit: null, uncontacted: null, claimed: null, contacted: null, followResultCode: '', minPayLevel: null, maxCommentRank: null, maxWatchRank: null, overdue: null, excludeTerminal: null, beginDate: null, endDate: null, ...filters }; activeSection.value = 'customers'; await loadCustomers(true) }

function batchParams() { const query = { ...batchQuery.value }; if (batchDateRange.value?.length === 2) [query.beginCapturedAt, query.endCapturedAt] = batchDateRange.value; return query }
async function loadBatches(reset = false) { if (!canViewRanks) return; if (reset === true) batchQuery.value.pageNum = 1; recordLoading.value = true; try { const response = await listAudienceRankBatches(batchParams()); batchRows.value = response?.rows || []; batchTotal.value = Number(response?.total || 0) } finally { recordLoading.value = false } }
async function loadSyncSummary() { if (!canViewRanks) return; syncSummary.value = (await getAudienceRankSummary({}))?.data || {} }
function rankParams() { const query = { ...rankQuery.value }; if (rankDateRange.value?.length === 2) [query.beginDataDate, query.endDataDate] = rankDateRange.value; return query }
async function loadRanks(reset = false) { if (!canViewRanks) return; if (reset === true) rankQuery.value.pageNum = 1; recordLoading.value = true; try { const response = await listAudienceRanks(rankParams()); rankRows.value = response?.rows || []; rankTotal.value = Number(response?.total || 0) } finally { recordLoading.value = false } }
function matchLabel(status) { return ({ MATCHED: '已匹配', UNMATCHED: '未匹配', AMBIGUOUS: '名称重复' })[status] || '需要处理' }
function matchTone(status) { return status === 'MATCHED' ? 'success' : 'warning' }
async function openBatchDetail(row) { batchDrawerVisible.value = true; batchDetailLoading.value = true; batchDetail.value = { batch: null, snapshots: [] }; try { batchDetail.value = responseData(await getAudienceRankBatch(row.batchId), { batch: row, snapshots: [] }) } finally { batchDetailLoading.value = false } }
function goRoomManagement() { router.push('/live-ops/liveSubject') }
async function switchSection(section) { if (detailDirty.value && !await confirmDiscard()) return; activeSection.value = section; if (section === 'workbench') await Promise.all([loadWorkbenchSummary(), loadWorkbench()]); else if (section === 'customers') await loadCustomers(); else if (section === 'team') await nextTick(() => teamDashboardRef.value?.load()); else await Promise.all([loadBatches(), loadSyncSummary()]) }
function refreshActive() { if (activeSection.value === 'workbench') return Promise.all([loadWorkbench(), loadWorkbenchSummary()]); if (activeSection.value === 'customers') return loadCustomers(); if (activeSection.value === 'team') return teamDashboardRef.value?.load(); return Promise.all([loadBatches(), loadSyncSummary()]) }
function exportCustomers() { proxy.download('/live/audience-rank/followup/export', { ...customerQuery.value, onlyMine: !canViewAllFollowups }, `观众客户库_${Date.now()}.xlsx`) }
function openProfile(secUid) { const value = String(secUid || '').trim(); if (value) window.open(`https://www.douyin.com/user/${encodeURIComponent(value)}`, '_blank', 'noopener,noreferrer') }
async function copyUid(secUid) { const value = String(secUid || '').trim(); if (!value) return; try { await navigator.clipboard.writeText(value); proxy.$modal.msgSuccess('用户标识已复制') } catch { proxy.$modal.msgError('复制失败') } }
</script>

<style scoped lang="scss">
.audience-page {
  --line: #e1e5ea;
  --ink: #182230;
  --muted: #697586;
  --blue: #245f9f;
  min-height: calc(100vh - 84px);
  padding: 18px 22px 30px;
  color: var(--ink);
  background: #f3f5f7;
}

.page-head {
  display: flex;
  min-height: 54px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.title-block {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 13px;
}

.title-block h2 {
  margin: 0;
  font-size: 21px;
  line-height: 1.35;
  letter-spacing: 0;
}

.title-block span {
  color: var(--muted);
  font-size: 12px;
}

.head-actions {
  display: flex;
  gap: 8px;
}

.section-nav {
  display: flex;
  min-height: 49px;
  align-items: flex-end;
  justify-content: space-between;
  gap: 14px;
  padding: 0 15px;
  border: 1px solid var(--line);
  border-bottom: 0;
  border-radius: 6px 6px 0 0;
  background: #fff;
}

.section-tabs {
  display: flex;
  gap: 24px;
}

.section-tabs button {
  position: relative;
  height: 48px;
  padding: 0 2px;
  border: 0;
  color: #667085;
  background: transparent;
  cursor: pointer;
}

.section-tabs button.active {
  color: var(--ink);
  font-weight: 650;
}

.section-tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: var(--blue);
  content: '';
}

.today-summary {
  display: flex;
  min-height: 48px;
  align-items: center;
  gap: 15px;
  color: #707a89;
  font-size: 11px;
}

.today-summary b {
  margin-left: 3px;
  color: var(--ink);
  font-size: 13px;
}

.today-summary .danger,
.today-summary .danger b {
  color: #b42318;
}

.workbench {
  display: grid;
  grid-template-columns: 150px minmax(420px, 1.05fr) minmax(430px, .95fr);
  min-height: 660px;
  border: 1px solid var(--line);
  background: #fff;
}

.task-sidebar {
  padding: 14px 10px;
  border-right: 1px solid var(--line);
  background: #f7f8fa;
}

.task-sidebar>small {
  display: block;
  padding: 5px 10px 9px;
  color: #8992a0;
  font-size: 11px;
}

.task-sidebar button {
  display: flex;
  width: 100%;
  min-height: 41px;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
  padding: 8px 10px;
  border: 0;
  border-radius: 4px;
  color: #5f6b7b;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.task-sidebar button:hover {
  background: #eef1f4;
}

.task-sidebar button.active {
  color: #194f87;
  background: #fff;
  box-shadow: inset 3px 0 #3d78b4, 0 1px 2px rgb(16 24 40 / 7%);
  font-weight: 600;
}

.task-sidebar b {
  font-weight: 600;
}

.task-sidebar .danger {
  color: #b42318;
}

.queue-pane {
  min-width: 0;
  border-right: 1px solid var(--line);
}

.queue-toolbar,
.customer-toolbar,
.record-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.queue-toolbar {
  min-height: 55px;
  padding: 9px 11px;
  border-bottom: 1px solid var(--line);
}

.queue-toolbar> :deep(.el-input) {
  min-width: 180px;
  flex: 1;
}

.queue-toolbar> :deep(.el-select) {
  width: 150px;
}

.quick-filter-popover label {
  display: block;
  margin: 10px 0 5px;
  color: #667085;
  font-size: 11px;
}

.quick-filter-popover label:first-child {
  margin-top: 0;
}

.quick-filter-popover :deep(.el-button) {
  margin-top: 8px;
}

.full-field {
  width: 100%;
}

.queue-meta {
  display: flex;
  min-height: 35px;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 0 12px;
  color: #667085;
  font-size: 11px;
}

.queue-meta small {
  color: #98a2b3;
}

.customer-queue {
  min-height: 500px;
}

.customer-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px;
  gap: 12px;
  min-height: 92px;
  padding: 13px 14px;
  border-top: 1px solid #edf0f3;
  cursor: pointer;
}

.customer-row:hover {
  background: #fafbfd;
}

.customer-row.selected {
  background: #edf5fc;
  box-shadow: inset 3px 0 #3f78b7;
}

.name-line {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 6px;
}

.name-line strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.priority-label {
  flex: none;
  padding: 1px 5px;
  border: 1px solid #ebc273;
  border-radius: 3px;
  color: #8a5200;
  background: #fff9ec;
  font-size: 10px;
}

.relation-line {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 6px;
  color: #778190;
  font-size: 10px;
}

.relation-line span+span::before {
  margin-right: 4px;
  color: #c4c9d1;
  content: '·';
}

.row-main p {
  margin: 9px 0 0;
  overflow: hidden;
  color: #596575;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-side {
  display: flex;
  align-items: flex-end;
  flex-direction: column;
  gap: 4px;
  text-align: right;
}

.row-side small {
  max-width: 118px;
  overflow: hidden;
  color: #8992a0;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-side .overdue,
.overdue {
  color: #b42318 !important;
  font-weight: 600;
}

.status-name {
  font-size: 11px;
  font-weight: 650;
}

.status-name.active {
  color: #2563a9;
}

.status-name.success {
  color: #16794b;
}

.status-name.pending {
  color: #8a5b12;
}

.status-name.muted {
  color: #7a8493;
}

.queue-pagination {
  justify-content: flex-end;
  padding: 12px;
  border-top: 1px solid var(--line);
}

.detail-pane {
  min-width: 0;
  max-height: calc(100vh - 190px);
  overflow-y: auto;
  padding: 15px;
  scrollbar-width: thin;
}

.empty-state {
  display: grid;
  min-height: 190px;
  place-content: center;
  color: #7f8896;
  text-align: center;
}

.empty-state p {
  margin: 0 0 6px;
}

.customers-page,
.records-page {
  padding: 0 16px 24px;
  border: 1px solid var(--line);
  background: #fff;
}

.customer-presets {
  display: flex;
  min-height: 53px;
  align-items: flex-end;
  gap: 20px;
  border-bottom: 1px solid #e8ebef;
}

.customer-presets button {
  position: relative;
  height: 42px;
  padding: 0 1px;
  border: 0;
  color: #697586;
  background: transparent;
  cursor: pointer;
}

.customer-presets button.active {
  color: #1d568f;
  font-weight: 600;
}

.customer-presets button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: #3475b6;
  content: '';
}

.customer-toolbar {
  flex-wrap: wrap;
  padding-top: 14px;
  margin-bottom: 10px;
}

.customer-toolbar> :deep(.el-input) {
  width: min(300px, 25vw);
}

.customer-toolbar> :deep(.el-select) {
  width: 160px;
}

.filter-active {
  color: var(--blue);
  border-color: #9dbce0;
  background: #f5f9fd;
}

.customer-more-filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 9px;
  margin-bottom: 11px;
  padding: 10px;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: #fafbfc;
}

.customer-more-filters> :deep(.el-select) {
  width: 150px;
}

.level-filter {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #667085;
  font-size: 11px;
}

.level-filter :deep(.el-input-number) {
  width: 120px;
}

.rank-filter {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #667085;
  font-size: 11px;
  white-space: nowrap;
}

.rank-filter :deep(.el-input-number) {
  width: 105px;
}

.rank-value {
  color: #9a5c19;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.rank-value.empty {
  color: #a0a7b2;
  font-weight: 400;
}

.batch-bar {
  display: flex;
  flex-wrap: wrap;
  min-height: 51px;
  align-items: center;
  gap: 8px;
  margin: 8px 0;
  padding: 8px 10px;
  border: 1px solid #bfd4eb;
  border-radius: 5px;
  background: #f4f8fd;
}

.batch-bar span {
  margin-right: auto;
  color: #285a8f;
  font-size: 12px;
}

.batch-bar> :deep(.el-select) {
  width: 190px;
}

.customer-count {
  min-height: 34px;
  color: #667085;
  font-size: 11px;
}

.customer-count strong {
  color: var(--ink);
}

.customer-table,
.record-table {
  width: 100%;
  border-top: 1px solid var(--line);
}

.table-customer {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 5px;
}

.table-customer small,
.cell-sub {
  display: block;
  max-width: 100%;
  margin-top: 4px;
  overflow: hidden;
  color: #8992a0;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-view-switch {
  display: inline-flex;
  margin-bottom: 13px;
  padding: 3px;
  border: 1px solid var(--line);
  border-radius: 5px;
  background: #f5f6f8;
}

.record-view-switch button {
  min-width: 92px;
  height: 31px;
  padding: 0 13px;
  border: 0;
  border-radius: 3px;
  color: #667085;
  background: transparent;
  cursor: pointer;
}

.record-view-switch button.active {
  color: var(--ink);
  background: #fff;
  box-shadow: 0 1px 2px rgb(16 24 40 / 9%);
  font-weight: 600;
}

.record-toolbar {
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.record-detail-head {
  display: flex;
  min-height: 45px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.record-detail-head strong {
  font-size: 14px;
  font-weight: 650;
}

.record-detail-head span {
  color: #7b8492;
  font-size: 11px;
}

.record-toolbar> :deep(.el-input) {
  width: 230px;
}

.record-toolbar> :deep(.el-date-editor) {
  width: 260px;
}

.rank-toolbar> :deep(.el-input) {
  width: 210px;
}

.rank-toolbar> :deep(.el-select) {
  width: 150px;
}

.version-state,
.match-state {
  font-size: 11px;
  font-weight: 600;
}

.version-state {
  color: #16794b;
}

.version-state.history {
  color: #8992a0;
}

.match-state.success {
  color: #16794b;
}

.match-state.warning {
  color: #9a620f;
}

.rank-customer {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 5px;
}

.rank-customer strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-customer span {
  display: inline-flex;
  flex: none;
}

.rank-customer :deep(.el-button) {
  width: 24px;
  height: 24px;
  margin: 0;
  padding: 0;
}

.muted-text {
  color: #98a2b3;
  font-size: 11px;
}

.batch-meta {
  display: grid;
  grid-template-columns: 95px minmax(0, 1fr);
  margin: 0;
  border-top: 1px solid var(--line);
}

.batch-meta dt,
.batch-meta dd {
  margin: 0;
  padding: 10px 4px;
  border-bottom: 1px solid var(--line);
}

.batch-meta dt {
  color: #7c8695;
}

.batch-meta dd {
  overflow-wrap: anywhere;
}

.drawer-section-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin: 18px 0 10px;
}

.drawer-section-title h3 {
  margin: 0;
  font-size: 15px;
}

.drawer-section-title span {
  color: #8992a0;
  font-size: 11px;
}

.records-page {
  padding-top: 0;
}

.sync-overview {
  display: grid;
  grid-template-columns: 1.3fr repeat(3, minmax(0, .7fr));
  margin-bottom: 14px;
  border-bottom: 1px solid #e8ebef;
}

.sync-overview>div {
  min-width: 0;
  padding: 15px 14px;
  border-right: 1px solid #edf0f3;
}

.sync-overview>div:first-child {
  padding-left: 2px;
}

.sync-overview>div:last-child {
  border-right: 0;
}

.sync-overview span,
.sync-overview strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sync-overview span {
  color: #7b8492;
  font-size: 10px;
}

.sync-overview strong {
  margin-top: 5px;
  font-size: 13px;
}

.sync-overview .warning strong {
  color: #b42318;
}

@media (max-width: 1179px) {
  .workbench {
    grid-template-columns: 132px minmax(0, 1fr);
  }

  .queue-pane {
    border-right: 0;
  }

  .customer-toolbar> :deep(.el-input) {
    width: min(360px, 40vw);
  }
}

@media (max-width: 760px) {
  .audience-page {
    padding: 12px 10px 22px;
  }

  .page-head {
    align-items: stretch;
    flex-direction: column;
    gap: 10px;
    padding-bottom: 10px;
  }

  .title-block {
    align-items: flex-start;
    flex-direction: column;
    gap: 2px;
  }

  .title-block h2 {
    font-size: 19px;
  }

  .head-actions {
    justify-content: flex-start;
  }

  .section-nav {
    align-items: flex-start;
    flex-direction: column;
    padding: 0 10px;
  }

  .section-tabs {
    width: 100%;
    gap: 18px;
    overflow-x: auto;
  }

  .section-tabs button {
    flex: none;
  }

  .today-summary {
    min-height: 36px;
    flex-wrap: wrap;
  }

  .workbench {
    grid-template-columns: 1fr;
  }

  .task-sidebar {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    border-right: 0;
    border-bottom: 1px solid var(--line);
  }

  .task-sidebar>small {
    display: none;
  }

  .queue-toolbar {
    flex-wrap: wrap;
  }

  .queue-toolbar> :deep(.el-input),
  .queue-toolbar> :deep(.el-select) {
    width: 100%;
    flex: auto;
  }

  .queue-meta small {
    display: none;
  }

  .customer-row {
    grid-template-columns: minmax(0, 1fr) 105px;
  }

  .customers-page,
  .records-page {
    padding: 11px 10px 18px;
  }

  .customer-presets {
    overflow-x: auto;
  }

  .customer-presets button {
    flex: none;
  }

  .customer-toolbar> :deep(.el-input),
  .customer-toolbar> :deep(.el-select),
  .record-toolbar> :deep(.el-input),
  .record-toolbar> :deep(.el-select),
  .record-toolbar> :deep(.el-date-editor),
  .rank-toolbar> :deep(.el-input) {
    width: 100%;
  }

  .customer-more-filters> :deep(.el-select),
  .level-filter {
    width: 100%;
  }

  .level-filter {
    justify-content: space-between;
  }

  .batch-bar {
    align-items: stretch;
    flex-direction: column;
  }

  .batch-bar span {
    margin: 0;
  }

  .batch-bar> :deep(.el-select) {
    width: 100%;
  }

  .sync-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sync-overview>div:nth-child(2) {
    border-right: 0;
  }

  .sync-overview>div:first-child {
    padding-left: 10px;
  }
}
</style>

<style scoped lang="scss">
/* Compact customer-library rhythm: keep the work surface dense without shrinking controls. */
.audience-page { padding: 12px 14px 24px; }
.library-head { min-height: 60px; }
.customer-presets { min-height: 45px; }
.customer-presets button { height: 38px; }
.customer-toolbar { min-height: 49px; }
.customer-more-filters {
  width: fit-content;
  max-width: 100%;
  margin-bottom: 8px;
  padding: 7px 8px;
}
.customer-count { min-height: 32px; }
.customer-table :deep(.el-table__header th) { height: 40px; }
.customer-table :deep(.el-table__row td) { padding: 8px 0; }
@media (max-width: 760px) {
  .audience-page { padding: 8px 7px 18px; }
  .customer-more-filters { width: 100%; }
  .rank-filter { width: 100%; justify-content: space-between; }
  .rank-filter :deep(.el-input-number) { min-width: 0; flex: 1; width: auto; }
}
</style>

<style scoped lang="scss">
.work-page {
  padding: 0 16px 22px;
  border: 1px solid var(--audience-line);
  background: #fff;
}

.work-head,
.library-head {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--audience-line);
}

.work-head h3,
.library-head h3 {
  margin: 0;
  font-size: 17px;
  font-weight: 650;
  letter-spacing: 0;
}

.work-head p,
.library-head p {
  margin: 5px 0 0;
  color: var(--audience-muted);
  font-size: 12px;
}

.work-tabs {
  display: flex;
  min-height: 48px;
  align-items: flex-end;
  gap: 24px;
  border-bottom: 1px solid var(--audience-line);
}

.work-tabs button {
  position: relative;
  height: 47px;
  padding: 0;
  border: 0;
  color: #697586;
  background: transparent;
  cursor: pointer;
}

.work-tabs button.active {
  color: var(--audience-ink);
  font-weight: 650;
}

.work-tabs button.active::after {
  position: absolute;
  right: 0;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: var(--audience-orange);
  content: '';
}

.work-tabs button.danger b {
  color: #b42318;
}

.work-tabs b {
  margin-left: 5px;
  color: #98a2b3;
  font-size: 11px;
  font-weight: 600;
}

.work-toolbar {
  display: flex;
  min-height: 58px;
  align-items: center;
  gap: 8px;
}

.work-toolbar> :deep(.el-input) {
  width: min(360px, 36vw);
}

.work-toolbar> :deep(.el-select) {
  width: 180px;
}

.work-count {
  margin-left: auto;
  color: #7b8492;
  font-size: 12px;
}

.work-table {
  width: 100%;
  border-top: 1px solid var(--audience-line);
}

.work-table :deep(.el-table__header th) {
  height: 44px;
  color: #596474;
  background: #f7f8fa;
  font-weight: 600;
}

.work-table :deep(.el-table__row td) {
  padding: 10px 0;
  cursor: pointer;
}

.work-table :deep(.el-table__row:hover td) {
  background: #fff8f3;
}

.reason-text {
  display: block;
  overflow: hidden;
  color: #a64d1f;
  font-size: 12px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.library-head :deep(.el-button) {
  margin: 0;
}

@media (max-width: 760px) {
  .work-page {
    padding: 0 10px 18px;
  }

  .work-head,
  .library-head {
    min-height: 68px;
  }

  .work-head p,
  .library-head p {
    display: none;
  }

  .work-tabs {
    gap: 19px;
    overflow-x: auto;
  }

  .work-tabs button {
    flex: none;
  }

  .work-toolbar {
    flex-wrap: wrap;
    padding: 10px 0;
  }

  .work-toolbar> :deep(.el-input),
  .work-toolbar> :deep(.el-select) {
    width: 100%;
  }

  .work-count {
    width: 100%;
    margin: 0;
  }
}
</style>

<style scoped lang="scss">
.audience-page {
  --audience-line: #e3e7eb;
  --audience-ink: #202631;
  --audience-muted: #747e8c;
  --audience-orange: #ed6a2c;
  --audience-blue: #2f6fa8;
  min-height: calc(100vh - 84px);
  padding: 16px 22px 30px;
  color: var(--audience-ink);
  background: #f4f6f8;
}

.page-head,
.section-nav,
.work-page,
.customers-page,
.records-page,
:deep(.team-page) {
  width: 100%;
  max-width: 1600px;
  margin-right: auto;
  margin-left: auto;
}

.page-head {
  min-height: 58px;
  align-items: center;
}

.title-block {
  align-items: center;
  gap: 11px;
}

.title-block>div {
  min-width: 0;
}

.title-block h2 {
  font-size: 20px;
  font-weight: 650;
}

.title-block>div>span {
  display: block;
  margin-top: 3px;
  font-size: 12px;
}

.title-mark {
  display: grid;
  width: 34px;
  height: 34px;
  flex: none;
  place-items: center;
  border-radius: 6px;
  color: #fff;
  background: var(--audience-orange);
}

.title-mark :deep(.el-icon) {
  font-size: 17px;
}

.head-actions {
  align-items: center;
}

.head-actions :deep(.el-button) {
  margin: 0;
}

.sync-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 0 3px;
  border: 0;
  color: #28744f;
  background: transparent;
  cursor: pointer;
  font-size: 12px;
}

.sync-entry i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2b9b65;
}

.sync-entry.warning {
  color: #a35b09;
}

.sync-entry.warning i {
  background: #d98618;
}

.sync-entry :deep(.el-icon) {
  font-size: 13px;
}

.sync-health {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 4px;
  color: #28744f;
  font-size: 12px;
}

.sync-health i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2b9b65;
}

.sync-health.warning {
  color: #a35b09;
}

.sync-health.warning i {
  background: #d98618;
}

.section-nav {
  min-height: 49px;
  padding: 0 17px;
  border-color: var(--audience-line);
  border-radius: 6px 6px 0 0;
}

.section-tabs {
  align-items: flex-end;
  gap: 25px;
}

.section-tabs button {
  height: 48px;
  font-size: 13px;
}

.section-tabs button.active {
  font-weight: 650;
}

.section-tabs button.active::after {
  background: var(--audience-orange);
}

.tab-divider {
  width: 1px;
  height: 18px;
  margin: 0 -7px 16px;
  background: var(--audience-line);
}

.priority-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  border: 1px solid var(--audience-line);
  border-bottom: 0;
  background: #fff;
}

.priority-strip button {
  min-width: 0;
  padding: 13px 18px;
  border: 0;
  border-right: 1px solid #edf0f3;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.priority-strip button:last-child {
  border-right: 0;
}

.priority-strip button:hover {
  background: #fafbfc;
}

.priority-strip button.active {
  background: #fff7f2;
  box-shadow: inset 0 -2px var(--audience-orange);
}

.priority-strip span,
.priority-strip small {
  display: block;
  overflow: hidden;
  color: var(--audience-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.priority-strip span {
  font-size: 12px;
}

.priority-strip strong {
  display: block;
  margin: 5px 0 3px;
  font-size: 22px;
  font-weight: 650;
}

.priority-strip small {
  font-size: 11px;
}

.priority-strip .danger strong {
  color: #b42318;
}

.workbench {
  grid-template-columns: minmax(390px, .86fr) minmax(530px, 1.14fr);
  min-height: 640px;
  border-color: var(--audience-line);
}

.queue-pane {
  border-right: 1px solid var(--audience-line);
}

.queue-toolbar {
  min-height: 57px;
  padding: 10px 12px;
}

.queue-toolbar> :deep(.el-select) {
  width: 155px;
}

.queue-meta {
  min-height: 38px;
  padding: 0 13px;
}

.queue-meta strong {
  color: #495465;
  font-size: 12px;
  font-weight: 600;
}

.queue-meta button {
  padding: 3px 0;
  border: 0;
  color: var(--audience-blue);
  background: transparent;
  cursor: pointer;
  font-size: 11px;
}

.customer-queue {
  min-height: 495px;
}

.customer-row {
  min-height: 94px;
  padding: 14px 15px;
}

.customer-row.selected {
  background: #edf5fc;
  box-shadow: inset 3px 0 var(--audience-blue);
}

.name-line strong {
  font-size: 14px;
}

.relation-line {
  margin-top: 7px;
  font-size: 11px;
}

.row-main p {
  margin-top: 9px;
  font-size: 12px;
}

.signal-tags {
  display: flex;
  min-width: 0;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.signal-tags span {
  max-width: 100%;
  overflow: hidden;
  padding: 2px 6px;
  border: 1px solid #d9e5f0;
  border-radius: 3px;
  color: #315f88;
  background: #f4f8fc;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-side {
  gap: 6px;
}

.queue-reason {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
  margin-top: 8px;
}

.queue-reason span {
  overflow: hidden;
  color: #4f6073;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.queue-reason small {
  overflow: hidden;
  color: #8992a0;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.row-side small {
  font-size: 11px;
}

.status-name {
  font-size: 12px;
}

.detail-pane {
  max-height: calc(100vh - 235px);
  padding: 18px;
}

.customers-page,
.records-page {
  padding: 0 17px 24px;
  border-color: var(--audience-line);
}

.customer-presets {
  min-height: 51px;
  gap: 24px;
}

.customer-presets button {
  height: 41px;
  font-size: 13px;
}

.customer-presets button.active {
  color: var(--audience-ink);
}

.customer-presets button.active::after {
  background: var(--audience-orange);
}

.customer-presets button b {
  margin-left: 4px;
  color: #98a2b3;
  font-size: 10px;
  font-weight: 500;
}

.customer-toolbar {
  min-height: 55px;
  padding-top: 0;
  margin-bottom: 0;
}

.customer-count {
  display: flex;
  min-height: 38px;
  align-items: center;
}

.customer-table {
  border-top-color: var(--audience-line);
}

.customer-table :deep(.el-table__header th) {
  height: 44px;
  color: #596474;
  background: #f7f8fa;
  font-weight: 600;
}

.customer-table :deep(.el-table__row td) {
  padding: 9px 0;
}

.customer-table :deep(.el-table__row:hover td) {
  background: #fffaf6;
}

.table-customer small,
.cell-sub {
  margin-top: 5px;
  font-size: 11px;
}

.next-action {
  display: block;
  margin-bottom: 4px;
  color: #7d8795;
  font-size: 11px;
}

.batch-bar {
  position: sticky;
  z-index: 5;
  top: 0;
  border-color: #f0c5aa;
  background: #fff8f3;
}

.batch-bar>strong {
  margin-right: auto;
  color: #8a4b23;
  font-size: 12px;
}

.batch-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.batch-group :deep(.el-select) {
  width: 190px;
}

.batch-group :deep(.el-date-editor) {
  width: 210px;
}

.records-head {
  display: flex;
  min-height: 75px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid var(--audience-line);
}

.records-head h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 650;
}

.records-head p {
  margin: 5px 0 0;
  color: var(--audience-muted);
  font-size: 12px;
}

.records-health {
  display: flex;
  align-items: center;
  gap: 12px;
}

.records-health>span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: #28744f;
  font-size: 12px;
}

.records-health>span i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #2b9b65;
}

.records-health>span.warning {
  color: #a35b09;
}

.records-health>span.warning i {
  background: #d98618;
}

.sync-overview {
  grid-template-columns: repeat(5, minmax(0, 1fr));
  margin-bottom: 15px;
}

.sync-overview>div {
  padding: 16px 14px;
}

.sync-overview>div:first-child {
  padding-left: 14px;
}

.sync-overview span {
  font-size: 11px;
}

.sync-overview strong {
  margin-top: 6px;
  font-size: 20px;
}

.sync-overview small {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  color: #929aa6;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-view-switch {
  margin-top: 2px;
}

.record-table :deep(.el-table__header th) {
  color: #596474;
  background: #f7f8fa;
  font-weight: 600;
}

@media (max-width: 1179px) {
  .workbench {
    grid-template-columns: minmax(0, 1fr);
  }

  .queue-pane {
    border-right: 0;
  }
}

@media (max-width: 760px) {
  .audience-page {
    padding: 10px 9px 22px;
  }

  .page-head {
    align-items: center;
    flex-direction: row;
  }

  .title-block {
    align-items: center;
    flex-direction: row;
  }

  .title-block h2 {
    font-size: 18px;
  }

  .title-block>div>span,
  .sync-health {
    display: none;
  }

  .head-actions {
    margin-left: auto;
  }

  .head-actions :deep(.el-button span) {
    display: none;
  }

  .head-actions :deep(.el-button) {
    width: 34px;
    padding: 0;
  }

  .section-nav {
    padding: 0 10px;
  }

  .section-tabs {
    gap: 19px;
  }

  .tab-divider {
    display: none;
  }

  .priority-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .priority-strip button {
    padding: 12px 13px;
  }

  .priority-strip button:nth-child(2) {
    border-right: 0;
  }

  .priority-strip button:nth-child(-n+2) {
    border-bottom: 1px solid #edf0f3;
  }

  .priority-strip strong {
    font-size: 20px;
  }

  .priority-strip small {
    display: none;
  }

  .queue-toolbar> :deep(.el-input) {
    width: calc(100% - 46px);
  }

  .queue-toolbar> :deep(.el-select) {
    width: 100%;
  }

  .customer-row {
    grid-template-columns: minmax(0, 1fr) 98px;
  }

  .customers-page,
  .records-page {
    padding: 0 10px 18px;
  }

  .customer-presets {
    gap: 20px;
  }

  .customer-toolbar {
    padding: 10px 0;
  }

  .records-head {
    align-items: flex-start;
    flex-direction: column;
    padding: 14px 0;
  }

  .records-health {
    width: 100%;
    justify-content: space-between;
  }

  .batch-group {
    width: 100%;
  }

  .batch-group :deep(.el-select),
  .batch-group :deep(.el-date-editor) {
    min-width: 0;
    flex: 1;
    width: auto;
  }

  .sync-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sync-overview>div {
    border-bottom: 1px solid #edf0f3;
  }

  .sync-overview>div:nth-child(even) {
    border-right: 0;
  }

  .sync-overview>div:last-child {
    grid-column: span 2;
  }
}
</style>
