package com.xinke.erp.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JdCustomerServiceTest {
    @Test
    void splitsTwoOrdersAndKeepsStringIdentifiers() {
        assertEquals(List.of("03667282009921545","3683449010438293"),
            JdCustomerService.orders("03667282009921545，3683449010438293,03667282009921545"));
        assertEquals(List.of(),JdCustomerService.orders(""));
    }

    @Test
    void readsQuotedCsvAndKeepsRowsWithoutOrders() {
        String csv="\uFEFF时间,客服昵称,客户昵称,订单号\n"
            +"2026-11-17 08:43:37,龚西征,jd_6ce46bad264c9,\n"
            +"2026-11-17 12:44:14,龚西征,jd_wFbJhiNILFJa,\"3683449000530278,3683449010438293\"\n";
        var rows=JdCustomerService.csv(csv.getBytes(StandardCharsets.UTF_8));
        assertEquals(2,rows.size());
        assertEquals("",rows.get(0).rawOrders());
        assertEquals(2,JdCustomerService.orders(rows.get(1).rawOrders()).size());
    }
}
