<template>
  <div class="app-container live-room-page">
    <section class="room-hero">
      <div>
        <p class="eyebrow">Live Room Mapping</p>
        <h2>直播间映射</h2>
        <p>把插件采集到的直播间 ID 维护成业务名称，追单列表会优先展示这里配置的名称。</p>
      </div>
      <el-button type="primary" icon="Plus" @click="handleAdd" v-hasPermi="['live:room:add']">新增映射</el-button>
    </section>

    <section class="search-panel">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch">
        <el-form-item label="直播间ID" prop="roomKey">
          <el-input v-model="queryParams.roomKey" placeholder="请输入插件上报的 roomId" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="直播间名称" prop="roomName">
          <el-input v-model="queryParams.roomName" placeholder="请输入直播间名称" clearable @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" value="0" />
            <el-option label="停用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </section>

    <section class="table-panel">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['live:room:add']">新增</el-button>
        </el-col>
        <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
      </el-row>

      <el-table v-loading="loading" :data="roomList" row-key="room_key" border>
        <el-table-column label="直播间名称" prop="room_name" min-width="180" show-overflow-tooltip />
        <el-table-column label="直播间ID" prop="room_key" min-width="220" show-overflow-tooltip />
        <el-table-column label="来源" prop="source" min-width="160" show-overflow-tooltip />
        <el-table-column label="状态" prop="status" width="100" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.status === '1' ? 'info' : 'success'" effect="light">
              {{ scope.row.status === '1' ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="最后上报" prop="last_report_time" width="170" align="center" show-overflow-tooltip />
        <el-table-column label="备注" prop="remark" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['live:room:edit']">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['live:room:remove']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog v-model="open" :title="title" width="560px" append-to-body>
      <el-form ref="roomRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="直播间ID" prop="roomKey">
          <el-input v-model="form.roomKey" :disabled="isEdit" placeholder="请输入插件上报的 roomId" />
        </el-form-item>
        <el-form-item label="直播间名称" prop="roomName">
          <el-input v-model="form.roomName" placeholder="例如：家电一号直播间" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="例如：douyin_anchor_dashboard" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="open = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LiveRoomMapping">
import { addLiveRoom, deleteLiveRoom, listLiveRooms, updateLiveRoom } from '@/api/live/viewer'

const { proxy } = getCurrentInstance()

const showSearch = ref(true)
const loading = ref(false)
const open = ref(false)
const isEdit = ref(false)
const title = ref('')
const roomList = ref([])

const queryParams = reactive({
  roomKey: undefined,
  roomName: undefined,
  status: undefined
})

const form = reactive({
  roomKey: undefined,
  roomName: undefined,
  source: 'douyin_anchor_dashboard',
  status: '0',
  remark: undefined
})

const rules = {
  roomKey: [{ required: true, message: '直播间ID不能为空', trigger: 'blur' }],
  roomName: [{ required: true, message: '直播间名称不能为空', trigger: 'blur' }]
}

function resetFormData() {
  form.roomKey = undefined
  form.roomName = undefined
  form.source = 'douyin_anchor_dashboard'
  form.status = '0'
  form.remark = undefined
  proxy.resetForm('roomRef')
}

function getList() {
  loading.value = true
  listLiveRooms(queryParams).then((res) => {
    roomList.value = res.rows || []
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  getList()
}

function handleAdd() {
  resetFormData()
  isEdit.value = false
  title.value = '新增直播间映射'
  open.value = true
}

function handleUpdate(row) {
  resetFormData()
  isEdit.value = true
  title.value = '修改直播间映射'
  form.roomKey = row.room_key
  form.roomName = row.room_name
  form.source = row.source
  form.status = row.status || '0'
  form.remark = row.remark
  open.value = true
}

function submitForm() {
  proxy.$refs.roomRef.validate((valid) => {
    if (!valid) return
    const payload = {
      roomKey: form.roomKey,
      roomName: form.roomName,
      source: form.source,
      status: form.status,
      remark: form.remark
    }
    const request = isEdit.value ? updateLiveRoom(form.roomKey, payload) : addLiveRoom(payload)
    request.then(() => {
      proxy.$modal.msgSuccess('保存成功')
      open.value = false
      getList()
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm(`确认删除直播间映射「${row.room_name || row.room_key}」吗？`).then(() => {
    return deleteLiveRoom(row.room_key)
  }).then(() => {
    proxy.$modal.msgSuccess('删除成功')
    getList()
  }).catch(() => {})
}

getList()
</script>

<style scoped lang="scss">
.live-room-page {
  background:
    radial-gradient(circle at 10% 10%, rgba(108, 92, 231, 0.14), transparent 24rem),
    radial-gradient(circle at 92% 8%, rgba(162, 155, 254, 0.2), transparent 22rem);
}

.room-hero,
.search-panel,
.table-panel {
  border: 1px solid rgba(132, 119, 255, 0.12);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 18px 45px rgba(74, 72, 128, 0.08);
  backdrop-filter: blur(16px);
}

.room-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  margin-bottom: 16px;

  h2 {
    margin: 8px 0;
    color: #20233a;
    font-size: 28px;
    font-weight: 800;
  }

  p {
    margin: 0;
    color: #707893;
  }
}

.eyebrow {
  display: inline-flex;
  margin: 0;
  padding: 6px 12px;
  border-radius: 999px;
  color: #6c5ce7;
  background: rgba(108, 92, 231, 0.1);
  font-weight: 700;
}

.search-panel,
.table-panel {
  padding: 18px;
  margin-bottom: 16px;
}
</style>
