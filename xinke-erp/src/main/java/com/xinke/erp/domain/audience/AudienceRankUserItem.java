package com.xinke.erp.domain.audience;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Fields shared by the comment and watch rankings.
 */
public abstract class AudienceRankUserItem
{
    @NotNull(message = "榜单名次不能为空")
    @Min(value = 1, message = "榜单名次必须大于0")
    @Max(value = 100000, message = "榜单名次不能超过100000")
    private Integer rank;

    @NotBlank(message = "secUid不能为空")
    @Size(max = 256, message = "secUid长度不能超过256")
    private String secUid;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 128, message = "昵称长度不能超过128")
    private String nickname;

    private Boolean isFollower;
    private Boolean isFollowing;

    @Min(value = 0, message = "消费等级不能小于0")
    @Max(value = 1000, message = "消费等级不能超过1000")
    private Integer payLevel;

    @Size(max = 1000, message = "等级图标地址长度不能超过1000")
    private String payIconUrl;

    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
    public String getSecUid() { return secUid; }
    public void setSecUid(String secUid) { this.secUid = secUid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public Boolean getIsFollower() { return isFollower; }
    public void setIsFollower(Boolean follower) { isFollower = follower; }
    public Boolean getIsFollowing() { return isFollowing; }
    public void setIsFollowing(Boolean following) { isFollowing = following; }
    public Integer getPayLevel() { return payLevel; }
    public void setPayLevel(Integer payLevel) { this.payLevel = payLevel; }
    public String getPayIconUrl() { return payIconUrl; }
    public void setPayIconUrl(String payIconUrl) { this.payIconUrl = payIconUrl; }
}
