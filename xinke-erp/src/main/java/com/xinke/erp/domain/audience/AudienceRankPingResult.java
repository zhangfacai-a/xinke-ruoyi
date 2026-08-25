package com.xinke.erp.domain.audience;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class AudienceRankPingResult
{
    private final boolean ready;
    private final int maxRowsPerRanking;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final Date serverTime;

    public AudienceRankPingResult(boolean ready, int maxRowsPerRanking, Date serverTime)
    {
        this.ready = ready;
        this.maxRowsPerRanking = maxRowsPerRanking;
        this.serverTime = serverTime;
    }

    public boolean isReady() { return ready; }
    public int getMaxRowsPerRanking() { return maxRowsPerRanking; }
    public Date getServerTime() { return serverTime; }
}
