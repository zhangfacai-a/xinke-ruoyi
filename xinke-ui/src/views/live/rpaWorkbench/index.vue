<template>
  <div class="app-container rpa-workbench">
    <header class="page-head">
      <div>
        <h2>影刀任务池</h2>
        <p>控制任务入队，查看影刀领取与执行结果，集中维护用户黑名单。</p>
      </div>
      <div><el-button type="primary" icon="Setting" @click="openShopSetup()">配置店铺</el-button><el-button icon="Refresh" @click="refreshAll">刷新</el-button></div>
    </header>

    <div class="stats-strip">
      <button v-for="item in statItems" :key="item.view" type="button" :class="{ active: activeView === item.view }" @click="changeView(item.view)">
        <span>{{ item.label }}</span><strong>{{ item.value }}</strong>
      </button>
    </div>

    <section class="workbench-body">
      <el-tabs v-model="activeView" @tab-change="handleTabChange">
        <el-tab-pane label="候选用户" name="CANDIDATE" />
        <el-tab-pane label="待领取" name="PENDING" />
        <el-tab-pane label="处理中" name="LEASED" />
        <el-tab-pane label="执行记录" name="HISTORY" />
        <el-tab-pane label="黑名单" name="BLACKLIST" />
      </el-tabs>

      <el-form :model="query" :inline="true" class="filter-row">
        <el-form-item label="日期">
          <el-date-picker v-model="query.dateRange" type="daterange" value-format="YYYY-MM-DD" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" style="width: 250px" />
        </el-form-item>
        <el-form-item label="店铺">
          <el-select v-model="query.shopConfigId" clearable filterable placeholder="全部店铺" style="width: 170px">
            <el-option v-for="shop in shops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId" />
          </el-select>
        </el-form-item>
        <el-form-item label="用户">
          <el-input v-model="query.keyword" clearable placeholder="昵称 / 抖音号 / 任务号" style="width: 210px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item v-if="activeView === 'CANDIDATE'" label="发送控制">
          <el-select v-model="query.sendControl" clearable placeholder="全部" style="width: 140px">
            <el-option label="当前可加入" value="ELIGIBLE" />
            <el-option label="跟随规则" value="AUTO" />
            <el-option label="强制发送" value="INCLUDE" />
            <el-option label="不发送" value="EXCLUDE" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="activeView === 'HISTORY'" label="结果">
          <el-select v-model="query.taskStatus" clearable placeholder="全部" style="width: 130px">
            <el-option label="已下单" value="ordered" /><el-option label="已触达" value="contacted" />
            <el-option label="已跳过" value="skipped" /><el-option label="执行失败" value="failed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="activeView === 'BLACKLIST'" label="原因">
          <el-select v-model="query.reason" clearable placeholder="全部" style="width: 150px">
            <el-option v-for="item in blacklistReasons" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>

      <div class="action-row" v-if="activeView === 'CANDIDATE'">
        <el-button type="primary" icon="Plus" :disabled="!selectedViewerIds.length" @click="enqueueSelected">加入待领取</el-button>
        <el-dropdown :disabled="!selectedViewerIds.length" @command="setSelectedControl">
          <el-button :disabled="!selectedViewerIds.length" icon="Operation">发送控制<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
          <template #dropdown><el-dropdown-menu>
            <el-dropdown-item command="INCLUDE">强制发送</el-dropdown-item>
            <el-dropdown-item command="EXCLUDE">不发送</el-dropdown-item>
            <el-dropdown-item command="AUTO" divided>恢复自动</el-dropdown-item>
          </el-dropdown-menu></template>
        </el-dropdown>
        <el-button type="danger" plain icon="CircleClose" :disabled="!selectedViewerIds.length" @click="openBlacklist(selectedRows)">加入黑名单</el-button>
        <span class="selection-tip">已选 {{ selectedViewerIds.length }} 人</span>
      </div>

      <el-table v-loading="loading" :data="rows" border stripe row-key="rowKey" height="560" @selection-change="handleSelection">
        <el-table-column v-if="activeView === 'CANDIDATE'" type="selection" width="48" fixed />
        <el-table-column label="用户" min-width="190" fixed>
          <template #default="scope"><div class="user-cell"><strong>{{ scope.row.nickname || '未知观众' }}</strong><small>{{ scope.row.douyinNo || scope.row.secUid || '-' }}</small></div></template>
        </el-table-column>
        <el-table-column v-if="activeView !== 'BLACKLIST'" label="店铺" min-width="140" show-overflow-tooltip>
          <template #default="scope"><span v-if="scope.row.shopConfigId">{{ scope.row.shopName }}</span><el-button v-else link type="danger" @click="openShopSetup(scope.row)">未配置店铺</el-button></template>
        </el-table-column>
        <el-table-column v-if="activeView !== 'BLACKLIST'" label="直播间" prop="liveRoomName" min-width="150" show-overflow-tooltip />
        <el-table-column v-if="activeView === 'CANDIDATE'" label="最近出现" prop="lastSeenDate" width="115" />
        <el-table-column v-if="activeView === 'CANDIDATE'" label="评论" min-width="210" show-overflow-tooltip>
          <template #default="scope"><el-tag :type="Number(scope.row.hasComment) ? 'success' : 'info'" size="small">{{ Number(scope.row.commentCount || 0) }} 条</el-tag><span class="comment-text">{{ scope.row.lastCommentContent || '暂无评论' }}</span></template>
        </el-table-column>
        <el-table-column v-if="activeView === 'CANDIDATE'" label="发送控制" width="125" align="center">
          <template #default="scope"><el-tag :type="controlTag(scope.row)">{{ controlLabel(scope.row) }}</el-tag></template>
        </el-table-column>
        <el-table-column v-if="['PENDING','LEASED','HISTORY'].includes(activeView)" label="任务状态" width="110" align="center">
          <template #default="scope"><el-tag :type="taskTag(scope.row.taskStatus)">{{ taskLabel(scope.row.taskStatus) }}</el-tag></template>
        </el-table-column>
        <el-table-column v-if="['PENDING','LEASED','HISTORY'].includes(activeView)" label="任务号" prop="taskNo" min-width="170" show-overflow-tooltip />
        <el-table-column v-if="activeView === 'LEASED'" label="执行电脑" prop="workerId" min-width="140" />
        <el-table-column v-if="activeView === 'LEASED'" label="租约到期" width="165"><template #default="scope">{{ formatTime(scope.row.leaseExpireTime) }}</template></el-table-column>
        <el-table-column v-if="activeView === 'HISTORY'" label="执行结果" min-width="180" show-overflow-tooltip><template #default="scope">{{ outcomeText(scope.row) }}</template></el-table-column>
        <el-table-column v-if="activeView === 'HISTORY'" label="完成时间" width="165"><template #default="scope">{{ formatTime(scope.row.completedTime) }}</template></el-table-column>
        <el-table-column v-if="activeView === 'BLACKLIST'" label="范围" prop="scopeName" min-width="140" />
        <el-table-column v-if="activeView === 'BLACKLIST'" label="原因" prop="reason" min-width="130" />
        <el-table-column v-if="activeView === 'BLACKLIST'" label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column v-if="activeView === 'BLACKLIST'" label="操作人" prop="operatorName" width="110" />
        <el-table-column v-if="activeView === 'BLACKLIST'" label="拉黑时间" width="165"><template #default="scope">{{ formatTime(scope.row.createTime) }}</template></el-table-column>
        <el-table-column label="操作" fixed="right" :width="activeView === 'CANDIDATE' ? 250 : 120">
          <template #default="scope">
            <template v-if="activeView === 'CANDIDATE'">
              <el-button link type="primary" @click="enqueueRows([scope.row])">加入任务</el-button>
              <el-dropdown @command="mode => setControl([scope.row.viewerId], mode)"><el-button link type="primary">设置</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="INCLUDE">强制发送</el-dropdown-item><el-dropdown-item command="EXCLUDE">不发送</el-dropdown-item><el-dropdown-item command="AUTO" divided>恢复自动</el-dropdown-item></el-dropdown-menu></template></el-dropdown>
              <el-button link type="danger" @click="openBlacklist([scope.row])">拉黑</el-button>
            </template>
            <el-button v-else-if="activeView === 'BLACKLIST'" link type="primary" @click="restore(scope.row)">恢复</el-button>
            <el-button v-else link type="primary" @click="copyProfile(scope.row)">主页</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="query.pageNum" v-model:limit="query.pageSize" @pagination="loadList" />
    </section>

    <el-dialog v-model="blacklistOpen" title="加入黑名单" width="520px" append-to-body>
      <el-alert title="加入后，待领取和处理中的任务会立即取消，并保留历史记录。" type="warning" :closable="false" class="mb12" />
      <el-form :model="blacklistForm" label-width="90px">
        <el-form-item label="用户"><span>已选择 {{ blacklistForm.viewerIds.length }} 人</span></el-form-item>
        <el-form-item label="范围" required><el-radio-group v-model="blacklistForm.scope"><el-radio value="GLOBAL">所有店铺</el-radio><el-radio value="SHOP">指定店铺</el-radio></el-radio-group></el-form-item>
        <el-form-item v-if="blacklistForm.scope === 'SHOP'" label="店铺" required><el-select v-model="blacklistForm.shopConfigId" filterable style="width:100%"><el-option v-for="shop in shops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId" /></el-select></el-form-item>
        <el-form-item label="原因" required><el-select v-model="blacklistForm.reason" style="width:100%"><el-option v-for="item in blacklistReasons" :key="item" :label="item" :value="item" /></el-select></el-form-item>
        <el-form-item label="备注"><el-input v-model="blacklistForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="blacklistOpen=false">取消</el-button><el-button type="danger" @click="submitBlacklist">确认拉黑</el-button></template>
    </el-dialog>

    <el-dialog v-model="shopSetupOpen" title="配置影刀店铺" width="560px" append-to-body>
      <el-alert title="直播间必须绑定店铺，服务器才能把用户交给正确的抖店账号。" type="warning" :closable="false" class="mb12" />
      <el-form :model="shopForm" label-width="110px">
        <el-form-item label="直播间" required><el-select v-model="shopForm.roomKey" filterable style="width:100%"><el-option v-for="room in unmappedRooms" :key="room.roomKey" :label="room.roomName || room.roomKey" :value="room.roomKey" /></el-select></el-form-item>
        <el-form-item label="内部店铺编码" required><el-input v-model="shopForm.shopCode" placeholder="例如：XW旗舰店" /></el-form-item>
        <el-form-item label="店铺名称" required><el-input v-model="shopForm.shopName" /></el-form-item>
        <el-form-item label="抖音账号" required><el-input v-model="shopForm.douyinAccountCode" placeholder="影刀操作时登录的抖音号" /></el-form-item>
        <el-form-item label="抖店名称" required><el-input v-model="shopForm.douyinShopName" /></el-form-item>
        <el-form-item label="每日上限" required><el-input-number v-model="shopForm.dailyLimit" :min="1" :max="10000" /></el-form-item>
        <el-form-item label="私信内容" required><el-input v-model="shopForm.messageTemplate" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="shopSetupOpen=false">取消</el-button><el-button type="primary" :loading="shopSaving" @click="saveShopSetup">保存并绑定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="RpaWorkbench">
import { addRpaShop, bindRpaShopRooms, blacklistRpaViewers, enqueueRpaViewers, getRpaWorkbenchStats, listRpaUnmappedRooms, listRpaWorkbench, listRpaWorkbenchShops, restoreRpaBlacklist, updateRpaViewerTracking } from '@/api/live/viewer'
const { proxy } = getCurrentInstance()
const addDays = (date, days) => { const value = new Date(date); value.setDate(value.getDate() + days); return value }
const dateText = (date) => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`
const defaultRange = () => [dateText(addDays(new Date(), -1)), dateText(new Date())]
const activeView = ref('CANDIDATE'), loading = ref(false), rows = ref([]), total = ref(0), shops = ref([])
const selectedRows = ref([]), selectedViewerIds = computed(() => [...new Set(selectedRows.value.map(row => Number(row.viewerId)).filter(Boolean))])
const stats = ref({}), blacklistOpen = ref(false)
const shopSetupOpen = ref(false), shopSaving = ref(false), unmappedRooms = ref([])
const query = reactive({ pageNum:1, pageSize:20, dateRange:defaultRange(), shopConfigId:undefined, keyword:undefined, sendControl:undefined, taskStatus:undefined, reason:undefined })
const blacklistForm = reactive({ viewerIds:[], scope:'GLOBAL', shopConfigId:undefined, reason:undefined, remark:undefined })
const shopForm = reactive({ roomKey:undefined, shopCode:'', shopName:'', douyinAccountCode:'', douyinShopName:'', messageTemplate:'', dailyLimit:100, status:'0', remark:'' })
const blacklistReasons = ['明确拒绝联系','投诉或风险用户','同行或供应商','无效账号','内部测试账号','其他']
const statItems = computed(() => [
  {label:'候选用户',value:stats.value.candidateCount||0,view:'CANDIDATE'}, {label:'待领取',value:stats.value.pendingCount||0,view:'PENDING'},
  {label:'处理中',value:stats.value.leasedCount||0,view:'LEASED'}, {label:'今日完成',value:stats.value.todayCompletedCount||0,view:'HISTORY'},
  {label:'等待重试',value:stats.value.retryCount||0,view:'PENDING'}, {label:'执行失败',value:stats.value.failedCount||0,view:'HISTORY'},
  {label:'黑名单',value:stats.value.blacklistCount||0,view:'BLACKLIST'}
])
function params(){ const [beginDate,endDate]=query.dateRange||defaultRange(); return {pageNum:query.pageNum,pageSize:query.pageSize,view:activeView.value,beginDate,endDate,shopConfigId:query.shopConfigId,keyword:query.keyword,sendControl:query.sendControl,taskStatus:query.taskStatus,reason:query.reason} }
function loadList(){ loading.value=true; listRpaWorkbench(params()).then(res=>{rows.value=res.rows||[];total.value=res.total||0}).finally(()=>loading.value=false) }
function loadStats(){ getRpaWorkbenchStats(params()).then(res=>stats.value=res.data||{}) }
function refreshAll(){ loadList(); loadStats() }
function changeView(view){ activeView.value=view; handleTabChange() }
function handleTabChange(){ query.pageNum=1; selectedRows.value=[]; loadList() }
function handleQuery(){ query.pageNum=1; refreshAll() }
function resetQuery(){ query.pageNum=1;query.dateRange=defaultRange();query.shopConfigId=undefined;query.keyword=undefined;query.sendControl=undefined;query.taskStatus=undefined;query.reason=undefined;refreshAll() }
function handleSelection(value){ selectedRows.value=value||[] }
function enqueueRows(items){ const unbound=items.find(row=>!row.shopConfigId);if(unbound){proxy.$modal.msgWarning('请先给该直播间配置店铺');return openShopSetup(unbound)}const ids=[...new Set(items.map(row=>Number(row.viewerId)).filter(Boolean))]; if(!ids.length)return; enqueueRpaViewers({viewerIds:ids}).then(res=>{proxy.$modal.msgSuccess(`已加入 ${res.data||0} 条待领取任务`);refreshAll()}) }
function enqueueSelected(){ enqueueRows(selectedRows.value) }
function setControl(ids,mode){ updateRpaViewerTracking({viewerIds:ids,mode}).then(()=>{proxy.$modal.msgSuccess('发送控制已更新');refreshAll()}) }
function setSelectedControl(mode){ setControl(selectedViewerIds.value,mode) }
function openBlacklist(items){ blacklistForm.viewerIds=[...new Set(items.map(row=>Number(row.viewerId)).filter(Boolean))];blacklistForm.scope='GLOBAL';blacklistForm.shopConfigId=items.length===1?items[0].shopConfigId:undefined;blacklistForm.reason=undefined;blacklistForm.remark=undefined;blacklistOpen.value=true }
function submitBlacklist(){ if(!blacklistForm.reason)return proxy.$modal.msgWarning('请选择拉黑原因');if(blacklistForm.scope==='SHOP'&&!blacklistForm.shopConfigId)return proxy.$modal.msgWarning('请选择店铺');blacklistRpaViewers({...blacklistForm}).then(()=>{proxy.$modal.msgSuccess('已加入黑名单');blacklistOpen.value=false;refreshAll()}) }
function restore(row){ proxy.$modal.confirm(`确认将“${row.nickname||'该用户'}”移出黑名单？`).then(()=>restoreRpaBlacklist(row.blacklistId)).then(()=>{proxy.$modal.msgSuccess('已恢复');refreshAll()}) }
function controlLabel(row){if(Number(row.blacklisted))return'黑名单';if(row.trackingMode==='INCLUDE')return'强制发送';if(row.trackingMode==='EXCLUDE')return'不发送';return Number(row.eligible)?'按规则发送':'暂不发送'}
function controlTag(row){if(Number(row.blacklisted)||row.trackingMode==='EXCLUDE')return'danger';if(row.trackingMode==='INCLUDE')return'warning';return Number(row.eligible)?'success':'info'}
function taskLabel(value){return({pending:'待领取',leased:'处理中',ordered:'已下单',contacted:'已触达',skipped:'已跳过',failed:'执行失败',cancelled:'已取消'})[value]||value||'-'}
function taskTag(value){return({pending:'warning',leased:'primary',ordered:'success',contacted:'success',skipped:'info',failed:'danger',cancelled:'info'})[value]||'info'}
function outcomeText(row){if(row.taskStatus==='ordered')return`已下单${row.orderNo?' · '+row.orderNo:''}`;if(row.taskStatus==='contacted')return`${Number(row.followed)?'已关注':'未关注'} / ${Number(row.messaged)?'已私信':'未私信'}`;return row.errorMessage||row.resultCode||taskLabel(row.taskStatus)}
function formatTime(value){return value?String(value).replace('T',' ').replace(/\.\d+$/,'').slice(0,19):'-'}
function copyProfile(row){navigator.clipboard?.writeText(row.profileUrl||`https://www.douyin.com/user/${row.secUid}`).then(()=>proxy.$modal.msgSuccess('主页链接已复制'))}
function loadShops(){listRpaWorkbenchShops().then(res=>shops.value=res.data||[])}
function openShopSetup(row){Object.assign(shopForm,{roomKey:row?.roomKey,shopCode:'',shopName:row?.liveRoomName||'',douyinAccountCode:'',douyinShopName:row?.liveRoomName||'',messageTemplate:'',dailyLimit:100,status:'0',remark:''});listRpaUnmappedRooms().then(res=>{unmappedRooms.value=res.data||[];if(row?.roomKey&&!unmappedRooms.value.some(item=>item.roomKey===row.roomKey))unmappedRooms.value.unshift({roomKey:row.roomKey,roomName:row.liveRoomName})});shopSetupOpen.value=true}
function saveShopSetup(){const required=['roomKey','shopCode','shopName','douyinAccountCode','douyinShopName','messageTemplate'];if(required.some(key=>!String(shopForm[key]||'').trim()))return proxy.$modal.msgWarning('请填写完整的店铺配置');shopSaving.value=true;const {roomKey,...data}=shopForm;addRpaShop(data).then(res=>bindRpaShopRooms(res.data,[roomKey])).then(()=>{proxy.$modal.msgSuccess('店铺已创建，直播间绑定成功');shopSetupOpen.value=false;loadShops();refreshAll()}).finally(()=>shopSaving.value=false)}
loadShops();refreshAll()
</script>

<style scoped>
.rpa-workbench{background:#f6f7f9;min-height:calc(100vh - 84px)}.page-head{display:flex;justify-content:space-between;align-items:flex-start;margin-bottom:14px}.page-head h2{margin:0 0 5px;font-size:22px;color:#202124}.page-head p{margin:0;color:#6b7280}.stats-strip{display:grid;grid-template-columns:repeat(7,minmax(108px,1fr));background:#fff;border:1px solid #e4e7ed;border-radius:6px;margin-bottom:14px;overflow:hidden}.stats-strip button{border:0;border-right:1px solid #ebeef5;background:#fff;padding:13px 14px;text-align:left;cursor:pointer}.stats-strip button:last-child{border-right:0}.stats-strip button.active{box-shadow:inset 0 -3px #f97316;background:#fff8f3}.stats-strip span{display:block;color:#6b7280;font-size:12px}.stats-strip strong{display:block;margin-top:4px;font-size:22px;color:#202124}.workbench-body{background:#fff;border:1px solid #e4e7ed;border-radius:6px;padding:0 16px 16px}.filter-row{padding-top:2px}.action-row{display:flex;gap:8px;align-items:center;margin-bottom:10px}.selection-tip{color:#909399;font-size:13px}.user-cell{display:flex;flex-direction:column;line-height:1.5}.user-cell small{color:#909399;overflow:hidden;text-overflow:ellipsis}.comment-text{margin-left:8px}.mb12{margin-bottom:12px}@media(max-width:1100px){.stats-strip{grid-template-columns:repeat(4,1fr)}.stats-strip button{border-bottom:1px solid #ebeef5}}@media(max-width:700px){.stats-strip{grid-template-columns:repeat(2,1fr)}.page-head{gap:10px}.page-head p{display:none}}
</style>
