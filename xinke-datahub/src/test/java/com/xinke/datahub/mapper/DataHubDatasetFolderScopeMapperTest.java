package com.xinke.datahub.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import com.xinke.datahub.domain.DataHubDataset;

class DataHubDatasetFolderScopeMapperTest
{
    @Test
    void folderScopeIsAppliedInTheAclFilteredDatasetQuery() throws Exception
    {
        String resource = "mapper/datahub/DataHubMapper.xml";
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource))
        {
            assertNotNull(input);
            Configuration configuration = new Configuration();
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();

            DataHubDataset query = new DataHubDataset();
            query.setFolderScope("FOLDER");
            query.setFolderId(12L);
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("query", query);
            parameters.put("userId", 7L);
            parameters.put("admin", false);
            String sql = configuration.getMappedStatement("com.xinke.datahub.mapper.DataHubMapper.selectDatasetList")
                    .getBoundSql(parameters).getSql().replaceAll("\\s+", " ");

            assertTrue(sql.contains("dh_folder_item fi"));
            assertTrue(sql.contains("fi.owner_user_id = ?"));
            assertTrue(sql.contains("dh_dataset_acl"));
            assertTrue(sql.contains("f.folder_id = ?"));
        }
    }
}
