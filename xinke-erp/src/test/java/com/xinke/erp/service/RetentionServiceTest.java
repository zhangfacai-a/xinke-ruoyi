package com.xinke.erp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import com.xinke.common.core.domain.entity.SysUser;
import com.xinke.common.core.domain.model.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.xinke.common.exception.ServiceException;
import com.xinke.erp.mapper.RetentionMapper;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {
 @Mock RetentionMapper mapper;
 RetentionService service;
 @BeforeEach void setup(){service=new RetentionService(mapper);login(1L);lenient().when(mapper.validShop(any())).thenReturn(1);lenient().when(mapper.validAssignee(any(),any())).thenReturn(1);}
 @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
 private void login(Long id){SysUser user=new SysUser();user.setUserName("test");LoginUser login=new LoginUser(id,1L,user,java.util.Set.of());SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(login,null,List.of()));}

 @Test void duplicateOrderHasBusinessMessage(){doThrow(new DuplicateKeyException("duplicate")).when(mapper).insert(any());ServiceException e=assertThrows(ServiceException.class,()->service.create(base(),1L,"测试员"));assertEquals("该店铺的订单号已存在，请搜索已有任务",e.getMessage());}


 @Test void allQueriesCarryAuthenticatedScope(){login(2L);when(mapper.dashboard(any())).thenReturn(Map.of());Map<String,Object> q=Map.of("ownerId",3L);service.list(q);service.dashboard(q);service.export(q);ArgumentCaptor<Map<String,Object>> cap=ArgumentCaptor.forClass(Map.class);verify(mapper,times(2)).list(cap.capture());for(Map<String,Object> v:cap.getAllValues())assertEquals(2L,v.get("scopeUserId"));verify(mapper).dashboard(argThat(v->Long.valueOf(2L).equals(v.get("scopeUserId"))));}
 @Test void resultAndAddressFiltersAreValidatedAndForwarded(){service.list(Map.of("resultType","REFERRAL_SUCCESS","addressStatus","MISSING"));verify(mapper).list(argThat(v->"REFERRAL_SUCCESS".equals(v.get("resultType"))&&"MISSING".equals(v.get("addressStatus"))));assertThrows(ServiceException.class,()->service.list(Map.of("resultType","OTHER")));assertThrows(ServiceException.class,()->service.list(Map.of("addressStatus","EMPTY")));}
 @Test void cannotReadOrEditAnotherPersonsOrder(){login(3L);when(mapper.get(9L)).thenReturn(old());assertThrows(ServiceException.class,()->service.detail(9L));assertThrows(ServiceException.class,()->service.update(9L,update(),3L,"徐顺"));verify(mapper,never()).history(any());verify(mapper,never()).update(any());}
 @Test void ownOrderCannotBeAssignedAway(){login(2L);when(mapper.get(9L)).thenReturn(old());Map<String,Object> v=update();v.put("ownerId",3L);assertThrows(ServiceException.class,()->service.update(9L,v,2L,"王修晨"));assertThrows(ServiceException.class,()->service.create(v,2L,"王修晨"));}
 @Test void combinedContactIgnoresRemovedFields(){Map<String,Object> v=base();v.put("phone","旧电话");v.put("description","旧描述");service.create(v,1L,"管理员");verify(mapper).insert(argThat(row->!row.containsKey("phone")&&!row.containsKey("description")&&"测试电话地址".equals(row.get("address"))));}
 @Test void assignmentRejectsOtherAccounts(){when(mapper.validAssignee(1L,8L)).thenReturn(0);Map<String,Object> v=base();v.put("ownerId",8L);assertThrows(ServiceException.class,()->service.create(v,1L,"管理员"));verify(mapper,never()).insert(any());}
 @Test void addressIsOptionalWhenCreating(){Map<String,Object> v=base();v.remove("address");service.create(v,1L,"管理员");verify(mapper).insert(argThat(row->"".equals(row.get("address"))));}

 @Test void batchCreateSplitsDeduplicatesAndSkipsExistingOrders(){Map<String,Object> v=new HashMap<>();v.put("shopId",1L);v.put("address","");v.put("ownerId",2L);v.put("orderNos",List.of("A001","A001","A002"));doAnswer(invocation->{Map<String,Object> row=invocation.getArgument(0);if("A002".equals(row.get("orderNo")))throw new DuplicateKeyException("duplicate");row.put("id",11L);return 1;}).when(mapper).insert(any());Map<String,Object> result=service.batchCreate(v,1L,"管理员");assertEquals(1,result.get("createdCount"));assertEquals(1,result.get("skippedCount"));assertEquals(1,result.get("duplicateInputCount"));verify(mapper,times(2)).insert(any());verify(mapper).historyInsert(argThat(row->"CREATE".equals(row.get("action"))));}
 @Test void batchCheckSeparatesExistingInputDuplicatesAndValidOrders(){String invalid="X".repeat(65);when(mapper.existingOrderNos(eq(1L),anyList())).thenReturn(List.of("A002"));Map<String,Object> result=service.batchCheck(Map.of("shopId",1L,"orderNos",List.of("A001","a001","A002","A003",invalid)));assertEquals(List.of("A001","A003"),result.get("validOrderNos"));assertEquals(List.of("A002"),result.get("existingOrderNos"));assertEquals(List.of("a001"),result.get("duplicateInputOrderNos"));assertEquals(List.of(invalid),result.get("invalidOrderNos"));verify(mapper).existingOrderNos(1L,List.of("A001","A002","A003"));}

 @Test void followupMovesPendingToFollowingAndClearsInactiveAmounts(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=update();v.put("isFollowup",true);v.put("retentionAmount","999");v.put("referralAmount","888");v.put("freightAmount","");service.update(9L,v,1L,"测试员");ArgumentCaptor<Map<String,Object>> cap=ArgumentCaptor.forClass(Map.class);verify(mapper).update(cap.capture());assertEquals("FOLLOWING",cap.getValue().get("progress"));assertEquals(BigDecimal.ZERO,cap.getValue().get("retentionAmount"));assertEquals(BigDecimal.ZERO,cap.getValue().get("referralAmount"));assertEquals(BigDecimal.ZERO,cap.getValue().get("freightAmount"));}

 @Test void changingProgressStartsFirstFollowupWithoutCheckbox(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=update();v.put("progress","FOLLOWING");service.update(9L,v,1L,"测试员");verify(mapper).update(argThat(row->Integer.valueOf(1).equals(row.get("startsFollowup"))));}

 @Test void completedOrderRequiresOutcome(){when(mapper.get(9L)).thenReturn(old());Map<String,Object> v=update();v.put("progress","COMPLETED");ServiceException e=assertThrows(ServiceException.class,()->service.update(9L,v,1L,"测试员"));assertEquals("完成前请选择至少一项业务结果",e.getMessage());}

 @Test void staleVersionCannotOverwrite(){Map<String,Object> old=old();old.put("version",3);when(mapper.get(9L)).thenReturn(old);Map<String,Object> v=update();v.put("version",2);assertThrows(ServiceException.class,()->service.update(9L,v,1L,"测试员"));verify(mapper,never()).update(any());}

 @Test void exportRangeAndRowCountAreBounded(){Map<String,Object> wide=Map.of("beginDate","2026-01-01","endDate","2026-02-02");assertThrows(ServiceException.class,()->service.export(wide));when(mapper.count(any())).thenReturn(5001L);assertThrows(ServiceException.class,()->service.export(Map.of("beginDate","2026-10-01","endDate","2026-10-02")));}

 @Test void referralAndFreightCanCoexistWithFailedRetention(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=update();v.put("progress","COMPLETED");v.put("retentionResult","FAILED");v.put("referralSuccess",true);v.put("referralShopId",1L);v.put("referralOrderNo","6955000000000000002");v.put("referralAmount","1499.00");v.put("freightCollected",true);v.put("freightAmount","50.00");service.update(9L,v,1L,"测试员");ArgumentCaptor<Map<String,Object>> cap=ArgumentCaptor.forClass(Map.class);verify(mapper).update(cap.capture());assertEquals(new BigDecimal("1499.00"),cap.getValue().get("referralAmount"));assertEquals(new BigDecimal("50.00"),cap.getValue().get("freightAmount"));assertEquals(BigDecimal.ZERO,cap.getValue().get("retentionAmount"));}

 @Test void negativeAndExcessPrecisionAmountsAreRejected(){when(mapper.get(9L)).thenReturn(old());for(String amount:List.of("-1","1.001","1000000000000")){Map<String,Object> v=update();v.put("retentionResult","SUCCESS");v.put("retentionAmount",amount);assertThrows(ServiceException.class,()->service.update(9L,v,1L,"测试员"));}verify(mapper,never()).update(any());}

 @Test void referralRequiresShopOrderAndAmount(){when(mapper.get(9L)).thenReturn(old());Map<String,Object> v=update();v.put("referralSuccess",true);assertThrows(ServiceException.class,()->service.update(9L,v,1L,"测试员"));v.put("referralShopId",1L);assertThrows(ServiceException.class,()->service.update(9L,v,1L,"测试员"));v.put("referralOrderNo","6955000000000000002");assertThrows(ServiceException.class,()->service.update(9L,v,1L,"测试员"));}

 @Test void ownerChangeRecordsNamesAndOperator(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);when(mapper.userName(2L)).thenReturn("王修晨");when(mapper.userName(3L)).thenReturn("徐顺");Map<String,Object> v=update();v.put("ownerId",3L);service.update(9L,v,1L,"管理员");ArgumentCaptor<Map<String,Object>> cap=ArgumentCaptor.forClass(Map.class);verify(mapper,times(2)).historyInsert(cap.capture());Map<String,Object> h=cap.getAllValues().get(1);assertEquals("OWNER_CHANGE",h.get("action"));assertEquals("管理员",h.get("userName"));org.junit.jupiter.api.Assertions.assertTrue(h.get("oldValue").toString().contains("王修晨"));org.junit.jupiter.api.Assertions.assertTrue(h.get("newValue").toString().contains("徐顺"));}

 @Test void concurrentEditTriggersVersionConflict(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(0);assertThrows(ServiceException.class,()->service.update(9L,update(),1L,"测试员"));verify(mapper,never()).historyInsert(any());}


 @Test void phoneStaffCanFollowUpWithoutChangingOrder(){login(2L);when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);service.update(9L,update(),2L,"电话客服");verify(mapper).update(any());}
 @Test void phoneStaffCanEditOwnAddressButNotOwner(){login(2L);when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=update();v.put("address","新电话和地址");v.put("note","");service.update(9L,v,2L,"电话客服");verify(mapper).update(argThat(row->"新电话和地址".equals(row.get("address"))&&"修改电话地址".equals(row.get("note"))));v.put("ownerId",3L);assertThrows(ServiceException.class,()->service.update(9L,v,2L,"电话客服"));}
 @Test void managerCanBatchAssignOpenOrders(){when(mapper.get(9L)).thenReturn(old());when(mapper.updateOwner(any())).thenReturn(1);when(mapper.userName(3L)).thenReturn("徐顺");Map<String,Object> input=Map.of("ownerId",3L,"orders",List.of(Map.of("id",9L,"version",0L)));Map<String,Object> result=service.batchAssign(input,1L,"管理员");assertEquals(1,result.get("assignedCount"));verify(mapper).updateOwner(argThat(row->Long.valueOf(3L).equals(row.get("ownerId"))));verify(mapper).historyInsert(argThat(row->"OWNER_CHANGE".equals(row.get("action"))&&"批量分配电话客服".equals(row.get("note"))));}
 @Test void batchAssignRejectsCompletedOrders(){Map<String,Object> row=old();row.put("progress","COMPLETED");when(mapper.get(9L)).thenReturn(row);Map<String,Object> input=Map.of("ownerId",3L,"orders",List.of(Map.of("id",9L,"version",0L)));assertThrows(ServiceException.class,()->service.batchAssign(input,1L,"管理员"));verify(mapper,never()).updateOwner(any());}
 @Test void shopAdminCanCreateAndEditAssignedShop(){login(4L);when(mapper.isShopAdmin(1L,4L)).thenReturn(1);when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=base();v.remove("description");service.create(v,4L,"客服管理员");service.update(9L,update(),4L,"客服管理员");verify(mapper).insert(any());verify(mapper).update(any());}
 @Test void shopAdminCannotCreateInOtherShop(){login(4L);assertThrows(ServiceException.class,()->service.create(base(),4L,"客服管理员"));verify(mapper,never()).insert(any());}
 @Test void freightAmountAloneMarksCollected(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=update();v.put("freightAmount","0.00");service.update(9L,v,1L,"管理员");verify(mapper).update(argThat(row->Integer.valueOf(1).equals(row.get("freightCollected"))));}
 @Test void successfulRetentionClearsFreight(){when(mapper.get(9L)).thenReturn(old());when(mapper.update(any())).thenReturn(1);Map<String,Object> v=update();v.put("retentionResult","SUCCESS");v.put("retentionAmount","100.00");v.put("freightAmount","50.00");service.update(9L,v,1L,"管理员");verify(mapper).update(argThat(row->Integer.valueOf(0).equals(row.get("freightCollected"))&&BigDecimal.ZERO.equals(row.get("freightAmount"))));}

 private Map<String,Object> base(){Map<String,Object> v=new HashMap<>();v.put("shopId",1L);v.put("orderNo","6955000000000000001");v.put("address","测试电话地址");v.put("ownerId",2L);return v;}
 @Test void managerCanDeleteOrderAndItsHistory(){login(4L);when(mapper.get(9L)).thenReturn(old());when(mapper.isShopAdmin(1L,4L)).thenReturn(1);when(mapper.deleteOrder(9L,0L)).thenReturn(1);service.delete(9L,0L);var order=inOrder(mapper);order.verify(mapper).deleteOrder(9L,0L);order.verify(mapper).deleteHistory(9L);}
 @Test void phoneStaffCannotDeleteOwnOrder(){login(2L);when(mapper.get(9L)).thenReturn(old());assertThrows(ServiceException.class,()->service.delete(9L,0L));verify(mapper,never()).deleteOrder(any(),any());verify(mapper,never()).deleteHistory(any());}
 @Test void managerCannotDeleteAnotherShopsOrder(){login(4L);when(mapper.get(9L)).thenReturn(old());assertThrows(ServiceException.class,()->service.delete(9L,0L));verify(mapper,never()).deleteOrder(any(),any());}
 @Test void staleDeletionDoesNotRemoveHistory(){when(mapper.get(9L)).thenReturn(old());assertThrows(ServiceException.class,()->service.delete(9L,0L));verify(mapper,never()).deleteHistory(any());}
 private Map<String,Object> old(){Map<String,Object> v=base();v.put("id",9L);v.put("version",0L);v.put("progress","PENDING");v.put("retentionResult","UNKNOWN");v.put("ownerName","测试员");return v;}
 private Map<String,Object> update(){Map<String,Object> v=base();v.put("version",0);v.put("progress","PENDING");v.put("retentionResult","UNKNOWN");v.put("note","电话沟通记录");v.put("isFollowup",false);v.put("referralSuccess",false);v.put("freightCollected",false);return v;}
}
