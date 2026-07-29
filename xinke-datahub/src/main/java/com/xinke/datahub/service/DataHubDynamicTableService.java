package com.xinke.datahub.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.enums.DataHubColumnType;
import com.xinke.datahub.naming.DataHubIdentifierService;

@Service
public class DataHubDynamicTableService
{
    private static final int MAX_INSERT_PARAMETERS = 60000;

    private final JdbcTemplate jdbcTemplate;
    private final DataHubIdentifierService identifiers;
    private final DataHubProperties properties;

    public DataHubDynamicTableService(JdbcTemplate jdbcTemplate, DataHubIdentifierService identifiers,
            DataHubProperties properties)
    {
        this.jdbcTemplate = jdbcTemplate;
        this.identifiers = identifiers;
        this.properties = properties;
    }

    public void createTable(String tableName, List<DataHubColumnDefinition> columns)
    {
        StringBuilder sql = new StringBuilder("create table ").append(identifiers.qualifiedTable(tableName)).append(" (")
                .append("`_id` bigint not null auto_increment,")
                .append("`_import_job_id` bigint not null,")
                .append("`_source_row_no` bigint not null,")
                .append("`_row_hash` binary(32) default null,")
                .append("`_created_at` datetime(3) not null default current_timestamp(3),");
        for (DataHubColumnDefinition column : columns)
        {
            sql.append(identifiers.quote(column.getPhysicalName())).append(' ').append(sqlType(column));
            if (!Boolean.TRUE.equals(column.getNullable())) sql.append(" not null");
            sql.append(',');
        }
        sql.append("primary key (`_id`), key `idx_import_job` (`_import_job_id`), key `idx_row_hash` (`_row_hash`)")
                .append(") engine=InnoDB default charset=utf8mb4 comment='DataHub dynamic dataset'");
        jdbcTemplate.execute(sql.toString());
    }

    public void insertRows(String tableName, long jobId, List<DataHubColumnDefinition> columns,
            List<PreparedDataRow> rows, LongConsumer progress)
    {
        if (rows.isEmpty()) return;
        StringBuilder insertPrefix = new StringBuilder("insert into ").append(identifiers.qualifiedTable(tableName))
                .append(" (`_import_job_id`,`_source_row_no`,`_row_hash`");
        for (DataHubColumnDefinition column : columns)
            insertPrefix.append(',').append(identifiers.quote(column.getPhysicalName()));
        insertPrefix.append(')');

        int parametersPerRow = columns.size() + 3;
        String rowPlaceholders = "(" + "?,".repeat(parametersPerRow - 1) + "?)";
        int configuredBatchSize = Math.max(1, properties.getInsertBatchSize());
        int batchSize = Math.min(configuredBatchSize,
                Math.max(1, MAX_INSERT_PARAMETERS / parametersPerRow));
        long processed = 0;
        for (int start = 0; start < rows.size(); start += batchSize)
        {
            int end = Math.min(rows.size(), start + batchSize);
            StringBuilder sql = new StringBuilder(insertPrefix).append(" values ");
            List<Object> arguments = new ArrayList<>((end - start) * parametersPerRow);
            for (int i = start; i < end; i++)
            {
                if (i > start) sql.append(',');
                sql.append(rowPlaceholders);
                PreparedDataRow row = rows.get(i);
                if (row.values().length != columns.size())
                    throw new ServiceException("待写入数据与字段数量不一致");
                arguments.add(jobId);
                arguments.add(row.sourceRowNo());
                arguments.add(row.rowHash());
                for (Object value : row.values()) arguments.add(value);
            }
            int expectedRows = end - start;
            int insertedRows = jdbcTemplate.update(sql.toString(), arguments.toArray());
            if (insertedRows != expectedRows) throw new ServiceException("批量写入行数不一致");
            processed += insertedRows;
            progress.accept(processed);
        }
    }

    public void publishVersion(String stagingTable, String targetTable)
    {
        jdbcTemplate.execute("rename table " + identifiers.qualifiedTable(stagingTable)
                + " to " + identifiers.qualifiedTable(targetTable));
    }

    public void createTableLike(String sourceTable, String targetTable)
    {
        jdbcTemplate.execute("create table " + identifiers.qualifiedTable(targetTable)
                + " like " + identifiers.qualifiedTable(sourceTable));
    }

    public long copyRows(String sourceTable, String targetTable)
    {
        return jdbcTemplate.update("insert into " + identifiers.qualifiedTable(targetTable)
                + " select * from " + identifiers.qualifiedTable(sourceTable));
    }

    public Map<String, Object> selectRow(String tableName, long rowId, List<DataHubColumn> columns)
    {
        StringBuilder sql = new StringBuilder("select `_id`,`_source_row_no`,lower(hex(`_row_hash`)) as `_row_hash`");
        for (DataHubColumn column : columns)
            sql.append(',').append(identifiers.quote(column.getPhysicalName()));
        sql.append(" from ").append(identifiers.qualifiedTable(tableName)).append(" where `_id` = ?");
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql.toString(), rowId);
        if (rows.isEmpty()) throw new ServiceException("编辑的数据行不存在，请刷新后重试", HttpStatus.CONFLICT.value());
        return new LinkedHashMap<>(rows.get(0));
    }

    public void updateRow(String tableName, long jobId, long rowId, String expectedRowHash,
            List<DataHubColumn> columns, Object[] values, byte[] rowHash)
    {
        if (values.length != columns.size()) throw new ServiceException("编辑值与字段数量不一致");
        StringBuilder sql = new StringBuilder("update ").append(identifiers.qualifiedTable(tableName))
                .append(" set `_import_job_id` = ?, `_row_hash` = ?");
        List<Object> arguments = new ArrayList<>();
        arguments.add(jobId);
        arguments.add(rowHash);
        for (int i = 0; i < columns.size(); i++)
        {
            sql.append(',').append(identifiers.quote(columns.get(i).getPhysicalName())).append(" = ?");
            arguments.add(values[i]);
        }
        sql.append(" where `_id` = ? and `_row_hash` = unhex(?)");
        arguments.add(rowId);
        arguments.add(expectedRowHash);
        if (jdbcTemplate.update(sql.toString(), arguments.toArray()) != 1)
            throw new ServiceException("数据行已变化，请刷新后重试", HttpStatus.CONFLICT.value());
    }

    public void deleteRow(String tableName, long rowId, String expectedRowHash)
    {
        int deleted = jdbcTemplate.update("delete from " + identifiers.qualifiedTable(tableName)
                + " where `_id` = ? and `_row_hash` = unhex(?)", rowId, expectedRowHash);
        if (deleted != 1) throw new ServiceException("数据行已变化，请刷新后重试", HttpStatus.CONFLICT.value());
    }

    public long selectInsertedRowId(String tableName, long jobId, long sourceRowNo)
    {
        Long rowId = jdbcTemplate.queryForObject("select `_id` from " + identifiers.qualifiedTable(tableName)
                + " where `_import_job_id` = ? and `_source_row_no` = ?", Long.class, jobId, sourceRowNo);
        if (rowId == null) throw new ServiceException("新增数据行标识读取失败");
        return rowId;
    }

    public long selectMaxSourceRowNo(String tableName)
    {
        Long value = jdbcTemplate.queryForObject("select coalesce(max(`_source_row_no`), 0) from "
                + identifiers.qualifiedTable(tableName), Long.class);
        return value == null ? 0L : value;
    }

    public List<Map<String, Object>> selectRowsByImportJob(String tableName, long jobId,
            List<DataHubColumn> columns)
    {
        StringBuilder sql = new StringBuilder("select `_id`,`_source_row_no`,lower(hex(`_row_hash`)) as `_row_hash`");
        for (DataHubColumn column : columns)
            sql.append(',').append(identifiers.quote(column.getPhysicalName()));
        sql.append(" from ").append(identifiers.qualifiedTable(tableName))
                .append(" where `_import_job_id` = ? order by `_source_row_no`,`_id`");
        return jdbcTemplate.queryForList(sql.toString(), jobId);
    }

    public void requireTable(String tableName)
    {
        jdbcTemplate.queryForList("select 1 from " + identifiers.qualifiedTable(tableName) + " limit 0");
    }

    public void dropIfExists(String tableName)
    {
        if (tableName == null || tableName.isBlank()) return;
        jdbcTemplate.execute("drop table if exists " + identifiers.qualifiedTable(tableName));
    }

    public JdbcTemplate getJdbcTemplate()
    {
        return jdbcTemplate;
    }

    public DataHubIdentifierService getIdentifiers()
    {
        return identifiers;
    }

    private String sqlType(DataHubColumnDefinition column)
    {
        DataHubColumnType type = DataHubColumnType.from(column.getDataType());
        return switch (type)
        {
            case VARCHAR -> "varchar(" + validLength(column.getLength()) + ")";
            case TEXT -> "text";
            case BIGINT -> "bigint";
            case DECIMAL -> decimalType(column.getPrecision(), column.getScale());
            case DATE -> "date";
            case DATETIME -> "datetime(3)";
            case BOOLEAN -> "tinyint(1)";
        };
    }

    private int validLength(Integer length)
    {
        int value = length == null ? 255 : length;
        if (value < 1 || value > 1000) throw new ServiceException("VARCHAR长度必须在1到1000之间");
        return value;
    }

    private String decimalType(Integer precision, Integer scale)
    {
        int p = precision == null ? 18 : precision;
        int s = scale == null ? 2 : scale;
        if (p < 1 || p > 38 || s < 0 || s > p) throw new ServiceException("DECIMAL精度配置不合法");
        return "decimal(" + p + "," + s + ")";
    }
}
