package com.xinke.datahub.parser;

import java.util.List;

public class ParsedRow
{
    private final int sourceRowNo;
    private final List<String> values;

    public ParsedRow(int sourceRowNo, List<String> values)
    {
        this.sourceRowNo = sourceRowNo;
        this.values = values;
    }

    public int getSourceRowNo() { return sourceRowNo; }
    public List<String> getValues() { return values; }
}
