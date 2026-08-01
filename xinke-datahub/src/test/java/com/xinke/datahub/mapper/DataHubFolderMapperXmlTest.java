package com.xinke.datahub.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.xinke.datahub.domain.DataHubFolder;

class DataHubFolderMapperXmlTest
{
    @Test
    void mapperRegistersFolderAndAclGuardedItemStatements() throws Exception
    {
        Configuration configuration = parse("mapper/datahub/DataHubFolderMapper.xml");
        String namespace = "com.xinke.datahub.mapper.DataHubFolderMapper.";
        assertTrue(configuration.hasStatement(namespace + "selectOwnedFolderList"));
        assertTrue(configuration.hasStatement(namespace + "selectOwnedFolderListForUpdate"));
        assertTrue(configuration.hasStatement(namespace + "updateFolder"));
        assertTrue(configuration.hasStatement(namespace + "countVisibleDataset"));
        assertTrue(configuration.hasStatement(namespace + "updateFolderItem"));

        String visibleSql = configuration.getMappedStatement(namespace + "countVisibleDataset")
                .getBoundSql(Map.of("datasetId", 3L, "userId", 7L, "admin", false))
                .getSql().replaceAll("\\s+", " ");
        assertTrue(visibleSql.contains("dh_dataset_acl"));
        assertTrue(visibleSql.contains("sys_user_role"));
        assertTrue(visibleSql.contains("r.status = '0'"));

        DataHubFolder folder = new DataHubFolder();
        folder.setFolderId(1L);
        folder.setOwnerUserId(7L);
        folder.setLockVersion(2);
        String updateSql = configuration.getMappedStatement(namespace + "updateFolder")
                .getBoundSql(folder).getSql().replaceAll("\\s+", " ");
        assertTrue(updateSql.contains("lock_version = lock_version + 1"));
        assertTrue(updateSql.contains("lock_version = ?"));
    }

    private Configuration parse(String resource) throws Exception
    {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input);
            Configuration configuration = new Configuration();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
            return configuration;
        }
    }
}
