package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.rbac.V5__provision_rbac;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
class AtualizacaoRbacTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_rbac_upgrade_test").withLabel("com.tsworkspace.purpose", "rbac-upgrade-test");

    @Test void deveAtualizarIdentidadeAposVerificacaoExplicitaPreservandoCredenciais() throws Exception {
        var source = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).target("3").load().migrate();
        String id = UUID.randomUUID().toString();
        String password = "upgrade test password only";
        String hash = new BCryptPasswordEncoder(12).encode(password);
        jdbc.update("INSERT INTO app_user VALUES (?, 'Phase One Test', 'upgrade@example.test', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", id, hash);
        jdbc.update("INSERT INTO app_profile VALUES (?, NULL, 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", id);
        Flyway.configure().dataSource(source).target("4").load().migrate();
        try (var connection = source.getConnection()) {
            Context context = new Context() {
                public Configuration getConfiguration() { return Flyway.configure().dataSource(source); }
                public java.sql.Connection getConnection() { return connection; }
            };
            assertThatThrownBy(() -> new V5__provision_rbac(false, "", "").migrate(context)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> new V5__provision_rbac(true, "someone-else@example.test", password).migrate(context)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> new V5__provision_rbac(true, "upgrade@example.test", "incorrect test password").migrate(context)).isInstanceOf(IllegalStateException.class);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class)).isZero();
            String partial = UUID.randomUUID().toString();
            jdbc.update("INSERT INTO app_user VALUES (?, 'Partial Test', 'partial@example.test', ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))", partial, hash);
            assertThatThrownBy(() -> new V5__provision_rbac(true, "upgrade@example.test", password).migrate(context)).isInstanceOf(IllegalStateException.class);
            jdbc.update("DELETE FROM app_user WHERE id=?", partial);
        }
        Flyway upgrade = Flyway.configure().dataSource(source)
            .javaMigrations(new V5__provision_rbac(true, "upgrade@example.test", password)).load();
        assertThat(upgrade.migrate().migrationsExecuted).isEqualTo(3);
        assertThat(upgrade.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM app_user", String.class)).isEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT email FROM app_user", String.class)).isEqualTo("upgrade@example.test");
        assertThat(hash.equals(jdbc.queryForObject("SELECT password_hash FROM app_user", String.class))).isTrue();
        assertThat(jdbc.queryForObject("SELECT r.role_key FROM app_profile p JOIN roles r ON r.id=p.role_id", String.class)).isEqualTo("SUPER_ADMIN");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM team", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM team_member tm JOIN team t ON t.id=tm.team_id WHERE tm.user_id=? AND t.team_key='fundadoras'", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT IS_NULLABLE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='app_profile' AND COLUMN_NAME='role_id'", String.class)).isEqualTo("NO");
    }
}
