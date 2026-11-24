package com.xinke.erp.service;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.xinke.erp.mapper.ShopStaffGroupMapper;
import static org.mockito.Mockito.*;

class LiveAutoAccessServiceTest {
    @Test void loggedInBaseRoleDoesNotRequireShopAssignmentAndIsRevokedWhenDisabled() {
        ShopStaffGroupMapper mapper=mock(ShopStaffGroupMapper.class);
        ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
        when(mapper.authenticatedRole()).thenReturn(12L);
        when(mapper.activeUser(7L)).thenReturn(1);
        new LiveAutoAccessService(mapper,events).reconcile(Set.of(7L));
        verify(mapper).grant(7L,12L);
        when(mapper.activeUser(7L)).thenReturn(0);
        when(mapper.hasRole(7L,12L)).thenReturn(1);
        new LiveAutoAccessService(mapper,events).reconcile(Set.of(7L));
        verify(mapper).revoke(7L,12L);
    }
    private final ShopStaffGroupMapper mapper=mock(ShopStaffGroupMapper.class);
    private final ApplicationEventPublisher events=mock(ApplicationEventPublisher.class);
    private final LiveAutoAccessService service=new LiveAutoAccessService(mapper,events);

    @Test void grantsRoleWhenUserHasAnyEligibleShop() {
        when(mapper.managedRoles()).thenReturn(List.of(20L));
        when(mapper.activeUser(7L)).thenReturn(1);
        when(mapper.eligible(7L,20L)).thenReturn(2);
        service.reconcile(Set.of(7L));
        verify(mapper).grant(7L,20L);
        verify(events).publishEvent(new LiveAccessChangedEvent(Set.of(7L)));
    }

    @Test void revokesOnlyManagedRoleAfterLastAssignmentIsRemoved() {
        when(mapper.managedRoles()).thenReturn(List.of(20L));
        when(mapper.activeUser(7L)).thenReturn(1);
        when(mapper.hasRole(7L,20L)).thenReturn(1);
        service.reconcile(Set.of(7L));
        verify(mapper).revoke(7L,20L);
        verify(mapper,never()).grant(anyLong(),anyLong());
    }

    @Test void keepsRoleWhileAnotherShopStillQualifies() {
        when(mapper.managedRoles()).thenReturn(List.of(20L));
        when(mapper.activeUser(7L)).thenReturn(1);
        when(mapper.eligible(7L,20L)).thenReturn(1);
        when(mapper.hasRole(7L,20L)).thenReturn(1);
        service.reconcile(Set.of(7L));
        verify(mapper,never()).revoke(anyLong(),anyLong());
        verifyNoInteractions(events);
    }
}
