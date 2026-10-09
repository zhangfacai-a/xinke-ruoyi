package com.xinke.erp.livecrm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LiveCrmAttachmentPresignRequest {
    @NotNull private Long customerId;
    private Long opportunityId;
    private Long orderId;
    @NotBlank @Size(max = 255) private String originalName;
    @NotBlank @Size(max = 128) private String contentType;
    @NotNull private Long fileSize;
    @Size(max = 64) private String sha256;

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long value) { customerId = value; }
    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long value) { opportunityId = value; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { orderId = value; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String value) { originalName = value; }
    public String getContentType() { return contentType; }
    public void setContentType(String value) { contentType = value; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long value) { fileSize = value; }
    public String getSha256() { return sha256; }
    public void setSha256(String value) { sha256 = value; }
}
