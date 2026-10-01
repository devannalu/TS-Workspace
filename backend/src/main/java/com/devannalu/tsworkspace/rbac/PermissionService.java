package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.auth.ProfileRepository;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissionService {
    private final ProfileRepository profiles;
    private final JdbcTemplate jdbc;

    public PermissionService(ProfileRepository profiles, JdbcTemplate jdbc) {
        this.profiles = profiles;
        this.jdbc = jdbc;
    }

    public record RoleResponse(String key, String name) { }
    public record Snapshot(RoleResponse role, PermissionPolicy.Context context) {
        public List<String> effectiveKeys() {
            return context.catalog().stream().filter(key -> PermissionPolicy.resolve(context, key)).sorted().toList();
        }
    }

    public Snapshot getUserPermissions(String userId) {
        var profile = profiles.findById(userId).orElse(null);
        if (profile == null || profile.getRole() == null || profile.getStatus() != ProfileStatus.ACTIVE) {
            return new Snapshot(null, new PermissionPolicy.Context(false, null, Set.of(), Set.of(), Map.of()));
        }
        Role role = profile.getRole();
        Set<String> catalog = new HashSet<>(jdbc.queryForList("SELECT permission_key FROM permissions", String.class));
        Set<String> grants = new HashSet<>(jdbc.queryForList("SELECT p.permission_key FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id WHERE rp.role_id=?", String.class, role.getId()));
        Map<String, PermissionEffect> overrides = new HashMap<>();
        jdbc.query("SELECT p.permission_key, up.effect FROM user_permissions up JOIN permissions p ON p.id=up.permission_id WHERE up.user_id=?",
            (org.springframework.jdbc.core.RowCallbackHandler) row -> overrides.put(row.getString(1), PermissionEffect.valueOf(row.getString(2))), userId);
        return new Snapshot(new RoleResponse(role.getKey(), role.getName()),
            new PermissionPolicy.Context(true, role.getKey(), catalog, grants, overrides));
    }

    public boolean hasPermission(String userId, String permission) {
        return PermissionPolicy.resolve(getUserPermissions(userId).context(), permission);
    }

    public void requirePermission(String userId, String permission) {
        if (!hasPermission(userId, permission)) throw new AccessDeniedException("Acesso negado.");
    }
}
