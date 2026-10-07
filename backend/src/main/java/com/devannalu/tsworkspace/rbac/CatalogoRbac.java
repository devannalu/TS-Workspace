package com.devannalu.tsworkspace.rbac;

import java.util.List;
import java.util.Map;

// Novas permissões ampliam o catálogo sem substituir os grants anteriores.
public final class CatalogoRbac {
    private CatalogoRbac() { }
    public static final List<String> PERMISSOES = List.of(
        "users.view", "users.create", "users.edit", "users.disable", "users.manage",
        "teams.view", "teams.create", "teams.edit", "teams.archive", "teams.manage_members",
        "permissions.view", "permissions.manage", "settings.view", "audit.view",
        "tasks.view", "tasks.create", "tasks.edit", "tasks.assign", "tasks.archive",
        "projects.view", "projects.create", "projects.edit", "projects.manage_members", "projects.archive",
        "tasks.comment", "projects.comment", "comments.moderate", "tasks.attach", "projects.attach", "attachments.remove", "meetings.view", "meetings.create", "meetings.edit", "meetings.archive", "events.view", "events.create", "events.edit", "events.archive", "content.view", "content.create", "content.edit", "content.archive"
    );
    public static final Map<String, String> PERFIS_ACESSO = Map.of(
        "SUPER_ADMIN", "Super Admin", "ADMIN", "Admin", "SUPERVISOR", "Supervisora", "SUPPORT", "Suporte"
    );
    public static final Map<String, List<String>> CONCESSOES = Map.of(
        "SUPER_ADMIN", PERMISSOES,
        "ADMIN", PERMISSOES.stream().filter(key -> !key.equals("permissions.manage")).toList(),
        "SUPERVISOR", List.of("users.view", "teams.view", "settings.view", "tasks.view", "tasks.create", "tasks.edit", "tasks.assign",
            "projects.view", "projects.create", "projects.edit", "projects.manage_members", "tasks.comment", "projects.comment", "tasks.attach", "projects.attach", "meetings.view", "meetings.create", "meetings.edit", "events.view", "events.create", "events.edit", "content.view", "content.create", "content.edit"),
        "SUPPORT", List.of("teams.view", "settings.view", "tasks.view", "tasks.create", "tasks.edit", "projects.view", "tasks.comment", "tasks.attach", "meetings.view", "events.view", "content.view")
    );
}
