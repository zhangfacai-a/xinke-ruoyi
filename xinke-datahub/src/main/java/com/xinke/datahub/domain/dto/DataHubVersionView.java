package com.xinke.datahub.domain.dto;

import java.util.Date;
import com.xinke.datahub.domain.DataHubDataVersion;

public class DataHubVersionView
{
    private Long versionId;
    private Long parentVersionId;
    private Long jobId;
    private Integer versionNo;
    private String versionType;
    private Long rowCount;
    private String status;
    private Date retentionUntil;
    private Date createTime;
    private boolean current;

    public static DataHubVersionView from(DataHubDataVersion version, Long currentVersionId)
    {
        DataHubVersionView view = new DataHubVersionView();
        view.versionId = version.getVersionId();
        view.parentVersionId = version.getParentVersionId();
        view.jobId = version.getJobId();
        view.versionNo = version.getVersionNo();
        view.versionType = version.getVersionType();
        view.rowCount = version.getRowCount();
        view.status = version.getStatus();
        view.retentionUntil = version.getRetentionUntil();
        view.createTime = version.getCreateTime();
        view.current = version.getVersionId() != null && version.getVersionId().equals(currentVersionId);
        return view;
    }

    public Long getVersionId() { return versionId; }
    public Long getParentVersionId() { return parentVersionId; }
    public Long getJobId() { return jobId; }
    public Integer getVersionNo() { return versionNo; }
    public String getVersionType() { return versionType; }
    public Long getRowCount() { return rowCount; }
    public String getStatus() { return status; }
    public Date getRetentionUntil() { return retentionUntil; }
    public Date getCreateTime() { return createTime; }
    public boolean isCurrent() { return current; }
}
