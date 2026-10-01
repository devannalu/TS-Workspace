package com.devannalu.tsworkspace.rbac;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacSeed {
    private final JdbcTemplate jdbc;
    public RbacSeed(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional
    public void seed() { seed(jdbc); }

    // Also used before JPA initialization by Flyway, on Flyway's own connection.
    public static void seed(JdbcTemplate jdbc) {
        for (String key : RbacBaseline.PERMISSIONS) {
            jdbc.update("INSERT INTO permissions (id, permission_key, name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE permission_key=permission_key",
                UUID.randomUUID().toString(), key, key);
        }
        RbacBaseline.ROLES.forEach((key, name) -> {
            jdbc.update("INSERT INTO roles (id, role_key, name, created_at, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)) ON DUPLICATE KEY UPDATE role_key=role_key",
                UUID.randomUUID().toString(), key, name);
            for (String permission : RbacBaseline.GRANTS.get(key)) {
                jdbc.update("INSERT INTO role_permissions (role_id, permission_id) SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.role_key=? AND p.permission_key=? ON DUPLICATE KEY UPDATE role_id=role_id", key, permission);
            }
        });
    }
}
