package com.xinke.datahub.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataset;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubColumnMapping;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.domain.dto.DataHubMutationPreviewResponse;
import com.xinke.datahub.domain.dto.DataHubPreviewResponse;
import com.xinke.datahub.parser.ParsedRow;
import com.xinke.datahub.parser.ParsedSpreadsheet;

@Service
public class DataHubMutationSchemaService
{
    private final DataHubSchemaInferenceService inferenceService;
    private final DataHubDefinitionValidator validator;

    public DataHubMutationSchemaService(DataHubSchemaInferenceService inferenceService,
            DataHubDefinitionValidator validator)
    {
        this.inferenceService = inferenceService;
        this.validator = validator;
    }

    public DataHubMutationPreviewResponse preview(String previewId, String fileName, String operation,
            DataHubDataset dataset, DataHubDataVersion version, List<DataHubColumn> targetColumns,
            ParsedSpreadsheet parsed, java.util.Date expiresAt, Map<String, String> dictionary)
    {
        DataHubPreviewResponse inferred = inferenceService.buildPreview(previewId, fileName, parsed, expiresAt, dictionary);
        DataHubMutationPreviewResponse response = new DataHubMutationPreviewResponse();
        response.setPreviewId(inferred.getPreviewId());
        response.setFileName(inferred.getFileName());
        response.setSheetNames(inferred.getSheetNames());
        response.setSheetName(inferred.getSheetName());
        response.setDisplayName(dataset.getDisplayName());
        response.setPhysicalName(dataset.getDatasetCode());
        response.setColumns(inferred.getColumns());
        response.setSampleRows(inferred.getSampleRows());
        response.setWarnings(inferred.getWarnings());
        response.setTotalRows(inferred.getTotalRows());
        response.setExpiresAt(inferred.getExpiresAt());
        response.setDatasetId(dataset.getDatasetId());
        response.setBaseVersionId(version.getVersionId());
        response.setOperation(operation);
        response.setTargetColumns(targetColumns);

        List<DataHubColumnMapping> suggestions = suggestMappings(parsed.getHeaders(), targetColumns);
        response.setSuggestedMappings(suggestions);
        Map<Integer, Long> bySource = new HashMap<>();
        for (DataHubColumnMapping mapping : suggestions) bySource.put(mapping.getSourceIndex(), mapping.getTargetColumnId());
        for (DataHubColumnDefinition column : response.getColumns())
            column.setTargetColumnId(bySource.get(column.getSourceIndex()));
        if (suggestions.size() != targetColumns.size())
            response.getWarnings().add("部分来源字段无法自动匹配，请在确认前完成全部字段映射");
        return response;
    }

    public DataHubCreateRequest lockedRequest(DataHubDataset dataset, List<DataHubColumn> targetColumns,
            ParsedSpreadsheet parsed, List<DataHubColumnMapping> mappings)
    {
        if (mappings == null || mappings.size() != parsed.getHeaders().size()
                || mappings.size() != targetColumns.size())
            throw new ServiceException("必须为每个来源字段和目标字段建立一一映射");

        Map<Long, DataHubColumn> targets = new HashMap<>();
        for (DataHubColumn column : targetColumns) targets.put(column.getColumnId(), column);
        Set<Integer> sourceIndexes = new HashSet<>();
        Set<Long> targetIds = new HashSet<>();
        List<DataHubColumnDefinition> definitions = new ArrayList<>();
        for (DataHubColumnMapping mapping : mappings)
        {
            if (mapping == null || mapping.getSourceIndex() == null || mapping.getTargetColumnId() == null)
                throw new ServiceException("字段映射不能为空");
            int sourceIndex = mapping.getSourceIndex();
            if (sourceIndex < 0 || sourceIndex >= parsed.getHeaders().size() || !sourceIndexes.add(sourceIndex))
                throw new ServiceException("来源字段映射重复或不合法");
            DataHubColumn target = targets.get(mapping.getTargetColumnId());
            if (target == null || !targetIds.add(target.getColumnId())) throw new ServiceException("目标字段映射重复或不存在");
            definitions.add(definition(sourceIndex, parsed, target));
        }
        if (sourceIndexes.size() != parsed.getHeaders().size() || targetIds.size() != targetColumns.size())
            throw new ServiceException("来源字段与目标字段必须完整一一映射");
        Map<Long, Integer> targetOrder = new HashMap<>();
        for (DataHubColumn column : targetColumns) targetOrder.put(column.getColumnId(), column.getOrdinalPosition());
        definitions.sort((left, right) -> Integer.compare(targetOrder.get(left.getTargetColumnId()),
                targetOrder.get(right.getTargetColumnId())));

        DataHubCreateRequest request = new DataHubCreateRequest();
        request.setDisplayName(dataset.getDisplayName());
        request.setPhysicalName(dataset.getDatasetCode());
        request.setColumns(definitions);
        validator.validateAgainstSource(request, parsed);
        return request;
    }

    public List<DataHubColumnMapping> suggestMappings(List<String> headers, List<DataHubColumn> columns)
    {
        List<DataHubColumnMapping> mappings = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        for (int index = 0; index < headers.size(); index++)
        {
            DataHubColumn matched = findTarget(headers.get(index), columns, used);
            if (matched == null) continue;
            DataHubColumnMapping mapping = new DataHubColumnMapping();
            mapping.setSourceIndex(index);
            mapping.setTargetColumnId(matched.getColumnId());
            mappings.add(mapping);
            used.add(matched.getColumnId());
        }
        return mappings;
    }

    private DataHubColumn findTarget(String header, List<DataHubColumn> columns, Set<Long> used)
    {
        String key = normalize(header);
        for (int pass = 0; pass < 3; pass++)
        {
            for (DataHubColumn column : columns)
            {
                if (used.contains(column.getColumnId())) continue;
                String candidate = switch (pass)
                {
                    case 0 -> column.getSourceName();
                    case 1 -> column.getDisplayName();
                    default -> column.getPhysicalName();
                };
                if (key.equals(normalize(candidate))) return column;
            }
        }
        return null;
    }

    private DataHubColumnDefinition definition(int sourceIndex, ParsedSpreadsheet parsed, DataHubColumn target)
    {
        DataHubColumnDefinition definition = new DataHubColumnDefinition();
        definition.setTargetColumnId(target.getColumnId());
        definition.setSourceIndex(sourceIndex);
        definition.setSourceName(parsed.getHeaders().get(sourceIndex));
        definition.setDisplayName(target.getDisplayName());
        definition.setPhysicalName(target.getPhysicalName());
        definition.setDataType(target.getDataType());
        definition.setLength(target.getColumnLength());
        definition.setPrecision(target.getNumericPrecision());
        definition.setScale(target.getNumericScale());
        definition.setNullable(target.getNullable());
        definition.setNeedsReview(Boolean.FALSE);
        definition.setTranslationSource("CURRENT_SCHEMA");
        List<String> samples = new ArrayList<>();
        for (ParsedRow row : parsed.getRows())
        {
            String value = sourceIndex < row.getValues().size() ? row.getValues().get(sourceIndex) : "";
            if (!value.isBlank() && !samples.contains(value)) samples.add(value);
            if (samples.size() == 3) break;
        }
        definition.setSamples(samples);
        return definition;
    }

    private String normalize(String value)
    {
        return value == null ? "" : value.strip().replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }
}
