package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.LiveHandoverRequest;
import com.xinke.erp.mapper.LiveMetricsMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness=Strictness.LENIENT)
class LiveHandoverServiceTest
{
    @Mock LiveMetricsMapper mapper;
    @Mock ApplicationEventPublisher events;
    LiveHandoverService service;
    Map<String,Object> session;

    @BeforeEach
    void setUp()
    {
        service=new LiveHandoverService(mapper,events);
        session=new LinkedHashMap<>(Map.of("sessionId",7L,"roomId",3L,"platformSessionId","7701099368803306283","liveStartTime","2026-10-30 08:00:00"));
        when(mapper.getHandoverSnapshotByKey("test-key")).thenReturn(null);
        when(mapper.findHandoverSessionByPlatformId("7701099368803306283")).thenReturn(session);
        when(mapper.lockHandoverSession(7L)).thenReturn(session);
        when(mapper.countRoomAnchor(3L,11L)).thenReturn(1);
        when(mapper.listRoomAnchors(3L)).thenReturn(List.of(Map.of("userId",11L,"userName","主播甲")));
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of());
    }

    @Test
    void firstReportUsesLiveStartZeroBaseline()
    {
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(null);
        AtomicLong ids=new AtomicLong(100);
        when(mapper.insertHandoverSnapshot(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("snapshotId",ids.getAndIncrement());return 1;});
        service.transition(request(null,200,20));
        verify(mapper,org.mockito.Mockito.times(2)).insertHandoverSnapshot(any());
        ArgumentCaptor<Map<String,Object>> segment=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(segment.capture());
        assertEquals("2026-10-30 08:00:00",segment.getValue().get("startTime"));
        assertEquals(200L,segment.getValue().get("watchDelta"));
        assertEquals(new BigDecimal("10.0000"),segment.getValue().get("exposureWatchRate"));
    }

    @Test
    void appReportRecordsAuthenticatedOperatorAndVersion()
    {
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(null);
        AtomicLong ids=new AtomicLong(100);
        when(mapper.insertHandoverSnapshot(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("snapshotId",ids.getAndIncrement());return 1;});
        service.transitionApp(request(null,200,20),42L,"operator-account","0.3.0");
        ArgumentCaptor<Map<String,Object>> segment=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(segment.capture());
        assertEquals("APP",segment.getValue().get("source"));
        assertEquals(42L,segment.getValue().get("operatorUserId"));
        assertEquals("operator-account",segment.getValue().get("createBy"));
        assertEquals("0.3.0",segment.getValue().get("clientVersion"));
        assertEquals(11L,segment.getValue().get("anchorUserId"));
    }

    @Test
    void appPreviewDoesNotPersistAndUsesSameDeltaAsSubmission()
    {
        Map<String,Object> previous=snapshot(90L,150,12);previous.put("capturedAt","2026-10-30 09:00:00");
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(previous);
        Map<String,Object> preview=service.previewApp(request(90L,200,20));
        assertEquals(50L,preview.get("watchDelta"));assertEquals(8L,preview.get("newFanDelta"));
        assertEquals("2026-10-30 09:00:00",preview.get("startTime"));
        verify(mapper,never()).insertHandoverSnapshot(any());verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void appRejectsDecreasingCountersAndOldBoundary()
    {
        Map<String,Object> previous=snapshot(90L,300,12);previous.put("capturedAt","2026-10-30 09:00:00");
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(previous);
        assertThrows(ServiceException.class,()->service.previewApp(request(90L,200,20)));
        previous.put("capturedAt","2099-01-01 00:00:00");
        assertThrows(ServiceException.class,()->service.previewApp(request(90L,400,20)));
    }

    @Test
    void appContinuesAfterLastManualReportInsteadOfCountingItTwice()
    {
        Map<String,Object> zero=snapshot(90L,0,0);zero.put("capturedAt","2026-10-30 08:00:00");zero.put("gmvTotal",BigDecimal.ZERO);zero.put("adCostTotal",BigDecimal.ZERO);zero.put("dealTotal",0L);zero.put("interactionTotal",0L);
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(zero);
        Map<String,Object> manual=existingSegment(12L,"2026-10-30 08:00:00","2026-10-30 09:00:00");manual.put("watchDelta",100L);manual.put("interactionDelta",5L);manual.put("newFanDelta",10L);manual.put("dealDelta",1L);manual.put("gmvDelta",BigDecimal.valueOf(500));manual.put("adCostDelta",BigDecimal.valueOf(50));manual.put("presaleOrderDelta",0L);manual.put("endSnapshotId",null);
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(manual));
        Map<String,Object> result=service.previewApp(request(90L,200,20));
        assertEquals("2026-10-30 09:00:00",result.get("startTime"));
        assertEquals(100L,result.get("watchDelta"));assertEquals(new BigDecimal("500"),result.get("gmvDelta"));assertEquals(true,result.get("baselineEstimated"));assertEquals(false,result.get("firstReport"));
        verify(mapper,never()).insertHandoverSnapshot(any());
    }

    @Test
    void duplicateAfterRoomLockDoesNotInsertSecondReport()
    {
        when(mapper.getHandoverSnapshotByKey("test-key")).thenReturn(null,Map.of("sessionId",7L));
        service.transitionApp(request(null,200,20),42L,"operator","0.3.0");
        verify(mapper,never()).insertHandoverSnapshot(any());verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void nextReportUsesPreviousSnapshotDelta()
    {
        Map<String,Object> previous=snapshot(90L,150,12);previous.put("capturedAt","2026-10-30 09:00:00");
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(previous);
        when(mapper.insertHandoverSnapshot(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("snapshotId",91L);return 1;});
        service.transition(request(90L,200,20));
        ArgumentCaptor<Map<String,Object>> segment=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(segment.capture());
        assertEquals(50L,segment.getValue().get("watchDelta"));
        assertEquals(8L,segment.getValue().get("newFanDelta"));
        assertEquals("2026-10-30 09:00:00",segment.getValue().get("startTime"));
    }

    @Test
    void stalePreviousSnapshotIsRejected()
    {
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(snapshot(91L,180,18));
        assertThrows(ServiceException.class,()->service.transition(request(90L,200,20)));
        verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void duplicateRequestDoesNotWriteAgain()
    {
        when(mapper.getHandoverSnapshotByKey("test-key")).thenReturn(Map.of("sessionId",7L));
        service.transition(request(null,100,10));
        verify(mapper,never()).insertHandoverSnapshot(any());
        verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void cumulativeRollbackIsStoredAsPlatformCorrection()
    {
        when(mapper.getLatestHandoverSnapshot(7L)).thenReturn(snapshot(90L,220,22));
        when(mapper.insertHandoverSnapshot(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("snapshotId",91L);return 1;});
        service.transition(request(90L,200,20));
        ArgumentCaptor<Map<String,Object>> segment=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(segment.capture());
        assertEquals(-20L,segment.getValue().get("watchDelta"));
        assertEquals(-2L,segment.getValue().get("newFanDelta"));
        assertEquals(BigDecimal.ZERO,segment.getValue().get("watchFollowRate"));
    }

    @Test
    void invalidHugeMoneyIsRejectedBeforeDatabaseWrite()
    {
        LiveHandoverRequest value=request(null,200,20);
        value.setGmvTotal(new BigDecimal("789678901234568000000000000000000"));
        assertThrows(ServiceException.class,()->service.transition(value));
        verify(mapper,never()).insertHandoverSnapshot(any());
    }

    @Test
    void firstAnchorForgotReportSplitsFromLiveStart()
    {
        Map<String,Object> old=existingSegment(55L,"2026-10-30 08:00:00","2026-10-30 10:00:00");
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(old));
        when(mapper.updateHandoverSegment(any())).thenReturn(1);
        when(mapper.insertHandoverSegment(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("segmentId",56L);return 1;});
        when(mapper.getHandoverSegment(56L)).thenReturn(Map.of("segmentId",56L));

        service.createSegment(7L,manualInput("2026-10-30 08:00:00","2026-10-30 09:00:00"),1L,"管理员");

        ArgumentCaptor<Map<String,Object>> adjusted=ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateHandoverSegment(adjusted.capture());
        assertEquals("2026-10-30 09:00:00",adjusted.getValue().get("startTime"));
        assertEquals(700L,adjusted.getValue().get("watchDelta"));
        verify(mapper,org.mockito.Mockito.times(2)).insertHandoverCorrection(any());
    }

    @Test
    void middleAnchorForgotReportKeepsPreviousAndRecalculatesFollowingSegment()
    {
        Map<String,Object> previous=existingSegment(54L,"2026-10-30 08:00:00","2026-10-30 09:15:00");
        Map<String,Object> old=new HashMap<>();
        old.put("segmentId",55L);old.put("sessionId",7L);old.put("roomId",3L);old.put("anchorUserId",12L);old.put("version",0);
        old.put("startTime","2026-10-30 09:15:00");old.put("endTime","2026-10-30 11:00:00");old.put("startSnapshotId",90L);old.put("endSnapshotId",91L);
        old.put("watchDelta",1000L);old.put("interactionDelta",100L);old.put("newFanDelta",100L);old.put("dealDelta",50L);old.put("presaleOrderDelta",10L);
        old.put("gmvDelta",new BigDecimal("5000"));old.put("adCostDelta",new BigDecimal("1000"));old.put("exposureWatchRate",new BigDecimal("5"));old.put("watchFollowRate",new BigDecimal("10"));old.put("watchInteractionRate",new BigDecimal("10"));old.put("watchDealRate",new BigDecimal("5"));
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(previous,old));
        when(mapper.updateHandoverSegment(any())).thenReturn(1);
        when(mapper.insertHandoverSegment(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("segmentId",56L);return 1;});
        when(mapper.getHandoverSegment(56L)).thenReturn(Map.of("segmentId",56L));

        assertEquals(56L,service.createSegment(7L,manualInput(),1L,"管理员").get("segmentId"));

        ArgumentCaptor<Map<String,Object>> adjusted=ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateHandoverSegment(adjusted.capture());
        assertEquals(55L,adjusted.getValue().get("segmentId"));
        assertEquals("2026-10-30 10:01:00",adjusted.getValue().get("startTime"));
        assertNull(adjusted.getValue().get("startSnapshotId"));
        assertEquals(700L,adjusted.getValue().get("watchDelta"));
        assertEquals(new BigDecimal("3311.5000"),adjusted.getValue().get("gmvDelta"));
        assertEquals("ADJUSTED",adjusted.getValue().get("source"));
        verify(mapper,org.mockito.Mockito.times(2)).insertHandoverCorrection(any());
    }

    @Test
    void lastAnchorForgotReportCanBeAddedAfterLatestSnapshot()
    {
        session.put("lastSnapshotAt","2026-10-30 09:00:00");
        Map<String,Object> old=existingSegment(55L,"2026-10-30 08:00:00","2026-10-30 09:00:00");
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(old));
        when(mapper.insertHandoverSegment(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("segmentId",56L);return 1;});
        when(mapper.getHandoverSegment(56L)).thenReturn(Map.of("segmentId",56L));

        service.createSegment(7L,manualInput("2026-10-30 09:00:00","2026-10-30 10:00:00"),1L,"管理员");

        ArgumentCaptor<Map<String,Object>> inserted=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(inserted.capture());
        assertEquals("2026-10-30 09:00:00",inserted.getValue().get("startTime"));
        assertEquals("2026-10-30 10:00:00",inserted.getValue().get("endTime"));
        verify(mapper,never()).updateHandoverSegment(any());
        verify(mapper).insertHandoverCorrection(any());
    }

    @Test
    void manualSegmentAcceptsCustomTimeAndMetrics()
    {
        when(mapper.getHandoverSession(7L)).thenReturn(session);
        when(mapper.countHandoverSegmentOverlap(7L,"2026-10-30 09:15:00","2026-10-30 10:01:00",null)).thenReturn(0);
        when(mapper.insertHandoverSegment(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("segmentId",56L);return 1;});
        when(mapper.getHandoverSegment(56L)).thenReturn(Map.of("segmentId",56L));

        Map<String,Object> input=manualInput("2026-10-30 08:00:00","2026-10-30 10:01:00");
        assertEquals(56L,service.createSegment(7L,input,1L,"管理员").get("segmentId"));

        ArgumentCaptor<Map<String,Object>> segment=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(segment.capture());
        Map<String,Object> saved=segment.getValue();
        assertEquals("MANUAL",saved.get("source"));
        assertEquals("MANUAL",saved.get("precisionStatus"));
        assertNull(saved.get("startSnapshotId"));
        assertNull(saved.get("endSnapshotId"));
        assertEquals("2026-10-30 08:00:00",saved.get("startTime"));
        assertEquals("2026-10-30 10:01:00",saved.get("endTime"));
        assertEquals(300L,saved.get("watchDelta"));
        assertEquals(25L,saved.get("interactionDelta"));
        assertEquals(new BigDecimal("1688.5000"),saved.get("gmvDelta"));
        assertEquals(new BigDecimal("8.2500"),saved.get("watchInteractionRate"));
        verify(mapper).insertHandoverCorrection(any());
    }

    @Test
    void disconnectedSupplementIsRejectedAndTwoMissingSegmentsMustBeAddedInOrder()
    {
        Map<String,Object> previous=existingSegment(55L,"2026-10-30 08:00:00","2026-10-30 09:00:00");
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(previous));

        ServiceException error=assertThrows(ServiceException.class,()->service.createSegment(7L,manualInput("2026-10-30 10:00:00","2026-10-30 11:00:00"),1L,"管理员"));

        assertEquals("补录的开始时间或结束时间至少要与已有记录无缝衔接；若连续漏报两场，请按时间先后分两次补录",error.getMessage());
        verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void supplementWithoutReasonUsesDefaultAuditDescription()
    {
        when(mapper.insertHandoverSegment(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("segmentId",56L);return 1;});
        when(mapper.getHandoverSegment(56L)).thenReturn(Map.of("segmentId",56L));
        Map<String,Object> input=manualInput("2026-10-30 08:00:00","2026-10-30 09:00:00");input.put("reason","");

        service.createSegment(7L,input,1L,"管理员");

        ArgumentCaptor<Map<String,Object>> inserted=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(inserted.capture());
        assertEquals("人工补录",inserted.getValue().get("remark"));
        ArgumentCaptor<Map<String,Object>> audit=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverCorrection(audit.capture());
        assertEquals("人工补录",audit.getValue().get("reason"));
    }

    @Test
    void minutePrecisionTimeSnapsToActualHandoverSeconds()
    {
        Map<String,Object> previous=existingSegment(54L,"2026-10-30 08:00:00","2026-10-30 09:15:37");
        Map<String,Object> following=existingSegment(55L,"2026-10-30 09:15:37","2026-10-30 11:00:00");
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(previous,following));
        when(mapper.updateHandoverSegment(any())).thenReturn(1);
        when(mapper.insertHandoverSegment(any())).thenAnswer(invocation->{invocation.<Map<String,Object>>getArgument(0).put("segmentId",56L);return 1;});
        when(mapper.getHandoverSegment(56L)).thenReturn(Map.of("segmentId",56L));

        service.createSegment(7L,manualInput("2026-10-30 09:15:00","2026-10-30 10:01:00"),1L,"管理员");

        ArgumentCaptor<Map<String,Object>> inserted=ArgumentCaptor.forClass(Map.class);
        verify(mapper).insertHandoverSegment(inserted.capture());
        assertEquals("2026-10-30 09:15:37",inserted.getValue().get("startTime"));
    }

    @Test
    void manualSegmentRejectsRateAboveOneHundred()
    {
        when(mapper.getHandoverSession(7L)).thenReturn(session);
        Map<String,Object> input=manualInput();
        input.put("watchDealRate",new BigDecimal("100.01"));
        assertThrows(ServiceException.class,()->service.createSegment(7L,input,1L,"管理员"));
        verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void manualSegmentRejectsOverlapThatCannotBeSplitReliably()
    {
        Map<String,Object> old=new HashMap<>();
        old.put("segmentId",55L);old.put("startTime","2026-10-30 09:00:00");old.put("endTime","2026-10-30 11:00:00");
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(old));
        assertThrows(ServiceException.class,()->service.createSegment(7L,manualInput(),1L,"管理员"));
        verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void pluginSegmentCanBeCorrectedToManualWithAudit()
    {
        Map<String,Object> old=new HashMap<>();
        old.put("segmentId",55L);old.put("sessionId",7L);old.put("roomId",3L);old.put("source","PLUGIN");old.put("version",0);
        when(mapper.getHandoverSegment(55L)).thenReturn(old,Map.of("segmentId",55L,"source","MANUAL"));
        when(mapper.getHandoverSession(7L)).thenReturn(session);
        when(mapper.countHandoverSegmentOverlap(7L,"2026-10-30 09:15:00","2026-10-30 10:01:00",55L)).thenReturn(0);
        when(mapper.updateHandoverSegment(any())).thenReturn(1);
        Map<String,Object> input=manualInput();input.put("version",0);

        service.updateSegment(55L,input,1L,"管理员");

        ArgumentCaptor<Map<String,Object>> segment=ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateHandoverSegment(segment.capture());
        assertEquals("MANUAL",segment.getValue().get("source"));
        assertNull(segment.getValue().get("startSnapshotId"));
        assertNull(segment.getValue().get("endSnapshotId"));
        verify(mapper).insertHandoverCorrection(any());
    }

    @Test
    void automaticSegmentRejectsOverlappingTimeRange()
    {
        Map<String,Object> start=snapshot(90L,100,10);start.put("capturedAt","2026-10-30 09:00:00");
        Map<String,Object> end=snapshot(91L,200,20);end.put("capturedAt","2026-10-30 10:00:00");
        when(mapper.getHandoverSession(7L)).thenReturn(session);
        when(mapper.getHandoverSnapshot(90L)).thenReturn(start);
        when(mapper.getHandoverSnapshot(91L)).thenReturn(end);
        when(mapper.countHandoverSegmentOverlap(7L,"2026-10-30 09:00:00","2026-10-30 10:00:00",null)).thenReturn(1);
        Map<String,Object> input=Map.of("dataMode","AUTO","anchorUserId",11L,"startSnapshotId",90L,"endSnapshotId",91L,"reason","补录");
        assertThrows(ServiceException.class,()->service.createSegment(7L,input,1L,"管理员"));
        verify(mapper,never()).insertHandoverSegment(any());
    }

    @Test
    void voidMiddleReportRecalculatesFollowingSegmentFromPreviousBoundary()
    {
        Map<String,Object> old=existingSegment(55L,"2026-10-30 09:00:00","2026-10-30 10:00:00");old.put("status","CLOSED");
        Map<String,Object> next=existingSegment(56L,"2026-10-30 10:00:00","2026-10-30 11:00:00");next.put("status","CLOSED");next.put("startSnapshotId",91L);next.put("endSnapshotId",92L);next.put("anchorName","主播乙");
        Map<String,Object> start=snapshot(90L,100,10);start.put("capturedAt","2026-10-30 09:00:00");
        Map<String,Object> end=snapshot(92L,360,36);end.put("capturedAt","2026-10-30 11:00:00");
        when(mapper.getHandoverSegment(55L)).thenReturn(old);
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(old,next));
        when(mapper.getHandoverSnapshot(90L)).thenReturn(start);
        when(mapper.getHandoverSnapshot(92L)).thenReturn(end);
        when(mapper.voidHandoverSegment(any())).thenReturn(1);
        when(mapper.updateHandoverSegment(any())).thenReturn(1);

        Map<String,Object> result=service.voidSegment(55L,Map.of("version",0,"reason","误点上报"),1L,"管理员");

        ArgumentCaptor<Map<String,Object>> adjusted=ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateHandoverSegment(adjusted.capture());
        assertEquals(90L,adjusted.getValue().get("startSnapshotId"));
        assertEquals("2026-10-30 09:00:00",adjusted.getValue().get("startTime"));
        assertEquals(260L,adjusted.getValue().get("watchDelta"));
        assertEquals("AUTO",adjusted.getValue().get("source"));
        assertEquals(56L,result.get("recalculatedSegmentId"));
        verify(mapper,org.mockito.Mockito.times(2)).insertHandoverCorrection(any());
        verify(mapper,never()).resetHandoverSessionSnapshot(any(),any());
    }

    @Test
    void voidLastReportResetsEffectiveSnapshotForNextPluginReport()
    {
        Map<String,Object> old=existingSegment(55L,"2026-10-30 09:00:00","2026-10-30 10:00:00");old.put("status","CLOSED");
        Map<String,Object> start=snapshot(90L,100,10);start.put("capturedAt","2026-10-30 09:00:00");
        when(mapper.getHandoverSegment(55L)).thenReturn(old);
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(old));
        when(mapper.getHandoverSnapshot(90L)).thenReturn(start);
        when(mapper.voidHandoverSegment(any())).thenReturn(1);

        Map<String,Object> result=service.voidSegment(55L,Map.of("version",0,"reason","重复上报"),1L,"管理员");

        assertNull(result.get("recalculatedSegmentId"));
        verify(mapper).resetHandoverSessionSnapshot(7L,"2026-10-30 09:00:00");
        verify(mapper,never()).updateHandoverSegment(any());
        verify(mapper).insertHandoverCorrection(argThat(value->"VOID".equals(value.get("actionType"))));
    }

    @Test
    void roomDetailIncludesAllSessionDataForTimeline()
    {
        Map<String,Object> room=new LinkedHashMap<>(Map.of("roomId",3L,"roomName","测试直播间"));
        Map<String,Object> listedSession=new LinkedHashMap<>(Map.of("sessionId",7L,"liveStartTime","2026-10-30 08:00:00"));
        Map<String,Object> segment=Map.of("segmentId",55L,"anchorName","主播甲");
        Map<String,Object> snapshot=Map.of("snapshotId",90L,"capturedAt","2026-10-30 09:00:00");
        Map<String,Object> correction=Map.of("correctionId",1L,"reason","测试调整");
        when(mapper.getHandoverRoom(3L)).thenReturn(room);
        when(mapper.listStaffOptions()).thenReturn(List.of());
        when(mapper.listHandoverSessions(any())).thenReturn(List.of(listedSession));
        when(mapper.listHandoverSegments(7L)).thenReturn(List.of(segment));
        when(mapper.listHandoverSnapshots(7L)).thenReturn(List.of(snapshot));
        when(mapper.listHandoverCorrections(7L)).thenReturn(List.of(correction));

        Map<String,Object> result=service.roomDetail(3L,Map.of("beginDate","2026-10-30","endDate","2026-10-30"));
        Map<String,Object> timelineSession=(Map<String,Object>)((List<?>)result.get("sessions")).get(0);
        assertEquals(List.of(segment),timelineSession.get("segments"));
        assertEquals(List.of(snapshot),timelineSession.get("snapshots"));
        assertEquals(List.of(correction),timelineSession.get("corrections"));
    }

    private LiveHandoverRequest request(Long previous,long watch,long fans)
    {
        LiveHandoverRequest value=new LiveHandoverRequest();value.setAction("REPORT");value.setRoomName("测试直播间");value.setPlatformSessionId("7701099368803306283");value.setLiveStartTime("2026-10-30 08:00:00");value.setIdempotencyKey("test-key");value.setAnchorUserId(11L);value.setPreviousSnapshotId(previous);value.setCapturedAt(System.currentTimeMillis());value.setWatchTotal(watch);value.setInteractionTotal(10L);value.setNewFanTotal(fans);value.setDealTotal(2L);value.setGmvTotal(BigDecimal.valueOf(1000));value.setAdCostTotal(BigDecimal.valueOf(100));value.setPresaleOrderTotal(0L);value.setExposureWatchRate(BigDecimal.TEN);value.setWatchFollowRate(BigDecimal.TEN);value.setWatchInteractionRate(BigDecimal.TEN);value.setWatchDealRate(BigDecimal.ONE);return value;
    }

    private Map<String,Object> snapshot(long id,long watch,long fans)
    {
        Map<String,Object> value=new LinkedHashMap<>(Map.of("snapshotId",id,"sessionId",7L,"watchTotal",watch,"interactionTotal",5L,"newFanTotal",fans,"dealTotal",1L,"gmvTotal",BigDecimal.valueOf(500),"adCostTotal",BigDecimal.valueOf(50),"presaleOrderTotal",0L));
        value.put("exposureWatchRate",BigDecimal.TEN);
        return value;
    }

    private Map<String,Object> manualInput()
    {
        return manualInput("2026-10-30 09:15:00","2026-10-30 10:01:00");
    }

    private Map<String,Object> manualInput(String startTime,String endTime)
    {
        Map<String,Object> value=new HashMap<>();
        value.put("dataMode","MANUAL");value.put("anchorUserId",11L);value.put("startTime",startTime);value.put("endTime",endTime);value.put("reason","主播忘记上报，管理员补录");
        value.put("watchDelta",300L);value.put("newFanDelta",18L);value.put("dealDelta",7L);value.put("presaleOrderDelta",2L);
        value.put("gmvDelta",new BigDecimal("1688.50"));value.put("adCostDelta",new BigDecimal("126.30"));
        value.put("exposureWatchRate",new BigDecimal("4.12"));value.put("watchFollowRate",new BigDecimal("6.00"));value.put("watchInteractionRate",new BigDecimal("8.25"));value.put("watchDealRate",new BigDecimal("2.33"));
        return value;
    }

    private Map<String,Object> existingSegment(Long segmentId,String startTime,String endTime)
    {
        Map<String,Object> value=new HashMap<>();
        value.put("segmentId",segmentId);value.put("sessionId",7L);value.put("roomId",3L);value.put("anchorUserId",12L);value.put("version",0);
        value.put("startTime",startTime);value.put("endTime",endTime);value.put("startSnapshotId",90L);value.put("endSnapshotId",91L);
        value.put("watchDelta",1000L);value.put("interactionDelta",100L);value.put("newFanDelta",100L);value.put("dealDelta",50L);value.put("presaleOrderDelta",10L);
        value.put("gmvDelta",new BigDecimal("5000"));value.put("adCostDelta",new BigDecimal("1000"));value.put("exposureWatchRate",new BigDecimal("5"));value.put("watchFollowRate",new BigDecimal("10"));value.put("watchInteractionRate",new BigDecimal("10"));value.put("watchDealRate",new BigDecimal("5"));
        return value;
    }
}
