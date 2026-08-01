package com.xinke.datahub.mapper;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.xinke.datahub.domain.DataHubImportJob;

class DataHubMutationMapperXmlTest
{
    private static final String NAMESPACE = "com.xinke.datahub.mapper.DataHubMutationMapper.";

    @Test
    void mapperXmlParsesAndKeepsPreviewAndPurgeRecoveryFields() throws Exception
    {
        String resource = "mapper/datahub/DataHubMutationMapper.xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input);
            Configuration configuration = new Configuration();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();

            String previewSql = sql(configuration, "updateMutationPreview", new DataHubImportJob());
            assertTrue(previewSql.contains("operation_payload_json = ?"));

            String claimSql = sql(configuration, "claimVersionForPurge", Map.of("versionId", 1L));
            assertTrue(claimSql.contains("status = 'PURGING'"));
            assertTrue(claimSql.contains("purge_claimed_at = sysdate(3)"));
            assertFalse(claimSql.contains("set v.status"));

            assertTrue(configuration.hasStatement(NAMESPACE + "selectStalePurgingVersions"));
            assertTrue(configuration.hasStatement(NAMESPACE + "reclaimStaleVersionForPurge"));
            assertTrue(configuration.hasStatement(NAMESPACE + "publishNewVersion"));
            assertTrue(configuration.hasStatement(NAMESPACE + "insertDataChanges"));
        }
    }

    private String sql(Configuration configuration, String statement, Object parameter)
    {
        return configuration.getMappedStatement(NAMESPACE + statement).getBoundSql(parameter).getSql()
                .replaceAll("\\s+", " ").strip();
    }
}
