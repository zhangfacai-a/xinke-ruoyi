package com.xinke.erp.livecrm.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.xinke.common.exception.ServiceException;
import com.xinke.common.config.XinKeConfig;
import com.xinke.erp.livecrm.config.LiveCosProperties;
import com.xinke.erp.livecrm.domain.LiveCrmAttachmentCompleteRequest;
import com.xinke.erp.livecrm.domain.LiveCrmAttachmentPresignRequest;
import com.xinke.erp.livecrm.mapper.LiveCrmMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.annotation.PreDestroy;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class LiveCrmAttachmentService {
    private static final Pattern SHA256 = Pattern.compile("[0-9a-fA-F]{64}");
    private static final Pattern SAFE_NAME = Pattern.compile("[^\\p{L}\\p{N}._-]");
    private final LiveCrmMapper mapper;
    private final LiveCosProperties properties;
    private volatile COSClient client;

    public LiveCrmAttachmentService(LiveCrmMapper mapper, LiveCosProperties properties) {
        this.mapper = mapper;
        this.properties = properties;
    }

    public Map<String, Object> presign(LiveCrmAttachmentPresignRequest request, Long userId) {
        ensureConfigured();
        validateFile(request.getOriginalName(), request.getContentType(), request.getFileSize(), request.getSha256());
        if (mapper.getCustomer(request.getCustomerId()) == null) throw new ServiceException("客户不存在");
        String key = buildObjectKey(request.getCustomerId(), request.getOriginalName());
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("customerId", request.getCustomerId()); row.put("opportunityId", request.getOpportunityId());
        row.put("orderId", request.getOrderId()); row.put("objectKey", key); row.put("originalName", request.getOriginalName());
        row.put("contentType", request.getContentType().toLowerCase(Locale.ROOT)); row.put("fileSize", request.getFileSize());
        row.put("sha256", blankToNull(request.getSha256())); row.put("uploadedBy", userId);
        mapper.insertAttachment(row);
        GeneratePresignedUrlRequest signed = new GeneratePresignedUrlRequest(properties.getBucket(), key, HttpMethodName.PUT);
        signed.setExpiration(java.util.Date.from(Instant.now().plusSeconds(properties.getPresignSeconds())));
        signed.setContentType(request.getContentType().toLowerCase(Locale.ROOT));
        URL uploadUrl = getClient().generatePresignedUrl(signed);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attachmentId", row.get("id")); result.put("objectKey", key); result.put("uploadUrl", uploadUrl.toExternalForm());
        result.put("contentType", request.getContentType().toLowerCase(Locale.ROOT));
        result.put("expiresAt", signed.getExpiration().toInstant().toString());
        return result;
    }

    public Map<String, Object> upload(MultipartFile file, Long customerId, Long opportunityId,
                                      Long orderId, Long userId) {
        ensureConfigured();
        if (file == null || file.isEmpty()) throw new ServiceException("请选择需要上传的图片");
        String originalName = file.getOriginalFilename();
        String contentType = file.getContentType();
        validateFile(originalName, contentType, file.getSize(), null);
        if (mapper.getCustomer(customerId) == null) throw new ServiceException("客户不存在");

        String key = buildObjectKey(customerId, originalName);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("customerId", customerId); row.put("opportunityId", opportunityId);
        row.put("orderId", orderId); row.put("objectKey", key); row.put("originalName", originalName);
        row.put("contentType", contentType.toLowerCase(Locale.ROOT)); row.put("fileSize", file.getSize());
        row.put("sha256", null); row.put("uploadedBy", userId);
        mapper.insertAttachment(row);
        Long attachmentId = ((Number) row.get("id")).longValue();

        try (InputStream input = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(contentType.toLowerCase(Locale.ROOT));
            getClient().putObject(new PutObjectRequest(properties.getBucket(), key, input, metadata));
            mapper.completeAttachment(attachmentId, null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("attachmentId", attachmentId);
            result.put("originalName", originalName);
            result.put("status", "READY");
            return result;
        } catch (Exception ex) {
            return saveLocally(file, attachmentId, key, originalName, ex);
        }
    }

    @Transactional
    public Map<String, Object> complete(Long attachmentId, LiveCrmAttachmentCompleteRequest request) {
        ensureConfigured();
        Map<String, Object> attachment = mapper.getAttachment(attachmentId);
        if (attachment == null) throw new ServiceException("附件不存在");
        try {
            ObjectMetadata metadata = getClient().getObjectMetadata(properties.getBucket(), String.valueOf(attachment.get("objectKey")));
            long expected = ((Number) attachment.get("fileSize")).longValue();
            String expectedType = String.valueOf(attachment.get("contentType"));
            if (metadata == null || metadata.getContentLength() != expected
                    || (metadata.getContentType() != null && !expectedType.equalsIgnoreCase(metadata.getContentType()))) {
                mapper.markAttachmentFailed(attachmentId, "文件大小校验失败");
                throw new ServiceException("上传文件校验失败，请重试");
            }
            String sha256 = blankToNull(request == null ? null : request.getSha256());
            if (sha256 != null && !SHA256.matcher(sha256).matches()) throw new ServiceException("文件校验码格式不正确");
            mapper.completeAttachment(attachmentId, sha256);
            return viewUrl(attachmentId);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            mapper.markAttachmentFailed(attachmentId, ex.getMessage());
            throw new ServiceException("无法确认 COS 文件，请检查上传是否完成");
        }
    }

    public Map<String, Object> viewUrl(Long attachmentId) {
        ensureConfigured();
        Map<String, Object> attachment = mapper.getAttachment(attachmentId);
        if (attachment == null || !"READY".equals(attachment.get("status"))) throw new ServiceException("附件尚未上传完成");
        String objectKey = String.valueOf(attachment.get("objectKey"));
        if (objectKey.startsWith("local:")) {
            String relative = objectKey.substring("local:".length()).replace('\\', '/');
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("attachmentId", attachmentId);
            result.put("url", "/profile/" + relative);
            result.put("storage", "LOCAL");
            return result;
        }
        GeneratePresignedUrlRequest signed = new GeneratePresignedUrlRequest(properties.getBucket(), objectKey, HttpMethodName.GET);
        signed.setExpiration(java.util.Date.from(Instant.now().plusSeconds(properties.getDownloadSeconds())));
        URL url = getClient().generatePresignedUrl(signed);
        Map<String, Object> result = new LinkedHashMap<>(); result.put("attachmentId", attachmentId); result.put("url", url.toExternalForm()); result.put("expiresAt", signed.getExpiration().toInstant().toString());
        return result;
    }

    /** Builds a download link for an already loaded attachment without another database query. */
    public String exportViewUrl(Map<String, Object> attachment) {
        String objectKey = String.valueOf(attachment.get("objectKey"));
        if (objectKey.startsWith("local:")) {
            String relative = objectKey.substring("local:".length()).replace('\\', '/');
            return currentBaseUrl() + "/profile/" + relative;
        }
        ensureConfigured();
        GeneratePresignedUrlRequest signed = new GeneratePresignedUrlRequest(
                properties.getBucket(), objectKey, HttpMethodName.GET);
        signed.setExpiration(java.util.Date.from(
                Instant.now().plusSeconds(properties.getExportDownloadSeconds())));
        return getClient().generatePresignedUrl(signed).toExternalForm();
    }

    private String currentBaseUrl() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) return "";
        var request = attributes.getRequest();
        String proto = blankToNull(request.getHeader("X-Forwarded-Proto"));
        String host = blankToNull(request.getHeader("X-Forwarded-Host"));
        if (proto == null) proto = request.getScheme();
        if (host == null) host = blankToNull(request.getHeader("Host"));
        if (host == null) {
            host = request.getServerName();
            if (!("http".equals(proto) && request.getServerPort() == 80)
                    && !("https".equals(proto) && request.getServerPort() == 443)) {
                host += ":" + request.getServerPort();
            }
        }
        return proto + "://" + host + request.getContextPath();
    }

    private COSClient getClient() {
        COSClient current = client;
        if (current == null) synchronized (this) {
            current = client;
            if (current == null) {
                COSCredentials credentials = new BasicCOSCredentials(properties.getSecretId(), properties.getSecretKey());
                client = current = new com.qcloud.cos.COSClient(credentials, new ClientConfig(new Region(properties.getRegion())));
            }
        }
        return current;
    }

    @PreDestroy
    public void shutdown() {
        COSClient current = client;
        if (current != null) current.shutdown();
    }

    private void ensureConfigured() { if (!properties.isConfigured()) throw new ServiceException("COS 未配置，请联系管理员"); }
    private Map<String, Object> saveLocally(MultipartFile file, Long attachmentId, String cloudKey,
                                            String originalName, Exception cloudError) {
        String relative = "live-customer/" + LocalDate.now() + "/" + cloudKey.substring(cloudKey.lastIndexOf('/') + 1);
        Path profile = Path.of(XinKeConfig.getProfile()).toAbsolutePath().normalize();
        Path target = profile.resolve(relative).normalize();
        if (!target.startsWith(profile)) throw new ServiceException("本地图片路径不正确");
        try (InputStream input = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
            mapper.updateAttachmentObjectKey(attachmentId, "local:" + relative.replace('\\', '/'));
            mapper.completeAttachment(attachmentId, null);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("attachmentId", attachmentId);
            result.put("originalName", originalName);
            result.put("status", "READY");
            result.put("storage", "LOCAL");
            result.put("cloudFailure", safeFailureReason(cloudError));
            return result;
        } catch (Exception localError) {
            mapper.markAttachmentFailed(attachmentId, "COS: " + safeFailureReason(cloudError) + "; LOCAL: " + safeFailureReason(localError));
            throw new ServiceException("图片云存储不可用，本地保存也失败，请联系管理员");
        }
    }
    private static String buildObjectKey(Long customerId, String originalName) {
        return "live-customer-v2/" + LocalDate.now() + "/" + customerId + "/"
                + UUID.randomUUID().toString().replace("-", "") + "-" + safeName(originalName);
    }
    private static String safeFailureReason(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) message = ex.getClass().getSimpleName();
        return message.length() > 480 ? message.substring(0, 480) : message;
    }
    private void validateFile(String name, String contentType, Long size, String sha256) {
        if (size == null || size <= 0 || size > properties.getMaxFileSize()) throw new ServiceException("截图大小不能超过 " + (properties.getMaxFileSize() / 1024 / 1024) + "MB");
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!(type.equals("image/jpeg") || type.equals("image/png") || type.equals("image/webp"))) throw new ServiceException("只支持 JPG、PNG 或 WEBP 图片");
        if (blankToNull(sha256) != null && !SHA256.matcher(sha256).matches()) throw new ServiceException("文件校验码格式不正确");
        if (name == null || safeName(name).isBlank()) throw new ServiceException("文件名不正确");
    }
    private static String safeName(String value) { String name = value == null ? "image" : value.replace('\\', '/'); name = name.substring(name.lastIndexOf('/') + 1); name = SAFE_NAME.matcher(name).replaceAll("_"); return name.length() > 180 ? name.substring(name.length() - 180) : name; }
    private static String blankToNull(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
