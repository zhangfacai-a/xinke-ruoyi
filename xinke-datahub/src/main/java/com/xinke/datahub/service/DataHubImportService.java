package com.xinke.datahub.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.DataHubImportError;
import com.xinke.datahub.domain.DataHubImportJob;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.domain.dto.DataHubJobView;
import com.xinke.datahub.domain.dto.DataHubPreviewResponse;
import com.xinke.datahub.enums.DataHubImportStatus;
import com.xinke.datahub.enums.DataHubOperationType;
import com.xinke.datahub.mapper.DataHubMapper;
import com.xinke.datahub.naming.EnglishNameGenerator;
import com.xinke.datahub.parser.ParsedSpreadsheet;
import com.xinke.datahub.parser.SpreadsheetParser;
import com.xinke.datahub.storage.DataHubStorageService;
import com.xinke.datahub.storage.StoredDataHubFile;

@Service
public class DataHubImportService
{
    private static final Logger log = LoggerFactory.getLogger(DataHubImportService.class);

    private final DataHubMapper mapper;
    private final DataHubStorageService storageService;
    private final SpreadsheetParser parser;
    private final DataHubSchemaInferenceService inferenceService;
    private final DataHubDefinitionValidator validator;
    private final EnglishNameGenerator nameGenerator;
    private final ObjectMapper objectMapper;
    private final DataHubImportWorker worker;
    private final DataHubMutationWorker mutationWorker;
    private final DataHubDynamicTableService tableService;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final DataHubProperties properties;

    public DataHubImportService(DataHubMapper mapper, DataHubStorageService storageService, SpreadsheetParser parser,
            DataHubSchemaInferenceService inferenceService, DataHubDefinitionValidator validator,
            EnglishNameGenerator nameGenerator, ObjectMapper objectMapper, DataHubImportWorker worker,
            DataHubMutationWorker mutationWorker, DataHubDynamicTableService tableService,
            @Qualifier("dataHubTaskExecutor") ThreadPoolTaskExecutor taskExecutor, DataHubProperties properties)
    {
        this.mapper = mapper;
        this.storageService = storageService;
        this.parser = parser;
        this.inferenceService = inferenceService;
        this.validator = validator;
        this.nameGenerator = nameGenerator;
        this.objectMapper = objectMapper;
        this.worker = worker;
        this.mutationWorker = mutationWorker;
        this.tableService = tableService;
        this.taskExecutor = taskExecutor;
        this.properties = properties;
    }

    public DataHubPreviewResponse preview(MultipartFile file, String sheetName, Long userId, String username)
    {
        String fileName = safeFileName(file == null ? null : file.getOriginalFilename());
        String jobNo = "DH" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        String previewId = UUID.randomUUID().toString().replace("-", "");
        StoredDataHubFile stored = storageService.save(file, jobNo);
        Date expiry = Date.from(Instant.now().plusSeconds(properties.getPreviewExpireHours() * 3600L));

        DataHubImportJob job = new DataHubImportJob();
        job.setJobNo(jobNo);
        job.setPreviewId(previewId);
        job.setOperationType(DataHubOperationType.CREATE.name());
        job.setStatus(DataHubImportStatus.PARSING.name());
        job.setPhase("解析预览");
        job.setSourceFileName(fileName);
        job.setStoredFilePath(stored.path().toString());
        job.setFileType(stored.extension().toUpperCase(Locale.ROOT));
        job.setFileHash(stored.hash());
        job.setUploadUserId(userId);
        job.setUploadUserName(username);
        job.setExpireTime(expiry);
        try
        {
            mapper.insertImportJob(job);
        }
        catch (RuntimeException e)
        {
            try { storageService.deleteStoredFile(stored.path().toString()); }
            catch (RuntimeException cleanupError) { log.warn("Failed to clean unregistered DataHub upload", cleanupError); }
            throw e;
        }

        try
        {
            ParsedSpreadsheet parsed = parser.parse(stored.path(), fileName, sheetName);
            DataHubPreviewResponse response = inferenceService.buildPreview(previewId, fileName, parsed, expiry, dictionary());
            DataHubImportJob update = new DataHubImportJob();
            update.setJobId(job.getJobId());
            update.setStatus(DataHubImportStatus.PENDING_CONFIRM.name());
            update.setPhase("等待确认");
            update.setSheetName(parsed.getSheetName());
            update.setProposedDisplayName(response.getDisplayName());
            update.setProposedPhysicalName(response.getPhysicalName());
            update.setTotalRows((long) parsed.getRows().size());
            mapper.updateImportJob(update);
            return response;
        }
        catch (RuntimeException e)
        {
            failPreview(job.getJobId(), e.getMessage());
            throw e;
        }
    }

    public DataHubPreviewResponse changeSheet(String previewId, String sheetName, Long userId, boolean admin)
    {
        DataHubImportJob job = requireJob(previewId, userId, admin);
        requireEditablePreview(job);
        ParsedSpreadsheet parsed = parser.parse(storageService.resolve(job.getStoredFilePath()), job.getSourceFileName(), sheetName);
        DataHubPreviewResponse response = inferenceService.buildPreview(previewId, job.getSourceFileName(), parsed,
                job.getExpireTime(), dictionary());
        DataHubImportJob update = new DataHubImportJob();
        update.setJobId(job.getJobId());
        update.setStatus(DataHubImportStatus.PENDING_CONFIRM.name());
        update.setPhase("等待确认");
        update.setSheetName(parsed.getSheetName());
        update.setProposedDisplayName(response.getDisplayName());
        update.setProposedPhysicalName(response.getPhysicalName());
        update.setTotalRows((long) parsed.getRows().size());
        update.setErrorMessage("");
        if (mapper.updatePreviewSheet(update) != 1)
            throw new ServiceException("预览状态已经变化，请刷新后重试");
        return response;
    }

    public DataHubJobView confirmCreate(String previewId, DataHubCreateRequest request, Long userId)
    {
        validator.validate(request);
        DataHubImportJob job = requireJob(previewId, userId, false);
        requireEditablePreview(job);
        String nameKey = nameGenerator.normalizeNameKey(request.getDisplayName());
        if (mapper.countDatasetByNameKey(nameKey) > 0) throw new ServiceException("数据表名称已经存在");
        if (mapper.countDatasetByCode(request.getPhysicalName()) > 0) throw new ServiceException("英文表名已经存在");
        try
        {
            request.setDisplayName(request.getDisplayName().strip());
            request.setPhysicalName(request.getPhysicalName().strip());
            String schemaJson = objectMapper.writeValueAsString(request);
            if (mapper.queueImportJob(previewId, userId, request.getDisplayName(), request.getPhysicalName(), schemaJson,
                    job.getSheetName()) != 1)
                throw new ServiceException("预览已过期或任务状态已变化，请重新上传");
            dispatch(previewId);
            return DataHubJobView.from(mapper.selectJobByPreviewId(previewId));
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("导入配置保存失败");
        }
    }

    public DataHubJobView getJob(String previewId, Long userId, boolean admin)
    {
        return DataHubJobView.from(requireJob(previewId, userId, admin));
    }

    public List<DataHubImportError> getErrors(String previewId, Long userId, boolean admin)
    {
        DataHubImportJob job = requireJob(previewId, userId, admin);
        return mapper.selectImportErrors(job.getJobId());
    }

    public boolean dispatchQueued(String previewId)
    {
        return dispatch(previewId);
    }

    @Scheduled(initialDelay = 5000L, fixedDelay = 30000L)
    public void recoverAndDispatch()
    {
        try
        {
            int staleMinutes = Math.max(10, properties.getRecoveryStaleMinutes());
            Date staleBefore = Date.from(Instant.now().minusSeconds(staleMinutes * 60L));
            int recovered = mapper.requeueStaleImportJobs(staleBefore);
            int interruptedPreviews = mapper.failStaleParsingJobs(staleBefore);
            if (recovered > 0 || interruptedPreviews > 0)
                log.warn("Recovered {} DataHub jobs and closed {} interrupted previews", recovered, interruptedPreviews);
            int batchSize = Math.min(100, Math.max(1, properties.getDispatchBatchSize()));
            for (String previewId : mapper.selectQueuedPreviewIds(batchSize)) dispatch(previewId);
        }
        catch (RuntimeException e)
        {
            log.error("DataHub queued job recovery failed", e);
        }
    }

    @Scheduled(initialDelay = 60000L, fixedDelay = 3600000L)
    public void cleanupExpiredFiles()
    {
        try
        {
            for (DataHubImportJob job : mapper.selectExpiredStoredFiles(100))
            {
                try
                {
                    cleanupExpiredTables(job);
                    storageService.deleteStoredFile(job.getStoredFilePath());
                    mapper.clearStoredFilePath(job.getJobId());
                }
                catch (RuntimeException e)
                {
                    log.warn("Failed to clean expired DataHub source file for job {}", job.getJobId(), e);
                }
            }
        }
        catch (RuntimeException e)
        {
            log.error("DataHub expired file cleanup failed", e);
        }
    }

    private boolean dispatch(String previewId)
    {
        if (mapper.claimQueuedImportJob(previewId) != 1) return false;
        DataHubImportJob claimed = mapper.selectJobByPreviewId(previewId);
        if (claimed == null || claimed.getLockVersion() == null)
            throw new ServiceException("导入任务领取失败");
        Integer lockVersion = claimed.getLockVersion();
        try
        {
            taskExecutor.execute(DataHubOperationType.CREATE.name().equals(claimed.getOperationType())
                    ? () -> worker.executeCreate(previewId, lockVersion)
                    : () -> mutationWorker.execute(previewId, lockVersion));
            return true;
        }
        catch (RuntimeException e)
        {
            mapper.requeueClaimedImportJob(previewId, lockVersion);
            if (e instanceof TaskRejectedException)
                log.info("DataHub executor is full; job {} remains queued", previewId);
            else
                log.error("DataHub job dispatch failed; job remains queued: " + previewId, e);
            return false;
        }
    }

    private void cleanupExpiredTables(DataHubImportJob job)
    {
        if (!List.of(DataHubImportStatus.FAILED.name(), DataHubImportStatus.VALIDATION_FAILED.name(),
                DataHubImportStatus.MANUAL_REQUIRED.name()).contains(job.getStatus())) return;
        if (job.getStagingTableName() != null && !job.getStagingTableName().isBlank())
            tableService.dropIfExists(job.getStagingTableName());
        if (job.getTargetTableName() != null && !job.getTargetTableName().isBlank()
                && mapper.countVersionByPhysicalTable(job.getTargetTableName()) == 0)
            tableService.dropIfExists(job.getTargetTableName());
    }

    private DataHubImportJob requireJob(String previewId, Long userId, boolean admin)
    {
        DataHubImportJob job = mapper.selectJobByPreviewId(previewId);
        if (job == null || (!admin && !userId.equals(job.getUploadUserId())))
            throw new ServiceException("导入任务不存在或无权访问");
        return job;
    }

    private void requireEditablePreview(DataHubImportJob job)
    {
        if (job.getExpireTime() == null || job.getExpireTime().before(new Date())) throw new ServiceException("预览已过期，请重新上传");
        if (!List.of(DataHubImportStatus.PENDING_CONFIRM.name(), DataHubImportStatus.VALIDATION_FAILED.name(),
                DataHubImportStatus.FAILED.name()).contains(job.getStatus()))
            throw new ServiceException("当前任务状态不允许修改或确认");
    }

    private Map<String, String> dictionary()
    {
        Map<String, String> values = new HashMap<>();
        for (Map<String, String> row : mapper.selectNameDictionary())
        {
            String source = mapValue(row, "sourceName", "source_name");
            String english = mapValue(row, "englishName", "english_name");
            if (source != null && english != null) values.put(source, english);
        }
        return values;
    }

    private String mapValue(Map<String, String> row, String camel, String snake)
    {
        Object value = row.get(camel);
        if (value == null) value = row.get(snake);
        if (value == null) value = row.get(camel.toUpperCase(Locale.ROOT));
        return value == null ? null : String.valueOf(value);
    }

    private void failPreview(Long jobId, String message)
    {
        DataHubImportJob failed = new DataHubImportJob();
        failed.setJobId(jobId);
        failed.setStatus(DataHubImportStatus.FAILED.name());
        failed.setPhase("预览失败");
        failed.setErrorMessage(message == null ? "表格处理失败" : truncate(message, 1000));
        failed.setFinishTime(new Date());
        mapper.updateImportJob(failed);
    }

    private String safeFileName(String name)
    {
        if (name == null || name.isBlank()) return "未命名文件";
        String normalized = name.replace('\\', '/');
        normalized = normalized.substring(normalized.lastIndexOf('/') + 1).strip();
        if (normalized.isBlank()) return "未命名文件";
        if (normalized.length() <= 255) return normalized;
        int dot = normalized.lastIndexOf('.');
        String suffix = dot >= 0 && normalized.length() - dot <= 10 ? normalized.substring(dot) : "";
        return normalized.substring(0, 255 - suffix.length()) + suffix;
    }

    private String truncate(String value, int length)
    {
        return value.length() <= length ? value : value.substring(0, length);
    }
}
