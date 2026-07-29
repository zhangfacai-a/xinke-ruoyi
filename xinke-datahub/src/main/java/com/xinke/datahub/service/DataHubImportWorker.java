package com.xinke.datahub.service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportError;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.DataHubSchema;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.enums.DataHubColumnType;
import com.xinke.datahub.enums.DataHubImportStatus;
import com.xinke.datahub.mapper.DataHubMapper;
import com.xinke.datahub.mapper.DataHubFolderMapper;
import com.xinke.datahub.naming.DataHubIdentifierService;
import com.xinke.datahub.naming.EnglishNameGenerator;
import com.xinke.datahub.parser.ParsedRow;
import com.xinke.datahub.parser.ParsedSpreadsheet;
import com.xinke.datahub.parser.SpreadsheetParser;
import com.xinke.datahub.storage.DataHubStorageService;

@Service
public class DataHubImportWorker
{
    private static final Logger log = LoggerFactory.getLogger(DataHubImportWorker.class);
    private static final int MAX_RECORDED_ERRORS = 1000;

    private final DataHubMapper mapper;
    private final DataHubFolderMapper folderMapper;
    private final ObjectMapper objectMapper;
    private final SpreadsheetParser parser;
    private final DataHubStorageService storageService;
    private final DataHubDefinitionValidator validator;
    private final DataHubDynamicTableService tableService;
    private final DataHubIdentifierService identifiers;
    private final EnglishNameGenerator nameGenerator;
    private final TransactionTemplate transactionTemplate;

    public DataHubImportWorker(DataHubMapper mapper, DataHubFolderMapper folderMapper,
            ObjectMapper objectMapper, SpreadsheetParser parser,
            DataHubStorageService storageService, DataHubDefinitionValidator validator,
            DataHubDynamicTableService tableService, DataHubIdentifierService identifiers,
            EnglishNameGenerator nameGenerator, PlatformTransactionManager transactionManager)
    {
        this.mapper = mapper;
        this.folderMapper = folderMapper;
        this.objectMapper = objectMapper;
        this.parser = parser;
        this.storageService = storageService;
        this.validator = validator;
        this.tableService = tableService;
        this.identifiers = identifiers;
        this.nameGenerator = nameGenerator;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void executeCreate(String previewId, Integer lockVersion)
    {
        DataHubImportJob job = mapper.selectJobByPreviewId(previewId);
        if (job == null || !DataHubImportStatus.VALIDATING.name().equals(job.getStatus())
                || lockVersion == null || !lockVersion.equals(job.getLockVersion())) return;
        String stagingTable = identifiers.stagingTable(job.getJobId(), lockVersion);
        String targetTable = null;
        try
        {
            mark(job.getJobId(), lockVersion, DataHubImportStatus.VALIDATING, "解析并校验数据", null);
            mapper.deleteImportErrors(job.getJobId());
            cleanupPreviousStaging(job.getStagingTableName(), stagingTable);
            DataHubCreateRequest request = objectMapper.readValue(job.getSchemaSnapshotJson(), DataHubCreateRequest.class);
            ParsedSpreadsheet parsed = parser.parse(storageService.resolve(job.getStoredFilePath()),
                    job.getSourceFileName(), job.getSheetName());
            validator.validateAgainstSource(request, parsed);

            PreparedResult prepared = prepare(job.getJobId(), lockVersion, request.getColumns(), parsed.getRows());
            updateCounts(job.getJobId(), lockVersion, (long) parsed.getRows().size(), 0L, 0L,
                    (long) prepared.invalidRows.size());
            if (!prepared.errors.isEmpty()) mapper.insertImportErrors(prepared.errors);
            if (!prepared.errors.isEmpty())
            {
                mark(job.getJobId(), lockVersion, DataHubImportStatus.VALIDATION_FAILED,
                        "数据校验失败", "存在" + prepared.invalidRows.size() + "行数据不符合字段配置");
                finish(job.getJobId(), lockVersion, DataHubImportStatus.VALIDATION_FAILED);
                return;
            }

            targetTable = identifiers.versionTable(request.getPhysicalName(), job.getJobId(), lockVersion, 1);
            cleanupPreviousTarget(job.getTargetTableName(), targetTable);
            resetOwnedTables(stagingTable, targetTable);
            DataHubImportJob tableNames = new DataHubImportJob();
            tableNames.setJobId(job.getJobId());
            tableNames.setLockVersion(lockVersion);
            tableNames.setStagingTableName(stagingTable);
            tableNames.setTargetTableName(targetTable);
            tableNames.setStatus(DataHubImportStatus.STAGING.name());
            tableNames.setPhase("写入暂存表");
            tableNames.setStartTime(new Date());
            requireActive(mapper.updateImportJob(tableNames));

            tableService.createTable(stagingTable, request.getColumns());
            tableService.insertRows(stagingTable, job.getJobId(), request.getColumns(), prepared.rows,
                    count -> updateProgress(job.getJobId(), lockVersion, count));

            mark(job.getJobId(), lockVersion, DataHubImportStatus.COMMITTING, "发布数据版本", null);
            tableService.publishVersion(stagingTable, targetTable);
            final String physicalTable = targetTable;
            transactionTemplate.executeWithoutResult(status -> publishMetadata(job, request, parsed, physicalTable));
        }
        catch (DuplicateKeyException e)
        {
            markFailed(job.getJobId(), lockVersion, "数据表名称已经存在，请修改后重试", e);
        }
        catch (Exception e)
        {
            markFailed(job.getJobId(), lockVersion, stableErrorMessage(e), e);
        }
        finally
        {
            safeDrop(stagingTable);
        }
    }

    PreparedResult prepare(Long jobId, Integer lockVersion, List<DataHubColumnDefinition> columns,
            List<ParsedRow> sourceRows) throws Exception
    {
        List<PreparedDataRow> rows = new ArrayList<>(sourceRows.size());
        List<DataHubImportError> errors = new ArrayList<>();
        Set<Integer> invalidRows = new HashSet<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        int scannedRows = 0;
        for (ParsedRow sourceRow : sourceRows)
        {
            scannedRows++;
            Object[] converted = new Object[columns.size()];
            boolean valid = true;
            for (int i = 0; i < columns.size(); i++)
            {
                DataHubColumnDefinition column = columns.get(i);
                String raw = column.getSourceIndex() < sourceRow.getValues().size()
                        ? sourceRow.getValues().get(column.getSourceIndex()) : "";
                try
                {
                    converted[i] = convert(column, raw);
                }
                catch (RuntimeException e)
                {
                    valid = false;
                    invalidRows.add(sourceRow.getSourceRowNo());
                    if (errors.size() < MAX_RECORDED_ERRORS)
                        errors.add(error(jobId, sourceRow.getSourceRowNo(), column, raw, e.getMessage()));
                }
            }
            if (valid) rows.add(new PreparedDataRow(sourceRow.getSourceRowNo(), rowHash(digest, sourceRow.getValues()), converted));
            if (scannedRows % 1000 == 0) requireActive(mapper.touchImportJob(jobId, lockVersion));
        }
        return new PreparedResult(rows, errors, invalidRows);
    }

    Object convert(DataHubColumnDefinition column, String raw)
    {
        if (raw == null || raw.isBlank())
        {
            if (!Boolean.TRUE.equals(column.getNullable())) throw new IllegalArgumentException("不能为空");
            return null;
        }
        DataHubColumnType type = DataHubColumnType.from(column.getDataType());
        int varcharLength = column.getLength() == null ? 255 : column.getLength();
        if (type == DataHubColumnType.VARCHAR && raw.length() > varcharLength)
            throw new IllegalArgumentException("长度超过" + varcharLength + "个字符");
        Object value = type.convert(raw);
        if (type == DataHubColumnType.DECIMAL)
        {
            BigDecimal decimal = (BigDecimal) value;
            int precision = column.getPrecision() == null ? 18 : column.getPrecision();
            int scale = column.getScale() == null ? 2 : column.getScale();
            int valueScale = Math.max(0, decimal.scale());
            int integerDigits = decimal.compareTo(BigDecimal.ZERO) == 0
                    ? 0 : Math.max(0, decimal.precision() - decimal.scale());
            if (valueScale > scale || integerDigits > precision - scale)
                throw new IllegalArgumentException("数字超过DECIMAL(" + precision + "," + scale + ")范围");
        }
        return value;
    }

    private void publishMetadata(DataHubImportJob job, DataHubCreateRequest request,
            ParsedSpreadsheet parsed, String targetTable)
    {
        if (mapper.countClaimedImportJob(job.getJobId(), job.getLockVersion(), DataHubImportStatus.COMMITTING.name()) != 1)
            throw new ServiceException("导入任务执行权已失效");
        String nameKey = nameGenerator.normalizeNameKey(request.getDisplayName());
        if (mapper.countDatasetByNameKey(nameKey) > 0) throw new DuplicateKeyException("dataset name exists");
        if (mapper.countDatasetByCode(request.getPhysicalName()) > 0) throw new DuplicateKeyException("dataset code exists");

        DataHubDataset dataset = new DataHubDataset();
        dataset.setDisplayName(request.getDisplayName().strip());
        dataset.setNormalizedName(nameKey);
        dataset.setDatasetCode(request.getPhysicalName());
        dataset.setOwnerUserId(job.getUploadUserId());
        dataset.setOwnerUserName(job.getUploadUserName());
        dataset.setRowCount((long) parsed.getRows().size());
        dataset.setColumnCount(request.getColumns().size());
        dataset.setStatus("BUILDING");
        dataset.setSourceFileName(job.getSourceFileName());
        dataset.setSourceSheetName(parsed.getSheetName());
        dataset.setCreateBy(job.getUploadUserName());
        dataset.setUpdateBy(job.getUploadUserName());
        mapper.insertDataset(dataset);

        DataHubSchema schema = new DataHubSchema();
        schema.setDatasetId(dataset.getDatasetId());
        schema.setVersionNo(1);
        schema.setSourceJobId(job.getJobId());
        schema.setSchemaHash(sha256(job.getSchemaSnapshotJson()));
        schema.setStatus("ACTIVE");
        schema.setCreateBy(job.getUploadUserName());
        mapper.insertSchema(schema);

        List<DataHubColumn> columns = new ArrayList<>();
        for (int i = 0; i < request.getColumns().size(); i++)
        {
            DataHubColumnDefinition definition = request.getColumns().get(i);
            DataHubColumn column = new DataHubColumn();
            column.setDatasetId(dataset.getDatasetId());
            column.setSchemaId(schema.getSchemaId());
            column.setSourceIndex(definition.getSourceIndex());
            column.setOrdinalPosition(i + 1);
            column.setSourceName(definition.getSourceName());
            column.setDisplayName(definition.getDisplayName());
            column.setPhysicalName(definition.getPhysicalName());
            column.setDataType(DataHubColumnType.from(definition.getDataType()).name());
            column.setColumnLength(definition.getLength());
            column.setNumericPrecision(definition.getPrecision());
            column.setNumericScale(definition.getScale());
            column.setNullable(Boolean.TRUE.equals(definition.getNullable()));
            column.setBusinessKey(Boolean.FALSE);
            column.setTranslationSource(definition.getTranslationSource());
            try { column.setSamplesJson(objectMapper.writeValueAsString(definition.getSamples())); }
            catch (Exception e) { column.setSamplesJson("[]"); }
            columns.add(column);
        }
        mapper.insertColumns(columns);

        DataHubDataVersion version = new DataHubDataVersion();
        version.setDatasetId(dataset.getDatasetId());
        version.setSchemaId(schema.getSchemaId());
        version.setJobId(job.getJobId());
        version.setVersionNo(1);
        version.setVersionType("CREATE");
        version.setPhysicalTableName(targetTable);
        version.setRowCount((long) parsed.getRows().size());
        version.setStatus("ACTIVE");
        mapper.insertVersion(version);

        dataset.setCurrentSchemaId(schema.getSchemaId());
        dataset.setCurrentVersionId(version.getVersionId());
        dataset.setStatus("ACTIVE");
        mapper.publishDataset(dataset);
        if (request.getTargetFolderId() != null && request.getTargetFolderId() > 0)
            folderMapper.insertFolderItemIfOwnedActive(dataset.getDatasetId(), job.getUploadUserId(),
                    request.getTargetFolderId(), job.getUploadUserName());

        DataHubImportJob success = new DataHubImportJob();
        success.setJobId(job.getJobId());
        success.setLockVersion(job.getLockVersion());
        success.setDatasetId(dataset.getDatasetId());
        success.setStatus(DataHubImportStatus.SUCCESS.name());
        success.setPhase("导入完成");
        success.setTotalRows((long) parsed.getRows().size());
        success.setProcessedRows((long) parsed.getRows().size());
        success.setSuccessRows((long) parsed.getRows().size());
        success.setFailedRows(0L);
        success.setFinishTime(new Date());
        requireActive(mapper.updateImportJob(success));
    }

    private DataHubImportError error(Long jobId, int rowNo, DataHubColumnDefinition column, String raw, String message)
    {
        DataHubImportError error = new DataHubImportError();
        error.setJobId(jobId);
        error.setSourceRowNo(rowNo);
        error.setSourceColumnName(column.getSourceName());
        error.setPhysicalColumnName(column.getPhysicalName());
        error.setRawValue(raw != null && raw.length() > 2000 ? raw.substring(0, 2000) : raw);
        error.setErrorCode("TYPE_VALIDATION_FAILED");
        error.setErrorMessage(message == null ? "字段值不合法" : message);
        return error;
    }

    byte[] rowHash(MessageDigest digest, List<String> values)
    {
        digest.reset();
        for (String value : values)
        {
            byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
            digest.update(ByteBuffer.allocate(4).putInt(bytes.length).array());
            digest.update(bytes);
        }
        return digest.digest();
    }

    private String sha256(String value)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (Exception e)
        {
            throw new ServiceException("字段结构摘要生成失败");
        }
    }

    private void resetOwnedTables(String stagingTable, String targetTable)
    {
        safeDrop(stagingTable);
        if (mapper.countVersionByPhysicalTable(targetTable) > 0)
            throw new ServiceException("目标物理表已经被正式数据版本占用");
        safeDrop(targetTable);
    }

    private void cleanupPreviousTarget(String previousTarget, String currentTarget)
    {
        if (previousTarget == null || previousTarget.isBlank() || previousTarget.equals(currentTarget)) return;
        if (mapper.countVersionByPhysicalTable(previousTarget) > 0)
            throw new ServiceException("上一次目标表已经被正式数据版本占用");
        safeDrop(previousTarget);
    }

    private void cleanupPreviousStaging(String previousStaging, String currentStaging)
    {
        if (previousStaging == null || previousStaging.isBlank() || previousStaging.equals(currentStaging)) return;
        safeDrop(previousStaging);
    }

    private void updateProgress(Long jobId, Integer lockVersion, long processed)
    {
        DataHubImportJob progress = new DataHubImportJob();
        progress.setJobId(jobId);
        progress.setLockVersion(lockVersion);
        progress.setProcessedRows(processed);
        requireActive(mapper.updateImportJob(progress));
    }

    private void updateCounts(Long jobId, Integer lockVersion, Long total, Long processed, Long success, Long failed)
    {
        DataHubImportJob counts = new DataHubImportJob();
        counts.setJobId(jobId);
        counts.setLockVersion(lockVersion);
        counts.setTotalRows(total);
        counts.setProcessedRows(processed);
        counts.setSuccessRows(success);
        counts.setFailedRows(failed);
        requireActive(mapper.updateImportJob(counts));
    }

    private void mark(Long jobId, Integer lockVersion, DataHubImportStatus status, String phase, String error)
    {
        DataHubImportJob update = new DataHubImportJob();
        update.setJobId(jobId);
        update.setLockVersion(lockVersion);
        update.setStatus(status.name());
        update.setPhase(phase);
        if (error != null) update.setErrorMessage(error);
        if (status == DataHubImportStatus.VALIDATING) update.setStartTime(new Date());
        requireActive(mapper.updateImportJob(update));
    }

    private void finish(Long jobId, Integer lockVersion, DataHubImportStatus status)
    {
        DataHubImportJob update = new DataHubImportJob();
        update.setJobId(jobId);
        update.setLockVersion(lockVersion);
        update.setStatus(status.name());
        update.setFinishTime(new Date());
        requireActive(mapper.updateImportJob(update));
    }

    private void markFailed(Long jobId, Integer lockVersion, String message, Exception cause)
    {
        log.error("DataHub import job {} failed", jobId, cause);
        try
        {
            DataHubImportJob failed = new DataHubImportJob();
            failed.setJobId(jobId);
            failed.setLockVersion(lockVersion);
            failed.setStatus(DataHubImportStatus.FAILED.name());
            failed.setPhase("导入失败");
            failed.setErrorMessage(message);
            failed.setFinishTime(new Date());
            mapper.markImportJobFailed(failed);
        }
        catch (RuntimeException updateError)
        {
            log.error("Failed to persist DataHub job failure for job {}", jobId, updateError);
        }
    }

    private String stableErrorMessage(Exception e)
    {
        if (e instanceof ServiceException && e.getMessage() != null) return e.getMessage();
        return "导入执行失败，正式数据未发生变化";
    }

    private void requireActive(int updatedRows)
    {
        if (updatedRows != 1) throw new ServiceException("导入任务执行权已失效");
    }

    private void safeDrop(String tableName)
    {
        try { tableService.dropIfExists(tableName); }
        catch (RuntimeException e) { log.warn("Failed to clean DataHub table {}", tableName, e); }
    }

    static record PreparedResult(List<PreparedDataRow> rows, List<DataHubImportError> errors,
                                 Set<Integer> invalidRows) { }
}
