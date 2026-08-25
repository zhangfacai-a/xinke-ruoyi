package com.xinke.erp.domain.audience;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class AudienceRankSummary
{
    private Long batchCount;
    private Long roomCount;
    private Long uniqueUserCount;
    private Long commentUserCount;
    private Long watchUserCount;
    private Long bothRankUserCount;
    private Long totalCommentCount;
    private Long totalWatchSeconds;
    private BigDecimal averageWatchSeconds;
    private Long followerCount;
    private Long unmatchedBatchCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date latestCapturedAt;

    public Long getBatchCount() { return batchCount; }
    public void setBatchCount(Long batchCount) { this.batchCount = batchCount; }
    public Long getRoomCount() { return roomCount; }
    public void setRoomCount(Long roomCount) { this.roomCount = roomCount; }
    public Long getUniqueUserCount() { return uniqueUserCount; }
    public void setUniqueUserCount(Long uniqueUserCount) { this.uniqueUserCount = uniqueUserCount; }
    public Long getCommentUserCount() { return commentUserCount; }
    public void setCommentUserCount(Long commentUserCount) { this.commentUserCount = commentUserCount; }
    public Long getWatchUserCount() { return watchUserCount; }
    public void setWatchUserCount(Long watchUserCount) { this.watchUserCount = watchUserCount; }
    public Long getBothRankUserCount() { return bothRankUserCount; }
    public void setBothRankUserCount(Long bothRankUserCount) { this.bothRankUserCount = bothRankUserCount; }
    public Long getTotalCommentCount() { return totalCommentCount; }
    public void setTotalCommentCount(Long totalCommentCount) { this.totalCommentCount = totalCommentCount; }
    public Long getTotalWatchSeconds() { return totalWatchSeconds; }
    public void setTotalWatchSeconds(Long totalWatchSeconds) { this.totalWatchSeconds = totalWatchSeconds; }
    public BigDecimal getAverageWatchSeconds() { return averageWatchSeconds; }
    public void setAverageWatchSeconds(BigDecimal averageWatchSeconds) { this.averageWatchSeconds = averageWatchSeconds; }
    public Long getFollowerCount() { return followerCount; }
    public void setFollowerCount(Long followerCount) { this.followerCount = followerCount; }
    public Long getUnmatchedBatchCount() { return unmatchedBatchCount; }
    public void setUnmatchedBatchCount(Long unmatchedBatchCount) { this.unmatchedBatchCount = unmatchedBatchCount; }
    public Date getLatestCapturedAt() { return latestCapturedAt; }
    public void setLatestCapturedAt(Date latestCapturedAt) { this.latestCapturedAt = latestCapturedAt; }
}
