package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import com.xinke.erp.domain.LiveMetricsSubmissionRequest;
import com.xinke.erp.event.LiveMetricsSubmittedEvent;
import com.xinke.erp.mapper.LiveMetricsMapper;

@ExtendWith(MockitoExtension.class)
class LiveMetricsReportingServiceTest
{
    @Mock
    private LiveMetricsMapper mapper;
    @Mock
    private ApplicationEventPublisher events;

    private LiveMetricsReportingService service;

    @BeforeEach
    void setUp()
    {
        service = new LiveMetricsReportingService(mapper, events);
    }

    @Test
    void unknownRoomIsSavedForAdminClaiming()
    {
        LiveMetricsSubmissionRequest request = request("测试新名称", 100, 20);
        when(mapper.getSubmissionByKey("test-测试新名称")).thenReturn(null);
        when(mapper.findRoomByAliasKey("测试新名称")).thenReturn(null);
        when(mapper.findLiveSession(any(), any())).thenReturn(null);
        doAnswer(call -> {
            ((Map<String, Object>) call.getArgument(0)).put("sessionId", 11L);
            return 1;
        }).when(mapper).insertLiveSession(any());
        doAnswer(call -> {
            ((Map<String, Object>) call.getArgument(0)).put("submissionId", 21L);
            return 1;
        }).when(mapper).insertRawSubmission(any());
        when(mapper.listSessionSubmissions(11L)).thenReturn(List.of());
        when(mapper.listSessionShifts(11L)).thenReturn(List.of());

        Map<String, Object> result = service.submit(request);

        assertEquals(Boolean.FALSE, result.getOrDefault("roomMatched", Boolean.FALSE));
        assertEquals("PENDING_ROOM", result.get("matchStatus"));
    }

    @Test
    void renamedRoomReusesTheSameRealLiveSession()
    {
        LiveMetricsSubmissionRequest request = request("直播间新名称", 100, 20);
        Map<String, Object> room = Map.of("roomId", 5L, "roomName", "直播间原名称");
        Map<String, Object> session = Map.of("sessionId", 11L, "roomId", 5L,
                "reportedRoomName", "直播间原名称", "matchStatus", "MATCHED");
        when(mapper.getSubmissionByKey("test-直播间新名称")).thenReturn(null);
        when(mapper.findRoomByAliasKey("直播间新名称")).thenReturn(room);
        when(mapper.findLiveSessionByRoom(any(), any())).thenReturn(session);
        doAnswer(call -> {
            ((Map<String, Object>) call.getArgument(0)).put("submissionId", 21L);
            return 1;
        }).when(mapper).insertRawSubmission(any());
        when(mapper.listSessionSubmissions(11L)).thenReturn(List.of());
        when(mapper.listSessionShifts(11L)).thenReturn(List.of());

        Map<String, Object> result = service.submit(request);

        assertEquals(11L, result.get("sessionId"));
        verify(mapper, org.mockito.Mockito.never()).insertLiveSession(any());
    }

    @Test
    void duplicateUploadReturnsTheSameResponseShape()
    {
        LiveMetricsSubmissionRequest request = request("测试直播间", 100, 20);
        when(mapper.getSubmissionByKey("test-测试直播间")).thenReturn(Map.of(
                "submissionId", 21L,
                "sessionId", 11L,
                "matchStatus", "PENDING_SHIFT"));
        when(mapper.getLiveSession(11L)).thenReturn(Map.of(
                "sessionId", 11L,
                "roomId", 5L,
                "reportedRoomName", "测试直播间"));

        Map<String, Object> result = service.submit(request);

        assertEquals(21L, result.get("submissionId"));
        assertEquals(11L, result.get("sessionId"));
        assertEquals(Boolean.TRUE, result.get("roomMatched"));
        assertEquals(Boolean.FALSE, result.get("shiftMatched"));
        assertEquals("PENDING_SHIFT", result.get("matchStatus"));
        verify(mapper, org.mockito.Mockito.never()).insertRawSubmission(any());
    }

    @Test
    void matchedSubmissionPublishesNotificationWithCalculatedDelta()
    {
        LiveMetricsSubmissionRequest request = request("测试直播间", 100, 20);
        Map<String, Object> session = Map.of("sessionId", 11L, "roomId", 5L,
                "reportedRoomName", "测试直播间", "matchStatus", "MATCHED");
        Map<String, Object> shift = new HashMap<>();
        shift.put("shiftId", 1L);
        shift.put("roomId", 5L);
        shift.put("startTime", "2026-09-29 09:00:00");
        shift.put("anchorName", "张帅");
        shift.put("dingUserId", "ding-test");
        Map<String, Object> submitted = submission(21L, 1L,
                "2026-09-29 09:00:00", 100, 20);

        when(mapper.getSubmissionByKey("test-测试直播间")).thenReturn(null);
        when(mapper.findRoomByAliasKey("测试直播间")).thenReturn(Map.of("roomId", 5L));
        when(mapper.findLiveSessionByRoom(any(), any())).thenReturn(session);
        shift.put("currentShift", 1L);
        shift.put("hasSubmission", 0L);
        when(mapper.findAutoMatchShifts(any(), any(), any(), any())).thenReturn(List.of(shift));
        doAnswer(call -> {
            ((Map<String, Object>) call.getArgument(0)).put("submissionId", 21L);
            return 1;
        }).when(mapper).insertRawSubmission(any());
        when(mapper.listSessionSubmissions(11L)).thenReturn(List.of(submitted));
        when(mapper.listSessionShifts(11L)).thenReturn(List.of(shift));
        when(mapper.getShift(1L)).thenReturn(shift);
        when(mapper.countShiftSubmissions(1L)).thenReturn(1);

        service.submit(request);

        ArgumentCaptor<LiveMetricsSubmittedEvent> event = ArgumentCaptor.forClass(LiveMetricsSubmittedEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals(100L, event.getValue().getSubmission().get("exposureDelta"));
        assertEquals(20L, event.getValue().getSubmission().get("watchDelta"));
        assertEquals(1, event.getValue().getSubmission().get("revisionCount"));
    }

    @Test
    void missingEarlierShiftStillPublishesNotificationWithWarningStatus()
    {
        Map<String, Object> first = submission(103L, 3L, "2026-09-29 15:00:00", 250, 50);
        when(mapper.listSessionSubmissions(8L)).thenReturn(List.of(first));
        when(mapper.listSessionShifts(8L)).thenReturn(List.of(
                shift(2L, "2026-09-29 12:00:00"),
                shift(3L, "2026-09-29 15:00:00")));
        Map<String, Object> shift = shift(3L, "2026-09-29 15:00:00");
        shift.put("roomId", 5L);
        shift.put("anchorName", "张帅");
        when(mapper.getShift(3L)).thenReturn(shift);
        when(mapper.countShiftSubmissions(3L)).thenReturn(1);

        Map<String, Object> calculated = service.recalculate(8L, 103L);
        java.lang.reflect.Method method;
        try
        {
            method = LiveMetricsReportingService.class.getDeclaredMethod("publishSubmitted", Map.class);
            method.setAccessible(true);
            method.invoke(service, calculated);
        }
        catch (ReflectiveOperationException exception)
        {
            throw new AssertionError(exception);
        }

        ArgumentCaptor<LiveMetricsSubmittedEvent> event = ArgumentCaptor.forClass(LiveMetricsSubmittedEvent.class);
        verify(events).publishEvent(event.capture());
        assertEquals("SUSPECT_MISSING", event.getValue().getSubmission().get("matchStatus"));
    }

    @Test
    void activeShiftWinsEvenWhenAnEarlierShiftWasNotReported()
    {
        LiveMetricsSubmissionRequest request = request("测试直播间", 100, 20);
        Map<String, Object> session = Map.of("sessionId", 11L, "roomId", 5L,
                "reportedRoomName", "测试直播间", "matchStatus", "MATCHED");
        Map<String, Object> earlier = shift(1L, "2026-09-29 08:00:00");
        earlier.put("currentShift", 0L);
        earlier.put("hasSubmission", 0L);
        Map<String, Object> active = shift(2L, "2026-09-29 10:00:00");
        active.put("roomId", 5L);
        active.put("currentShift", 1L);
        active.put("hasSubmission", 0L);

        when(mapper.getSubmissionByKey("test-测试直播间")).thenReturn(null);
        when(mapper.findRoomByAliasKey("测试直播间")).thenReturn(Map.of("roomId", 5L));
        when(mapper.findLiveSessionByRoom(any(), any())).thenReturn(session);
        when(mapper.findAutoMatchShifts(any(), any(), any(), any())).thenReturn(List.of(active, earlier));
        when(mapper.getShift(2L)).thenReturn(active);
        doAnswer(call -> { ((Map<String, Object>) call.getArgument(0)).put("submissionId", 21L); return 1; })
                .when(mapper).insertRawSubmission(any());
        when(mapper.listSessionSubmissions(11L)).thenReturn(List.of());
        when(mapper.listSessionShifts(11L)).thenReturn(List.of());

        Map<String, Object> result = service.submit(request);

        assertEquals(Boolean.TRUE, result.get("shiftMatched"));
        assertEquals(2L, ((Map<?, ?>) result.get("shift")).get("shiftId"));
    }

    @Test
    void multipleUnreportedEndedShiftsRequireManualSelection()
    {
        LiveMetricsSubmissionRequest request = request("测试直播间", 100, 20);
        Map<String, Object> session = Map.of("sessionId", 11L, "roomId", 5L,
                "reportedRoomName", "测试直播间", "matchStatus", "MATCHED");
        Map<String, Object> first = shift(1L, "2026-09-29 08:00:00");
        first.put("currentShift", 0L); first.put("hasSubmission", 0L);
        Map<String, Object> second = shift(2L, "2026-09-29 10:00:00");
        second.put("currentShift", 0L); second.put("hasSubmission", 0L);

        when(mapper.getSubmissionByKey("test-测试直播间")).thenReturn(null);
        when(mapper.findRoomByAliasKey("测试直播间")).thenReturn(Map.of("roomId", 5L));
        when(mapper.findLiveSessionByRoom(any(), any())).thenReturn(session);
        when(mapper.findAutoMatchShifts(any(), any(), any(), any())).thenReturn(List.of(second, first));
        doAnswer(call -> { ((Map<String, Object>) call.getArgument(0)).put("submissionId", 21L); return 1; })
                .when(mapper).insertRawSubmission(any());
        when(mapper.listSessionSubmissions(11L)).thenReturn(List.of());
        when(mapper.listSessionShifts(11L)).thenReturn(List.of());

        Map<String, Object> result = service.submit(request);

        assertEquals(Boolean.FALSE, result.get("shiftMatched"));
        assertEquals("PENDING_SHIFT", result.get("matchStatus"));
    }

    @Test
    void rematchRecalculatesSessionAndRestoresThePreviousShiftStatus()
    {
        Map<String, Object> targetShift = shift(2L, "2026-09-29 12:00:00");
        targetShift.put("roomId", 5L);
        Map<String, Object> reassigned = submission(101L, 2L,
                "2026-09-29 12:00:00", 180, 35);

        when(mapper.getSubmission(101L)).thenReturn(Map.of(
                "submissionId", 101L,
                "sessionId", 8L,
                "shiftId", 1L));
        when(mapper.getShift(2L)).thenReturn(targetShift);
        when(mapper.getLiveSession(8L)).thenReturn(Map.of(
                "sessionId", 8L,
                "roomId", 5L,
                "reportedRoomName", "测试直播间"));
        when(mapper.listSessionSubmissions(8L)).thenReturn(List.of(reassigned));
        when(mapper.listSessionShifts(8L)).thenReturn(List.of(
                shift(1L, "2026-09-29 09:00:00"),
                targetShift));
        when(mapper.countShiftSubmissions(2L)).thenReturn(1);

        service.rematch(Map.of(
                "submissionId", 101L,
                "shiftId", 2L,
                "reason", "修正误匹配班次"), 9L, "admin");

        ArgumentCaptor<Map<String, Object>> update = ArgumentCaptor.forClass(Map.class);
        verify(mapper).updateSubmissionShift(update.capture());
        assertEquals(2L, update.getValue().get("shiftId"));
        assertEquals("修正误匹配班次", update.getValue().get("remark"));
        verify(mapper).resetSessionCalculation(8L);
        verify(mapper).refreshShiftStatus(1L);
        verify(mapper).insertCorrection(any());
    }

    @Test
    void recalculationUsesPreviousEffectiveCumulativeSnapshot()
    {
        Map<String, Object> first = submission(101L, 1L, "2026-09-29 09:00:00", 100, 20);
        Map<String, Object> second = submission(102L, 2L, "2026-09-29 12:00:00", 180, 35);
        when(mapper.listSessionSubmissions(8L)).thenReturn(List.of(first, second));
        when(mapper.listSessionShifts(8L)).thenReturn(List.of(
                shift(1L, "2026-09-29 09:00:00"),
                shift(2L, "2026-09-29 12:00:00")));

        service.recalculate(8L, 102L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateSubmissionCalculation(captor.capture());
        List<Map<String, Object>> calculations = captor.getAllValues();
        assertEquals(100L, calculations.get(0).get("exposureDelta"));
        assertEquals(80L, calculations.get(1).get("exposureDelta"));
        assertEquals(15L, calculations.get(1).get("watchDelta"));
        assertEquals(101L, calculations.get(1).get("baselineSubmissionId"));
    }

    @Test
    void missingMiddleShiftMakesLaterResultSuspicious()
    {
        Map<String, Object> first = submission(101L, 1L, "2026-09-29 09:00:00", 100, 20);
        Map<String, Object> third = submission(103L, 3L, "2026-09-29 15:00:00", 250, 50);
        when(mapper.listSessionSubmissions(8L)).thenReturn(List.of(first, third));
        when(mapper.listSessionShifts(8L)).thenReturn(List.of(
                shift(1L, "2026-09-29 09:00:00"),
                shift(2L, "2026-09-29 12:00:00"),
                shift(3L, "2026-09-29 15:00:00")));

        service.recalculate(8L, 103L);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateSubmissionCalculation(captor.capture());
        assertEquals("SUSPECT_MISSING", captor.getAllValues().get(1).get("matchStatus"));
    }

    private LiveMetricsSubmissionRequest request(String room, long exposure, long watch)
    {
        LiveMetricsSubmissionRequest request = new LiveMetricsSubmissionRequest();
        request.setRoomName(room);
        request.setLiveStartTime(LocalDateTime.now().minusHours(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        request.setIdempotencyKey("test-" + room);
        request.setExposureTotal(exposure);
        request.setWatchTotal(watch);
        request.setDealTotal(1L);
        request.setNewFanTotal(2L);
        request.setInteractionTotal(3L);
        return request;
    }

    private Map<String, Object> submission(long id, long shiftId, String start, long exposure, long watch)
    {
        Map<String, Object> value = new HashMap<>();
        value.put("submissionId", id);
        value.put("shiftId", shiftId);
        value.put("startTime", start);
        value.put("exposureTotal", exposure);
        value.put("watchTotal", watch);
        value.put("dealTotal", 0L);
        value.put("newFanTotal", 0L);
        value.put("interactionTotal", 0L);
        return value;
    }

    private Map<String, Object> shift(long id, String start)
    {
        Map<String, Object> value = new HashMap<>();
        value.put("shiftId", id);
        value.put("startTime", start);
        return value;
    }
}
