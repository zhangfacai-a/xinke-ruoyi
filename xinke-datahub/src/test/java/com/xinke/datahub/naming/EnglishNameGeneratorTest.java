package com.xinke.datahub.naming;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnglishNameGeneratorTest
{
    private final EnglishNameGenerator generator = new EnglishNameGenerator();

    @Test
    void translatesKnownBusinessName()
    {
        GeneratedName result = generator.generate("客户订单", "table", 1, Map.of());
        assertEquals("customer_order", result.getIdentifier());
        assertEquals("DICTIONARY", result.getSource());
        assertFalse(result.isNeedsReview());
    }

    @Test
    void combinesKnownChineseTokens()
    {
        GeneratedName result = generator.generate("客户金额", "column", 1, Map.of());
        assertEquals("customer_amount", result.getIdentifier());
        assertFalse(result.isNeedsReview());
    }

    @Test
    void marksUnknownChineseForReview()
    {
        GeneratedName result = generator.generate("未知含义", "column", 7, Map.of());
        assertEquals("column_007", result.getIdentifier());
        assertTrue(result.isNeedsReview());
    }

    @Test
    void avoidsMysqlReservedWord()
    {
        GeneratedName result = generator.generate("order", "column", 1, Map.of());
        assertEquals("field_order", result.getIdentifier());
    }

    @Test
    void normalizesNameKeyWithUnicodeAndWhitespace()
    {
        assertEquals("客户订单", generator.normalizeNameKey("  客户 订单  "));
    }
}
