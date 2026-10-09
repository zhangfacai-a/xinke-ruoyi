package com.xinke.erp.livecrm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Runtime configuration for the private COS bucket used by customer attachments. */
@Component
public class LiveCosProperties {
    @Value("${live.cos.enabled:false}") private boolean enabled;
    @Value("${live.cos.region:ap-shanghai}") private String region;
    @Value("${live.cos.bucket:}") private String bucket;
    @Value("${live.cos.secret-id:}") private String secretId;
    @Value("${live.cos.secret-key:}") private String secretKey;
    @Value("${live.cos.presign-seconds:300}") private long presignSeconds;
    @Value("${live.cos.download-seconds:300}") private long downloadSeconds;
    @Value("${live.cos.max-file-size:10485760}") private long maxFileSize;

    public boolean isConfigured() { return enabled && hasText(region) && hasText(bucket) && hasText(secretId) && hasText(secretKey); }
    public boolean isEnabled() { return enabled; }
    public String getRegion() { return region; }
    public String getBucket() { return bucket; }
    public String getSecretId() { return secretId; }
    public String getSecretKey() { return secretKey; }
    public long getPresignSeconds() { return Math.max(60, Math.min(presignSeconds, 900)); }
    public long getDownloadSeconds() { return Math.max(60, Math.min(downloadSeconds, 900)); }
    public long getMaxFileSize() { return maxFileSize > 0 ? Math.min(maxFileSize, 50L * 1024 * 1024) : 10L * 1024 * 1024; }
    private static boolean hasText(String value) { return value != null && !value.trim().isEmpty(); }
}
