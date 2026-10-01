package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.auth.EmailNormalizer;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit upgrade of the sole Phase Java 1 bootstrap identity; never guesses an account. */
public class V5__provision_rbac extends BaseJavaMigration {
    private final boolean provisionExisting;
    private final String email;
    private final String password;

    public V5__provision_rbac(boolean provisionExisting, String email, String password) {
        this.provisionExisting = provisionExisting;
        this.email = email;
        this.password = password;
    }

    @Override public Integer getChecksum() { return 1; }

    @Override
    public void migrate(Context context) {
        JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(context.getConnection(), true));
        Integer users = jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class);
        Integer profiles = jdbc.queryForObject("SELECT COUNT(*) FROM app_profile", Integer.class);
        String existingId = null;
        if (users != 0 || profiles != 0) {
            if (!provisionExisting || users != 1 || profiles != 1 || email == null || email.isBlank() || password == null || password.isBlank()) {
                throw new IllegalStateException("Provisionamento RBAC requer bootstrap explícito e identidade única completa.");
            }
            var matches = jdbc.queryForList("SELECT u.id, u.password_hash FROM app_user u JOIN app_profile p ON p.user_id=u.id WHERE u.email=? AND p.status='ACTIVE' AND (p.role_id IS NULL OR p.role_id IN (SELECT id FROM roles WHERE role_key='SUPER_ADMIN'))", EmailNormalizer.normalize(email));
            if (matches.size() != 1 || !new BCryptPasswordEncoder().matches(password, (String) matches.get(0).get("password_hash"))) {
                throw new IllegalStateException("Identidade de bootstrap incompatível; provisionamento RBAC cancelado.");
            }
            existingId = (String) matches.get(0).get("id");
        }
        RbacSeed.seed(jdbc);
        if (existingId != null) {
            jdbc.update("UPDATE app_profile SET role_id=(SELECT id FROM roles WHERE role_key='SUPER_ADMIN'), updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=? AND role_id IS NULL", existingId);
        }
        jdbc.execute("ALTER TABLE app_profile MODIFY COLUMN role_id VARCHAR(36) NOT NULL");
    }
}
