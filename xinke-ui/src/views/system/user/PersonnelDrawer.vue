<template>
  <el-drawer v-model="visible" title="人员详情" size="min(1040px, 100%)" append-to-body destroy-on-close>
    <div v-loading="loading" v-if="visible" class="personnel-detail">
      <template v-if="detail">
        <header class="person-header"><div class="initial">{{ (person.nickName || '?').slice(0,1) }}</div><div><h2>{{ person.nickName || '未填写姓名' }}</h2><p>登录账号：{{ person.userName }} · {{ detail.account.deptName || '未分配部门' }}</p></div><el-tag :type="detail.account.status==='0'?'success':'info'">{{ detail.account.status==='0'?'账号启用':'账号停用' }}</el-tag></header>
        <el-alert v-if="detail.isSuperAdmin" title="超级管理员拥有全部权限，不在这里调整个人业务权限。" type="info" :closable="false" />
        <el-alert v-if="detail.localDisabled" title="此账号已由网站停用，钉钉同步不会自动启用。历史业务记录保留。" type="warning" :closable="false" />
        <el-tabs v-model="tab">
          <el-tab-pane label="基本资料" name="profile">
            <div class="section-heading"><h3>基本资料</h3><el-button v-if="!detail.isSuperAdmin" v-hasPermi="['system:user:edit']" @click="emit('edit',person)">编辑资料</el-button></div>
            <el-descriptions :column="2" border><el-descriptions-item label="姓名">{{ person.nickName }}</el-descriptions-item><el-descriptions-item label="登录账号">{{ person.userName }}</el-descriptions-item><el-descriptions-item label="手机号">{{ detail.account.phonenumber || '未填写' }}</el-descriptions-item><el-descriptions-item label="邮箱">{{ detail.account.email || '未填写' }}</el-descriptions-item><el-descriptions-item label="部门">{{ detail.account.deptName || '未分配' }}</el-descriptions-item><el-descriptions-item label="资料来源">{{ detail.account.source==='DINGTALK'?'钉钉通讯录':'网站建立' }}</el-descriptions-item></el-descriptions>
            <p class="note" v-if="detail.account.source==='DINGTALK'">姓名、手机号及部门等资料随钉钉同步更新；既有登录账号保持不变。网站业务权限和本地停用状态由网站维护。</p>
            <div class="responsibilities"><div><span>未完成挽单</span><strong>{{ detail.pendingRetention }}</strong></div><div><span>待办跟进</span><strong>{{ detail.pendingFollowups }}</strong></div><div><span>业务分工</span><strong>{{ detail.assignments.length }}</strong></div></div>
            <p class="note">离职或不再使用账号时，优先停用并安排交接。历史录入人、负责人及业绩记录保留。</p>
          </el-tab-pane>
          <el-tab-pane label="店铺与直播间分工" name="assignments">
            <div class="section-heading"><div><h3>业务分工</h3><p class="note">共享客服在人员组维护一次；关联店铺继承同一份名单。</p></div><div><el-button v-hasPermi="['system:user:edit']" @click="go('/ops-people/shops')">店铺与人员组</el-button><el-button v-hasPermi="['system:user:edit','live:metrics:manage']" @click="go('/ops-people/liveSubject')">直播间分工</el-button></div></div>
            <el-table :data="detail.assignments" border empty-text="尚未分配店铺或直播间"><el-table-column prop="subjectName" label="店铺 / 直播间" min-width="180" /><el-table-column label="职责" width="120"><template #default="{row}">{{ roleName(row.roleCode) }}</template></el-table-column><el-table-column label="状态" width="100"><template #default="{row}">{{ row.subjectStatus==='0'?'启用':'停用' }}</template></el-table-column><el-table-column label="分工来源" min-width="150"><template #default="{row}">{{ row.groupName || '单独配置' }}</template></el-table-column></el-table>
            <h3>岗位带来的默认权限</h3><el-table :data="detail.expectedAutoRoles" empty-text="没有分工触发自动授权"><el-table-column prop="roleName" label="默认权限" /><el-table-column label="状态" width="120"><template #default="{row}"><el-tag :type="row.granted?'success':'warning'">{{ row.granted?'已同步':'待同步' }}</el-tag></template></el-table-column></el-table>
          </el-tab-pane>
          <el-tab-pane label="功能与数据权限" name="permissions" lazy>
            <PersonnelFreight v-if="!detail.isSuperAdmin" :key="person.userId" :person="person" :editable="canAdmin" @changed="refresh" />
            <h3>其他业务的页面权限</h3><p class="note">以下展示有效角色带来的页面入口；数据范围还由各业务的分工规则控制。京东客户信息对正常登录账号开放。</p>
            <el-table :data="detail.pages" empty-text="暂无角色页面授权"><el-table-column prop="menuName" label="页面" /><el-table-column prop="roleName" label="权限来源" /></el-table>
          </el-tab-pane>
          <el-tab-pane label="系统角色与授权来源" name="roles" lazy>
            <h3>系统角色</h3><p class="note">系统角色负责通用权限。业务自动角色与个人运费配置由对应功能维护，不能在这里手动授予他人。</p>
            <el-checkbox-group v-model="selectedRoles" :disabled="!canEditRoles || roleSaving"><div v-for="role in roleOptions" :key="role.roleId" class="role-option"><el-checkbox :value="role.roleId" :disabled="managed(role) || role.status!=='0'"><strong>{{ role.roleName }}</strong></el-checkbox><span>{{ managed(role)?'自动维护': '人工授权' }}{{ role.status==='0'?'':' · 已停用' }}</span></div></el-checkbox-group>
            <el-button v-if="canEditRoles" type="primary" :loading="roleSaving" @click="saveRoles">保存系统角色</el-button>
            <h3>最终授予的权限及来源</h3><el-input v-model="permissionSearch" clearable placeholder="搜索功能、权限标识或角色" class="permission-search" /><el-table :data="filteredPermissions" empty-text="暂无匹配权限"><el-table-column prop="menuName" label="功能" min-width="150" /><el-table-column prop="perms" label="权限标识" min-width="190" /><el-table-column prop="roleName" label="来源角色" min-width="170" /></el-table>
            <p class="note">运费存在个人配置时，以“功能与数据权限”中的设置为准；账号停用后不能使用这些功能。</p>
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
  </el-drawer>
</template>
<script setup>
import {ref,computed} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage,ElMessageBox} from 'element-plus'
import {personnelDetail} from '@/api/live/personnel'
import {getAuthRole,updateAuthRole} from '@/api/system/user'
import {checkPermi,checkRole} from '@/utils/permission'
import PersonnelFreight from './PersonnelFreight.vue'
const emit=defineEmits(['edit','changed'])
const router=useRouter()
const visible=ref(false),loading=ref(false),detail=ref(null),person=ref({}),tab=ref('profile')
const roleOptions=ref([]),selectedRoles=ref([]),roleSaving=ref(false),permissionSearch=ref('')
const canAdmin=computed(()=>checkRole(['admin'])&&!detail.value?.isSuperAdmin)
const canEditRoles=computed(()=>checkPermi(['system:user:edit'])&&!detail.value?.isSuperAdmin)
const managed=r=>/^(auto_live_|personnel_freight_)/.test(r.roleKey||'')
const filteredPermissions=computed(()=>(detail.value?.permissions||[]).filter(r=>Object.values(r).join(' ').toLowerCase().includes(permissionSearch.value.toLowerCase())))
const roleName=code=>({anchor:'主播',controller:'场控',operator:'运营',customer_service:'电话客服',customer_admin:'客服管理员'}[code]||code)
let generation=0
async function open(row){const token=++generation;person.value=row;detail.value=null;tab.value='profile';permissionSearch.value='';visible.value=true;loading.value=true;try{const [res,roles]=await Promise.all([personnelDetail(row.userId),getAuthRole(row.userId)]);if(token===generation){detail.value=res.data;person.value={...row,...res.data.account};roleOptions.value=roles.roles||[];selectedRoles.value=roleOptions.value.filter(r=>r.flag).map(r=>r.roleId)}}finally{if(token===generation)loading.value=false}}
async function refresh(){const res=await personnelDetail(person.value.userId);detail.value=res.data;emit('changed')}
async function saveRoles(){await ElMessageBox.confirm('确认调整此人的人工系统角色？自动角色和业务分工继续由对应配置维护。','保存系统角色');roleSaving.value=true;try{await updateAuthRole({userId:person.value.userId,roleIds:selectedRoles.value.join(',')});ElMessage.success('系统角色已保存');await refresh()}finally{roleSaving.value=false}}
function go(path){visible.value=false;router.push(path)}
defineExpose({open})
</script>
<style scoped>
.person-header{display:flex;align-items:center;gap:16px;margin-bottom:24px}.initial{width:52px;height:52px;border-radius:14px;background:var(--el-color-primary-light-9);color:var(--el-color-primary);display:grid;place-items:center;font-size:24px}.person-header h2{margin:0 0 7px;font-size:23px}.person-header p{margin:0;color:var(--el-text-color-secondary);font-size:13px}.person-header>.el-tag{margin-left:auto}.section-heading{display:flex;justify-content:space-between;align-items:center;gap:15px;margin:16px 0}h3{font-size:15px;margin:24px 0 12px}.note{font-size:13px;color:var(--el-text-color-secondary);line-height:1.8}.responsibilities{display:flex;gap:40px;padding:24px 0;border-bottom:1px solid var(--el-border-color-light)}.responsibilities div{display:flex;gap:16px;align-items:center}.responsibilities strong{font-size:24px}.responsibilities span{font-size:13px;color:var(--el-text-color-secondary)}.role-option{display:flex;align-items:center;gap:14px;padding:10px 0;border-bottom:1px solid var(--el-border-color-lighter)}.role-option>span{font-size:12px;color:var(--el-text-color-secondary)}.el-checkbox-group{margin-bottom:20px}.permission-search{margin:0 0 16px;max-width:400px}@media(max-width:600px){.responsibilities{flex-direction:column;gap:14px}.section-heading{align-items:flex-start;flex-direction:column}.person-header p{line-height:1.7}}
</style>
