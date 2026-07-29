package com.xinke.datahub.naming;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;

class DataHubIdentifierServiceTest
{
    @Test
    void rejectsUnsafeIdentifier()
    {
        DataHubIdentifierService service = new DataHubIdentifierService(new DataHubProperties());
        assertThrows(ServiceException.class, () -> service.quote("orders`; drop table sys_user; --"));
        assertThrows(ServiceException.class, () -> service.quote("UPPER_CASE"));
    }

    @Test
    void generatesBoundedVersionTableName()
    {
        DataHubIdentifierService service = new DataHubIdentifierService(new DataHubProperties());
        String table = service.versionTable("very_long_customer_order_table_name_for_uploaded_spreadsheet",
                123456789L, 2, 1);
        assertTrue(table.length() <= 64);
        assertTrue(table.matches("[a-z][a-z0-9_]*"));
        assertTrue(table.endsWith("_v000001"));
    }
}
