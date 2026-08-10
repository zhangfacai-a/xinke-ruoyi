<template>
  <div class="app-container room-mapping-page">
    <header class="page-head">
      <div>
        <h2>直播间与店铺映射</h2>
        <p>每个直播间绑定一个抖店，影刀会按店铺领取同一批用户。</p>
      </div>
      <div class="head-actions">
        <el-button icon="Shop" @click="openShopDialog">新建店铺</el-button>
        <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['live:room:add']">新增直播间</el-button>
      </div>
    </header>

    <div class="summary-bar">
      <div><span>直播间</span><strong>{{ total }}</strong></div>
      <div><span>本页已映射</span><strong class="success">{{ pageMapped }}</strong></div>
      <div><span>本页待映射</span><strong class="warning">{{ pageUnmapped }}</strong></div>
      <div><span>可用店铺</span><strong>{{ activeShops.length }}</strong></div>
    </div>

    <section class="workspace">
      <el-form :model="queryParams" ref="queryRef" :inline="true" class="filters">
        <el-form-item label="直播间"><el-input v-model="queryParams.roomName" placeholder="名称或直播间ID" clearable @keyup.enter="handleQuery" /></el-form-item>
        <el-form-item label="映射状态">
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
        <div><strong>还没有可映射的店铺</strong><span>先建立抖店与影刀账号配置，再给直播间绑定。</span></div>
        <el-button type="primary" @click="openShopDialog">立即配置</el-button>
      </div>

      <el-table v-loading="loading" :data="roomList" row-key="room_key" class="room-table">
        <el-table-column label="直播间" min-width="220" fixed>
          <template #default="scope"><div class="room-cell"><strong>{{ scope.row.room_name || scope.row.room_key }}</strong><small>{{ scope.row.room_key }}</small></div></template>
        </el-table-column>
        <el-table-column label="映射状态" width="110" align="center">
          <template #default="scope"><el-tag :type="scope.row.shop_config_id ? 'success' : 'warning'" effect="light">{{ scope.row.shop_config_id ? '已映射' : '待映射' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="对应店铺" min-width="240">
          <template #default="scope">
            <el-select :model-value="scope.row.shop_config_id" filterable placeholder="选择店铺" style="width:100%" @change="value => changeMapping(scope.row, value)">
              <el-option v-for="shop in activeShops" :key="shop.shopConfigId" :label="shop.shopName" :value="shop.shopConfigId"><span>{{ shop.shopName }}</span><small class="shop-account">{{ shop.douyinAccountCode }}</small></el-option>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="影刀抖音号" prop="douyin_account_code" min-width="150"><template #default="scope">{{ scope.row.douyin_account_code || '-' }}</template></el-table-column>
        <el-table-column label="数据状态" width="100" align="center">
          <template #default="scope"><el-tag :type="scope.row.status === '1' ? 'info' : 'success'" effect="plain">{{ scope.row.status === '1' ? '停用' : '启用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="最后上报" width="165"><template #default="scope">{{ formatTime(scope.row.last_report_time) }}</template></el-table-column>
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['live:room:edit']">编辑</el-button>
            <el-button v-if="scope.row.shop_config_id" link type="warning" @click="removeMapping(scope.row)" v-hasPermi="['live:room:edit']">解除</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['live:room:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    </section>

    <el-dialog v-model="roomOpen" :title="roomTitle" width="520px" append-to-body>
      <el-form ref="roomRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="直播间ID" prop="roomKey"><el-input v-model="form.roomKey" :disabled="isEdit" /></el-form-item>
        <el-form-item label="显示名称" prop="roomName"><el-input v-model="form.roomName" /></el-form-item>
        <el-form-item label="来源"><el-input v-model="form.source" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="form.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="roomOpen=false">取消</el-button><el-button type="primary" @click="submitForm">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="shopOpen" title="新建影刀店铺" width="560px" append-to-body>
      <el-form :model="shopForm" label-width="110px">
        <el-form-item label="内部店铺编码" required><el-input v-model="shopForm.shopCode" placeholder="例如：XW旗舰店" /></el-form-item>
        <el-form-item label="店铺名称" required><el-input v-model="shopForm.shopName" /></el-form-item>
        <el-form-item label="抖音账号" required><el-input v-model="shopForm.douyinAccountCode" placeholder="影刀操作时登录的抖音号" /></el-form-item>
        <el-form-item label="抖店名称" required><el-input v-model="shopForm.douyinShopName" /></el-form-item>
        <el-form-item label="每日上限"><el-input-number v-model="shopForm.dailyLimit" :min="1" :max="10000" /></el-form-item>
        <el-form-item label="私信内容" required><el-input v-model="shopForm.messageTemplate" type="textarea" :rows="4" maxlength="1000" show-word-limit /></el-form-item>
      </el-form>
      <template #footer><el-button @click="shopOpen=false">取消</el-button><el-button type="primary" :loading="shopSaving" @click="saveShop">保存店铺</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="LiveRoomMapping">
import { addLiveRoom, addRpaShop, deleteLiveRoom, listLiveRooms, listRpaWorkbenchShops, mapLiveRoomShop, unmapLiveRoomShop, updateLiveRoom } from '@/api/live/viewer'
const { proxy } = getCurrentInstance()
const loading=ref(false), roomOpen=ref(false), shopOpen=ref(false), shopSaving=ref(false), isEdit=ref(false), roomTitle=ref(''), roomList=ref([]), shops=ref([]), total=ref(0)
const mappingOptions=[{label:'全部',value:''},{label:'待映射',value:'UNMAPPED'},{label:'已映射',value:'MAPPED'}]
const queryParams=reactive({pageNum:1,pageSize:20,roomName:undefined,mappingStatus:'',shopConfigId:undefined})
const form=reactive({roomKey:undefined,roomName:undefined,source:'douyin_anchor_dashboard',status:'0',remark:undefined})
const shopForm=reactive({shopCode:'',shopName:'',douyinAccountCode:'',douyinShopName:'',messageTemplate:'',dailyLimit:100,status:'0',remark:''})
const rules={roomKey:[{required:true,message:'直播间ID不能为空',trigger:'blur'}],roomName:[{required:true,message:'直播间名称不能为空',trigger:'blur'}]}
const activeShops=computed(()=>shops.value.filter(item=>item.status!=='1'))
const pageMapped=computed(()=>roomList.value.filter(item=>item.shop_config_id).length)
const pageUnmapped=computed(()=>roomList.value.length-pageMapped.value)
function getList(){loading.value=true;listLiveRooms(queryParams).then(res=>{roomList.value=res.rows||[];total.value=res.total||0}).finally(()=>loading.value=false)}
function loadShops(){return listRpaWorkbenchShops().then(res=>shops.value=res.data||[])}
function handleQuery(){queryParams.pageNum=1;getList()}
function resetQuery(){Object.assign(queryParams,{pageNum:1,roomName:undefined,mappingStatus:'',shopConfigId:undefined});getList()}
function resetRoom(){Object.assign(form,{roomKey:undefined,roomName:undefined,source:'douyin_anchor_dashboard',status:'0',remark:undefined});proxy.resetForm('roomRef')}
function handleAdd(){resetRoom();isEdit.value=false;roomTitle.value='新增直播间';roomOpen.value=true}
function handleUpdate(row){resetRoom();isEdit.value=true;roomTitle.value='编辑直播间';Object.assign(form,{roomKey:row.room_key,roomName:row.room_name,source:row.source,status:row.status||'0',remark:row.remark});roomOpen.value=true}
function submitForm(){proxy.$refs.roomRef.validate(valid=>{if(!valid)return;const payload={...form};(isEdit.value?updateLiveRoom(form.roomKey,payload):addLiveRoom(payload)).then(()=>{proxy.$modal.msgSuccess('保存成功');roomOpen.value=false;getList()})})}
function changeMapping(row,shopConfigId){mapLiveRoomShop(row.room_key,shopConfigId).then(()=>{proxy.$modal.msgSuccess('店铺映射已更新');getList()})}
function removeMapping(row){proxy.$modal.confirm(`确认解除“${row.room_name||row.room_key}”的店铺映射吗？`).then(()=>unmapLiveRoomShop(row.room_key)).then(()=>{proxy.$modal.msgSuccess('已解除映射');getList()}).catch(()=>{})}
function handleDelete(row){proxy.$modal.confirm(`确认删除直播间“${row.room_name||row.room_key}”吗？`).then(()=>deleteLiveRoom(row.room_key)).then(()=>{proxy.$modal.msgSuccess('删除成功');getList()}).catch(()=>{})}
function openShopDialog(){Object.assign(shopForm,{shopCode:'',shopName:'',douyinAccountCode:'',douyinShopName:'',messageTemplate:'',dailyLimit:100,status:'0',remark:''});shopOpen.value=true}
function saveShop(){const required=['shopCode','shopName','douyinAccountCode','douyinShopName','messageTemplate'];if(required.some(key=>!String(shopForm[key]||'').trim()))return proxy.$modal.msgWarning('请填写完整的店铺配置');shopSaving.value=true;addRpaShop({...shopForm}).then(()=>{proxy.$modal.msgSuccess('店铺创建成功');shopOpen.value=false;return loadShops()}).finally(()=>shopSaving.value=false)}
function formatTime(value){return value?String(value).replace('T',' ').replace(/\.\d+$/,'').slice(0,19):'-'}
loadShops();getList()
</script>

<style scoped>
.room-mapping-page{min-height:calc(100vh - 84px);background:#f6f7f9}.page-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:14px}.page-head h2{margin:0 0 4px;font-size:22px;color:#202124}.page-head p{margin:0;color:#6b7280}.head-actions{display:flex;gap:8px}.summary-bar{display:grid;grid-template-columns:repeat(4,1fr);margin-bottom:14px;border:1px solid #e4e7ed;border-radius:6px;background:#fff}.summary-bar div{padding:12px 18px;border-right:1px solid #ebeef5}.summary-bar div:last-child{border-right:0}.summary-bar span{display:block;font-size:12px;color:#6b7280}.summary-bar strong{display:block;margin-top:3px;font-size:22px;color:#202124}.summary-bar .success{color:#16a34a}.summary-bar .warning{color:#f97316}.workspace{padding:14px 16px 16px;border:1px solid #e4e7ed;border-radius:6px;background:#fff}.filters{border-bottom:1px solid #ebeef5;margin-bottom:12px}.empty-config{display:flex;align-items:center;justify-content:space-between;padding:12px 16px;margin-bottom:12px;border-left:3px solid #f97316;background:#fff7ed}.empty-config strong,.empty-config span{display:block}.empty-config span{margin-top:3px;font-size:13px;color:#6b7280}.room-cell{display:flex;flex-direction:column;line-height:1.5}.room-cell small{color:#909399}.shop-account{float:right;margin-left:18px;color:#909399}.room-table{width:100%}@media(max-width:900px){.summary-bar{grid-template-columns:repeat(2,1fr)}.page-head{align-items:flex-start}.page-head p{display:none}.head-actions{flex-wrap:wrap;justify-content:flex-end}}
</style>
