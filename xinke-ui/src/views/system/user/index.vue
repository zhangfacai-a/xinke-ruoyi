<template>
  <div class="app-container tree-sidebar-manage-wrap">
    <tree-panel title="组织机构" :tree-data="deptOptions" search-placeholder="请输入部门名称" storage-key="dept-sidebar-width" :defaultExpandAll="true" @node-click="handleNodeClick" @refresh="getDeptTree" ref="deptTreeRef" />
    <div class="tree-sidebar-content">
      <div class="content-inner">
        <header class="people-header"><div><h2>人员与权限</h2><p>按姓名找人，在人员详情中统一维护资料、业务分工和功能权限。</p></div><div class="people-shortcuts"><el-button @click="router.push('/ops-people/role')" v-hasPermi="['system:role:list']">岗位与通用权限</el-button><el-button @click="router.push('/ops-people/shops')" v-hasPermi="['system:user:edit']">店铺与人员组</el-button><el-button @click="router.push('/ops-people/liveSubject')" v-hasPermi="['system:user:edit','live:metrics:manage']">直播间分工</el-button></div></header>
        <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="找人员" prop="personKeyword">
            <el-input v-model="queryParams.personKeyword" placeholder="姓名 / 登录账号 / 手机号" clearable style="width: 220px" @keyup.enter="handleQuery" />
          </el-form-item>
          <el-form-item label="账号状态" prop="status">
            <el-select v-model="queryParams.status" placeholder="账号状态" clearable style="width: 220px">
              <el-option v-for="dict in sys_normal_disable" :key="dict.value" :label="dict.label" :value="dict.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="advancedSearch" label="创建时间" style="width: 308px">
            <el-date-picker v-model="dateRange" value-format="YYYY-MM-DD" type="daterange" range-separator="-" start-placeholder="开始日期" end-placeholder="结束日期"></el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button><el-button link @click="advancedSearch = !advancedSearch">{{ advancedSearch ? '收起筛选' : '更多筛选' }}</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8 people-tools">
          <el-col :span="1.5"><el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['system:user:add']">新增人员</el-button></el-col>
          <el-col :span="1.5"><el-button plain icon="Refresh" :loading="dingSyncing" @click="handleDingTalkSync" v-hasPermi="['system:user:add']">同步钉钉</el-button></el-col>
          <el-col :span="1.5"><el-dropdown><el-button>更多工具<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-hasPermi="['system:user:import']" @click="handleImport">导入人员</el-dropdown-item><el-dropdown-item v-hasPermi="['system:user:export']" @click="handleExport">导出人员</el-dropdown-item><el-dropdown-item v-hasPermi="['system:user:edit']" :disabled="reconciling" @click="reconcileAccess">核对业务自动权限</el-dropdown-item></el-dropdown-menu></template></el-dropdown></el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" :columns="columns" storageKey="personnel-columns-v2"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="userList">
          <el-table-column label="人员编号" align="center" key="userId" prop="userId" v-if="columns.userId.visible" />
          <el-table-column label="姓名" min-width="155" v-if="columns.nickName.visible"><template #default="{row}"><el-button link type="primary" @click="personnelDrawer.open(row)">{{ row.nickName || '未填写姓名' }}</el-button></template></el-table-column>
          <el-table-column label="登录账号" prop="userName" min-width="145" v-if="columns.userName.visible" show-overflow-tooltip />
          <el-table-column label="部门" align="center" key="deptName" prop="dept.deptName" v-if="columns.deptName.visible" :show-overflow-tooltip="true" />
          <el-table-column label="手机号码" align="center" key="phonenumber" prop="phonenumber" v-if="columns.phonenumber.visible" width="120" />
          <el-table-column label="资料来源" width="110"><template #default="{row}"><el-tag type="info" size="small">{{ row.source==='DINGTALK'?'钉钉':'网站' }}</el-tag></template></el-table-column>
          <el-table-column label="业务岗位" min-width="170">
            <template #default="{row}"><span>{{ businessRoleNames(row.businessRoles) }}</span><el-tag v-if="row.inactiveAssignment" type="warning" size="small">停用待核查</el-tag><el-tag v-if="row.noRole" type="info" size="small">未关联角色</el-tag></template>
          </el-table-column>
          <el-table-column label="业务分配" width="140">
            <template #default="{row}"><el-button link type="primary" v-hasPermi="['system:user:query']" @click="personnelDrawer.open(row)">{{ row.assignmentCount ?? '—' }} 项 · 人员详情</el-button></template>
          </el-table-column>
          <el-table-column label="账号状态" align="center" key="status" v-if="columns.status.visible">
            <template #default="scope">
              <el-switch
                v-model="scope.row.status"
                :disabled="!checkPermi(['system:user:edit']) || scope.row.userId === 1"
                active-value="0"
                inactive-value="1"
                @change="handleStatusChange(scope.row)"
              ></el-switch>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" v-if="columns.createTime.visible" width="160">
            <template #default="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="165" fixed="right"><template #default="{row}"><el-button link type="primary" @click="personnelDrawer.open(row)">详情</el-button><el-button v-if="row.userId!==1" link v-hasPermi="['system:user:edit']" @click="handleUpdate(row)">编辑</el-button><el-dropdown v-if="row.userId!==1" class="person-more"><el-button link>更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-hasPermi="['system:user:resetPwd']" @click="handleResetPwd(row)">重置密码</el-dropdown-item><el-dropdown-item v-hasPermi="['system:user:remove']" divided @click="handleDelete(row)">删除账号</el-dropdown-item></el-dropdown-menu></template></el-dropdown></template></el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" v-model:page="queryParams.pageNum" v-model:limit="queryParams.pageSize" @pagination="getList" />
      </div>
    </div>

    <!-- 添加或编辑人员配置对话框 -->
    <el-dialog :title="title" v-model="open" width="600px" append-to-body>
      <el-alert title="姓名用于识别人员，登录账号用于登录。业务分工及运费权限请在人员详情中配置。" type="info" :closable="false" class="mb8" />
      <el-form :model="form" :rules="rules" ref="userRef" label-width="80px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="姓名" prop="nickName">
              <el-input v-model="form.nickName" placeholder="请输入姓名" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属部门" prop="deptId">
              <el-tree-select v-model="form.deptId" :data="enabledDeptOptions" :props="{ value: 'id', label: 'label', children: 'children' }" value-key="id" placeholder="请选择归属部门" clearable check-strictly />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号码" prop="phonenumber">
              <el-input v-model="form.phonenumber" placeholder="请输入手机号码" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱" prop="email">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="登录账号" prop="userName">
              <el-input v-model="form.userName" :disabled="form.userId != undefined" placeholder="请输入登录账号" maxlength="30" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="form.userId == undefined" label="用户密码" prop="password" :rules="pwdValidator">
              <el-input v-model="form.password" placeholder="请输入用户密码" type="password" maxlength="20" show-password />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户性别">
              <el-select v-model="form.sex" placeholder="请选择">
                <el-option v-for="dict in sys_user_sex" :key="dict.value" :label="dict.label" :value="dict.value"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态">
              <el-radio-group v-model="form.status">
                <el-radio v-for="dict in sys_normal_disable" :key="dict.value" :value="dict.value">{{ dict.label }}</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="组织岗位">
              <el-select v-model="form.postIds" multiple placeholder="请选择">
                <el-option v-for="item in postOptions" :key="item.postId" :label="item.postName" :value="item.postId" :disabled="item.status == 1"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="系统角色">
              <el-select v-model="form.roleIds" multiple placeholder="请选择">
                <el-option v-for="item in roleOptions" :key="item.roleId" :label="item.roleName" :value="item.roleId" :disabled="item.status == 1 || isManagedRole(item)"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"></el-input>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <PersonnelDrawer ref="personnelDrawer" @edit="handleUpdate" @changed="getList" />
    <!-- 用户导入对话框 -->
    <excel-import-dialog ref="importUserRef" title="用户导入" action="/system/user/importData" template-action="/system/user/importTemplate" template-file-name="user_template" update-support-label="是否更新已经存在的用户数据" @success="getList" />
  </div>
</template>

<script setup name="User">
import TreePanel from "@/components/TreePanel"
import ExcelImportDialog from "@/components/ExcelImportDialog"
import { checkPermi } from '@/utils/permission'
import PersonnelDrawer from './PersonnelDrawer.vue'
import { personnelSummaries, personnelDetail, reconcilePersonnel } from '@/api/live/personnel'
import { usePasswordRule } from "@/utils/passwordRule"
import { changeUserStatus, listUser, resetUserPwd, delUser, getUser, updateUser, addUser, deptTreeSelect, syncDingTalkUsers } from "@/api/system/user"

const isManagedRole = role => /^(auto_live_|personnel_freight_)/.test(role.roleKey || '')
const router = useRouter()
const { proxy } = getCurrentInstance()
const { pwdValidator, pwdPromptValidator } = usePasswordRule()
const { sys_normal_disable, sys_user_sex } = useDict("sys_normal_disable", "sys_user_sex")

const userList = ref([])
const open = ref(false)
const loading = ref(true)
const dingSyncing = ref(false)
const personnelDrawer = ref(null)
const reconciling = ref(false)
async function reconcileAccess(){await proxy.$modal.confirm('按当前有效业务分配核对自动权限，保留人工角色与历史订单。确认执行？');reconciling.value=true;try{await reconcilePersonnel();proxy.$modal.msgSuccess('自动权限已核对');await getList()}finally{reconciling.value=false}}
const businessRoleNames=value=>String(value||'').split(',').filter(Boolean).map(code=>({anchor:'主播',controller:'场控',operator:'运营',customer_service:'电话客服',customer_admin:'客服管理员'}[code]||code)).join('、')||'未配置'
const showSearch = ref(true)
const advancedSearch = ref(false)
const total = ref(0)
const title = ref("")
const dateRange = ref([])
const deptOptions = ref(undefined)
const enabledDeptOptions = ref(undefined)
const initPassword = ref(undefined)
const postOptions = ref([])
const roleOptions = ref([])
// 列显隐信息
const columns = ref({
  userId: { label: '人员编号', visible: false },
  userName: { label: '登录账号', visible: true },
  nickName: { label: '姓名', visible: true },
  deptName: { label: '部门', visible: true },
  phonenumber: { label: '手机号码', visible: false },
  status: { label: '状态', visible: true },
  createTime: { label: '创建时间', visible: false }
})

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    personKeyword: undefined,
    phonenumber: undefined,
    status: undefined,
    deptId: undefined
  },
  rules: {
    userName: [{ required: true, message: "登录账号不能为空", trigger: "blur" }, { min: 2, max: 20, message: "登录账号长度必须介于 2 和 20 之间", trigger: "blur" }],
    nickName: [{ required: true, message: "姓名不能为空", trigger: "blur" }],
    email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: ["blur", "change"] }],
    phonenumber: [{ pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号码", trigger: "blur" }]
  }
})

const { queryParams, form, rules } = toRefs(data)

/** 查询用户列表 */
let listGeneration=0
async function getList() {
  const generation=++listGeneration
  loading.value=true
  try {
    const requestParams=proxy.addDateRange({...queryParams.value},dateRange.value)
    requestParams.params={...requestParams.params,personKeyword:queryParams.value.personKeyword}
    const res=await listUser(requestParams)
    if(generation!==listGeneration)return
    userList.value=res.rows;total.value=res.total
    if(res.rows.length){
      const summaries=(await personnelSummaries(res.rows.map(row=>row.userId))).data||[]
      if(generation===listGeneration){const byId=new Map(summaries.map(row=>[String(row.userId),row]));userList.value=res.rows.map(row=>({...row,...byId.get(String(row.userId))}))}
    }
  }finally{if(generation===listGeneration)loading.value=false}
}

/** 查询部门下拉树结构 */
function getDeptTree() {
  deptTreeSelect().then(response => {
    deptOptions.value = response.data
    enabledDeptOptions.value = filterDisabledDept(JSON.parse(JSON.stringify(response.data)))
  })
}

/** 过滤禁用的部门 */
function filterDisabledDept(deptList) {
  return deptList.filter(dept => {
    if (dept.disabled) {
      return false
    }
    if (dept.children && dept.children.length) {
      dept.children = filterDisabledDept(dept.children)
    }
    return true
  })
}

/** 节点单击事件 */
function handleNodeClick(data) {
  queryParams.value.deptId = data.id
  handleQuery()
}

/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  queryParams.value.deptId = undefined
  proxy.$refs.deptTreeRef.setCurrentKey(null)
  handleQuery()
}

/** 删除按钮操作 */
function handleDelete(row) {
  const userIds = row.userId
  proxy.$modal.confirm('确认删除“'+row.nickName+'”的登录账号？如人员离职，建议先停用并完成交接。历史业务记录保留。').then(function () {
    return delUser(userIds)
  }).then(() => {
    getList()
    proxy.$modal.msgSuccess("删除成功")
  }).catch(() => {})
}

/** 导出按钮操作 */
function handleExport() {
  const params=proxy.addDateRange({...queryParams.value},dateRange.value)
  params.params={...params.params,personKeyword:queryParams.value.personKeyword}
  proxy.download("system/user/export", params,`user_${new Date().getTime()}.xlsx`)
}

/** 账号状态修改  */
async function handleStatusChange(row) {
  const text=row.status==='0'?'启用':'停用'
  try {
    let message='确认'+text+'账号“'+row.nickName+'”？'
    if(row.status==='1'){
      const info=(await personnelDetail(row.userId)).data
      message+=` 当前有 ${info.pendingRetention} 条未完成挽单、${info.pendingFollowups} 条待办跟进、${info.assignments.length} 项业务分配。请先核查交接；停用将退出登录并保留历史记录。`
    }
    await proxy.$modal.confirm(message)
    await changeUserStatus(row.userId,row.status)
    proxy.$modal.msgSuccess(text+'成功')
    await getList()
  }catch{row.status=row.status==='0'?'1':'0'}
}

/** 重置密码按钮操作 */
function handleResetPwd(row) {
  proxy.$prompt(`为${row.nickName}（${row.userName}）设置新密码`, "重置密码", {
    confirmButtonText: "确定",
    cancelButtonText: "取消",
    closeOnClickModal: false,
    inputValidator: pwdPromptValidator
  }).then(({ value }) => {
    resetUserPwd(row.userId, value).then(() => {
      proxy.$modal.msgSuccess("密码已重置")
    })
  }).catch(() => {})
}

/** 导入按钮操作 */
function handleImport() {
  proxy.$refs["importUserRef"].open()
}

async function handleDingTalkSync() {
  dingSyncing.value = true
  try {
    const response = await syncDingTalkUsers()
    const result = response.data || {}
    proxy.$modal.msgSuccess(`同步完成：成功 ${result.success || 0} 人，停用 ${result.disabled || 0} 人`)
    if (result.failures?.length) {
      proxy.$alert(result.failures.slice(0, 30).join('\n'), '未同步人员')
    }
    getDeptTree()
    getList()
  } finally {
    dingSyncing.value = false
  }
}

/** 重置操作表单 */
function reset() {
  form.value = {
    userId: undefined,
    deptId: undefined,
    userName: undefined,
    nickName: undefined,
    password: undefined,
    phonenumber: undefined,
    email: undefined,
    sex: undefined,
    status: "0",
    remark: undefined,
    postIds: [],
    roleIds: []
  }
  proxy.resetForm("userRef")
}

/** 取消按钮 */
function cancel() {
  open.value = false
  reset()
}

/** 新增按钮操作 */
function handleAdd() {
  reset()
  getUser().then(response => {
    postOptions.value = response.posts
    roleOptions.value = response.roles
    open.value = true
    title.value = "新增人员"
    form.value.password = initPassword.value
  })
}

/** 修改按钮操作 */
function handleUpdate(row) {
  reset()
  const userId = row.userId
  getUser(userId).then(response => {
    form.value = response.data
    postOptions.value = response.posts
    roleOptions.value = response.roles
    form.value.postIds = response.postIds
    form.value.roleIds = response.roleIds
    open.value = true
    title.value = "编辑人员"
    form.value.password = ""
  })
}

/** 提交按钮 */
function submitForm() {
  proxy.$refs["userRef"].validate(valid => {
    if (valid) {
      if (form.value.userId != undefined) {
        updateUser(form.value).then(() => {
          proxy.$modal.msgSuccess("修改成功")
          open.value = false
          getList()
          personnelDrawer.value?.open(form.value)
        })
      } else {
        addUser(form.value).then(() => {
          proxy.$modal.msgSuccess("新增成功")
          open.value = false
          getList()
        })
      }
    }
  })
}

onMounted(() => {
  getDeptTree()
  getList()
  proxy.getConfigKey("sys.user.initPassword").then(response => {
    initPassword.value = response.msg
  })
})
</script>

<style scoped>
.people-header{display:flex;justify-content:space-between;gap:20px;align-items:center;margin-bottom:24px}.people-header h2{font-size:23px;margin:0 0 8px}.people-header p{color:var(--el-text-color-secondary);margin:0;line-height:1.7;font-size:13px}.people-shortcuts{display:flex;flex-wrap:wrap;gap:8px}.people-tools{align-items:center;row-gap:10px}.person-more{margin-left:12px}@media(max-width:900px){.people-header{align-items:flex-start;flex-direction:column}}
</style>
