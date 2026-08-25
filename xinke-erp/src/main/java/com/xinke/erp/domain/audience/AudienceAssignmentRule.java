package com.xinke.erp.domain.audience;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;

/** Room-level round-robin assignment rule for new audience customers. */
public class AudienceAssignmentRule
{
    private Long ruleId;
    private Long roomId;
    private String roomName;
    private Boolean enabled;
    private Integer maxActivePerOwner;
    private Integer reclaimHours;
    private Integer nextMemberIndex;
    private Boolean qualificationEnabled;
    private Integer commentRankThreshold;
    private Integer watchRankThreshold;
    private Integer minPayLevel;
    private Integer minVisitDays;
    private Boolean followerQualifies;
    private Boolean followingQualifies;
    private List<Long> memberUserIds;
    private List<Map<String, Object>> members;
    private String updateBy;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long value) { ruleId = value; }
    public Long getRoomId() { return roomId; }
    public void setRoomId(Long value) { roomId = value; }
    public String getRoomName() { return roomName; }
    public void setRoomName(String value) { roomName = value; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean value) { enabled = value; }
    public Integer getMaxActivePerOwner() { return maxActivePerOwner; }
    public void setMaxActivePerOwner(Integer value) { maxActivePerOwner = value; }
    public Integer getReclaimHours() { return reclaimHours; }
    public void setReclaimHours(Integer value) { reclaimHours = value; }
    public Integer getNextMemberIndex() { return nextMemberIndex; }
    public void setNextMemberIndex(Integer value) { nextMemberIndex = value; }
    public Boolean getQualificationEnabled() { return qualificationEnabled; }
    public void setQualificationEnabled(Boolean value) { qualificationEnabled = value; }
    public Integer getCommentRankThreshold() { return commentRankThreshold; }
    public void setCommentRankThreshold(Integer value) { commentRankThreshold = value; }
    public Integer getWatchRankThreshold() { return watchRankThreshold; }
    public void setWatchRankThreshold(Integer value) { watchRankThreshold = value; }
    public Integer getMinPayLevel() { return minPayLevel; }
    public void setMinPayLevel(Integer value) { minPayLevel = value; }
    public Integer getMinVisitDays() { return minVisitDays; }
    public void setMinVisitDays(Integer value) { minVisitDays = value; }
    public Boolean getFollowerQualifies() { return followerQualifies; }
    public void setFollowerQualifies(Boolean value) { followerQualifies = value; }
    public Boolean getFollowingQualifies() { return followingQualifies; }
    public void setFollowingQualifies(Boolean value) { followingQualifies = value; }
    public List<Long> getMemberUserIds() { return memberUserIds; }
    public void setMemberUserIds(List<Long> value) { memberUserIds = value; }
    public List<Map<String, Object>> getMembers() { return members; }
    public void setMembers(List<Map<String, Object>> value) { members = value; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String value) { updateBy = value; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date value) { updateTime = value; }
}
