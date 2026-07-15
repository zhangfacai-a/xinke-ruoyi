<template>
  <div class="app-container">
    <el-row :gutter="12">
      <el-col :span="14">
        <el-card shadow="never">
          <template #header><span>供应商设置</span></template>
          <el-row class="mb8"><el-button type="primary" icon="Plus" @click="openSupplier()">新增供应商</el-button></el-row>
          <el-table v-loading="loading" :data="suppliers" border size="small">
            <el-table-column label="名称" prop="supplierName" min-width="160" />
            <el-table-column label="编码" prop="supplierCode" width="120" />
            <el-table-column label="状态" prop="status" width="90">
              <template #default="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusLabel(scope.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="排序" prop="sortOrder" width="80" />
            <el-table-column label="备注" prop="remark" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="150">
              <template #default="scope"><el-button link type="primary" @click="openSupplier(scope.row)">编辑</el-button><el-button link type="danger" @click="removeSupplier(scope.row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="10">
        <el-card shadow="never">
          <template #header><span>供应商别名</span></template>
          <el-row class="mb8"><el-button type="primary" icon="Plus" @click="openAlias()">新增别名</el-button></el-row>
          <el-table :data="aliases" border size="small">
            <el-table-column label="供应商" prop="supplierName" min-width="130" />
            <el-table-column label="别名" prop="aliasName" min-width="150" />
            <el-table-column label="状态" prop="status" width="80">
              <template #default="scope"><el-tag :type="scope.row.status === '0' ? 'success' : 'info'">{{ statusLabel(scope.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="scope"><el-button link type="primary" @click="openAlias(scope.row)">编辑</el-button><el-button link type="danger" @click="removeAlias(scope.row)">删除</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="supplierOpen" title="供应商" width="460px">
      <el-form :model="supplierForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="supplierForm.supplierName" /></el-form-item>
        <el-form-item label="编码"><el-input v-model="supplierForm.supplierCode" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="supplierForm.status"><el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="supplierForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="supplierForm.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="supplierOpen=false">取消</el-button><el-button type="primary" @click="submitSupplier">确定</el-button></template>
    </el-dialog>

    <el-dialog v-model="aliasOpen" title="供应商别名" width="460px">
      <el-form :model="aliasForm" label-width="90px">
        <el-form-item label="供应商"><el-select v-model="aliasForm.supplierId" filterable style="width:100%"><el-option v-for="s in suppliers" :key="s.supplierId" :label="s.supplierName" :value="s.supplierId" /></el-select></el-form-item>
        <el-form-item label="别名"><el-input v-model="aliasForm.aliasName" /></el-form-item>
        <el-form-item label="状态"><el-radio-group v-model="aliasForm.status"><el-radio label="0">启用</el-radio><el-radio label="1">停用</el-radio></el-radio-group></el-form-item>
      </el-form>
      <template #footer><el-button @click="aliasOpen=false">取消</el-button><el-button type="primary" @click="submitAlias">确定</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup name="PurchaseSupplier">
import { listSupplier, addSupplier, updateSupplier, delSupplier, listAlias, addAlias, updateAlias, delAlias } from '@/api/erp/purchaseMatch'
const { proxy } = getCurrentInstance()
const suppliers = ref([]), aliases = ref([]), loading = ref(false)
const supplierOpen = ref(false), aliasOpen = ref(false), supplierForm = ref({}), aliasForm = ref({})
function statusLabel(status){ return status === '0' || status === 0 ? '启用' : '停用' }
function getList(){ loading.value=true; listSupplier({ pageNum:1, pageSize:999 }).then(r=>{suppliers.value=r.rows; loading.value=false}); listAlias({ pageNum:1, pageSize:999 }).then(r=>aliases.value=r.rows) }
function openSupplier(row){ supplierForm.value = row ? { ...row } : { status:'0', sortOrder:0 }; supplierOpen.value=true }
function submitSupplier(){ const api=supplierForm.value.supplierId ? updateSupplier : addSupplier; api(supplierForm.value).then(()=>{ proxy.$modal.msgSuccess('保存成功'); supplierOpen.value=false; getList() }) }
function removeSupplier(row){ proxy.$modal.confirm('确认删除供应商？').then(()=>delSupplier(row.supplierId)).then(()=>{ proxy.$modal.msgSuccess('删除成功'); getList() }) }
function openAlias(row){ aliasForm.value = row ? { ...row } : { status:'0' }; aliasOpen.value=true }
function submitAlias(){ const api=aliasForm.value.aliasId ? updateAlias : addAlias; api(aliasForm.value).then(()=>{ proxy.$modal.msgSuccess('保存成功'); aliasOpen.value=false; getList() }) }
function removeAlias(row){ proxy.$modal.confirm('确认删除别名？').then(()=>delAlias(row.aliasId)).then(()=>{ proxy.$modal.msgSuccess('删除成功'); getList() }) }
getList()
</script>
