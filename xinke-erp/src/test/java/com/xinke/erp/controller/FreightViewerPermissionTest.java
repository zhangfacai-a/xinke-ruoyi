package com.xinke.erp.controller;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class FreightViewerPermissionTest {
    @Test void viewerPermissionDoesNotPermitWritesOrExport() throws Exception {
        assertEquals("@freightAccess.allowed('view')",
            FreightCollectionController.class.getAnnotation(PreAuthorize.class).value());
        for(var method:java.util.List.of(
            FreightCollectionController.class.getMethod("create",Map.class),
            FreightCollectionController.class.getMethod("update",Long.class,Map.class),
            FreightCollectionController.class.getMethod("importFile",MultipartFile.class),
            FreightCollectionController.class.getMethod("export",Map.class,jakarta.servlet.http.HttpServletResponse.class))) {
            String action=switch(method.getName()){case "create"->"create";case "update"->"edit";case "importFile"->"import";default->"export";};
            assertEquals("@freightAccess.allowed('"+action+"')",method.getAnnotation(PreAuthorize.class).value());
        }
    }
}
