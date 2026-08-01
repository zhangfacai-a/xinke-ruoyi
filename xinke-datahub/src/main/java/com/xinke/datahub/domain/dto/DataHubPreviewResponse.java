package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DataHubPreviewResponse
{
    private String previewId;
    private String fileName;
    private List<String> sheetNames = new ArrayList<>();
    private String sheetName;
    private String displayName;
    private String physicalName;
    private List<DataHubColumnDefinition> columns = new ArrayList<>();
    private List<Map<String, String>> sampleRows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();
    private long totalRows;
    private Date expiresAt;

    public String getPreviewId() { return previewId; }
    public void setPreviewId(String previewId) { this.previewId = previewId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public List<String> getSheetNames() { return sheetNames; }
    public void setSheetNames(List<String> sheetNames) { this.sheetNames = sheetNames; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getPhysicalName() { return physicalName; }
    public void setPhysicalName(String physicalName) { this.physicalName = physicalName; }
    public List<DataHubColumnDefinition> getColumns() { return columns; }
    public void setColumns(List<DataHubColumnDefinition> columns) { this.columns = columns; }
    public List<Map<String, String>> getSampleRows() { return sampleRows; }
    public void setSampleRows(List<Map<String, String>> sampleRows) { this.sampleRows = sampleRows; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public long getTotalRows() { return totalRows; }
    public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
    public Date getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Date expiresAt) { this.expiresAt = expiresAt; }

    public static Map<String, String> sampleRow(List<DataHubColumnDefinition> columns, List<String> values)
    {
        Map<String, String> row = new LinkedHashMap<>();
        for (DataHubColumnDefinition column : columns)
        {
            int index = column.getSourceIndex();
            row.put(column.getPhysicalName(), index < values.size() ? values.get(index) : "");
        }
        return row;
    }
}
