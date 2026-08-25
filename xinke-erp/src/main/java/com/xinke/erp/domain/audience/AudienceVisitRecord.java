package com.xinke.erp.domain.audience;

import java.time.LocalDate;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** One effective day on which a customer appeared in either audience ranking. */
public class AudienceVisitRecord
{
    private LocalDate visitDate;
    private Long roomId;
    private String roomName;
    private Long batchId;
    private Long commentCount;
    private Integer commentRank;
    private Long watchSeconds;
    private Integer watchRank;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date capturedAt;

    public LocalDate getVisitDate() { return visitDate; }
    public void setVisitDate(LocalDate value) { visitDate = value; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long value) { roomId = value; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String value) { roomName = value; }
    public Long getBatchId() { return batchId; }
    public void setBatchId(Long value) { batchId = value; }
    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long value) { commentCount = value; }
    public Integer getCommentRank() { return commentRank; }
    public void setCommentRank(Integer value) { commentRank = value; }
    public Long getWatchSeconds() { return watchSeconds; }
    public void setWatchSeconds(Long value) { watchSeconds = value; }
    public Integer getWatchRank() { return watchRank; }
    public void setWatchRank(Integer value) { watchRank = value; }
    public Date getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Date value) { capturedAt = value; }
}
