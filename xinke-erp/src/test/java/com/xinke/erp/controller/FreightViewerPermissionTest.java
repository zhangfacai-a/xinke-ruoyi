package com.xinke.erp.controller;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class FreightViewerPermissionTest {
    @Test void viewerPermissionDoesNotPermitWritesOrExport() throws Exception {
        assertEquals("@ss.hasAnyPermi('live:freight:access,live:freight:viewAll')",
            FreightCollectionController.class.getAnnotation(PreAuthorize.class).value());
        for(var method:java.util.List.of(
            FreightCollectionController.class.getMethod("create",Map.class),
            FreightCollectionController.class.getMethod("update",Long.class,Map.class),
            FreightCollectionController.class.getMethod("importFile",MultipartFile.class),
            FreightCollectionController.class.getMethod("export",Map.class,jakarta.servlet.http.HttpServletResponse.class))) {
            assertEquals("@ss.hasPermi('live:freight:access')",method.getAnnotation(PreAuthorize.class).value());
        }
    }
}
