<template>
  <div class="app-container room-mapping-page">
    <header class="page-head">
      <div>
        <h2>直播间店铺配置</h2>
        <p>指定每个直播间由哪个店铺账号负责，未配置的直播间不会自动跟进用户。</p>
      </div>
      <div class="head-actions">
        <el-button icon="Shop" @click="openShopDialog">管理店铺账号</el-button>
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['live:room:add']">新增直播间</el-button>
      </div>
    </header>

    <div class="summary-bar">
      <div><span>全部直播间</span><strong>{{ total }}</strong></div>
      <div><span>本页正常</span><strong class="success">{{ pageReady }}</strong></div>
      <div><span>本页待配置</span><strong class="warning">{{ pageUnmapped }}</strong></div>
      <div><span>可执行店铺</span><strong>{{ activeShops.length }}</strong></div>
    </div>

    <section class="workspace">
      <el-form :model="queryParams" ref="queryRef" :inline="true" class="filters">
        <el-form-item label="直播间"><el-input v-model="queryParams.roomName" placeholder="名称或直播间ID" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="配置状态">
          <el-segmented v-model="queryParams.mappingStatus" :options="mappingOptions" @change="handleQuery" />
        </el-form-item>
        <el-form-item label="店铺">
          <el-select v-model="queryParams.shopConfigId" clearable filterable placeholder="全部店铺" style="width:180px" @change="handleQuery">
            <el-option v-for="shop in activeShops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId" />
          </el-select>
        </el-form-item>
        <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">查询</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
      </el-form>

      <div v-if="!activeShops.length" class="empty-config">
        <div><strong>还没有可执行的店铺</strong><span>先填写店铺名称、实际操作账号和私信内容，再为直播间指定店铺。</span></div>
        <el-button type="primary" @click="openShopDialog">立即配置</el-button>
      </div>

      <el-table v-loading="loading" :data="roomList" row-key="room_key" class="room-table">
        <el-table-column label="直播间" min-width="220" fixed>
          <template #default="scope"><div class="room-cell"><strong>{{ scope.row.room_name || scope.row.room_key }}</strong><small>{{ scope.row.room_key }}</small></div></template>
        </el-table-column>
        <el-table-column label="执行状态" width="140" align="center">
          <template #default="scope"><el-tag :type="roomStatus(scope.row).type" effect="light">{{ roomStatus(scope.row).text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="负责店铺" min-width="210">
          <template #default="scope">
            <div v-if="scope.row.shop_config_id" class="shop-cell"><strong>{{ scope.row.shop_name || currentShop(scope.row)?.shopName || '-' }}</strong><small>{{ scope.row.douyin_account_code || currentShop(scope.row)?.douyinAccountCode || '-' }}</small></div>
            <span v-else class="muted">尚未指定</span>
          </template>
        </el-table-column>
        <el-table-column label="配置完整度" width="150">
          <template #default="scope"><el-progress :percentage="shopCompletion(currentShop(scope.row))" :status="shopCompletion(currentShop(scope.row))===100?'success':undefined" /></template>
        </el-table-column>
        <el-table-column label="数据状态" width="100" align="center">
          <template #default="scope"><el-tag :type="scope.row.status === '1' ? 'info' : 'success'" effect="plain">{{ scope.row.status === '1' ? '停用' : '启用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="最后上报" width="165"><template #default="scope">{{ formatTime(scope.row.last_report_time) }}</template></el-table-column>
        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" @click="openMapping(scope.row)">{{ scope.row.shop_config_id ? '更换店铺' : '指定店铺' }}</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['live:room:edit']">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['live:room:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog v-model="roomOpen" :title="roomTitle" width="520px" append-to-body>
      <el-form ref="roomRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="直播间识别码" prop="roomKey"><el-input v-model="form.roomKey" :disabled="isEdit" placeholder="通常由采集插件自动填写" /><div class="field-help">用于识别直播间，保存后不可修改。</div></el-form-item>
        <el-form-item label="直播间名称" prop="roomName"><el-input v-model="form.roomName" placeholder="例如：晚间好物专场" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roomOpen=false">取消</el-button><el-button type="primary" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="mappingOpen" :title="mappingForm.currentShopId ? '更换负责店铺' : '指定负责店铺'" width="560px" append-to-body>
      <div class="mapping-summary"><span>直播间</span><strong>{{ mappingForm.roomName }}</strong></div>
      <el-form label-width="100px">
        <el-form-item label="负责店铺" required>
          <el-select v-model="mappingForm.shopConfigId" filterable placeholder="请选择店铺" style="width:100%">
            <el-option v-for="shop in activeShops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId">
              <span>{{ shop.shopName }}</span><small class="shop-account">操作账号：{{ shop.douyinAccountCode }}</small>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <el-alert v-if="selectedMappingShop" :title="`确认后，该直播间的新用户将由 ${selectedMappingShop.shopName} 的账号处理。`" type="warning" :closable="false" show-icon />
      <div v-if="mappingForm.currentShopId" class="mapping-policy"><strong>历史任务不会改变</strong><span>已领取和已完成的任务仍保留原店铺，避免执行到错误账号。</span></div>
      <template #footer><el-button @click="mappingOpen=false">取消</el-button><el-button v-if="mappingForm.currentShopId" @click="removeMapping(mappingForm.row)">解除绑定</el-button><el-button type="primary" :loading="mappingSaving" @click="confirmMapping">确认并生效</el-button></template>
    </el-dialog>

    <el-dialog v-model="shopOpen" title="影刀店铺配置" width="980px" append-to-body destroy-on-close>
      <div class="shop-config-layout">
        <aside class="shop-list">
          <el-button type="primary" plain icon="Plus" @click="newShop">新建店铺</el-button>
          <button v-for="shop in shops" :key="shop.shopConfigId" type="button" :class="{active:shopForm.shopConfigId===shop.shopConfigId}" @click="editShop(shop)">
            <strong>{{ shop.shopName }}</strong><small>{{ shop.douyinAccountCode }}</small>
          </button>
        </aside>
        <div class="shop-editor">
          <el-tabs v-model="shopTab">
            <el-tab-pane label="基础信息" name="basic">
              <el-form :model="shopForm" label-width="110px" class="shop-form">
                <el-form-item label="店铺名称" required><el-input v-model="shopForm.shopName" placeholder="与抖店名称保持一致" /></el-form-item>
                <el-form-item label="操作抖音号" required><el-input v-model="shopForm.douyinAccountCode" placeholder="影刀浏览器实际登录的抖音号" /></el-form-item>
                <el-form-item label="配置状态"><el-switch v-model="shopForm.enabled" inline-prompt active-text="启用" inactive-text="停用" /></el-form-item>
                <el-form-item label="备注"><el-input v-model="shopForm.remark" type="textarea" :rows="3" maxlength="500" show-word-limit /></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="私信模板" name="templates">
              <div class="template-tools"><span>服务器按评论关键词自动选择模板，影刀直接使用最终内容。</span><el-button icon="Plus" @click="addTemplate">新增模板</el-button></div>
              <div class="template-list">
                <div v-for="(item,index) in shopForm.messageTemplates" :key="item.templateKey" class="template-row">
                  <div class="template-meta">
                    <el-input v-model="item.templateName" placeholder="模板名称" />
                    <el-select v-model="item.scene" style="width:120px"><el-option label="通用" value="GENERAL"/><el-option label="评论用户" value="COMMENT"/><el-option label="关键词" value="KEYWORD"/><el-option label="兜底" value="FALLBACK"/></el-select>
                    <el-switch v-model="item.enabled" />
                    <el-checkbox :model-value="item.defaultTemplate" @change="value=>setDefaultTemplate(index,value)">默认</el-checkbox>
                    <el-button link type="danger" icon="Delete" @click="removeTemplate(index)" :disabled="shopForm.messageTemplates.length===1" />
                  </div>
                  <el-input v-model="item.content" type="textarea" :rows="2" maxlength="1000" show-word-limit />
                  <el-input v-if="item.scene==='KEYWORD'" v-model="item.keywordText" placeholder="命中关键词，用中文逗号分隔，例如：优惠，价格，赠品" class="keyword-input" />
                  <small v-pre>可用变量：{{nickname}}、{{comment}}、{{shopName}}、{{liveRoomName}}</small>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="执行限制" name="limits">
              <el-form :model="shopForm" label-width="130px" class="shop-form two-column">
                <el-form-item label="每日最多处理"><el-input-number v-model="shopForm.dailyLimit" :min="1" :max="10000" /></el-form-item>
                <el-form-item label="每小时最多处理"><el-input-number v-model="shopForm.hourlyLimit" :min="1" :max="1000" /></el-form-item>
                <el-form-item label="连续操作人数"><el-input-number v-model="shopForm.burstSize" :min="1" :max="100" /></el-form-item>
                <el-form-item label="批次休息分钟"><el-input-number v-model="shopForm.restMinutes" :min="1" :max="120" /></el-form-item>
                <el-form-item label="允许执行时段"><el-time-picker v-model="shopForm.allowedTimeRange" is-range value-format="HH:mm" format="HH:mm" range-separator="至" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
                <el-form-item label="连续失败暂停"><el-input-number v-model="shopForm.maxConsecutiveFailures" :min="1" :max="100" /></el-form-item>
                <el-form-item label="验证码处理"><el-switch v-model="shopForm.pauseOnCaptcha" active-text="立即暂停店铺" /></el-form-item>
              </el-form>
            </el-tab-pane>
            <el-tab-pane label="退款策略" name="refund">
              <el-alert title="退款和售后用户默认停止自动营销，冷却结束后仍需人工确认。" type="warning" :closable="false" class="mb12" />
              <el-form :model="shopForm" label-width="160px" class="shop-form">
                <el-form-item label="已退款冷却期"><el-input-number v-model="shopForm.refundCooldownDays" :min="1" :max="3650" /><span class="unit">天</span></el-form-item>
                <el-form-item label="取消未付款冷却期"><el-input-number v-model="shopForm.cancelledCooldownDays" :min="1" :max="365" /><span class="unit">天</span></el-form-item>
                <el-form-item label="退款处理中"><el-tag type="danger">禁止自动营销</el-tag></el-form-item>
                <el-form-item label="质量问题退款"><el-tag type="danger">永久抑制，转人工售后</el-tag></el-form-item>
                <el-form-item label="冷却结束"><el-tag type="warning">进入人工审核，不自动恢复</el-tag></el-form-item>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
      <template #footer><el-button @click="shopOpen=false">取消</el-button><el-button type="primary" :loading="shopSaving" @click="saveShop">保存店铺配置</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="LiveRoomMapping">
import { addLiveRoom, addRpaShop, deleteLiveRoom, listLiveRooms, listRpaWorkbenchShops, mapLiveRoomShop, unmapLiveRoomShop, updateLiveRoom, updateRpaShop } from '@/api/live/viewer'
const { proxy } = getCurrentInstance()
const loading=ref(false), roomOpen=ref(false), shopOpen=ref(false), shopSaving=ref(false), mappingOpen=ref(false), mappingSaving=ref(false), isEdit=ref(false), roomTitle=ref(''), roomList=ref([]), shops=ref([]), total=ref(0), shopTab=ref('basic')
const mappingForm=reactive({row:null,roomKey:'',roomName:'',currentShopId:undefined,shopConfigId:undefined})
const mappingOptions=[{label:'全部',value:''},{label:'待映射',value:'UNMAPPED'},{label:'已映射',value:'MAPPED'}]
const queryParams=reactive({pageNum:1,pageSize:20,roomName:undefined,mappingStatus:'',shopConfigId:undefined})
const form=reactive({roomKey:undefined,roomName:undefined,source:'douyin_anchor_dashboard',status:'0',remark:undefined})
const defaultTemplates=()=>[
  {templateKey:'GENERAL',templateName:'通用问候',scene:'GENERAL',content:'你好，看到你刚刚来过{{liveRoomName}}，想了解哪款产品呢？有问题可以直接告诉我。',enabled:true,defaultTemplate:true,priority:60,keywordText:''},
  {templateKey:'COMMENT',templateName:'评论用户',scene:'COMMENT',content:'你好，看到你在直播间咨询了“{{comment}}”，这边可以继续帮你详细解答。',enabled:true,defaultTemplate:false,priority:50,keywordText:''},
  {templateKey:'PRODUCT',templateName:'产品咨询',scene:'KEYWORD',content:'你好，看到你在直播间关注了我们的产品，需要了解型号、价格或活动都可以直接问我。',enabled:true,defaultTemplate:false,priority:10,keywordText:'型号，区别，功能，适合'},
  {templateKey:'PROMOTION',templateName:'优惠咨询',scene:'KEYWORD',content:'你好，看到你在直播间咨询优惠活动，目前具体活动可以在这里继续了解，有需要我可以帮你确认。',enabled:true,defaultTemplate:false,priority:20,keywordText:'优惠，价格，赠品，活动，便宜'},
  {templateKey:'AFTER_SALES',templateName:'售后咨询',scene:'KEYWORD',content:'你好，看到你在直播间咨询安装或售后问题，可以把具体情况发给我，这边帮你确认。',enabled:true,defaultTemplate:false,priority:30,keywordText:'安装，发货，维修，退换，售后'},
  {templateKey:'FALLBACK',templateName:'兜底模板',scene:'FALLBACK',content:'你好，感谢关注{{shopName}}，有任何产品问题都可以直接告诉我。',enabled:true,defaultTemplate:false,priority:99,keywordText:''}
]
const emptyShop=()=>({shopConfigId:undefined,shopName:'',douyinAccountCode:'',dailyLimit:100,hourlyLimit:15,burstSize:10,restMinutes:5,allowedTimeRange:['09:00','22:00'],refundCooldownDays:90,cancelledCooldownDays:7,pauseOnCaptcha:true,maxConsecutiveFailures:5,enabled:true,remark:'',messageTemplates:defaultTemplates()})
const shopForm=reactive(emptyShop())
const rules={roomKey:[{required:true,message:'直播间ID不能为空',trigger:'blur'}],roomName:[{required:true,message:'直播间名称不能为空',trigger:'blur'}]}
const activeShops=computed(()=>shops.value.filter(item=>item.status!=='1'))
const selectedMappingShop=computed(()=>activeShops.value.find(item=>String(item.shopConfigId)===String(mappingForm.shopConfigId)))
const pageMapped=computed(()=>roomList.value.filter(item=>item.shop_config_id).length)
const pageReady=computed(()=>roomList.value.filter(item=>roomStatus(item).text==='正常执行').length)
const pageUnmapped=computed(()=>roomList.value.length-pageMapped.value)
function getList(){loading.value=true;listLiveRooms(queryParams).then(res=>{roomList.value=res.rows||[];total.value=res.total||0}).finally(()=>loading.value=false)}
function loadShops(){return listRpaWorkbenchShops().then(res=>shops.value=res.data||[])}
function handleQuery(){queryParams.pageNum=1;getList()}
function resetQuery(){Object.assign(queryParams,{pageNum:1,roomName:undefined,mappingStatus:'',shopConfigId:undefined});getList()}
function resetRoom(){Object.assign(form,{roomKey:undefined,roomName:undefined,source:'douyin_anchor_dashboard',status:'0',remark:undefined});proxy.resetForm('roomRef')}
function handleAdd(){resetRoom();isEdit.value=false;roomTitle.value='新增直播间';roomOpen.value=true}
function handleUpdate(row){resetRoom();isEdit.value=true;roomTitle.value='编辑直播间';Object.assign(form,{roomKey:row.room_key,roomName:row.room_name,source:row.source,status:row.status||'0',remark:row.remark});roomOpen.value=true}
function submitForm(){proxy.$refs.roomRef.validate(valid=>{if(!valid)return;const payload={...form};(isEdit.value?updateLiveRoom(form.roomKey,payload):addLiveRoom(payload)).then(()=>{proxy.$modal.msgSuccess('保存成功');roomOpen.value=false;getList()})})}
function currentShop(row){return shops.value.find(item=>String(item.shopConfigId)===String(row?.shop_config_id))}
function shopCompletion(shop){if(!shop)return 0;const checks=[shop.shopName,shop.douyinAccountCode,Number(shop.dailyLimit)>0,Number(shop.hourlyLimit)>0,shop.allowedStartTime,shop.allowedEndTime,(shop.messageTemplates||[]).some(item=>item.enabled&&String(item.content||'').trim())];return Math.round(checks.filter(Boolean).length/checks.length*100)}
function roomStatus(row){if(row.status==='1')return{text:'直播间已停用',type:'info'};if(!row.shop_config_id)return{text:'待指定店铺',type:'warning'};const shop=currentShop(row);if(!shop)return{text:'店铺已停用',type:'danger'};if(shopCompletion(shop)<100)return{text:'店铺配置不完整',type:'danger'};return{text:'正常执行',type:'success'} }
function openMapping(row){Object.assign(mappingForm,{row,roomKey:row.room_key,roomName:row.room_name||row.room_key,currentShopId:row.shop_config_id,shopConfigId:row.shop_config_id});mappingOpen.value=true}
function confirmMapping(){if(!mappingForm.shopConfigId)return proxy.$modal.msgWarning('请先选择负责店铺');if(String(mappingForm.shopConfigId)===String(mappingForm.currentShopId)){mappingOpen.value=false;return}mappingSaving.value=true;mapLiveRoomShop(mappingForm.roomKey,mappingForm.shopConfigId).then(()=>{proxy.$modal.msgSuccess('负责店铺已更新');mappingOpen.value=false;getList()}).finally(()=>mappingSaving.value=false)}
function removeMapping(row){proxy.$modal.confirm(`解除后，“${row.room_name||row.room_key}”的新用户不会进入自动跟进。确认解除吗？`).then(()=>unmapLiveRoomShop(row.room_key)).then(()=>{proxy.$modal.msgSuccess('已解除绑定，历史任务不受影响');mappingOpen.value=false;getList()}).catch(()=>{})}
function handleDelete(row){if(row.shop_config_id)return proxy.$modal.msgWarning('请先解除负责店铺，再删除直播间');proxy.$modal.confirm(`删除后不可恢复，但不会删除历史用户和执行记录。确认删除“${row.room_name||row.room_key}”吗？`).then(()=>deleteLiveRoom(row.room_key)).then(()=>{proxy.$modal.msgSuccess('直播间已删除');getList()}).catch(()=>{})}
function newShop(){Object.assign(shopForm,emptyShop());shopTab.value='basic'}
function editShop(shop){const templates=(shop.messageTemplates?.length?shop.messageTemplates:defaultTemplates()).map(item=>({...item,keywordText:(item.keywords||[]).join('，')}));Object.assign(shopForm,emptyShop(),shop,{enabled:shop.status!=='1',allowedTimeRange:[shop.allowedStartTime||'09:00',shop.allowedEndTime||'22:00'],messageTemplates:templates});shopTab.value='basic'}
function openShopDialog(){if(shops.value.length)editShop(shops.value[0]);else newShop();shopOpen.value=true}
function addTemplate(){shopForm.messageTemplates.push({templateKey:`CUSTOM_${Date.now()}`,templateName:'自定义模板',scene:'KEYWORD',content:'',enabled:true,defaultTemplate:false,priority:80,keywordText:''})}
function removeTemplate(index){shopForm.messageTemplates.splice(index,1)}
function setDefaultTemplate(index,value){shopForm.messageTemplates.forEach((item,i)=>item.defaultTemplate=Boolean(value)&&i===index);if(!value&&shopForm.messageTemplates.length)shopForm.messageTemplates[0].defaultTemplate=true}
function saveShop(){if(!shopForm.shopName.trim()||!shopForm.douyinAccountCode.trim())return proxy.$modal.msgWarning('请填写店铺名称和操作抖音号');if(!shopForm.messageTemplates.some(item=>item.enabled&&item.content.trim()))return proxy.$modal.msgWarning('至少保留一条可用私信模板');const [allowedStartTime,allowedEndTime]=shopForm.allowedTimeRange||['09:00','22:00'];const data={...shopForm,status:shopForm.enabled?'0':'1',douyinShopName:shopForm.shopName,allowedStartTime,allowedEndTime,messageTemplates:shopForm.messageTemplates.map(({keywordText,...item})=>({...item,keywords:String(keywordText||'').split(/[，,]/).map(v=>v.trim()).filter(Boolean)}))};delete data.shopConfigId;delete data.enabled;delete data.allowedTimeRange;shopSaving.value=true;const request=shopForm.shopConfigId?updateRpaShop(shopForm.shopConfigId,data):addRpaShop(data);request.then(()=>{proxy.$modal.msgSuccess('店铺配置已保存');return loadShops()}).then(()=>{if(shopForm.shopConfigId){const current=shops.value.find(item=>item.shopConfigId===shopForm.shopConfigId);if(current)editShop(current)}else if(shops.value.length)editShop(shops.value[shops.value.length-1])}).finally(()=>shopSaving.value=false)}
function formatTime(value){return value?String(value).replace('T',' ').replace(/\.\d+$/,'').slice(0,19):'-'}
loadShops();getList()
</script>

<style scoped>
.room-mapping-page{min-height:calc(100vh - 84px);background:#f6f7f9}.page-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}.page-head h2{margin:0 0 4px;font-size:22px;color:#202124}.page-head p{margin:0;color:#6b7280}.head-actions{display:flex;gap:8px}.summary-bar{display:grid;grid-template-columns:repeat(4,1fr);margin-bottom:14px;border:1px solid #e4e7ed;border-radius:6px;background:#fff}.summary-bar div{padding:12px 18px;border-right:1px solid #ebeef5}.summary-bar div:last-child{border-right:0}.summary-bar span{display:block;font-size:12px;color:#6b7280}.summary-bar strong{display:block;margin-top:3px;font-size:22px;color:#202124}.summary-bar .success{color:#16a34a}.summary-bar .warning{color:#f97316}.workspace{padding:14px 16px 16px;border:1px solid #e4e7ed;border-radius:6px;background:#fff}.filters{border-bottom:1px solid #ebeef5;margin-bottom:12px}.empty-config{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;margin-bottom:12px;border-left:3px solid #f97316;background:#fff7ed}.empty-config strong,.empty-config span{display:block}.empty-config span{margin-top:3px;font-size:13px;color:#6b7280}.room-cell{display:flex;flex-direction:column;line-height:1.5}.room-cell small{color:#909399}.shop-account{float:right;margin-left:18px;color:#909399}.room-table{width:100%}.shop-config-layout{display:grid;grid-template-columns:190px minmax(0,1fr);min-height:560px;border:1px solid #e4e7ed}.shop-list{padding:12px;border-right:1px solid #e4e7ed;background:#f7f8fa}.shop-list>.el-button{width:100%;margin-bottom:10px}.shop-list button{display:flex;flex-direction:column;width:100%;padding:10px;margin-bottom:5px;border:0;border-radius:4px;background:transparent;text-align:left;cursor:pointer}.shop-list button:hover,.shop-list button.active{background:#fff1e8;color:#ea580c}.shop-list small{margin-top:3px;color:#909399}.shop-editor{padding:0 18px 18px;min-width:0}.shop-form{max-width:650px;padding-top:12px}.two-column{display:grid;grid-template-columns:repeat(2,minmax(260px,1fr));max-width:none}.template-tools{display:flex;align-items:center;justify-content:space-between;margin:6px 0 12px;color:#6b7280}.template-list{max-height:470px;overflow:auto}.template-row{padding:12px 0;border-bottom:1px solid #ebeef5}.template-meta{display:grid;grid-template-columns:160px 120px 42px 70px 32px;gap:8px;align-items:center;margin-bottom:8px}.template-row small{display:block;margin-top:6px;color:#909399}.keyword-input{margin-top:8px}.unit{margin-left:8px;color:#6b7280}.mb12{margin-bottom:12px}@media(max-width:900px){.summary-bar{grid-template-columns:repeat(2,1fr)}.page-head{align-items:flex-start}.page-head p{display:none}.head-actions{flex-wrap:wrap;justify-content:flex-end}.shop-config-layout{grid-template-columns:1fr}.shop-list{display:none}.two-column{grid-template-columns:1fr}}
.shop-cell{display:flex;flex-direction:column;line-height:1.45}.shop-cell small,.field-help,.muted{color:#909399;font-size:12px}.mapping-summary{display:flex;gap:12px;align-items:center;background:#fff7ed;border:1px solid #fed7aa;padding:12px 14px;margin-bottom:18px}.mapping-summary span{color:#9a3412}.mapping-policy{display:flex;flex-direction:column;gap:3px;margin-top:14px;padding:10px 12px;border-left:3px solid #f97316;background:#fff7ed}.mapping-policy span{color:#6b7280;font-size:12px}
</style>
