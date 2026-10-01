package com.devannalu.tsworkspace.rbac;

import java.util.Map;
import java.util.Set;

public final class PermissionPolicy {
    private PermissionPolicy() { }

    public record Context(boolean active, String role, Set<String> catalog, Set<String> grants,
                          Map<String, PermissionEffect> overrides) { }

    public static boolean resolve(Context context, String permission) {
        if (!context.active() || context.role() == null || !RbacBaseline.ROLES.containsKey(context.role())
            || permission == null || !context.catalog().contains(permission)) return false;
        // Legacy policy: SUPER_ADMIN bypasses even an individual DENY.
        if (context.role().equals("SUPER_ADMIN")) return true;
        PermissionEffect override = context.overrides().get(permission);
        if (override != null) return override == PermissionEffect.ALLOW;
        return context.grants().contains(permission);
    }
}
