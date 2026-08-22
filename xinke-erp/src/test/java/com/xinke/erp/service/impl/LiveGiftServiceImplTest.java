package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.LiveGiftBatchSaveRequest;
import com.xinke.erp.domain.LiveGiftSaveRequest;
import com.xinke.erp.mapper.LiveGiftMapper;

@ExtendWith(MockitoExtension.class)
class LiveGiftServiceImplTest
{
    @Mock private LiveGiftMapper mapper;
    @InjectMocks private LiveGiftServiceImpl service;

    @Test
    void savesInitialCostWithNewGift()
    {
        Map<String, Object> gift = new java.util.HashMap<>();
        gift.put("giftCode", "G-NEW"); gift.put("giftName", "新礼品");
        gift.put("unitCost", new BigDecimal("12.50")); gift.put("effectiveDate", "2026-08-20");
        org.mockito.Mockito.doAnswer(invocation -> {
            ((Map<String, Object>) invocation.getArgument(0)).put("giftId", 18L);
            return 1;
        }).when(mapper).insertGift(any());

        service.saveGift(gift, "tester");

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertGiftCost(captor.capture());
        assertEquals(18L, captor.getValue().get("giftId"));
        assertEquals(new BigDecimal("12.50"), captor.getValue().get("unitCost"));
        assertEquals("2026-08-20", captor.getValue().get("effectiveDate"));
    }

    @Test
    void rejectsNewGiftWhenNameAlreadyExists()
    {
        Map<String, Object> gift = new java.util.HashMap<>();
        gift.put("giftCode", "G-NEW"); gift.put("giftName", "已有礼品");
        when(mapper.selectGiftByName("已有礼品", null)).thenReturn(Map.of("giftId", 7L));

        assertThrows(ServiceException.class, () -> service.saveGift(gift, "tester"));
        verify(mapper, never()).insertGift(any());
    }

    @Test
    void allowsExistingGiftToKeepItsNameAndCode()
    {
        Map<String, Object> gift = new java.util.HashMap<>();
        gift.put("giftId", 7L); gift.put("giftCode", "G-007"); gift.put("giftName", "原礼品");
        when(mapper.selectGiftById(7L)).thenReturn(Map.of("giftId", 7L, "giftCode", "G-007", "giftName", "原礼品"));

        service.saveGift(gift, "tester");

        verify(mapper).updateGift(gift);
        verify(mapper, never()).selectGiftByName(anyString(), any());
    }

    @Test
    void rejectsGiftCodeChangeDuringEdit()
    {
        Map<String, Object> gift = new java.util.HashMap<>();
        gift.put("giftId", 7L); gift.put("giftCode", "G-CHANGED"); gift.put("giftName", "原礼品");
        when(mapper.selectGiftById(7L)).thenReturn(Map.of("giftId", 7L, "giftCode", "G-007", "giftName", "原礼品"));

        assertThrows(ServiceException.class, () -> service.saveGift(gift, "tester"));
        verify(mapper, never()).updateGift(any());
    }

    @Test
    void findsHistoricalGiftByChinesePinyinInitials()
    {
        when(mapper.selectGifts(any())).thenReturn(List.of(
            Map.of("giftId", 1L, "giftCode", "G-1", "giftName", "美的汤锅28cm"),
            Map.of("giftId", 2L, "giftCode", "G-2", "giftName", "循环扇")));

        List<Map<String, Object>> result = service.listGifts(Map.of("keyword", "mdtg"));

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("giftId"));
    }

    @Test
    void savesHistoricalCostSnapshotForOrderDate()
    {
        when(mapper.selectOrderByNo("ORDER-1")).thenReturn(Map.of("orderDate", Date.valueOf("2026-04-15")));
        when(mapper.selectGiftById(8L)).thenReturn(Map.of("giftId",8L,"giftName","循环扇","unit","件","status","0"));
        when(mapper.selectApplicableCost(8L, LocalDate.of(2026,4,15)))
            .thenReturn(Map.of("unitCost",new BigDecimal("48.00"),"effectiveDate",Date.valueOf("2026-04-01")));
        LiveGiftSaveRequest request=request("ORDER-1",8L,2);

        service.saveOrderGift(request,"tester");

        ArgumentCaptor<Map<String,Object>> captor=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertOrderGift(captor.capture());
        assertEquals(new BigDecimal("96.00"),captor.getValue().get("totalCost"));
        assertEquals(new BigDecimal("48.00"),captor.getValue().get("unitCost"));
    }

    @Test
    void rejectsGiftWithoutCostOnOrderDate()
    {
        when(mapper.selectOrderByNo("ORDER-2")).thenReturn(Map.of("orderDate",Date.valueOf("2025-01-01")));
        when(mapper.selectGiftById(9L)).thenReturn(Map.of("giftId",9L,"giftName","夏凉被","unit","件","status","0"));
        when(mapper.selectApplicableCost(9L,LocalDate.of(2025,1,1))).thenReturn(null);

        assertThrows(ServiceException.class,()->service.saveOrderGift(request("ORDER-2",9L,1),"tester"));
        verify(mapper,never()).upsertOrderGiftStatus(any());
        verify(mapper,never()).deleteOrderGiftItems(anyString());
    }

    @Test
    void batchReturnsPerOrderFailures()
    {
        when(mapper.selectOrderByNo("OK-1")).thenReturn(Map.of("orderDate",Date.valueOf("2026-05-01")));
        when(mapper.selectOrderByNo("MISSING")).thenReturn(null);
        when(mapper.selectGiftById(1L)).thenReturn(Map.of("giftId",1L,"giftName","冰格","unit","件","status","0"));
        when(mapper.selectApplicableCost(1L,LocalDate.of(2026,5,1)))
            .thenReturn(Map.of("unitCost",BigDecimal.ONE,"effectiveDate",Date.valueOf("2025-12-01")));
        when(mapper.selectApplicableCost(1L,LocalDate.now()))
            .thenReturn(Map.of("unitCost",BigDecimal.ONE,"effectiveDate",Date.valueOf("2025-12-01")));
        LiveGiftBatchSaveRequest request=new LiveGiftBatchSaveRequest();
        request.setOrderNos(List.of("OK-1","MISSING"));
        request.setGifts(List.of(Map.of("giftId",1L,"quantity",1)));

        Map<String,Object> result=service.batchSaveOrderGifts(request,"tester");

        assertEquals(2,result.get("success"));
        assertEquals(0,result.get("failure"));
    }

    @Test
    void createsGiftRecordBeforePlatformOrderIsSynchronized()
    {
        when(mapper.selectOrderByNo("NEW-ORDER")).thenReturn(null);
        when(mapper.selectGiftById(3L)).thenReturn(Map.of("giftId",3L,"giftName","保温杯","unit","件","status","0"));
        when(mapper.selectApplicableCost(3L,LocalDate.now()))
            .thenReturn(Map.of("unitCost",new BigDecimal("9.90"),"effectiveDate",Date.valueOf("2026-08-01")));

        service.saveOrderGift(request("NEW-ORDER",3L,1),"tester");

        verify(mapper).upsertOrderGiftStatus(any());
        verify(mapper).insertOrderGiftLog(
            org.mockito.ArgumentMatchers.eq("NEW-ORDER"),
            org.mockito.ArgumentMatchers.eq("CREATE"),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.eq("tester"));
    }

    @Test
    void savesStructuredOrderServiceFieldsWithoutGift()
    {
        LiveGiftSaveRequest request = new LiveGiftSaveRequest();
        request.setOrderNo("SERVICE-ONLY"); request.setProcessStatus("not_applicable");
        request.setGifts(List.of()); request.setAnchorUserId(11L); request.setAnchorNameSnapshot("主播甲");
        request.setControllerUserId(12L); request.setControllerNameSnapshot("场控乙");
        request.setRefundAmount(new BigDecimal("25.50")); request.setRefundReason("活动补差");
        request.setOtherRemark("备注"); request.setUrgent(true); request.setExtendedWarranty(true);
        request.setParsedText("主播：主播甲 + 是否加急：是");

        service.saveOrderGift(request, "tester");

        ArgumentCaptor<Map<String,Object>> captor=ArgumentCaptor.forClass(Map.class);
        verify(mapper).upsertOrderGiftStatus(captor.capture());
        assertEquals(11L,captor.getValue().get("anchorUserId"));
        assertEquals(new BigDecimal("25.50"),captor.getValue().get("refundAmount"));
        assertEquals(true,captor.getValue().get("urgent"));
        assertEquals("主播：主播甲 + 是否加急：是",captor.getValue().get("parsedText"));
    }

    private LiveGiftSaveRequest request(String orderNo,Long giftId,int quantity)
    {
        LiveGiftSaveRequest request=new LiveGiftSaveRequest();
        request.setOrderNo(orderNo);request.setProcessStatus("selected");
        request.setGifts(List.of(Map.of("giftId",giftId,"quantity",quantity)));
        return request;
    }
}
