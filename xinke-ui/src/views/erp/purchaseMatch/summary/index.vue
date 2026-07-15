<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="供应商"><el-select v-model="queryParams.documentSupplierId" clearable filterable style="width:180px"><el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" /></el-select></el-form-item>
      <el-form-item label="采购单备注"><el-input v-model="queryParams.purchaseOrderRemark" clearable style="width:180px" /></el-form-item>
      <el-form-item label="商家编码"><el-input v-model="queryParams.merchantCode" clearable style="width:160px" /></el-form-item>
      <el-form-item label="匹配状态"><el-select v-model="queryParams.matchStatus" clearable style="width:150px"><el-option v-for="s in statuses" :key="s.value" :label="s.label" :value="s.value" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8 stat-row">
      <el-col :span="3"><el-statistic title="总数据量" :value="stats.totalCount || 0" /></el-col>
      <el-col :span="3"><el-statistic title="匹配成功" :value="stats.successCount || 0" /></el-col>
      <el-col :span="3"><el-statistic title="未找到订单" :value="stats.notFoundCount || 0" /></el-col>
      <el-col :span="3"><el-statistic title="物流为空" :value="stats.emptyCount || 0" /></el-col>
      <el-col :span="3"><el-statistic title="供应商未识别" :value="stats.supplierNotFoundCount || 0" /></el-col>
      <el-col :span="3"><el-statistic title="数据冲突" :value="stats.conflictCount || 0" /></el-col>
    </el-row>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Upload" @click="openUpload">上传Excel</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="Refresh" @click="rematch">重新匹配未成功</el-button></el-col>
      <el-col :span="1.5"><el-button plain icon="Download" @click="handleExport">导出</el-button></el-col>
      <right-toolbar @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="rows" border size="small">
      <el-table-column label="单据供应商" prop="documentSupplierName" width="150" fixed />
      <el-table-column label="采购单备注" prop="purchaseOrderRemark" width="180" fixed show-overflow-tooltip />
      <el-table-column label="合同备注" prop="contractRemark" width="160" show-overflow-tooltip />
      <el-table-column label="商家编码" prop="merchantCode" width="150" />
      <el-table-column label="采购数量" prop="purchaseQuantity" width="90" align="right" />
      <el-table-column label="匹配运单号" prop="matchedLogisticsNo" width="170" />
      <el-table-column label="运单号" prop="originalLogisticsNo" width="170" />
      <el-table-column label="核查发货" prop="shippingCheckStatus" width="100" />
      <el-table-column label="异常" prop="exceptionReason" width="150" show-overflow-tooltip />
      <el-table-column label="采购人员/备注" prop="purchaserRemark" width="170" show-overflow-tooltip />
      <el-table-column label="匹配状态" prop="matchStatus" width="130">
        <template #default="scope"><el-tag :type="tagType(scope.row.matchStatus)">{{ statusLabel(scope.row.matchStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="匹配说明" prop="matchMessage" width="200" show-overflow-tooltip />
      <el-table-column label="匹配时间" prop="matchTime" width="170" />
      <el-table-column label="上传人" prop="uploadUserName" width="110" />
      <el-table-column label="上传时间" prop="uploadTime" width="170" />
      <el-table-column label="来源文件" prop="sourceFileName" width="180" show-overflow-tooltip />
      <el-table-column label="操作" fixed="right" width="140">
        <template #default="scope"><el-button link type="primary" @click="openBind(scope.row)">手工指定</el-button><el-button link type="danger" @click="remove(scope.row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="bindOpen" title="手工指定手工订单" width="420px">
      <el-form label-width="110px"><el-form-item label="手工订单ID"><el-input v-model="bindForm.manualOrderId" placeholder="请输入手工订单ID" /></el-form-item></el-form>
      <template #footer><el-button @click="bindOpen=false">取消</el-button><el-button type="primary" @click="submitBind">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="uploadOpen" title="上传采购汇总表" width="900px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="Excel文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleUploadChange" :on-remove="clearUpload" accept=".xls,.xlsx">
            <el-button icon="Upload">选择文件</el-button>
          </el-upload>
          <el-button class="ml8" icon="Download" @click="downloadSummaryTemplate">下载模板</el-button>
        </el-form-item>
        <el-form-item v-if="uploadForm.busy || uploadForm.message">
          <div class="progress-block">
            <el-progress :percentage="uploadForm.progress" :status="uploadForm.progressStatus" />
            <div class="progress-text">{{ uploadForm.message }}</div>
          </div>
        </el-form-item>
      </el-form>
      <el-card v-if="uploadForm.preview" shadow="never" class="mb8">
        <template #header>
          <span>表头校验与前20行预览</span>
          <el-tag class="ml8" :type="uploadForm.preview.valid ? 'success' : 'danger'">{{ uploadForm.preview.valid ? '可导入' : '不可导入' }}</el-tag>
        </template>
        <el-alert v-if="uploadForm.preview.message" :title="uploadForm.preview.message" :type="uploadForm.preview.valid ? 'success' : 'warning'" show-icon class="mb8" />
        <el-table :data="uploadForm.preview.rows || []" border size="small" max-height="300">
          <el-table-column v-for="head in uploadForm.preview.headers" :key="head" :label="head" :prop="head" min-width="130" show-overflow-tooltip />
        </el-table>
      </el-card>
      <template #footer>
        <el-button @click="uploadOpen=false">关闭</el-button>
        <el-button type="success" icon="Check" :loading="uploadForm.busy && uploadForm.phase === 'confirm'" :disabled="!uploadForm.preview || !uploadForm.preview.valid" @click="confirmExcel">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="PurchaseSummary">
import { saveAs } from 'file-saver'
import { listSummary, summaryStats, delSummary, rematchFailed, manualBind, exportSummary, downloadSummaryTemplate as getSummaryTemplate, supplierOptions, previewImport, confirmImport } from '@/api/erp/purchaseMatch'
const { proxy } = getCurrentInstance()
const matchStatusMap = {
  SUCCESS: '匹配成功',
  NOT_FOUND: '未找到订单',
  LOGISTICS_EMPTY: '物流为空',
  MULTIPLE: '多条匹配',
  SUPPLIER_NOT_FOUND: '供应商未识别',
  CONFLICT: '数据冲突',
  PENDING: '待匹配'
}
const statuses = Object.entries(matchStatusMap).map(([value, label]) => ({ value, label }))
const suppliers = ref([]), rows = ref([])
const stats = ref({}), loading = ref(false), total = ref(0), bindOpen = ref(false), uploadOpen = ref(false)
const bindForm = ref({})
const uploadForm = reactive({ file: undefined, preview: undefined, busy: false, phase: '', progress: 0, progressStatus: '', message: '' })
const queryParams = ref({ pageNum: 1, pageSize: 10 })
function statusLabel(s){ return matchStatusMap[s] || s || '-' }
function tagType(s){ return ({SUCCESS:'success',NOT_FOUND:'danger',LOGISTICS_EMPTY:'warning',SUPPLIER_NOT_FOUND:'info',CONFLICT:'danger',MULTIPLE:'warning',PENDING:'info'})[s] || 'info' }
function getList(){ loading.value=true; listSummary(queryParams.value).then(r=>{rows.value=r.rows; total.value=r.total; loading.value=false}); summaryStats(queryParams.value).then(r=>stats.value=r.data||{}) }
function handleQuery(){ queryParams.value.pageNum=1; getList() }
function resetQuery(){ proxy.resetForm('queryRef'); handleQuery() }
function rematch(){ rematchFailed(queryParams.value).then(r=>{ proxy.$modal.msgSuccess(`已处理${r.data || 0}条`); getList() }) }
function openBind(row){ bindForm.value={ summaryId: row.summaryId }; bindOpen.value=true }
function submitBind(){ manualBind(bindForm.value).then(()=>{ proxy.$modal.msgSuccess('绑定成功'); bindOpen.value=false; getList() }) }
function remove(row){ proxy.$modal.confirm('确认删除该采购汇总？').then(()=>delSummary(row.summaryId)).then(()=>{ proxy.$modal.msgSuccess('删除成功'); getList() }) }
function handleExport(){ exportSummary(queryParams.value).then(blob => saveAs(blob, '采购汇总总表.xlsx')) }
function openUpload(){ clearUpload(); uploadOpen.value = true }
function clearUpload(){ uploadForm.file=undefined; uploadForm.preview=undefined; uploadForm.progress=0; uploadForm.progressStatus=''; uploadForm.message=''; uploadForm.busy=false; uploadForm.phase='' }
function downloadSummaryTemplate(){ getSummaryTemplate().then(blob => saveAs(blob, '采购汇总总表模板.xlsx')) }
function handleUploadChange(file){
  uploadForm.file = file.raw
  uploadForm.preview = undefined
  previewExcel()
}
function uploadData(){ const data = new FormData(); data.append('importType', 'PURCHASE_SUMMARY'); data.append('file', uploadForm.file); return data }
function startUpload(phase, message){ uploadForm.busy=true; uploadForm.phase=phase; uploadForm.progress=0; uploadForm.progressStatus=''; uploadForm.message=message }
function progressHandler(event){ if (!event.total) return; const percent = Math.round((event.loaded * 100) / event.total); uploadForm.progress = Math.min(percent, 95); if (percent >= 100) uploadForm.message = uploadForm.phase === 'confirm' ? '文件已上传，正在写入和匹配...' : '文件已上传，正在解析预览...' }
function finishUpload(message){ uploadForm.progress=100; uploadForm.progressStatus='success'; uploadForm.message=message; window.setTimeout(()=>{ uploadForm.busy=false; uploadForm.phase='' }, 400) }
function failUpload(message){ uploadForm.progressStatus='exception'; uploadForm.message=message; uploadForm.busy=false; uploadForm.phase='' }
function previewExcel(){
  if (!uploadForm.file) return proxy.$modal.msgWarning('请选择Excel文件')
  startUpload('preview', '正在上传并解析文件...')
  previewImport(uploadData(), progressHandler).then(res=>{ uploadForm.preview=res.data; finishUpload(res.data.valid ? '预览完成，请确认导入' : '表头校验未通过，请检查文件') }).catch(()=>failUpload('预览失败'))
}
function confirmExcel(){
  if (!uploadForm.preview || !uploadForm.preview.valid) return proxy.$modal.msgWarning('请先完成有效预览')
  startUpload('confirm', '正在导入，请不要关闭页面...')
  confirmImport(uploadData(), progressHandler).then(res=>{ proxy.$modal.msgSuccess(`导入完成：新增${res.data.successRows || 0}，冲突${res.data.conflictRows || 0}，失败${res.data.failedRows || 0}`); finishUpload('导入完成'); uploadOpen.value=false; getList() }).catch(()=>failUpload('导入失败'))
}
supplierOptions().then(r=>suppliers.value=r.data||[])
getList()
</script>

<style scoped>
.stat-row :deep(.el-statistic) { background:#fff; padding:10px 12px; border:1px solid #edf0f5; }
.mb8 { margin-bottom: 8px; }
.ml8 { margin-left: 8px; }
.progress-block { width: 100%; max-width: 520px; }
.progress-text { margin-top: 4px; color: #606266; font-size: 13px; }
</style>
