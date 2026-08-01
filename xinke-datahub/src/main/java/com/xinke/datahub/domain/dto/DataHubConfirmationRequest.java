package com.xinke.datahub.domain.dto;

public class DataHubConfirmationRequest
{
    private Long baseVersionId;
    private String confirmationName;

    public Long getBaseVersionId() { return baseVersionId; }
    public void setBaseVersionId(Long baseVersionId) { this.baseVersionId = baseVersionId; }
    public String getConfirmationName() { return confirmationName; }
    public void setConfirmationName(String confirmationName) { this.confirmationName = confirmationName; }
}
