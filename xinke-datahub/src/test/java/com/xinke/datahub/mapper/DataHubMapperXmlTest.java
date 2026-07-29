package com.xinke.datahub.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class DataHubMapperXmlTest
{
    @Test
    void mapperXmlParsesAndRegistersCoreStatements() throws Exception
    {
        String resource = "mapper/datahub/DataHubMapper.xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input);
            Configuration configuration = new Configuration();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            assertTrue(configuration.hasStatement("com.xinke.datahub.mapper.DataHubMapper.insertImportJob"));
            assertTrue(configuration.hasStatement("com.xinke.datahub.mapper.DataHubMapper.selectDatasetList"));
            assertTrue(configuration.hasStatement("com.xinke.datahub.mapper.DataHubMapper.selectAccessMask"));
            assertTrue(configuration.hasStatement("com.xinke.datahub.mapper.DataHubMapper.claimQueuedImportJob"));
            assertTrue(configuration.hasStatement("com.xinke.datahub.mapper.DataHubMapper.requeueStaleImportJobs"));
            assertTrue(configuration.hasStatement("com.xinke.datahub.mapper.DataHubMapper.countClaimedImportJob"));
            String recoverySql = configuration
                    .getMappedStatement("com.xinke.datahub.mapper.DataHubMapper.requeueStaleImportJobs")
                    .getBoundSql(new java.util.Date()).getSql().replaceAll("\\s+", " ");
            assertTrue(recoverySql.contains("lock_version = lock_version + 1"));
        }
    }
}
