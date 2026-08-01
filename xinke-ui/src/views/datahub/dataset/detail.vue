<template>
  <div class="app-container datahub-detail-page">
    <el-skeleton v-if="pageLoading" :rows="8" animated />

    <el-empty v-else-if="loadFailed" description="数据表加载失败">
      <el-button type="primary" icon="Refresh" @click="loadDetail">重新加载</el-button>
      <el-button @click="handleBack">返回列表</el-button>
    </el-empty>

    <template v-else>
      <div class="page-toolbar">
        <el-button circle icon="ArrowLeft" aria-label="返回" @click="handleBack" />
        <div class="page-heading">
          <div class="heading-line">
            <h2>{{ dataset.displayName || '数据表详情' }}</h2>
            <el-tag :type="datasetStatusType(dataset.status)" effect="light">{{ datasetStatusLabel(dataset.status) }}</el-tag>
          </div>
          <span class="mono">{{ dataset.datasetCode || '-' }}</span>
        </div>
        <div class="mutation-toolbar">
          <el-tooltip v-if="showAppendAction" :disabled="!writeActionDisabled" :content="writeDisabledReason" placement="bottom">
            <span>
              <el-button type="primary" icon="Upload" :disabled="writeActionDisabled" @click="openImportWizard('APPEND')">追加数据</el-button>
            </span>
          </el-tooltip>
          <el-tooltip v-if="showEditAction" :disabled="!datasetBusy || editMode" :content="busyReason" placement="bottom">
            <span>
              <el-button
                :type="editMode ? 'warning' : 'default'"
                :icon="editMode ? 'Close' : 'Edit'"
                :disabled="datasetBusy && !editMode"
                @click="toggleEditMode"
              >{{ editMode ? '退出编辑' : '编辑数据' }}</el-button>
            </span>
          </el-tooltip>
          <el-dropdown v-if="showManageActions" trigger="click" @command="handleMutationCommand">
            <el-button icon="MoreFilled">更多操作<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canReplace" command="replace" icon="Refresh" :disabled="writeActionDisabled">覆盖数据</el-dropdown-item>
                <el-dropdown-item v-if="canRollback" command="rollback" icon="RefreshLeft" :disabled="writeActionDisabled">回滚版本</el-dropdown-item>
                <el-dropdown-item v-if="canClear" command="clear" icon="Delete" divided :disabled="writeActionDisabled || Number(dataset.rowCount) === 0">清空数据</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <el-button class="refresh-button" circle icon="Refresh" aria-label="刷新" @click="refreshCurrentTab" />
      </div>

      <el-alert
        v-if="datasetBusy"
        :title="busyReason"
        description="当前写任务完成前不能追加、覆盖、清空、回滚或编辑行数据。可在“导入记录”中查看进度。"
        type="warning"
        :closable="false"
        show-icon
        class="dataset-busy-alert"
      />

      <div class="dataset-summary">
        <div class="summary-item">
          <span>数据量</span>
          <strong>{{ formatCount(dataset.rowCount) }}</strong>
        </div>
        <div class="summary-item">
          <span>字段数</span>
          <strong>{{ formatCount(dataset.columnCount) }}</strong>
        </div>
        <div class="summary-item">
          <span>当前版本</span>
          <strong>v{{ dataset.currentVersionNo || dataset.currentSchemaVersion || 1 }}</strong>
        </div>
        <div class="summary-item summary-wide">
          <span>来源</span>
          <strong :title="sourceText">{{ sourceText }}</strong>
        </div>
        <div class="summary-item">
          <span>创建人</span>
          <strong>{{ dataset.ownerUserName || '-' }}</strong>
        </div>
        <div class="summary-item">
          <span>创建时间</span>
          <strong>{{ parseTime(dataset.createTime) || '-' }}</strong>
        </div>
      </div>

      <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="handleTabChange">
        <el-tab-pane label="数据" name="data">
          <div v-if="editMode" class="edit-mode-bar">
            <div>
              <strong>编辑模式</strong>
              <span>基于版本 v{{ mutationBaseVersionNo }}，{{ pendingChanges.length }} 项变更待提交</span>
            </div>
            <div class="edit-mode-actions">
              <el-button icon="Plus" @click="openNewRow">新增行</el-button>
              <el-button icon="Delete" :disabled="!selectedDataRows.length" @click="queueSelectedDeletes">
                删除所选{{ selectedDataRows.length ? ` (${selectedDataRows.length})` : '' }}
              </el-button>
              <el-button type="primary" icon="List" :disabled="!pendingChanges.length" @click="openMutationReview">
                待提交清单{{ pendingChanges.length ? ` (${pendingChanges.length})` : '' }}
              </el-button>
              <el-button @click="toggleEditMode">取消编辑</el-button>
            </div>
          </div>

          <div class="data-filter-bar">
            <el-select
              v-model="filterEditor.columnId"
              placeholder="选择字段"
              filterable
              clearable
              style="width: 190px"
            >
              <el-option
                v-for="column in columns"
                :key="column.columnId"
                :label="column.displayName"
                :value="column.columnId"
              />
            </el-select>
            <el-select
              v-model="filterEditor.operator"
              placeholder="条件"
              :disabled="!filterEditor.columnId"
              style="width: 128px"
            >
              <el-option v-for="item in availableOperators" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
            <template v-if="operatorNeedsValue">
              <el-select
                v-if="selectedFilterColumn?.dataType === 'BOOLEAN'"
                v-model="filterEditor.value"
                placeholder="请选择"
                style="width: 130px"
              >
                <el-option label="是" value="true" />
                <el-option label="否" value="false" />
              </el-select>
              <el-date-picker
                v-else-if="selectedFilterColumn?.dataType === 'DATE'"
                v-model="filterEditor.value"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 160px"
              />
              <el-date-picker
                v-else-if="selectedFilterColumn?.dataType === 'DATETIME'"
                v-model="filterEditor.value"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                placeholder="选择时间"
                style="width: 190px"
              />
              <el-input
                v-else
                v-model="filterEditor.value"
                placeholder="筛选值"
                clearable
                style="width: 180px"
                @keyup.enter="addFilterAndQuery"
              />
              <template v-if="filterEditor.operator === 'BETWEEN'">
                <span class="range-separator">至</span>
                <el-date-picker
                  v-if="selectedFilterColumn?.dataType === 'DATE'"
                  v-model="filterEditor.valueTo"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="结束日期"
                  style="width: 160px"
                />
                <el-date-picker
                  v-else-if="selectedFilterColumn?.dataType === 'DATETIME'"
                  v-model="filterEditor.valueTo"
                  type="datetime"
                  value-format="YYYY-MM-DD HH:mm:ss"
                  placeholder="结束时间"
                  style="width: 190px"
                />
                <el-input v-else v-model="filterEditor.valueTo" placeholder="结束值" clearable style="width: 180px" />
              </template>
            </template>
            <el-button icon="Plus" :disabled="!filterEditor.columnId" @click="addFilter">添加条件</el-button>
            <el-button type="primary" icon="Search" @click="handleDataQuery">查询</el-button>
            <el-button icon="Refresh" @click="resetDataQuery">重置</el-button>
          </div>

          <div v-if="filters.length" class="active-filters">
            <el-tag v-for="(filter, index) in filters" :key="`${filter.columnId}-${index}`" closable @close="removeFilter(index)">
              {{ filterDescription(filter) }}
            </el-tag>
          </div>

          <el-row :gutter="10" class="mb8 table-tools-row">
            <div class="row-count">共 {{ formatCount(dataTotal) }} 行</div>
            <right-toolbar
              v-if="dataColumnsReady"
              :key="columnStorageKey"
              :search="false"
              :columns="dataColumns"
              :storage-key="columnStorageKey"
              @queryTable="queryData"
            />
          </el-row>

          <el-table
            ref="dataTableRef"
            v-loading="dataLoading"
            :data="displayDataRows"
            row-key="_id"
            empty-text="暂无数据"
            @sort-change="handleSortChange"
            @selection-change="handleDataSelectionChange"
            :row-class-name="dataRowClassName"
          >
            <el-table-column v-if="editMode" type="selection" width="46" fixed="left" :selectable="rowSelectable" />
            <el-table-column type="index" label="#" width="68" fixed="left" :index="rowIndex" />
            <el-table-column label="源行号" prop="_source_row_no" width="90" align="right" fixed="left" />
            <el-table-column v-if="editMode" label="待提交" width="92" align="center">
              <template #default="scope">
                <el-tag v-if="scope.row._pendingOperation" :type="pendingOperationType(scope.row._pendingOperation)" effect="light">
                  {{ pendingOperationLabel(scope.row._pendingOperation) }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column
              v-for="column in visibleDataColumns"
              :key="column.columnId"
              :column-key="String(column.columnId)"
              :label="column.displayName"
              :prop="column.physicalName"
              :min-width="dataColumnWidth(column)"
              sortable="custom"
            >
              <template #header>
                <div class="dynamic-column-header">
                  <span>{{ column.displayName }}</span>
                  <small>{{ column.dataType }}</small>
                </div>
              </template>
              <template #default="scope">
                <span :class="{
                  'null-value': isNullValue(scope.row[column.physicalName]),
                  'empty-string-value': scope.row[column.physicalName] === ''
                }">
                  {{ formatDataCell(scope.row[column.physicalName], column.dataType) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column v-if="editMode" label="操作" width="82" align="center" fixed="right">
              <template #default="scope">
                <el-button
                  link
                  type="primary"
                  icon="Edit"
                  :disabled="scope.row._pendingOperation === 'DELETE'"
                  @click="openRowEditor(scope.row)"
                >编辑</el-button>
              </template>
            </el-table-column>
          </el-table>

          <pagination
            v-show="dataTotal > 0"
            :total="dataTotal"
            v-model:page="dataQuery.pageNum"
            v-model:limit="dataQuery.pageSize"
            @pagination="queryData"
          />
        </el-tab-pane>

        <el-tab-pane label="字段" name="schema">
          <el-table :data="columns" row-key="columnId" empty-text="暂无字段">
            <el-table-column label="#" prop="ordinalPosition" width="68" align="center" />
            <el-table-column label="显示名称" prop="displayName" min-width="150" fixed="left" />
            <el-table-column label="英文字段名" prop="physicalName" min-width="180">
              <template #default="scope"><span class="mono">{{ scope.row.physicalName }}</span></template>
            </el-table-column>
            <el-table-column label="原始列名" prop="sourceName" min-width="150" show-overflow-tooltip />
            <el-table-column label="字段类型" width="150">
              <template #default="scope"><el-tag effect="plain">{{ columnTypeText(scope.row) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="允许为空" width="95" align="center">
              <template #default="scope">
                <el-icon :color="scope.row.nullable ? '#67c23a' : '#909399'">
                  <CircleCheck v-if="scope.row.nullable" />
                  <CircleClose v-else />
                </el-icon>
              </template>
            </el-table-column>
            <el-table-column label="翻译来源" prop="translationSource" width="120" />
            <el-table-column label="示例值" min-width="220">
              <template #default="scope">{{ schemaSamples(scope.row.samplesJson) }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="版本" name="versions">
          <el-row :gutter="10" class="mb8 version-tools-row">
            <div class="version-summary">当前版本：v{{ dataset.currentVersionNo || dataset.currentSchemaVersion || 1 }}</div>
            <el-button circle icon="Refresh" aria-label="刷新版本" :loading="versionsLoading" @click="loadVersions(true)" />
          </el-row>
          <el-table v-loading="versionsLoading" :data="versions" empty-text="暂无版本记录" row-key="versionId">
            <el-table-column label="版本" width="100" fixed="left">
              <template #default="scope">
                <strong>v{{ scope.row.versionNo }}</strong>
                <el-tag v-if="isCurrentVersion(scope.row)" type="success" size="small" class="current-version-tag">当前</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="类型" prop="versionType" width="110">
              <template #default="scope">{{ operationLabel(scope.row.versionType) }}</template>
            </el-table-column>
            <el-table-column label="数据量" prop="rowCount" width="110" align="right">
              <template #default="scope">{{ formatCount(scope.row.rowCount) }}</template>
            </el-table-column>
            <el-table-column label="状态" prop="status" width="110">
              <template #default="scope"><el-tag :type="versionStatusType(scope.row.status)" effect="light">{{ versionStatusLabel(scope.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="来源任务" prop="jobId" min-width="130" show-overflow-tooltip>
              <template #default="scope">{{ scope.row.jobId ? `#${scope.row.jobId}` : '-' }}</template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" width="170">
              <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
            </el-table-column>
            <el-table-column v-if="canRollback" label="操作" width="100" align="center" fixed="right">
              <template #default="scope">
                <el-button
                  link
                  type="primary"
                  icon="RefreshLeft"
                  :disabled="datasetBusy || isCurrentVersion(scope.row) || !rollbackableVersion(scope.row)"
                  @click="openRollbackDialog(scope.row)"
                >回滚</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="导入记录" name="jobs">
          <el-table v-loading="jobsLoading" :data="jobs" empty-text="暂无导入记录">
            <el-table-column label="操作" prop="operationType" width="100">
              <template #default="scope">{{ operationLabel(scope.row.operationType) }}</template>
            </el-table-column>
            <el-table-column label="文件" prop="fileName" min-width="190" show-overflow-tooltip />
            <el-table-column label="Sheet" prop="sheetName" min-width="120" show-overflow-tooltip />
            <el-table-column label="状态" prop="status" width="125">
              <template #default="scope"><el-tag :type="jobStatusType(scope.row.status)">{{ jobStatusLabel(scope.row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="进度" min-width="160">
              <template #default="scope">
                <el-progress :percentage="jobRowProgress(scope.row)" :status="jobRowProgressStatus(scope.row)" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column label="总行数" prop="totalRows" width="90" align="right" />
            <el-table-column label="成功" prop="successRows" width="80" align="right" />
            <el-table-column label="失败" prop="failedRows" width="80" align="right" />
            <el-table-column label="错误信息" prop="errorMessage" min-width="220" show-overflow-tooltip />
            <el-table-column label="提交时间" prop="createTime" width="170">
              <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
            </el-table-column>
            <el-table-column label="完成时间" prop="finishTime" width="170">
              <template #default="scope">{{ parseTime(scope.row.finishTime) || '-' }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="canGrant" label="权限" name="acl">
          <el-row :gutter="10" class="mb8">
            <el-col :span="1.5">
              <el-button type="primary" plain icon="Plus" @click="openAclDialog">添加授权</el-button>
            </el-col>
            <el-col :span="1.5">
              <el-button type="success" plain icon="Check" :loading="aclSaving" @click="saveAcl">保存权限</el-button>
            </el-col>
          </el-row>

          <el-table v-loading="aclLoading" :data="aclEntries" empty-text="暂无额外授权">
            <el-table-column label="对象类型" prop="subjectType" width="110">
              <template #default="scope">
                <el-tag :type="scope.row.subjectType === 'ROLE' ? 'warning' : 'info'">
                  {{ scope.row.subjectType === 'ROLE' ? '角色' : '用户' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="授权对象" prop="subjectName" min-width="180" />
            <el-table-column label="查看" width="110" align="center">
              <template #default="scope">
                <el-checkbox :model-value="hasMask(scope.row.permissionMask, ACCESS_READ)" @change="toggleAclMask(scope.row, ACCESS_READ, $event)" />
              </template>
            </el-table-column>
            <el-table-column label="导入" width="110" align="center">
              <template #default="scope">
                <el-checkbox :model-value="hasMask(scope.row.permissionMask, ACCESS_IMPORT)" @change="toggleAclMask(scope.row, ACCESS_IMPORT, $event)" />
              </template>
            </el-table-column>
            <el-table-column label="编辑" width="110" align="center">
              <template #default="scope">
                <el-checkbox :model-value="hasMask(scope.row.permissionMask, ACCESS_EDIT)" @change="toggleAclMask(scope.row, ACCESS_EDIT, $event)" />
              </template>
            </el-table-column>
            <el-table-column label="管理" width="110" align="center">
              <template #default="scope">
                <el-checkbox :model-value="hasMask(scope.row.permissionMask, ACCESS_MANAGE)" @change="toggleAclMask(scope.row, ACCESS_MANAGE, $event)" />
              </template>
            </el-table-column>
            <el-table-column label="授权时间" prop="createTime" width="170">
              <template #default="scope">{{ parseTime(scope.row.createTime) || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center" fixed="right">
              <template #default="scope">
                <el-button link type="danger" icon="Delete" @click="removeAcl(scope.$index)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </template>

    <el-dialog v-model="aclDialogOpen" title="添加数据表授权" width="520px" append-to-body @closed="resetAclDialog">
      <el-form label-width="88px">
        <el-form-item label="对象类型">
          <el-radio-group v-model="aclForm.subjectType" @change="handleSubjectTypeChange">
            <el-radio-button value="USER">用户</el-radio-button>
            <el-radio-button value="ROLE">角色</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="授权对象" required>
          <el-select
            v-model="aclForm.subjectId"
            filterable
            placeholder="请选择授权对象"
            :loading="accessOptionsLoading"
            style="width: 100%"
          >
            <el-option v-for="item in currentSubjectOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="权限" required>
          <el-checkbox-group v-model="aclForm.permissions" @change="normalizeAclPermissions">
            <el-checkbox :value="ACCESS_READ">查看</el-checkbox>
            <el-checkbox :value="ACCESS_IMPORT">导入</el-checkbox>
            <el-checkbox :value="ACCESS_EDIT">编辑</el-checkbox>
            <el-checkbox :value="ACCESS_MANAGE">管理</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aclDialogOpen = false">取消</el-button>
        <el-button type="primary" @click="addAclEntry">确定</el-button>
      </template>
    </el-dialog>

    <DataRowMutationDrawer
      v-model="rowEditorOpen"
      :mode="rowEditorMode"
      :columns="columns"
      :row="rowEditorRow"
      @save="queueRowDraft"
    />

    <el-dialog v-model="mutationReviewOpen" title="待提交变更" width="min(920px, calc(100vw - 24px))" append-to-body :close-on-click-modal="false">
      <el-alert
        :title="`全部变更将基于数据版本 v${mutationBaseVersionNo} 一次提交`"
        description="提交期间若数据版本或行内容已经变化，服务端会拒绝本批变更，不会静默覆盖。"
        type="info"
        :closable="false"
        show-icon
        class="review-alert"
      />
      <el-table :data="pendingChanges" row-key="clientMutationId" max-height="430" empty-text="暂无待提交变更">
        <el-table-column label="操作" width="90">
          <template #default="scope"><el-tag :type="pendingOperationType(scope.row.operation)">{{ pendingOperationLabel(scope.row.operation) }}</el-tag></template>
        </el-table-column>
        <el-table-column label="数据行" min-width="130">
          <template #default="scope">{{ pendingRowText(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="变更摘要" min-width="360" show-overflow-tooltip>
          <template #default="scope">{{ pendingChangeSummary(scope.row) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="scope">
            <el-button v-if="scope.row.operation !== 'DELETE'" link type="primary" icon="Edit" @click="editPendingChange(scope.row)">修改</el-button>
            <el-button link type="danger" icon="Delete" @click="removePendingChange(scope.row.clientMutationId)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <div class="review-footer">
          <span>新增 {{ pendingStats.INSERT }} · 修改 {{ pendingStats.UPDATE }} · 删除 {{ pendingStats.DELETE }}</span>
          <div>
            <el-button @click="mutationReviewOpen = false">返回编辑</el-button>
            <el-button type="primary" icon="Check" :loading="mutationSubmitting" :disabled="!pendingChanges.length" @click="submitMutations">提交全部变更</el-button>
          </div>
        </div>
      </template>
    </el-dialog>

    <DataImportWizard
      v-model="importWizardOpen"
      :dataset-id="datasetId"
      :operation="importOperation"
      :dataset="dataset"
      :columns="columns"
      @submitted="handleMutationSubmitted"
      @completed="handleImportCompleted"
      @conflict="handleVersionConflict"
    />

    <el-dialog v-model="clearDialogOpen" title="清空数据" width="min(560px, calc(100vw - 24px))" append-to-body :close-on-click-modal="false" @closed="clearConfirmationName = ''">
      <el-alert
        title="清空会创建一个空数据版本，不改变字段结构"
        description="服务端发布新版本前，当前数据保持可用；是否可回滚取决于旧版本保留策略。"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="danger-impact">
        <span>数据表</span><strong>{{ dataset.displayName }}</strong>
        <span>当前版本</span><strong>v{{ dataset.currentVersionNo || dataset.currentSchemaVersion || 1 }}</strong>
        <span>将清空</span><strong>{{ formatCount(dataset.rowCount) }} 行</strong>
      </div>
      <el-form label-position="top">
        <el-form-item :label="`请输入数据表名称“${dataset.displayName}”确认清空`">
          <el-input v-model="clearConfirmationName" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearDialogOpen = false">取消</el-button>
        <el-button type="danger" icon="Delete" :loading="clearSubmitting" :disabled="clearConfirmationName !== dataset.displayName" @click="submitClear">清空数据</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="rollbackDialogOpen" title="回滚数据版本" width="min(560px, calc(100vw - 24px))" append-to-body :close-on-click-modal="false" @closed="resetRollbackDialog">
      <el-alert
        title="回滚会把目标版本切换为当前可见版本"
        description="回滚不会修改目标版本中的行数据；提交前会再次校验当前版本，避免覆盖其他写任务。"
        type="warning"
        :closable="false"
        show-icon
      />
      <div class="danger-impact">
        <span>当前版本</span><strong>v{{ dataset.currentVersionNo || dataset.currentSchemaVersion || 1 }}</strong>
        <span>目标版本</span><strong>v{{ rollbackTarget?.versionNo || '-' }}</strong>
        <span>目标数据量</span><strong>{{ formatCount(rollbackTarget?.rowCount) }} 行</strong>
      </div>
      <el-form label-position="top">
        <el-form-item :label="`请输入数据表名称“${dataset.displayName}”确认回滚`">
          <el-input v-model="rollbackConfirmationName" autocomplete="off" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rollbackDialogOpen = false">取消</el-button>
        <el-button type="danger" icon="RefreshLeft" :loading="rollbackSubmitting" :disabled="rollbackConfirmationName !== dataset.displayName" @click="submitRollback">确认回滚</el-button>
      </template>
    </el-dialog>

    <DataHubJobStatusDialog
      v-model="maintenanceJobOpen"
      :title="maintenanceJobTitle"
      :success-title="maintenanceSuccessTitle"
      :initial-job="maintenanceJob"
      @completed="handleMaintenanceCompleted"
    />
  </div>
</template>

<script setup name="DataHubDatasetDetail">
import {
  getDataset,
  getDatasetAcl,
  listAccessRoles,
  listAccessUsers,
  listDatasetJobs,
  queryDatasetData,
  updateDatasetAcl
} from '@/api/datahub/dataset'
import {
  clearDataset,
  commitDatasetEdit,
  listDatasetVersions,
  rollbackDatasetVersion
} from '@/api/datahub/mutation'
import { checkPermi } from '@/utils/permission'
import DataHubJobStatusDialog from './components/DataHubJobStatusDialog.vue'
import DataImportWizard from './components/DataImportWizard.vue'
import DataRowMutationDrawer from './components/DataRowMutationDrawer.vue'

const ACCESS_READ = 1
const ACCESS_IMPORT = 2
const ACCESS_MANAGE = 4
const ACCESS_EDIT = 8

const route = useRoute()
const { proxy } = getCurrentInstance()
const datasetId = computed(() => route.params.datasetId)
const pageLoading = ref(true)
const loadFailed = ref(false)
const activeTab = ref('data')
const dataset = reactive({})
const columns = ref([])
const access = reactive({ accessMask: 0, canRead: false, canImport: false, canManage: false, canEdit: false })

const dataLoading = ref(false)
const dataRows = ref([])
const dataTotal = ref(0)
const dataColumns = ref([])
const dataColumnsReady = ref(false)
const dataTableRef = ref(null)
const filters = ref([])
const filterEditor = reactive({ columnId: undefined, operator: 'CONTAINS', value: '', valueTo: '' })
const dataQuery = reactive({ pageNum: 1, pageSize: 20, sortColumnId: undefined, sortDirection: 'DESC' })

const jobsLoading = ref(false)
const jobsLoaded = ref(false)
const jobs = ref([])

const versionsLoading = ref(false)
const versionsLoaded = ref(false)
const versions = ref([])

const editMode = ref(false)
const mutationBaseVersionId = ref(undefined)
const mutationBaseVersionNo = ref(1)
const pendingChanges = ref([])
const selectedDataRows = ref([])
const rowEditorOpen = ref(false)
const rowEditorMode = ref('UPDATE')
const rowEditorRow = ref({})
const editingMutationId = ref(undefined)
const mutationReviewOpen = ref(false)
const mutationSubmitting = ref(false)
let mutationSequence = 0

const importWizardOpen = ref(false)
const importOperation = ref('APPEND')
const clearDialogOpen = ref(false)
const clearConfirmationName = ref('')
const clearSubmitting = ref(false)
const rollbackDialogOpen = ref(false)
const rollbackTarget = ref(null)
const rollbackConfirmationName = ref('')
const rollbackSubmitting = ref(false)
const maintenanceJobOpen = ref(false)
const maintenanceJob = ref({})
const maintenanceJobTitle = ref('数据任务')
const maintenanceSuccessTitle = ref('操作成功')

const aclLoading = ref(false)
const aclSaving = ref(false)
const aclLoaded = ref(false)
const aclEntries = ref([])
const aclDialogOpen = ref(false)
const accessOptionsLoading = ref(false)
const userOptions = ref([])
const roleOptions = ref([])
const aclForm = reactive({ subjectType: 'USER', subjectId: undefined, permissions: [ACCESS_READ] })

const sourceText = computed(() => {
  const source = [dataset.sourceFileName, dataset.sourceSheetName].filter(Boolean)
  return source.length ? source.join(' / ') : '-'
})

const canImport = computed(() => Boolean(access.canImport) || hasMask(access.accessMask, ACCESS_IMPORT))
const canManage = computed(() => Boolean(access.canManage) || hasMask(access.accessMask, ACCESS_MANAGE))
const canEdit = computed(() => Boolean(access.canEdit) || hasMask(access.accessMask, ACCESS_EDIT))
const canGrant = computed(() => canManage.value && checkPermi(['datahub:dataset:grant']))
const canAppend = computed(() => canImport.value && checkPermi(['datahub:dataset:append']))
const canReplace = computed(() => canManage.value && checkPermi(['datahub:dataset:replace']))
const canClear = computed(() => canManage.value && checkPermi(['datahub:dataset:clear']))
const canRollback = computed(() => canManage.value && checkPermi(['datahub:dataset:rollback']))
const canEditRows = computed(() => canEdit.value && checkPermi(['datahub:dataset:edit']))
const showAppendAction = computed(() => checkPermi(['datahub:dataset:append']) && canImport.value)
const showEditAction = computed(() => checkPermi(['datahub:dataset:edit']) && canEdit.value)
const showManageActions = computed(() => canReplace.value || canClear.value || canRollback.value)
const datasetBusy = computed(() => Boolean(dataset.activeJobId) || ['BUILDING', 'CREATING', 'IMPORTING'].includes(dataset.status))
const busyReason = computed(() => dataset.activeJobId ? `数据表存在执行中的写任务 #${dataset.activeJobId}` : '数据表正在执行写操作')
const writeActionDisabled = computed(() => datasetBusy.value || editMode.value)
const writeDisabledReason = computed(() => editMode.value ? '请先提交或退出编辑模式' : busyReason.value)
const visibleDataColumns = computed(() => dataColumns.value.filter(column => column.visible !== false))
const columnStorageKey = computed(() => `datahub-columns-${datasetId.value}-${dataset.currentSchemaVersion || 1}`)
const selectedFilterColumn = computed(() => columns.value.find(column => String(column.columnId) === String(filterEditor.columnId)))
const currentSubjectOptions = computed(() => aclForm.subjectType === 'ROLE' ? roleOptions.value : userOptions.value)
const pendingStats = computed(() => pendingChanges.value.reduce((stats, change) => {
  if (Object.prototype.hasOwnProperty.call(stats, change.operation)) stats[change.operation] += 1
  return stats
}, { INSERT: 0, UPDATE: 0, DELETE: 0 }))
const displayDataRows = computed(() => {
  const byRowId = new Map(pendingChanges.value
    .filter(change => change.rowId != null)
    .map(change => [String(change.rowId), change]))
  return dataRows.value.map(row => {
    const change = byRowId.get(String(row._id))
    if (!change) return row
    const next = { ...row, _pendingOperation: change.operation }
    if (change.operation === 'UPDATE') Object.assign(next, mutationDisplayValues(change.values))
    return next
  })
})

const allOperators = {
  EQ: '等于',
  NE: '不等于',
  CONTAINS: '包含',
  GT: '大于',
  GTE: '大于等于',
  LT: '小于',
  LTE: '小于等于',
  BETWEEN: '区间',
  IS_NULL: '为空',
  IS_NOT_NULL: '不为空'
}

const availableOperators = computed(() => {
  const type = selectedFilterColumn.value?.dataType
  let values
  if (['VARCHAR', 'TEXT'].includes(type)) values = ['CONTAINS', 'EQ', 'NE', 'IS_NULL', 'IS_NOT_NULL']
  else if (type === 'BOOLEAN') values = ['EQ', 'NE', 'IS_NULL', 'IS_NOT_NULL']
  else values = ['EQ', 'NE', 'GT', 'GTE', 'LT', 'LTE', 'BETWEEN', 'IS_NULL', 'IS_NOT_NULL']
  return values.map(value => ({ value, label: allOperators[value] }))
})

const operatorNeedsValue = computed(() => !['IS_NULL', 'IS_NOT_NULL'].includes(filterEditor.operator))

watch(() => filterEditor.columnId, () => {
  filterEditor.operator = ['VARCHAR', 'TEXT'].includes(selectedFilterColumn.value?.dataType) ? 'CONTAINS' : 'EQ'
  filterEditor.value = ''
  filterEditor.valueTo = ''
})

watch(() => filterEditor.operator, operator => {
  if (operator !== 'BETWEEN') filterEditor.valueTo = ''
})

async function loadDetail() {
  pageLoading.value = true
  loadFailed.value = false
  try {
    const response = await getDataset(datasetId.value)
    const detail = response.data || {}
    Object.assign(dataset, detail.dataset || {})
    columns.value = [...(detail.columns || [])].sort((a, b) => (a.ordinalPosition || 0) - (b.ordinalPosition || 0))
    Object.assign(access, { accessMask: 0, canRead: false, canImport: false, canManage: false, canEdit: false }, detail.access || {})
    dataColumnsReady.value = false
    dataColumns.value = columns.value.map((column, index) => ({ ...column, key: index, visible: true }))
    await nextTick()
    dataColumnsReady.value = true
    await queryData().catch(() => {})
  } catch (error) {
    loadFailed.value = true
  } finally {
    pageLoading.value = false
  }
}

function normalizePayload(response) {
  return response?.data ?? response ?? {}
}

async function queryData() {
  if (!datasetId.value || !columns.value.length) {
    dataRows.value = []
    dataTotal.value = 0
    return
  }
  dataLoading.value = true
  try {
    const response = await queryDatasetData(datasetId.value, {
      pageNum: dataQuery.pageNum,
      pageSize: dataQuery.pageSize,
      filters: filters.value.map(filter => ({
        columnId: filter.columnId,
        operator: filter.operator,
        value: filter.value,
        valueTo: filter.valueTo
      })),
      sortColumnId: dataQuery.sortColumnId,
      sortDirection: dataQuery.sortDirection
    })
    const page = normalizePayload(response)
    dataRows.value = page.rows || []
    dataTotal.value = Number(page.total) || 0
    selectedDataRows.value = []
    if (page.pageNum) dataQuery.pageNum = Number(page.pageNum)
    if (page.pageSize) dataQuery.pageSize = Number(page.pageSize)
  } finally {
    dataLoading.value = false
  }
}

function nextMutationId() {
  mutationSequence += 1
  return `mutation-${Date.now()}-${mutationSequence}`
}

function rowExpectedHash(row) {
  return row?._row_version ?? row?._row_hash ?? row?.rowHash ?? row?.rowVersion ?? null
}

function mutationDisplayValues(values) {
  const display = {}
  for (const item of values || []) {
    const column = columns.value.find(candidate => String(candidate.columnId) === String(item.columnId))
    if (column) display[column.physicalName] = item.isNull ? null : item.value
  }
  return display
}

function handleDataSelectionChange(rows) {
  selectedDataRows.value = rows || []
}

function rowSelectable(row) {
  return row._pendingOperation !== 'DELETE'
}

function pendingOperationLabel(operation) {
  return { INSERT: '新增', UPDATE: '修改', DELETE: '删除' }[operation] || operation || '-'
}

function pendingOperationType(operation) {
  return { INSERT: 'success', UPDATE: 'warning', DELETE: 'danger' }[operation] || 'info'
}

function dataRowClassName({ row }) {
  return row._pendingOperation ? `pending-row pending-row-${String(row._pendingOperation).toLowerCase()}` : ''
}

function pendingChangeForRow(rowId) {
  return pendingChanges.value.find(change => change.rowId != null && String(change.rowId) === String(rowId))
}

function openNewRow() {
  editingMutationId.value = undefined
  rowEditorMode.value = 'INSERT'
  rowEditorRow.value = {}
  rowEditorOpen.value = true
}

function openRowEditor(row) {
  if (row._pendingOperation === 'DELETE') return proxy.$modal.msgWarning('该行已经加入删除清单')
  const pending = pendingChangeForRow(row._id)
  editingMutationId.value = pending?.clientMutationId
  rowEditorMode.value = 'UPDATE'
  rowEditorRow.value = { ...row }
  rowEditorOpen.value = true
}

function queueRowDraft(draft) {
  const values = draft.values || []
  const existingIndex = editingMutationId.value
    ? pendingChanges.value.findIndex(change => change.clientMutationId === editingMutationId.value)
    : -1
  if (existingIndex >= 0) {
    const existing = pendingChanges.value[existingIndex]
    const nextValues = draft.operation === 'UPDATE'
      ? mergeMutationValues(existing.values, values)
      : values
    pendingChanges.value.splice(existingIndex, 1, { ...existing, values: nextValues })
  } else if (draft.operation === 'INSERT') {
    pendingChanges.value.push({
      clientMutationId: nextMutationId(),
      operation: 'INSERT',
      values
    })
  } else {
    pendingChanges.value.push({
      clientMutationId: nextMutationId(),
      operation: 'UPDATE',
      rowId: rowEditorRow.value._id,
      sourceRowNo: rowEditorRow.value._source_row_no,
      expectedRowHash: rowExpectedHash(rowEditorRow.value),
      values
    })
  }
  editingMutationId.value = undefined
  proxy.$modal.msgSuccess('已加入待提交清单')
}

function mergeMutationValues(currentValues = [], nextValues = []) {
  const values = new Map(currentValues.map(value => [String(value.columnId), value]))
  for (const value of nextValues) values.set(String(value.columnId), value)
  return [...values.values()]
}

function editPendingChange(change) {
  editingMutationId.value = change.clientMutationId
  rowEditorMode.value = change.operation
  if (change.operation === 'INSERT') {
    rowEditorRow.value = mutationDisplayValues(change.values)
  } else {
    const current = dataRows.value.find(row => String(row._id) === String(change.rowId)) || {}
    rowEditorRow.value = { ...current, ...mutationDisplayValues(change.values) }
  }
  rowEditorOpen.value = true
}

function removePendingChange(clientMutationId) {
  pendingChanges.value = pendingChanges.value.filter(change => change.clientMutationId !== clientMutationId)
  if (!pendingChanges.value.length) mutationReviewOpen.value = false
}

function queueSelectedDeletes() {
  const rows = selectedDataRows.value.filter(row => row?._id != null)
  if (!rows.length) return
  for (const row of rows) {
    const existingIndex = pendingChanges.value.findIndex(change => change.rowId != null && String(change.rowId) === String(row._id))
    const deletion = {
      clientMutationId: existingIndex >= 0 ? pendingChanges.value[existingIndex].clientMutationId : nextMutationId(),
      operation: 'DELETE',
      rowId: row._id,
      sourceRowNo: row._source_row_no,
      expectedRowHash: existingIndex >= 0 ? pendingChanges.value[existingIndex].expectedRowHash : rowExpectedHash(row),
      values: []
    }
    if (existingIndex >= 0) pendingChanges.value.splice(existingIndex, 1, deletion)
    else pendingChanges.value.push(deletion)
  }
  selectedDataRows.value = []
  dataTableRef.value?.clearSelection()
  proxy.$modal.msgSuccess(`已将 ${rows.length} 行加入删除清单`)
}

function openMutationReview() {
  if (!pendingChanges.value.length) return proxy.$modal.msgWarning('待提交清单为空')
  mutationReviewOpen.value = true
}

function pendingRowText(change) {
  if (change.operation === 'INSERT') return '新增行'
  return change.sourceRowNo ? `源行 ${change.sourceRowNo} · ID ${change.rowId}` : `行 ID ${change.rowId}`
}

function pendingChangeSummary(change) {
  if (change.operation === 'DELETE') return '删除整行数据'
  const parts = (change.values || []).slice(0, 4).map(item => {
    const column = columns.value.find(candidate => String(candidate.columnId) === String(item.columnId))
    const value = item.isNull ? 'NULL' : item.value === '' ? '空字符串' : String(item.value)
    return `${column?.displayName || item.columnId}=${value}`
  })
  const remaining = Math.max(0, (change.values || []).length - parts.length)
  return parts.join('，') + (remaining ? `，另 ${remaining} 个字段` : '')
}

async function submitMutations() {
  if (!pendingChanges.value.length) return
  mutationSubmitting.value = true
  try {
    const response = await commitDatasetEdit(datasetId.value, {
      baseVersionId: mutationBaseVersionId.value,
      mutations: pendingChanges.value.map(change => ({
        clientMutationId: change.clientMutationId,
        operation: change.operation,
        rowId: change.rowId,
        expectedRowHash: change.expectedRowHash,
        values: change.values || []
      }))
    })
    proxy.$modal.msgSuccess('数据变更已提交')
    mutationReviewOpen.value = false
    finishEditMode()
    await handleMaintenanceResponse(response, '提交行编辑', '数据修改成功')
  } catch (error) {
    handleVersionConflict(error)
  } finally {
    mutationSubmitting.value = false
  }
}

function toggleEditMode() {
  if (!editMode.value) {
    if (!canEditRows.value) return proxy.$modal.msgWarning('当前账号没有该数据表的编辑权限')
    if (datasetBusy.value) return proxy.$modal.msgWarning(busyReason.value)
    activeTab.value = 'data'
    mutationBaseVersionId.value = dataset.currentVersionId
    mutationBaseVersionNo.value = dataset.currentVersionNo || dataset.currentSchemaVersion || 1
    pendingChanges.value = []
    selectedDataRows.value = []
    editMode.value = true
    return
  }
  if (!pendingChanges.value.length) return finishEditMode()
  proxy.$modal.confirm(`有 ${pendingChanges.value.length} 项变更尚未提交，确认全部放弃吗？`).then(finishEditMode).catch(() => {})
}

function finishEditMode() {
  editMode.value = false
  pendingChanges.value = []
  selectedDataRows.value = []
  mutationBaseVersionId.value = undefined
  mutationReviewOpen.value = false
  rowEditorOpen.value = false
  dataTableRef.value?.clearSelection()
}

function isVersionConflict(error) {
  const status = error?.response?.status
  const code = error?.response?.data?.code || error?.code
  const message = String(error?.message || error?.response?.data?.msg || '')
  return Number(status) === 409 || Number(code) === 409
    || code === 'DATA_VERSION_CONFLICT' || code === 'ROW_VERSION_CONFLICT'
    || /版本.*(变化|冲突|过期)/.test(message)
}

function handleVersionConflict(error) {
  if (!isVersionConflict(error)) return
  proxy.$modal.confirm('当前数据版本或行内容已经变化。是否放弃本地待提交内容并重新加载？').then(async () => {
    importWizardOpen.value = false
    clearDialogOpen.value = false
    rollbackDialogOpen.value = false
    finishEditMode()
    await loadDetail()
    versionsLoaded.value = false
    jobsLoaded.value = false
  }).catch(() => {})
}

function validateFilterEditor() {
  if (!filterEditor.columnId) return '请选择筛选字段'
  if (operatorNeedsValue.value && (filterEditor.value === '' || filterEditor.value === null || filterEditor.value === undefined)) return '请输入筛选值'
  if (filterEditor.operator === 'BETWEEN' && (filterEditor.valueTo === '' || filterEditor.valueTo === null || filterEditor.valueTo === undefined)) return '请输入区间结束值'
  return ''
}

function addFilter() {
  const errorMessage = validateFilterEditor()
  if (errorMessage) return proxy.$modal.msgWarning(errorMessage)
  filters.value.push({
    columnId: filterEditor.columnId,
    columnName: selectedFilterColumn.value?.displayName || '',
    operator: filterEditor.operator,
    value: operatorNeedsValue.value ? String(filterEditor.value) : null,
    valueTo: filterEditor.operator === 'BETWEEN' ? String(filterEditor.valueTo) : null
  })
  filterEditor.columnId = undefined
  filterEditor.value = ''
  filterEditor.valueTo = ''
}

function addFilterAndQuery() {
  const count = filters.value.length
  addFilter()
  if (filters.value.length > count) handleDataQuery()
}

function removeFilter(index) {
  filters.value.splice(index, 1)
  handleDataQuery()
}

function handleDataQuery() {
  dataQuery.pageNum = 1
  queryData()
}

function resetDataQuery() {
  filters.value = []
  filterEditor.columnId = undefined
  filterEditor.operator = 'CONTAINS'
  filterEditor.value = ''
  filterEditor.valueTo = ''
  dataQuery.pageNum = 1
  dataQuery.sortColumnId = undefined
  dataQuery.sortDirection = 'DESC'
  dataTableRef.value?.clearSort()
  queryData()
}

function handleSortChange({ column, order }) {
  dataQuery.sortColumnId = order ? Number(column.columnKey) : undefined
  dataQuery.sortDirection = order === 'ascending' ? 'ASC' : 'DESC'
  dataQuery.pageNum = 1
  queryData()
}

function rowIndex(index) {
  return (dataQuery.pageNum - 1) * dataQuery.pageSize + index + 1
}

function filterDescription(filter) {
  const operator = allOperators[filter.operator] || filter.operator
  if (filter.operator === 'BETWEEN') return `${filter.columnName} ${operator} ${filter.value} 至 ${filter.valueTo}`
  if (['IS_NULL', 'IS_NOT_NULL'].includes(filter.operator)) return `${filter.columnName} ${operator}`
  return `${filter.columnName} ${operator} ${filter.value}`
}

function isNullValue(value) {
  return value === null || value === undefined
}

function formatDataCell(value, dataType) {
  if (isNullValue(value)) return 'NULL'
  if (value === '') return '""'
  if (dataType === 'BOOLEAN' || typeof value === 'boolean') {
    return value === true || value === 1 || value === '1' ? '是' : '否'
  }
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}

function dataColumnWidth(column) {
  const labelLength = String(column.displayName || '').length
  if (column.dataType === 'DATETIME') return Math.max(170, labelLength * 16 + 38)
  if (column.dataType === 'DATE') return Math.max(130, labelLength * 16 + 38)
  return Math.max(120, Math.min(280, labelLength * 16 + 52))
}

function columnTypeText(column) {
  if (column.dataType === 'VARCHAR') return `VARCHAR(${column.columnLength || 255})`
  if (column.dataType === 'DECIMAL') return `DECIMAL(${column.numericPrecision || 18},${column.numericScale || 0})`
  return column.dataType || '-'
}

function schemaSamples(samplesJson) {
  if (!samplesJson) return '-'
  try {
    const samples = typeof samplesJson === 'string' ? JSON.parse(samplesJson) : samplesJson
    return Array.isArray(samples) ? samples.slice(0, 4).join('、') || '-' : String(samplesJson)
  } catch (error) {
    return String(samplesJson)
  }
}

function openImportWizard(operation) {
  if (editMode.value) return proxy.$modal.msgWarning('请先提交或退出编辑模式')
  if (datasetBusy.value) return proxy.$modal.msgWarning(busyReason.value)
  importOperation.value = operation
  importWizardOpen.value = true
}

function handleMutationCommand(command) {
  if (editMode.value) return proxy.$modal.msgWarning('请先提交或退出编辑模式')
  if (datasetBusy.value) return proxy.$modal.msgWarning(busyReason.value)
  if (command === 'replace') return openImportWizard('REPLACE')
  if (command === 'clear') {
    clearConfirmationName.value = ''
    clearDialogOpen.value = true
    return
  }
  if (command === 'rollback') {
    activeTab.value = 'versions'
    loadVersions()
    proxy.$modal.msg('请在版本列表中选择需要回滚的版本')
  }
}

async function handleImportCompleted() {
  jobsLoaded.value = false
  versionsLoaded.value = false
  await loadDetail()
  if (activeTab.value === 'versions') await loadVersions(true)
}

async function handleMutationSubmitted() {
  jobsLoaded.value = false
  await loadDetail()
}

async function loadVersions(force = false) {
  if (versionsLoaded.value && !force) return
  versionsLoading.value = true
  try {
    const response = await listDatasetVersions(datasetId.value)
    versions.value = normalizeArrayResponse(response, ['versions', 'rows'])
      .sort((a, b) => Number(b.versionNo || 0) - Number(a.versionNo || 0))
    versionsLoaded.value = true
  } finally {
    versionsLoading.value = false
  }
}

function isCurrentVersion(version) {
  return String(version.versionId) === String(dataset.currentVersionId)
}

function rollbackableVersion(version) {
  return ['ACTIVE', 'ARCHIVED', 'READY'].includes(String(version.status || '').toUpperCase())
}

function versionStatusLabel(status) {
  return { ACTIVE: '可用', ARCHIVED: '历史', PREPARING: '准备中', READY: '可用', ERROR: '异常' }[status] || status || '-'
}

function versionStatusType(status) {
  if (['ACTIVE', 'READY'].includes(status)) return 'success'
  if (status === 'ERROR') return 'danger'
  if (status === 'PREPARING') return 'warning'
  return 'info'
}

function openRollbackDialog(version) {
  if (datasetBusy.value) return proxy.$modal.msgWarning(busyReason.value)
  rollbackTarget.value = version
  rollbackConfirmationName.value = ''
  rollbackDialogOpen.value = true
}

function resetRollbackDialog() {
  rollbackTarget.value = null
  rollbackConfirmationName.value = ''
}

async function submitClear() {
  if (clearConfirmationName.value !== dataset.displayName) return
  clearSubmitting.value = true
  try {
    const response = await clearDataset(datasetId.value, {
      baseVersionId: dataset.currentVersionId,
      confirmationName: clearConfirmationName.value
    })
    clearDialogOpen.value = false
    await handleMaintenanceResponse(response, '清空数据', '数据已清空')
  } catch (error) {
    handleVersionConflict(error)
  } finally {
    clearSubmitting.value = false
  }
}

async function submitRollback() {
  if (!rollbackTarget.value || rollbackConfirmationName.value !== dataset.displayName) return
  rollbackSubmitting.value = true
  try {
    const response = await rollbackDatasetVersion(datasetId.value, rollbackTarget.value.versionId, {
      baseVersionId: dataset.currentVersionId,
      confirmationName: rollbackConfirmationName.value
    })
    rollbackDialogOpen.value = false
    await handleMaintenanceResponse(response, '回滚数据版本', '版本回滚成功')
  } catch (error) {
    handleVersionConflict(error)
  } finally {
    rollbackSubmitting.value = false
  }
}

async function handleMaintenanceResponse(response, title, successTitle) {
  const payload = normalizePayload(response)
  jobsLoaded.value = false
  versionsLoaded.value = false
  if (payload.previewId || payload.status) {
    maintenanceJob.value = { ...payload }
    maintenanceJobTitle.value = title
    maintenanceSuccessTitle.value = successTitle
    maintenanceJobOpen.value = true
    await loadDetail()
    return
  }
  proxy.$modal.msgSuccess(successTitle)
  await loadDetail()
  if (activeTab.value === 'versions') await loadVersions(true)
}

async function handleMaintenanceCompleted(completedJob) {
  jobsLoaded.value = false
  versionsLoaded.value = false
  await loadDetail()
  if (activeTab.value === 'versions') await loadVersions(true)
}

async function loadJobs(force = false) {
  if (jobsLoaded.value && !force) return
  jobsLoading.value = true
  try {
    const response = await listDatasetJobs(datasetId.value)
    jobs.value = normalizeArrayResponse(response, ['jobs', 'rows'])
    jobsLoaded.value = true
  } finally {
    jobsLoading.value = false
  }
}

function jobRowProgress(row) {
  const total = Number(row.totalRows)
  const processed = Number(row.processedRows)
  if (row.status === 'SUCCESS') return 100
  if (Number.isFinite(total) && total > 0 && Number.isFinite(processed)) return Math.min(100, Math.round((processed * 100) / total))
  const fallback = { QUEUED: 8, VALIDATING: 30, STAGING: 65, COMMITTING: 90, RECOVERING: 92 }
  return fallback[row.status] || (['FAILED', 'VALIDATION_FAILED', 'MANUAL_REQUIRED'].includes(row.status) ? 100 : 0)
}

function jobRowProgressStatus(row) {
  if (row.status === 'SUCCESS') return 'success'
  if (['FAILED', 'VALIDATION_FAILED', 'MANUAL_REQUIRED'].includes(row.status)) return 'exception'
  return ''
}

const jobStatusLabels = {
  PARSING: '解析中',
  PENDING_CONFIRM: '等待确认',
  QUEUED: '排队中',
  STAGING: '写入中',
  VALIDATING: '校验中',
  COMMITTING: '发布中',
  SUCCESS: '成功',
  VALIDATION_FAILED: '校验失败',
  FAILED: '失败',
  RECOVERING: '恢复中',
  MANUAL_REQUIRED: '人工处理'
}

function jobStatusLabel(status) {
  return jobStatusLabels[status] || status || '-'
}

function jobStatusType(status) {
  if (status === 'SUCCESS') return 'success'
  if (['FAILED', 'VALIDATION_FAILED', 'MANUAL_REQUIRED'].includes(status)) return 'danger'
  if (['VALIDATING', 'COMMITTING', 'RECOVERING'].includes(status)) return 'warning'
  return 'info'
}

function operationLabel(operation) {
  return { CREATE: '新建', APPEND: '追加', REPLACE: '覆盖', UPSERT: '更新插入', EDIT: '行编辑', CLEAR: '清空', ROLLBACK: '回滚' }[operation] || operation || '-'
}

async function loadAcl(force = false) {
  if (!canGrant.value || (aclLoaded.value && !force)) return
  aclLoading.value = true
  try {
    const response = await getDatasetAcl(datasetId.value)
    aclEntries.value = normalizeArrayResponse(response, ['entries', 'rows']).map(entry => ({ ...entry, permissionMask: Number(entry.permissionMask) || 0 }))
    aclLoaded.value = true
  } finally {
    aclLoading.value = false
  }
}

async function loadAccessOptions() {
  if (userOptions.value.length || roleOptions.value.length) return
  accessOptionsLoading.value = true
  try {
    const [usersResponse, rolesResponse] = await Promise.all([listAccessUsers(), listAccessRoles()])
    userOptions.value = normalizeArrayResponse(usersResponse, ['users', 'rows'])
      .map(user => ({
        id: user.subjectId ?? user.userId ?? user.id,
        label: user.description
          ? `${user.description}（${user.subjectName || user.userName || user.subjectId}）`
          : user.subjectName || user.userName || user.name || String(user.subjectId ?? user.userId ?? user.id)
      }))
      .filter(user => String(user.id) !== String(dataset.ownerUserId))
    roleOptions.value = normalizeArrayResponse(rolesResponse, ['roles', 'rows']).map(role => ({
      id: role.subjectId ?? role.roleId ?? role.id,
      label: role.description
        ? `${role.subjectName || role.roleName || role.subjectId}（${role.description}）`
        : role.subjectName || role.roleName || role.name || String(role.subjectId ?? role.roleId ?? role.id)
    }))
  } finally {
    accessOptionsLoading.value = false
  }
}

function normalizeArrayResponse(response, keys = []) {
  const payload = response?.data ?? response
  if (Array.isArray(payload)) return payload
  for (const key of keys) {
    if (Array.isArray(payload?.[key])) return payload[key]
    if (Array.isArray(response?.[key])) return response[key]
  }
  return []
}

function openAclDialog() {
  aclDialogOpen.value = true
  loadAccessOptions()
}

function handleSubjectTypeChange() {
  aclForm.subjectId = undefined
}

function normalizeAclPermissions(values) {
  if (values.some(value => [ACCESS_IMPORT, ACCESS_EDIT, ACCESS_MANAGE].includes(value)) && !values.includes(ACCESS_READ)) {
    aclForm.permissions = [ACCESS_READ, ...values]
  }
}

function resetAclDialog() {
  aclForm.subjectType = 'USER'
  aclForm.subjectId = undefined
  aclForm.permissions = [ACCESS_READ]
}

function addAclEntry() {
  if (aclForm.subjectId === undefined || aclForm.subjectId === null) return proxy.$modal.msgWarning('请选择授权对象')
  if (!aclForm.permissions.length) return proxy.$modal.msgWarning('请至少选择一项权限')
  const option = currentSubjectOptions.value.find(item => String(item.id) === String(aclForm.subjectId))
  const permissionMask = aclForm.permissions.reduce((mask, value) => mask | Number(value), 0)
  const existing = aclEntries.value.find(entry => entry.subjectType === aclForm.subjectType && String(entry.subjectId) === String(aclForm.subjectId))
  if (existing) {
    existing.permissionMask = permissionMask
    existing.subjectName = option?.label || existing.subjectName
  } else {
    aclEntries.value.push({
      subjectType: aclForm.subjectType,
      subjectId: aclForm.subjectId,
      subjectName: option?.label || String(aclForm.subjectId),
      permissionMask
    })
  }
  aclDialogOpen.value = false
}

function toggleAclMask(entry, flag, checked) {
  let mask = Number(entry.permissionMask) || 0
  mask = checked ? mask | flag : mask & ~flag
  if (checked && [ACCESS_IMPORT, ACCESS_EDIT, ACCESS_MANAGE].includes(flag)) mask |= ACCESS_READ
  if (!checked && flag === ACCESS_READ) mask = 0
  entry.permissionMask = mask
}

function removeAcl(index) {
  aclEntries.value.splice(index, 1)
}

async function saveAcl() {
  aclSaving.value = true
  try {
    await updateDatasetAcl(datasetId.value, {
      entries: aclEntries.value
        .filter(entry => Number(entry.permissionMask) > 0)
        .map(entry => ({
          subjectType: entry.subjectType,
          subjectId: entry.subjectId,
          permissionMask: Number(entry.permissionMask)
        }))
    })
    proxy.$modal.msgSuccess('权限保存成功')
    aclLoaded.value = false
    await loadAcl(true)
  } finally {
    aclSaving.value = false
  }
}

function hasMask(mask, flag) {
  return (Number(mask) & flag) !== 0
}

function handleTabChange(name) {
  if (name === 'versions') loadVersions()
  if (name === 'jobs') loadJobs()
  if (name === 'acl') loadAcl()
}

function refreshCurrentTab() {
  if (activeTab.value === 'data') return queryData()
  if (activeTab.value === 'versions') return loadVersions(true)
  if (activeTab.value === 'jobs') return loadJobs(true)
  if (activeTab.value === 'acl') return loadAcl(true)
  loadDetail()
}

function datasetStatusLabel(status) {
  return { ACTIVE: '可用', NORMAL: '可用', READY: '可用', CREATING: '创建中', IMPORTING: '导入中', FAILED: '失败', DISABLED: '停用' }[status] || status || '-'
}

function datasetStatusType(status) {
  if (['ACTIVE', 'NORMAL', 'READY'].includes(status)) return 'success'
  if (['FAILED', 'DISABLED'].includes(status)) return 'danger'
  if (['CREATING', 'IMPORTING'].includes(status)) return 'warning'
  return 'info'
}

function formatCount(value) {
  const count = Number(value)
  return Number.isFinite(count) ? count.toLocaleString('zh-CN') : '0'
}

function handleBack() {
  if (editMode.value && pendingChanges.value.length) {
    proxy.$modal.confirm(`有 ${pendingChanges.value.length} 项变更尚未提交，确认放弃并返回列表吗？`).then(() => {
      finishEditMode()
      proxy.$tab.closeOpenPage({ path: '/datahub/dataset' })
    }).catch(() => {})
    return
  }
  proxy.$tab.closeOpenPage({ path: '/datahub/dataset' })
}

loadDetail()
</script>

<style scoped>
.datahub-detail-page {
  min-width: 0;
}

.page-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.page-heading {
  min-width: 0;
  flex: 1;
}

.heading-line {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.heading-line h2 {
  margin: 0;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 20px;
  line-height: 28px;
  letter-spacing: 0;
}

.page-heading > span {
  display: block;
  margin-top: 2px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.refresh-button {
  margin-left: auto;
}

.mutation-toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.mutation-toolbar > span {
  display: inline-flex;
}

.dataset-busy-alert {
  margin-bottom: 12px;
}

.mono {
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 12px;
}

.dataset-summary {
  display: grid;
  grid-template-columns: repeat(6, minmax(110px, 1fr));
  border-top: 1px solid #ebeef5;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 14px;
}

.summary-item {
  min-width: 0;
  padding: 12px 16px;
  border-right: 1px solid #ebeef5;
}

.summary-item:last-child {
  border-right: 0;
}

.summary-item span,
.summary-item strong {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-item span {
  color: #909399;
  font-size: 12px;
}

.summary-item strong {
  margin-top: 5px;
  color: #303133;
  font-size: 14px;
  font-weight: 600;
}

.summary-wide {
  grid-column: span 1;
}

.detail-tabs :deep(.el-tabs__content) {
  overflow: visible;
}

.edit-mode-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 10px 12px;
  margin-bottom: 10px;
  border: 1px solid #e6a23c;
  background: #fdf6ec;
}

.edit-mode-bar strong,
.edit-mode-bar span {
  display: block;
}

.edit-mode-bar span {
  margin-top: 2px;
  color: #606266;
  font-size: 12px;
}

.edit-mode-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.datahub-detail-page :deep(.pending-row-update > td.el-table__cell) {
  background: #fdf6ec;
}

.datahub-detail-page :deep(.pending-row-delete > td.el-table__cell) {
  background: #fef0f0;
  color: #a8abb2;
  text-decoration: line-through;
}

.data-filter-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  padding: 2px 0 10px;
}

.range-separator {
  color: #909399;
  font-size: 13px;
}

.active-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-bottom: 10px;
}

.table-tools-row {
  align-items: center;
  min-height: 32px;
}

.row-count {
  color: #606266;
  font-size: 13px;
  line-height: 32px;
}

.version-tools-row {
  align-items: center;
  justify-content: space-between;
  min-height: 32px;
}

.version-summary {
  color: #606266;
  font-size: 13px;
}

.current-version-tag {
  margin-left: 6px;
}

.review-alert {
  margin-bottom: 12px;
}

.review-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: 100%;
}

.review-footer > span {
  color: #606266;
  font-size: 13px;
}

.danger-impact {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 10px 16px;
  padding: 16px 0;
}

.danger-impact span {
  color: #909399;
}

.danger-impact strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dynamic-column-header {
  min-width: 0;
  line-height: 18px;
}

.dynamic-column-header span,
.dynamic-column-header small {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  letter-spacing: 0;
}

.dynamic-column-header small {
  color: #a8abb2;
  font-family: Consolas, 'SFMono-Regular', monospace;
  font-size: 10px;
  font-weight: 400;
}

.null-value {
  color: #c0c4cc;
  font-family: Consolas, 'SFMono-Regular', monospace;
}

.empty-string-value {
  color: #909399;
  font-family: Consolas, 'SFMono-Regular', monospace;
}

@media (max-width: 1200px) {
  .dataset-summary {
    grid-template-columns: repeat(3, minmax(120px, 1fr));
  }

  .summary-item:nth-child(3) {
    border-right: 0;
  }

  .summary-item:nth-child(-n + 3) {
    border-bottom: 1px solid #ebeef5;
  }
}

@media (max-width: 768px) {
  .datahub-detail-page {
    padding: 12px;
  }

  .dataset-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .summary-item,
  .summary-item:nth-child(3) {
    border-right: 1px solid #ebeef5;
    border-bottom: 1px solid #ebeef5;
  }

  .summary-item:nth-child(2n) {
    border-right: 0;
  }

  .summary-item:nth-last-child(-n + 2) {
    border-bottom: 0;
  }

  .data-filter-bar > :deep(.el-select),
  .data-filter-bar > :deep(.el-input),
  .data-filter-bar > :deep(.el-date-editor) {
    width: 100% !important;
  }

  .page-toolbar {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .mutation-toolbar {
    order: 3;
    width: 100%;
  }

  .edit-mode-bar,
  .review-footer {
    align-items: stretch;
    flex-direction: column;
  }

  .edit-mode-actions {
    align-items: stretch;
  }

  .edit-mode-actions :deep(.el-button) {
    flex: 1;
  }
}
</style>
