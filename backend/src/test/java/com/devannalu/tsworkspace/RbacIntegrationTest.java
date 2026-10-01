package com.devannalu.tsworkspace;

import com.devannalu.tsworkspace.auth.*;
import com.devannalu.tsworkspace.rbac.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.util.HashSet;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class RbacIntegrationTest {
    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4.11")
        .withDatabaseName("ts_workspace_rbac_test").withLabel("com.tsworkspace.purpose", "rbac-test");
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired RbacSeed seed;
    @Autowired BootstrapService bootstrap;
    @Autowired UserRepository users;
    @Autowired PermissionService permissions;
    @Autowired PermissionController controller;
    @Autowired AppUserDetailsService details;
    @Autowired Flyway flyway;
    private static final String TEST_PASSWORD = "RBAC test password only 2026";

    @BeforeEach void reset() {
        jdbc.update("DELETE FROM SPRING_SESSION_ATTRIBUTES");
        jdbc.update("DELETE FROM SPRING_SESSION");
        jdbc.update("DELETE FROM user_permissions");
        jdbc.update("DELETE FROM app_profile");
        jdbc.update("DELETE FROM app_user");
        jdbc.update("DELETE FROM role_permissions");
        seed.seed();
    }

    @Test void migrationsSeedAndActualDatabaseMatrixMatchFrozenBaseline() throws Exception {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("5");
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions", Integer.class)).isEqualTo(32);
        try (var input = getClass().getResourceAsStream("/rbac-baseline.json")) {
            var baseline = mapper.readTree(input);
            var expectedCatalog = new HashSet<String>();
            baseline.get("permissions").forEach(p -> expectedCatalog.add(p.asText()));
            assertThat(jdbc.queryForList("SELECT permission_key FROM permissions", String.class)).containsExactlyInAnyOrderElementsOf(expectedCatalog);
            var entries = baseline.get("roles").fields();
            while (entries.hasNext()) {
                var entry = entries.next();
                var expectedGrants = new HashSet<String>();
                entry.getValue().forEach(p -> expectedGrants.add(p.asText()));
                assertThat(jdbc.queryForList("SELECT p.permission_key FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id JOIN roles r ON r.id=rp.role_id WHERE r.role_key=?", String.class, entry.getKey())).containsExactlyInAnyOrderElementsOf(expectedGrants);
                String id = createUser(entry.getKey());
                for (String key : expectedCatalog) assertThat(permissions.hasPermission(id, key)).as(entry.getKey()+": "+key).isEqualTo(expectedGrants.contains(key));
            }
        }
    }

    @Test void seedIsIdempotentAndPreservesAdministrativeDecisions() {
        String id = createUser("SUPPORT");
        override(id, "teams.view", "DENY");
        jdbc.update("INSERT INTO role_permissions SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.role_key='SUPPORT' AND p.permission_key='audit.view'");
        seed.seed(); seed.seed();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM roles", Integer.class)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM permissions", Integer.class)).isEqualTo(14);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM role_permissions", Integer.class)).isEqualTo(33);
        assertThat(permissions.hasPermission(id, "audit.view")).isTrue();
        assertThat(permissions.hasPermission(id, "teams.view")).isFalse();
    }

    @Test void endpointUses401ForAnonymousAnd403ForInsufficientPermission() throws Exception {
        mvc.perform(get("/api/v1/permissions")).andExpect(status().isUnauthorized());
        String id = createUser("SUPPORT");
        mvc.perform(get("/api/v1/permissions").cookie(login(id))).andExpect(status().isForbidden());
    }
    @Test void adminCanListCatalogAndMeExposesOnlyEffectiveKeys() throws Exception {
        String id = createUser("ADMIN");
        Cookie session = login(id);
        mvc.perform(get("/api/v1/permissions").cookie(session)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(14));
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.role.key").value("ADMIN"))
            .andExpect(jsonPath("$.permissions.length()").value(13))
            .andExpect(jsonPath("$.passwordHash").doesNotExist()).andExpect(jsonPath("$.sessionId").doesNotExist());
    }
    @Test void allowAndDenyTakeEffectImmediatelyInExistingSession() throws Exception {
        String id = createUser("SUPPORT");
        Cookie session = login(id);
        override(id, "permissions.view", "ALLOW");
        mvc.perform(get("/api/v1/permissions").cookie(session)).andExpect(status().isOk());
        jdbc.update("UPDATE user_permissions SET effect='DENY' WHERE user_id=?", id);
        mvc.perform(get("/api/v1/permissions").cookie(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(jsonPath("$.permissions", org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("permissions.view"))));
    }
    @Test void denyOverridesAdminRoleGrant() throws Exception {
        String id = createUser("ADMIN");
        override(id, "permissions.view", "DENY");
        mvc.perform(get("/api/v1/permissions").cookie(login(id))).andExpect(status().isForbidden());
        assertThatThrownBy(() -> permissions.requirePermission(id, "permissions.view")).isInstanceOf(AccessDeniedException.class);
    }
    @Test void superAdminBypassesDenyWithoutRoleGrantButNotUnknownPermissions() throws Exception {
        String id = createUser("SUPER_ADMIN");
        override(id, "permissions.view", "DENY");
        jdbc.update("DELETE rp FROM role_permissions rp JOIN roles r ON r.id=rp.role_id WHERE r.role_key='SUPER_ADMIN'");
        mvc.perform(get("/api/v1/permissions").cookie(login(id))).andExpect(status().isOk());
        assertThat(permissions.hasPermission(id, "unknown.permission")).isFalse();
    }
    @Test void changedRoleIsReadFromDatabaseInsteadOfStalePrincipal() throws Exception {
        String id = createUser("ADMIN");
        Cookie session = login(id);
        jdbc.update("UPDATE app_profile SET role_id=(SELECT id FROM roles WHERE role_key='SUPPORT') WHERE user_id=?", id);
        mvc.perform(get("/api/v1/permissions").cookie(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(jsonPath("$.role.key").value("SUPPORT"));
    }
    @Test void inactiveSuperAdminIsBlockedAndSessionRevoked() throws Exception {
        String id = createUser("SUPER_ADMIN");
        Cookie session = login(id);
        jdbc.update("UPDATE app_profile SET status='INACTIVE' WHERE user_id=?", id);
        mvc.perform(get("/api/v1/permissions").cookie(session)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/auth/me").cookie(session)).andExpect(status().isUnauthorized());
        assertThat(permissions.hasPermission(id, "permissions.view")).isFalse();
    }
    @Test void databaseEnforcesOverrideUniquenessRoleNotNullAndForeignKeys() {
        String id = createUser("SUPPORT");
        override(id, "teams.view", "ALLOW");
        assertThatThrownBy(() -> override(id, "teams.view", "DENY")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE app_profile SET role_id=NULL WHERE user_id=?", id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE app_profile SET role_id=? WHERE user_id=?", UUID.randomUUID().toString(), id)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> override(UUID.randomUUID().toString(), "teams.view", "ALLOW")).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE user_permissions SET effect='OTHER' WHERE user_id=?", id))
            .isInstanceOf(org.springframework.jdbc.UncategorizedSQLException.class)
            .satisfies(error -> assertThat(((org.springframework.jdbc.UncategorizedSQLException) error)
                .getSQLException().getErrorCode()).isEqualTo(3819));
    }
    @Test void bootstrapIsIdempotentWithoutChangingIdentityCredentialsAndRejectsPartialRole() {
        bootstrap.createFirstIdentity("Test Bootstrap", "bootstrap-rbac@example.test", TEST_PASSWORD);
        User user = users.findByEmail("bootstrap-rbac@example.test").orElseThrow();
        String originalHash = user.getPasswordHash();
        bootstrap.createFirstIdentity("Ignored Name", "bootstrap-rbac@example.test", "different test password");
        User after = users.findByEmail("bootstrap-rbac@example.test").orElseThrow();
        assertThat(after.getId()).isEqualTo(user.getId());
        assertThat(after.getName()).isEqualTo("Test Bootstrap");
        assertThat(after.getPasswordHash().equals(originalHash)).isTrue();
        assertThat(permissions.getUserPermissions(user.getId()).role().key()).isEqualTo("SUPER_ADMIN");
        jdbc.update("UPDATE app_profile SET role_id=(SELECT id FROM roles WHERE role_key='SUPPORT') WHERE user_id=?", user.getId());
        assertThatThrownBy(() -> bootstrap.createFirstIdentity("Test", "bootstrap-rbac@example.test", TEST_PASSWORD)).isInstanceOf(IllegalStateException.class);
    }
    @Test void methodSecurityProtectsDirectBeanInvocation() {
        String id = createUser("SUPPORT");
        var principal = details.loadUserByUsername(users.findById(id).orElseThrow().getEmail());
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
        try { assertThatThrownBy(() -> controller.list()).isInstanceOf(AccessDeniedException.class); }
        finally { SecurityContextHolder.clearContext(); }
    }

    private String createUser(String role) {
        String email = UUID.randomUUID()+"@example.test";
        bootstrap.createFirstIdentity("RBAC Test", email, TEST_PASSWORD);
        String id = users.findByEmail(email).orElseThrow().getId();
        jdbc.update("UPDATE app_profile SET role_id=(SELECT id FROM roles WHERE role_key=?) WHERE user_id=?", role, id);
        return id;
    }
    private void override(String id, String key, String effect) {
        jdbc.update("INSERT INTO user_permissions (user_id, permission_id, effect) SELECT ?, id, ? FROM permissions WHERE permission_key=?", id, effect, key);
    }
    private Cookie login(String id) throws Exception {
        MvcResult csrf = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token = mapper.readTree(csrf.getResponse().getContentAsString()).get("token").asText();
        String email = users.findById(id).orElseThrow().getEmail();
        MvcResult login = mvc.perform(post("/api/v1/auth/login").cookie(csrf.getResponse().getCookie("XSRF-TOKEN"))
            .header("X-XSRF-TOKEN", token).contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(java.util.Map.of("email", email, "password", TEST_PASSWORD))))
            .andExpect(status().isOk()).andReturn();
        return login.getResponse().getCookie("TS_SESSION");
    }
}
