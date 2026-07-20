package com.xinke.erp.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.domain.ErpProductSku;
import com.xinke.erp.domain.ErpPurchaseOrder;
import com.xinke.erp.domain.ErpPurchaseOrderItem;
import com.xinke.erp.domain.ErpSupplier;
import com.xinke.erp.domain.ErpWarehouse;
import com.xinke.erp.mapper.ErpProductSkuMapper;
import com.xinke.erp.mapper.ErpPurchaseOrderMapper;
import com.xinke.erp.mapper.ErpSupplierMapper;
import com.xinke.erp.mapper.ErpWarehouseMapper;

@ExtendWith(MockitoExtension.class)
class ErpPurchaseOrderServiceImplTest
{
    @Mock private ErpPurchaseOrderMapper purchaseOrderMapper;
    @Mock private ErpWarehouseMapper warehouseMapper;
    @Mock private ErpSupplierMapper supplierMapper;
    @Mock private ErpProductSkuMapper skuMapper;
    @InjectMocks private ErpPurchaseOrderServiceImpl service;

    @BeforeEach
    void setUpMasterData()
    {
        ErpSupplier supplier = new ErpSupplier();
        supplier.setSupplierId(1L);
        supplier.setSupplierName("测试供应商");
        supplier.setStatus("0");
        when(supplierMapper.selectSupplierById(1L)).thenReturn(supplier);

        ErpWarehouse warehouse = new ErpWarehouse();
        warehouse.setWarehouseId(2L);
        warehouse.setWarehouseUsage("normal");
        warehouse.setStatus("0");
        when(warehouseMapper.selectWarehouseById(2L)).thenReturn(warehouse);
    }

    @Test
    void createCalculatesTaxAndTaxInclusiveTotal()
    {
        ErpProductSku sku = activeSku(3L, "SKU-003", "测试SKU");
        when(skuMapper.selectSkuById(3L)).thenReturn(sku);
        when(purchaseOrderMapper.insertPurchaseOrder(any())).thenAnswer(invocation -> {
            ErpPurchaseOrder order = invocation.getArgument(0);
            order.setPurchaseId(9L);
            return 1;
        });
        when(purchaseOrderMapper.batchInsertPurchaseOrderItems(any())).thenReturn(1);

        ErpPurchaseOrder order = order(item(3L, 2, "10.00", "0.13"));
        service.insertPurchaseOrder(order);

        assertEquals(new BigDecimal("20.00"), order.getTotalAmount());
        assertEquals(new BigDecimal("2.60"), order.getTaxAmount());
        assertEquals(new BigDecimal("22.60"), order.getTotalWithTax());
        assertEquals("测试供应商", order.getSupplierName());
        assertEquals(9L, order.getItems().get(0).getPurchaseId());
        assertEquals("SKU-003", order.getItems().get(0).getSkuCode());
    }

    @Test
    void duplicateSkuIsRejected()
    {
        when(skuMapper.selectSkuById(3L)).thenReturn(activeSku(3L, "SKU-003", "测试SKU"));
        ErpPurchaseOrder order = order(
                item(3L, 1, "10.00", "0.13"),
                item(3L, 2, "9.00", "0.13"));

        assertThrows(ServiceException.class, () -> service.insertPurchaseOrder(order));
    }

    private ErpPurchaseOrder order(ErpPurchaseOrderItem... items)
    {
        ErpPurchaseOrder order = new ErpPurchaseOrder();
        order.setPurchaseNo("PO-TEST");
        order.setSupplierId(1L);
        order.setWarehouseId(2L);
        order.setItems(List.of(items));
        return order;
    }

    private ErpPurchaseOrderItem item(Long skuId, int quantity, String price, String taxRate)
    {
        ErpPurchaseOrderItem item = new ErpPurchaseOrderItem();
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setPurchasePrice(new BigDecimal(price));
        item.setTaxRate(new BigDecimal(taxRate));
        return item;
    }

    private ErpProductSku activeSku(Long skuId, String skuCode, String skuName)
    {
        ErpProductSku sku = new ErpProductSku();
        sku.setSkuId(skuId);
        sku.setSkuCode(skuCode);
        sku.setSkuName(skuName);
        sku.setStatus("0");
        return sku;
    }
}
