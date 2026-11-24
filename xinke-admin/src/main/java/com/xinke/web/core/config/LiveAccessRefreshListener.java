package com.xinke.web.core.config;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.xinke.erp.service.LiveAccessChangedEvent;
import com.xinke.framework.web.service.SysPermissionService;
import com.xinke.framework.web.service.TokenService;
import com.xinke.system.service.ISysUserService;

@Component
public class LiveAccessRefreshListener {
    private final TokenService tokens;
    private final SysPermissionService permissions;
    private final ISysUserService users;

    public LiveAccessRefreshListener(TokenService tokens,SysPermissionService permissions,ISysUserService users) {
        this.tokens=tokens;this.permissions=permissions;this.users=users;
    }

    @TransactionalEventListener(phase=TransactionPhase.AFTER_COMMIT)
    public void refresh(LiveAccessChangedEvent event) {
        for(Long userId:event.userIds()) tokens.refreshPermissionByUserId(userId,permissions,users);
    }
}
