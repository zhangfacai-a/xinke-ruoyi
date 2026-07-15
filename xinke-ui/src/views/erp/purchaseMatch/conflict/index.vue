<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true">
      <el-form-item label="目标表"><el-select v-model="queryParams.targetTable" clearable style="width:180px"><el-option label="手工订单" value="purchase_manual_order" /><el-option label="采购汇总" value="purchase_summary" /></el-select></el-form-item>
      <el-form-item label="状态"><el-select v-model="queryParams.conflictStatus" clearable style="width:150px"><el-option label="待处理" value="PENDING" /><el-option label="已处理" value="RESOLVED" /><el-option label="忽略" value="IGNORED" /></el-select></el-form-item>
      <el-form-item><el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button><el-button icon="Refresh" @click="resetQuery">重置</el-button></el-form-item>
    </el-form>
    <el-table v-loading="loading" :data="rows" border size="small">
      <el-table-column label="冲突ID" prop="conflictId" width="90" />
      <el-table-column label="目标表" prop="targetTable" width="170">
        <template #default="scope">{{ targetTableLabel(scope.row.targetTable) }}</template>
      </el-table-column>
      <el-table-column label="记录ID" prop="targetRecordId" width="100" />
      <el-table-column label="字段" prop="conflictFields" min-width="180" show-overflow-tooltip />
      <el-table-column label="原值" prop="oldDataJson" min-width="260" show-overflow-tooltip />
      <el-table-column label="新值" prop="newDataJson" min-width="260" show-overflow-tooltip />
      <el-table-column label="来源文件" prop="sourceFileName" width="180" show-overflow-tooltip />
      <el-table-column label="上传人" prop="uploadUserName" width="110" />
      <el-table-column label="上传时间" prop="uploadTime" width="170" />
      <el-table-column label="状态" prop="conflictStatus" width="100">
        <template #default="scope"><el-tag :type="conflictStatusType(scope.row.conflictStatus)">{{ conflictStatusLabel(scope.row.conflictStatus) }}</el-tag></template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="170">
        <template #default="scope">
          <el-button link type="primary" @click="resolve(scope.row, 'USE_NEW')">使用新值</el-button>
          <el-button link @click="resolve(scope.row, 'KEEP_OLD')">保留原值</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script setup name="PurchaseConflict">
import { listConflict, resolveConflict } from '@/api/erp/purchaseMatch'
const { proxy } = getCurrentInstance()
const rows = ref([]), loading = ref(false), total = ref(0)
const queryParams = ref({ pageNum: 1, pageSize: 10, conflictStatus: 'PENDING' })
const targetTableMap = { purchase_manual_order: '手工订单', purchase_summary: '采购汇总' }
const conflictStatusMap = { PENDING: '待处理', RESOLVED: '已处理', IGNORED: '已忽略' }
const conflictStatusTypes = { PENDING: 'warning', RESOLVED: 'success', IGNORED: 'info' }
function targetTableLabel(value){ return targetTableMap[value] || value || '-' }
function conflictStatusLabel(value){ return conflictStatusMap[value] || value || '-' }
function conflictStatusType(value){ return conflictStatusTypes[value] || 'info' }
function getList(){ loading.value=true; listConflict(queryParams.value).then(r=>{rows.value=r.rows; total.value=r.total; loading.value=false}) }
function handleQuery(){ queryParams.value.pageNum=1; getList() }
function resetQuery(){ proxy.resetForm('queryRef'); handleQuery() }
function resolve(row, action){ resolveConflict({ conflictId: row.conflictId, action }).then(()=>{ proxy.$modal.msgSuccess('处理完成'); getList() }) }
getList()
</script>
