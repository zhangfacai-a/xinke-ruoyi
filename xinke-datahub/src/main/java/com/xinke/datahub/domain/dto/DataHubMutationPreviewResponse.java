package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;
import com.xinke.datahub.domain.DataHubColumn;

public class DataHubMutationPreviewResponse extends DataHubPreviewResponse
{
    private Long datasetId;
    private Long baseVersionId;
    private String operation;
    private List<DataHubColumn> targetColumns = new ArrayList<>();
    private List<DataHubColumnMapping> suggestedMappings = new ArrayList<>();

    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Long getBaseVersionId() { return baseVersionId; }
    public void setBaseVersionId(Long baseVersionId) { this.baseVersionId = baseVersionId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public List<DataHubColumn> getTargetColumns() { return targetColumns; }
    public void setTargetColumns(List<DataHubColumn> targetColumns) { this.targetColumns = targetColumns; }
    public List<DataHubColumnMapping> getSuggestedMappings() { return suggestedMappings; }
    public void setSuggestedMappings(List<DataHubColumnMapping> suggestedMappings) { this.suggestedMappings = suggestedMappings; }
}
