package com.xinke.datahub.service;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataChange;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubEditCellValue;
import com.xinke.datahub.domain.dto.DataHubEditRequest;
import com.xinke.datahub.domain.dto.DataHubRowMutation;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.enums.DataHubImportStatus;
import com.xinke.datahub.enums.DataHubColumnType;
import com.xinke.datahub.enums.DataHubOperationType;
import com.xinke.datahub.mapper.DataHubMapper;
import com.xinke.datahub.mapper.DataHubMutationMapper;
import com.xinke.datahub.naming.DataHubIdentifierService;
import com.xinke.datahub.parser.ParsedSpreadsheet;
import com.xinke.datahub.parser.SpreadsheetParser;
import com.xinke.datahub.storage.DataHubStorageService;

@Service
public class DataHubMutationWorker
{
    private static final Logger log = LoggerFactory.getLogger(DataHubMutationWorker.class);

    private final DataHubMapper mapper;
    private final DataHubMutationMapper mutationMapper;
    private final DataHubImportWorker importWorker;
    private final DataHubDynamicTableService tableService;
    private final DataHubIdentifierService identifiers;
    private final SpreadsheetParser parser;
    private final DataHubStorageService storageService;
    private final DataHubDefinitionValidator validator;
    private final DataHubProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public DataHubMutationWorker(DataHubMapper mapper, DataHubMutationMapper mutationMapper,
            DataHubImportWorker importWorker, DataHubDynamicTableService tableService,
            DataHubIdentifierService identifiers, SpreadsheetParser parser,
            DataHubStorageService storageService, DataHubDefinitionValidator validator,
            DataHubProperties properties, ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager)
    {
        this.mapper = mapper;
        this.mutationMapper = mutationMapper;
        this.importWorker = importWorker;
        this.tableService = tableService;
        this.identifiers = identifiers;
        this.parser = parser;
        this.storageService = storageService;
        this.validator = validator;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void execute(String previewId, Integer lockVersion)
    {
        DataHubImportJob job = mapper.selectJobByPreviewId(previewId);
        if (job == null || lockVersion == null || !lockVersion.equals(job.getLockVersion())
                || !DataHubImportStatus.VALIDATING.name().equals(job.getStatus())) return;
        String stagingTable = null;
        String targetTable = null;
        try
        {
            DataHubOperationType operation = mutationOperation(job.getOperationType());
            if (reconcilePublished(job)) return;
            DataHubDataset dataset = requireOwnedDataset(job);
            DataHubDataVersion source = requireSourceVersion(job, dataset);
            mark(job, DataHubImportStatus.VALIDATING, "校验变更数据");
            mapper.deleteImportErrors(job.getJobId());

            if (operation == DataHubOperationType.ROLLBACK)
            {
                executeRollback(job, dataset, source);
                return;
            }

            FileData fileData = null;
            if (operation == DataHubOperationType.APPEND || operation == DataHubOperationType.REPLACE)
            {
                fileData = prepareFile(job, source, operation);
                if (fileData == null) return;
            }

            int versionNo = mutationMapper.selectNextVersionNo(dataset.getDatasetId());
            stagingTable = identifiers.stagingTable(job.getJobId(), lockVersion);
            targetTable = identifiers.versionTable(dataset.getDatasetCode(), job.getJobId(), lockVersion, versionNo);
            cleanupPreviousTables(job, stagingTable, targetTable);
            markStaging(job, stagingTable, targetTable);

            long rowCount;
            List<DataHubDataChange> changes = new ArrayList<>();
            switch (operation)
            {
                case APPEND ->
                {
                    cloneSource(source, stagingTable);
                    requireCopiedRows(source, tableService.copyRows(source.getPhysicalTableName(), stagingTable));
                    DataHubImportWorker.PreparedResult prepared = fileData.prepared();
                    tableService.insertRows(stagingTable, job.getJobId(), fileData.request().getColumns(),
                            prepared.rows(), count -> updateProgress(job, count));
                    rowCount = checkedDatasetRows(source.getRowCount(), prepared.rows().size());
                }
                case REPLACE ->
                {
                    tableService.createTable(stagingTable, fileData.request().getColumns());
                    DataHubImportWorker.PreparedResult prepared = fileData.prepared();
                    tableService.insertRows(stagingTable, job.getJobId(), fileData.request().getColumns(),
                            prepared.rows(), count -> updateProgress(job, count));
                    rowCount = prepared.rows().size();
                }
                case CLEAR ->
                {
                    cloneSource(source, stagingTable);
                    rowCount = 0L;
                }
                case EDIT ->
                {
                    cloneSource(source, stagingTable);
                    requireCopiedRows(source, tableService.copyRows(source.getPhysicalTableName(), stagingTable));
                    EditResult result = applyEdits(job, source, stagingTable);
                    rowCount = result.rowCount();
                    changes.addAll(result.changes());
                }
                default -> throw new ServiceException("不支持的数据变更操作");
            }

            mark(job, DataHubImportStatus.COMMITTING, "发布数据版本");
            tableService.publishVersion(stagingTable, targetTable);
            stagingTable = null;
            publishNewVersion(job, source, operation, versionNo, targetTable, rowCount, changes);
            targetTable = null;
        }
        catch (Exception e)
        {
            if (!reconcilePublished(job))
            {
                markFailedAndRelease(job, stableMessage(e), e);
                safeDropUnowned(targetTable);
            }
        }
        finally
        {
            safeDrop(stagingTable);
        }
    }

    private FileData prepareFile(DataHubImportJob job, DataHubDataVersion source, DataHubOperationType operation)
            throws Exception
    {
        DataHubCreateRequest request = objectMapper.readValue(job.getSchemaSnapshotJson(), DataHubCreateRequest.class);
        ParsedSpreadsheet parsed = parser.parse(storageService.resolve(job.getStoredFilePath()),
                job.getSourceFileName(), job.getSheetName());
        validator.validateAgainstSource(request, parsed);
        DataHubImportWorker.PreparedResult prepared = importWorker.prepare(job.getJobId(), job.getLockVersion(),
                request.getColumns(), parsed.getRows());
        updateCounts(job, (long) parsed.getRows().size(), 0L, 0L,
                (long) prepared.invalidRows().size());
        if (!prepared.errors().isEmpty()) mapper.insertImportErrors(prepared.errors());
        if (!prepared.errors().isEmpty())
        {
            validationFailed(job, prepared.invalidRows().size());
            return null;
        }
        if (operation == DataHubOperationType.APPEND)
            checkedDatasetRows(source.getRowCount(), prepared.rows().size());
        else
            checkedReplacementRows(prepared.rows().size());
        return new FileData(request, prepared);
    }

    private EditResult applyEdits(DataHubImportJob job, DataHubDataVersion source, String stagingTable)
            throws Exception
    {
        DataHubEditRequest request = objectMapper.readValue(job.getOperationPayloadJson(), DataHubEditRequest.class);
        if (request.getMutations() == null || request.getMutations().isEmpty())
            throw new ServiceException("编辑任务不包含数据变更");
        List<DataHubColumn> columns = mutationMapper.selectSchemaColumns(job.getDatasetId(), source.getSchemaId());
        if (columns.isEmpty()) throw new ServiceException("当前字段结构不可用");
        Map<Long, Integer> columnIndexes = new HashMap<>();
        List<DataHubColumnDefinition> definitions = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++)
        {
            DataHubColumn column = columns.get(i);
            columnIndexes.put(column.getColumnId(), i);
            definitions.add(definition(column));
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        List<DataHubDataChange> changes = new ArrayList<>();
        List<PendingInsert> pendingInserts = new ArrayList<>();
        long nextSourceRow = tableService.selectMaxSourceRowNo(stagingTable);
        int deleted = 0;
        int processed = 0;
        for (DataHubRowMutation mutation : request.getMutations())
        {
            String action = mutation.getOperation();
            if ("INSERT".equals(action))
            {
                if (nextSourceRow >= Integer.MAX_VALUE) throw new ServiceException("来源行号已达到系统上限");
                int sourceRowNo = Math.toIntExact(++nextSourceRow);
                Object[] values = editValues(null, mutation.getValues(), columns, definitions, columnIndexes);
                pendingInserts.add(new PendingInsert(mutation, sourceRowNo,
                        new PreparedDataRow(sourceRowNo, rowHash(digest, values), values)));
            }
            else
            {
                Map<String, Object> before = tableService.selectRow(stagingTable, mutation.getRowId(), columns);
                if ("DELETE".equals(action))
                {
                    tableService.deleteRow(stagingTable, mutation.getRowId(), mutation.getExpectedRowHash());
                    changes.add(change(job, mutation, mutation.getRowId(), before, null));
                    deleted++;
                }
                else if ("UPDATE".equals(action))
                {
                    Object[] values = editValues(before, mutation.getValues(), columns, definitions, columnIndexes);
                    tableService.updateRow(stagingTable, job.getJobId(), mutation.getRowId(),
                            mutation.getExpectedRowHash(), columns, values, rowHash(digest, values));
                    Map<String, Object> after = tableService.selectRow(stagingTable, mutation.getRowId(), columns);
                    changes.add(change(job, mutation, mutation.getRowId(), before, after));
                }
                else
                {
                    throw new ServiceException("编辑任务包含不支持的操作");
                }
            }
            processed++;
            if (processed % 100 == 0) requireActive(mapper.touchImportJob(job.getJobId(), job.getLockVersion()));
        }

        if (!pendingInserts.isEmpty())
        {
            List<PreparedDataRow> rows = pendingInserts.stream().map(PendingInsert::row).toList();
            tableService.insertRows(stagingTable, job.getJobId(), definitions, rows, count -> { });
            Map<Long, Map<String, Object>> insertedBySourceRow = new HashMap<>();
            for (Map<String, Object> row : tableService.selectRowsByImportJob(stagingTable, job.getJobId(), columns))
                insertedBySourceRow.put(number(row.get("_source_row_no")).longValue(), new LinkedHashMap<>(row));
            for (PendingInsert pending : pendingInserts)
            {
                Map<String, Object> after = insertedBySourceRow.get((long) pending.sourceRowNo());
                if (after == null) throw new ServiceException("新增数据行审计信息读取失败");
                Long rowId = number(after.get("_id")).longValue();
                changes.add(change(job, pending.mutation(), rowId, null, after));
            }
        }

        long sourceRows = source.getRowCount() == null ? 0L : source.getRowCount();
        long rowCount = Math.addExact(sourceRows, pendingInserts.size()) - deleted;
        if (rowCount > Math.max(1, properties.getMaxDatasetRows()))
            throw new ServiceException("编辑后数据行数不能超过" + properties.getMaxDatasetRows() + "行");
        updateProgress(job, request.getMutations().size());
        return new EditResult(rowCount, changes);
    }

    private Object[] editValues(Map<String, Object> current, List<DataHubEditCellValue> edits,
            List<DataHubColumn> columns, List<DataHubColumnDefinition> definitions,
            Map<Long, Integer> columnIndexes)
    {
        Object[] values = new Object[columns.size()];
        if (current != null)
            for (int i = 0; i < columns.size(); i++) values[i] = current.get(columns.get(i).getPhysicalName());
        boolean[] supplied = new boolean[columns.size()];
        if (edits != null)
        {
            for (DataHubEditCellValue edit : edits)
            {
                Integer index = edit == null ? null : columnIndexes.get(edit.getColumnId());
                if (index == null || supplied[index]) throw new ServiceException("编辑字段不存在或重复");
                supplied[index] = true;
                values[index] = convertEditValue(definitions.get(index), edit);
            }
        }
        if (current == null)
        {
            for (int i = 0; i < definitions.size(); i++)
                if (!supplied[i]) values[i] = importWorker.convert(definitions.get(i), null);
        }
        return values;
    }

    Object convertEditValue(DataHubColumnDefinition definition, DataHubEditCellValue edit)
    {
        if (edit == null) throw new ServiceException("编辑字段值不能为空");
        if (Boolean.TRUE.equals(edit.getIsNull())) return importWorker.convert(definition, null);
        DataHubColumnType type = DataHubColumnType.from(definition.getDataType());
        String raw = edit.getValue();
        if (type != DataHubColumnType.VARCHAR && type != DataHubColumnType.TEXT)
            return importWorker.convert(definition, raw);
        if (raw == null) raw = "";
        if (raw.length() > properties.getMaxCellLength())
            throw new ServiceException("编辑内容超过" + properties.getMaxCellLength() + "个字符");
        if (type == DataHubColumnType.VARCHAR)
        {
            int length = definition.getLength() == null ? 255 : definition.getLength();
            if (raw.length() > length) throw new ServiceException("长度超过" + length + "个字符");
        }
        return raw;
    }

    private void publishNewVersion(DataHubImportJob job, DataHubDataVersion source,
            DataHubOperationType operation, int versionNo, String targetTable, long rowCount,
            List<DataHubDataChange> changes)
    {
        transactionTemplate.executeWithoutResult(status -> {
            requireClaimed(job, DataHubImportStatus.COMMITTING);
            DataHubDataVersion existing = mutationMapper.selectVersionByJobId(job.getJobId());
            if (existing != null) throw new ServiceException("该任务已经生成数据版本");

            DataHubDataVersion version = new DataHubDataVersion();
            version.setDatasetId(job.getDatasetId());
            version.setParentVersionId(source.getVersionId());
            version.setSchemaId(source.getSchemaId());
            version.setJobId(job.getJobId());
            version.setVersionNo(versionNo);
            version.setVersionType(operation.name());
            version.setPhysicalTableName(targetTable);
            version.setRowCount(rowCount);
            version.setStatus("ACTIVE");
            mutationMapper.insertMutationVersion(version);

            Date retentionUntil = Date.from(Instant.now().plusSeconds(
                    Math.max(1, properties.getVersionRetentionDays()) * 86400L));
            requireOne(mutationMapper.archiveVersion(source.getVersionId(), retentionUntil), "来源版本状态已变化");
            requireOne(mutationMapper.publishNewVersion(job.getDatasetId(), job.getJobId(), source.getVersionId(),
                    expectedDatasetLock(job), version.getVersionId(), source.getSchemaId(), rowCount,
                    job.getUploadUserName()), "数据表版本已变化，无法发布本次变更");
            if (!changes.isEmpty())
            {
                for (DataHubDataChange change : changes) change.setTargetVersionId(version.getVersionId());
                requireOne(mutationMapper.insertDataChanges(changes) == changes.size() ? 1 : 0,
                        "行级变更审计保存失败");
            }
            requireOne(mutationMapper.completeMutationJob(job.getJobId(), job.getLockVersion(),
                    version.getVersionId(), processedRows(job, operation), new Date()), "变更任务状态已变化");
        });
    }

    private void executeRollback(DataHubImportJob job, DataHubDataset dataset, DataHubDataVersion source)
    {
        DataHubDataVersion target = mutationMapper.selectDatasetVersion(job.getDatasetId(),
                job.getRollbackTargetVersionId());
        if (target == null || !"ARCHIVED".equals(target.getStatus()))
            throw new ServiceException("回滚目标版本不可用");
        tableService.requireTable(target.getPhysicalTableName());
        mark(job, DataHubImportStatus.COMMITTING, "切换回滚版本");
        transactionTemplate.executeWithoutResult(status -> {
            requireClaimed(job, DataHubImportStatus.COMMITTING);
            Date retentionUntil = Date.from(Instant.now().plusSeconds(
                    Math.max(1, properties.getVersionRetentionDays()) * 86400L));
            requireOne(mutationMapper.archiveVersion(source.getVersionId(), retentionUntil), "当前版本状态已变化");
            requireOne(mutationMapper.activateVersion(target.getVersionId()), "回滚目标版本状态已变化");
            requireOne(mutationMapper.publishRollback(dataset.getDatasetId(), job.getJobId(), source.getVersionId(),
                    expectedDatasetLock(job), target.getVersionId(), target.getSchemaId(), target.getRowCount(),
                    job.getUploadUserName()), "数据表版本已变化，无法完成回滚");
            requireOne(mutationMapper.completeMutationJob(job.getJobId(), job.getLockVersion(), target.getVersionId(),
                    1L, new Date()), "回滚任务状态已变化");
        });
    }

    private boolean reconcilePublished(DataHubImportJob job)
    {
        try
        {
            DataHubImportJob latest = mapper.selectJobByPreviewId(job.getPreviewId());
            if (latest != null && DataHubImportStatus.SUCCESS.name().equals(latest.getStatus())) return true;
            DataHubDataset dataset = mutationMapper.selectMutationDataset(job.getDatasetId());
            if (dataset == null || Objects.equals(dataset.getActiveJobId(), job.getJobId())) return false;
            Long targetVersionId = null;
            DataHubOperationType operation = mutationOperation(job.getOperationType());
            if (operation == DataHubOperationType.ROLLBACK)
            {
                if (Objects.equals(dataset.getCurrentVersionId(), job.getRollbackTargetVersionId()))
                    targetVersionId = job.getRollbackTargetVersionId();
            }
            else
            {
                DataHubDataVersion version = mutationMapper.selectVersionByJobId(job.getJobId());
                if (version != null && Objects.equals(dataset.getCurrentVersionId(), version.getVersionId()))
                    targetVersionId = version.getVersionId();
            }
            if (targetVersionId == null) return false;
            mutationMapper.reconcileCompletedJob(job.getJobId(), targetVersionId,
                    processedRows(job, operation), new Date());
            return true;
        }
        catch (RuntimeException e)
        {
            log.warn("Failed to reconcile DataHub mutation job {}", job.getJobId(), e);
            return false;
        }
    }

    private DataHubDataset requireOwnedDataset(DataHubImportJob job)
    {
        DataHubDataset dataset = mutationMapper.selectMutationDataset(job.getDatasetId());
        if (dataset == null || !Objects.equals(dataset.getActiveJobId(), job.getJobId())
                || !Objects.equals(dataset.getCurrentVersionId(), job.getSourceVersionId())
                || !Objects.equals(dataset.getLockVersion(), expectedDatasetLock(job)))
            throw new ServiceException("数据表写入权或基础版本已变化");
        return dataset;
    }

    private DataHubDataVersion requireSourceVersion(DataHubImportJob job, DataHubDataset dataset)
    {
        DataHubDataVersion source = mutationMapper.selectDatasetVersion(dataset.getDatasetId(),
                job.getSourceVersionId());
        if (source == null || !"ACTIVE".equals(source.getStatus()))
            throw new ServiceException("基础数据版本不可用");
        tableService.requireTable(source.getPhysicalTableName());
        return source;
    }

    private void cloneSource(DataHubDataVersion source, String stagingTable)
    {
        tableService.createTableLike(source.getPhysicalTableName(), stagingTable);
    }

    private void requireCopiedRows(DataHubDataVersion source, long copied)
    {
        long expected = source.getRowCount() == null ? 0L : source.getRowCount();
        if (copied != expected) throw new ServiceException("基础版本复制行数不一致");
    }

    private long checkedDatasetRows(Long sourceRowCount, long importedRows)
    {
        long sourceRows = sourceRowCount == null ? 0L : sourceRowCount;
        long limit = Math.max(1L, properties.getMaxDatasetRows());
        if (importedRows < 0 || sourceRows > limit - importedRows)
            throw new ServiceException("追加后数据行数不能超过" + limit + "行");
        return sourceRows + importedRows;
    }

    private long checkedReplacementRows(long importedRows)
    {
        long limit = Math.max(1L, properties.getMaxDatasetRows());
        if (importedRows < 0 || importedRows > limit)
            throw new ServiceException("覆盖后数据行数不能超过" + limit + "行");
        return importedRows;
    }

    private void cleanupPreviousTables(DataHubImportJob job, String stagingTable, String targetTable)
    {
        if (job.getStagingTableName() != null && !job.getStagingTableName().equals(stagingTable))
            safeDrop(job.getStagingTableName());
        if (job.getTargetTableName() != null && !job.getTargetTableName().equals(targetTable))
            safeDropUnowned(job.getTargetTableName());
        safeDrop(stagingTable);
        safeDropUnowned(targetTable);
    }

    private void markStaging(DataHubImportJob job, String stagingTable, String targetTable)
    {
        DataHubImportJob update = update(job);
        update.setStatus(DataHubImportStatus.STAGING.name());
        update.setPhase("构建不可变版本");
        update.setStagingTableName(stagingTable);
        update.setTargetTableName(targetTable);
        requireActive(mapper.updateImportJob(update));
        job.setStatus(DataHubImportStatus.STAGING.name());
        job.setStagingTableName(stagingTable);
        job.setTargetTableName(targetTable);
    }

    private void mark(DataHubImportJob job, DataHubImportStatus status, String phase)
    {
        DataHubImportJob update = update(job);
        update.setStatus(status.name());
        update.setPhase(phase);
        if (status == DataHubImportStatus.VALIDATING) update.setStartTime(new Date());
        requireActive(mapper.updateImportJob(update));
        job.setStatus(status.name());
    }

    private void updateProgress(DataHubImportJob job, long processed)
    {
        DataHubImportJob update = update(job);
        update.setProcessedRows(processed);
        requireActive(mapper.updateImportJob(update));
    }

    private void updateCounts(DataHubImportJob job, Long total, Long processed, Long success, Long failed)
    {
        DataHubImportJob update = update(job);
        update.setTotalRows(total);
        update.setProcessedRows(processed);
        update.setSuccessRows(success);
        update.setFailedRows(failed);
        requireActive(mapper.updateImportJob(update));
        job.setTotalRows(total);
    }

    private void validationFailed(DataHubImportJob job, int invalidRows)
    {
        transactionTemplate.executeWithoutResult(status -> {
            DataHubImportJob failed = update(job);
            failed.setStatus(DataHubImportStatus.VALIDATION_FAILED.name());
            failed.setPhase("数据校验失败");
            failed.setErrorMessage("存在" + invalidRows + "行数据不符合当前字段结构");
            failed.setFinishTime(new Date());
            requireActive(mapper.updateImportJob(failed));
            requireOne(mutationMapper.releaseDatasetJob(job.getDatasetId(), job.getJobId()), "数据表写入锁释放失败");
        });
    }

    private void markFailedAndRelease(DataHubImportJob job, String message, Exception cause)
    {
        log.error("DataHub mutation job {} failed", job.getJobId(), cause);
        try
        {
            transactionTemplate.executeWithoutResult(status -> {
                DataHubImportJob failed = update(job);
                failed.setStatus(DataHubImportStatus.FAILED.name());
                failed.setPhase("数据变更失败");
                failed.setErrorMessage(message);
                failed.setFinishTime(new Date());
                if (mapper.markImportJobFailed(failed) == 1)
                    mutationMapper.releaseDatasetJob(job.getDatasetId(), job.getJobId());
            });
        }
        catch (RuntimeException e)
        {
            log.error("Failed to persist DataHub mutation failure for job {}", job.getJobId(), e);
        }
    }

    private void requireClaimed(DataHubImportJob job, DataHubImportStatus status)
    {
        requireOne(mapper.countClaimedImportJob(job.getJobId(), job.getLockVersion(), status.name()),
                "变更任务执行权已失效");
    }

    private int expectedDatasetLock(DataHubImportJob job)
    {
        if (job.getSourceLockVersion() == null || job.getSourceLockVersion() == Integer.MAX_VALUE)
            throw new ServiceException("数据表锁版本不合法");
        return job.getSourceLockVersion() + 1;
    }

    private long processedRows(DataHubImportJob job, DataHubOperationType operation)
    {
        if (operation == DataHubOperationType.ROLLBACK) return 1L;
        if (operation == DataHubOperationType.CLEAR) return 0L;
        return job.getTotalRows() == null ? 0L : job.getTotalRows();
    }

    private DataHubDataChange change(DataHubImportJob job, DataHubRowMutation mutation, Long rowId,
            Map<String, Object> before, Map<String, Object> after)
    {
        DataHubDataChange change = new DataHubDataChange();
        change.setDatasetId(job.getDatasetId());
        change.setJobId(job.getJobId());
        change.setClientMutationId(mutation.getClientMutationId());
        change.setRowId(rowId);
        change.setAction(mutation.getOperation());
        change.setBeforeJson(json(before));
        change.setAfterJson(json(after));
        change.setCreateBy(job.getUploadUserName());
        return change;
    }

    private String json(Object value)
    {
        if (value == null) return null;
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new ServiceException("行级变更审计序列化失败"); }
    }

    private byte[] rowHash(MessageDigest digest, Object[] values)
    {
        List<String> canonical = new ArrayList<>(values.length);
        for (Object value : values) canonical.add(canonicalValue(value));
        return importWorker.rowHash(digest, canonical);
    }

    private String canonicalValue(Object value)
    {
        if (value == null) return "";
        if (value instanceof BigDecimal decimal) return decimal.stripTrailingZeros().toPlainString();
        if (value instanceof java.sql.Date date) return date.toLocalDate().toString();
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toString();
        if (value instanceof LocalDate date) return date.toString();
        if (value instanceof LocalDateTime dateTime) return dateTime.toString();
        if (value instanceof Date date) return date.toInstant().atOffset(ZoneOffset.UTC).toString();
        return String.valueOf(value);
    }

    private DataHubColumnDefinition definition(DataHubColumn column)
    {
        DataHubColumnDefinition definition = new DataHubColumnDefinition();
        definition.setTargetColumnId(column.getColumnId());
        definition.setSourceIndex(column.getSourceIndex());
        definition.setSourceName(column.getSourceName());
        definition.setDisplayName(column.getDisplayName());
        definition.setPhysicalName(column.getPhysicalName());
        definition.setDataType(column.getDataType());
        definition.setLength(column.getColumnLength());
        definition.setPrecision(column.getNumericPrecision());
        definition.setScale(column.getNumericScale());
        definition.setNullable(column.getNullable());
        definition.setTranslationSource("CURRENT_SCHEMA");
        return definition;
    }

    private Number number(Object value)
    {
        if (value instanceof Number number) return number;
        if (value == null) throw new ServiceException("动态数据行标识缺失");
        try { return Long.valueOf(String.valueOf(value)); }
        catch (NumberFormatException e) { throw new ServiceException("动态数据行标识不合法"); }
    }

    private DataHubImportJob update(DataHubImportJob job)
    {
        DataHubImportJob update = new DataHubImportJob();
        update.setJobId(job.getJobId());
        update.setLockVersion(job.getLockVersion());
        return update;
    }

    private DataHubOperationType mutationOperation(String operation)
    {
        try
        {
            DataHubOperationType value = DataHubOperationType.valueOf(operation);
            if (value == DataHubOperationType.CREATE) throw new IllegalArgumentException();
            return value;
        }
        catch (Exception e)
        {
            throw new ServiceException("不支持的数据变更操作");
        }
    }

    private void requireOne(int count, String message)
    {
        if (count != 1) throw new ServiceException(message);
    }

    private void requireActive(int count)
    {
        requireOne(count, "变更任务执行权已失效");
    }

    private String stableMessage(Exception error)
    {
        String message = error instanceof ServiceException ? error.getMessage() : null;
        if (message == null || message.isBlank()) return "数据变更执行失败，正式数据未发生变化";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private void safeDropUnowned(String tableName)
    {
        if (tableName == null || tableName.isBlank()) return;
        try
        {
            if (mapper.countVersionByPhysicalTable(tableName) == 0) tableService.dropIfExists(tableName);
        }
        catch (RuntimeException e)
        {
            log.warn("Failed to clean unowned DataHub target table {}", tableName, e);
        }
    }

    private void safeDrop(String tableName)
    {
        if (tableName == null || tableName.isBlank()) return;
        try { tableService.dropIfExists(tableName); }
        catch (RuntimeException e) { log.warn("Failed to clean DataHub staging table {}", tableName, e); }
    }

    private record FileData(DataHubCreateRequest request, DataHubImportWorker.PreparedResult prepared) { }
    private record EditResult(long rowCount, List<DataHubDataChange> changes) { }
    private record PendingInsert(DataHubRowMutation mutation, int sourceRowNo, PreparedDataRow row) { }
}
