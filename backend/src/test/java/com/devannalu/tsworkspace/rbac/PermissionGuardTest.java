package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PermissionGuardTest {
    private final PermissionService service = mock(PermissionService.class);
    private final PermissionGuard guard = new PermissionGuard(service);

    @Test void rejectsMissingUnauthenticatedAndForeignPrincipal() {
        assertThat(guard.has(null, "users.view")).isFalse();
        assertThat(guard.has(UsernamePasswordAuthenticationToken.unauthenticated("client-id", ""), "users.view")).isFalse();
        assertThat(guard.has(UsernamePasswordAuthenticationToken.authenticated("client-id", "", List.of()), "users.view")).isFalse();
        verifyNoInteractions(service);
    }
    @Test void takesIdentityOnlyFromAuthenticatedPrincipal() {
        var principal = new AppUserPrincipal("session-user", "Test", "test@example.test", "", null, ProfileStatus.ACTIVE);
        when(service.hasPermission("session-user", "users.view")).thenReturn(true);
        assertThat(guard.has(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()), "users.view")).isTrue();
        verify(service).hasPermission("session-user", "users.view");
    }
}
