package com.xinke.datahub.domain.dto;

public class DataHubEditCellValue
{
    private Long columnId;
    private String value;
    private Boolean isNull = Boolean.FALSE;

    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public Boolean getIsNull() { return isNull; }
    public void setIsNull(Boolean isNull) { this.isNull = isNull; }
}
