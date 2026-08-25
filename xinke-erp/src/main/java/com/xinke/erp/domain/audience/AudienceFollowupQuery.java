package com.xinke.erp.domain.audience;

import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

public class AudienceFollowupQuery
{
    private Long roomId;
    private String roomName;
    private Long anchorUserId;
    private Long controllerUserId;
    private Long ownerUserId;
    private String status;
    private String stage;
    private String intentLevel;
    private String followResultCode;
    private String keyword;
    private Boolean overdue;
    private Boolean todayDue;
    private Boolean onlyMine;
    private Boolean mineAssigned;
    private Boolean priority;
    private Boolean hasOrder;
    private Boolean repeatVisit;
    private Boolean uncontacted;
    private Boolean claimed;
    private Boolean contacted;
    private Boolean isFollower;
    private Boolean isFollowing;
    private Integer minPayLevel;
    private Integer maxCommentRank;
    private Integer maxWatchRank;
    private Boolean excludeTerminal;
    private Boolean reactivationPending;
    private Boolean qualified;
    private Long currentUserId;
    private Long sourceBatchId;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginDate;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    public Long getRoomId() { return roomId; }
    public void setRoomId(Long value) { roomId = value; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String value) { roomName = value; }
    public Long getAnchorUserId() { return anchorUserId; }
    public void setAnchorUserId(Long value) { anchorUserId = value; }
    public Long getControllerUserId() { return controllerUserId; }
    public void setControllerUserId(Long value) { controllerUserId = value; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long value) { ownerUserId = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getStage() { return stage; }
    public void setStage(String value) { stage = value; }
    public String getIntentLevel() { return intentLevel; }
    public void setIntentLevel(String value) { intentLevel = value; }
    public String getFollowResultCode() { return followResultCode; }
    public void setFollowResultCode(String value) { followResultCode = value; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String value) { keyword = value; }
    public Boolean getOverdue() { return overdue; }
    public void setOverdue(Boolean value) { overdue = value; }
    public Boolean getTodayDue() { return todayDue; }
    public void setTodayDue(Boolean value) { todayDue = value; }
    public Boolean getOnlyMine() { return onlyMine; }
    public void setOnlyMine(Boolean value) { onlyMine = value; }
    public Boolean getMineAssigned() { return mineAssigned; }
    public void setMineAssigned(Boolean value) { mineAssigned = value; }
    public Boolean getPriority() { return priority; }
    public void setPriority(Boolean value) { priority = value; }
    public Boolean getHasOrder() { return hasOrder; }
    public void setHasOrder(Boolean value) { hasOrder = value; }
    public Boolean getRepeatVisit() { return repeatVisit; }
    public void setRepeatVisit(Boolean value) { repeatVisit = value; }
    public Boolean getUncontacted() { return uncontacted; }
    public void setUncontacted(Boolean value) { uncontacted = value; }
    public Boolean getClaimed() { return claimed; }
    public void setClaimed(Boolean value) { claimed = value; }
    public Boolean getContacted() { return contacted; }
    public void setContacted(Boolean value) { contacted = value; }
    public Boolean getIsFollower() { return isFollower; }
    public void setIsFollower(Boolean value) { isFollower = value; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean value) { isFollowing = value; }
    public Integer getMinPayLevel() { return minPayLevel; }
    public void setMinPayLevel(Integer value) { minPayLevel = value; }
    public Integer getMaxCommentRank() { return maxCommentRank; }
    public void setMaxCommentRank(Integer value) { maxCommentRank = value; }
    public Integer getMaxWatchRank() { return maxWatchRank; }
    public void setMaxWatchRank(Integer value) { maxWatchRank = value; }
    public Boolean getExcludeTerminal() { return excludeTerminal; }
    public void setExcludeTerminal(Boolean value) { excludeTerminal = value; }
    public Boolean getReactivationPending() { return reactivationPending; }
    public void setReactivationPending(Boolean value) { reactivationPending = value; }
    public Boolean getQualified() { return qualified; }
    public void setQualified(Boolean value) { qualified = value; }
    public Long getCurrentUserId() { return currentUserId; }
    public void setCurrentUserId(Long value) { currentUserId = value; }
    public Long getSourceBatchId() { return sourceBatchId; }
    public void setSourceBatchId(Long value) { sourceBatchId = value; }
    public LocalDate getBeginDate() { return beginDate; }
    public void setBeginDate(LocalDate value) { beginDate = value; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate value) { endDate = value; }
}
