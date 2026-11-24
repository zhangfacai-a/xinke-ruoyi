package com.xinke.erp.livecrm.mapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class LiveCrmMapperXmlTest {
    @Test
    void registersOwnershipAndAttachmentStatements() {
        String resource = "mapper/erp/LiveCrmMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        } catch (Exception exception) {
            throw new AssertionError("MyBatis could not load LiveCrmMapper.xml", exception);
        }
        String namespace = LiveCrmMapper.class.getName() + ".";
        assertTrue(configuration.hasStatement(namespace + "lockCustomerForUpdate"));
        assertTrue(configuration.hasStatement(namespace + "claimCustomerIfUnassigned"));
        assertTrue(configuration.hasStatement(namespace + "synchronizeActiveOpportunityOwners"));
        assertTrue(configuration.hasStatement(namespace + "listReadyAttachmentsForCustomers"));
        assertTrue(configuration.hasStatement(namespace + "searchCustomerSuggestions"));
        assertTrue(configuration.hasStatement(namespace + "findOrderByOrderNo"));

        String claimSql = configuration.getMappedStatement(namespace + "claimCustomerIfUnassigned")
                .getBoundSql(Map.of("customerId", 1L, "ownerUserId", 2L, "ownerName", "负责人"))
                .getSql();
        assertTrue(claimSql.contains("owner_user_id is null"));
        assertTrue(claimSql.contains("not exists"));
        String attachmentsSql = configuration.getMappedStatement(namespace + "listReadyAttachmentsForCustomers")
                .getBoundSql(Map.of("customerIds", List.of(1L, 2L))).getSql();
        assertTrue(attachmentsSql.contains("status='READY'"));
        assertTrue(attachmentsSql.contains("customer_id in"));
        String suggestionsSql = configuration.getMappedStatement(namespace + "searchCustomerSuggestions")
                .getBoundSql(Map.of("keyword", "测试", "limit", 8)).getSql();
        assertTrue(suggestionsSql.contains("limit ?"));
        assertTrue(suggestionsSql.contains("c.display_name"));
        assertTrue(!suggestionsSql.contains("dy_audience_v2_interaction_event"));
        String orderSql = configuration.getMappedStatement(namespace + "findOrderByOrderNo")
                .getBoundSql(Map.of("orderNo", "6918963760272256717")).getSql();
        assertTrue(orderSql.contains("where order_no=?"));
        assertTrue(orderSql.contains("customer_id customerId"));
    }
}
