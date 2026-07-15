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
      <el-table-column label="收入" prop="incomeAmount" align="right" />
      <el-table-column label="退款" prop="refundAmount" align="right" />
      <el-table-column label="佣金" prop="commissionFee" align="right" />
      <el-table-column label="广告费" prop="adFee" align="right" />
      <el-table-column label="应收" prop="receivableAmount" align="right" />
      <el-table-column label="已收" prop="receivedAmount" align="right" />
      <el-table-column label="差异" prop="diffAmount" align="right" />
      <el-table-column label="结算状态" prop="settlementStatus" align="center" />
      <el-table-column label="对账状态" prop="reconcileStatus" align="center" />
      <el-table-column label="操作" align="center" width="210" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="handleApprove(scope.row)" v-hasPermi="['finance:settlement:audit']">确认</el-button>
          <el-button link type="primary" @click="handleVoucher(scope.row)" v-hasPermi="['finance:voucher:add']">生成凭证</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />

    <el-dialog title="新增平台结算单" v-model="open" width="720px" append-to-body>
      <el-form ref="settlementRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="结算单号">
              <el-input v-model="form.settlementNo" placeholder="不填则自动生成" />
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
              <el-input-number v-model="form.incomeAmount" :precision="2" :step="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款金额">
              <el-input-number v-model="form.refundAmount" :precision="2" :step="100" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="平台佣金">
              <el-input-number v-model="form.commissionFee" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付手续费">
              <el-input-number v-model="form.paymentFee" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="广告费">
              <el-input-number v-model="form.adFee" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务/运费/其他">
              <el-input-number v-model="form.otherFee" :precision="2" :step="10" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
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
import { listSettlement, addSettlement, approveSettlement, createSettlementVoucher } from '@/api/finance/settlement'

const { proxy } = getCurrentInstance()
const dataList = ref([])
const loading = ref(true)
const showSearch = ref(true)
const total = ref(0)
const open = ref(false)

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

function currentPeriod() {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
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
    billEndDate: new Date().toISOString().slice(0, 10),
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
function handleAdd() { reset(); open.value = true }
function submitForm() {
  proxy.$refs.settlementRef.validate(valid => {
    if (!valid) return
    addSettlement(form.value).then(() => {
      proxy.$modal.msgSuccess('新增结算单成功')
      open.value = false
      getList()
    })
  })
}
function handleApprove(row) {
  proxy.$modal.confirm(`确认结算单 ${row.settlementNo} 并生成应收单？`).then(() => approveSettlement(row.settlementNo)).then(() => {
    proxy.$modal.msgSuccess('确认成功')
    getList()
  }).catch(() => {})
}
function handleVoucher(row) {
  proxy.$modal.confirm(`确认根据结算单 ${row.settlementNo} 生成会计凭证？`).then(() => createSettlementVoucher(row.settlementNo)).then(() => {
    proxy.$modal.msgSuccess('凭证生成成功')
    getList()
  }).catch(() => {})
}
getList()
</script>
