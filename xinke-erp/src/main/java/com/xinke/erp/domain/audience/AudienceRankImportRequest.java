package com.xinke.erp.domain.audience;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AudienceRankImportRequest
{
    @JsonAlias("accountName")
    @NotBlank(message = "直播间名称不能为空")
    @Size(max = 128, message = "直播间名称长度不能超过128")
    private String roomName;

    @NotBlank(message = "评论榜数据日期不能为空")
    @Pattern(regexp = "^\\d{4}([.-])\\d{2}\\1\\d{2}$", message = "评论榜数据日期格式必须为yyyy.MM.dd或yyyy-MM-dd")
    private String commentDataDate;

    @NotBlank(message = "观看榜数据日期不能为空")
    @Pattern(regexp = "^\\d{4}([.-])\\d{2}\\1\\d{2}$", message = "观看榜数据日期格式必须为yyyy.MM.dd或yyyy-MM-dd")
    private String watchDataDate;

    @Positive(message = "采集时间必须是有效的毫秒时间戳")
    private Long capturedAt;

    @Valid
    @NotNull(message = "评论榜数据不能为空")
    @Size(max = 500, message = "评论榜单次不能超过500人")
    private List<AudienceCommentRankItem> commentRanks;

    @Valid
    @NotNull(message = "观看榜数据不能为空")
    @Size(max = 500, message = "观看榜单次不能超过500人")
    private List<AudienceWatchRankItem> watchRanks;

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }
    public String getCommentDataDate() { return commentDataDate; }
    public void setCommentDataDate(String commentDataDate) { this.commentDataDate = commentDataDate; }
    public String getWatchDataDate() { return watchDataDate; }
    public void setWatchDataDate(String watchDataDate) { this.watchDataDate = watchDataDate; }
    public Long getCapturedAt() { return capturedAt; }
    public void setCapturedAt(Long capturedAt) { this.capturedAt = capturedAt; }
    public List<AudienceCommentRankItem> getCommentRanks() { return commentRanks; }
    public void setCommentRanks(List<AudienceCommentRankItem> commentRanks) { this.commentRanks = commentRanks; }
    public List<AudienceWatchRankItem> getWatchRanks() { return watchRanks; }
    public void setWatchRanks(List<AudienceWatchRankItem> watchRanks) { this.watchRanks = watchRanks; }
}
