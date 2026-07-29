package com.xinke.datahub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.naming.EnglishNameGenerator;
import com.xinke.datahub.parser.ParsedRow;
import com.xinke.datahub.parser.ParsedSpreadsheet;

class DataHubSchemaInferenceServiceTest
{
    private final DataHubSchemaInferenceService service = new DataHubSchemaInferenceService(
            new EnglishNameGenerator(), new DataHubProperties());

    @Test
    void infersConservativeBusinessTypes()
    {
        ParsedSpreadsheet sheet = new ParsedSpreadsheet();
        sheet.setHeaders(List.of("订单编号", "数量", "金额", "日期", "是否有效"));
        sheet.setRows(List.of(
                new ParsedRow(2, List.of("00123", "2", "12.50", "2026-07-01", "是")),
                new ParsedRow(3, List.of("00124", "10", "9.90", "2026-07-02", "否"))));

        List<DataHubColumnDefinition> columns = service.inferColumns(sheet, Map.of(), new ArrayList<>());
        assertEquals("VARCHAR", columns.get(0).getDataType());
        assertEquals("BIGINT", columns.get(1).getDataType());
        assertEquals("DECIMAL", columns.get(2).getDataType());
        assertEquals(4, columns.get(2).getPrecision());
        assertEquals(2, columns.get(2).getScale());
        assertEquals("DATE", columns.get(3).getDataType());
        assertEquals("BOOLEAN", columns.get(4).getDataType());
    }

    @Test
    void deduplicatesTranslatedColumnNames()
    {
        ParsedSpreadsheet sheet = new ParsedSpreadsheet();
        sheet.setHeaders(List.of("客户名称", "客户名称"));
        sheet.setRows(List.of(new ParsedRow(2, List.of("甲", "乙"))));
        List<String> warnings = new ArrayList<>();

        List<DataHubColumnDefinition> columns = service.inferColumns(sheet, Map.of(), warnings);
        assertEquals("customer_name", columns.get(0).getPhysicalName());
        assertEquals("customer_name_2", columns.get(1).getPhysicalName());
        assertEquals(1, warnings.size());
    }

    @Test
    void decimalPrecisionIncludesIntegerAndFractionCapacity()
    {
        ParsedSpreadsheet sheet = new ParsedSpreadsheet();
        sheet.setHeaders(List.of("金额"));
        sheet.setRows(List.of(
                new ParsedRow(2, List.of("999")),
                new ParsedRow(3, List.of("0.01"))));

        DataHubColumnDefinition column = service.inferColumns(sheet, Map.of(), new ArrayList<>()).get(0);
        assertEquals("DECIMAL", column.getDataType());
        assertEquals(5, column.getPrecision());
        assertEquals(2, column.getScale());
    }
}
