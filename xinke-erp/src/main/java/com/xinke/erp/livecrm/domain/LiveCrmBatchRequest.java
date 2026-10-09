package com.xinke.erp.livecrm.domain;

import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LiveCrmBatchRequest {
    @NotBlank @Size(max=80) private String batchId;
    @NotNull private Long sessionId;
    @NotBlank @Size(max=80) private String installationId;
    @NotBlank @Size(max=30) private String pluginVersion;
    @Size(max=500) private List<Map<String,Object>> events;
    private Boolean encrypted;
    private String algorithm;
    private String encryptedKey;
    private String iv;
    @Size(max=2_000_000) private String ciphertext;
    public String getBatchId(){return batchId;} public void setBatchId(String v){batchId=v;}
    public Long getSessionId(){return sessionId;} public void setSessionId(Long v){sessionId=v;}
    public String getInstallationId(){return installationId;} public void setInstallationId(String v){installationId=v;}
    public String getPluginVersion(){return pluginVersion;} public void setPluginVersion(String v){pluginVersion=v;}
    public List<Map<String,Object>> getEvents(){return events;} public void setEvents(List<Map<String,Object>> v){events=v;}
    public Boolean getEncrypted(){return encrypted;} public void setEncrypted(Boolean v){encrypted=v;}
    public String getAlgorithm(){return algorithm;} public void setAlgorithm(String v){algorithm=v;}
    public String getEncryptedKey(){return encryptedKey;} public void setEncryptedKey(String v){encryptedKey=v;}
    public String getIv(){return iv;} public void setIv(String v){iv=v;}
    public String getCiphertext(){return ciphertext;} public void setCiphertext(String v){ciphertext=v;}
}
