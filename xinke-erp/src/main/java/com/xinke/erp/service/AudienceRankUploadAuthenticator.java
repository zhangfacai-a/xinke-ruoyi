package com.xinke.erp.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.xinke.common.constant.HttpStatus;
import com.xinke.common.exception.ServiceException;

@Component
public class AudienceRankUploadAuthenticator
{
    private final String uploadKey;

    public AudienceRankUploadAuthenticator(@Value("${live.audience-rank.upload-key:}") String uploadKey)
    {
        this.uploadKey = uploadKey;
    }

    public void authenticate(String suppliedKey)
    {
        if (uploadKey == null || uploadKey.isBlank())
        {
            throw new ServiceException("观众榜单上传密钥尚未配置", 503);
        }
        byte[] expected = digest(uploadKey);
        byte[] actual = digest(suppliedKey == null ? "" : suppliedKey);
        if (!MessageDigest.isEqual(expected, actual))
        {
            throw new ServiceException("上传密钥不正确", HttpStatus.UNAUTHORIZED);
        }
    }

    private byte[] digest(String value)
    {
        try
        {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
