<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="供应商"><el-select v-model="queryParams.supplierId" clearable filterable style="width:180px"><el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" /></el-select></el-form-item>
      <el-form-item label="订单号"><el-input v-model="queryParams.orderNo" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="型号"><el-input v-model="queryParams.productModel" clearable style="width:160px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item label="物流单号"><el-input v-model="queryParams.logisticsNo" clearable style="width:180px" @keyup.enter="handleQuery" /></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5"><el-button type="primary" plain icon="Upload" @click="openUpload">上传Excel</el-button></el-col>
      <el-col :span="1.5"><el-button type="primary" plain icon="Plus" @click="openForm()">新增订单</el-button></el-col>
      <el-col :span="1.5"><el-button plain icon="Download" @click="handleExport">导出</el-button></el-col>
      <right-toolbar @queryTable="getList" />
    </el-row>
    <el-table v-loading="loading" :data="rows" border size="small">
      <el-table-column label="日期" prop="orderDate" width="110" fixed />
      <el-table-column label="供应商" prop="supplierNameSnapshot" width="150" fixed />
      <el-table-column label="订单号" prop="orderNo" width="180" fixed show-overflow-tooltip />
      <el-table-column label="型号" prop="productModel" width="150" />
      <el-table-column label="单价" prop="unitPrice" width="90" align="right" />
      <el-table-column label="数量" prop="quantity" width="80" align="right" />
      <el-table-column label="金额" prop="totalAmount" width="100" align="right" />
      <el-table-column label="姓名" prop="customerName" width="100" />
      <el-table-column label="电话" prop="customerPhone" width="140" />
      <el-table-column label="地址" prop="customerAddress" min-width="220" show-overflow-tooltip />
      <el-table-column label="备注" prop="orderRemark" min-width="180" show-overflow-tooltip />
      <el-table-column label="物流单号" prop="logisticsNo" width="170" />
      <el-table-column label="是否售后" prop="afterSaleFlag" width="90" />
      <el-table-column label="开票情况" prop="invoiceStatus" width="110" />
      <el-table-column label="对账情况" prop="reconciliationStatus" width="110" />
      <el-table-column label="上传人" prop="uploadUserName" width="110" />
      <el-table-column label="上传时间" prop="uploadTime" width="170" />
      <el-table-column label="来源文件" prop="sourceFileName" width="180" show-overflow-tooltip />
      <el-table-column label="数据状态" prop="dataStatus" width="110">
        <template #default="scope"><el-tag :type="dataStatusType(scope.row.dataStatus)">{{ dataStatusLabel(scope.row.dataStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="150">
        <template #default="scope"><el-button link type="primary" @click="openForm(scope.row)">编辑</el-button><el-button link type="danger" @click="remove(scope.row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog v-model="open" :title="form.manualOrderId ? '编辑手工订单' : '新增手工订单'" width="760px">
      <el-form :model="form" label-width="98px">
        <el-row :gutter="10">
          <el-col :span="12"><el-form-item label="供应商"><el-select v-model="form.supplierId" filterable style="width:100%"><el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="日期"><el-date-picker v-model="form.orderDate" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="订单号"><el-input v-model="form.orderNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="物流单号"><el-input v-model="form.logisticsNo" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="型号"><el-input v-model="form.productModel" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="单价"><el-input v-model="form.unitPrice" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="数量"><el-input v-model="form.quantity" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="姓名"><el-input v-model="form.customerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="电话"><el-input v-model="form.customerPhone" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="地址"><el-input v-model="form.customerAddress" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.orderRemark" type="textarea" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer><el-button @click="open=false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="uploadOpen" title="上传手工订单表" width="900px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="供应商">
          <el-select v-model="uploadForm.supplierId" filterable placeholder="请选择供应商" style="width: 320px" @change="autoPreviewManual">
            <el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" />
          </el-select>
        </el-form-item>
        <el-form-item label="Excel文件">
          <el-upload :auto-upload="false" :limit="1" :on-change="handleUploadChange" :on-remove="clearUpload" accept=".xls,.xlsx">
            <el-button icon="Upload">选择文件</el-button>
          </el-upload>
          <el-button class="ml8" icon="Download" @click="downloadManualTemplate">下载模板</el-button>
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

<script setup name="PurchaseManual">
import { saveAs } from 'file-saver'
import { listManual, addManual, updateManual, delManual, exportManual, downloadManualTemplate as getManualTemplate, supplierOptions, previewImport, confirmImport } from '@/api/erp/purchaseMatch'
const { proxy } = getCurrentInstance()
const suppliers = ref([]), rows = ref([])
const loading = ref(false), open = ref(false), uploadOpen = ref(false), total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10 })
const form = ref({})
const uploadForm = reactive({ supplierId: undefined, file: undefined, preview: undefined, busy: false, phase: '', progress: 0, progressStatus: '', message: '' })
const dataStatusMap = { NORMAL: '正常', CONFLICT: '冲突', IGNORED: '已忽略' }
const dataStatusTypeMap = { NORMAL: 'success', CONFLICT: 'warning', IGNORED: 'info' }
function dataStatusLabel(status){ return dataStatusMap[status] || status || '-' }
function dataStatusType(status){ return dataStatusTypeMap[status] || 'info' }
function getList(){ loading.value=true; listManual(queryParams.value).then(r=>{rows.value=r.rows; total.value=r.total; loading.value=false}) }
function handleQuery(){ queryParams.value.pageNum=1; getList() }
function resetQuery(){ proxy.resetForm('queryRef'); handleQuery() }
function openForm(row){ form.value = row ? { ...row } : { dataStatus: 'NORMAL' }; open.value=true }
function submit(){ const api=form.value.manualOrderId ? updateManual : addManual; api(form.value).then(()=>{ proxy.$modal.msgSuccess('保存成功'); open.value=false; getList() }) }
function remove(row){ proxy.$modal.confirm('确认删除该订单？').then(()=>delManual(row.manualOrderId)).then(()=>{ proxy.$modal.msgSuccess('删除成功'); getList() }) }
function handleExport(){ exportManual(queryParams.value).then(blob => saveAs(blob, '手工订单总表.xlsx')) }
function openUpload(){ clearUpload(); uploadForm.supplierId = undefined; uploadOpen.value = true }
function clearUpload(){ uploadForm.file=undefined; uploadForm.preview=undefined; uploadForm.progress=0; uploadForm.progressStatus=''; uploadForm.message=''; uploadForm.busy=false; uploadForm.phase='' }
function downloadManualTemplate(){ getManualTemplate().then(blob => saveAs(blob, '手工订单总表模板.xlsx')) }
function handleUploadChange(file){
  uploadForm.file = file.raw
  uploadForm.preview = undefined
  if (!uploadForm.supplierId) return proxy.$modal.msgWarning('请先选择供应商，再选择文件')
  previewExcel()
}
function autoPreviewManual(){
  if (uploadForm.file && !uploadForm.preview && !uploadForm.busy) previewExcel()
}
function uploadData(){
  const data = new FormData()
  data.append('importType', 'MANUAL_ORDER')
  data.append('supplierId', uploadForm.supplierId)
  data.append('file', uploadForm.file)
  return data
}
function startUpload(phase, message){ uploadForm.busy=true; uploadForm.phase=phase; uploadForm.progress=0; uploadForm.progressStatus=''; uploadForm.message=message }
function progressHandler(event){ if (!event.total) return; const percent = Math.round((event.loaded * 100) / event.total); uploadForm.progress = Math.min(percent, 95); if (percent >= 100) uploadForm.message = uploadForm.phase === 'confirm' ? '文件已上传，正在写入和匹配...' : '文件已上传，正在解析预览...' }
function finishUpload(message){ uploadForm.progress=100; uploadForm.progressStatus='success'; uploadForm.message=message; window.setTimeout(()=>{ uploadForm.busy=false; uploadForm.phase='' }, 400) }
function failUpload(message){ uploadForm.progressStatus='exception'; uploadForm.message=message; uploadForm.busy=false; uploadForm.phase='' }
function previewExcel(){
  if (!uploadForm.supplierId) return proxy.$modal.msgWarning('请先选择供应商')
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
.mb8 { margin-bottom: 8px; }
.ml8 { margin-left: 8px; }
.progress-block { width: 100%; max-width: 520px; }
.progress-text { margin-top: 4px; color: #606266; font-size: 13px; }
</style>
