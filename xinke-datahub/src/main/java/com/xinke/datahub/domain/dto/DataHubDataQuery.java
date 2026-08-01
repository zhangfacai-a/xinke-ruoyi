package com.xinke.datahub.domain.dto;

import java.util.ArrayList;
import java.util.List;

public class DataHubDataQuery
{
    private Integer pageNum = 1;
    private Integer pageSize = 20;
    private List<DataHubDataFilter> filters = new ArrayList<>();
    private Long sortColumnId;
    private String sortDirection = "DESC";

    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public List<DataHubDataFilter> getFilters() { return filters; }
    public void setFilters(List<DataHubDataFilter> filters) { this.filters = filters; }
    public Long getSortColumnId() { return sortColumnId; }
    public void setSortColumnId(Long sortColumnId) { this.sortColumnId = sortColumnId; }
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
}
