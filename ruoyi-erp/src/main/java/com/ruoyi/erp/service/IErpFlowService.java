package com.ruoyi.erp.service;

import java.util.List;
import java.util.Map;

public interface IErpFlowService
{
    List<Map<String, Object>> list(String flowType, Map<String, Object> query);

    Map<String, Object> getInfo(String flowType, Long id);

    int add(String flowType, Map<String, Object> form, String username);

    int edit(String flowType, Long id, Map<String, Object> form, String username);

    int remove(String flowType, Long[] ids);

    int approve(String flowType, String bizNo, String username);

    int createInventoryIn(Map<String, Object> form, String username);

    int createInventoryOut(Map<String, Object> form, String username);

    int createInventoryTransfer(Map<String, Object> form, String username);

    int createInventoryLoss(Map<String, Object> form, String username);

    int generatePayableFromSupplierReconcile(String reconcileNo, String username);
}
