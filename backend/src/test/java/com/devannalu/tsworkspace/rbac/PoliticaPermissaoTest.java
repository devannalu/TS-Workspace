package com.devannalu.tsworkspace.rbac;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaPermissaoTest {
    private PoliticaPermissao.ContextoPermissao context(boolean active, String role, Map<String, EfeitoPermissao> overrides) {
        return new PoliticaPermissao.ContextoPermissao(active, role, Set.of("teams.view", "users.view"), Set.of("teams.view"), overrides);
    }

    @Test void deveHerdarConcessaoDoPerfil() {
        assertThat(PoliticaPermissao.resolverPermissao(context(true, "SUPPORT", Map.of()), "teams.view")).isTrue();
        assertThat(PoliticaPermissao.resolverPermissao(context(true, "SUPPORT", Map.of()), "users.view")).isFalse();
    }
    @Test void deveAplicarConcessaoIndividual() {
        assertThat(PoliticaPermissao.resolverPermissao(context(true, "SUPPORT", Map.of("users.view", EfeitoPermissao.ALLOW)), "users.view")).isTrue();
    }
    @Test void devePriorizarNegacaoIndividual() {
        assertThat(PoliticaPermissao.resolverPermissao(context(true, "SUPPORT", Map.of("teams.view", EfeitoPermissao.DENY)), "teams.view")).isFalse();
    }
    @Test void devePreservarPrecedenciaDaSuperAdmin() {
        assertThat(PoliticaPermissao.resolverPermissao(context(true, "SUPER_ADMIN", Map.of("teams.view", EfeitoPermissao.DENY)), "teams.view")).isTrue();
    }
    @Test void deveBloquearUsuarioInativaMesmoComConcessao() {
        assertThat(PoliticaPermissao.resolverPermissao(context(false, "SUPER_ADMIN", Map.of("users.view", EfeitoPermissao.ALLOW)), "users.view")).isFalse();
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"UNKNOWN"})
    void deveNegarAcessoSemPerfilValido(String role) {
        assertThat(PoliticaPermissao.resolverPermissao(context(true, role, Map.of("users.view", EfeitoPermissao.ALLOW)), "users.view")).isFalse();
    }
    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"unknown.permission"})
    void deveNegarPermissaoDesconhecidaMesmoParaSuperAdmin(String key) {
        assertThat(PoliticaPermissao.resolverPermissao(context(true, "SUPER_ADMIN", Map.of()), key)).isFalse();
    }
}
