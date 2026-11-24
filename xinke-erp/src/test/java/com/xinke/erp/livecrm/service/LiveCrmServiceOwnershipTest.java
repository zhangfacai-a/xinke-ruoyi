package com.xinke.erp.livecrm.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.livecrm.domain.LiveCrmRegistrationRequest;
import com.xinke.erp.livecrm.domain.LiveCrmOwnerChangeRequest;
import com.xinke.erp.livecrm.domain.LiveCrmTaskActionRequest;
import com.xinke.erp.livecrm.mapper.LiveCrmMapper;

@ExtendWith(MockitoExtension.class)
class LiveCrmServiceOwnershipTest {
    @Mock private LiveCrmMapper mapper;

    @Test
    void rejectsClaimWhenCustomerAlreadyHasAnotherOwner() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.getTask(11L)).thenReturn(task(21L, 31L));
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of(
                "id", 31L, "ownerUserId", 88L, "ownerName", "原负责人"));
        LiveCrmTaskActionRequest request = new LiveCrmTaskActionRequest();
        request.setAction("CLAIM");

        assertThrows(ServiceException.class,
                () -> service.actOnTask(11L, request, 99L, "新操作人"));

        verify(mapper, never()).claimCustomerIfUnassigned(any(), any(), any());
        verify(mapper, never()).updateTask(any());
        verify(mapper, never()).synchronizeActiveOpportunityOwners(any(), any(), any());
    }

    @Test
    void claimsCustomerAtomicallyAndSynchronizesAllActiveOpportunities() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.getTask(11L)).thenReturn(task(21L, 31L));
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of("id", 31L));
        when(mapper.claimCustomerIfUnassigned(31L, 99L, "领取人")).thenReturn(1);
        LiveCrmTaskActionRequest request = new LiveCrmTaskActionRequest();
        request.setAction("CLAIM");

        service.actOnTask(11L, request, 99L, "领取人");

        verify(mapper).claimCustomerIfUnassigned(31L, 99L, "领取人");
        verify(mapper).synchronizeActiveOpportunityOwners(31L, 99L, "领取人");
        ArgumentCaptor<Map<String, Object>> taskUpdate = mapCaptor();
        verify(mapper).updateTask(taskUpdate.capture());
        org.junit.jupiter.api.Assertions.assertEquals(99L, taskUpdate.getValue().get("assigneeUserId"));
    }

    @Test
    void continuingCommunicationKeepsExistingOwnerWhenNoTransferIsRequested() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.getCustomer(31L)).thenReturn(Map.of("customerId", 31L, "nickname", "客户甲"));
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of(
                "id", 31L, "ownerUserId", 88L, "ownerName", "原负责人"));
        when(mapper.findEnabledUser(88L)).thenReturn(Map.of(
                "userId", 88L, "userName", "原负责人", "account", "owner"));
        when(mapper.findEnabledUser(99L)).thenReturn(Map.of(
                "userId", 99L, "userName", "本次登记人", "account", "operator"));
        when(mapper.findOpenOpportunity(31L, null)).thenReturn(Map.of("id", 21L));

        LiveCrmRegistrationRequest request = new LiveCrmRegistrationRequest();
        request.setCustomerId(31L);
        request.setNickname("客户甲");
        request.setStage("CONTACTED");
        request.setInteractionType("COMMENT");
        request.setAddFanStatus("UNKNOWN");

        Map<String, Object> result = service.register(request, 99L, "本次登记人");

        org.junit.jupiter.api.Assertions.assertEquals(31L, result.get("customerId"));

        ArgumentCaptor<Map<String, Object>> customerUpdate = mapCaptor();
        verify(mapper).updateCustomer(customerUpdate.capture());
        org.junit.jupiter.api.Assertions.assertEquals(88L, customerUpdate.getValue().get("ownerUserId"));
        ArgumentCaptor<Map<String, Object>> opportunityUpdate = mapCaptor();
        verify(mapper).updateOpportunity(opportunityUpdate.capture());
        org.junit.jupiter.api.Assertions.assertEquals(88L, opportunityUpdate.getValue().get("ownerUserId"));
        org.junit.jupiter.api.Assertions.assertEquals("原负责人", opportunityUpdate.getValue().get("ownerName"));
        verify(mapper, never()).insertRegistration(org.mockito.ArgumentMatchers.argThat(
                value -> "OWNER_CHANGE".equals(value.get("interactionType"))));
    }

    @Test
    void registrationAssignsUnclaimedCustomerToCurrentAccount() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.getCustomer(31L)).thenReturn(Map.of("customerId", 31L, "nickname", "客户甲"));
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of("id", 31L));
        when(mapper.findEnabledUser(99L)).thenReturn(Map.of(
                "userId", 99L, "userName", "当前账号姓名", "account", "13900000000"));
        when(mapper.findOpenOpportunity(31L, null)).thenReturn(Map.of("id", 21L));

        LiveCrmRegistrationRequest request = registration(31L);
        service.register(request, 99L, "登录账号");

        ArgumentCaptor<Map<String, Object>> customerUpdate = mapCaptor();
        verify(mapper).updateCustomer(customerUpdate.capture());
        org.junit.jupiter.api.Assertions.assertEquals(99L, customerUpdate.getValue().get("ownerUserId"));
        org.junit.jupiter.api.Assertions.assertEquals("当前账号姓名", customerUpdate.getValue().get("ownerName"));
        verify(mapper).synchronizeActiveOpportunityOwners(31L, 99L, "当前账号姓名");
        verify(mapper).synchronizeOpenTaskAssignees(31L, 99L, "当前账号姓名");
        ArgumentCaptor<Map<String, Object>> records = mapCaptor();
        verify(mapper, org.mockito.Mockito.times(2)).insertRegistration(records.capture());
        org.junit.jupiter.api.Assertions.assertEquals("OWNER_CHANGE", records.getAllValues().get(1).get("interactionType"));
        org.junit.jupiter.api.Assertions.assertEquals("负责人由“待领取”修改为“当前账号姓名”", records.getAllValues().get(1).get("note"));
    }

    @Test
    void registrationCanTransferClaimedCustomerToSelectedOwner() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.getCustomer(31L)).thenReturn(Map.of("customerId", 31L, "nickname", "客户甲"));
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of(
                "id", 31L, "ownerUserId", 88L, "ownerName", "原负责人"));
        when(mapper.findEnabledUser(77L)).thenReturn(Map.of(
                "userId", 77L, "userName", "登记人", "account", "operator"));
        when(mapper.findEnabledUser(99L)).thenReturn(Map.of(
                "userId", 99L, "userName", "新负责人", "account", "new-owner"));
        when(mapper.findOpenOpportunity(31L, null)).thenReturn(Map.of("id", 21L));

        LiveCrmRegistrationRequest request = registration(31L);
        request.setOwnerUserId(99L);
        service.register(request, 77L, "登录账号");

        verify(mapper).synchronizeActiveOpportunityOwners(31L, 99L, "新负责人");
        verify(mapper).synchronizeOpenTaskAssignees(31L, 99L, "新负责人");
        ArgumentCaptor<Map<String, Object>> records = mapCaptor();
        verify(mapper, org.mockito.Mockito.times(2)).insertRegistration(records.capture());
        org.junit.jupiter.api.Assertions.assertEquals("负责人由“原负责人”修改为“新负责人”", records.getAllValues().get(1).get("note"));
    }

    @Test
    void rejectsOrderNumberAlreadyRegisteredToAnotherCustomer() {
        LiveCrmService service = new LiveCrmService(mapper);
        prepareOrderedRegistration(31L, 21L);
        when(mapper.findOrderByOrderNo("6918963760272256717")).thenReturn(Map.of(
                "id", 3637L, "customerId", 285645L, "opportunityId", 27698L,
                "orderNo", "6918963760272256717", "orderStatus", "ENTERED_PENDING_SYNC"));

        LiveCrmRegistrationRequest request = registration(31L);
        request.setStage("ORDERED");
        request.setOrderNo("6918963760272256717");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.register(request, 99L, "当前账号姓名"));

        assertEquals("该订单号已登记到其他客户，请核对客户或订单号", exception.getMessage());
        verify(mapper, never()).insertOrder(any());
        verify(mapper, never()).updateOrder(any());
    }

    @Test
    void reusesOrderNumberAlreadyRegisteredToSameCustomer() {
        LiveCrmService service = new LiveCrmService(mapper);
        prepareOrderedRegistration(31L, 21L);
        when(mapper.findOrderByOrderNo("6918963760272256717")).thenReturn(Map.of(
                "id", 3637L, "customerId", 31L, "opportunityId", 18L,
                "orderNo", "6918963760272256717", "orderStatus", "ENTERED_PENDING_SYNC"));

        LiveCrmRegistrationRequest request = registration(31L);
        request.setStage("ORDERED");
        request.setOrderNo("6918963760272256717");
        service.register(request, 99L, "当前账号姓名");

        verify(mapper, never()).insertOrder(any());
        ArgumentCaptor<Map<String, Object>> orderUpdate = mapCaptor();
        verify(mapper).updateOrder(orderUpdate.capture());
        assertEquals(3637L, orderUpdate.getValue().get("id"));
        assertEquals(31L, orderUpdate.getValue().get("customerId"));
    }

    @Test
    void changesOwnerWithNicknameAndWritesAuditRecord() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of(
                "id", 31L, "ownerUserId", 88L, "ownerName", "13800000000"));
        when(mapper.findEnabledUser(99L)).thenReturn(Map.of(
                "userId", 99L, "userName", "张帅", "account", "13900000000"));
        when(mapper.findEnabledUser(88L)).thenReturn(Map.of(
                "userId", 88L, "userName", "原负责人", "account", "13800000000"));
        when(mapper.findEnabledUser(77L)).thenReturn(Map.of(
                "userId", 77L, "userName", "管理员", "account", "admin"));
        when(mapper.getCustomer(31L)).thenReturn(Map.of("customerId", 31L, "stage", "ORDERED"));
        LiveCrmOwnerChangeRequest request = new LiveCrmOwnerChangeRequest();
        request.setOwnerUserId(99L);
        request.setReason("跨直播间交接");

        Map<String, Object> result = service.changeOwner(31L, request, 77L);

        org.junit.jupiter.api.Assertions.assertEquals("张帅", result.get("ownerName"));
        org.junit.jupiter.api.Assertions.assertEquals(true, result.get("changed"));
        verify(mapper).transferCustomerOwner(31L, 99L, "张帅");
        verify(mapper).synchronizeActiveOpportunityOwners(31L, 99L, "张帅");
        verify(mapper).synchronizeOpenTaskAssignees(31L, 99L, "张帅");
        ArgumentCaptor<Map<String, Object>> record = mapCaptor();
        verify(mapper).insertRegistration(record.capture());
        org.junit.jupiter.api.Assertions.assertEquals("OWNER_CHANGE", record.getValue().get("interactionType"));
        org.junit.jupiter.api.Assertions.assertEquals("管理员", record.getValue().get("registeredName"));
        org.junit.jupiter.api.Assertions.assertEquals("负责人由“原负责人”修改为“张帅”；原因：跨直播间交接", record.getValue().get("note"));
    }

    @Test
    void refreshesOwnerNameButSkipsAuditRecordWhenOwnerDoesNotChange() {
        LiveCrmService service = new LiveCrmService(mapper);
        when(mapper.lockCustomerForUpdate(31L)).thenReturn(Map.of(
                "id", 31L, "ownerUserId", 99L, "ownerName", "旧快照"));
        when(mapper.findEnabledUser(99L)).thenReturn(Map.of(
                "userId", 99L, "userName", "张帅", "account", "13900000000"));
        LiveCrmOwnerChangeRequest request = new LiveCrmOwnerChangeRequest();
        request.setOwnerUserId(99L);

        Map<String, Object> result = service.changeOwner(31L, request, 77L);

        org.junit.jupiter.api.Assertions.assertEquals(false, result.get("changed"));
        org.junit.jupiter.api.Assertions.assertEquals("张帅", result.get("ownerName"));
        verify(mapper).transferCustomerOwner(31L, 99L, "张帅");
        verify(mapper).synchronizeActiveOpportunityOwners(31L, 99L, "张帅");
        verify(mapper).synchronizeOpenTaskAssignees(31L, 99L, "张帅");
        verify(mapper, never()).insertRegistration(any());
    }

    private static Map<String, Object> task(Long opportunityId, Long customerId) {
        Map<String, Object> task = new HashMap<>();
        task.put("id", 11L);
        task.put("opportunityId", opportunityId);
        task.put("customerId", customerId);
        task.put("taskType", "FOLLOWUP");
        return task;
    }

    private static LiveCrmRegistrationRequest registration(Long customerId) {
        LiveCrmRegistrationRequest request = new LiveCrmRegistrationRequest();
        request.setCustomerId(customerId);
        request.setNickname("客户甲");
        request.setStage("CONTACTED");
        request.setInteractionType("COMMENT");
        request.setAddFanStatus("UNKNOWN");
        return request;
    }

    private void prepareOrderedRegistration(Long customerId, Long opportunityId) {
        when(mapper.getCustomer(customerId)).thenReturn(Map.of("customerId", customerId, "nickname", "客户甲"));
        when(mapper.lockCustomerForUpdate(customerId)).thenReturn(Map.of(
                "id", customerId, "ownerUserId", 99L, "ownerName", "当前账号姓名"));
        when(mapper.findEnabledUser(99L)).thenReturn(Map.of(
                "userId", 99L, "userName", "当前账号姓名", "account", "operator"));
        when(mapper.findOpenOpportunity(customerId, null)).thenReturn(Map.of("id", opportunityId));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static ArgumentCaptor<Map<String, Object>> mapCaptor() {
        return ArgumentCaptor.forClass((Class) Map.class);
    }
}
