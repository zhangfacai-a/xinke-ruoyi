package com.xinke.datahub.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;
import com.xinke.datahub.domain.dto.DataHubColumnDefinition;
import com.xinke.datahub.domain.dto.DataHubCreateRequest;
import com.xinke.datahub.naming.DataHubIdentifierService;

class DataHubDefinitionValidatorTest
{
    private final DataHubDefinitionValidator validator;

    DataHubDefinitionValidatorTest()
    {
        DataHubProperties properties = new DataHubProperties();
        validator = new DataHubDefinitionValidator(new DataHubIdentifierService(properties), properties);
    }

    @Test
    void normalizesConfirmedDefinition()
    {
        DataHubColumnDefinition column = column(0, 64);
        column.setPhysicalName(" customer_name ");
        column.setDisplayName(" 客户名称 ");
        column.setDataType("varchar");
        column.setNullable(null);
        column.setSamples(List.of("甲", "乙", "丙", "丁"));

        DataHubCreateRequest request = request(List.of(column));
        validator.validate(request);

        assertEquals("customer_table", request.getPhysicalName());
        assertEquals("customer_name", column.getPhysicalName());
        assertEquals("客户名称", column.getDisplayName());
        assertEquals("VARCHAR", column.getDataType());
        assertTrue(column.getNullable());
        assertEquals(3, column.getSamples().size());
    }

    @Test
    void rejectsDefinitionsThatExceedMysqlRowCapacity()
    {
        List<DataHubColumnDefinition> columns = new ArrayList<>();
        for (int i = 0; i < 20; i++) columns.add(column(i, 1000));
        assertThrows(ServiceException.class, () -> validator.validate(request(columns)));
    }

    private DataHubCreateRequest request(List<DataHubColumnDefinition> columns)
    {
        DataHubCreateRequest request = new DataHubCreateRequest();
        request.setDisplayName(" 客户表 ");
        request.setPhysicalName(" customer_table ");
        request.setColumns(columns);
        return request;
    }

    private DataHubColumnDefinition column(int index, int length)
    {
        DataHubColumnDefinition column = new DataHubColumnDefinition();
        column.setSourceIndex(index);
        column.setSourceName("字段" + index);
        column.setDisplayName("字段" + index);
        column.setPhysicalName("column_" + index);
        column.setDataType("VARCHAR");
        column.setLength(length);
        return column;
    }
}
