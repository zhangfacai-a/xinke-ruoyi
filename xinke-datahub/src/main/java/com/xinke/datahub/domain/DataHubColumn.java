package com.xinke.datahub.domain;

import java.util.Date;

public class DataHubColumn
{
    private Long columnId;
    private Long datasetId;
    private Long schemaId;
    private Integer sourceIndex;
    private Integer ordinalPosition;
    private String sourceName;
    private String displayName;
    private String physicalName;
    private String dataType;
    private Integer columnLength;
    private Integer numericPrecision;
    private Integer numericScale;
    private Boolean nullable;
    private Boolean businessKey;
    private String translationSource;
    private String samplesJson;
    private Date createTime;

    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public Long getDatasetId() { return datasetId; }
    public void setDatasetId(Long datasetId) { this.datasetId = datasetId; }
    public Long getSchemaId() { return schemaId; }
    public void setSchemaId(Long schemaId) { this.schemaId = schemaId; }
    public Integer getSourceIndex() { return sourceIndex; }
    public void setSourceIndex(Integer sourceIndex) { this.sourceIndex = sourceIndex; }
    public Integer getOrdinalPosition() { return ordinalPosition; }
    public void setOrdinalPosition(Integer ordinalPosition) { this.ordinalPosition = ordinalPosition; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhysicalName() { return physicalName; }
    public void setPhysicalName(String physicalName) { this.physicalName = physicalName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public Integer getColumnLength() { return columnLength; }
    public void setColumnLength(Integer columnLength) { this.columnLength = columnLength; }
    public Integer getNumericPrecision() { return numericPrecision; }
    public void setNumericPrecision(Integer numericPrecision) { this.numericPrecision = numericPrecision; }
    public Integer getNumericScale() { return numericScale; }
    public void setNumericScale(Integer numericScale) { this.numericScale = numericScale; }
    public Boolean getNullable() { return nullable; }
    public void setNullable(Boolean nullable) { this.nullable = nullable; }
    public Boolean getBusinessKey() { return businessKey; }
    public void setBusinessKey(Boolean businessKey) { this.businessKey = businessKey; }
    public String getTranslationSource() { return translationSource; }
    public void setTranslationSource(String translationSource) { this.translationSource = translationSource; }
    public String getSamplesJson() { return samplesJson; }
    public void setSamplesJson(String samplesJson) { this.samplesJson = samplesJson; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
