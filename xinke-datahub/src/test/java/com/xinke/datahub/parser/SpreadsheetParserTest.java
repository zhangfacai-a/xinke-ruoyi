package com.xinke.datahub.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;

class SpreadsheetParserTest
{
    @TempDir
    Path tempDir;

    private final SpreadsheetParser parser = new SpreadsheetParser(new DataHubProperties());

    @Test
    void parsesUtf8BomAndQuotedCsv() throws Exception
    {
        Path file = tempDir.resolve("客户.csv");
        String csv = "\uFEFF客户名称,备注\r\n张三,\"包含,逗号\"\r\n李四,普通文本\r\n";
        Files.writeString(file, csv, StandardCharsets.UTF_8);

        ParsedSpreadsheet parsed = parser.parse(file, "客户.csv", null);
        assertEquals("客户", parsed.getSheetName());
        assertEquals(2, parsed.getRows().size());
        assertEquals("包含,逗号", parsed.getRows().get(0).getValues().get(1));
    }

    @Test
    void usesStoredExtensionWhenDisplayFileNameHasNoSuffix() throws Exception
    {
        Path file = tempDir.resolve("source.csv");
        Files.writeString(file, "名称\r\n张三\r\n", StandardCharsets.UTF_8);
        ParsedSpreadsheet parsed = parser.parse(file, "a".repeat(255), null);
        assertEquals(1, parsed.getRows().size());
    }

    @Test
    void preservesLeadingZeroAndNormalizesExcelDate() throws Exception
    {
        Path file = tempDir.resolve("订单.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            var sheet = workbook.createSheet("客户订单");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("订单编号");
            header.createCell(1).setCellValue("日期");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("00123");
            Date date = Date.from(LocalDate.of(2026, 7, 26).atStartOfDay(ZoneId.systemDefault()).toInstant());
            data.createCell(1).setCellValue(date);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd"));
            data.getCell(1).setCellStyle(dateStyle);
            try (var output = Files.newOutputStream(file)) { workbook.write(output); }
        }

        ParsedSpreadsheet parsed;
        try
        {
            parsed = parser.parse(file, "订单.xlsx", null);
        }
        catch (ServiceException e)
        {
            throw new AssertionError(e.getDetailMessage(), e);
        }
        assertEquals("00123", parsed.getRows().get(0).getValues().get(0));
        assertEquals("2026-07-26", parsed.getRows().get(0).getValues().get(1));
    }

    @Test
    void rejectsDataOutsideCsvHeaderRange() throws Exception
    {
        Path file = tempDir.resolve("invalid.csv");
        Files.writeString(file, "名称\r\n张三,未声明字段\r\n", StandardCharsets.UTF_8);
        assertThrows(ServiceException.class, () -> parser.parse(file, "invalid.csv", null));
    }

    @Test
    void rejectsDataOutsideExcelHeaderRange() throws Exception
    {
        Path file = tempDir.resolve("invalid.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            var sheet = workbook.createSheet("数据");
            sheet.createRow(0).createCell(0).setCellValue("名称");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("张三");
            data.createCell(1).setCellValue("未声明字段");
            try (var output = Files.newOutputStream(file)) { workbook.write(output); }
        }
        assertThrows(ServiceException.class, () -> parser.parse(file, "invalid.xlsx", null));
    }

    @Test
    void allowsBlankExcelCellOutsideHeaderRange() throws Exception
    {
        Path file = tempDir.resolve("blank-tail.xlsx");
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            var sheet = workbook.createSheet("数据");
            sheet.createRow(0).createCell(0).setCellValue("名称");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("张三");
            data.createCell(1);
            try (var output = Files.newOutputStream(file)) { workbook.write(output); }
        }
        assertEquals(1, parser.parse(file, "blank-tail.xlsx", null).getRows().size());
    }

    @Test
    void limitsBlankCsvSourceRecords() throws Exception
    {
        DataHubProperties limited = new DataHubProperties();
        limited.setMaxRows(2);
        SpreadsheetParser limitedParser = new SpreadsheetParser(limited);
        Path file = tempDir.resolve("too-many-empty-rows.csv");
        Files.writeString(file, "名称\r\n\r\n\r\n\r\n", StandardCharsets.UTF_8);
        assertThrows(ServiceException.class, () -> limitedParser.parse(file, "too-many-empty-rows.csv", null));
    }
}
