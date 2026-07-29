<template>
  <div class="app-container datahub-list-page">
    <div class="dataset-layout" :class="{ 'without-folder-panel': !canListFolders || isMobile }">
      <aside v-if="canListFolders && !isMobile" class="folder-sidebar" aria-label="个人文件夹">
        <div class="folder-panel">
          <div class="folder-panel-header">
            <span>个人文件夹</span>
            <div class="folder-header-actions">
              <el-tooltip v-if="canAddFolder" content="新建文件夹" placement="top">
                <el-button text circle :icon="Plus" aria-label="新建文件夹" @click="openCreateFolder()" />
              </el-tooltip>
              <el-tooltip content="刷新文件夹" placement="top">
                <el-button text circle :icon="Refresh" aria-label="刷新文件夹" @click="refreshAll" />
              </el-tooltip>
            </div>
          </div>

          <nav class="virtual-folder-list" aria-label="数据表范围">
            <button
              v-for="item in virtualFolders"
              :key="item.key"
              type="button"
              class="folder-nav-button"
              :class="{ active: activeFolderKey === item.key }"
              @click="selectVirtualFolder(item)"
            >
              <el-icon><component :is="item.icon" /></el-icon>
              <span>{{ item.label }}</span>
            </button>
          </nav>

          <div class="custom-folder-heading">自定义目录</div>
          <el-tree
            ref="folderTreeRef"
            v-loading="folderLoading"
            class="folder-tree"
            :data="folderTree"
            :props="treeProps"
            node-key="folderId"
            highlight-current
            default-expand-all
            :expand-on-click-node="false"
            empty-text="暂无自定义目录"
            @node-click="selectCustomFolder"
          >
            <template #default="{ data }">
              <div class="folder-node">
                <el-icon><Folder /></el-icon>
                <span class="folder-node-label">{{ data.folderName }}</span>
                <span v-if="Number(data.itemCount) > 0" class="folder-count">{{ formatCount(data.itemCount) }}</span>
                <el-dropdown
                  v-if="hasFolderActions"
                  trigger="click"
                  @click.stop
                  @command="handleFolderCommand($event, data)"
                >
                  <el-button text circle :icon="MoreFilled" class="folder-more" aria-label="文件夹操作" @click.stop />
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-if="canAddFolder && data.depth < 8" command="add" :icon="Plus">新建子目录</el-dropdown-item>
                      <el-dropdown-item v-if="canEditFolder" command="rename" :icon="Edit">重命名</el-dropdown-item>
                      <el-dropdown-item v-if="canRemoveFolder" command="delete" :icon="Delete" divided>删除空目录</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </template>
          </el-tree>
        </div>
      </aside>

      <main class="dataset-workspace">
        <div class="dataset-scope-header">
          <div class="dataset-scope-title">
            <el-button
              v-if="canListFolders && isMobile"
              :icon="FolderOpened"
              class="mobile-folder-button"
              @click="folderDrawerOpen = true"
            >文件夹</el-button>
            <div>
              <h2>{{ activeFolderLabel }}</h2>
              <span>{{ formatCount(total) }} 张数据表</span>
            </div>
          </div>
        </div>

        <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch" class="dataset-search-form">
          <el-form-item label="数据表名称" prop="displayName">
            <el-input
              v-model="queryParams.displayName"
              placeholder="请输入数据表名称"
              clearable
              style="width: 220px"
              @keyup.enter="handleQuery"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
          </el-form-item>
        </el-form>

        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button
              type="primary"
              plain
              icon="Upload"
              @click="handleCreate"
              v-hasPermi="['datahub:dataset:add']"
            >从文件创建</el-button>
          </el-col>
          <right-toolbar v-model:showSearch="showSearch" @queryTable="refreshAll" />
        </el-row>

        <el-table v-loading="loading" :data="datasetList">
          <el-table-column label="数据表名称" prop="displayName" min-width="180" fixed="left">
            <template #default="scope">
              <el-button v-if="canQuery" link type="primary" class="dataset-name" @click="handleDetail(scope.row)">
                {{ scope.row.displayName || '-' }}
              </el-button>
              <span v-else>{{ scope.row.displayName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="英文表名" prop="datasetCode" min-width="190" show-overflow-tooltip>
            <template #default="scope"><span class="mono">{{ scope.row.datasetCode || '-' }}</span></template>
          </el-table-column>
          <el-table-column v-if="canListFolders" label="所在文件夹" min-width="150" show-overflow-tooltip>
            <template #default="scope">{{ folderLabel(scope.row.folderId) }}</template>
          </el-table-column>
          <el-table-column label="数据量" prop="rowCount" width="110" align="right">
            <template #default="scope">{{ formatCount(scope.row.rowCount) }}</template>
          </el-table-column>
          <el-table-column label="字段数" prop="columnCount" width="90" align="right" />
          <el-table-column label="状态" prop="status" width="110" align="center">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)" effect="light">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建人" prop="ownerUserName" width="120" show-overflow-tooltip />
          <el-table-column label="来源文件" prop="sourceFileName" min-width="180" show-overflow-tooltip />
          <el-table-column label="Sheet" prop="sourceSheetName" min-width="130" show-overflow-tooltip />
          <el-table-column label="版本" width="90" align="center">
            <template #default="scope">v{{ scope.row.currentVersionNo || scope.row.currentSchemaVersion || 1 }}</template>
          </el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="170" align="center">
            <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" :width="canMoveDataset ? 162 : 90" align="center" fixed="right">
            <template #default="scope">
              <el-button link type="primary" icon="View" @click="handleDetail(scope.row)" v-hasPermi="['datahub:dataset:query']">详情</el-button>
              <el-button v-if="canMoveDataset" link type="primary" :icon="FolderOpened" @click="openMoveDataset(scope.row)">移动</el-button>
            </template>
          </el-table-column>
        </el-table>

        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </main>
    </div>

    <el-drawer
      v-if="canListFolders && isMobile"
      v-model="folderDrawerOpen"
      title="个人文件夹"
      size="min(86vw, 320px)"
      direction="ltr"
      append-to-body
      class="datahub-folder-drawer"
    >
      <div class="folder-panel mobile-folder-panel">
        <div class="folder-panel-header">
          <span>浏览范围</span>
          <div class="folder-header-actions">
            <el-tooltip v-if="canAddFolder" content="新建文件夹" placement="top">
              <el-button text circle :icon="Plus" aria-label="新建文件夹" @click="openCreateFolder()" />
            </el-tooltip>
            <el-tooltip content="刷新文件夹" placement="top">
              <el-button text circle :icon="Refresh" aria-label="刷新文件夹" @click="refreshAll" />
            </el-tooltip>
          </div>
        </div>

        <nav class="virtual-folder-list" aria-label="数据表范围">
          <button
            v-for="item in virtualFolders"
            :key="item.key"
            type="button"
            class="folder-nav-button"
            :class="{ active: activeFolderKey === item.key }"
            @click="selectVirtualFolder(item)"
          >
            <el-icon><component :is="item.icon" /></el-icon>
            <span>{{ item.label }}</span>
          </button>
        </nav>

        <div class="custom-folder-heading">自定义目录</div>
        <el-tree
          v-loading="folderLoading"
          class="folder-tree"
          :data="folderTree"
          :props="treeProps"
          node-key="folderId"
          highlight-current
          default-expand-all
          :current-node-key="activeCustomFolderId"
          :expand-on-click-node="false"
          empty-text="暂无自定义目录"
          @node-click="selectCustomFolder"
        >
          <template #default="{ data }">
            <div class="folder-node">
              <el-icon><Folder /></el-icon>
              <span class="folder-node-label">{{ data.folderName }}</span>
              <span v-if="Number(data.itemCount) > 0" class="folder-count">{{ formatCount(data.itemCount) }}</span>
              <el-dropdown
                v-if="hasFolderActions"
                trigger="click"
                @click.stop
                @command="handleFolderCommand($event, data)"
              >
                <el-button text circle :icon="MoreFilled" class="folder-more" aria-label="文件夹操作" @click.stop />
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="canAddFolder && data.depth < 8" command="add" :icon="Plus">新建子目录</el-dropdown-item>
                    <el-dropdown-item v-if="canEditFolder" command="rename" :icon="Edit">重命名</el-dropdown-item>
                    <el-dropdown-item v-if="canRemoveFolder" command="delete" :icon="Delete" divided>删除空目录</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-tree>
      </div>
    </el-drawer>

    <el-dialog v-model="folderDialog.visible" :title="folderDialogTitle" width="480px" append-to-body destroy-on-close>
      <el-form ref="folderFormRef" :model="folderDialog" :rules="folderRules" label-width="92px">
        <el-form-item v-if="folderDialog.mode === 'create'" label="上级目录" prop="parentFolderId">
          <el-tree-select
            v-model="folderDialog.parentFolderId"
            :data="createParentOptions"
            :props="treeSelectProps"
            node-key="folderId"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目录名称" prop="folderName">
          <el-input v-model="folderDialog.folderName" maxlength="128" show-word-limit @keyup.enter="submitFolder" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="folderDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="folderSaving" @click="submitFolder">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveDialog.visible" title="移动数据表" width="480px" append-to-body destroy-on-close>
      <el-form label-width="92px">
        <el-form-item label="数据表">
          <span class="move-dataset-name">{{ moveDialog.datasetName }}</span>
        </el-form-item>
        <el-form-item label="目标目录" required>
          <el-tree-select
            v-model="moveDialog.folderId"
            :data="moveFolderOptions"
            :props="treeSelectProps"
            node-key="folderId"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="moveDialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="moveSaving" :disabled="moveTargetUnchanged" @click="submitMoveDataset">移动</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DataHubDataset">
import { Collection, Delete, Edit, Folder, FolderOpened, FolderRemove, MoreFilled, Plus, Refresh, Share, User } from '@element-plus/icons-vue'
import { useWindowSize } from '@vueuse/core'
import { listDataset } from '@/api/datahub/dataset'
import { createFolder, deleteFolder, listFolderTree, moveDatasetToFolder, updateFolder } from '@/api/datahub/folder'
import { checkPermi } from '@/utils/permission'

const { proxy } = getCurrentInstance()
const { width } = useWindowSize()
const isMobile = computed(() => width.value < 900)
const loading = ref(false)
const folderLoading = ref(false)
const folderSaving = ref(false)
const moveSaving = ref(false)
const showSearch = ref(true)
const folderDrawerOpen = ref(false)
const folderTreeRef = ref(null)
const folderFormRef = ref(null)
const datasetList = ref([])
const folderTree = ref([])
const total = ref(0)
const activeFolderKey = ref('ALL')

const canQuery = checkPermi(['datahub:dataset:query'])
const canListFolders = checkPermi(['datahub:folder:list'])
const canAddFolder = checkPermi(['datahub:folder:add'])
const canEditFolder = checkPermi(['datahub:folder:edit'])
const canRemoveFolder = checkPermi(['datahub:folder:remove'])
const canMoveDataset = canListFolders && checkPermi(['datahub:folder:item:edit'])
const hasFolderActions = canAddFolder || canEditFolder || canRemoveFolder

const virtualFolders = [
  { key: 'ALL', scope: 'ALL', label: '全部数据表', icon: Collection },
  { key: 'OWNED', scope: 'OWNED', label: '我创建的', icon: User },
  { key: 'SHARED', scope: 'SHARED', label: '共享给我', icon: Share },
  { key: 'UNCLASSIFIED', scope: 'UNCLASSIFIED', label: '未分类', icon: FolderRemove }
]

const treeProps = { children: 'children', label: 'folderName' }
const treeSelectProps = { children: 'children', label: 'folderName', value: 'folderId', disabled: 'disabled' }

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  displayName: undefined,
  folderScope: 'ALL',
  folderId: undefined
})

const folderDialog = reactive({
  visible: false,
  mode: 'create',
  folderId: undefined,
  parentFolderId: 0,
  folderName: '',
  lockVersion: undefined
})

const moveDialog = reactive({
  visible: false,
  datasetId: undefined,
  datasetName: '',
  folderId: 0,
  originalFolderId: 0,
  itemVersion: undefined
})

const folderRules = {
  folderName: [
    { required: true, message: '请输入目录名称', trigger: 'blur' },
    { max: 128, message: '目录名称不能超过128个字符', trigger: 'blur' },
    { validator: validateFolderName, trigger: 'blur' }
  ]
}

const statusLabels = {
  ACTIVE: '可用',
  NORMAL: '可用',
  READY: '可用',
  CREATING: '创建中',
  IMPORTING: '导入中',
  QUEUED: '排队中',
  FAILED: '失败',
  DISABLED: '停用'
}

const folderById = computed(() => {
  const result = new Map()
  walkFolders(folderTree.value, folder => result.set(Number(folder.folderId), folder))
  return result
})

const activeCustomFolderId = computed(() => {
  if (!activeFolderKey.value.startsWith('FOLDER:')) return undefined
  return Number(activeFolderKey.value.slice(7))
})

const activeFolderLabel = computed(() => {
  const virtual = virtualFolders.find(item => item.key === activeFolderKey.value)
  if (virtual) return virtual.label
  return folderById.value.get(activeCustomFolderId.value)?.folderName || '全部数据表'
})

const folderDialogTitle = computed(() => folderDialog.mode === 'create' ? '新建文件夹' : '重命名文件夹')

const createParentOptions = computed(() => [{
  folderId: 0,
  folderName: '个人文件夹',
  children: parentOptionNodes(folderTree.value)
}])

const moveFolderOptions = computed(() => [
  { folderId: 0, folderName: '未分类', children: [] },
  ...folderOptionNodes(folderTree.value)
])

const moveTargetUnchanged = computed(() => Number(moveDialog.folderId || 0) === Number(moveDialog.originalFolderId || 0))

function validateFolderName(rule, value, callback) {
  const name = String(value || '').trim()
  if (!name) return callback(new Error('请输入目录名称'))
  if (/[\\/\u0000-\u001f\u007f]/.test(name)) return callback(new Error('目录名称不能包含斜杠或控制字符'))
  callback()
}

function normalizeFolders(nodes, depth = 1) {
  if (!Array.isArray(nodes)) return []
  return nodes.map(node => ({
    ...node,
    folderId: Number(node.folderId),
    parentFolderId: Number(node.parentFolderId || 0),
    lockVersion: Number(node.lockVersion || 0),
    itemCount: Number(node.itemCount || 0),
    depth,
    children: normalizeFolders(node.children, depth + 1)
  }))
}

function parentOptionNodes(nodes) {
  return nodes.map(node => ({
    folderId: node.folderId,
    folderName: node.folderName,
    disabled: node.depth >= 8,
    children: parentOptionNodes(node.children || [])
  }))
}

function folderOptionNodes(nodes) {
  return nodes.map(node => ({
    folderId: node.folderId,
    folderName: node.folderName,
    children: folderOptionNodes(node.children || [])
  }))
}

function walkFolders(nodes, visitor) {
  for (const node of nodes || []) {
    visitor(node)
    walkFolders(node.children, visitor)
  }
}

function folderLabel(folderId) {
  if (folderId == null) return '未分类'
  return folderById.value.get(Number(folderId))?.folderName || '未分类'
}

function statusLabel(status) {
  return statusLabels[status] || status || '-'
}

function statusType(status) {
  if (['ACTIVE', 'NORMAL', 'READY'].includes(status)) return 'success'
  if (['FAILED', 'DISABLED'].includes(status)) return 'danger'
  if (['CREATING', 'IMPORTING', 'QUEUED'].includes(status)) return 'warning'
  return 'info'
}

function formatCount(value) {
  const count = Number(value)
  return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '0'
}

async function loadFolderTree() {
  if (!canListFolders) return
  folderLoading.value = true
  try {
    const response = await listFolderTree()
    folderTree.value = normalizeFolders(response?.data ?? response ?? [])
    await nextTick()
    if (activeCustomFolderId.value && folderById.value.has(activeCustomFolderId.value)) {
      folderTreeRef.value?.setCurrentKey(activeCustomFolderId.value)
    } else if (activeCustomFolderId.value) {
      applyFolderScope('ALL')
    }
  } finally {
    folderLoading.value = false
  }
}

async function getList() {
  loading.value = true
  try {
    const response = await listDataset({ ...queryParams })
    datasetList.value = response.rows || []
    total.value = Number(response.total) || 0
  } finally {
    loading.value = false
  }
}

async function refreshAll() {
  const tasks = [getList()]
  if (canListFolders) tasks.push(loadFolderTree())
  await Promise.allSettled(tasks)
}

function applyFolderScope(scope, folderId) {
  activeFolderKey.value = scope === 'FOLDER' ? `FOLDER:${folderId}` : scope
  queryParams.folderScope = scope
  queryParams.folderId = scope === 'FOLDER' ? Number(folderId) : undefined
  queryParams.pageNum = 1
}

function selectVirtualFolder(item) {
  applyFolderScope(item.scope)
  folderTreeRef.value?.setCurrentKey(null)
  folderDrawerOpen.value = false
  getList()
}

function selectCustomFolder(folder) {
  applyFolderScope('FOLDER', folder.folderId)
  folderDrawerOpen.value = false
  getList()
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function openCreateFolder(parent) {
  folderDrawerOpen.value = false
  Object.assign(folderDialog, {
    visible: true,
    mode: 'create',
    folderId: undefined,
    parentFolderId: parent?.folderId ?? 0,
    folderName: '',
    lockVersion: undefined
  })
  nextTick(() => folderFormRef.value?.clearValidate())
}

function openRenameFolder(folder) {
  folderDrawerOpen.value = false
  Object.assign(folderDialog, {
    visible: true,
    mode: 'rename',
    folderId: folder.folderId,
    parentFolderId: folder.parentFolderId || 0,
    folderName: folder.folderName,
    lockVersion: folder.lockVersion
  })
  nextTick(() => folderFormRef.value?.clearValidate())
}

async function submitFolder() {
  if (!folderFormRef.value || folderSaving.value) return
  const valid = await folderFormRef.value.validate().catch(() => false)
  if (!valid) return
  folderSaving.value = true
  try {
    const payload = {
      parentFolderId: Number(folderDialog.parentFolderId || 0),
      folderName: folderDialog.folderName.trim()
    }
    if (folderDialog.mode === 'create') {
      await createFolder(payload)
      proxy.$modal.msgSuccess('文件夹创建成功')
    } else {
      payload.lockVersion = folderDialog.lockVersion
      await updateFolder(folderDialog.folderId, payload)
      proxy.$modal.msgSuccess('文件夹已重命名')
    }
    folderDialog.visible = false
    await loadFolderTree()
  } catch (error) {
    await loadFolderTree().catch(() => {})
    const refreshed = folderById.value.get(Number(folderDialog.folderId))
    if (refreshed) folderDialog.lockVersion = refreshed.lockVersion
  } finally {
    folderSaving.value = false
  }
}

async function removeFolder(folder) {
  if ((folder.children || []).length || Number(folder.itemCount) > 0) {
    proxy.$modal.msgWarning('只能删除不含子目录和数据表的空目录')
    return
  }
  try {
    await proxy.$modal.confirm(`确认删除空目录“${folder.folderName}”吗？`)
  } catch (error) {
    return
  }
  try {
    await deleteFolder(folder.folderId, folder.lockVersion)
    const removedActiveFolder = activeCustomFolderId.value === Number(folder.folderId)
    if (removedActiveFolder) applyFolderScope('ALL')
    await loadFolderTree()
    await getList()
    proxy.$modal.msgSuccess('文件夹已删除')
  } catch (error) {
    await loadFolderTree().catch(() => {})
  }
}

function handleFolderCommand(command, folder) {
  if (command === 'add') openCreateFolder(folder)
  else if (command === 'rename') openRenameFolder(folder)
  else if (command === 'delete') removeFolder(folder)
}

function openMoveDataset(row) {
  Object.assign(moveDialog, {
    visible: true,
    datasetId: row.datasetId,
    datasetName: row.displayName || row.datasetCode || '未命名数据表',
    folderId: Number(row.folderId || 0),
    originalFolderId: Number(row.folderId || 0),
    itemVersion: row.folderItemVersion ?? undefined
  })
}

async function submitMoveDataset() {
  if (moveTargetUnchanged.value) return
  moveSaving.value = true
  try {
    await moveDatasetToFolder(moveDialog.datasetId, {
      folderId: Number(moveDialog.folderId || 0) || null,
      itemVersion: moveDialog.itemVersion ?? null
    })
    moveDialog.visible = false
    await Promise.all([loadFolderTree(), getList()])
    proxy.$modal.msgSuccess('数据表已移动')
  } catch (error) {
    moveDialog.visible = false
    await Promise.allSettled([loadFolderTree(), getList()])
  } finally {
    moveSaving.value = false
  }
}

function handleCreate() {
  const params = activeCustomFolderId.value ? { folderId: String(activeCustomFolderId.value) } : undefined
  proxy.$tab.openPage('创建数据表', '/datahub/dataset-create/index', params)
}

function handleDetail(row) {
  proxy.$tab.openPage(row.displayName || '数据表详情', `/datahub/dataset-detail/index/${row.datasetId}`)
}

async function initialize() {
  if (canListFolders) await loadFolderTree().catch(() => {})
  await getList().catch(() => {})
}

initialize()
</script>

<style scoped>
.datahub-list-page {
  min-width: 0;
}

.dataset-layout {
  display: grid;
  grid-template-columns: 248px minmax(0, 1fr);
  min-height: calc(100vh - 150px);
}

.dataset-layout.without-folder-panel {
  grid-template-columns: minmax(0, 1fr);
}

.folder-sidebar {
  min-width: 0;
  padding-right: 16px;
  border-right: 1px solid #e4e7ed;
}

.dataset-workspace {
  min-width: 0;
  padding-left: 20px;
}

.without-folder-panel .dataset-workspace {
  padding-left: 0;
}

.folder-panel {
  width: 100%;
  min-width: 0;
}

.folder-panel-header,
.dataset-scope-header,
.dataset-scope-title,
.folder-header-actions {
  display: flex;
  align-items: center;
}

.folder-panel-header {
  height: 40px;
  justify-content: space-between;
  padding: 0 4px 0 10px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.folder-header-actions {
  gap: 2px;
}

.virtual-folder-list {
  display: grid;
  gap: 2px;
  margin-top: 6px;
}

.folder-nav-button {
  display: grid;
  grid-template-columns: 22px minmax(0, 1fr);
  align-items: center;
  width: 100%;
  height: 36px;
  padding: 0 10px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: #606266;
  font: inherit;
  text-align: left;
  cursor: pointer;
}

.folder-nav-button:hover {
  background: #f2f3f5;
  color: #303133;
}

.folder-nav-button.active {
  background: #ecf5ff;
  color: #337ecc;
  font-weight: 600;
}

.folder-nav-button span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.custom-folder-heading {
  margin: 18px 10px 8px;
  color: #909399;
  font-size: 12px;
}

.folder-tree {
  background: transparent;
}

.folder-tree :deep(.el-tree-node__content) {
  height: 36px;
  border-radius: 4px;
  padding-right: 2px;
}

.folder-tree :deep(.el-tree-node__content:hover) {
  background: #f2f3f5;
}

.folder-tree :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: #ecf5ff;
  color: #337ecc;
}

.folder-node {
  display: flex;
  align-items: center;
  min-width: 0;
  width: 100%;
  gap: 6px;
}

.folder-node-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.folder-count {
  min-width: 18px;
  color: #909399;
  font-size: 11px;
  text-align: right;
}

.folder-more {
  width: 28px;
  height: 28px;
  opacity: 0;
}

.folder-node:hover .folder-more,
.folder-more:focus-visible {
  opacity: 1;
}

.dataset-scope-header {
  min-height: 40px;
  margin-bottom: 12px;
  justify-content: space-between;
}

.dataset-scope-title {
  min-width: 0;
  gap: 10px;
}

.dataset-scope-title h2 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  line-height: 24px;
  letter-spacing: 0;
}

.dataset-scope-title span {
  display: block;
  margin-top: 2px;
  color: #909399;
  font-size: 12px;
}

.dataset-search-form {
  margin-bottom: 2px;
}

.datahub-list-page :deep(.dataset-name) {
  max-width: 100%;
  justify-content: flex-start;
}

.datahub-list-page :deep(.dataset-name > span) {
  overflow: hidden;
  text-overflow: ellipsis;
}

.mono {
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 12px;
}

.move-dataset-name {
  display: block;
  max-width: 100%;
  overflow: hidden;
  color: #303133;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-folder-panel {
  padding-bottom: 20px;
}

:global(.datahub-folder-drawer .el-drawer__body) {
  padding: 8px 14px 20px;
}

@media (max-width: 899px) {
  .dataset-layout {
    display: block;
    min-height: 0;
  }

  .dataset-workspace {
    padding-left: 0;
  }

  .dataset-scope-header {
    margin-bottom: 10px;
  }

  .mobile-folder-button {
    flex: none;
  }

  .folder-more {
    opacity: 1;
  }
}
</style>
