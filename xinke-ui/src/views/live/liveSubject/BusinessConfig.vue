<template>
  <main class="app-container business-config">
    <header><div><h2>{{ mode==='ROOM'?'直播间与主播分配':mode==='SHOP'?'店铺与人员组':'共享人员组' }}</h2><p>按业务岗位配置人员，自动获得对应业务权限；历史记录保留。</p></div><el-button icon="Refresh" :loading="loading" @click="load">刷新</el-button></header>
    <div class="toolbar">
      <el-segmented v-model="mode" :options="modes" @change="load" />
      <el-input v-if="mode!=='GROUP'" v-model.trim="keyword" clearable placeholder="搜索名称或编码" @keyup.enter="load" />
      <el-select v-if="mode!=='GROUP'" v-model="status" clearable placeholder="全部状态" @change="load"><el-option label="启用" value="0"/><el-option label="停用" value="1"/></el-select>
      <el-button v-if="canEdit" type="primary" icon="Plus" @click="mode==='GROUP'?editGroup():editSubject()">{{ mode==='GROUP'?'新建人员组':mode==='SHOP'?'新增店铺':'新增直播间' }}</el-button>
    </div>
    <el-alert v-if="mode==='SHOP'" type="info" :closable="false" show-icon title="相同客服名单只需维护一个人员组。店铺继承组内成员；特殊店铺可选择单独配置。" />
    <el-table v-if="mode!=='GROUP'" v-loading="loading" :data="rows" border row-key="subjectId">
      <el-table-column label="名称 / 编码" min-width="220"><template #default="{row}"><strong>{{ row.subjectName }}</strong><small class="code">{{ row.subjectCode }}</small></template></el-table-column>
      <el-table-column label="状态" width="85"><template #default="{row}"><el-tag :type="row.status==='0'?'success':'info'">{{ row.status==='0'?'启用':'停用' }}</el-tag></template></el-table-column>
      <el-table-column v-for="field in staffingFields" :key="field.key" :label="field.label" min-width="155"><template #default="{row}"><span>{{ row[field.names] || '未配置' }}</span></template></el-table-column>
      <el-table-column v-if="mode==='SHOP'" label="配置来源" min-width="145"><template #default="{row}"><el-tag :type="row.staffGroupId?'':'info'" effect="plain">{{ groupName(row.staffGroupId) || '单独配置' }}</el-tag></template></el-table-column>
      <el-table-column v-if="canEdit" label="操作" width="180" fixed="right"><template #default="{row}"><el-button link type="primary" @click="editAssignment(row)">{{ row.staffGroupId?'编辑人员组':'配置人员' }}</el-button><el-button link @click="editSubject(row)">编辑资料</el-button></template></el-table-column>
    </el-table>
    <el-table v-else v-loading="loading" :data="groups" border><el-table-column prop="groupName" label="人员组名称" min-width="220"/><el-table-column prop="memberCount" label="成员数" width="120"/><el-table-column prop="shopCount" label="关联店铺" width="120"/><el-table-column label="操作" width="140"><template #default="{row}"><el-button link type="primary" @click="editGroup(row.groupId)">编辑成员与店铺</el-button></template></el-table-column></el-table>
    <el-dialog append-to-body align-center class="business-config-dialog" v-model="subjectVisible" :title="subject.id?'编辑资料':'新增资料'" width="560px" :close-on-click-modal="false">
      <el-form ref="subjectForm" :model="subject" label-position="top">
        <el-form-item label="名称" prop="name" :rules="[{required:true,message:'请输入名称'}]"><el-input v-model.trim="subject.name" maxlength="100"/></el-form-item>
        <el-form-item label="编码 / 平台 ID" prop="code" :rules="[{required:true,message:'请输入编码'}]"><el-input v-model.trim="subject.code" maxlength="50"/></el-form-item>
        <el-form-item v-if="mode==='SHOP'" label="平台名称"><el-input v-model.trim="subject.platformName" maxlength="50"/></el-form-item>
        <el-form-item v-if="mode==='SHOP'" label="人员配置来源"><el-select v-model="subject.staffGroupId" clearable placeholder="单独配置"><el-option v-for="group in groups" :key="group.groupId" :value="group.groupId" :label="group.groupName"/></el-select><p class="hint">切换人员组会替换当前名单；清空人员组则保留当前名单，改为单独配置。</p></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="subject.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="subject.remark" type="textarea" maxlength="500"/></el-form-item>
      </el-form>
      <template #footer><el-button @click="subjectVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitSubject">保存</el-button></template>
    </el-dialog>
    <el-dialog append-to-body align-center class="business-config-dialog" v-model="assignmentVisible" :title="'配置人员 · '+assignment.subjectName" width="620px" :close-on-click-modal="false">
      <el-form label-position="top"><el-form-item v-for="field in staffingFields" :key="field.key" :label="field.label"><el-select v-model="assignment[field.key]" multiple filterable><el-option v-for="person in staff" :key="person.userId" :value="person.userId" :label="`${person.userName}（${person.account || person.userId}）`"/></el-select></el-form-item></el-form>
      <p class="hint">保存后按有效岗位自动同步对应权限，不改派已有订单。</p>
      <template #footer><el-button @click="assignmentVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAssignment">确认保存</el-button></template>
    </el-dialog>
    <el-dialog append-to-body align-center class="business-config-dialog" v-model="groupVisible" title="共享人员组" width="min(880px, 96vw)" :close-on-click-modal="false">
      <el-form ref="groupForm" :model="group" label-position="top">
        <el-form-item label="组名称" prop="groupName" :rules="[{required:true,message:'请输入组名称'}]"><el-input v-model.trim="group.groupName" maxlength="100"/></el-form-item>
        <div class="group-fields"><el-form-item v-for="field in shopFields" :key="field.key" :label="field.label"><el-select v-model="group[field.key]" multiple filterable><el-option v-for="person in staff" :key="person.userId" :value="person.userId" :label="`${person.userName}（${person.account || person.userId}）`"/></el-select></el-form-item></div>
        <el-form-item label="关联店铺"><el-select v-model="group.shopIds" multiple filterable><el-option v-for="shop in allShops" :key="shop.subjectId" :value="shop.subjectId" :label="shop.subjectName"/></el-select></el-form-item>
      </el-form>
      <div class="preview-head"><strong>保存影响预览</strong><small>移出组的店铺保留现有名单；切换组的店铺使用本组名单。</small></div>
      <el-table :data="groupEffects" border max-height="260"><el-table-column prop="name" label="店铺"/><el-table-column prop="effect" label="变更" width="130"/><el-table-column prop="added" label="新增岗位分配" min-width="160"/><el-table-column prop="removed" label="移除岗位分配" min-width="160"/></el-table>
      <template #footer><el-button @click="groupVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitGroup">确认保存 {{ groupEffects.length }} 家店铺配置</el-button></template>
    </el-dialog>
  </main>
</template>
<script setup>
import {ref,computed,onMounted,watch} from 'vue'
import {useRoute} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {checkPermi} from '@/utils/permission'
import {listStaff,listMappings,listStaffGroups,getStaffGroup,saveStaffGroup,saveMapping,getRoom,getShop,saveRoom,saveShop} from '@/api/live/gift'
const route=useRoute()
const canManageShops=computed(()=>checkPermi(['system:user:edit']))
const canManageRooms=computed(()=>checkPermi(['system:user:edit','live:metrics:manage']))
const initialMode=()=>route.query.kind==='SHOP'||route.path.endsWith('/shops')?'SHOP':'ROOM'
const mode=ref(initialMode()),keyword=ref(''),status=ref('0'),loading=ref(false),saving=ref(false)
const modes=computed(()=>[{label:'直播间与主播',value:'ROOM'},...(canManageShops.value?[{label:'店铺',value:'SHOP'},{label:'人员组',value:'GROUP'}]:[])])
const canEdit=computed(()=>mode.value==='ROOM'?canManageRooms.value:canManageShops.value)
const rows=ref([]),groups=ref([]),staff=ref([]),allShops=ref([])
const subject=ref({}),assignment=ref({}),group=ref({shopIds:[]}),subjectVisible=ref(false),assignmentVisible=ref(false),groupVisible=ref(false),subjectForm=ref(null),groupForm=ref(null)
const shopFields=[{key:'operatorIds',names:'operatorNames',label:'运营'},{key:'customerServiceIds',names:'customerServiceNames',label:'电话客服'},{key:'customerAdminIds',names:'customerAdminNames',label:'客服管理员'}]
const roomFields=[{key:'anchorIds',names:'anchorNames',label:'主播'},{key:'controllerIds',names:'controllerNames',label:'场控'}]
const staffingFields=computed(()=>mode.value==='ROOM'?roomFields:shopFields)
const ids=value=>Array.isArray(value)?value.map(Number):String(value||'').split(',').filter(Boolean).map(Number)
const groupName=id=>groups.value.find(g=>String(g.groupId)===String(id))?.groupName
let loadGeneration=0
async function load(){const generation=++loadGeneration;loading.value=true;rows.value=[];try{const [mapping,groupList,people]=await Promise.all([listMappings({subjectType:mode.value==='GROUP'?'SHOP':mode.value,keyword:keyword.value,status:status.value,limit:500}),canManageShops.value?listStaffGroups():Promise.resolve({data:[]}),canEdit.value?listStaff({status:'0',limit:500}):Promise.resolve({data:[]})]);if(generation!==loadGeneration)return;rows.value=mapping.data||[];groups.value=groupList.data||[];staff.value=people.data||[]}finally{if(generation===loadGeneration)loading.value=false}}
async function editSubject(row){if(row){const data=(await(mode.value==='ROOM'?getRoom(row.subjectId):getShop(row.subjectId))).data;subject.value={...data,id:row.subjectId,code:data.roomCode||data.shopCode,name:data.roomName||data.shopName,staffGroupId:row.staffGroupId||null}}else subject.value={id:null,code:'',name:'',platformName:'',status:'0',remark:'',staffGroupId:null};subjectVisible.value=true}
async function submitSubject(){await subjectForm.value.validate();if(mode.value==='SHOP'&&subject.value.id&&String(rows.value.find(r=>r.subjectId===subject.value.id)?.staffGroupId||'')!==String(subject.value.staffGroupId||''))await ElMessageBox.confirm('切换人员配置来源会影响此店铺的有效名单和自动业务权限，确认保存？','配置变更');saving.value=true;try{if(mode.value==='ROOM')await saveRoom({...subject.value,roomId:subject.value.id,roomCode:subject.value.code,roomName:subject.value.name});else await saveShop({...subject.value,shopId:subject.value.id,shopCode:subject.value.code,shopName:subject.value.name});subjectVisible.value=false;ElMessage.success('已保存');await load()}finally{saving.value=false}}
function editAssignment(row){if(row.staffGroupId)return editGroup(row.staffGroupId);assignment.value={...row};for(const field of staffingFields.value)assignment.value[field.key]=ids(row[field.key]);assignmentVisible.value=true}
async function submitAssignment(){saving.value=true;try{await saveMapping(assignment.value);assignmentVisible.value=false;ElMessage.success('人员分配已保存');await load()}finally{saving.value=false}}
async function editGroup(id){const shops=(await listMappings({subjectType:'SHOP',limit:500})).data||[];allShops.value=shops;group.value=id?(await getStaffGroup(id)).data:{groupName:'',shopIds:[],operatorIds:[],customerServiceIds:[],customerAdminIds:[]};groupVisible.value=true}
function assignmentSet(row){return new Set(shopFields.flatMap(field=>ids(row[field.key]).map(id=>field.label+'：'+(staff.value.find(p=>String(p.userId)===String(id))?.userName||'#'+id))))}
const groupEffects=computed(()=>{const wanted=new Set(ids(group.value.shopIds));const next=assignmentSet(group.value);return allShops.value.filter(shop=>wanted.has(Number(shop.subjectId))||(group.value.groupId&&String(shop.staffGroupId)===String(group.value.groupId))).map(shop=>{const retained=wanted.has(Number(shop.subjectId));const previous=assignmentSet(shop);return{name:shop.subjectName,effect:retained?'使用本组配置':'转为单独配置',added:retained?[...next].filter(v=>!previous.has(v)).join('、')||'无':'无',removed:retained?[...previous].filter(v=>!next.has(v)).join('、')||'无':'无'}})})
async function submitGroup(){await groupForm.value.validate();await ElMessageBox.confirm(`确认保存人员组？将更新 ${groupEffects.value.length} 家店铺的配置，已有订单负责人保留。`,'保存人员组');saving.value=true;try{await saveStaffGroup(group.value);groupVisible.value=false;ElMessage.success('人员组已保存');await load()}finally{saving.value=false}}
watch(()=>route.fullPath,()=>{mode.value=initialMode();load()})
onMounted(load)
</script>
<style scoped>
:global(.business-config-dialog .el-dialog__body){max-height:calc(100dvh - 210px);overflow:auto}

.business-config{max-width:1700px}header{display:flex;align-items:center;justify-content:space-between}h2{margin:0;font-size:22px}header p{color:#7c8796;font-size:13px}.toolbar{display:flex;gap:12px;align-items:center;flex-wrap:wrap;margin:20px 0 14px}.toolbar .el-input{width:260px}.toolbar .el-select{width:125px}.toolbar>.el-button{margin-left:auto}.el-alert{margin-bottom:14px}.code{display:block;margin-top:6px;color:#89929f}.el-form .el-select{width:100%}.hint{font-size:12px;line-height:1.7;color:#7c8796;margin:8px 0}.group-fields{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:16px}.preview-head{display:flex;flex-wrap:wrap;gap:15px;margin:12px 0}.preview-head small{color:#7c8796}@media(max-width:700px){.group-fields{grid-template-columns:1fr}.toolbar .el-input{width:100%}}
</style>
