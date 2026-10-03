package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Map;

// Catálogo congelado no checkpoint fabb672 para manter a compatibilidade.
public final class CatalogoRbac {
    private CatalogoRbac() { }
    public static final List<String> PERMISSOES = List.of(
        "users.view", "users.create", "users.edit", "users.disable", "users.manage",
        "teams.view", "teams.create", "teams.edit", "teams.archive", "teams.manage_members",
        "permissions.view", "permissions.manage", "settings.view", "audit.view"
    );
    public static final Map<String, String> PERFIS_ACESSO = Map.of(
        "SUPER_ADMIN", "Super Admin", "ADMIN", "Admin", "SUPERVISOR", "Supervisora", "SUPPORT", "Suporte"
    );
    public static final Map<String, List<String>> CONCESSOES = Map.of(
        "SUPER_ADMIN", PERMISSOES,
        "ADMIN", PERMISSOES.stream().filter(key -> !key.equals("permissions.manage")).toList(),
        "SUPERVISOR", List.of("users.view", "teams.view", "settings.view"),
        "SUPPORT", List.of("teams.view", "settings.view")
    );
}
