package com.xinke.erp.livecrm.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LiveCrmOwnerChangeRequest {
    @NotNull
    private Long ownerUserId;

    @Size(max = 500)
    private String reason;

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
