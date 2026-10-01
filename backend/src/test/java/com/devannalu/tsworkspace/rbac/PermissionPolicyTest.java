package com.devannalu.tsworkspace.rbac;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PermissionPolicyTest {
    private PermissionPolicy.Context context(boolean active, String role, Map<String, PermissionEffect> overrides) {
        return new PermissionPolicy.Context(active, role, Set.of("teams.view", "users.view"), Set.of("teams.view"), overrides);
    }

    @Test void inheritsRoleGrant() {
        assertThat(PermissionPolicy.resolve(context(true, "SUPPORT", Map.of()), "teams.view")).isTrue();
        assertThat(PermissionPolicy.resolve(context(true, "SUPPORT", Map.of()), "users.view")).isFalse();
    }
    @Test void allowGrantsMissingRolePermission() {
        assertThat(PermissionPolicy.resolve(context(true, "SUPPORT", Map.of("users.view", PermissionEffect.ALLOW)), "users.view")).isTrue();
    }
    @Test void denyOverridesRoleGrant() {
        assertThat(PermissionPolicy.resolve(context(true, "SUPPORT", Map.of("teams.view", PermissionEffect.DENY)), "teams.view")).isFalse();
    }
    @Test void superAdminBypassesDenyExactlyAsLegacyPolicy() {
        assertThat(PermissionPolicy.resolve(context(true, "SUPER_ADMIN", Map.of("teams.view", PermissionEffect.DENY)), "teams.view")).isTrue();
    }
    @Test void inactiveBlocksEvenSuperAdminAndAllow() {
        assertThat(PermissionPolicy.resolve(context(false, "SUPER_ADMIN", Map.of("users.view", PermissionEffect.ALLOW)), "users.view")).isFalse();
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"UNKNOWN"})
    void missingOrInvalidRoleFailsClosed(String role) {
        assertThat(PermissionPolicy.resolve(context(true, role, Map.of("users.view", PermissionEffect.ALLOW)), "users.view")).isFalse();
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"unknown.permission"})
    void unknownPermissionIsDeniedEvenForSuperAdmin(String key) {
        assertThat(PermissionPolicy.resolve(context(true, "SUPER_ADMIN", Map.of()), key)).isFalse();
    }
}
