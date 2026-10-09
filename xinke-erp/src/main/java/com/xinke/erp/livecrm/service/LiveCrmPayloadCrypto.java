package com.xinke.erp.livecrm.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.xinke.erp.livecrm.domain.LiveCrmBatchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;

/** Decrypts the plugin's per-request RSA-OAEP + AES-GCM envelope. */
@Component
public class LiveCrmPayloadCrypto {
    @Value("${live.crm.v2.encryption.private-key-v3:}")
    private String privateKeyV3;
    @Value("${live.crm.v2.encryption.enabled:true}")
    private boolean enabled;
    @Value("${live.crm.v2.encryption.private-key-v2:${live.crm.v2.encryption.private-key:}}")
    private String privateKeyBase64;
    @Value("${live.crm.v2.encryption.allow-plaintext:true}")
    private boolean allowPlaintext;

    public void decryptInto(LiveCrmBatchRequest request) {
        if (!Boolean.TRUE.equals(request.getEncrypted())) {
            if (enabled && !allowPlaintext) throw new IllegalArgumentException("明文上传已停用");
            return;
        }
        if (!enabled) throw new IllegalArgumentException("应用层加密未启用");
        try {
            if ((privateKeyV3 == null || privateKeyV3.isBlank()) && (privateKeyBase64 == null || privateKeyBase64.isBlank())) throw new IllegalArgumentException("服务端未配置解密私钥");
            if (!"RSA-OAEP-256/AES-256-GCM".equals(request.getAlgorithm())) throw new IllegalArgumentException("不支持的加密算法");
            byte[] wrapped = Base64.getDecoder().decode(request.getEncryptedKey());
            byte[] iv = Base64.getDecoder().decode(request.getIv());
            byte[] ciphertext = Base64.getDecoder().decode(request.getCiphertext());
            Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPPadding");
            rsa.init(Cipher.DECRYPT_MODE, privateKey(), new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT));
            byte[] aes = rsa.doFinal(wrapped);
            Cipher gcm = Cipher.getInstance("AES/GCM/NoPadding");
            gcm.init(Cipher.DECRYPT_MODE, new javax.crypto.spec.SecretKeySpec(aes, "AES"), new GCMParameterSpec(128, iv));
            JSONObject payload = JSON.parseObject(new String(gcm.doFinal(ciphertext), StandardCharsets.UTF_8));
            request.setEvents(payload == null ? Collections.emptyList() : (java.util.List<java.util.Map<String,Object>>)(java.util.List<?>) payload.getList("events", java.util.Map.class));
        } catch (Exception ex) {
            throw new IllegalArgumentException("上传数据解密失败", ex);
        }
    }

    private PrivateKey privateKey() throws Exception {
        String encoded = privateKeyV3 != null && !privateKeyV3.isBlank() ? privateKeyV3 : privateKeyBase64;
        byte[] der = Base64.getDecoder().decode(encoded.replaceAll("\\s+", ""));
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
    }
}
