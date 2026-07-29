package com.xinke.datahub.domain.dto;

public class DataHubDataFilter
{
    private Long columnId;
    private String operator;
    private String value;
    private String valueTo;

    public Long getColumnId() { return columnId; }
    public void setColumnId(Long columnId) { this.columnId = columnId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getValueTo() { return valueTo; }
    public void setValueTo(String valueTo) { this.valueTo = valueTo; }
}
