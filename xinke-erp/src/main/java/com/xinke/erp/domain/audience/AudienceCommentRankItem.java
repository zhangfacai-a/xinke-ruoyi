package com.xinke.erp.domain.audience;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class AudienceCommentRankItem extends AudienceRankUserItem
{
    @NotNull(message = "评论次数不能为空")
    @PositiveOrZero(message = "评论次数不能小于0")
    private Long commentCount;

    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }
}
