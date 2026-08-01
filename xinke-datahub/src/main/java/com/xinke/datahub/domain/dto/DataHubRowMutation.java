package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubRowMutation
{
    private String clientMutationId;
    private String operation;
    private Long rowId;
    private String expectedRowHash;
    private List<DataHubEditCellValue> values = new ArrayList<>();

    public String getClientMutationId() { return clientMutationId; }
    public void setClientMutationId(String clientMutationId) { this.clientMutationId = clientMutationId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public Long getRowId() { return rowId; }
    public void setRowId(Long rowId) { this.rowId = rowId; }
    public String getExpectedRowHash() { return expectedRowHash; }
    public void setExpectedRowHash(String expectedRowHash) { this.expectedRowHash = expectedRowHash; }
    public List<DataHubEditCellValue> getValues() { return values; }
    public void setValues(List<DataHubEditCellValue> values) { this.values = values; }
}
