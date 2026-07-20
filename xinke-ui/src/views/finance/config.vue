<template>
  <div class="app-container">
    <el-alert class="config-guide" type="info" :closable="false" show-icon title="财务基础资料当前采用受控维护：这里用于核对期间、科目、费用类型和资金账户，避免业务人员随意修改已用于凭证的基础数据。" />
    <div class="config-toolbar"><el-button icon="Refresh" :loading="loading" @click="loadData">刷新基础资料</el-button></div>
    <el-tabs v-model="activeName">
      <el-tab-pane label="会计期间" name="period">
        <el-table v-loading="loading" :data="periodList">
          <el-table-column label="期间" prop="periodCode" align="center" />
          <el-table-column label="开始日期" prop="startDate" align="center" />
          <el-table-column label="结束日期" prop="endDate" align="center" />
          <el-table-column label="结账状态" align="center"><template #default="{ row }"><el-tag round :type="row.closeStatus === 'closed' ? 'success' : 'warning'">{{ row.closeStatus === 'closed' ? '已关账' : '开放' }}</el-tag></template></el-table-column>
          <el-table-column label="结账人" prop="closeBy" align="center" />
          <el-table-column label="结账时间" prop="closeTime" align="center" width="170" />
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="会计科目" name="subject">
        <el-table v-loading="loading" :data="subjectList">
          <el-table-column label="科目编码" prop="subjectCode" align="center" />
          <el-table-column label="科目名称" prop="subjectName" align="center" min-width="160" />
          <el-table-column label="科目类型" align="center"><template #default="{ row }">{{ subjectTypeLabel(row.subjectType) }}</template></el-table-column>
          <el-table-column label="上级科目" prop="parentCode" align="center" />
          <el-table-column label="余额方向" align="center"><template #default="{ row }">{{ row.balanceDirection === 'debit' ? '借方' : '贷方' }}</template></el-table-column>
          <el-table-column label="状态" align="center"><template #default="{ row }"><el-tag round :type="row.status === '0' ? 'success' : 'info'">{{ row.status === '0' ? '启用' : '停用' }}</el-tag></template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="费用类型" name="feeType">
        <el-table v-loading="loading" :data="feeTypeList">
          <el-table-column label="费用编码" prop="feeCode" align="center" min-width="180" />
          <el-table-column label="费用名称" prop="feeName" align="center" />
          <el-table-column label="费用分类" prop="feeCategory" align="center" />
          <el-table-column label="默认科目" prop="defaultSubjectCode" align="center" />
          <el-table-column label="分摊规则" prop="allocateRule" align="center" />
          <el-table-column label="状态" align="center"><template #default="{ row }">{{ row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="平台账户" name="platformAccount">
        <el-table v-loading="loading" :data="platformAccountList">
          <el-table-column label="平台" prop="platformName" align="center" />
          <el-table-column label="店铺" prop="shopName" align="center" min-width="150" />
          <el-table-column label="账户号" prop="accountNo" align="center" min-width="180" />
          <el-table-column label="币种" prop="currency" align="center" />
          <el-table-column label="状态" align="center"><template #default="{ row }">{{ row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="银行账户" name="bankAccount">
        <el-table v-loading="loading" :data="bankAccountList">
          <el-table-column label="账户名称" prop="accountName" align="center" />
          <el-table-column label="开户行" prop="bankName" align="center" />
          <el-table-column label="银行账号" prop="bankNo" align="center" min-width="180" />
          <el-table-column label="币种" prop="currency" align="center" />
          <el-table-column label="期初余额" align="right"><template #default="{ row }">{{ money(row.openingBalance) }}</template></el-table-column>
          <el-table-column label="状态" align="center"><template #default="{ row }">{{ row.status === '0' ? '启用' : '停用' }}</template></el-table-column>
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

function money(value) { return `¥${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}` }
function subjectTypeLabel(value) { return ({ asset: '资产', liability: '负债', equity: '权益', revenue: '收入', cost: '成本', expense: '费用' })[value] || value || '-' }

loadData()
</script>

<style scoped>
.config-guide { margin-bottom:12px; }.config-toolbar { display:flex; justify-content:flex-end; margin-bottom:8px; }
</style>
