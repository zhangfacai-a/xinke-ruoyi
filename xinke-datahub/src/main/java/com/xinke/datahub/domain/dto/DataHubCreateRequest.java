package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubCreateRequest
{
    private String displayName;
    private String physicalName;
    private Long targetFolderId;
    private List<DataHubColumnDefinition> columns = new ArrayList<>();

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhysicalName() { return physicalName; }
    public void setPhysicalName(String physicalName) { this.physicalName = physicalName; }
    public Long getTargetFolderId() { return targetFolderId; }
    public void setTargetFolderId(Long targetFolderId) { this.targetFolderId = targetFolderId; }
    public List<DataHubColumnDefinition> getColumns() { return columns; }
    public void setColumns(List<DataHubColumnDefinition> columns) { this.columns = columns; }
}
