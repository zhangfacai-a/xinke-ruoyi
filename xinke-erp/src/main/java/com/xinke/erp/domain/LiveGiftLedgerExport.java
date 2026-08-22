package com.xinke.erp.domain;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import com.xinke.common.annotation.Excel;

public class LiveGiftLedgerExport
{
    @Excel(name="订单号") private String orderNo;
    @Excel(name="录入日期") private String entryDate;
    @Excel(name="礼品") private String giftText;
    @Excel(name="礼品件数") private Integer giftQuantity;
    @Excel(name="礼品成本") private BigDecimal giftCost;
    @Excel(name="主播") private String anchorName;
    @Excel(name="场控") private String controllerName;
    @Excel(name="到返金额") private BigDecimal refundAmount;
    @Excel(name="到返理由") private String refundReason;
    @Excel(name="售后补偿") private String afterSaleCompensation;
    @Excel(name="服务标记") private String serviceMark;
    @Excel(name="是否延保") private String extendedWarranty;
    @Excel(name="是否价保") private String priceProtection;
    @Excel(name="是否延迟") private String delayed;
    @Excel(name="是否追单") private String followUp;
    @Excel(name="是否加急") private String urgent;
    @Excel(name="处理状态") private String processStatus;
    @Excel(name="录入人") private String createBy;

    public static LiveGiftLedgerExport from(Map<String,Object> row)
    {
        LiveGiftLedgerExport value=new LiveGiftLedgerExport();
        value.orderNo=text(row.get("orderNo"));value.entryDate=text(row.get("entryDate"));
        value.giftText=text(row.get("giftText"));
        value.giftQuantity=number(row.get("giftQuantity"));
        value.giftCost=row.get("giftCost")==null?BigDecimal.ZERO:new BigDecimal(row.get("giftCost").toString());
        value.anchorName=text(row.get("anchorName")); value.controllerName=text(row.get("controllerName"));
        value.refundAmount=row.get("refundAmount")==null?null:new BigDecimal(row.get("refundAmount").toString());
        value.refundReason=text(row.get("refundReason")); value.afterSaleCompensation=text(row.get("afterSaleCompensation")); value.serviceMark=text(row.get("serviceMark"));
        value.extendedWarranty=boolText(row.get("extendedWarranty")); value.priceProtection=boolText(row.get("priceProtection")); value.delayed=boolText(row.get("delayed")); value.followUp=boolText(row.get("followUp")); value.urgent=boolText(row.get("urgent"));
        value.processStatus=text(row.get("processStatus"));value.createBy=text(row.get("createBy"));
        return value;
    }
    private static String text(Object value){return Objects.toString(value,"");}
    private static Integer number(Object value){return value==null?0:Integer.valueOf(value.toString());}
    private static String boolText(Object value){String text=Objects.toString(value,"");return ("1".equals(text)||"true".equalsIgnoreCase(text)||"是".equals(text))?"是":"否";}
    public String getOrderNo(){return orderNo;} public String getEntryDate(){return entryDate;}
    public String getGiftText(){return giftText;} public Integer getGiftQuantity(){return giftQuantity;} public BigDecimal getGiftCost(){return giftCost;}
    public String getAnchorName(){return anchorName;} public String getControllerName(){return controllerName;} public BigDecimal getRefundAmount(){return refundAmount;}
    public String getRefundReason(){return refundReason;} public String getAfterSaleCompensation(){return afterSaleCompensation;} public String getServiceMark(){return serviceMark;}
    public String getExtendedWarranty(){return extendedWarranty;} public String getPriceProtection(){return priceProtection;} public String getDelayed(){return delayed;} public String getFollowUp(){return followUp;} public String getUrgent(){return urgent;}
    public String getProcessStatus(){return processStatus;} public String getCreateBy(){return createBy;}
}
