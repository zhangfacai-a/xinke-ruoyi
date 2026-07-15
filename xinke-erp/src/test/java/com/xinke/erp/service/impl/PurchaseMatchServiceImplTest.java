package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class PurchaseMatchServiceImplTest
{
    private final PurchaseMatchServiceImpl service = new PurchaseMatchServiceImpl();

    @Test
    void cellTextKeepsLongBusinessNumbersAndPhoneText() throws Exception
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            Sheet sheet = workbook.createSheet("导入");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("300012800649");
            row.createCell(1).setCellValue("138-0000-0000");

            assertEquals("300012800649", invokeCellText(row.getCell(0), workbook.getCreationHelper().createFormulaEvaluator()));
            assertEquals("138-0000-0000", invokeCellText(row.getCell(1), workbook.getCreationHelper().createFormulaEvaluator()));
        }
    }

    @Test
    void cellTextReadsFormulaCalculatedAmount() throws Exception
    {
        try (XSSFWorkbook workbook = new XSSFWorkbook())
        {
            Sheet sheet = workbook.createSheet("导入");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(12.5D);
            row.createCell(1).setCellValue(4D);
            row.createCell(2).setCellFormula("A1*B1");
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            assertEquals("50", invokeCellText(row.getCell(2), evaluator));
        }
    }

    @Test
    void normalizeHeaderAcceptsHalfAndFullWidthReceiptParentheses() throws Exception
    {
        Method method = PurchaseMatchServiceImpl.class.getDeclaredMethod("normalizeHeader", Object.class);
        method.setAccessible(true);

        assertEquals("回单（）", method.invoke(service, " 回单() "));
        assertEquals("采购单备注", method.invoke(service, "　采购单备注 "));
    }

    @Test
    void amountCheckDetectsMismatchWithoutBlockingImport() throws Exception
    {
        Method method = PurchaseMatchServiceImpl.class.getDeclaredMethod("amountCheck", Map.class);
        method.setAccessible(true);
        Map<String, Object> row = new HashMap<>();
        row.put("unitPrice", new BigDecimal("10.00"));
        row.put("quantity", new BigDecimal("3.00"));
        row.put("totalAmount", new BigDecimal("31.00"));

        assertEquals("AMOUNT_MISMATCH", method.invoke(service, row));
    }

    private String invokeCellText(Cell cell, FormulaEvaluator evaluator) throws Exception
    {
        Method method = PurchaseMatchServiceImpl.class.getDeclaredMethod("cellText", Cell.class, FormulaEvaluator.class);
        method.setAccessible(true);
        return (String) method.invoke(service, cell, evaluator);
    }
}
