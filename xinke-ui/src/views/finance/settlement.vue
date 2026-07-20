<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
      <el-form-item label="结算单号" prop="settlementNo">
        <el-input v-model="queryParams.settlementNo" placeholder="请输入结算单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="期间" prop="periodCode">
        <el-input v-model="queryParams.periodCode" placeholder="如 2026-06" clearable style="width: 140px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item label="店铺" prop="shopName">
        <el-input v-model="queryParams.shopName" placeholder="请输入店铺" clearable style="width: 180px" @keyup.enter="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
        <el-button icon="Refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['finance:settlement:add']">新增结算单</el-button>
      </el-col>
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="dataList">
      <el-table-column label="结算单号" prop="settlementNo" align="center" min-width="170" />
      <el-table-column label="平台" prop="platformName" align="center" />
      <el-table-column label="店铺" prop="shopName" align="center" min-width="140" />
      <el-table-column label="期间" prop="periodCode" align="center" />
      <el-table-column label="收入" align="right"><template #default="{ row }">{{ money(row.incomeAmount) }}</template></el-table-column>
      <el-table-column label="退款" align="right"><template #default="{ row }">{{ money(row.refundAmount) }}</template></el-table-column>
      <el-table-column label="平台费用" align="right"><template #default="{ row }">{{ money(totalFee(row)) }}</template></el-table-column>
      <el-table-column label="应收" align="right"><template #default="{ row }"><strong>{{ money(row.receivableAmount) }}</strong></template></el-table-column>
      <el-table-column label="已收" align="right"><template #default="{ row }">{{ money(row.receivedAmount) }}</template></el-table-column>
      <el-table-column label="差异" align="right"><template #default="{ row }"><span :class="{ 'diff-text': Number(row.diffAmount || 0) !== 0 }">{{ money(row.diffAmount) }}</span></template></el-table-column>
      <el-table-column label="结算状态" align="center"><template #default="{ row }"><el-tag round :type="settlementStatusType(row.settlementStatus)">{{ settlementStatusLabel(row.settlementStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="对账状态" align="center"><template #default="{ row }"><el-tag round effect="plain" :type="reconcileStatusType(row.reconcileStatus)">{{ reconcileStatusLabel(row.reconcileStatus) }}</el-tag></template></el-table-column>
      <el-table-column label="后续单据" min-width="180"><template #default="{ row }"><div class="linked-bills"><span v-if="row.receivableNo">应收 {{ row.receivableNo }}</span><span v-if="row.voucherNo">凭证 {{ row.voucherNo }}</span><em v-if="!row.receivableNo && !row.voucherNo">确认后自动生成</em></div></template></el-table-column>
      <el-table-column label="操作" align="center" width="300" fixed="right">
        <template #default="scope">
          <el-button v-if="scope.row.settlementStatus === 'draft'" link type="primary" @click="handleEdit(scope.row)" v-hasPermi="['finance:settlement:edit']">修改</el-button>
          <el-button v-if="scope.row.settlementStatus === 'draft'" link type="primary" :loading="actionNo === scope.row.settlementNo" @click="handleApprove(scope.row)" v-hasPermi="['finance:settlement:audit']">确认并生成应收</el-button>
          <el-button v-if="scope.row.settlementStatus === 'draft'" link type="danger" :loading="actionNo === scope.row.settlementNo" @click="handleVoid(scope.row)" v-hasPermi="['finance:settlement:edit']">作废</el-button>
          <el-button v-if="['confirmed', 'approved'].includes(scope.row.settlementStatus) && !scope.row.voucherNo" link type="primary" :loading="actionNo === scope.row.settlementNo" @click="handleVoucher(scope.row)" v-hasPermi="['finance:voucher:add']">生成凭证</el-button>
          <span v-if="scope.row.voucherNo" class="done-text">已生成凭证</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="editMode ? '修改平台结算单' : '新增平台结算单'" v-model="open" width="720px" append-to-body>
      <el-form ref="settlementRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="结算单号">
              <el-input v-model="form.settlementNo" placeholder="不填则自动生成" :disabled="editMode" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期间" prop="periodCode">
              <el-input v-model="form.periodCode" placeholder="如 2026-06" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台编码" prop="platformCode">
              <el-input v-model="form.platformCode" placeholder="如 douyin / tmall / jd" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台名称">
              <el-input v-model="form.platformName" placeholder="请输入平台名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="店铺ID">
              <el-input v-model="form.shopId" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="店铺名称">
              <el-input v-model="form.shopName" placeholder="可选" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账单开始" prop="billStartDate">
              <el-date-picker v-model="form.billStartDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账单结束" prop="billEndDate">
              <el-date-picker v-model="form.billEndDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收入金额">
              <el-input-number v-model="form.incomeAmount" :precision="2" :step="100" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款金额">
              <el-input-number v-model="form.refundAmount" :precision="2" :step="100" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台佣金">
              <el-input-number v-model="form.commissionFee" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付手续费">
              <el-input-number v-model="form.paymentFee" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="广告费">
              <el-input-number v-model="form.adFee" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台服务费">
              <el-input-number v-model="form.serviceFee" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台运费">
              <el-input-number v-model="form.freightFee" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="其他扣费">
              <el-input-number v-model="form.otherFee" :precision="2" :step="10" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="24"><el-alert :closable="false" type="info" show-icon><template #title>预计平台应收：<strong>{{ money(expectedReceivable) }}</strong>，确认后以该金额生成应收单。</template></el-alert></el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确定</el-button>
          <el-button @click="open = false">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="FinanceSettlement">
import { listSettlement, addSettlement, updateSettlement, voidSettlement, approveSettlement, createSettlementVoucher } from '@/api/finance/settlement'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)
const editMode = ref(false)
const actionNo = ref('')

const data = reactive({
  form: {},
  queryParams: { pageNum: 1, pageSize: 10, settlementNo: undefined, periodCode: undefined, shopName: undefined },
  rules: {
    platformCode: [{ required: true, message: '平台编码不能为空', trigger: 'blur' }],
    periodCode: [{ required: true, message: '期间不能为空', trigger: 'blur' }],
    billStartDate: [{ required: true, message: '账单开始日期不能为空', trigger: 'change' }],
    billEndDate: [{ required: true, message: '账单结束日期不能为空', trigger: 'change' }]
  }
})
const { form, queryParams, rules } = toRefs(data)
const expectedReceivable = computed(() => Number(form.value.incomeAmount || 0) - Number(form.value.refundAmount || 0) - totalFee(form.value))

function currentPeriod() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
}
function currentDate() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10)
}
function reset() {
  form.value = {
    settlementNo: undefined,
    platformCode: 'douyin',
    platformName: '抖音电商',
    shopId: undefined,
    shopName: undefined,
    periodCode: currentPeriod(),
    billStartDate: currentPeriod() + '-01',
    billEndDate: currentDate(),
    incomeAmount: 0,
    refundAmount: 0,
    commissionFee: 0,
    paymentFee: 0,
    adFee: 0,
    serviceFee: 0,
    freightFee: 0,
    otherFee: 0,
    remark: undefined
  }
  proxy.resetForm('settlementRef')
}
function getList() {
  loading.value = true
  listSettlement(queryParams.value).then(res => {
    dataList.value = res.rows
    total.value = res.total
  }).finally(() => { loading.value = false })
}
function handleQuery() { queryParams.value.pageNum = 1; getList() }
function resetQuery() { proxy.resetForm('queryRef'); handleQuery() }
function handleAdd() { reset(); editMode.value = false; open.value = true }
function handleEdit(row) {
  reset()
  editMode.value = true
  form.value = { ...row }
  open.value = true
}
function submitForm() {
  proxy.$refs.settlementRef.validate(valid => {
    if (!valid) return
    if (expectedReceivable.value < 0) return proxy.$modal.msgError('应收金额不能小于0，请检查收入、退款和平台扣费')
    const request = editMode.value ? updateSettlement(form.value.settlementNo, form.value) : addSettlement(form.value)
    request.then(() => {
      proxy.$modal.msgSuccess(editMode.value ? '结算单修改成功' : '新增结算单成功')
      open.value = false
      getList()
    })
  })
}
function handleVoid(row) {
  proxy.$modal.confirm(`确认作废草稿结算单 ${row.settlementNo}？作废后不再参与应收和对账。`).then(async () => {
    actionNo.value = row.settlementNo
    try { await voidSettlement(row.settlementNo); proxy.$modal.msgSuccess('结算单已作废'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function handleApprove(row) {
  proxy.$modal.confirm(`确认 ${row.settlementNo} 的收入、退款和平台扣费无误？确认后将生成 ${money(row.receivableAmount)} 应收账款。`).then(async () => {
    actionNo.value = row.settlementNo
    try { await approveSettlement(row.settlementNo); proxy.$modal.msgSuccess('结算已确认，应收单已生成'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function handleVoucher(row) {
  proxy.$modal.confirm(`根据结算单 ${row.settlementNo} 生成收入、平台费用和应收账款凭证？`).then(async () => {
    actionNo.value = row.settlementNo
    try { await createSettlementVoucher(row.settlementNo); proxy.$modal.msgSuccess('草稿凭证已生成，请到凭证管理检查并过账'); await getList() } finally { actionNo.value = '' }
  }).catch(error => { if (error !== 'cancel' && error !== 'close') throw error })
}
function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function totalFee(row) { return ['commissionFee', 'paymentFee', 'adFee', 'serviceFee', 'freightFee', 'otherFee'].reduce((sum, key) => sum + Number(row?.[key] || 0), 0) }
function settlementStatusLabel(value) { return ({ draft: '待确认', confirmed: '已确认', approved: '已确认', voided: '已作废' })[value] || value || '-' }
function settlementStatusType(value) { return ({ draft: 'warning', confirmed: 'success', approved: 'success', voided: 'info' })[value] || 'info' }
function reconcileStatusLabel(value) { return ({ unmatched: '未对账', partial: '部分到账', matched: '已匹配', difference: '有差异' })[value] || value || '-' }
function reconcileStatusType(value) { return ({ unmatched: 'warning', partial: 'primary', matched: 'success', difference: 'danger' })[value] || 'info' }
getList()
</script>

<style scoped>
.linked-bills span, .linked-bills em { display:block; line-height:1.55; font-size:12px; }.linked-bills span { color:#6254df; }.linked-bills em { color:#9aa1b4; font-style:normal; }.done-text { color:#00a879; font-size:13px; }.diff-text { color:#e24c62; font-weight:700; }
</style>
