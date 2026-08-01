package com.xinke.datahub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "datahub")
public class DataHubProperties
{
    private String schema = "";
    private String tablePrefix = "dh_data_";
    private String storagePath = "";
    private DataSize maxFileSize = DataSize.ofMegabytes(30);
    private int maxRows = 50000;
    private int maxDatasetRows = 200000;
    private int maxColumns = 200;
    private int maxCellLength = 10000;
    private int previewRows = 20;
    private int previewExpireHours = 24;
    private int insertBatchSize = 500;
    private int recoveryStaleMinutes = 30;
    private int dispatchBatchSize = 20;
    private int maxEditMutations = 1000;
    private int versionRetentionDays = 30;

    public String getSchema() { return schema; }
    public void setSchema(String schema) { this.schema = schema; }
    public String getTablePrefix() { return tablePrefix; }
    public void setTablePrefix(String tablePrefix) { this.tablePrefix = tablePrefix; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public DataSize getMaxFileSize() { return maxFileSize; }
    public void setMaxFileSize(DataSize maxFileSize) { this.maxFileSize = maxFileSize; }
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
    public int getMaxDatasetRows() { return maxDatasetRows; }
    public void setMaxDatasetRows(int maxDatasetRows) { this.maxDatasetRows = maxDatasetRows; }
    public int getMaxColumns() { return maxColumns; }
    public void setMaxColumns(int maxColumns) { this.maxColumns = maxColumns; }
    public int getMaxCellLength() { return maxCellLength; }
    public void setMaxCellLength(int maxCellLength) { this.maxCellLength = maxCellLength; }
    public int getPreviewRows() { return previewRows; }
    public void setPreviewRows(int previewRows) { this.previewRows = previewRows; }
    public int getPreviewExpireHours() { return previewExpireHours; }
    public void setPreviewExpireHours(int previewExpireHours) { this.previewExpireHours = previewExpireHours; }
    public int getInsertBatchSize() { return insertBatchSize; }
    public void setInsertBatchSize(int insertBatchSize) { this.insertBatchSize = insertBatchSize; }
    public int getRecoveryStaleMinutes() { return recoveryStaleMinutes; }
    public void setRecoveryStaleMinutes(int recoveryStaleMinutes) { this.recoveryStaleMinutes = recoveryStaleMinutes; }
    public int getDispatchBatchSize() { return dispatchBatchSize; }
    public void setDispatchBatchSize(int dispatchBatchSize) { this.dispatchBatchSize = dispatchBatchSize; }
    public int getMaxEditMutations() { return maxEditMutations; }
    public void setMaxEditMutations(int maxEditMutations) { this.maxEditMutations = maxEditMutations; }
    public int getVersionRetentionDays() { return versionRetentionDays; }
    public void setVersionRetentionDays(int versionRetentionDays) { this.versionRetentionDays = versionRetentionDays; }
}
