package com.xinke.erp.service;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import com.xinke.erp.mapper.LiveGiftMapper;
import com.xinke.common.exception.ServiceException;
@ExtendWith(MockitoExtension.class)
class LiveSubjectServiceTest {
    @Mock LiveGiftMapper mapper;
    @Mock ShopStaffGroupService shopStaffGroups;
    @InjectMocks LiveSubjectService service;
    @Test
    void shopMappingUsesOperatorsAndTwoCustomerServiceRoles()
    {
        when(mapper.selectShopById(8L)).thenReturn(Map.of("shopId",8L));
        when(shopStaffGroups.groupForShop(8L)).thenReturn(null);
        when(shopStaffGroups.subjectUsers("SHOP",8L)).thenReturn(java.util.Set.of());
        when(mapper.countActiveStaff(anyLong())).thenReturn(1);
        service.saveSubjectMapping(Map.of("subjectType","SHOP","subjectId",8L,
            "operatorIds",List.of(11L,11L),"customerServiceIds",List.of(12L),"customerAdminIds",List.of(15L),
            "anchorIds",List.of(13L),"controllerIds",List.of(14L)),"tester");
        verify(mapper).deleteSubjectMappings("SHOP",8L);
        verify(mapper).insertSubjectMapping("SHOP",8L,11L,"operator","tester");
        verify(mapper).insertSubjectMapping("SHOP",8L,12L,"customer_service","tester");
        verify(mapper).insertSubjectMapping("SHOP",8L,15L,"customer_admin","tester");
        verify(mapper,never()).insertSubjectMapping("SHOP",8L,13L,"anchor","tester");
        verify(mapper,never()).insertSubjectMapping("SHOP",8L,14L,"controller","tester");
    }

    @Test
    void roomMappingRetainsAnchorAndControllerRoles()
    {
        when(mapper.selectRoomById(8L)).thenReturn(Map.of("roomId",8L));
        when(shopStaffGroups.subjectUsers("ROOM",8L)).thenReturn(java.util.Set.of());
        when(mapper.countActiveStaff(anyLong())).thenReturn(1);
        service.saveSubjectMapping(Map.of("subjectType","ROOM","subjectId",8L,
            "anchorIds",List.of(11L),"controllerIds",List.of(12L),
            "operatorIds",List.of(13L),"customerServiceIds",List.of(14L)),"tester");
        verify(mapper).insertSubjectMapping("ROOM",8L,11L,"anchor","tester");
        verify(mapper).insertSubjectMapping("ROOM",8L,12L,"controller","tester");
        verify(mapper,never()).insertSubjectMapping("ROOM",8L,13L,"operator","tester");
    }

    @Test void disabledAssigneeIsRejectedBeforeReplacingMappings() {
        when(mapper.selectRoomById(8L)).thenReturn(Map.of("roomId",8L));
        assertThrows(ServiceException.class,()->service.saveSubjectMapping(Map.of("subjectType","ROOM","subjectId",8L,"anchorIds",List.of(99L)),"tester"));
        verify(mapper,never()).deleteSubjectMappings(anyString(),anyLong());
    }
    @Test
    void deletesShopAndPersonnelMappings()
    {
        when(mapper.selectShopById(8L)).thenReturn(Map.of("shopId",8L));
        when(shopStaffGroups.affectedUsers(8L)).thenReturn(java.util.Set.of());
        service.deleteShop(8L);
        verify(mapper).deleteShop(8L);
        verify(mapper).deleteSubjectMappings("SHOP",8L);
    }

    @Test
    void cannotRestoreDeletedShopThroughSave()
    {
        when(mapper.selectShopById(8L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.saveShop(new java.util.HashMap<>(Map.of(
            "shopId",8L,"shopCode","SHOP-8","shopName","店铺")),"tester"));
        verify(mapper,never()).updateShop(any());
    }

}
