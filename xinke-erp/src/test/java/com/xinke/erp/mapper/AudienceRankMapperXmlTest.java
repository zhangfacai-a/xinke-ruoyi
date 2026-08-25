package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.xinke.erp.domain.audience.AudienceRankQuery;
import com.xinke.erp.domain.audience.AudienceFollowup;

class AudienceRankMapperXmlTest
{
    @Test
    void registersFollowupStatementsInMyBatis()
    {
        String resource = "mapper/erp/AudienceRankMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input, "AudienceRankMapper.xml should be available on the runtime classpath");
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }
        catch (Exception exception)
        {
            throw new AssertionError("MyBatis could not load AudienceRankMapper.xml", exception);
        }

        String namespace = AudienceRankMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "selectFollowupList"));
        assertTrue(configuration.hasStatement(namespace + "selectFollowupSummary"));
        assertTrue(configuration.hasStatement(namespace + "selectFollowupLogs"));
        assertTrue(configuration.hasStatement(namespace + "selectFollowupVisits"));
        assertTrue(configuration.hasStatement(namespace + "selectFollowupVisitStats"));
        assertTrue(configuration.hasStatement(namespace + "selectTeamOverview"));
        assertTrue(configuration.hasStatement(namespace + "selectTeamFunnel"));
        assertTrue(configuration.hasStatement(namespace + "selectOwnerPerformance"));
        assertTrue(configuration.hasStatement(namespace + "selectRoomPerformance"));
        assertTrue(configuration.hasStatement(namespace + "selectDailyTrend"));
        assertTrue(configuration.hasStatement(namespace + "updateFollowup"));
        assertTrue(configuration.hasStatement(namespace + "insertFollowups"));
        String snapshotSql = configuration.getMappedStatement(namespace + "selectSnapshotList")
                .getBoundSql(new AudienceRankQuery()).getSql();
        assertTrue(snapshotSql.contains("dy_audience_followup"));
        assertTrue(snapshotSql.contains("f.followup_id"));
        String updateSql = configuration.getMappedStatement(namespace + "updateFollowup")
                .getBoundSql(new AudienceFollowup()).getSql();
        assertTrue(updateSql.contains("status <=> ?"));
        assertTrue(updateSql.indexOf("status_changed_at") < updateSql.indexOf("status = ?"));
        assertTrue(updateSql.contains("room_id = ?"));
        assertTrue(updateSql.contains("owner_user_id = ?"));
        assertTrue(updateSql.contains("follow_result_code = ?"));
        assertTrue(updateSql.contains("intent_level = ?"));
    }
}
