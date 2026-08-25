package com.xinke.erp.domain.audience;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** An order belonging to a customer and, optionally, one opportunity. */
public class AudienceCustomerOrder
{
    private Long customerOrderId;
    private Long followupId;
    private Long opportunityId;
    private String orderNo;
    private String orderStatus;
    private String productModel;
    private String remark;
    private Integer version;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date orderedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    public Long getCustomerOrderId() { return customerOrderId; }
    public void setCustomerOrderId(Long value) { customerOrderId = value; }
    public Long getFollowupId() { return followupId; }
    public void setFollowupId(Long value) { followupId = value; }
    public Long getOpportunityId() { return opportunityId; }
    public void setOpportunityId(Long value) { opportunityId = value; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String value) { orderNo = value; }
    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String value) { orderStatus = value; }
    public String getProductModel() { return productModel; }
    public void setProductModel(String value) { productModel = value; }
    public String getRemark() { return remark; }
    public void setRemark(String value) { remark = value; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer value) { version = value; }
    public Date getOrderedAt() { return orderedAt; }
    public void setOrderedAt(Date value) { orderedAt = value; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date value) { createTime = value; }
    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date value) { updateTime = value; }
}
