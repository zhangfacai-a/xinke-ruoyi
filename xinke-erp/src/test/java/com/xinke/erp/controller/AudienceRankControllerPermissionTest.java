package com.xinke.erp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AudienceRankControllerPermissionTest
{
    @Test
    void followupHistoryRequiresDedicatedPermission() throws Exception
    {
        Method method = AudienceRankController.class.getDeclaredMethod("followupLogs", Long.class);
        PreAuthorize permission = method.getAnnotation(PreAuthorize.class);

        assertEquals("@ss.hasPermi('live:audienceRank:followup:history')", permission.value());

        Method visits = AudienceRankController.class.getDeclaredMethod("followupVisits", Long.class);
        PreAuthorize visitPermission = visits.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermi('live:audienceRank:followup:history')", visitPermission.value());

        Method dashboard = AudienceRankController.class.getDeclaredMethod("followupDashboard", com.xinke.erp.domain.audience.AudienceFollowupQuery.class);
        PreAuthorize dashboardPermission = dashboard.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermi('live:audienceRank:followup:assign')", dashboardPermission.value());
    }
}
