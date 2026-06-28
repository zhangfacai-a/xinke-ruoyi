<template>
  <div class="app-container">
    <el-tabs v-model="activeName">
      <el-tab-pane label="会计期间" name="period">
        <el-table v-loading="loading" :data="periodList">
          <el-table-column label="期间" prop="periodCode" align="center" />
          <el-table-column label="开始日期" prop="startDate" align="center" />
          <el-table-column label="结束日期" prop="endDate" align="center" />
          <el-table-column label="结账状态" prop="closeStatus" align="center" />
          <el-table-column label="结账人" prop="closeBy" align="center" />
          <el-table-column label="结账时间" prop="closeTime" align="center" width="170" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="会计科目" name="subject">
        <el-table v-loading="loading" :data="subjectList">
          <el-table-column label="科目编码" prop="subjectCode" align="center" />
          <el-table-column label="科目名称" prop="subjectName" align="center" min-width="160" />
          <el-table-column label="科目类型" prop="subjectType" align="center" />
          <el-table-column label="上级科目" prop="parentCode" align="center" />
          <el-table-column label="余额方向" prop="balanceDirection" align="center" />
          <el-table-column label="状态" prop="status" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="费用类型" name="feeType">
        <el-table v-loading="loading" :data="feeTypeList">
          <el-table-column label="费用编码" prop="feeCode" align="center" min-width="180" />
          <el-table-column label="费用名称" prop="feeName" align="center" />
          <el-table-column label="费用分类" prop="feeCategory" align="center" />
          <el-table-column label="默认科目" prop="defaultSubjectCode" align="center" />
          <el-table-column label="分摊规则" prop="allocateRule" align="center" />
          <el-table-column label="状态" prop="status" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="平台账户" name="platformAccount">
        <el-table v-loading="loading" :data="platformAccountList">
          <el-table-column label="平台" prop="platformName" align="center" />
          <el-table-column label="店铺" prop="shopName" align="center" min-width="150" />
          <el-table-column label="账户号" prop="accountNo" align="center" min-width="180" />
          <el-table-column label="币种" prop="currency" align="center" />
          <el-table-column label="状态" prop="status" align="center" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="银行账户" name="bankAccount">
        <el-table v-loading="loading" :data="bankAccountList">
          <el-table-column label="账户名称" prop="accountName" align="center" />
          <el-table-column label="开户行" prop="bankName" align="center" />
          <el-table-column label="银行账号" prop="bankNo" align="center" min-width="180" />
          <el-table-column label="币种" prop="currency" align="center" />
          <el-table-column label="期初余额" prop="openingBalance" align="right" />
          <el-table-column label="状态" prop="status" align="center" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup name="FinanceConfig">
import { listPeriod, listSubject, listFeeType, listPlatformAccount, listBankAccount } from '@/api/finance/config'

const loading = ref(false)
const activeName = ref('period')
const periodList = ref([])
const subjectList = ref([])
const feeTypeList = ref([])
const platformAccountList = ref([])
const bankAccountList = ref([])

function loadData() {
  loading.value = true
  Promise.all([
    listPeriod(),
    listSubject(),
    listFeeType(),
    listPlatformAccount(),
    listBankAccount()
  ]).then(([periodRes, subjectRes, feeTypeRes, platformRes, bankRes]) => {
    periodList.value = periodRes.rows || []
    subjectList.value = subjectRes.rows || []
    feeTypeList.value = feeTypeRes.rows || []
    platformAccountList.value = platformRes.rows || []
    bankAccountList.value = bankRes.rows || []
  }).finally(() => {
    loading.value = false
  })
}

loadData()
</script>
