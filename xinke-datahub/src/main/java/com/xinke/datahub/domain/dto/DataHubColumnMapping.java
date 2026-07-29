package com.xinke.datahub.domain.dto;

public class DataHubColumnMapping
{
    private Integer sourceIndex;
    private Long targetColumnId;

    public Integer getSourceIndex() { return sourceIndex; }
    public void setSourceIndex(Integer sourceIndex) { this.sourceIndex = sourceIndex; }
    public Long getTargetColumnId() { return targetColumnId; }
    public void setTargetColumnId(Long targetColumnId) { this.targetColumnId = targetColumnId; }
}
