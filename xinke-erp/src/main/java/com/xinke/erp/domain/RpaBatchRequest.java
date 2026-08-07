package com.xinke.erp.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RpaBatchRequest
{
    @NotBlank(message = "batchNo不能为空")
    @Size(max = 64, message = "batchNo长度不能超过64")
    private String batchNo;

    @NotBlank(message = "leaseToken不能为空")
    @Size(max = 64, message = "leaseToken长度不能超过64")
    private String leaseToken;

    @NotBlank(message = "workerId不能为空")
    @Size(max = 128, message = "workerId长度不能超过128")
    private String workerId;

    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getLeaseToken() { return leaseToken; }
    public void setLeaseToken(String leaseToken) { this.leaseToken = leaseToken; }
    public String getWorkerId() { return workerId; }
    public void setWorkerId(String workerId) { this.workerId = workerId; }
}
