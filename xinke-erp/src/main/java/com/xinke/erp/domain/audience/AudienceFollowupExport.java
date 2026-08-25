package com.xinke.erp.domain.audience;

import java.util.Date;
import com.xinke.common.annotation.Excel;

/** Flat export row for the customer follow-up workbench. */
public class AudienceFollowupExport
{
    @Excel(name = "观众昵称")
    private String nickname;
    @Excel(name = "sec_uid")
    private String secUid;
    @Excel(name = "直播间")
    private String roomName;
    @Excel(name = "评论次数")
    private Long commentCount;
    @Excel(name = "观看时长(秒)")
    private Long watchSeconds;
    @Excel(name = "是否关注")
    private String follower;
    @Excel(name = "领取人")
    private String owner;
    @Excel(name = "跟单主播")
    private String anchor;
    @Excel(name = "跟单场控")
    private String controller;
    @Excel(name = "咨询型号")
    private String consultModel;
    @Excel(name = "订单号")
    private String orderNo;
    @Excel(name = "跟进状态")
    private String status;
    @Excel(name = "下次跟进", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date nextFollowAt;
    @Excel(name = "最近跟进结果")
    private String lastFollowResult;
    @Excel(name = "备注")
    private String remark;

    public static AudienceFollowupExport from(AudienceFollowup row)
    {
        AudienceFollowupExport value = new AudienceFollowupExport();
        value.nickname = row.getNicknameSnapshot();
        value.secUid = row.getSecUid();
        value.roomName = row.getRoomNameSnapshot();
        value.commentCount = row.getCommentCount();
        value.watchSeconds = row.getWatchSeconds();
        value.follower = Boolean.TRUE.equals(row.getIsFollower()) ? "是" : "否";
        value.owner = row.getOwnerNameSnapshot();
        value.anchor = row.getAnchorNameSnapshot();
        value.controller = row.getControllerNameSnapshot();
        value.consultModel = row.getConsultModel();
        value.orderNo = row.getOrderNo();
        value.status = statusName(row.getStatus());
        value.nextFollowAt = row.getNextFollowAt();
        value.lastFollowResult = row.getLastFollowResult();
        value.remark = row.getRemark();
        return value;
    }

    private static String statusName(String status)
    {
        if (status == null) return "";
        return switch (status)
        {
            case "UNASSIGNED" -> "待分配";
            case "PENDING" -> "待联系";
            case "CONTACTED" -> "已联系";
            case "QUALIFIED" -> "有意向";
            case "QUOTED" -> "已报价";
            case "ORDER_PENDING" -> "待下单";
            case "ORDERED" -> "已下单";
            case "CLOSED" -> "已完成";
            case "PAUSED" -> "暂停跟进";
            case "INVALID" -> "无意向/无效";
            default -> status;
        };
    }

    public String getNickname() { return nickname; }
    public String getSecUid() { return secUid; }
    public String getRoomName() { return roomName; }
    public Long getCommentCount() { return commentCount; }
    public Long getWatchSeconds() { return watchSeconds; }
    public String getFollower() { return follower; }
    public String getOwner() { return owner; }
    public String getAnchor() { return anchor; }
    public String getController() { return controller; }
    public String getConsultModel() { return consultModel; }
    public String getOrderNo() { return orderNo; }
    public String getStatus() { return status; }
    public Date getNextFollowAt() { return nextFollowAt; }
    public String getLastFollowResult() { return lastFollowResult; }
    public String getRemark() { return remark; }
}
