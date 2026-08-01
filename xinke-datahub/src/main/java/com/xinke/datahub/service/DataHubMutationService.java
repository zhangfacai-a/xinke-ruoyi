package com.xinke.datahub.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.constant.DataHubConstants;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubConfirmationRequest;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.domain.dto.DataHubEditCellValue;
import com.xinke.datahub.domain.dto.DataHubEditRequest;
import com.xinke.datahub.domain.dto.DataHubJobView;
import com.xinke.datahub.domain.dto.DataHubMutationConfirmRequest;
import com.xinke.datahub.domain.dto.DataHubMutationPreviewResponse;
import com.xinke.datahub.domain.dto.DataHubRowMutation;
import com.xinke.datahub.domain.dto.DataHubVersionView;
import com.xinke.datahub.enums.DataHubImportStatus;
import com.xinke.datahub.enums.DataHubOperationType;
import com.xinke.datahub.mapper.DataHubMapper;
import com.xinke.datahub.mapper.DataHubMutationMapper;
import com.xinke.datahub.parser.ParsedSpreadsheet;
import com.xinke.datahub.parser.SpreadsheetParser;
import com.xinke.datahub.storage.DataHubStorageService;
import com.xinke.datahub.storage.StoredDataHubFile;

@Service
public class DataHubMutationService
{
    private static final Logger log = LoggerFactory.getLogger(DataHubMutationService.class);
    private static final Pattern CLIENT_MUTATION_ID = Pattern.compile("[A-Za-z0-9._:-]{1,64}");
    private static final Pattern ROW_HASH = Pattern.compile("[0-9a-fA-F]{64}");

    private final DataHubMutationMapper mutationMapper;
    private final DataHubMapper mapper;
    private final DataHubStorageService storageService;
    private final SpreadsheetParser parser;
    private final DataHubMutationSchemaService schemaService;
    private final DataHubDynamicTableService tableService;
    private final DataHubImportService importService;
    private final DataHubProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    public DataHubMutationService(DataHubMutationMapper mutationMapper, DataHubMapper mapper,
            DataHubStorageService storageService, SpreadsheetParser parser,
            DataHubMutationSchemaService schemaService, DataHubDynamicTableService tableService,
            DataHubImportService importService, DataHubProperties properties, ObjectMapper objectMapper,
            PlatformTransactionManager transactionManager)
    {
        this.mutationMapper = mutationMapper;
        this.mapper = mapper;
        this.storageService = storageService;
        this.parser = parser;
        this.schemaService = schemaService;
        this.tableService = tableService;
        this.importService = importService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public DataHubMutationPreviewResponse previewFile(Long datasetId, String operation, Long baseVersionId,
            MultipartFile file, String sheetName, Long userId, String username, boolean admin)
    {
        DataHubOperationType type = fileOperation(operation);
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(dataset, userId, admin, requiredFileAccess(type));
        DataHubDataVersion version = requireCurrentVersion(dataset, baseVersionId);

        String jobNo = jobNo();
        String previewId = previewId();
        String fileName = safeFileName(file == null ? null : file.getOriginalFilename());
        StoredDataHubFile stored = storageService.save(file, jobNo);
        Date expiry = Date.from(Instant.now().plusSeconds(properties.getPreviewExpireHours() * 3600L));
        DataHubImportJob job = new DataHubImportJob();
        job.setJobNo(jobNo);
        job.setPreviewId(previewId);
        job.setDatasetId(datasetId);
        job.setOperationType(type.name());
        job.setSourceVersionId(version.getVersionId());
        job.setSourceLockVersion(dataset.getLockVersion());
        job.setStatus(DataHubImportStatus.PARSING.name());
        job.setPhase("解析预览");
        job.setSourceFileName(fileName);
        job.setStoredFilePath(stored.path().toString());
        job.setFileType(stored.extension().toUpperCase(Locale.ROOT));
        job.setFileHash(stored.hash());
        job.setUploadUserId(userId);
        job.setUploadUserName(username);
        job.setProposedDisplayName(dataset.getDisplayName());
        job.setProposedPhysicalName(dataset.getDatasetCode());
        job.setExpireTime(expiry);
        try
        {
            mutationMapper.insertMutationJob(job);
        }
        catch (RuntimeException e)
        {
            storageService.deleteStoredFile(stored.path().toString());
            throw e;
        }

        try
        {
            return buildAndStorePreview(job, dataset, version, sheetName);
        }
        catch (RuntimeException e)
        {
            mutationMapper.markMutationPreviewFailed(job.getJobId(), stableMessage(e), new Date());
            throw e;
        }
    }

    public DataHubMutationPreviewResponse changeSheet(Long datasetId, String previewId, String sheetName,
            Long userId, boolean admin)
    {
        DataHubImportJob job = requireJob(datasetId, previewId, userId, admin);
        requireEditablePreview(job);
        DataHubDataset dataset = requireDataset(datasetId);
        DataHubOperationType type = fileOperation(job.getOperationType());
        requireAccess(dataset, userId, admin, requiredFileAccess(type));
        DataHubDataVersion version = requireCurrentVersion(dataset, job.getSourceVersionId());
        return buildAndStorePreview(job, dataset, version, sheetName);
    }

    public String previewOperation(Long datasetId, String previewId, Long userId, boolean admin)
    {
        return fileOperation(requireJob(datasetId, previewId, userId, admin).getOperationType()).name();
    }

    public DataHubJobView confirmFile(Long datasetId, String previewId, DataHubMutationConfirmRequest request,
            Long userId, boolean admin)
    {
        if (request == null || request.getBaseVersionId() == null) throw new ServiceException("缺少基础版本");
        DataHubImportJob job = requireJob(datasetId, previewId, userId, admin);
        requireEditablePreview(job);
        DataHubOperationType type = fileOperation(job.getOperationType());
        if (!request.getBaseVersionId().equals(job.getSourceVersionId())) throw conflict("预览基础版本不一致，请重新预览");
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(dataset, userId, admin, requiredFileAccess(type));
        if (type == DataHubOperationType.REPLACE)
        {
            String confirmation = request.getConfirmationName() == null ? "" : request.getConfirmationName().strip();
            if (!dataset.getDisplayName().equals(confirmation)) throw new ServiceException("覆盖确认名称不匹配");
        }
        DataHubDataVersion version = requireCurrentVersion(dataset, request.getBaseVersionId());
        String sheetName = request.getSheetName() == null || request.getSheetName().isBlank()
                ? job.getSheetName() : request.getSheetName().strip();
        if (!sheetName.equals(job.getSheetName())) throw new ServiceException("请先切换Sheet并重新预览");

        DataHubMutationPreviewResponse preview = readPreview(job);
        requireDatasetCapacity(type, version, preview.getTotalRows());
        ParsedSpreadsheet source = previewSource(preview);
        List<DataHubColumn> columns = mutationMapper.selectSchemaColumns(datasetId, version.getSchemaId());
        DataHubCreateRequest locked = schemaService.lockedRequest(dataset, columns, source, request.getMappings());
        String schemaJson = writeJson(locked, "字段映射保存失败");

        transactionTemplate.executeWithoutResult(status -> {
            DataHubDataset current = requireDataset(datasetId);
            requireCurrentVersion(current, job.getSourceVersionId());
            if (mutationMapper.acquireDatasetJob(datasetId, job.getJobId(), job.getSourceVersionId(),
                    current.getLockVersion()) != 1) throw conflict("数据表正在变更或版本已更新，请刷新后重试");
            if (mutationMapper.queueMutationJob(job.getJobId(), userId, job.getSourceVersionId(),
                    current.getLockVersion(), sheetName, schemaJson) != 1)
                throw conflict("预览已过期或任务状态已经变化，请重新上传");
        });
        importService.dispatchQueued(previewId);
        return DataHubJobView.from(mutationMapper.selectMutationJob(previewId));
    }

    public DataHubJobView edit(Long datasetId, DataHubEditRequest request, Long userId, String username, boolean admin)
    {
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(dataset, userId, admin, DataHubConstants.ACCESS_EDIT);
        DataHubDataVersion version = requireCurrentVersion(dataset, request == null ? null : request.getBaseVersionId());
        List<DataHubColumn> columns = mutationMapper.selectSchemaColumns(datasetId, version.getSchemaId());
        validateEdit(request, columns);
        return createImmediateJob(dataset, version, DataHubOperationType.EDIT, writeJson(request, "编辑请求保存失败"),
                null, userId, username);
    }

    public DataHubJobView clear(Long datasetId, DataHubConfirmationRequest request, Long userId,
            String username, boolean admin)
    {
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(dataset, userId, admin, DataHubConstants.ACCESS_MANAGE);
        confirmName(dataset, request);
        DataHubDataVersion version = requireCurrentVersion(dataset, request.getBaseVersionId());
        return createImmediateJob(dataset, version, DataHubOperationType.CLEAR, null, null, userId, username);
    }

    public DataHubJobView rollback(Long datasetId, Long versionId, DataHubConfirmationRequest request,
            Long userId, String username, boolean admin)
    {
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(dataset, userId, admin, DataHubConstants.ACCESS_MANAGE);
        confirmName(dataset, request);
        DataHubDataVersion source = requireCurrentVersion(dataset, request.getBaseVersionId());
        DataHubDataVersion target = mutationMapper.selectDatasetVersion(datasetId, versionId);
        if (target == null || !List.of("ACTIVE", "ARCHIVED").contains(target.getStatus()))
            throw new ServiceException("回滚版本不存在、已清理或不可用");
        if (source.getVersionId().equals(target.getVersionId())) throw conflict("该版本已经是当前版本");
        tableService.requireTable(target.getPhysicalTableName());
        return createImmediateJob(dataset, source, DataHubOperationType.ROLLBACK, null,
                target.getVersionId(), userId, username);
    }

    public List<DataHubVersionView> versions(Long datasetId, Long userId, boolean admin)
    {
        DataHubDataset dataset = requireDataset(datasetId);
        requireAccess(dataset, userId, admin, DataHubConstants.ACCESS_READ);
        List<DataHubVersionView> views = new ArrayList<>();
        for (DataHubDataVersion version : mutationMapper.selectDatasetVersions(datasetId))
            views.add(DataHubVersionView.from(version, dataset.getCurrentVersionId()));
        return views;
    }

    @Scheduled(initialDelay = 300000L, fixedDelay = 3600000L)
    public void purgeExpiredVersions()
    {
        try
        {
            reconcileInterruptedPurges();
            for (DataHubDataVersion version : mutationMapper.selectPurgeCandidates(20))
            {
                if (mutationMapper.claimVersionForPurge(version.getVersionId()) != 1) continue;
                try
                {
                    tableService.dropIfExists(version.getPhysicalTableName());
                    mutationMapper.markVersionPurged(version.getVersionId());
                }
                catch (RuntimeException e)
                {
                    log.warn("Failed to purge DataHub version {}", version.getVersionId(), e);
                }
            }
        }
        catch (RuntimeException e)
        {
            log.error("DataHub version retention cleanup failed", e);
        }
    }

    private void reconcileInterruptedPurges()
    {
        int staleMinutes = Math.max(10, properties.getRecoveryStaleMinutes());
        Date staleBefore = Date.from(Instant.now().minusSeconds(staleMinutes * 60L));
        for (DataHubDataVersion version : mutationMapper.selectStalePurgingVersions(staleBefore, 20))
        {
            if (mutationMapper.reclaimStaleVersionForPurge(version.getVersionId(), staleBefore) != 1) continue;
            try
            {
                tableService.dropIfExists(version.getPhysicalTableName());
                mutationMapper.markVersionPurged(version.getVersionId());
            }
            catch (RuntimeException e)
            {
                log.warn("Failed to reconcile interrupted DataHub purge for version {}", version.getVersionId(), e);
            }
        }
    }

    private DataHubMutationPreviewResponse buildAndStorePreview(DataHubImportJob job, DataHubDataset dataset,
            DataHubDataVersion version, String sheetName)
    {
        ParsedSpreadsheet parsed = parser.parse(storageService.resolve(job.getStoredFilePath()),
                job.getSourceFileName(), sheetName);
        requireDatasetCapacity(fileOperation(job.getOperationType()), version, parsed.getRows().size());
        List<DataHubColumn> columns = mutationMapper.selectSchemaColumns(dataset.getDatasetId(), version.getSchemaId());
        DataHubMutationPreviewResponse response = schemaService.preview(job.getPreviewId(), job.getSourceFileName(),
                job.getOperationType(), dataset, version, columns, parsed, job.getExpireTime(), dictionary());
        job.setSheetName(parsed.getSheetName());
        job.setTotalRows((long) parsed.getRows().size());
        job.setOperationPayloadJson(writeJson(response, "预览结果保存失败"));
        if (mutationMapper.updateMutationPreview(job) != 1) throw conflict("预览状态已经变化，请重新上传");
        return response;
    }

    private DataHubJobView createImmediateJob(DataHubDataset dataset, DataHubDataVersion source,
            DataHubOperationType operation, String payloadJson, Long rollbackTargetVersionId,
            Long userId, String username)
    {
        DataHubImportJob job = new DataHubImportJob();
        job.setJobNo(jobNo());
        job.setPreviewId(previewId());
        job.setDatasetId(dataset.getDatasetId());
        job.setOperationType(operation.name());
        job.setSourceVersionId(source.getVersionId());
        job.setSourceLockVersion(dataset.getLockVersion());
        job.setRollbackTargetVersionId(rollbackTargetVersionId);
        job.setStatus(DataHubImportStatus.QUEUED.name());
        job.setPhase("等待执行");
        job.setOperationPayloadJson(payloadJson);
        job.setTotalRows(operation == DataHubOperationType.EDIT
                ? (long) readEdit(payloadJson).getMutations().size() : operation == DataHubOperationType.ROLLBACK ? 1L : 0L);
        job.setUploadUserId(userId);
        job.setUploadUserName(username);
        transactionTemplate.executeWithoutResult(status -> {
            mutationMapper.insertMutationJob(job);
            if (mutationMapper.acquireDatasetJob(dataset.getDatasetId(), job.getJobId(), source.getVersionId(),
                    dataset.getLockVersion()) != 1) throw conflict("数据表正在变更或版本已更新，请刷新后重试");
        });
        importService.dispatchQueued(job.getPreviewId());
        return DataHubJobView.from(mutationMapper.selectMutationJob(job.getPreviewId()));
    }

    private void validateEdit(DataHubEditRequest request, List<DataHubColumn> columns)
    {
        if (request == null || request.getMutations() == null || request.getMutations().isEmpty())
            throw new ServiceException("至少需要提交一条数据变更");
        if (request.getMutations().size() > Math.max(1, properties.getMaxEditMutations()))
            throw new ServiceException("单次编辑不能超过" + properties.getMaxEditMutations() + "条");
        Set<Long> columnIds = new HashSet<>();
        for (DataHubColumn column : columns) columnIds.add(column.getColumnId());
        Set<String> mutationIds = new HashSet<>();
        Set<Long> rowIds = new HashSet<>();
        for (DataHubRowMutation mutation : request.getMutations())
        {
            if (mutation == null || mutation.getClientMutationId() == null
                    || !CLIENT_MUTATION_ID.matcher(mutation.getClientMutationId()).matches()
                    || !mutationIds.add(mutation.getClientMutationId()))
                throw new ServiceException("clientMutationId缺失、重复或格式不合法");
            String operation = mutation.getOperation() == null ? "" : mutation.getOperation().toUpperCase(Locale.ROOT);
            if (!Set.of("INSERT", "UPDATE", "DELETE").contains(operation)) throw new ServiceException("不支持的数据编辑操作");
            mutation.setOperation(operation);
            if ("INSERT".equals(operation))
            {
                if (mutation.getRowId() != null || mutation.getExpectedRowHash() != null)
                    throw new ServiceException("新增数据不能指定rowId或expectedRowHash");
            }
            else
            {
                if (mutation.getRowId() == null || mutation.getRowId() <= 0 || !rowIds.add(mutation.getRowId()))
                    throw new ServiceException("编辑或删除的rowId缺失或重复");
                if (mutation.getExpectedRowHash() == null || !ROW_HASH.matcher(mutation.getExpectedRowHash()).matches())
                    throw new ServiceException("expectedRowHash格式不合法，请刷新数据后重试");
                mutation.setExpectedRowHash(mutation.getExpectedRowHash().toLowerCase(Locale.ROOT));
            }
            List<DataHubEditCellValue> values = mutation.getValues() == null ? List.of() : mutation.getValues();
            if ("DELETE".equals(operation) && !values.isEmpty()) throw new ServiceException("删除操作不能包含字段值");
            if (!"DELETE".equals(operation) && values.isEmpty()) throw new ServiceException("新增或编辑操作必须包含字段值");
            Set<Long> valueColumns = new HashSet<>();
            for (DataHubEditCellValue value : values)
            {
                if (value == null || value.getColumnId() == null || !columnIds.contains(value.getColumnId())
                        || !valueColumns.add(value.getColumnId())) throw new ServiceException("编辑字段不存在或重复");
                if (Boolean.TRUE.equals(value.getIsNull())) value.setValue(null);
            }
        }
    }

    private void confirmName(DataHubDataset dataset, DataHubConfirmationRequest request)
    {
        if (request == null || request.getBaseVersionId() == null) throw new ServiceException("缺少基础版本");
        String name = request.getConfirmationName() == null ? "" : request.getConfirmationName().strip();
        if (!dataset.getDisplayName().equals(name)) throw new ServiceException("确认名称不匹配");
    }

    private DataHubImportJob requireJob(Long datasetId, String previewId, Long userId, boolean admin)
    {
        DataHubImportJob job = mutationMapper.selectMutationJob(previewId);
        if (job == null || !datasetId.equals(job.getDatasetId())
                || (!admin && !userId.equals(job.getUploadUserId())))
            throw new ServiceException("变更任务不存在或无权访问");
        return job;
    }

    private void requireEditablePreview(DataHubImportJob job)
    {
        if (job.getExpireTime() == null || job.getExpireTime().before(new Date())) throw new ServiceException("预览已过期，请重新上传");
        if (!List.of(DataHubImportStatus.PENDING_CONFIRM.name(), DataHubImportStatus.VALIDATION_FAILED.name(),
                DataHubImportStatus.FAILED.name()).contains(job.getStatus()))
            throw conflict("当前任务状态不允许修改或确认");
    }

    private DataHubDataset requireDataset(Long datasetId)
    {
        DataHubDataset dataset = datasetId == null ? null : mutationMapper.selectMutationDataset(datasetId);
        if (dataset == null) throw new ServiceException("数据表不存在");
        return dataset;
    }

    private DataHubDataVersion requireCurrentVersion(DataHubDataset dataset, Long versionId)
    {
        if (versionId == null || !versionId.equals(dataset.getCurrentVersionId()))
            throw conflict("数据表版本已更新，请刷新后重试");
        DataHubDataVersion version = mutationMapper.selectDatasetVersion(dataset.getDatasetId(), versionId);
        if (version == null || !"ACTIVE".equals(version.getStatus())) throw conflict("当前数据版本不可用，请刷新后重试");
        return version;
    }

    private void requireAccess(DataHubDataset dataset, Long userId, boolean admin, int required)
    {
        if (admin || userId.equals(dataset.getOwnerUserId())) return;
        Integer mask = mapper.selectAccessMask(dataset.getDatasetId(), userId);
        if (mask == null || (mask & required) == 0) throw new ServiceException("无权执行该数据表操作");
    }

    private DataHubOperationType fileOperation(String operation)
    {
        try
        {
            DataHubOperationType type = DataHubOperationType.valueOf(operation == null ? "" : operation.toUpperCase(Locale.ROOT));
            if (type != DataHubOperationType.APPEND && type != DataHubOperationType.REPLACE) throw new IllegalArgumentException();
            return type;
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException("文件变更仅支持APPEND或REPLACE");
        }
    }

    private int requiredFileAccess(DataHubOperationType operation)
    {
        return operation == DataHubOperationType.REPLACE
                ? DataHubConstants.ACCESS_MANAGE : DataHubConstants.ACCESS_IMPORT;
    }

    private void requireDatasetCapacity(DataHubOperationType operation, DataHubDataVersion source, long importedRows)
    {
        long limit = Math.max(1L, properties.getMaxDatasetRows());
        if (operation == DataHubOperationType.REPLACE)
        {
            if (importedRows < 0 || importedRows > limit)
                throw new ServiceException("覆盖后数据行数不能超过" + limit + "行");
            return;
        }
        if (operation != DataHubOperationType.APPEND) return;
        long sourceRows = source.getRowCount() == null ? 0L : source.getRowCount();
        if (importedRows < 0 || sourceRows > limit - importedRows)
            throw new ServiceException("追加后数据行数不能超过" + limit + "行");
    }

    private DataHubMutationPreviewResponse readPreview(DataHubImportJob job)
    {
        try { return objectMapper.readValue(job.getOperationPayloadJson(), DataHubMutationPreviewResponse.class); }
        catch (Exception e) { throw new ServiceException("预览结果不可用，请重新上传"); }
    }

    private ParsedSpreadsheet previewSource(DataHubMutationPreviewResponse preview)
    {
        List<DataHubColumnDefinition> definitions = new ArrayList<>(preview.getColumns());
        definitions.sort((left, right) -> Integer.compare(left.getSourceIndex(), right.getSourceIndex()));
        ParsedSpreadsheet parsed = new ParsedSpreadsheet();
        List<String> headers = new ArrayList<>();
        for (int index = 0; index < definitions.size(); index++)
        {
            DataHubColumnDefinition definition = definitions.get(index);
            if (definition.getSourceIndex() == null || definition.getSourceIndex() != index)
                throw new ServiceException("预览字段不完整，请重新上传");
            headers.add(definition.getSourceName());
        }
        parsed.setHeaders(headers);
        parsed.setSheetName(preview.getSheetName());
        parsed.setSheetNames(preview.getSheetNames());
        return parsed;
    }

    private DataHubEditRequest readEdit(String payload)
    {
        try { return objectMapper.readValue(payload, DataHubEditRequest.class); }
        catch (Exception e) { throw new ServiceException("编辑请求不可读取"); }
    }

    private Map<String, String> dictionary()
    {
        Map<String, String> values = new HashMap<>();
        for (Map<String, String> row : mapper.selectNameDictionary())
        {
            Object source = row.get("sourceName");
            if (source == null) source = row.get("source_name");
            Object english = row.get("englishName");
            if (english == null) english = row.get("english_name");
            if (source != null && english != null) values.put(String.valueOf(source), String.valueOf(english));
        }
        return values;
    }

    private String writeJson(Object value, String message)
    {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception e) { throw new ServiceException(message); }
    }

    private ServiceException conflict(String message)
    {
        return new ServiceException(message, HttpStatus.CONFLICT.value());
    }

    private String jobNo()
    {
        return "DH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private String previewId()
    {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String safeFileName(String name)
    {
        if (name == null || name.isBlank()) return "未命名文件";
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).strip();
        if (normalized.length() <= 255) return normalized;
        int dot = normalized.lastIndexOf('.');
        String suffix = dot >= 0 && normalized.length() - dot <= 10 ? normalized.substring(dot) : "";
        return normalized.substring(0, 255 - suffix.length()) + suffix;
    }

    private String stableMessage(RuntimeException error)
    {
        String message = error.getMessage();
        if (message == null || message.isBlank()) return "表格处理失败";
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
