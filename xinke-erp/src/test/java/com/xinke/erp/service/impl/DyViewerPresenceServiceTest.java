package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.erp.domain.DyCaptureReport;
import com.xinke.erp.domain.DyViewerPayload;
import com.xinke.erp.mapper.DyViewerLeadMapper;

@ExtendWith(MockitoExtension.class)
class DyViewerPresenceServiceTest
{
    @Mock
    private DyViewerLeadMapper viewerLeadMapper;

    @InjectMocks
    private DyViewerLeadServiceImpl service;

    @Test
    void onlineAudienceCreatesEstimatedStaySession()
    {
        when(viewerLeadMapper.selectViewerIdBySecUid("sec-test")).thenReturn(7L);
        when(viewerLeadMapper.selectLeadId(anyString(), anyString(), anyString())).thenReturn(8L);
        when(viewerLeadMapper.insertAudienceObservation(any())).thenReturn(1);
        when(viewerLeadMapper.selectLatestViewerStay(anyString(), anyString())).thenReturn(null);

        Map<String, Object> result = service.report(onlineAudienceReport());

        assertEquals(1, result.get("observationsInserted"));
        assertEquals(1, result.get("staySessionsTouched"));
        verify(viewerLeadMapper).upsertLiveSession(any());

        ArgumentCaptor<Map<String, Object>> stay = ArgumentCaptor.forClass(Map.class);
        verify(viewerLeadMapper).insertViewerStay(stay.capture());
        assertEquals("room-100", stay.getValue().get("liveSessionKey"));
        assertEquals(60, stay.getValue().get("estimatedStaySeconds"));
        assertEquals(60, stay.getValue().get("sampleIntervalSeconds"));
    }

    @Test
    void duplicateObservationDoesNotCreateAnotherStaySession()
    {
        when(viewerLeadMapper.selectViewerIdBySecUid("sec-test")).thenReturn(7L);
        when(viewerLeadMapper.selectLeadId(anyString(), anyString(), anyString())).thenReturn(8L);
        when(viewerLeadMapper.insertAudienceObservation(any())).thenReturn(0);

        Map<String, Object> result = service.report(onlineAudienceReport());

        assertEquals(0, result.get("observationsInserted"));
        verify(viewerLeadMapper, never()).insertViewerStay(any());
        verify(viewerLeadMapper, never()).updateViewerStay(any());
    }

    @Test
    void nextMinuteExtendsCurrentStaySession()
    {
        DyCaptureReport report = onlineAudienceReport();
        long observedAt = report.getAudiences().get(0).getCapturedAt();
        when(viewerLeadMapper.selectViewerIdBySecUid("sec-test")).thenReturn(7L);
        when(viewerLeadMapper.selectLeadId(anyString(), anyString(), anyString())).thenReturn(8L);
        when(viewerLeadMapper.insertAudienceObservation(any())).thenReturn(1);
        when(viewerLeadMapper.selectLatestViewerStay(anyString(), anyString())).thenReturn(Map.of(
                "stayId", 9L,
                "firstSeenAt", observedAt - 60_000L,
                "lastSeenAt", observedAt - 60_000L,
                "observationCount", 1));

        service.report(report);

        ArgumentCaptor<Map<String, Object>> stay = ArgumentCaptor.forClass(Map.class);
        verify(viewerLeadMapper).updateViewerStay(stay.capture());
        assertEquals(2, stay.getValue().get("observationCount"));
        assertEquals(120, stay.getValue().get("estimatedStaySeconds"));
    }

    private DyCaptureReport onlineAudienceReport()
    {
        DyViewerPayload audience = new DyViewerPayload();
        audience.setSecUid("sec-test");
        audience.setNickname("viewer");
        audience.setRoomId("room-100");
        audience.setCapturedAt(1_785_899_400_000L);
        audience.setSource("online_audiences");

        DyCaptureReport report = new DyCaptureReport();
        report.setRoomKey("anchor-1");
        report.setLiveSessionKey("room-100");
        report.setPayloadType("online_audiences");
        report.setClientId("client-1");
        report.setSampleIntervalSeconds(60);
        report.setAudiences(List.of(audience));
        return report;
    }
}
