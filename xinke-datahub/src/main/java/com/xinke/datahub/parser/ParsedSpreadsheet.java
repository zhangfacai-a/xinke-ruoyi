package com.xinke.datahub.parser;

import java.util.ArrayList;
import java.util.List;

public class ParsedSpreadsheet
{
    private List<String> sheetNames = new ArrayList<>();
    private String sheetName;
    private int headerRowNo;
    private List<String> headers = new ArrayList<>();
    private List<ParsedRow> rows = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public List<String> getSheetNames() { return sheetNames; }
    public void setSheetNames(List<String> sheetNames) { this.sheetNames = sheetNames; }
    public String getSheetName() { return sheetName; }
    public void setSheetName(String sheetName) { this.sheetName = sheetName; }
    public int getHeaderRowNo() { return headerRowNo; }
    public void setHeaderRowNo(int headerRowNo) { this.headerRowNo = headerRowNo; }
    public List<String> getHeaders() { return headers; }
    public void setHeaders(List<String> headers) { this.headers = headers; }
    public List<ParsedRow> getRows() { return rows; }
    public void setRows(List<ParsedRow> rows) { this.rows = rows; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
}
