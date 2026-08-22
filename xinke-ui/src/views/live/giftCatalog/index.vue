<template>
  <div :class="['gift-catalog-page', { 'app-container': !embedded }]">
    <div v-if="!embedded" class="page-head">
      <div><h2>礼品管理</h2><p>统一维护礼品名称、搜索别名和分日期成本。</p></div>
      <div class="head-actions">
        <el-upload :show-file-list="false" :http-request="handleImport" accept=".xlsx,.xls"><el-button icon="Upload" :loading="importing">导入历史成本</el-button></el-upload>
        <el-button type="primary" icon="Plus" @click="openGift()">新增礼品</el-button>
      </div>
    </div>

    <div v-else class="embedded-actions">
      <el-upload :show-file-list="false" :http-request="handleImport" accept=".xlsx,.xls"><el-button icon="Upload" :loading="importing">导入历史成本</el-button></el-upload>
      <el-button type="primary" icon="Plus" @click="openGift()">新增礼品</el-button>
    </div>

    <div class="filter-bar">
      <el-input v-model.trim="query.keyword" clearable placeholder="搜索名称、编码、别名或拼音简写" @keyup.enter="load"><template #prefix><el-icon><Search /></el-icon></template></el-input>
      <el-segmented v-model="query.status" :options="statusOptions" @change="load" />
      <el-button icon="Refresh" title="刷新" @click="load" />
      <span class="result-count">共 {{ rows.length }} 个礼品</span>
    </div>

    <div v-loading="loading" class="gift-grid">
      <article v-for="row in rows" :key="row.giftId" :class="['gift-card',{disabled:row.status!=='0','personal-hidden':row.personalHidden}]">
        <div class="gift-card-head"><div class="gift-title"><strong>{{ row.giftName }}</strong><span class="code">{{ row.giftCode }}</span></div><el-switch v-model="row.status" active-value="0" inactive-value="1" :loading="row.statusSaving" @change="toggleStatus(row)" /></div>
        <div class="gift-card-main"><div><small>当前成本</small><strong v-if="row.currentCost!=null" class="cost">¥{{ money(row.currentCost) }}</strong><el-tag v-else type="danger" effect="plain">未设置</el-tag></div><button type="button" class="history-entry" @click="openHistory(row)"><el-icon><Clock /></el-icon><span><strong>变更记录</strong><small>资料版本与成本历史</small></span><el-icon><ArrowRight /></el-icon></button></div>
        <div v-if="row.aliases" class="alias-line" :title="row.aliases">别名：{{ row.aliases }}</div>
        <div class="gift-card-actions"><el-button link type="primary" icon="Edit" @click="openGift(row)">编辑</el-button><el-button v-hasPermi="['live:gift:cost']" link type="warning" icon="Money" @click="openCost(row)">调成本</el-button></div>
      </article>
      <el-empty v-if="!loading&&!rows.length" description="没有找到礼品" class="grid-empty" />
    </div>

    <el-dialog v-model="giftVisible" :title="giftForm.giftId ? '编辑礼品' : '新增礼品'" width="680px" destroy-on-close>
      <el-form ref="giftFormRef" :model="giftForm" :rules="giftRules" label-position="top" class="gift-form">
        <el-form-item label="礼品编码" prop="giftCode"><el-input v-model.trim="giftForm.giftCode" maxlength="50" :disabled="!!giftForm.giftId" /><div v-if="giftForm.giftId" class="field-tip">创建后不可修改</div></el-form-item>
        <el-form-item label="礼品名称" prop="giftName"><el-input v-model.trim="giftForm.giftName" maxlength="100" autofocus /></el-form-item>
        <el-form-item label="简称"><el-input v-model.trim="giftForm.shortName" maxlength="50" placeholder="用于快速搜索，可不填" /></el-form-item>
        <el-form-item label="规格"><el-input v-model.trim="giftForm.specification" maxlength="100" placeholder="例如：500ml、红色" /></el-form-item>
        <el-form-item label="单位"><el-input v-model.trim="giftForm.unit" maxlength="20" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="giftForm.sortOrder" :min="1" :max="9999" controls-position="right" /></el-form-item>
        <el-form-item label="搜索别名" class="full"><el-select v-model="giftForm.aliases" multiple filterable allow-create default-first-option clearable placeholder="输入别名后按回车，可添加多个" style="width:100%" /><div class="field-tip">保存时自动补充礼品名称和别名的中文拼音首字母。</div></el-form-item>
        <template v-if="!giftForm.giftId">
          <div class="form-divider full">首个成本</div>
          <el-form-item label="单位成本" prop="unitCost"><el-input-number v-model="giftForm.unitCost" :min="0" :precision="2" :step="1" controls-position="right" /></el-form-item>
          <el-form-item label="生效日期" prop="effectiveDate"><el-date-picker v-model="giftForm.effectiveDate" value-format="YYYY-MM-DD" /></el-form-item>
          <el-form-item v-if="giftForm.unitCost === 0" label="0元原因" class="full"><el-input v-model="giftForm.changeReason" placeholder="请说明免费、赠送或暂估原因" /></el-form-item>
        </template>
        <el-form-item label="状态"><el-radio-group v-model="giftForm.status"><el-radio value="0">启用</el-radio><el-radio value="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="备注"><el-input v-model="giftForm.remark" maxlength="500" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="giftVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitGift">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="costVisible" title="调整礼品成本" width="500px" destroy-on-close>
      <div class="cost-target"><span>礼品</span><strong>{{ costForm.giftName }}</strong><small>当前成本 ¥{{ money(costForm.currentCost) }}</small></div>
      <el-form ref="costFormRef" :model="costForm" :rules="costRules" label-position="top">
        <el-form-item label="新单位成本" prop="unitCost"><el-input-number v-model="costForm.unitCost" :min="0" :precision="2" :step="1" controls-position="right" /></el-form-item>
        <el-form-item label="生效日期" prop="effectiveDate"><el-date-picker v-model="costForm.effectiveDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="调整原因" :required="costForm.unitCost === 0"><el-input v-model="costForm.changeReason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="例如：供应商调价" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="costVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitCost">确认调整</el-button></template>
    </el-dialog>

    <el-drawer v-model="historyVisible" :title="`${historyGift.giftName || ''} · 变更记录`" size="560px">
      <div class="history-current"><span>当前成本</span><strong>¥{{ money(historyGift.currentCost) }}</strong></div>
      <h4 class="history-title">资料版本</h4>
      <el-timeline v-if="historyGift.versions?.length">
        <el-timeline-item v-for="item in historyGift.versions" :key="item.versionId" :timestamp="item.createTime" placement="top">
          <div class="history-item"><strong>{{ actionText(item.actionType) }} · {{ item.giftName }}</strong><span>编码 {{ item.giftCode }} · {{ item.status==='0'?'启用':'停用' }}</span><small>{{ item.createBy || '-' }}<template v-if="item.aliases"> · 别名：{{ item.aliases }}</template></small></div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无资料版本" :image-size="72" />
      <h4 class="history-title">成本记录</h4>
      <el-timeline v-if="historyGift.costs?.length">
        <el-timeline-item v-for="item in costHistory" :key="item.costId" :timestamp="`${item.effectiveDate} 起生效`" placement="top">
          <div class="history-item"><strong>¥{{ money(item.unitCost) }} <el-icon v-if="item.costDirection" :class="['cost-change',item.costDirection]" :title="item.costDirection==='up'?'较上一条成本上涨':'较上一条成本下降'"><ArrowUp v-if="item.costDirection==='up'" /><ArrowDown v-else /></el-icon></strong><span>{{ item.changeReason || '未填写调整原因' }}</span><small>{{ item.createBy || '-' }} · {{ item.createTime || '' }}</small></div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无成本记录" />
    </el-drawer>
  </div>
</template>

<script setup name="GiftCatalog">
import { addGiftCost, getGift, importGiftCosts, listGifts, saveGift } from '@/api/live/gift'
const props = defineProps({ embedded: { type: Boolean, default: false } })
const { proxy }=getCurrentInstance()
const today=()=>new Date().toISOString().slice(0,10)
const statusOptions=[{label:'全部',value:''},{label:'启用',value:'0'},{label:'停用',value:'1'}]
const rows=ref([]),loading=ref(false),giftVisible=ref(false),costVisible=ref(false),historyVisible=ref(false),importing=ref(false),saving=ref(false),giftFormRef=ref(),costFormRef=ref()
const query=ref({keyword:'',status:'0'}),giftForm=ref({}),costForm=ref({}),historyGift=ref({}),knownGiftNames=ref(new Set())
const costHistory=computed(()=>{const costs=historyGift.value.costs||[];return costs.map((item,index)=>{const previous=costs[index+1];const diff=previous==null?0:Number(item.unitCost)-Number(previous.unitCost);return {...item,costDirection:diff>0?'up':diff<0?'down':''}})})
function validateGiftName(rule,value,callback){if(!giftForm.value.giftId&&knownGiftNames.value.has(String(value||'').trim()))callback(new Error('礼品名称已经存在，不能重复新建'));else callback()}
const giftRules={giftCode:[{required:true,message:'请输入礼品编码',trigger:'blur'}],giftName:[{required:true,message:'请输入礼品名称',trigger:'blur'},{validator:validateGiftName,trigger:'blur'}],unitCost:[{required:true,message:'请输入单位成本',trigger:'change'}],effectiveDate:[{required:true,message:'请选择生效日期',trigger:'change'}]}
const costRules={unitCost:[{required:true,message:'请输入新成本',trigger:'change'}],effectiveDate:[{required:true,message:'请选择生效日期',trigger:'change'}]}
const money=value=>Number(value||0).toFixed(2)
const actionText=value=>({CREATE:'新建',EDIT:'编辑',STATUS:'启停',BASELINE:'历史基线'}[value]||'变更')
const aliasArray=value=>Array.isArray(value)?value:String(value||'').split(/[，,、]/).map(item=>item.trim()).filter(Boolean)
let searchTimer
watch(()=>query.value.keyword,()=>{clearTimeout(searchTimer);searchTimer=setTimeout(load,250)})
async function load(){loading.value=true;try{rows.value=(await listGifts({...query.value,...(props.embedded?{}:{includeHidden:'1'})})).data||[]}finally{loading.value=false}}
async function openGift(row){if(row){const detail=(await getGift(row.giftId)).data;giftForm.value={...detail,aliases:aliasArray(detail.aliases)}}else{const all=(await listGifts({})).data||[];knownGiftNames.value=new Set(all.map(item=>String(item.giftName||'').trim()));giftForm.value={giftCode:`LP${new Date().toISOString().replace(/\D/g,'').slice(2,14)}`,giftName:'',aliases:[],unit:'件',status:'0',sortOrder:100,unitCost:null,effectiveDate:today(),changeReason:''}}giftVisible.value=true}
async function submitGift(){await giftFormRef.value.validate();if(giftForm.value.unitCost===0&&!String(giftForm.value.changeReason||'').trim())return proxy.$modal.msgWarning('0元成本请填写原因');saving.value=true;try{await saveGift(giftForm.value);proxy.$modal.msgSuccess('礼品已保存');giftVisible.value=false;await load()}finally{saving.value=false}}
async function toggleStatus(row){row.statusSaving=true;try{await saveGift({...row,aliases:aliasArray(row.aliases),actionType:'STATUS'});proxy.$modal.msgSuccess(row.status==='0'?'礼品已启用':'礼品已停用')}catch(error){row.status=row.status==='0'?'1':'0';throw error}finally{row.statusSaving=false}}
function openCost(row){costForm.value={giftId:row.giftId,giftName:row.giftName,currentCost:row.currentCost,unitCost:row.currentCost,effectiveDate:today(),changeReason:''};costVisible.value=true}
async function submitCost(){await costFormRef.value.validate();if(costForm.value.unitCost===0&&!String(costForm.value.changeReason||'').trim())return proxy.$modal.msgWarning('0元成本请填写原因');saving.value=true;try{await addGiftCost(costForm.value);proxy.$modal.msgSuccess('成本已调整');costVisible.value=false;await load()}finally{saving.value=false}}
async function openHistory(row){historyGift.value={...row,costs:[]};historyVisible.value=true;historyGift.value={...row,...(await getGift(row.giftId)).data,currentCost:row.currentCost}}
async function handleImport(options){const data=new FormData();data.append('file',options.file);importing.value=true;try{const result=(await importGiftCosts(data)).data;proxy.$modal.msgSuccess(`导入完成：成功 ${result.success}，失败 ${result.failure}`);if(result.failures?.length)proxy.$alert(result.failures.slice(0,30).join('\n'),'未导入记录');await load()}finally{importing.value=false}}
load()
</script>

<style scoped>
.embedded-actions{display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-bottom:8px}
.gift-grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(285px,1fr));gap:12px;min-height:180px;padding-top:14px}.gift-card{display:flex;min-width:0;flex-direction:column;padding:14px;border:1px solid #dfe3e8;border-radius:6px;background:#fff;transition:border-color .15s,box-shadow .15s}.gift-card:hover{border-color:#f26b21;box-shadow:0 4px 14px rgba(23,32,51,.08)}.gift-card.disabled{background:#f7f8fa;opacity:.72}.gift-card-head,.gift-card-actions{display:flex;align-items:center}.gift-card-head{justify-content:space-between;gap:10px}.gift-title{min-width:0}.gift-title strong,.gift-title span{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.gift-title strong{font-size:15px}.gift-title .code{margin-top:4px;color:#8a949f;font-size:11px}.gift-card-main{display:grid;grid-template-columns:minmax(90px,.7fr) minmax(165px,1.3fr);align-items:center;gap:12px;margin-top:14px;padding:12px 0;border-top:1px solid #eef0f2;border-bottom:1px solid #eef0f2}.gift-card-main>div{min-width:0}.gift-card-main small{display:block;margin-bottom:5px;color:#909399}.gift-card-main .cost{font-size:17px}.history-entry{display:grid;grid-template-columns:22px minmax(0,1fr) 18px;align-items:center;gap:7px;min-width:0;padding:8px 9px;border:1px solid #e4e7ed;border-radius:5px;background:#fafafa;color:#606266;text-align:left;cursor:pointer}.history-entry:hover{border-color:#f26b21;background:#fff7f2;color:#d85209}.history-entry>span{min-width:0}.history-entry strong,.history-entry small{display:block;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.history-entry strong{font-size:14px}.history-entry small{margin:2px 0 0;font-size:11px}.alias-line{overflow:hidden;margin-top:9px;color:#7a8591;font-size:12px;text-overflow:ellipsis;white-space:nowrap}.gift-card-actions{justify-content:flex-end;margin-top:auto;padding-top:9px}.grid-empty{grid-column:1/-1}
.gift-catalog-page{max-width:1600px}.page-head,.head-actions,.filter-bar,.cost-target,.history-current{display:flex;align-items:center}.page-head{justify-content:space-between;gap:16px;margin-bottom:18px}.page-head h2{margin:0 0 4px;font-size:22px}.page-head p{margin:0;color:#6b7280}.head-actions{gap:8px}.filter-bar{gap:10px;padding:12px 0;border-bottom:1px solid #ebeef5}.filter-bar .el-input{width:360px}.result-count{margin-left:auto;color:#6b7280;font-size:13px}.code{font-family:Consolas,monospace}.sub-text{margin-top:3px;color:#6b7280;font-size:12px}.cost{color:#c2410c}.gift-form{display:grid;grid-template-columns:1fr 1fr;gap:0 16px}.gift-form .full{grid-column:1/-1}.form-divider{padding:5px 0 12px;border-top:1px solid #ebeef5;font-weight:600}.gift-form :deep(.el-input-number),.gift-form :deep(.el-date-editor){width:100%}.field-tip{width:100%;margin-top:4px;color:#909399;font-size:12px}.cost-target{gap:12px;padding:12px;margin-bottom:14px;background:#f8fafc}.cost-target span,.cost-target small{color:#6b7280}.cost-target small{margin-left:auto}.history-current{justify-content:space-between;padding:14px 0 20px;border-bottom:1px solid #ebeef5;margin-bottom:20px}.history-current strong{font-size:26px;color:#c2410c}.history-title{margin:20px 0 14px;font-size:15px}.history-item{display:flex;flex-direction:column;gap:5px;padding:4px 0}.history-item strong{font-size:16px}.history-item span{color:#374151}.history-item small{color:#909399}
@media(max-width:720px){.page-head{align-items:flex-start;flex-direction:column}.head-actions,.embedded-actions{align-self:stretch;flex-wrap:wrap;justify-content:flex-end}.filter-bar{align-items:stretch;flex-wrap:wrap}.filter-bar .el-input{width:100%}.result-count{margin-left:0}.gift-grid{grid-template-columns:1fr}.gift-form{grid-template-columns:1fr}.gift-form .full{grid-column:auto}}
.cost-change{font-size:15px;vertical-align:-1px}.cost-change.up{color:#dc2626}.cost-change.down{color:#16a34a}
</style>
