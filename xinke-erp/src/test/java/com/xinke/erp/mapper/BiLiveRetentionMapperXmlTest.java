package com.xinke.erp.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class BiLiveRetentionMapperXmlTest {
    @Test void loadsOrderDetailSyncStatements() {
        String resource="mapper/erp/BiLiveRetentionMapper.xml";
        Configuration configuration=new Configuration();
        try(InputStream input=Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input,configuration,resource,configuration.getSqlFragments()).parse();
        } catch(Exception exception){throw new AssertionError("BI retention mapper cannot be loaded",exception);}

        String namespace=BiLiveRetentionMapper.class.getName()+".";
        String upsert=configuration.getMappedStatement(namespace+"upsertChanged").getBoundSql(null).getSql();
        assertTrue(upsert.contains("o.version"));
        assertTrue(upsert.contains("b.source_updated_at<>o.updated_at"));
        assertTrue(upsert.contains("completion_seconds"));
        assertFalse(upsert.contains("o.address"));
        assertFalse(upsert.contains("o.latest_note"));
        String deleted=configuration.getMappedStatement(namespace+"markMissingDeleted").getBoundSql(null).getSql();
        assertTrue(deleted.contains("o.id IS NULL AND b.is_deleted=0"));
        assertTrue(configuration.hasStatement(namespace+"reconcile"));
    }
}
