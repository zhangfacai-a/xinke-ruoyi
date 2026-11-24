package com.xinke.web.core.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.xinke.erp.service.LiveAccessChangedEvent;
import com.xinke.framework.web.service.SysPermissionService;
import com.xinke.framework.web.service.TokenService;
import com.xinke.system.service.ISysUserService;
import com.xinke.common.core.domain.model.UserManagementChangedEvent;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

@Component
public class LiveAccessRefreshListener {
    private final TokenService tokens;
    private final SysPermissionService permissions;
    private final ISysUserService users;

    public LiveAccessRefreshListener(TokenService tokens,SysPermissionService permissions,ISysUserService users) {
        this.tokens=tokens;this.permissions=permissions;this.users=users;
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void refresh(LiveAccessChangedEvent event) {
        tokens.refreshPermissionsByUserIds(event.userIds(),permissions,users);
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT, fallbackExecution=true)
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public void refreshAccount(UserManagementChangedEvent event) {
        tokens.refreshPermissionsByUserIds(event.userIds(),permissions,users);
    }
}
