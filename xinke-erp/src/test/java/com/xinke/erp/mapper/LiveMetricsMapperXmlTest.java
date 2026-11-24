package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LiveMetricsMapperXmlTest
{
    @Test
    void registersHandoverStatementsWithoutLegacySchedulingTables()
    {
        String resource="mapper/erp/LiveMetricsMapper.xml";
        Configuration configuration=new Configuration();
        try(InputStream input=Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input);
            new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
        }
        catch(Exception exception){throw new AssertionError("LiveMetricsMapper.xml could not load",exception);}
        String namespace=LiveMetricsMapper.class.getName()+".";
        assertTrue(configuration.hasStatement(namespace+"findHandoverSessionByPlatformId"));
        assertTrue(configuration.hasStatement(namespace+"insertHandoverSnapshot"));
        assertTrue(configuration.hasStatement(namespace+"insertHandoverSegment"));
        assertTrue(configuration.hasStatement(namespace+"countHandoverSegmentOverlap"));
        assertFalse(configuration.hasStatement(namespace+"closeHandoverSegment"));
        assertTrue(configuration.hasStatement(namespace+"insertHandoverCorrection"));
        String sql=configuration.getMappedStatement(namespace+"listHandoverRooms").getBoundSql(null).getSql();
        assertTrue(sql.contains("live_metric_anchor_segment"));
        assertFalse(sql.contains("live_metric_shift "));
        String segmentSql=configuration.getMappedStatement(namespace+"listHandoverSegments").getBoundSql(Map.of("sessionId",1L)).getSql();
        assertTrue(segmentSql.contains("g.exposure_watch_rate"));
        assertTrue(migrationContains("exposure_watch_rate"),"数据库升级脚本必须包含详情查询使用的区间进房率字段");
        assertTrue(manualMigrationContains("start_snapshot_id BIGINT NULL"),"手工记录必须允许不绑定快照");
    }

    private boolean migrationContains(String value)
    {
        try
        {
            Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();
            if(root.getFileName().toString().equals("xinke-erp"))root=root.getParent();
            return Files.readString(root.resolve("config/database/upgrade-live-metrics-website-v6.sql")).contains(value);
        }
        catch(Exception exception){throw new AssertionError("无法读取主播数据升级脚本",exception);}
    }

    private boolean manualMigrationContains(String value)
    {
        try
        {
            Path root=Path.of(System.getProperty("user.dir")).toAbsolutePath();
            if(root.getFileName().toString().equals("xinke-erp"))root=root.getParent();
            return Files.readString(root.resolve("config/database/upgrade-live-metrics-manual-v7.sql")).contains(value);
        }
        catch(Exception exception){throw new AssertionError("无法读取主播手工数据升级脚本",exception);}
    }
}
