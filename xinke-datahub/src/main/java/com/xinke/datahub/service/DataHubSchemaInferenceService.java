package com.xinke.datahub.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubPreviewResponse;
import com.xinke.datahub.enums.DataHubColumnType;
import com.xinke.datahub.naming.EnglishNameGenerator;
import com.xinke.datahub.naming.GeneratedName;
import com.xinke.datahub.parser.ParsedRow;
import com.xinke.datahub.parser.ParsedSpreadsheet;

@Service
public class DataHubSchemaInferenceService
{
    private static final long MAX_ESTIMATED_ROW_BYTES = 60000L;
    private static final Pattern INTEGER = Pattern.compile("^[-+]?\\d+$");
    private static final Pattern DECIMAL = Pattern.compile("^[-+]?(?:\\d+\\.?\\d*|\\d*\\.\\d+)$");
    private static final Pattern LEADING_ZERO = Pattern.compile("^[-+]?0\\d+$");
    private static final Pattern IDENTIFIER_HEADER = Pattern.compile(
            ".*(编号|编码|单号|电话|手机|身份证|邮编|账号|卡号|条码|code|id|no|phone).*", Pattern.CASE_INSENSITIVE);

    private final EnglishNameGenerator nameGenerator;
    private final DataHubProperties properties;

    public DataHubSchemaInferenceService(EnglishNameGenerator nameGenerator, DataHubProperties properties)
    {
        this.nameGenerator = nameGenerator;
        this.properties = properties;
    }

    public DataHubPreviewResponse buildPreview(String previewId, String fileName, ParsedSpreadsheet parsed,
            java.util.Date expiresAt, Map<String, String> dictionary)
    {
        DataHubPreviewResponse response = new DataHubPreviewResponse();
        response.setPreviewId(previewId);
        response.setFileName(fileName);
        response.setSheetNames(parsed.getSheetNames());
        response.setSheetName(parsed.getSheetName());
        response.setDisplayName(parsed.getSheetName());
        GeneratedName tableName = nameGenerator.generate(parsed.getSheetName(), "table", 1, dictionary);
        response.setPhysicalName(tableName.getIdentifier());
        response.setExpiresAt(expiresAt);
        response.setTotalRows(parsed.getRows().size());
        response.getWarnings().addAll(parsed.getWarnings());
        if (tableName.isNeedsReview()) response.getWarnings().add("表名未能完整翻译，请确认英文表名");

        List<DataHubColumnDefinition> columns = inferColumns(parsed, dictionary, response.getWarnings());
        response.setColumns(columns);
        int sampleSize = Math.min(properties.getPreviewRows(), parsed.getRows().size());
        for (int i = 0; i < sampleSize; i++)
            response.getSampleRows().add(DataHubPreviewResponse.sampleRow(columns, parsed.getRows().get(i).getValues()));
        return response;
    }

    public List<DataHubColumnDefinition> inferColumns(ParsedSpreadsheet parsed, Map<String, String> dictionary, List<String> warnings)
    {
        List<DataHubColumnDefinition> columns = new ArrayList<>();
        Set<String> identifiers = new HashSet<>();
        Map<String, Integer> headerCounts = new HashMap<>();
        for (int index = 0; index < parsed.getHeaders().size(); index++)
        {
            String header = parsed.getHeaders().get(index);
            headerCounts.merge(header, 1, Integer::sum);
            GeneratedName generated = nameGenerator.generate(header, "column", index + 1, dictionary);
            String identifier = uniqueIdentifier(generated.getIdentifier(), identifiers);
            DataHubColumnDefinition column = inferColumn(index, header, identifier, generated, parsed.getRows());
            columns.add(column);
        }
        headerCounts.forEach((header, count) -> {
            if (count > 1) warnings.add("列名“" + header + "”重复，英文列名已自动添加序号");
        });
        long reviewCount = columns.stream().filter(c -> Boolean.TRUE.equals(c.getNeedsReview())).count();
        if (reviewCount > 0) warnings.add("有" + reviewCount + "个列名需要人工确认");
        fitMySqlRow(columns, warnings);
        return columns;
    }

    private DataHubColumnDefinition inferColumn(int index, String header, String identifier,
            GeneratedName generated, List<ParsedRow> rows)
    {
        List<String> values = new ArrayList<>();
        LinkedHashSet<String> samples = new LinkedHashSet<>();
        int maxLength = 0;
        for (ParsedRow row : rows)
        {
            String value = index < row.getValues().size() ? row.getValues().get(index) : "";
            if (!value.isBlank())
            {
                values.add(value.strip());
                maxLength = Math.max(maxLength, value.length());
                if (samples.size() < 3) samples.add(value);
            }
        }

        InferredType inferred = inferType(header, values, maxLength);
        DataHubColumnDefinition column = new DataHubColumnDefinition();
        column.setSourceIndex(index);
        column.setSourceName(header);
        column.setDisplayName(header);
        column.setPhysicalName(identifier);
        column.setDataType(inferred.type.name());
        column.setLength(inferred.length);
        column.setPrecision(inferred.precision);
        column.setScale(inferred.scale);
        column.setNullable(Boolean.TRUE);
        column.setNeedsReview(generated.isNeedsReview());
        column.setTranslationSource(generated.getSource());
        column.setSamples(new ArrayList<>(samples));
        return column;
    }

    private InferredType inferType(String header, List<String> values, int maxLength)
    {
        if (values.isEmpty()) return new InferredType(DataHubColumnType.VARCHAR, 255, null, null);
        if (isBoolean(values)) return new InferredType(DataHubColumnType.BOOLEAN, null, null, null);
        if (isDateTime(values)) return new InferredType(DataHubColumnType.DATETIME, null, null, null);
        if (isDate(values)) return new InferredType(DataHubColumnType.DATE, null, null, null);

        boolean identifierHeader = IDENTIFIER_HEADER.matcher(header.toLowerCase(Locale.ROOT)).matches();
        if (!identifierHeader && values.stream().allMatch(INTEGER.asPredicate())
                && values.stream().noneMatch(LEADING_ZERO.asPredicate()) && fitsLong(values))
            return new InferredType(DataHubColumnType.BIGINT, null, 19, 0);

        if (!identifierHeader && values.stream().allMatch(DECIMAL.asPredicate())
                && values.stream().noneMatch(LEADING_ZERO.asPredicate()))
        {
            int integerDigits = 0;
            int scale = 0;
            for (String value : values)
            {
                BigDecimal decimal = new BigDecimal(value);
                int valueScale = Math.max(0, decimal.scale());
                integerDigits = Math.max(integerDigits, Math.max(0, decimal.precision() - decimal.scale()));
                scale = Math.max(scale, valueScale);
            }
            int precision = Math.max(1, integerDigits + scale);
            if (precision <= 38) return new InferredType(DataHubColumnType.DECIMAL, null, precision, scale);
        }

        if (maxLength > 1000) return new InferredType(DataHubColumnType.TEXT, null, null, null);
        return new InferredType(DataHubColumnType.VARCHAR, varcharLength(maxLength), null, null);
    }

    private boolean isBoolean(List<String> values)
    {
        boolean hasWordValue = false;
        for (String value : values)
        {
            String text = value.toLowerCase(Locale.ROOT);
            if (!Set.of("true", "false", "yes", "no", "y", "n", "是", "否", "1", "0").contains(text)) return false;
            hasWordValue |= !"1".equals(text) && !"0".equals(text);
        }
        return hasWordValue;
    }

    private boolean isDate(List<String> values)
    {
        return values.stream().allMatch(value -> canConvert(DataHubColumnType.DATE, value));
    }

    private boolean isDateTime(List<String> values)
    {
        boolean hasTime = values.stream().anyMatch(value -> value.contains(":") || value.contains("T"));
        return hasTime && values.stream().allMatch(value -> canConvert(DataHubColumnType.DATETIME, value));
    }

    private boolean canConvert(DataHubColumnType type, String value)
    {
        try { type.convert(value); return true; }
        catch (RuntimeException e) { return false; }
    }

    private boolean fitsLong(List<String> values)
    {
        try { values.forEach(Long::valueOf); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private int varcharLength(int maxLength)
    {
        if (maxLength <= 64) return 64;
        if (maxLength <= 128) return 128;
        if (maxLength <= 255) return 255;
        if (maxLength <= 512) return 512;
        return 1000;
    }

    private String uniqueIdentifier(String candidate, Set<String> identifiers)
    {
        String value = candidate;
        int suffix = 2;
        while (!identifiers.add(value))
        {
            String addition = "_" + suffix++;
            int maxBase = 48 - addition.length();
            value = (candidate.length() > maxBase ? candidate.substring(0, maxBase) : candidate) + addition;
        }
        return value;
    }

    private void fitMySqlRow(List<DataHubColumnDefinition> columns, List<String> warnings)
    {
        int promoted = 0;
        while (estimatedRowBytes(columns) > MAX_ESTIMATED_ROW_BYTES)
        {
            DataHubColumnDefinition widest = columns.stream()
                    .filter(column -> DataHubColumnType.VARCHAR.name().equals(column.getDataType()))
                    .max((left, right) -> Integer.compare(left.getLength() == null ? 255 : left.getLength(),
                            right.getLength() == null ? 255 : right.getLength()))
                    .orElse(null);
            if (widest == null) break;
            widest.setDataType(DataHubColumnType.TEXT.name());
            widest.setLength(null);
            promoted++;
        }
        if (promoted > 0) warnings.add("为满足MySQL单行容量限制，已将" + promoted + "个长字段调整为TEXT");
    }

    private long estimatedRowBytes(List<DataHubColumnDefinition> columns)
    {
        long bytes = 128;
        for (DataHubColumnDefinition column : columns)
        {
            DataHubColumnType type = DataHubColumnType.from(column.getDataType());
            bytes += switch (type)
            {
                case VARCHAR -> 2L + 4L * (column.getLength() == null ? 255 : column.getLength());
                case TEXT -> 20L;
                case BIGINT, DATETIME -> 8L;
                case DECIMAL -> 20L;
                case DATE -> 3L;
                case BOOLEAN -> 1L;
            };
        }
        return bytes;
    }

    private record InferredType(DataHubColumnType type, Integer length, Integer precision, Integer scale) { }
}
