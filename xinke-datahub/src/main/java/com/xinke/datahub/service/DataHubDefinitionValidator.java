package com.xinke.datahub.service;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.enums.DataHubColumnType;
import com.xinke.datahub.naming.DataHubIdentifierService;
import com.xinke.datahub.parser.ParsedSpreadsheet;

@Component
public class DataHubDefinitionValidator
{
    private static final long MAX_ESTIMATED_ROW_BYTES = 60000L;

    private final DataHubIdentifierService identifiers;
    private final DataHubProperties properties;

    public DataHubDefinitionValidator(DataHubIdentifierService identifiers, DataHubProperties properties)
    {
        this.identifiers = identifiers;
        this.properties = properties;
    }

    public void validate(DataHubCreateRequest request)
    {
        if (request == null) throw new ServiceException("缺少建表配置");
        String displayName = request.getDisplayName() == null ? "" : request.getDisplayName().strip();
        if (displayName.isBlank() || displayName.length() > 255) throw new ServiceException("表名长度必须在1到255个字符之间");
        String physicalName = request.getPhysicalName() == null ? "" : request.getPhysicalName().strip();
        identifiers.requireIdentifier(physicalName, "英文表名只能包含小写字母、数字和下划线，且必须以字母开头");
        request.setDisplayName(displayName);
        request.setPhysicalName(physicalName);
        if (request.getTargetFolderId() != null && request.getTargetFolderId() <= 0)
            throw new ServiceException("目标文件夹编号不合法");
        if (request.getColumns() == null || request.getColumns().isEmpty()) throw new ServiceException("至少需要保留一个字段");
        if (request.getColumns().size() > properties.getMaxColumns())
            throw new ServiceException("字段数量不能超过" + properties.getMaxColumns());

        Set<Integer> indexes = new HashSet<>();
        Set<String> names = new HashSet<>();
        for (DataHubColumnDefinition column : request.getColumns())
        {
            if (column == null) throw new ServiceException("字段配置不能为空");
            if (column.getSourceIndex() == null || column.getSourceIndex() < 0 || !indexes.add(column.getSourceIndex()))
                throw new ServiceException("来源列序号重复或不合法");
            String sourceName = column.getSourceName() == null ? "" : column.getSourceName().strip();
            if (sourceName.isBlank() || sourceName.length() > 255) throw new ServiceException("来源列名长度必须在1到255个字符之间");
            String columnDisplayName = column.getDisplayName() == null ? "" : column.getDisplayName().strip();
            if (columnDisplayName.isBlank() || columnDisplayName.length() > 255)
                throw new ServiceException("字段显示名称长度必须在1到255个字符之间");
            String physicalColumn = column.getPhysicalName() == null ? "" : column.getPhysicalName().strip();
            identifiers.requireIdentifier(physicalColumn, "英文列名只能包含小写字母、数字和下划线");
            if (!names.add(physicalColumn)) throw new ServiceException("英文列名不能重复：" + physicalColumn);
            DataHubColumnType type = DataHubColumnType.from(column.getDataType());
            column.setSourceName(sourceName);
            column.setDisplayName(columnDisplayName);
            column.setPhysicalName(physicalColumn);
            column.setDataType(type.name());
            column.setNullable(!Boolean.FALSE.equals(column.getNullable()));
            normalizeAuditFields(column);
            if (type == DataHubColumnType.VARCHAR)
            {
                int length = column.getLength() == null ? 255 : column.getLength();
                if (length < 1 || length > 1000) throw new ServiceException("VARCHAR长度必须在1到1000之间");
            }
            if (type == DataHubColumnType.DECIMAL)
            {
                int precision = column.getPrecision() == null ? 18 : column.getPrecision();
                int scale = column.getScale() == null ? 2 : column.getScale();
                if (precision < 1 || precision > 38 || scale < 0 || scale > precision)
                    throw new ServiceException("DECIMAL精度配置不合法");
            }
        }
        validateEstimatedRowSize(request.getColumns());
    }

    public void validateAgainstSource(DataHubCreateRequest request, ParsedSpreadsheet parsed)
    {
        validate(request);
        if (request.getColumns().size() != parsed.getHeaders().size())
            throw new ServiceException("字段数量与预览时不一致，请重新预览文件");
        for (DataHubColumnDefinition column : request.getColumns())
        {
            int index = column.getSourceIndex();
            if (index >= parsed.getHeaders().size() || !parsed.getHeaders().get(index).equals(column.getSourceName()))
                throw new ServiceException("来源字段已发生变化，请重新预览文件");
        }
        Set<Integer> expected = new HashSet<>();
        for (int i = 0; i < parsed.getHeaders().size(); i++) expected.add(i);
        for (DataHubColumnDefinition column : request.getColumns()) expected.remove(column.getSourceIndex());
        if (!expected.isEmpty()) throw new ServiceException("存在未配置的来源字段，请重新预览文件");
    }

    private void normalizeAuditFields(DataHubColumnDefinition column)
    {
        String source = column.getTranslationSource();
        column.setTranslationSource(source == null || source.isBlank() ? "USER" : source.strip().substring(0, Math.min(32, source.strip().length())));
        List<String> samples = new ArrayList<>();
        if (column.getSamples() != null)
        {
            for (String sample : column.getSamples())
            {
                if (sample == null) continue;
                samples.add(sample.length() > 1000 ? sample.substring(0, 1000) : sample);
                if (samples.size() == 3) break;
            }
        }
        column.setSamples(samples);
    }

    private void validateEstimatedRowSize(List<DataHubColumnDefinition> columns)
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
        if (bytes > MAX_ESTIMATED_ROW_BYTES)
            throw new ServiceException("字段配置超过MySQL单行容量，请将部分VARCHAR字段改为TEXT");
    }
}
