package com.xinke.erp.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class RetentionExportTest {
 @Test void preservesLongOrderNumbersAsText(){assertEquals("\"'6955000000000000001\"",RetentionController.csvCell("6955000000000000001",true));}
 @Test void escapesCsvAndProtectsFormulas(){assertEquals("\"'  =1+1\"",RetentionController.csvCell("  =1+1",false));assertEquals("\"地址,\"\"备注\"\"\n第二行\"",RetentionController.csvCell("地址,\"备注\"\n第二行",false));}
}
