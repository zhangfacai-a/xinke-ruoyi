package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.xinke.common.annotation.Excel;

public class LiveGiftCostImportRow
{
    @Excel(name="礼品编码") private String giftCode;
    @Excel(name="礼品名称") private String giftName;
    @Excel(name="单位成本") private BigDecimal unitCost;
    @Excel(name="生效日期",dateFormat="yyyy-MM-dd")
    @JsonFormat(pattern="yyyy-MM-dd") private Date effectiveDate;
    @Excel(name="备注") private String remark;
    public String getGiftCode(){return giftCode;} public void setGiftCode(String v){giftCode=v;}
    public String getGiftName(){return giftName;} public void setGiftName(String v){giftName=v;}
    public BigDecimal getUnitCost(){return unitCost;} public void setUnitCost(BigDecimal v){unitCost=v;}
    public Date getEffectiveDate(){return effectiveDate;} public void setEffectiveDate(Date v){effectiveDate=v;}
    public String getRemark(){return remark;} public void setRemark(String v){remark=v;}
}
