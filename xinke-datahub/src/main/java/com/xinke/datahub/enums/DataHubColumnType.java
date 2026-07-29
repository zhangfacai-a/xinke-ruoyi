package com.xinke.datahub.enums;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import com.xinke.common.exception.ServiceException;

public enum DataHubColumnType
{
    VARCHAR,
    TEXT,
    BIGINT,
    DECIMAL,
    DATE,
    DATETIME,
    BOOLEAN;

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"));

    private static final List<DateTimeFormatter> DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy/M/d H:mm"));

    public static DataHubColumnType from(String value)
    {
        try
        {
            return DataHubColumnType.valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException("不支持的字段类型：" + value);
        }
    }

    public Object convert(String value)
    {
        if (value == null || value.isBlank()) return null;
        String text = value.trim();
        try
        {
            return switch (this)
            {
                case VARCHAR, TEXT -> value;
                case BIGINT -> Long.valueOf(text);
                case DECIMAL -> new BigDecimal(text);
                case DATE -> Date.valueOf(parseDate(text));
                case DATETIME -> Timestamp.valueOf(parseDateTime(text));
                case BOOLEAN -> parseBoolean(text);
            };
        }
        catch (RuntimeException e)
        {
            throw new IllegalArgumentException("值“" + value + "”不能转换为 " + name(), e);
        }
    }

    private static LocalDate parseDate(String text)
    {
        for (DateTimeFormatter formatter : DATE_FORMATTERS)
        {
            try { return LocalDate.parse(text, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        throw new DateTimeParseException("Unsupported date", text, 0);
    }

    private static LocalDateTime parseDateTime(String text)
    {
        for (DateTimeFormatter formatter : DATETIME_FORMATTERS)
        {
            try { return LocalDateTime.parse(text, formatter); }
            catch (DateTimeParseException ignored) { }
        }
        return parseDate(text).atStartOfDay();
    }

    private static Boolean parseBoolean(String text)
    {
        return switch (text.toLowerCase(Locale.ROOT))
        {
            case "true", "yes", "y", "是", "1" -> Boolean.TRUE;
            case "false", "no", "n", "否", "0" -> Boolean.FALSE;
            default -> throw new IllegalArgumentException("Unsupported boolean");
        };
    }
}
