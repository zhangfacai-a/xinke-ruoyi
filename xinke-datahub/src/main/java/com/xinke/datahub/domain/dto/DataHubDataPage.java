package com.xinke.datahub.domain.dto;

import java.util.List;
import java.util.Map;

public class DataHubDataPage
{
    private long total;
    private int pageNum;
    private int pageSize;
    private List<Map<String, Object>> rows;

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public List<Map<String, Object>> getRows() { return rows; }
    public void setRows(List<Map<String, Object>> rows) { this.rows = rows; }
}
