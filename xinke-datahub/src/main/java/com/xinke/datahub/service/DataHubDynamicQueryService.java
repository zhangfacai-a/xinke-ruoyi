package com.xinke.datahub.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.dto.DataHubDataFilter;
import com.xinke.datahub.domain.dto.DataHubDataPage;
import com.xinke.datahub.domain.dto.DataHubDataQuery;
import com.xinke.datahub.enums.DataHubColumnType;
import com.xinke.datahub.naming.DataHubIdentifierService;

@Service
public class DataHubDynamicQueryService
{
    private static final Set<String> OPERATORS = Set.of(
            "EQ", "NE", "CONTAINS", "GT", "GTE", "LT", "LTE", "BETWEEN", "IS_NULL", "IS_NOT_NULL");

    private final JdbcTemplate jdbcTemplate;
    private final DataHubIdentifierService identifiers;

    public DataHubDynamicQueryService(JdbcTemplate jdbcTemplate, DataHubIdentifierService identifiers)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.identifiers = identifiers;
    }

    public DataHubDataPage query(DataHubDataVersion version, List<DataHubColumn> columns, DataHubDataQuery request)
    {
        DataHubDataQuery query = request == null ? new DataHubDataQuery() : request;
        int pageNum = query.getPageNum() == null ? 1 : Math.max(1, query.getPageNum());
        int pageSize = query.getPageSize() == null ? 20 : Math.min(100, Math.max(1, query.getPageSize()));
        List<DataHubDataFilter> filters = query.getFilters() == null ? List.of() : query.getFilters();
        if (filters.size() > 20) throw new ServiceException("查询条件不能超过20个");

        Map<Long, DataHubColumn> byId = new HashMap<>();
        for (DataHubColumn column : columns) byId.put(column.getColumnId(), column);
        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> arguments = new ArrayList<>();
        for (DataHubDataFilter filter : filters) appendFilter(where, arguments, byId, filter);

        String table = identifiers.qualifiedTable(version.getPhysicalTableName());
        Long total = jdbcTemplate.queryForObject("select count(1) from " + table + where, Long.class, arguments.toArray());

        StringBuilder select = new StringBuilder(
                "select cast(`_id` as char) as `_id`, cast(`_import_job_id` as char) as `_import_job_id`, "
                + "cast(`_source_row_no` as char) as `_source_row_no`, "
                + "lower(hex(`_row_hash`)) as `_row_hash`, `_created_at`");
        for (DataHubColumn column : columns)
        {
            String name = identifiers.quote(column.getPhysicalName());
            DataHubColumnType type = DataHubColumnType.from(column.getDataType());
            if (type == DataHubColumnType.BIGINT || type == DataHubColumnType.DECIMAL)
                select.append(",cast(").append(name).append(" as char) as ").append(name);
            else if (type == DataHubColumnType.DATE)
                select.append(",date_format(").append(name).append(", '%Y-%m-%d') as ").append(name);
            else if (type == DataHubColumnType.DATETIME)
                select.append(",date_format(").append(name)
                        .append(", '%Y-%m-%d %H:%i:%s') as ").append(name);
            else
                select.append(',').append(name);
        }
        select.append(" from ").append(table).append(where).append(" order by ");
        DataHubColumn sortColumn = query.getSortColumnId() == null ? null : byId.get(query.getSortColumnId());
        select.append(sortColumn == null ? "`_id`" : identifiers.quote(sortColumn.getPhysicalName()));
        select.append("ASC".equalsIgnoreCase(query.getSortDirection()) ? " asc" : " desc");
        select.append(" limit ? offset ?");
        arguments.add(pageSize);
        arguments.add((long) (pageNum - 1) * pageSize);

        DataHubDataPage page = new DataHubDataPage();
        page.setTotal(total == null ? 0 : total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        page.setRows(jdbcTemplate.queryForList(select.toString(), arguments.toArray()));
        return page;
    }

    private void appendFilter(StringBuilder sql, List<Object> arguments, Map<Long, DataHubColumn> columns,
            DataHubDataFilter filter)
    {
        if (filter == null || filter.getColumnId() == null) throw new ServiceException("查询字段不能为空");
        DataHubColumn column = columns.get(filter.getColumnId());
        if (column == null) throw new ServiceException("查询字段不存在");
        String operator = filter.getOperator() == null ? "" : filter.getOperator().toUpperCase(Locale.ROOT);
        if (!OPERATORS.contains(operator)) throw new ServiceException("不支持的查询操作符");
        DataHubColumnType type = DataHubColumnType.from(column.getDataType());
        String name = identifiers.quote(column.getPhysicalName());
        switch (operator)
        {
            case "EQ" -> { sql.append(" and ").append(name).append(" = ?"); arguments.add(type.convert(filter.getValue())); }
            case "NE" -> { sql.append(" and ").append(name).append(" <> ?"); arguments.add(type.convert(filter.getValue())); }
            case "GT" -> { sql.append(" and ").append(name).append(" > ?"); arguments.add(type.convert(filter.getValue())); }
            case "GTE" -> { sql.append(" and ").append(name).append(" >= ?"); arguments.add(type.convert(filter.getValue())); }
            case "LT" -> { sql.append(" and ").append(name).append(" < ?"); arguments.add(type.convert(filter.getValue())); }
            case "LTE" -> { sql.append(" and ").append(name).append(" <= ?"); arguments.add(type.convert(filter.getValue())); }
            case "CONTAINS" -> {
                if (type != DataHubColumnType.VARCHAR && type != DataHubColumnType.TEXT)
                    throw new ServiceException("只有文本字段支持包含查询");
                sql.append(" and ").append(name).append(" like ?");
                arguments.add("%" + (filter.getValue() == null ? "" : filter.getValue()) + "%");
            }
            case "BETWEEN" -> {
                sql.append(" and ").append(name).append(" between ? and ?");
                arguments.add(type.convert(filter.getValue()));
                arguments.add(type.convert(filter.getValueTo()));
            }
            case "IS_NULL" -> sql.append(" and ").append(name).append(" is null");
            case "IS_NOT_NULL" -> sql.append(" and ").append(name).append(" is not null");
            default -> throw new ServiceException("不支持的查询操作符");
        }
    }
}
