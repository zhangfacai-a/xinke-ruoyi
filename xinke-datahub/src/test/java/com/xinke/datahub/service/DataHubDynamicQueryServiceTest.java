package com.xinke.datahub.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.DataHubColumn;
import com.xinke.datahub.domain.DataHubDataVersion;
import com.xinke.datahub.domain.dto.DataHubDataQuery;
import com.xinke.datahub.naming.DataHubIdentifierService;

class DataHubDynamicQueryServiceTest
{
    @Test
    void serializesIdentifiersNumbersAndDatesWithoutBrowserPrecisionOrFormatLoss()
    {
        RecordingJdbcTemplate jdbcTemplate = new RecordingJdbcTemplate();
        DataHubDynamicQueryService service = new DataHubDynamicQueryService(jdbcTemplate,
                new DataHubIdentifierService(new DataHubProperties()));
        DataHubDataVersion version = new DataHubDataVersion();
        version.setPhysicalTableName("dh_data_test");

        service.query(version, List.of(
                column(1L, "large_id", "BIGINT"),
                column(2L, "amount", "DECIMAL"),
                column(3L, "business_date", "DATE"),
                column(4L, "updated_at", "DATETIME"),
                column(5L, "description", "VARCHAR")), new DataHubDataQuery());

        String sql = jdbcTemplate.querySql.replaceAll("\\s+", " ");
        assertTrue(sql.contains("cast(`_id` as char) as `_id`"));
        assertTrue(sql.contains("cast(`large_id` as char) as `large_id`"));
        assertTrue(sql.contains("cast(`amount` as char) as `amount`"));
        assertTrue(sql.contains("date_format(`business_date`, '%Y-%m-%d') as `business_date`"));
        assertTrue(sql.contains("date_format(`updated_at`, '%Y-%m-%d %H:%i:%s') as `updated_at`"));
        assertTrue(sql.contains(",`description`"));
    }

    private DataHubColumn column(Long id, String physicalName, String type)
    {
        DataHubColumn column = new DataHubColumn();
        column.setColumnId(id);
        column.setPhysicalName(physicalName);
        column.setDataType(type);
        return column;
    }

    private static class RecordingJdbcTemplate extends JdbcTemplate
    {
        private String querySql;

        @Override
        public <T> T queryForObject(String sql, Class<T> requiredType, Object... args)
        {
            return requiredType.cast(0L);
        }

        @Override
        public List<Map<String, Object>> queryForList(String sql, Object... args)
        {
            querySql = sql;
            return List.of();
        }
    }
}
