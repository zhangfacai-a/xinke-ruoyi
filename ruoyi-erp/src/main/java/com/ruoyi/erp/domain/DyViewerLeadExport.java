package com.ruoyi.erp.domain;

import com.ruoyi.common.annotation.Excel;

public class DyViewerLeadExport
{
    private static final long serialVersionUID = 1L;

    @Excel(name = "日期", width = 14)
    private String leadDate;

    @Excel(name = "直播间", width = 24)
    private String liveRoomName;

    @Excel(name = "客户昵称", width = 24)
    private String nickname;

    @Excel(name = "抖音主页", width = 45)
    private String profileUrl;

    @Excel(name = "评论数量", width = 12)
    private Integer commentCount;

    @Excel(name = "评论明细", width = 60, wrapText = true)
    private String comments;

    @Excel(name = "意向", width = 12)
    private String intent;

    @Excel(name = "状态", width = 12)
    private String status;

    @Excel(name = "负责人", width = 16)
    private String ownerName;

    @Excel(name = "订单号", width = 24)
    private String orderNo;

    @Excel(name = "备注", width = 30, wrapText = true)
    private String remark;

    @Excel(name = "更新时间", width = 22)
    private String updateTime;

    public String getLeadDate() { return leadDate; }
    public void setLeadDate(String leadDate) { this.leadDate = leadDate; }
    public String getLiveRoomName() { return liveRoomName; }
    public void setLiveRoomName(String liveRoomName) { this.liveRoomName = liveRoomName; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getProfileUrl() { return profileUrl; }
    public void setProfileUrl(String profileUrl) { this.profileUrl = profileUrl; }
    public Integer getCommentCount() { return commentCount; }
    public void setCommentCount(Integer commentCount) { this.commentCount = commentCount; }
    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getUpdateTime() { return updateTime; }
    public void setUpdateTime(String updateTime) { this.updateTime = updateTime; }
}
