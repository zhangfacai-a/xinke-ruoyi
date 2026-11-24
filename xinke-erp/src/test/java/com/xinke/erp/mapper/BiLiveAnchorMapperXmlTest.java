package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class BiLiveAnchorMapperXmlTest
{
    @Test
    void registersIdempotentSyncSoftDeleteAndReconciliation()
    {
        String resource="mapper/erp/BiLiveAnchorMapper.xml";
        Configuration configuration=new Configuration();
        try(InputStream input=Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input);
            new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
        }
        catch(Exception exception){throw new AssertionError("BiLiveAnchorMapper.xml could not load",exception);}
        String namespace=BiLiveAnchorMapper.class.getName()+".";
        String upsert=configuration.getMappedStatement(namespace+"upsertSegment").getBoundSql(Map.of("segmentId",1L)).getSql();
        assertTrue(upsert.contains("on duplicate key update"));
        assertTrue(upsert.contains("g.segment_id=?"));
        assertTrue(upsert.contains("timestampdiff(second,g.start_time,g.end_time)"));
        assertTrue(upsert.contains("SESSION_STALE"));
        String recent=configuration.getMappedStatement(namespace+"upsertRecentSegments").getBoundSql(Map.of()).getSql();
        assertTrue(recent.contains("bx.source_version<>g.version"));
        assertTrue(recent.contains("bx.is_deleted=1"));
        String deleted=configuration.getMappedStatement(namespace+"markMissingDeleted").getBoundSql(Map.of()).getSql();
        assertTrue(deleted.contains("g.segment_id is null"));
        assertTrue(deleted.contains("g.status<>'CLOSED'"));
        String reconcile=configuration.getMappedStatement(namespace+"reconcile").getBoundSql(Map.of()).getSql();
        assertTrue(reconcile.contains("source_count=bi_count"));
        assertTrue(reconcile.contains("source_duration=bi_duration"));
        assertTrue(reconcile.contains("source_interaction=bi_interaction"));
        assertTrue(reconcile.contains("source_new_fan=bi_new_fan"));
        assertTrue(reconcile.contains("source_gmv=bi_gmv"));
        assertTrue(reconcile.contains("source_presale=bi_presale"));
        assertTrue(migration().contains("CREATE TABLE IF NOT EXISTS bi_live_anchor_segment_fact"));
        assertTrue(migration().contains("ON DUPLICATE KEY UPDATE"));
        assertTrue(migration().contains("g.status<>'CLOSED'"));
    }

    private String migration()
    {
        try
        {
            Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();
            if(root.getFileName().toString().equals("xinke-erp"))root=root.getParent();
            return Files.readString(root.resolve("config/database/add-bi-live-anchor-segment-fact.sql"));
        }
        catch(Exception exception){throw new AssertionError("无法读取主播 BI 建表脚本",exception);}
    }
}
