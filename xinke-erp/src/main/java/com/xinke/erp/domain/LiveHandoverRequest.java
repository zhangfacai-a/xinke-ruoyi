package com.xinke.erp.domain;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class LiveHandoverRequest
{
    @NotBlank @Pattern(regexp="REPORT") private String action;
    @NotBlank @Size(max=160) private String roomName;
    @NotBlank @Size(max=64) @Pattern(regexp="^\\d{6,64}$") private String platformSessionId;
    @NotBlank @Size(max=30) private String liveStartTime;
    @NotBlank @Size(max=100) private String idempotencyKey;
    @Positive private Long anchorUserId;
    @Positive private Long previousSnapshotId;
    private Long capturedAt;
    @NotNull @PositiveOrZero private Long watchTotal;
    @NotNull @PositiveOrZero private Long interactionTotal;
    @NotNull @PositiveOrZero private Long newFanTotal;
    @NotNull @PositiveOrZero private Long dealTotal;
    @NotNull @DecimalMin("0") private BigDecimal gmvTotal;
    @NotNull @DecimalMin("0") private BigDecimal adCostTotal;
    @NotNull @PositiveOrZero private Long presaleOrderTotal;
    @NotNull @DecimalMin("0") private BigDecimal exposureWatchRate;
    @NotNull @DecimalMin("0") private BigDecimal watchFollowRate;
    @NotNull @DecimalMin("0") private BigDecimal watchInteractionRate;
    @NotNull @DecimalMin("0") private BigDecimal watchDealRate;

    public String getAction(){return action;} public void setAction(String v){action=v;}
    public String getRoomName(){return roomName;} public void setRoomName(String v){roomName=v;}
    public String getPlatformSessionId(){return platformSessionId;} public void setPlatformSessionId(String v){platformSessionId=v;}
    public String getLiveStartTime(){return liveStartTime;} public void setLiveStartTime(String v){liveStartTime=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    public Long getAnchorUserId(){return anchorUserId;} public void setAnchorUserId(Long v){anchorUserId=v;}
    public Long getPreviousSnapshotId(){return previousSnapshotId;} public void setPreviousSnapshotId(Long v){previousSnapshotId=v;}
    public Long getCapturedAt(){return capturedAt;} public void setCapturedAt(Long v){capturedAt=v;}
    public Long getWatchTotal(){return watchTotal;} public void setWatchTotal(Long v){watchTotal=v;}
    public Long getInteractionTotal(){return interactionTotal;} public void setInteractionTotal(Long v){interactionTotal=v;}
    public Long getNewFanTotal(){return newFanTotal;} public void setNewFanTotal(Long v){newFanTotal=v;}
    public Long getDealTotal(){return dealTotal;} public void setDealTotal(Long v){dealTotal=v;}
    public BigDecimal getGmvTotal(){return gmvTotal;} public void setGmvTotal(BigDecimal v){gmvTotal=v;}
    public BigDecimal getAdCostTotal(){return adCostTotal;} public void setAdCostTotal(BigDecimal v){adCostTotal=v;}
    public Long getPresaleOrderTotal(){return presaleOrderTotal;} public void setPresaleOrderTotal(Long v){presaleOrderTotal=v;}
    public BigDecimal getExposureWatchRate(){return exposureWatchRate;} public void setExposureWatchRate(BigDecimal v){exposureWatchRate=v;}
    public BigDecimal getWatchFollowRate(){return watchFollowRate;} public void setWatchFollowRate(BigDecimal v){watchFollowRate=v;}
    public BigDecimal getWatchInteractionRate(){return watchInteractionRate;} public void setWatchInteractionRate(BigDecimal v){watchInteractionRate=v;}
    public BigDecimal getWatchDealRate(){return watchDealRate;} public void setWatchDealRate(BigDecimal v){watchDealRate=v;}
}
