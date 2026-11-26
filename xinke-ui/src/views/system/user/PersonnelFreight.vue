<template>
  <section v-loading="loading" class="freight-settings">
    <template v-if="loaded">
      <div class="section-heading"><div><h3>运费征收</h3><p>查看与操作独立设置。保存个人配置后，运费将按这里的设置执行。</p></div><el-tag :type="data.policy?'success':'info'">{{ data.policy?'个人配置生效':'沿用现有角色与分工' }}</el-tag></div>
      <el-alert v-if="!data.policy" title="当前尚无个人配置。下面列出已有授权来源，首次保存会改为按个人配置执行。" type="info" :closable="false" />
      <el-form label-position="top" :disabled="!editable || saving" class="policy-form">
        <el-form-item label="允许使用的功能"><el-checkbox-group v-model="form.actions"><el-checkbox v-for="a in actions" :key="a.value" :value="a.value">{{ a.label }}</el-checkbox></el-checkbox-group></el-form-item>
        <div class="scope-grid">
          <div><el-form-item label="查看范围"><el-select v-model="form.viewScope"><el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" /></el-select></el-form-item><el-form-item v-if="form.viewScope==='SELECTED'" label="允许查看的店铺"><el-select v-model="form.viewShopIds" multiple filterable><el-option v-for="s in data.shops" :key="s.shopId" :label="s.shopName+(s.status==='0'?'':'（已停用）')" :value="s.shopId" :disabled="s.status!=='0'" /></el-select></el-form-item></div>
          <div><el-form-item label="操作范围"><el-select v-model="form.operateScope"><el-option v-for="s in scopes" :key="s.value" :label="s.label" :value="s.value" /></el-select></el-form-item><el-form-item v-if="form.operateScope==='SELECTED'" label="允许操作的店铺"><el-select v-model="form.operateShopIds" multiple filterable><el-option v-for="s in data.shops" :key="s.shopId" :label="s.shopName+(s.status==='0'?'':'（已停用）')" :value="s.shopId" :disabled="s.status!=='0'" /></el-select></el-form-item></div>
        </div>
        <p class="help">“本人”只查看或修改本人录入的明细；新增、导入限已分配的客服店铺。“负责店铺”包含店铺客服及客服管理员分工；“指定店铺”和“全部店铺”无需再配置客服岗位。编辑转移到其他店铺时也会校验目标店铺。</p>
        <div class="policy-preview"><strong>{{ person.nickName }} 的配置预览</strong><p>{{ actionText }}；查看：{{ scopeText(form.viewScope,form.viewShopIds) }}；操作：{{ scopeText(form.operateScope,form.operateShopIds) }}。</p><small>不勾选任何功能并保存，将关闭此人的运费功能；其他业务权限不受影响。</small></div>
        <el-button v-if="editable" type="primary" :loading="saving" @click="save">保存运费权限</el-button>
      </el-form>
      <h3>现有角色授权来源</h3><el-table :data="data.sources" empty-text="没有角色授予运费权限"><el-table-column prop="roleName" label="角色" /><el-table-column label="授权内容"><template #default="{row}">{{ permLabels[row.perms] || row.perms }}</template></el-table-column></el-table>
      <h3>最近的权限调整</h3><el-table :data="data.history" empty-text="暂无个人配置调整记录"><el-table-column prop="actorName" label="操作人" width="110" /><el-table-column label="时间" width="170"><template #default="{row}">{{ String(row.createdAt).replace('T',' ') }}</template></el-table-column><el-table-column label="调整后"><template #default="{row}">{{ historyLabel(row.afterConfig) }}</template></el-table-column></el-table>
    </template>
  </section>
</template>
<script setup>
import {ref,computed,onMounted} from 'vue'
import {ElMessage,ElMessageBox} from 'element-plus'
import {personnelFreight,savePersonnelFreight} from '@/api/live/personnel'
const props=defineProps({person:Object,editable:Boolean})
const emit=defineEmits(['changed'])
const loading=ref(false),saving=ref(false),loaded=ref(false),data=ref({shops:[],sources:[],history:[]})
const form=ref({actions:[],viewScope:'OWN',operateScope:'OWN',viewShopIds:[],operateShopIds:[],version:0})
const actions=[{value:'view',label:'查看明细'},{value:'dashboard',label:'查看看板'},{value:'create',label:'新增'},{value:'edit',label:'编辑'},{value:'import',label:'导入'},{value:'export',label:'导出'}]
const scopes=[{value:'OWN',label:'本人明细'},{value:'ASSIGNED',label:'负责店铺'},{value:'SELECTED',label:'指定店铺'},{value:'ALL',label:'全部店铺'}]
const scopeLabel=v=>scopes.find(s=>s.value===v)?.label||v
const scopeText=(scope,ids)=>scope==='SELECTED'?`指定店铺（${ids.map(id=>data.value.shops.find(s=>String(s.shopId)===String(id))?.shopName||id).join('、')||'未选择'}）`:scopeLabel(scope)
const actionText=computed(()=>actions.filter(a=>form.value.actions.includes(a.value)).map(a=>a.label).join('、')||'不允许使用运费功能')
const permLabels={'live:freight:access':'原有运费业务操作','live:freight:viewAll':'原有全店铺查看','live:freight:entry':'个人配置入口'}
function historyLabel(raw){try{const p=JSON.parse(raw);return `${(p.actions||[]).map(v=>actions.find(a=>a.value===v)?.label||v).join('、')||'关闭功能'} / 查看${scopeLabel(p.viewScope)} / 操作${scopeLabel(p.operateScope)}`}catch{return '配置调整'}}
async function load(){loading.value=true;try{data.value=(await personnelFreight(props.person.userId)).data;const p=data.value.policy;if(p)form.value={...p};loaded.value=true}finally{loading.value=false}}
async function save(){
 if(form.value.actions.length&&!form.value.actions.includes('view')){ElMessage.warning('请先勾选查看明细');return}
 if((form.value.viewScope==='SELECTED'&&!form.value.viewShopIds.length)||(form.value.operateScope==='SELECTED'&&!form.value.operateShopIds.length)){ElMessage.warning('请选择指定店铺');return}
 await ElMessageBox.confirm(`将${props.person.nickName}的运费权限改为：${actionText.value}。查看${scopeLabel(form.value.viewScope)}，操作${scopeLabel(form.value.operateScope)}。确认保存？`,'确认权限变更')
 saving.value=true;try{await savePersonnelFreight(props.person.userId,form.value);ElMessage.success('权限已保存，后台立即生效');await load();emit('changed')}finally{saving.value=false}
}
onMounted(load)
</script>
<style scoped>
.section-heading{display:flex;justify-content:space-between;align-items:center;gap:15px}.section-heading h3{margin:0}.section-heading p,.help{font-size:13px;color:var(--el-text-color-secondary);line-height:1.8}.policy-form{margin-top:24px}.scope-grid{display:grid;grid-template-columns:1fr 1fr;gap:24px}.el-select{width:100%}.policy-preview{padding:18px 20px;margin:20px 0;background:var(--el-fill-color-light);border-radius:8px}.policy-preview p{font-size:14px;line-height:1.8}.policy-preview small{color:var(--el-text-color-secondary)}h3{font-size:15px;margin:28px 0 12px}@media(max-width:600px){.scope-grid{grid-template-columns:1fr}.section-heading{align-items:flex-start;flex-direction:column}}
</style>
