package com.xinke.datahub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.naming.DataHubIdentifierService;

class DataHubDynamicTableServiceTest
{
    @Test
    void insertsMultipleRowsWithOneStatementPerChunk()
    {
        DataHubProperties properties = new DataHubProperties();
        properties.setInsertBatchSize(2);
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate(2, 1);
        DataHubDynamicTableService service = new DataHubDynamicTableService(jdbcTemplate,
                new DataHubIdentifierService(properties), properties);

        List<DataHubColumnDefinition> columns = List.of(column("name"), column("amount"));
        List<PreparedDataRow> rows = List.of(
                row(2, "甲", 10),
                row(3, "乙", 20),
                row(4, "丙", 30));
        List<Long> progress = new ArrayList<>();

        service.insertRows("dh_data_test", 9L, columns, rows, progress::add);

        assertEquals(2, jdbcTemplate.sql.size());
        assertTrue(jdbcTemplate.sql.get(0).contains("values (?,?,?,?,?),(?,?,?,?,?)"));
        assertEquals(10, jdbcTemplate.arguments.get(0).length);
        assertEquals(5, jdbcTemplate.arguments.get(1).length);
        assertEquals(List.of(2L, 3L), progress);
    }

    private DataHubColumnDefinition column(String physicalName)
    {
        DataHubColumnDefinition column = new DataHubColumnDefinition();
        column.setPhysicalName(physicalName);
        return column;
    }

    private PreparedDataRow row(int sourceRowNo, Object... values)
    {
        return new PreparedDataRow(sourceRowNo, new byte[] { 1, 2, 3 }, values);
    }

    private static class RecordingJdbcTemplate extends JdbcTemplate
    {
        private final List<Integer> updateCounts;
        private final List<String> sql = new ArrayList<>();
        private final List<Object[]> arguments = new ArrayList<>();

        RecordingJdbcTemplate(Integer... updateCounts)
        {
            this.updateCounts = List.of(updateCounts);
        }

        @Override
        public int update(String statement, Object... values)
        {
            sql.add(statement);
            arguments.add(values);
            return updateCounts.get(sql.size() - 1);
        }
    }
}
