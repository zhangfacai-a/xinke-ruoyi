package com.xinke.datahub.naming;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;

@Component
public class DataHubIdentifierService
{
    private static final Pattern IDENTIFIER = Pattern.compile("[a-z][a-z0-9_]{0,63}");

    private final String schema;
    private final String tablePrefix;

    public DataHubIdentifierService(DataHubProperties properties)
    {
        schema = properties.getSchema() == null ? "" : properties.getSchema().strip().toLowerCase(Locale.ROOT);
        tablePrefix = properties.getTablePrefix() == null ? "" : properties.getTablePrefix().strip().toLowerCase(Locale.ROOT);
        if (!schema.isEmpty()) requireIdentifier(schema, "DataHub Schema配置不合法");
        requireIdentifier(tablePrefix.endsWith("_") ? tablePrefix.substring(0, tablePrefix.length() - 1) : tablePrefix,
                "DataHub表前缀配置不合法");
        if (tablePrefix.length() > 24) throw new IllegalStateException("DataHub表前缀不能超过24个字符");
    }

    public String requireIdentifier(String identifier, String message)
    {
        if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) throw new ServiceException(message);
        return identifier;
    }

    public String quote(String identifier)
    {
        return "`" + requireIdentifier(identifier, "数据库标识符不合法") + "`";
    }

    public String qualifiedTable(String tableName)
    {
        if (tableName == null || !tableName.startsWith(tablePrefix))
            throw new ServiceException("动态表不属于DataHub命名空间");
        String table = quote(tableName);
        return schema.isEmpty() ? table : quote(schema) + "." + table;
    }

    public String stagingTable(long jobId, int attempt)
    {
        String suffix = jobSuffix(jobId, attempt);
        return limit(tablePrefix + "stage_" + suffix, 64);
    }

    public String versionTable(String englishName, long jobId, int attempt, int versionNo)
    {
        String base = requireIdentifier(englishName, "英文表名只能包含小写字母、数字和下划线");
        String suffix = jobSuffix(jobId, attempt);
        String version = "_v" + String.format(Locale.ROOT, "%06d", versionNo);
        int available = 64 - tablePrefix.length() - suffix.length() - version.length() - 1;
        if (base.length() > available) base = base.substring(0, available).replaceAll("_+$", "");
        return tablePrefix + base + "_" + suffix + version;
    }

    private String jobSuffix(long jobId, int attempt)
    {
        if (jobId <= 0 || attempt <= 0) throw new ServiceException("导入任务标识不合法");
        return "j" + Long.toUnsignedString(jobId, 36) + "a" + Integer.toUnsignedString(attempt, 36);
    }

    private String limit(String value, int length)
    {
        return value.length() <= length ? value : value.substring(0, length);
    }
}
