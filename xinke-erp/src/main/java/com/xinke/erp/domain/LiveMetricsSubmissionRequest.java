package com.xinke.erp.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class LiveMetricsSubmissionRequest
{
    @NotBlank @Size(max=100) private String roomName;
    @NotBlank @Size(max=30) private String liveStartTime;
    @NotBlank @Size(max=100) private String idempotencyKey;
    private Long capturedAt;
    @NotNull @PositiveOrZero private Long exposureTotal;
    @NotNull @PositiveOrZero private Long watchTotal;
    @NotNull @PositiveOrZero private Long dealTotal;
    @NotNull @PositiveOrZero private Long newFanTotal;
    @NotNull @PositiveOrZero private Long interactionTotal;

    public String getRoomName(){return roomName;} public void setRoomName(String v){roomName=v;}
    public String getLiveStartTime(){return liveStartTime;} public void setLiveStartTime(String v){liveStartTime=v;}
    public String getIdempotencyKey(){return idempotencyKey;} public void setIdempotencyKey(String v){idempotencyKey=v;}
    public Long getCapturedAt(){return capturedAt;} public void setCapturedAt(Long v){capturedAt=v;}
    public Long getExposureTotal(){return exposureTotal;} public void setExposureTotal(Long v){exposureTotal=v;}
    public Long getWatchTotal(){return watchTotal;} public void setWatchTotal(Long v){watchTotal=v;}
    public Long getDealTotal(){return dealTotal;} public void setDealTotal(Long v){dealTotal=v;}
    public Long getNewFanTotal(){return newFanTotal;} public void setNewFanTotal(Long v){newFanTotal=v;}
    public Long getInteractionTotal(){return interactionTotal;} public void setInteractionTotal(Long v){interactionTotal=v;}
}
