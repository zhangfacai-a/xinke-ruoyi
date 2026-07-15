<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="类型"><el-select v-model="queryParams.importType" clearable style="width:180px"><el-option label="手工订单" value="MANUAL_ORDER" /><el-option label="采购汇总" value="PURCHASE_SUMMARY" /></el-select></el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.batchStatus" clearable style="width:150px">
          <el-option v-for="s in importStatuses" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
      </el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border size="small">
      <el-table-column label="批次号" prop="batchNo" width="170" />
      <el-table-column label="类型" prop="importType" width="150">
        <template #default="scope">{{ importTypeLabel(scope.row.importType) }}</template>
      </el-table-column>
      <el-table-column label="供应商" prop="supplierName" width="150" />
      <el-table-column label="文件名" prop="originalFileName" min-width="220" show-overflow-tooltip />
      <el-table-column label="文件Hash" prop="fileHash" width="220" show-overflow-tooltip />
      <el-table-column label="上传人" prop="uploadUserName" width="110" />
      <el-table-column label="上传时间" prop="uploadTime" width="170" />
      <el-table-column label="总行" prop="totalRows" width="70" />
      <el-table-column label="新增" prop="successRows" width="70" />
      <el-table-column label="重复" prop="duplicateRows" width="70" />
      <el-table-column label="补充" prop="updatedRows" width="70" />
      <el-table-column label="冲突" prop="conflictRows" width="70" />
      <el-table-column label="失败" prop="failedRows" width="70" />
      <el-table-column label="状态" prop="batchStatus" width="110">
        <template #default="scope"><el-tag :type="importStatusType(scope.row.batchStatus)">{{ importStatusLabel(scope.row.batchStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="完成时间" prop="finishTime" width="170" />
      <el-table-column label="操作" fixed="right" width="90">
        <template #default="scope"><el-button link type="primary" @click="showDetail(scope.row)">明细</el-button></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
    <el-dialog v-model="open" title="逐行结果" width="900px"><el-table :data="details" size="small" max-height="520"><el-table-column label="行号" prop="sourceRowNumber" width="80" /><el-table-column label="业务键" prop="businessKey" min-width="220" show-overflow-tooltip /><el-table-column label="结果" prop="processResult" width="140"><template #default="scope">{{ rowStatusLabel(scope.row.processResult) }}</template></el-table-column><el-table-column label="错误" prop="errorMessage" min-width="260" show-overflow-tooltip /></el-table></el-dialog>
  </div>
</template>

<script setup name="PurchaseImport">
import { listBatch, listImportDetail } from '@/api/erp/purchaseMatch'
const { proxy } = getCurrentInstance()
const rows = ref([]), details = ref([]), loading = ref(false), open = ref(false), total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10 })
const importTypeMap = { MANUAL_ORDER: '手工订单', PURCHASE_SUMMARY: '采购汇总' }
const importStatusMap = { PROCESSING: '处理中', SUCCESS: '成功', FAILED: '失败', PARTIAL_SUCCESS: '部分完成' }
const importStatusTypes = { SUCCESS: 'success', FAILED: 'danger', PARTIAL_SUCCESS: 'warning', PROCESSING: 'info' }
const rowStatusMap = { INSERTED: '新增', DUPLICATE: '重复无变化', FILLED_EMPTY_FIELDS: '补充空字段', CONFLICT: '数据冲突', INVALID: '无效行', FAILED: '失败' }
const importStatuses = Object.entries(importStatusMap).map(([value, label]) => ({ value, label }))
function importTypeLabel(value){ return importTypeMap[value] || value || '-' }
function importStatusLabel(value){ return importStatusMap[value] || value || '-' }
function importStatusType(value){ return importStatusTypes[value] || 'info' }
function rowStatusLabel(value){ return rowStatusMap[value] || value || '-' }
function getList(){ loading.value=true; listBatch(queryParams.value).then(r=>{rows.value=r.rows; total.value=r.total; loading.value=false}) }
function handleQuery(){ queryParams.value.pageNum=1; getList() }
function resetQuery(){ proxy.resetForm('queryRef'); handleQuery() }
function showDetail(row){ listImportDetail({ batchId: row.batchId, pageNum: 1, pageSize: 300 }).then(r=>{details.value=r.rows; open.value=true}) }
getList()
</script>
