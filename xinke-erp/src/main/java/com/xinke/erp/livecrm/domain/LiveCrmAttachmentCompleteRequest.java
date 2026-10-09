package com.xinke.erp.livecrm.domain;

import jakarta.validation.constraints.Size;

public class LiveCrmAttachmentCompleteRequest {
    @Size(max = 64) private String sha256;
    public String getSha256() { return sha256; }
    public void setSha256(String value) { sha256 = value; }
}
