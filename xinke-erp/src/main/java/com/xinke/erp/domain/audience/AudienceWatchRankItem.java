package com.xinke.erp.domain.audience;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public class AudienceWatchRankItem extends AudienceRankUserItem
{
    @NotNull(message = "观看时长不能为空")
    @PositiveOrZero(message = "观看时长不能小于0")
    private Long watchSeconds;

    public Long getWatchSeconds() { return watchSeconds; }
    public void setWatchSeconds(Long watchSeconds) { this.watchSeconds = watchSeconds; }
}
