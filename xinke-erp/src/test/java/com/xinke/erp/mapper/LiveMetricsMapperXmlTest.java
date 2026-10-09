package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LiveMetricsMapperXmlTest
{
    @Test
    void registersV2ReportingStatementsInMyBatis()
    {
        String resource = "mapper/erp/LiveMetricsMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input, "LiveMetricsMapper.xml should be available on the runtime classpath");
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        catch (Exception exception)
        {
            throw new AssertionError("MyBatis could not load LiveMetricsMapper.xml", exception);
        }

        String namespace = LiveMetricsMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "findLiveSessionByRoom"));
        assertTrue(configuration.hasStatement(namespace + "insertRawSubmission"));
        assertTrue(configuration.hasStatement(namespace + "listSessionSubmissions"));
        assertTrue(configuration.hasStatement(namespace + "listReportingRows"));
        String shiftSql = configuration.getMappedStatement(namespace + "listShifts")
                .getBoundSql(Map.of()).getSql();
        assertTrue(shiftSql.contains("live_metric_submission"));
        assertTrue(shiftSql.contains("exposure_delta"));
        String matchSql = configuration.getMappedStatement(namespace + "findAutoMatchShifts")
                .getBoundSql(Map.of("roomId", 1L, "sessionId", 2L,
                        "liveStartTime", "2026-09-29 08:00:00",
                        "snapshotAt", "2026-09-29 12:05:00")).getSql();
        assertTrue(matchSql.contains("s.start_time<="));
        assertTrue(matchSql.contains("s.end_time>=date_sub"));
        assertTrue(matchSql.contains("currentShift"));
        assertTrue(matchSql.contains("hasSubmission"));
        assertTrue(matchSql.contains("abs(timestampdiff(second,s.end_time,"));
        assertTrue(configuration.hasStatement(namespace + "refreshShiftStatus"));
        String sql = configuration.getMappedStatement(namespace + "listReportingRows")
                .getBoundSql(Map.of()).getSql();
        assertTrue(sql.contains("live_metric_live_session"));
        assertTrue(sql.contains("live_metric_submission"));
    }
}
