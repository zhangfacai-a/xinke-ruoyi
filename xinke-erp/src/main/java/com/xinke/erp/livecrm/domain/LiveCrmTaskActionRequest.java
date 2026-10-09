package com.xinke.erp.livecrm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LiveCrmTaskActionRequest {
    @NotBlank private String action;
    private String nextTaskDate;
    @Size(max=1000) private String result;
    public String getAction(){return action;} public void setAction(String value){action=value;}
    public String getNextTaskDate(){return nextTaskDate;} public void setNextTaskDate(String value){nextTaskDate=value;}
    public String getResult(){return result;} public void setResult(String value){result=value;}
}
