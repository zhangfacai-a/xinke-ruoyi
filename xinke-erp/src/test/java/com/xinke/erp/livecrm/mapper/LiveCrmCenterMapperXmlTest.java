package com.xinke.erp.livecrm.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LiveCrmCenterMapperXmlTest {
    @Test
    void registersQualifiedCustomerCenterStatements() {
        String resource = "mapper/erp/LiveCrmCenterMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, "LiveCrmCenterMapper.xml should be available on the runtime classpath");
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        } catch (Exception exception) {
            throw new AssertionError("MyBatis could not load LiveCrmCenterMapper.xml", exception);
        }

        String namespace = LiveCrmCenterMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "listCustomers"));
        assertTrue(configuration.hasStatement(namespace + "listCommentContents"));
        assertTrue(configuration.hasStatement(namespace + "dashboard"));
        assertTrue(configuration.hasStatement(namespace + "ownerRanking"));
        assertTrue(configuration.hasStatement(namespace + "roomStats"));
        assertTrue(configuration.hasStatement(namespace + "exportCustomers"));

        Map<String, Object> params = new HashMap<>();
        params.put("beginAt", "2026-09-30 00:00:00");
        params.put("endAt", "2026-10-01 00:00:00");
        params.put("roomNameKey", null);
        params.put("queue", "INBOX");
        params.put("keyword", null);
        params.put("stage", null);
        params.put("commentedOnly", false);
        params.put("offset", 0);
        params.put("limit", 20);
        String sql = configuration.getMappedStatement(namespace + "listCustomers")
                .getBoundSql(params).getSql();
        assertTrue(sql.contains("qualified_presence"));
        assertTrue(sql.contains("$.watchSeconds"));
        assertTrue(sql.contains(">=300"));
        assertTrue(sql.contains("count(distinct sessionId)"));
        assertTrue(sql.contains("lastCommentId"));

        String dashboardSql = configuration.getMappedStatement(namespace + "dashboard")
                .getBoundSql(params).getSql();
        assertTrue(dashboardSql.contains("claimedCommented"));
        assertTrue(dashboardSql.contains("unclaimedCommented"));

        String roomStatsSql = configuration.getMappedStatement(namespace + "roomStats")
                .getBoundSql(params).getSql();
        assertTrue(roomStatsSql.contains("commented_customer"));
        assertTrue(roomStatsSql.contains("claimedCustomers"));

        params.put("queue", "FOLLOWUP");
        params.put("stage", "CONSULTING");
        params.put("commentedOnly", true);
        String exportSql = configuration.getMappedStatement(namespace + "exportCustomers")
                .getBoundSql(params).getSql();
        assertTrue(exportSql.contains("owner_user_id"));
        assertTrue(exportSql.contains("consult_stage"));
        assertTrue(exportSql.contains("commentCount>0"));
        assertTrue(exportSql.contains("orderNos"));
        assertTrue(exportSql.contains("screenshotCount"));
        assertTrue(exportSql.contains("dy_audience_v2_attachment"));
    }
}
