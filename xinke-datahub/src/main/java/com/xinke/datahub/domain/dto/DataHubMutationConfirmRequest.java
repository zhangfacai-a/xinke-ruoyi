package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubMutationConfirmRequest
{
    private Long baseVersionId;
    private String sheetName;
    private String confirmationName;
    private List<DataHubColumnMapping> mappings = new ArrayList<>();

    public Long getBaseVersionId() { return baseVersionId; }
    public void setBaseVersionId(Long baseVersionId) { this.baseVersionId = baseVersionId; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public String getConfirmationName() { return confirmationName; }
    public void setConfirmationName(String confirmationName) { this.confirmationName = confirmationName; }
    public List<DataHubColumnMapping> getMappings() { return mappings; }
    public void setMappings(List<DataHubColumnMapping> mappings) { this.mappings = mappings; }
}
