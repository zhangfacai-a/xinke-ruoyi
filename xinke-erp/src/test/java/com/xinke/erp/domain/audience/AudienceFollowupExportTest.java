package com.xinke.erp.domain.audience;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xinke.common.annotation.Excel;
import org.junit.jupiter.api.Test;

class AudienceFollowupExportTest
{
    @Test
    void exportsBusinessStatusNameAndFormattedReminder() throws Exception
    {
        AudienceFollowup source = new AudienceFollowup();
        source.setStatus("ORDER_PENDING");

        AudienceFollowupExport row = AudienceFollowupExport.from(source);

        assertEquals("待下单", row.getStatus());
        Excel annotation = AudienceFollowupExport.class.getDeclaredField("nextFollowAt").getAnnotation(Excel.class);
        assertEquals("yyyy-MM-dd HH:mm:ss", annotation.dateFormat());
    }
}
