package com.xinke.erp.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import com.xinke.common.core.domain.model.UserManagementChangedEvent;

@Component
public class StaffAccessListener {
    private final LiveAutoAccessService access;
    public StaffAccessListener(LiveAutoAccessService access) { this.access = access; }

    @TransactionalEventListener(fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void changed(UserManagementChangedEvent event) { access.reconcile(event.userIds()); }
}
