package com.xinke.erp.domain;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RpaMessageTemplateRequest
{
    @Size(max = 64) private String templateKey;
    @NotBlank @Size(max = 100) private String templateName;
    @NotBlank @Size(max = 32) private String scene;
    @NotBlank @Size(max = 1000) private String content;
    private Boolean enabled;
    private Boolean defaultTemplate;
    private Integer priority;
    private List<String> keywords;

    public String getTemplateKey() { return templateKey; }
    public void setTemplateKey(String templateKey) { this.templateKey = templateKey; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getDefaultTemplate() { return defaultTemplate; }
    public void setDefaultTemplate(Boolean defaultTemplate) { this.defaultTemplate = defaultTemplate; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }
}
