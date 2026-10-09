package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.LiveMetricsMapper;

@ExtendWith(MockitoExtension.class)
class LiveMetricsServiceTest
{
    @Mock
    private LiveMetricsMapper mapper;

    private LiveMetricsService service;

    @BeforeEach
    void setUp()
    {
        service = new LiveMetricsService(mapper);
        org.mockito.Mockito.lenient().when(mapper.countActiveUser(anyLong())).thenReturn(1);
        org.mockito.Mockito.lenient().when(mapper.countRoomAnchor(anyLong(), anyLong())).thenReturn(1);
        org.mockito.Mockito.lenient().when(mapper.countOverlappingShifts(any())).thenReturn(0);
        org.mockito.Mockito.lenient().when(mapper.countOverlappingAnchorShifts(any())).thenReturn(0);
    }

    @Test
    void allowsFourShiftsForOneAnchorInOneDay()
    {
        when(mapper.countAnchorShiftsOnDate(any())).thenReturn(3);

        service.saveShift(shift("2026-09-29 18:00:00", "2026-09-29 21:00:00"), "admin");

        verify(mapper).insertShift(any());
    }

    @Test
    void rejectsFifthShiftForOneAnchorInOneDay()
    {
        when(mapper.countAnchorShiftsOnDate(any())).thenReturn(4);

        ServiceException error = assertThrows(ServiceException.class,
                () -> service.saveShift(shift("2026-09-29 21:00:00", "2026-09-30 00:00:00"), "admin"));

        assertEquals("该主播当天已有4个班次，不能继续添加", error.getMessage());
    }

    @Test
    void batchSchedulingCreatesEverySelectedShift()
    {
        when(mapper.countAnchorShiftsOnDate(any())).thenReturn(0);
        Map<String, Object> request = new HashMap<>();
        request.put("roomId", 3L);
        request.put("items", List.of(
                item("2026-09-29 09:00:00", "2026-09-29 12:00:00"),
                item("2026-09-29 14:00:00", "2026-09-29 17:00:00"),
                item("2026-09-30 09:00:00", "2026-09-30 12:00:00")));

        Map<String, Object> result = service.saveWeek(request, "admin");

        assertEquals(3, result.get("created"));
        assertEquals(0, result.get("skipped"));
    }

    @Test
    void staffVisibilityIsSavedPerViewerAndNeverHidesTheViewer()
    {
        service.saveStaffVisibility(8L, Map.of("hiddenUserIds", List.of(8L, 9L, 9L)));

        verify(mapper).deleteHiddenStaff(8L);
        verify(mapper).insertHiddenStaff(8L, List.of(9L));
    }

    @Test
    void deletesShiftWithoutReportingHistory()
    {
        when(mapper.getShift(9L)).thenReturn(Map.of("shiftId", 9L));
        when(mapper.countShiftSubmissions(9L)).thenReturn(0);
        when(mapper.deleteShift(9L)).thenReturn(1);

        service.deleteShift(9L);

        verify(mapper).deleteShift(9L);
    }

    @Test
    void rejectsDeletingShiftWithReportingHistory()
    {
        when(mapper.getShift(9L)).thenReturn(Map.of("shiftId", 9L));
        when(mapper.countShiftSubmissions(9L)).thenReturn(1);

        ServiceException error = assertThrows(ServiceException.class, () -> service.deleteShift(9L));

        assertEquals("该班次已有上报记录，不能删除；如排班有误，请修改班次或重新匹配上报记录", error.getMessage());
    }

    private Map<String, Object> shift(String start, String end)
    {
        Map<String, Object> value = item(start, end);
        value.put("roomId", 3L);
        return value;
    }

    private Map<String, Object> item(String start, String end)
    {
        Map<String, Object> value = new HashMap<>();
        value.put("anchorUserId", 8L);
        value.put("startTime", start);
        value.put("endTime", end);
        return value;
    }
}
