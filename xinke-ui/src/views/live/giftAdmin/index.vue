<template>
  <div class="app-container material-center">
    <el-tabs v-if="tabs.length" v-model="activeTab" :class="['center-tabs', { 'single-section': fixedSection }]" @tab-change="loadActiveTab">
      <el-tab-pane v-if="canGift" label="礼品与库存" name="gift">
        <nav class="gift-workbench-nav" aria-label="礼品资料视图"><button v-for="item in giftSections" :key="item.value" type="button" :class="{active:giftSection===item.value}" @click="giftSection=item.value"><span>{{ item.label }}</span><small>{{ item.value==='catalog'?'维护名称、成本和搜索别名':'处理入库、盘点和库存流水' }}</small></button></nav>
        <GiftCatalog v-show="giftSection==='catalog'" embedded />
        <GiftInventory v-if="canInventory" v-show="giftSection==='inventory'" embedded />
      </el-tab-pane>
      <el-tab-pane v-if="canTemplate" label="快捷模板" name="template">
        <div class="template-toolbar"><div class="template-chips"><button :class="{active:!templateForm.templateId}" @click="newTemplate"><el-icon><Plus /></el-icon>新模板</button><button v-for="item in templates" :key="item.templateId" :class="{active:templateForm.templateId===item.templateId}" @click="selectTemplate(item)">{{ item.templateName }}</button></div><el-button icon="MagicStick" @click="openParser">一键解析</el-button></div>
        <div ref="templateEditorRef" class="template-editor">
          <section ref="templateComposeRef" class="template-compose">
            <div class="compose-head"><el-input v-model.trim="templateForm.templateName" maxlength="100" placeholder="给模板起个名字" /><div class="compose-actions"><el-switch v-model="templateForm.status" active-value="0" inactive-value="1" active-text="启用" /><el-button v-if="templateForm.templateId" icon="Delete" circle title="删除模板" @click="removeTemplate" /><el-button type="primary" icon="Check" :loading="saving" @click="submitTemplate">保存</el-button></div></div>
            <h4>点一下添加内容</h4>
            <div class="field-palette"><button v-for="item in availableFields" :key="item.key" @click="enableField(item.key)"><el-icon><Plus /></el-icon>{{ item.label }}</button></div>
            <div class="selected-fields">
              <div v-for="(field,index) in selectedFields" :key="field.key" :class="['selected-row',{boolean:field.type==='boolean'}]"><span>{{ field.label }}</span><el-checkbox v-if="field.type==='boolean'" v-model="field.value" class="boolean-check" @change="handleBooleanChange(field,$event)">勾选表示“是”</el-checkbox><el-select v-else-if="field.key==='anchor'||field.key==='controller'" v-model="field.value" filterable clearable :placeholder="roleOptions(field.key).length?'请选择账号':'请先在直播间配置中分配人员'"><el-option v-for="item in roleOptions(field.key)" :key="item.userId" :label="item.label" :value="item.userName" /></el-select><el-input-number v-else-if="field.type==='number'" v-model="field.value" :min="0" :precision="2" :step="1" :value-on-clear="null" placeholder="请输入金额" /><el-input v-else v-model="field.value" clearable placeholder="默认内容可不填" /><el-button icon="ArrowUp" text circle title="上移" :disabled="index===0" @click="moveSelected(field,-1)" /><el-button icon="ArrowDown" text circle title="下移" :disabled="index===selectedFields.length-1" @click="moveSelected(field,1)" /><el-button icon="Close" text circle type="danger" title="移除" @click="field.enabled=false" /></div>
              <el-empty v-if="!selectedFields.length" description="从上面点选需要的字段" :image-size="70" />
            </div>
            <div class="gift-heading"><h4>礼品</h4><el-button type="primary" link icon="Plus" @click="addTemplateGift">添加礼品</el-button></div>
            <div v-for="(gift,index) in templateForm.content.gifts" :key="index" class="gift-row"><el-select v-model="gift.giftId" filterable placeholder="选择礼品" @change="syncGiftName(gift)"><el-option v-for="item in giftOptions" :key="item.giftId" :label="`${item.giftName}（${item.giftCode}）`" :value="item.giftId" /></el-select><el-input-number v-model="gift.quantity" :min="1" :max="10" @change="normalizeGiftQuantity(gift)" /><el-button icon="Delete" text circle type="danger" title="删除礼品" @click="templateForm.content.gifts.splice(index,1)" /></div>
          </section>
          <aside class="template-preview"><div><strong>复制预览</strong><el-button type="primary" icon="CopyDocument" @click="copyPreview">一键复制</el-button></div><pre>{{ templatePreview || '添加字段后，这里会立即生成内容' }}</pre></aside>
        </div>
      </el-tab-pane>
    </el-tabs>
    <el-empty v-else description="当前账号没有直播资料权限，请联系管理员授权" />

    <el-dialog v-model="parserVisible" title="一键解析为模板" width="680px" destroy-on-close>
      <el-form label-position="top">
        <el-form-item label="模板名称"><el-input v-model.trim="parserName" maxlength="100" placeholder="可先不填，解析后再命名" /></el-form-item>
        <el-form-item label="粘贴内容"><el-input v-model="parserText" type="textarea" :rows="8" resize="vertical" placeholder="主播：张三 + 是否延保：是 + 礼品：汤锅｜数量：2｜编码：LP001" /></el-form-item>
      </el-form>
      <el-alert v-if="parserError" :title="parserError" type="error" show-icon :closable="false" />
      <div class="parser-tip">字段之间可用英文 + 或换行分隔；旧模板也能直接解析。</div>
      <template #footer><el-button @click="parserVisible=false">取消</el-button><el-button type="primary" icon="MagicStick" @click="applyParsedTemplate">解析并填入</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="GiftAdmin">
import GiftCatalog from '@/views/live/giftCatalog/index.vue'
import GiftInventory from '@/views/live/giftInventory/index.vue'
import { useRoute } from 'vue-router'
import {checkPermi} from '@/utils/permission'
import {assertLiveTemplateParsable,booleanValue,formatLiveTemplate,LIVE_TEMPLATE_FIELDS,parseLiveTemplate} from '@/utils/liveTemplateCodec'
import {deleteTemplate,getTemplate,listGifts,listMappings,listStaff,listTemplates,saveTemplate} from '@/api/live/gift'
const {proxy}=getCurrentInstance()
const props=defineProps({section:{type:String,default:''}})
const route=useRoute()
const canGift=checkPermi(['live:gift:catalog']),canInventory=checkPermi(['live:gift:inventory']),canTemplate=checkPermi(['live:gift:template'])
const sectionAccess={gift:canGift,template:canTemplate},fixedSection=computed(()=>['gift','template'].includes(props.section)?props.section:''),tabs=computed(()=>fixedSection.value?(sectionAccess[fixedSection.value]?[fixedSection.value]:[]):[canGift&&'gift',canTemplate&&'template'].filter(Boolean)),activeTab=ref(fixedSection.value||tabs.value[0]||''),saving=ref(false)
const giftSection=ref('catalog'),giftSections=computed(()=>[{label:'礼品档案',value:'catalog'},...(canInventory?[{label:'库存管理',value:'inventory'}]:[])])
const staffOptions=ref([])
const templates=ref([]),giftOptions=ref([]),templateMappings=ref([]),templateEditorRef=ref(),templateComposeRef=ref()
const parserVisible=ref(false),parserText=ref(''),parserName=ref(''),parserError=ref('')
const fields=()=>LIVE_TEMPLATE_FIELDS.map(item=>({...item,enabled:false,value:item.type==='boolean'?false:item.type==='number'?null:''}))
const emptyTemplate=()=>({templateId:null,templateName:'',status:'0',sortOrder:100,content:{fields:fields(),gifts:[]}}),templateForm=ref(emptyTemplate())
const ids=value=>String(value||'').split(',').filter(Boolean).map(Number),names=value=>String(value||'').split('、').filter(Boolean),userLabel=item=>item.departmentName?`${item.userName} · ${item.departmentName}`:item.userName
const selectedFields=computed(()=>templateForm.value.content.fields.filter(item=>item.enabled)),availableFields=computed(()=>templateForm.value.content.fields.filter(item=>!item.enabled))
const templatePreview=computed(()=>formatLiveTemplate(templateForm.value.content,giftOptions.value))
function roleOptions(key){const role=key==='anchor'?'anchor':'controller',staffById=new Map(staffOptions.value.map(item=>[Number(item.userId),item])),result=new Map();templateMappings.value.filter(item=>item.status==='0').forEach(mapping=>{const roleIds=ids(mapping[`${role}Ids`]),roleNames=names(mapping[`${role}Names`]);roleIds.forEach((userId,index)=>{const staff=staffById.get(userId),userName=staff?.userName||roleNames[index];if(userName&&!result.has(userId))result.set(userId,{userId,userName,label:staff?userLabel(staff):userName})})});return [...result.values()].sort((a,b)=>a.userName.localeCompare(b.userName,'zh-CN'))}
function parseContent(value){try{return typeof value==='string'?JSON.parse(value):value||{}}catch{return{}}}
function normalizeContent(value){const content=parseContent(value),defaults=fields(),saved=new Map((content.fields||[]).map(item=>[item.key,item]));return{fields:[...(content.fields||[]).map(item=>{const normalized={...defaults.find(base=>base.key===item.key),...item};if(normalized.type==='boolean'){normalized.value=booleanValue(normalized.value);normalized.enabled=normalized.enabled&&normalized.value}return normalized}),...defaults.filter(item=>!saved.has(item.key))],gifts:Array.isArray(content.gifts)?content.gifts:[]}}
async function loadActiveTab(name){if(name==='template')await loadTemplates()}
async function loadTemplates(){const[templateRes,giftRes,mappingRes,staffRes]=await Promise.all([listTemplates(),giftOptions.value.length?Promise.resolve({data:giftOptions.value}):listGifts({status:'0'}),listMappings({}),staffOptions.value.length?Promise.resolve({data:staffOptions.value}):listStaff({status:'0'})]);templates.value=templateRes.data||[];giftOptions.value=giftRes.data||[];templateMappings.value=mappingRes.data||[];staffOptions.value=staffRes.data||[];if(!templateForm.value.templateId&&templates.value.length)await selectTemplate(templates.value[0])}
function newTemplate(){templateForm.value=emptyTemplate()}
async function selectTemplate(item){const value=(await getTemplate(item.templateId)).data;templateForm.value={...value,content:normalizeContent(value.contentJson)}}
function enableField(key){const item=templateForm.value.content.fields.find(field=>field.key===key);if(item){item.enabled=true;if(item.type==='boolean')item.value=true}}
function handleBooleanChange(field,checked){if(!checked){field.enabled=false;field.value=false}}
function moveSelected(field,direction){const selected=selectedFields.value,index=selected.findIndex(item=>item.key===field.key),target=selected[index+direction];if(!target)return;const list=templateForm.value.content.fields,a=list.indexOf(field),b=list.indexOf(target);[list[a],list[b]]=[list[b],list[a]]}
async function addTemplateGift(){templateForm.value.content.gifts.push({giftId:null,giftName:'',quantity:1});await nextTick();templateComposeRef.value?.querySelector('.gift-row:last-child')?.scrollIntoView({behavior:'smooth',block:'center'})}
function normalizeGiftQuantity(gift){gift.quantity=Math.min(10,Math.max(1,Number(gift.quantity)||1))}
function syncGiftName(gift){const selected=giftOptions.value.find(item=>item.giftId===gift.giftId)||{};gift.giftName=selected.giftName||'';gift.giftCode=selected.giftCode||''}
function openParser(){parserName.value='';parserText.value='';parserError.value='';parserVisible.value=true}
const normalizedToken=value=>String(value||'').trim().toLocaleLowerCase()
function giftTokens(gift){return [gift.giftName,gift.shortName,...String(gift.aliases||'').split(/[，,、]/)].map(normalizedToken).filter(Boolean)}
function resolveParsedGift(item){const code=normalizedToken(item.giftCode),name=normalizedToken(item.giftName);return giftOptions.value.find(gift=>code&&normalizedToken(gift.giftCode)===code)||giftOptions.value.find(gift=>giftTokens(gift).includes(name))}
async function applyParsedTemplate(){parserError.value='';if(!parserText.value.trim()){parserError.value='请先粘贴需要解析的内容';return}try{const parsed=parseLiveTemplate(parserText.value),defaults=fields(),byKey=new Map(defaults.map(item=>[item.key,item])),orderedKeys=[...parsed.fieldOrder,...defaults.map(item=>item.key).filter(key=>!parsed.fieldOrder.includes(key))],parsedFields=orderedKeys.map(key=>{const field=byKey.get(key),hasValue=Object.prototype.hasOwnProperty.call(parsed.fields,key),value=parsed.fields[key];return{...field,enabled:hasValue&&(field.type!=='boolean'||value===true),value:field.type==='boolean'?value===true:(hasValue?value:'')}}),unresolved=[],gifts=[];parsed.gifts.forEach(item=>{const match=resolveParsedGift(item);if(!match){unresolved.push(item.giftCode?`${item.giftName}（${item.giftCode}）`:item.giftName);return}gifts.push({giftId:match.giftId,giftName:match.giftName,giftCode:match.giftCode,quantity:item.quantity})});templateForm.value={...emptyTemplate(),templateName:parserName.value,content:{fields:parsedFields,gifts}};parserVisible.value=false;await nextTick();templateEditorRef.value?.scrollIntoView({behavior:'smooth',block:'start'});if(unresolved.length)proxy.$modal.msgWarning(`以下礼品未匹配，请手动添加：${unresolved.join('、')}`);else proxy.$modal.msgSuccess(`已解析 ${parsed.fieldOrder.length} 个字段、${gifts.length} 个礼品，请确认后保存`)}catch(error){parserError.value=error.message||'内容无法解析，请检查格式'}}
async function submitTemplate(){if(!templateForm.value.templateName)return proxy.$modal.msgWarning('请输入模板名称');if(!selectedFields.value.length&&!templateForm.value.content.gifts.some(item=>item.giftId))return proxy.$modal.msgWarning('请至少添加一个字段或礼品');try{assertLiveTemplateParsable(templateForm.value.content,giftOptions.value)}catch(error){return proxy.$modal.msgError(error.message)}saving.value=true;try{await saveTemplate(templateForm.value);proxy.$modal.msgSuccess('模板已保存');templateForm.value=emptyTemplate();await loadTemplates()}finally{saving.value=false}}
async function removeTemplate(){await proxy.$modal.confirm(`确认删除模板“${templateForm.value.templateName}”吗？`);await deleteTemplate(templateForm.value.templateId);proxy.$modal.msgSuccess('模板已删除');templateForm.value=emptyTemplate();await loadTemplates()}
async function copyPreview(){if(!templatePreview.value)return proxy.$modal.msgWarning('暂无可复制内容');try{const {text}=assertLiveTemplateParsable(templateForm.value.content,giftOptions.value);await navigator.clipboard.writeText(text);proxy.$modal.msgSuccess('已复制，可由系统直接解析')}catch(error){proxy.$modal.msgError(error.message)}}
onMounted(()=>{if(route.query.section==='inventory'&&canInventory){activeTab.value='gift';giftSection.value='inventory'}loadActiveTab(activeTab.value)})
</script>

<style scoped>
.material-center{max-width:1600px}.gift-workbench-nav{display:flex;gap:0;margin-bottom:14px;border-bottom:1px solid #dfe3e8}.gift-workbench-nav button{display:flex;min-width:220px;flex-direction:column;gap:3px;padding:11px 18px;border:0;border-bottom:3px solid transparent;background:transparent;color:#6b7280;text-align:left;cursor:pointer}.gift-workbench-nav button.active{border-bottom-color:#f26b21;color:#c2410c}.gift-workbench-nav span{font-size:15px;font-weight:700}.gift-workbench-nav small{font-size:11px;color:#909399}.center-tabs :deep(.el-tabs__header){margin-bottom:16px}.template-toolbar{display:flex;align-items:flex-start;gap:10px;padding-bottom:12px}.template-chips{display:flex;flex:1;gap:8px;overflow-x:auto}.template-chips button,.field-palette button{display:inline-flex;align-items:center;gap:5px;border:1px solid #d8dde3;border-radius:5px;background:#fff;color:#4b5563;cursor:pointer}.template-chips button{flex:none;padding:9px 14px}.template-chips button.active,.field-palette button:hover{border-color:#f26b21;color:#d85209;background:#fff7f2}.template-editor{display:grid;grid-template-columns:minmax(480px,1.25fr) minmax(320px,.75fr);gap:24px;padding-top:16px;border-top:1px solid #ebeef5;scroll-margin-top:12px}.template-compose{min-width:0}.compose-head,.compose-actions,.gift-heading,.template-preview>div{display:flex;align-items:center}.compose-head{gap:12px}.compose-head>.el-input{max-width:440px}.compose-actions{gap:10px;margin-left:auto}.template-compose h4{margin:22px 0 10px}.field-palette{display:flex;flex-wrap:wrap;gap:8px}.field-palette button{padding:8px 11px}.selected-fields{margin-top:14px}.selected-row{display:grid;grid-template-columns:105px minmax(150px,1fr) 32px 32px 32px;align-items:center;gap:5px;padding:7px 0;border-bottom:1px solid #eef0f2}.selected-row>span{font-weight:600}.selected-row :deep(.el-input-number){width:100%}.gift-heading{justify-content:space-between}.gift-row{display:grid;grid-template-columns:minmax(180px,1fr) 150px 36px;gap:8px;margin-bottom:8px;scroll-margin:80px}.template-preview{position:sticky;top:10px;height:max-content;padding:16px;border-left:3px solid #f26b21;background:#f7f8fa}.template-preview>div{justify-content:space-between}.template-preview pre{min-height:300px;margin:14px 0 0;white-space:pre-wrap;word-break:break-word;font:14px/1.9 Consolas,"Microsoft YaHei",sans-serif}.parser-tip{margin-top:10px;color:#7a8591;font-size:13px}.boolean-check{min-height:32px;padding:0 10px;border:1px solid #dcdfe6;border-radius:4px;background:#fafafa}.boolean-check.is-checked{border-color:#f26b21;background:#fff7f2}.single-section :deep(.el-tabs__header){display:none}.single-section :deep(.el-tabs__content){overflow:visible}@media(max-width:900px){.template-editor{grid-template-columns:1fr}.template-preview{position:static;border-left:0;border-top:3px solid #f26b21}}@media(max-width:650px){.gift-workbench-nav button{min-width:0;flex:1;padding:10px}.gift-workbench-nav small{display:none}}
</style>
