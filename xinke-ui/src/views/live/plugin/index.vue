<template>
  <div class="app-container plugin-page">
    <section class="plugin-hero">
      <div>
        <span class="eyebrow">采集控制</span>
        <h2>直播采集插件控制</h2>
        <p>统一控制插件是否允许上报数据，拦截过期版本，也可以单独禁用某台电脑。</p>
      </div>
      <el-tag :type="form.enabled ? 'success' : 'danger'" size="large">
        {{ form.enabled ? '允许上报' : '已关闭上报' }}
      </el-tag>
    </section>

    <section class="control-grid">
      <article class="panel switch-panel">
        <div class="panel-head">
          <div>
            <h3>采集总开关</h3>
            <p>关闭后，插件下次上报会收到停止响应，并自动停止监听。</p>
          </div>
          <el-switch
            v-model="form.enabled"
            size="large"
            :loading="saving"
            active-text="开启"
            inactive-text="关闭"
            @change="saveData"
          />
        </div>
        <div class="status-card" :class="{ off: !form.enabled }">
          <strong>{{ form.enabled ? '插件当前可以写入数据' : '插件当前禁止写入数据' }}</strong>
          <span>{{ form.enabled ? '适合正常直播采集。' : '适合排查异常、冻结采集或停止旧版本插件。' }}</span>
        </div>
      </article>

      <article class="panel">
        <div class="panel-head">
          <div>
            <h3>版本策略</h3>
            <p>低于最低允许版本的插件会被后端拒绝，并收到自动停止指令。</p>
          </div>
        </div>
        <el-form :model="form" label-width="120px" class="version-form">
          <el-form-item label="最低允许版本">
            <el-input v-model="form.minVersion" placeholder="例如 4.0.8" />
          </el-form-item>
          <el-form-item label="最新版本">
            <el-input v-model="form.latestVersion" placeholder="例如 4.0.8" />
          </el-form-item>
        </el-form>
      </article>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h3>停止响应</h3>
          <p>插件收到以下业务响应后会自动停止，本地待上报队列会保留，不会直接丢弃。</p>
        </div>
        <div class="actions">
          <el-button icon="Refresh" @click="loadData">刷新</el-button>
          <el-button type="primary" icon="Check" :loading="saving" @click="saveData">保存版本策略</el-button>
        </div>
      </div>

      <el-table :data="responseRows" border>
        <el-table-column label="场景" prop="scene" width="190" />
        <el-table-column label="业务码" prop="code" width="140" />
        <el-table-column label="插件动作" prop="action" width="150" />
        <el-table-column label="说明" prop="remark" min-width="300" />
      </el-table>
    </section>

    <section class="panel">
      <div class="panel-head">
        <div>
          <h3>插件电脑列表</h3>
          <p>每台电脑首次运行插件会生成唯一编号。禁用后，该电脑下次上报会自动停止。</p>
        </div>
        <el-button icon="Refresh" @click="loadClients">刷新列表</el-button>
      </div>

      <el-form :model="clientQuery" :inline="true" class="client-filter">
        <el-form-item label="电脑编号">
          <el-input v-model="clientQuery.clientId" clearable placeholder="搜索 clientId" @keyup.enter="loadClients" />
        </el-form-item>
        <el-form-item label="电脑名称">
          <el-input v-model="clientQuery.clientName" clearable placeholder="搜索名称" @keyup.enter="loadClients" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="clientQuery.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="允许" value="0" />
            <el-option label="禁用" value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadClients">搜索</el-button>
          <el-button icon="Refresh" @click="resetClientQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table v-loading="clientLoading" :data="clients" border>
        <el-table-column label="电脑名称" min-width="150">
          <template #default="{ row }">
            <el-input v-model="row.clientName" placeholder="给这台电脑起个名字" @change="saveClient(row)" />
          </template>
        </el-table-column>
        <el-table-column label="电脑编号" prop="clientId" min-width="250" show-overflow-tooltip />
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status !== '1'"
              active-text="允许"
              inactive-text="禁用"
              @change="(enabled) => toggleClient(row, enabled)"
            />
          </template>
        </el-table-column>
        <el-table-column label="版本" prop="pluginVersion" width="100" />
        <el-table-column label="直播间" prop="liveRoomName" min-width="160" show-overflow-tooltip />
        <el-table-column label="上报次数" prop="reportCount" width="100" align="right" />
        <el-table-column label="异常次数" prop="failCount" width="100" align="right" />
        <el-table-column label="最后上报" prop="lastReportTime" min-width="165" />
        <el-table-column label="最后成功" prop="lastSuccessTime" min-width="165" />
        <el-table-column label="最后异常" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.lastErrorTime || '-' }}</span>
            <span v-if="row.lastErrorMsg" class="error-msg"> {{ row.lastErrorMsg }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.remark" placeholder="备注" @change="saveClient(row)" />
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="clientTotal > 0"
        v-model:page="clientQuery.pageNum"
        v-model:limit="clientQuery.pageSize"
        :total="clientTotal"
        @pagination="loadClients"
      />
    </section>
  </div>
</template>

<script setup name="LivePluginControl">
import { computed, getCurrentInstance, onMounted, reactive, ref } from 'vue'
import { getPluginControl, listPluginClients, updatePluginClient, updatePluginControl } from '@/api/live/plugin'

const { proxy } = getCurrentInstance()

const saving = ref(false)
const clientLoading = ref(false)
const clients = ref([])
const clientTotal = ref(0)

const form = reactive({
  enabled: true,
  minVersion: '4.0.8',
  latestVersion: '4.0.8',
  disabledCode: 46001,
  versionBlockedCode: 46002,
  clientDisabledCode: 46003
})

const clientQuery = reactive({
  pageNum: 1,
  pageSize: 10,
  clientId: '',
  clientName: '',
  status: ''
})

const responseRows = computed(() => [
  {
    scene: '系统关闭采集',
    code: form.disabledCode,
    action: '停止插件',
    remark: '采集总开关关闭时返回。'
  },
  {
    scene: '插件版本过低',
    code: form.versionBlockedCode,
    action: '停止插件',
    remark: `插件版本低于 ${form.minVersion || '-'} 时返回。`
  },
  {
    scene: '单台电脑禁用',
    code: form.clientDisabledCode,
    action: '停止插件',
    remark: '后台禁用某台电脑后，该电脑下次上报时返回。'
  }
])

function loadData() {
  getPluginControl().then((res) => {
    const data = res.data || {}
    form.enabled = data.enabled === true || data.enabled === 'true'
    form.minVersion = data.minVersion || '4.0.8'
    form.latestVersion = data.latestVersion || form.minVersion
    form.disabledCode = data.disabledCode || 46001
    form.versionBlockedCode = data.versionBlockedCode || 46002
    form.clientDisabledCode = data.clientDisabledCode || 46003
  })
}

function saveData() {
  if (saving.value) return
  saving.value = true
  updatePluginControl({
    enabled: form.enabled,
    minVersion: form.minVersion,
    latestVersion: form.latestVersion
  }).then((res) => {
    const data = res.data || {}
    form.enabled = data.enabled === true || data.enabled === 'true'
    form.minVersion = data.minVersion || form.minVersion
    form.latestVersion = data.latestVersion || form.latestVersion
    proxy.$modal.msgSuccess('插件控制设置已保存')
  }).finally(() => {
    saving.value = false
  })
}

function loadClients() {
  clientLoading.value = true
  listPluginClients(clientQuery).then((res) => {
    clients.value = (res.rows || []).map(normalizeClientRow)
    clientTotal.value = res.total || 0
  }).finally(() => {
    clientLoading.value = false
  })
}

function normalizeClientRow(row) {
  return {
    ...row,
    clientId: row.clientId || row.client_id || '',
    clientName: row.clientName || row.client_name || '',
    pluginVersion: row.pluginVersion || row.plugin_version || '',
    roomKey: row.roomKey || row.room_key || '',
    liveRoomName: row.liveRoomName || row.live_room_name || '',
    reportCount: row.reportCount ?? row.report_count ?? 0,
    failCount: row.failCount ?? row.fail_count ?? 0,
    lastReportTime: row.lastReportTime || row.last_report_time || '',
    lastSuccessTime: row.lastSuccessTime || row.last_success_time || '',
    lastErrorTime: row.lastErrorTime || row.last_error_time || '',
    lastErrorMsg: row.lastErrorMsg || row.last_error_msg || '',
    status: String(row.status ?? '0')
  }
}

function resetClientQuery() {
  clientQuery.pageNum = 1
  clientQuery.clientId = ''
  clientQuery.clientName = ''
  clientQuery.status = ''
  loadClients()
}

function saveClient(row) {
  if (!row.clientId) {
    proxy.$modal.msgError('电脑编号为空，无法保存')
    return
  }
  updatePluginClient(row.clientId, {
    clientName: row.clientName,
    status: row.status,
    remark: row.remark
  }).then(() => {
    proxy.$modal.msgSuccess('电脑配置已保存')
  })
}

function toggleClient(row, enabled) {
  row.status = enabled ? '0' : '1'
  saveClient(row)
}

onMounted(() => {
  loadData()
  loadClients()
})
</script>

<style scoped lang="scss">
.plugin-page {
  display: grid;
  gap: 16px;
}

.plugin-hero,
.panel {
  border: 1px solid rgba(122, 118, 255, .12);
  border-radius: 16px;
  background: rgba(255, 255, 255, .92);
  box-shadow: 0 18px 42px rgba(80, 86, 160, .1);
}

.plugin-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px 24px;
  background:
    radial-gradient(circle at 86% 0%, rgba(162, 155, 254, .22), transparent 34%),
    rgba(255, 255, 255, .92);

  h2 {
    margin: 8px 0;
    font-size: 28px;
  }

  p {
    margin: 0;
    color: #717a96;
  }
}

.eyebrow {
  display: inline-flex;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(108, 92, 231, .1);
  color: #6c5ce7;
  font-weight: 800;
}

.control-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 16px;
}

.panel {
  min-width: 0;
  padding: 18px;
}

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;

  h3 {
    margin: 0;
    font-size: 18px;
  }

  p {
    margin: 6px 0 0;
    color: #8790aa;
    font-size: 13px;
  }
}

.status-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 14px;
  background: linear-gradient(135deg, rgba(20, 184, 166, .12), rgba(108, 92, 231, .08));
  color: #0f766e;

  strong {
    font-size: 20px;
  }

  span {
    color: #64748b;
  }

  &.off {
    background: linear-gradient(135deg, rgba(248, 113, 113, .14), rgba(251, 191, 36, .1));
    color: #b91c1c;
  }
}

.actions,
.client-filter {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.error-msg {
  color: #ef4444;
}

@media (max-width: 1200px) {
  .control-grid {
    grid-template-columns: 1fr;
  }
}
</style>
