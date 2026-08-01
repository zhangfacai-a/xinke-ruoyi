package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubColumnDefinition
{
    private Long targetColumnId;
    private Integer sourceIndex;
    private String sourceName;
    private String displayName;
    private String physicalName;
    private String dataType;
    private Integer length;
    private Integer precision;
    private Integer scale;
    private Boolean nullable = Boolean.TRUE;
    private Boolean needsReview = Boolean.FALSE;
    private String translationSource;
    private List<String> samples = new ArrayList<>();

    public Long getTargetColumnId() { return targetColumnId; }
    public void setTargetColumnId(Long targetColumnId) { this.targetColumnId = targetColumnId; }
    public Integer getSourceIndex() { return sourceIndex; }
    public void setSourceIndex(Integer sourceIndex) { this.sourceIndex = sourceIndex; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhysicalName() { return physicalName; }
    public void setPhysicalName(String physicalName) { this.physicalName = physicalName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Integer getLength() { return length; }
    public void setLength(Integer length) { this.length = length; }
    public Integer getPrecision() { return precision; }
    public void setPrecision(Integer precision) { this.precision = precision; }
    public Integer getScale() { return scale; }
    public void setScale(Integer scale) { this.scale = scale; }
    public Boolean getNullable() { return nullable; }
    public void setNullable(Boolean nullable) { this.nullable = nullable; }
    public Boolean getNeedsReview() { return needsReview; }
    public void setNeedsReview(Boolean needsReview) { this.needsReview = needsReview; }
    public String getTranslationSource() { return translationSource; }
    public void setTranslationSource(String translationSource) { this.translationSource = translationSource; }
    public List<String> getSamples() { return samples; }
    public void setSamples(List<String> samples) { this.samples = samples; }
}
