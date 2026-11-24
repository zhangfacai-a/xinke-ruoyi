<template>
  <el-drawer v-model="visible" title="业务分配与权限" size="min(840px, 100%)" append-to-body destroy-on-close>
    <div v-loading="loading" class="personnel-detail">
      <template v-if="detail">
        <h2>{{ person.nickName }} <small>{{ person.userName }}</small></h2>
        <el-alert v-if="detail.isSuperAdmin" title="此账号是超级管理员，拥有全部系统权限。" type="info" :closable="false" />
        <el-alert v-if="detail.localDisabled" title="此账号由网站停用；钉钉同步不会自动启用。" type="warning" :closable="false" />
        <div class="responsibilities"><div><span>未完成挽单</span><strong>{{ detail.pendingRetention }}</strong></div><div><span>待办跟进</span><strong>{{ detail.pendingFollowups }}</strong></div></div>
        <p class="note">停用前请核查待办并安排交接。停用账号会退出登录；历史负责人和业绩记录保留。</p>
        <h3>业务分配</h3>
        <el-table :data="detail.assignments" border empty-text="尚未分配店铺或直播间">
          <el-table-column prop="subjectName" label="店铺 / 直播间" min-width="170" />
          <el-table-column label="岗位" width="110"><template #default="{row}">{{ roleName(row.roleCode) }}</template></el-table-column>
          <el-table-column label="状态" width="95"><template #default="{row}"><el-tag :type="row.subjectStatus==='0'?'success':'info'">{{ row.subjectStatus==='0'?'启用':'已停用' }}</el-tag></template></el-table-column>
          <el-table-column label="配置来源" min-width="135"><template #default="{row}">{{ row.groupName || '单独配置' }}</template></el-table-column>
        </el-table>
        <h3>业务自动授权核对</h3>
        <el-table :data="detail.expectedAutoRoles" border empty-text="没有有效岗位触发自动授权">
          <el-table-column prop="roleName" label="岗位对应权限" /><el-table-column label="核对结果" width="140"><template #default="{row}"><el-tag :type="row.granted?'success':'danger'">{{ row.granted?'已同步':'待同步' }}</el-tag></template></el-table-column>
        </el-table>
        <h3>角色与数据范围</h3>
        <el-table :data="detail.roles" border empty-text="未关联系统角色">
          <el-table-column prop="roleName" label="角色" min-width="170" /><el-table-column prop="source" label="来源" min-width="150" /><el-table-column label="数据范围" min-width="125"><template #default="{row}">{{ scopeNames[row.dataScope] || row.dataScope }}</template></el-table-column><el-table-column label="状态" width="85"><template #default="{row}">{{ row.status==='0'?'有效':'停用' }}</template></el-table-column>
        </el-table>
        <h3>页面授权来源</h3>
        <el-table :data="detail.pages" border empty-text="无单独角色页面授权"><el-table-column prop="menuName" label="页面" /><el-table-column prop="roleName" label="来自角色" /></el-table>
        <p v-for="text in detail.publicAccess" :key="text" class="note">{{ text }}</p>
        <p class="note">实际数据访问还受账号状态和业务规则限制；多种角色可能同时授予同一页面。</p>
      </template>
    </div>
  </el-drawer>
</template>
<script setup>
import {ref} from 'vue'
import {personnelDetail} from '@/api/live/personnel'
const visible=ref(false),loading=ref(false),detail=ref(null),person=ref({})
const scopeNames={'1':'全部数据','2':'指定部门','3':'本部门','4':'本部门及下级','5':'本人'}
const roleName=code=>({anchor:'主播',controller:'场控',operator:'运营',customer_service:'电话客服',customer_admin:'客服管理员'}[code]||code)
let generation=0
async function open(row){const token=++generation;person.value=row;detail.value=null;visible.value=true;loading.value=true;try{const response=await personnelDetail(row.userId);if(token===generation)detail.value=response.data}finally{if(token===generation)loading.value=false}}
defineExpose({open})
</script>
<style scoped>
h2{margin:0 0 16px;font-size:20px}h2 small{margin-left:8px;color:#87909c;font-size:13px;font-weight:400}h3{margin:26px 0 10px;font-size:15px}.responsibilities{display:flex;gap:40px;padding:20px 0;border-bottom:1px solid #e5e7eb}.responsibilities div{display:flex;align-items:center;gap:16px}.responsibilities span,.note{color:#758090}.responsibilities strong{font-size:22px}.note{font-size:13px;line-height:1.8}
</style>
