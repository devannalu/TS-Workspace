package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("permissionGuard")
public class PermissionGuard {
    private final PermissionService permissions;
    public PermissionGuard(PermissionService permissions) { this.permissions = permissions; }

    public boolean has(Authentication authentication, String permission) {
        return authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof AppUserPrincipal principal
            && permissions.hasPermission(principal.id(), permission);
    }
}
