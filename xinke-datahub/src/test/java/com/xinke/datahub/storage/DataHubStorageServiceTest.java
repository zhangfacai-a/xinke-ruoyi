package com.xinke.datahub.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.xinke.common.config.XinKeConfig;
import com.xinke.datahub.config.DataHubProperties;

class DataHubStorageServiceTest
{
    @TempDir
    Path tempDir;

    @Test
    void rejectsStorageInsidePublicProfile()
    {
        String originalProfile = XinKeConfig.getProfile();
        XinKeConfig xinkeConfig = new XinKeConfig();
        try
        {
            xinkeConfig.setProfile(tempDir.toString());
            DataHubProperties properties = new DataHubProperties();
            properties.setStoragePath(tempDir.resolve("datahub").toString());
            assertThrows(IllegalStateException.class, () -> new DataHubStorageService(properties, xinkeConfig));
        }
        finally
        {
            xinkeConfig.setProfile(originalProfile);
        }
    }
}
