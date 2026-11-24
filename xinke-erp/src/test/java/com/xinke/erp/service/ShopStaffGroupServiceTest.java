package com.xinke.erp.service;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.xinke.erp.mapper.LiveGiftMapper;
import com.xinke.erp.mapper.ShopStaffGroupMapper;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ShopStaffGroupServiceTest {
    private final ShopStaffGroupMapper groups=mock(ShopStaffGroupMapper.class);
    private final LiveGiftMapper gifts=mock(LiveGiftMapper.class);
    private final LiveAutoAccessService access=mock(LiveAutoAccessService.class);
    private final ShopStaffGroupService service=new ShopStaffGroupService(groups,gifts,access);

    @Test void groupSaveUpdatesAllSelectedShopsAndDetachesRemovedShop() {
        when(groups.updateGroup(anyMap())).thenReturn(1);
        when(groups.activeUser(5L)).thenReturn(1);
        when(groups.activeShop(10L)).thenReturn(1);
        when(groups.shops(3L)).thenReturn(List.of(10L,11L));
        when(groups.groupForShop(10L)).thenReturn(3L);
        when(groups.members(3L)).thenReturn(List.of(Map.of("userId",5L,"roleCode","customer_service")));
        service.save(Map.of("groupId",3L,"groupName","客服组","shopIds",List.of(10L),
            "operatorIds",List.of(),"customerAdminIds",List.of(),"customerServiceIds",List.of(5L)),"admin");
        verify(groups).unlinkShop(11L);
        verify(gifts).deleteSubjectMappings("SHOP",10L);
        verify(gifts).insertSubjectMapping("SHOP",10L,5L,"customer_service","admin");
        verify(gifts,never()).deleteSubjectMappings("SHOP",11L);
    }

    @Test void cannotEditInheritedShopAsIndividual() {
        when(groups.groupForShop(10L)).thenReturn(3L);
        assertEquals(3L,service.groupForShop(10L));
    }
}
