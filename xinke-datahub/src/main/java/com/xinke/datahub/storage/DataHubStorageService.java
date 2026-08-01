package com.xinke.datahub.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.DigestInputStream;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.xinke.common.config.XinKeConfig;
import com.xinke.common.exception.ServiceException;
import com.xinke.datahub.config.DataHubProperties;

@Service
public class DataHubStorageService
{
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("xls", "xlsx", "csv");

    private final DataHubProperties properties;
    private final Path basePath;

    public DataHubStorageService(DataHubProperties properties, XinKeConfig xinkeConfig)
    {
        this.properties = properties;
        String configured = properties.getStoragePath();
        if (configured == null || configured.isBlank())
            configured = Path.of(System.getProperty("java.io.tmpdir"), "xinke-datahub").toString();
        basePath = Path.of(configured).toAbsolutePath().normalize();
        rejectPublicProfilePath(basePath);
    }

    public StoredDataHubFile save(MultipartFile file, String jobNo)
    {
        validate(file);
        String extension = extension(file.getOriginalFilename());
        Path directory = basePath.resolve(jobNo).normalize();
        ensureInsideBase(directory);
        Path target = directory.resolve("source." + extension).normalize();
        ensureInsideBase(target);
        try
        {
            Files.createDirectories(directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            long size;
            try (InputStream raw = file.getInputStream(); DigestInputStream input = new DigestInputStream(raw, digest);
                    OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE))
            {
                size = input.transferTo(output);
            }
            return new StoredDataHubFile(target, HexFormat.of().formatHex(digest.digest()), size, extension);
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            deleteQuietly(target);
            deleteQuietly(directory);
            throw new ServiceException("上传文件保存失败").setDetailMessage(e.getMessage());
        }
    }

    public Path resolve(String storedPath)
    {
        if (storedPath == null || storedPath.isBlank()) throw new ServiceException("导入文件不存在");
        Path path = Path.of(storedPath).toAbsolutePath().normalize();
        ensureInsideBase(path);
        if (!Files.isRegularFile(path)) throw new ServiceException("导入文件已过期或不存在");
        return path;
    }

    public void deleteStoredFile(String storedPath)
    {
        if (storedPath == null || storedPath.isBlank()) return;
        Path path = Path.of(storedPath).toAbsolutePath().normalize();
        ensureInsideBase(path);
        try
        {
            Files.deleteIfExists(path);
            Path parent = path.getParent();
            if (parent != null && !parent.equals(basePath))
            {
                try { Files.deleteIfExists(parent); }
                catch (DirectoryNotEmptyException ignored) { }
            }
        }
        catch (IOException e)
        {
            throw new ServiceException("过期导入文件清理失败").setDetailMessage(e.getMessage());
        }
    }

    private void validate(MultipartFile file)
    {
        if (file == null || file.isEmpty()) throw new ServiceException("请选择Excel或CSV文件");
        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new ServiceException("只支持 .xls、.xlsx 和 .csv 文件");
        if (file.getSize() > properties.getMaxFileSize().toBytes())
            throw new ServiceException("文件不能超过" + properties.getMaxFileSize().toMegabytes() + "MB");
    }

    private String extension(String name)
    {
        if (name == null) return "";
        String safeName = name.replace('\\', '/');
        safeName = safeName.substring(safeName.lastIndexOf('/') + 1);
        int dot = safeName.lastIndexOf('.');
        return dot < 0 ? "" : safeName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void ensureInsideBase(Path path)
    {
        if (!path.startsWith(basePath)) throw new ServiceException("文件存储路径不合法");
    }

    private void rejectPublicProfilePath(Path path)
    {
        String profile = XinKeConfig.getProfile();
        if (profile == null || profile.isBlank()) return;
        Path publicProfile = Path.of(profile).toAbsolutePath().normalize();
        if (path.startsWith(publicProfile))
            throw new IllegalStateException("datahub.storage-path 不能位于 xinke.profile 静态资源目录内");
    }

    private void deleteQuietly(Path path)
    {
        try { Files.deleteIfExists(path); }
        catch (IOException ignored) { }
    }
}
