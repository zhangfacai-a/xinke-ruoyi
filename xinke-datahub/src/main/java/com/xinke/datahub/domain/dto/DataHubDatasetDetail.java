package com.xinke.datahub.domain.dto;

import java.util.List;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataset;

public class DataHubDatasetDetail
{
    private DataHubDataset dataset;
    private List<DataHubColumn> columns;
    private DataHubAccess access;

    public DataHubDataset getDataset() { return dataset; }
    public void setDataset(DataHubDataset dataset) { this.dataset = dataset; }
    public List<DataHubColumn> getColumns() { return columns; }
    public void setColumns(List<DataHubColumn> columns) { this.columns = columns; }
    public DataHubAccess getAccess() { return access; }
    public void setAccess(DataHubAccess access) { this.access = access; }
}
