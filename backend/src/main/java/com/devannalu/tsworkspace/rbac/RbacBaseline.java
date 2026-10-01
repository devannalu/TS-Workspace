package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Map;

/** Frozen baseline: fabb67294509f89005c9bae0d7713b077f314691. */
public final class RbacBaseline {
    private RbacBaseline() { }
    public static final List<String> PERMISSIONS = List.of(
        "users.view", "users.create", "users.edit", "users.disable", "users.manage",
        "teams.view", "teams.create", "teams.edit", "teams.archive", "teams.manage_members",
        "permissions.view", "permissions.manage", "settings.view", "audit.view"
    );
    public static final Map<String, String> ROLES = Map.of(
        "SUPER_ADMIN", "Super Admin", "ADMIN", "Admin", "SUPERVISOR", "Supervisora", "SUPPORT", "Suporte"
    );
    public static final Map<String, List<String>> GRANTS = Map.of(
        "SUPER_ADMIN", PERMISSIONS,
        "ADMIN", PERMISSIONS.stream().filter(key -> !key.equals("permissions.manage")).toList(),
        "SUPERVISOR", List.of("users.view", "teams.view", "settings.view"),
        "SUPPORT", List.of("teams.view", "settings.view")
    );
}
