package com.xinke.erp.service;

import java.util.HashSet;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.xinke.erp.mapper.ShopStaffGroupMapper;

@Service
public class LiveAutoAccessService {
    private final ShopStaffGroupMapper mapper;
    private final ApplicationEventPublisher events;

    public LiveAutoAccessService(ShopStaffGroupMapper mapper, ApplicationEventPublisher events) {
        this.mapper=mapper;
        this.events=events;
    }

    @Transactional
    public void reconcile(Set<Long> userIds) {
        if (userIds==null || userIds.isEmpty()) return;
        Set<Long> changed=new HashSet<>();
        for (Long userId:userIds) {
            if (userId==null) continue;
            for (Long roleId:mapper.managedRoles()) {
                boolean eligible=mapper.activeUser(userId)>0 && mapper.eligible(userId,roleId)>0;
                boolean granted=mapper.hasRole(userId,roleId)>0;
                if (eligible && !granted) { mapper.grant(userId,roleId); changed.add(userId); }
                if (!eligible && granted) { mapper.revoke(userId,roleId); changed.add(userId); }
            }
        }
        if (!changed.isEmpty()) events.publishEvent(new LiveAccessChangedEvent(Set.copyOf(changed)));
    }

    @Transactional
    public void reconcileAll() {
        Set<Long> users=new HashSet<>(mapper.allStaffUsers());
        for (Long roleId:mapper.managedRoles()) users.addAll(mapper.roleUsers(roleId));
        reconcile(users);
    }
}
