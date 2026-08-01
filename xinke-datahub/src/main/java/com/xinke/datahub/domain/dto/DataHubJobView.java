package com.xinke.datahub.domain.dto;

import java.util.Date;
import com.xinke.datahub.domain.DataHubImportJob;

public class DataHubJobView
{
    private String previewId;
    private Long datasetId;
    private String operationType;
    private Long sourceVersionId;
    private Long targetVersionId;
    private Long rollbackTargetVersionId;
    private String status;
    private String phase;
    private String fileName;
    private String sheetName;
    private Long totalRows;
    private Long processedRows;
    private Long successRows;
    private Long failedRows;
    private String errorMessage;
    private Date startTime;
    private Date finishTime;
    private Date createTime;

    public static DataHubJobView from(DataHubImportJob job)
    {
        DataHubJobView view = new DataHubJobView();
        view.previewId = job.getPreviewId();
        view.datasetId = job.getDatasetId();
        view.operationType = job.getOperationType();
        view.sourceVersionId = job.getSourceVersionId();
        view.targetVersionId = job.getTargetVersionId();
        view.rollbackTargetVersionId = job.getRollbackTargetVersionId();
        view.status = job.getStatus();
        view.phase = job.getPhase();
        view.fileName = job.getSourceFileName();
        view.sheetName = job.getSheetName();
        view.totalRows = job.getTotalRows();
        view.processedRows = job.getProcessedRows();
        view.successRows = job.getSuccessRows();
        view.failedRows = job.getFailedRows();
        view.errorMessage = job.getErrorMessage();
        view.startTime = job.getStartTime();
        view.finishTime = job.getFinishTime();
        view.createTime = job.getCreateTime();
        return view;
    }

    public String getPreviewId() { return previewId; }
    public Long getDatasetId() { return datasetId; }
    public String getOperationType() { return operationType; }
    public Long getSourceVersionId() { return sourceVersionId; }
    public Long getTargetVersionId() { return targetVersionId; }
    public Long getRollbackTargetVersionId() { return rollbackTargetVersionId; }
    public String getStatus() { return status; }
    public String getPhase() { return phase; }
    public String getFileName() { return fileName; }
    public String getSheetName() { return sheetName; }
    public Long getTotalRows() { return totalRows; }
    public Long getProcessedRows() { return processedRows; }
    public Long getSuccessRows() { return successRows; }
    public Long getFailedRows() { return failedRows; }
    public String getErrorMessage() { return errorMessage; }
    public Date getStartTime() { return startTime; }
    public Date getFinishTime() { return finishTime; }
    public Date getCreateTime() { return createTime; }
}
