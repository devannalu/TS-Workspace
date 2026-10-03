package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class VerificadorPermissaoTest {
    private final PermissaoService service = mock(PermissaoService.class);
    private final VerificadorPermissao guard = new VerificadorPermissao(service);

    @Test void deveRejeitarPrincipalAusenteNaoAutenticadoOuExterno() {
        assertThat(guard.possuiPermissao(null, "users.view")).isFalse();
        assertThat(guard.possuiPermissao(UsernamePasswordAuthenticationToken.unauthenticated("client-id", ""), "users.view")).isFalse();
        assertThat(guard.possuiPermissao(UsernamePasswordAuthenticationToken.authenticated("client-id", "", List.of()), "users.view")).isFalse();
        verifyNoInteractions(service);
    }
    @Test void deveObterIdentidadeSomenteDoPrincipalAutenticado() {
        var principal = new AppUserPrincipal("session-user", "Test", "test@example.test", "", null, ProfileStatus.ACTIVE);
        when(service.possuiPermissao("session-user", "users.view")).thenReturn(true);
        assertThat(guard.possuiPermissao(UsernamePasswordAuthenticationToken.authenticated(principal, null, List.of()), "users.view")).isTrue();
        verify(service).possuiPermissao("session-user", "users.view");
    }
}
