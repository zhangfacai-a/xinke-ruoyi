package com.xinke.erp.domain.audience;

import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;

public class AudienceRankBatchQuery
{
    private String roomName;
    private String roomMatchStatus;
    private Boolean needsAttention;
    private Boolean currentOnly;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date beginCapturedAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date endCapturedAt;

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getRoomMatchStatus() { return roomMatchStatus; }
    public void setRoomMatchStatus(String roomMatchStatus) { this.roomMatchStatus = roomMatchStatus; }
    public Boolean getNeedsAttention() { return needsAttention; }
    public void setNeedsAttention(Boolean value) { needsAttention = value; }
    public Boolean getCurrentOnly() { return currentOnly; }
    public void setCurrentOnly(Boolean value) { currentOnly = value; }
    public Date getBeginCapturedAt() { return beginCapturedAt; }
    public void setBeginCapturedAt(Date beginCapturedAt) { this.beginCapturedAt = beginCapturedAt; }
    public Date getEndCapturedAt() { return endCapturedAt; }
    public void setEndCapturedAt(Date endCapturedAt) { this.endCapturedAt = endCapturedAt; }
}
