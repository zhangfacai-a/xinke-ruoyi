package com.xinke.erp.domain.audience;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

public class AudienceRankQuery
{
    private Long batchId;
    private String roomName;
    private String keyword;
    private String nickname;
    private String secUid;
    private String rankType;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDataDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDataDate;

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getSecUid() { return secUid; }
    public void setSecUid(String secUid) { this.secUid = secUid; }
    public String getRankType() { return rankType; }
    public void setRankType(String rankType) { this.rankType = rankType; }
    public LocalDate getBeginDataDate() { return beginDataDate; }
    public void setBeginDataDate(LocalDate beginDataDate) { this.beginDataDate = beginDataDate; }
    public LocalDate getEndDataDate() { return endDataDate; }
    public void setEndDataDate(LocalDate endDataDate) { this.endDataDate = endDataDate; }
}
