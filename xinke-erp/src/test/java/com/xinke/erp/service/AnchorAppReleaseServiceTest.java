package com.xinke.erp.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class AnchorAppReleaseServiceTest {
    @TempDir Path root;
    @Test void alteredSignatureAndMalformedManifestAreRejected()throws Exception{
        AnchorAppReleaseService service=new AnchorAppReleaseService(root.resolve("store").toString(),root.resolve("feed").toString());
        assertThrows(Exception.class,()->service.verify("{\"payload\":\"e30=\",\"signature\":\"AA==\"}".getBytes()));
        assertThrows(Exception.class,()->service.verify("{}".getBytes()));
        assertTrue(service.list().isEmpty());
        assertThrows(Exception.class,()->service.publish("../escape"));
    }
}
