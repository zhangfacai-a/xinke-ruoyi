package com.xinke.datahub.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;

@Component
public class SpreadsheetParser
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataHubProperties properties;

    public SpreadsheetParser(DataHubProperties properties)
    {
        this.properties = properties;
    }

    public ParsedSpreadsheet parse(Path file, String originalFileName, String requestedSheet)
    {
        String extension = file == null ? "" : extension(file.getFileName().toString());
        if (!List.of("xls", "xlsx", "csv").contains(extension)) extension = extension(originalFileName);
        try
        {
            return "csv".equals(extension)
                    ? parseCsv(file, originalFileName, requestedSheet)
                    : parseExcel(file, requestedSheet);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("表格解析失败，请检查文件格式和内容").setDetailMessage(e.getMessage());
        }
    }

    private ParsedSpreadsheet parseExcel(Path file, String requestedSheet) throws Exception
    {
        try (InputStream input = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(input))
        {
            if (workbook.getNumberOfSheets() == 0) throw new ServiceException("Excel中没有可读取的Sheet");
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) sheetNames.add(workbook.getSheetName(i));
            Sheet sheet = requestedSheet == null || requestedSheet.isBlank()
                    ? workbook.getSheetAt(0) : workbook.getSheet(requestedSheet);
            if (sheet == null) throw new ServiceException("Sheet不存在：" + requestedSheet);

            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            DataFormatter formatter = new DataFormatter(Locale.CHINA);
            int headerIndex = findHeaderRow(sheet, formatter, evaluator);
            if (headerIndex < 0) throw new ServiceException("Sheet中没有可读取的数据");
            validateSourceRowCount(sheet.getLastRowNum() - headerIndex);
            Row headerRow = sheet.getRow(headerIndex);
            int columnCount = effectiveColumnCount(headerRow, formatter, evaluator);
            validateColumnCount(columnCount);

            ParsedSpreadsheet parsed = new ParsedSpreadsheet();
            parsed.setSheetNames(sheetNames);
            parsed.setSheetName(sheet.getSheetName());
            parsed.setHeaderRowNo(headerIndex + 1);
            parsed.setHeaders(readHeaders(headerRow, columnCount, formatter, evaluator, parsed.getWarnings()));

            List<ParsedRow> rows = new ArrayList<>();
            for (int rowIndex = headerIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++)
            {
                Row row = sheet.getRow(rowIndex);
                validateNoExtraExcelValues(row, columnCount, formatter, evaluator, rowIndex + 1);
                List<String> values = new ArrayList<>(columnCount);
                boolean hasValue = false;
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++)
                {
                    String value = excelCellValue(row == null ? null : row.getCell(columnIndex), formatter, evaluator);
                    validateCellLength(value, rowIndex + 1, columnIndex + 1);
                    values.add(value);
                    hasValue |= !value.isBlank();
                }
                if (hasValue)
                {
                    rows.add(new ParsedRow(rowIndex + 1, values));
                    validateRowCount(rows.size());
                }
            }
            parsed.setRows(rows);
            return parsed;
        }
    }

    private ParsedSpreadsheet parseCsv(Path file, String originalFileName, String requestedSheet) throws IOException
    {
        byte[] bytes = Files.readAllBytes(file);
        String content = decodeCsv(bytes);
        if (!content.isEmpty() && content.charAt(0) == '\uFEFF') content = content.substring(1);
        char delimiter = detectDelimiter(content);
        String syntheticSheet = baseName(originalFileName);
        if (requestedSheet != null && !requestedSheet.isBlank() && !syntheticSheet.equals(requestedSheet))
            throw new ServiceException("CSV不存在Sheet：" + requestedSheet);

        CSVFormat format = CSVFormat.DEFAULT.builder().setDelimiter(delimiter).setIgnoreEmptyLines(false).build();
        try (CSVParser csv = new CSVParser(new StringReader(content), format))
        {
            var records = csv.iterator();
            if (!records.hasNext()) throw new ServiceException("CSV中没有可读取的数据");
            CSVRecord headerRecord = records.next();
            int columnCount = trimCsvColumnCount(headerRecord);
            validateColumnCount(columnCount);

            ParsedSpreadsheet parsed = new ParsedSpreadsheet();
            parsed.setSheetNames(Collections.singletonList(syntheticSheet));
            parsed.setSheetName(syntheticSheet);
            parsed.setHeaderRowNo(1);
            parsed.setHeaders(readCsvHeaders(headerRecord, columnCount, parsed.getWarnings()));

            List<ParsedRow> rows = new ArrayList<>();
            int rowIndex = 1;
            while (records.hasNext())
            {
                CSVRecord record = records.next();
                validateSourceRowCount(rowIndex);
                validateNoExtraCsvValues(record, columnCount, rowIndex + 1);
                List<String> values = new ArrayList<>(columnCount);
                boolean hasValue = false;
                for (int columnIndex = 0; columnIndex < columnCount; columnIndex++)
                {
                    String value = columnIndex < record.size() ? record.get(columnIndex) : "";
                    validateCellLength(value, rowIndex + 1, columnIndex + 1);
                    values.add(value);
                    hasValue |= !value.isBlank();
                }
                if (hasValue)
                {
                    rows.add(new ParsedRow(rowIndex + 1, values));
                    validateRowCount(rows.size());
                }
                rowIndex++;
            }
            parsed.setRows(rows);
            return parsed;
        }
    }

    private int findHeaderRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator)
    {
        int last = Math.min(sheet.getLastRowNum(), 20);
        for (int i = sheet.getFirstRowNum(); i <= last; i++)
        {
            Row row = sheet.getRow(i);
            if (row != null && effectiveColumnCount(row, formatter, evaluator) > 0) return i;
        }
        return -1;
    }

    private int effectiveColumnCount(Row row, DataFormatter formatter, FormulaEvaluator evaluator)
    {
        if (row == null || row.getLastCellNum() < 0) return 0;
        int count = row.getLastCellNum();
        while (count > 0 && excelCellValue(row.getCell(count - 1), formatter, evaluator).isBlank()) count--;
        return count;
    }

    private List<String> readHeaders(Row row, int count, DataFormatter formatter, FormulaEvaluator evaluator, List<String> warnings)
    {
        List<String> headers = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            String value = excelCellValue(row.getCell(i), formatter, evaluator).strip();
            if (value.isBlank())
            {
                value = "未命名列" + (i + 1);
                warnings.add("第" + (i + 1) + "列没有表头，已生成临时名称");
            }
            validateHeaderLength(value, i + 1);
            headers.add(value);
        }
        return headers;
    }

    private List<String> readCsvHeaders(CSVRecord record, int count, List<String> warnings)
    {
        List<String> headers = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            String value = record.get(i).strip();
            if (value.isBlank())
            {
                value = "未命名列" + (i + 1);
                warnings.add("第" + (i + 1) + "列没有表头，已生成临时名称");
            }
            validateHeaderLength(value, i + 1);
            headers.add(value);
        }
        return headers;
    }

    private String excelCellValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator)
    {
        if (cell == null || cell.getCellType() == CellType.BLANK) return "";
        CellType valueType = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();
        if (valueType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
        {
            LocalDateTime value = cell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
            return value.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)
                    ? DATE_FORMAT.format(value) : DATETIME_FORMAT.format(value);
        }
        if (valueType == CellType.NUMERIC && "General".equalsIgnoreCase(cell.getCellStyle().getDataFormatString()))
            return BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros().toPlainString();
        return formatter.formatCellValue(cell, evaluator).strip();
    }

    private String decodeCsv(byte[] bytes)
    {
        try { return decode(bytes, StandardCharsets.UTF_8); }
        catch (CharacterCodingException ignored)
        {
            try { return decode(bytes, Charset.forName("GB18030")); }
            catch (CharacterCodingException e) { throw new ServiceException("CSV编码无法识别，请使用UTF-8或GB18030"); }
        }
    }

    private String decode(byte[] bytes, Charset charset) throws CharacterCodingException
    {
        return charset.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    private char detectDelimiter(String content)
    {
        int end = content.indexOf('\n');
        String line = end < 0 ? content : content.substring(0, end);
        char[] candidates = { ',', '\t', ';' };
        char selected = ',';
        int max = -1;
        for (char candidate : candidates)
        {
            int count = 0;
            boolean quoted = false;
            for (int i = 0; i < line.length(); i++)
            {
                if (line.charAt(i) == '"') quoted = !quoted;
                else if (!quoted && line.charAt(i) == candidate) count++;
            }
            if (count > max) { max = count; selected = candidate; }
        }
        return selected;
    }

    private int trimCsvColumnCount(CSVRecord record)
    {
        int count = record.size();
        while (count > 0 && record.get(count - 1).isBlank()) count--;
        return count;
    }

    private void validateColumnCount(int count)
    {
        if (count == 0) throw new ServiceException("表格第一行没有有效列名");
        if (count > properties.getMaxColumns())
            throw new ServiceException("表格列数不能超过" + properties.getMaxColumns() + "列");
    }

    private void validateRowCount(int count)
    {
        if (count > properties.getMaxRows())
            throw new ServiceException("表格数据不能超过" + properties.getMaxRows() + "行");
    }

    private void validateSourceRowCount(int count)
    {
        if (count > properties.getMaxRows())
            throw new ServiceException("表格原始数据记录不能超过" + properties.getMaxRows() + "行（空行也计入）");
    }

    private void validateCellLength(String value, int row, int column)
    {
        if (value.length() > properties.getMaxCellLength())
            throw new ServiceException("第" + row + "行第" + column + "列内容超过" + properties.getMaxCellLength() + "个字符");
    }

    private void validateHeaderLength(String value, int column)
    {
        if (value.length() > 255) throw new ServiceException("第" + column + "列表头不能超过255个字符");
    }

    private void validateNoExtraExcelValues(Row row, int columnCount, DataFormatter formatter,
            FormulaEvaluator evaluator, int rowNo)
    {
        if (row == null || row.getLastCellNum() <= columnCount) return;
        for (int column = columnCount; column < row.getLastCellNum(); column++)
        {
            if (!excelCellValue(row.getCell(column), formatter, evaluator).isBlank())
                throw new ServiceException("第" + rowNo + "行存在超出表头范围的数据");
        }
    }

    private void validateNoExtraCsvValues(CSVRecord record, int columnCount, int rowNo)
    {
        for (int column = columnCount; column < record.size(); column++)
        {
            if (!record.get(column).isBlank())
                throw new ServiceException("第" + rowNo + "行存在超出表头范围的数据");
        }
    }

    private String extension(String fileName)
    {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String baseName(String fileName)
    {
        String name = fileName == null ? "数据" : fileName;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) name = name.substring(slash + 1);
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
