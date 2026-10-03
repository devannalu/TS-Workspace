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
public class PermissaoService {
    private final ProfileRepository perfis;
    private final JdbcTemplate jdbc;

    public PermissaoService(ProfileRepository perfis, JdbcTemplate jdbc) {
        this.perfis = perfis;
        this.jdbc = jdbc;
    }

    public record PerfilAcessoResponse(String key, String name) { }
    public record PermissoesEfetivas(PerfilAcessoResponse role, PoliticaPermissao.ContextoPermissao contexto) {
        public List<String> chavesEfetivas() {
            return contexto.catalogo().stream().filter(key -> PoliticaPermissao.resolverPermissao(contexto, key)).sorted().toList();
        }
    }

    public PermissoesEfetivas buscarPermissoesUsuario(String usuarioId) {
        var perfil = perfis.findById(usuarioId).orElse(null);
        if (perfil == null || perfil.getRole() == null || perfil.getStatus() != ProfileStatus.ACTIVE) {
            return new PermissoesEfetivas(null, new PoliticaPermissao.ContextoPermissao(false, null, Set.of(), Set.of(), Map.of()));
        }
        PerfilAcesso perfilAcesso = perfil.getRole();
        Set<String> catalogo = new HashSet<>(jdbc.queryForList("SELECT permission_key FROM permissions", String.class));
        Set<String> concessoes = new HashSet<>(jdbc.queryForList("SELECT p.permission_key FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id WHERE rp.role_id=?", String.class, perfilAcesso.getId()));
        Map<String, EfeitoPermissao> excecoes = new HashMap<>();
        jdbc.query("SELECT p.permission_key, up.effect FROM user_permissions up JOIN permissions p ON p.id=up.permission_id WHERE up.user_id=?",
            (org.springframework.jdbc.core.RowCallbackHandler) row -> excecoes.put(row.getString(1), EfeitoPermissao.valueOf(row.getString(2))), usuarioId);
        return new PermissoesEfetivas(new PerfilAcessoResponse(perfilAcesso.getKey(), perfilAcesso.getName()),
            new PoliticaPermissao.ContextoPermissao(true, perfilAcesso.getKey(), catalogo, concessoes, excecoes));
    }

    public boolean possuiPermissao(String usuarioId, String chavePermissao) {
        return PoliticaPermissao.resolverPermissao(buscarPermissoesUsuario(usuarioId).contexto(), chavePermissao);
    }

    public void exigirPermissao(String usuarioId, String chavePermissao) {
        if (!possuiPermissao(usuarioId, chavePermissao)) throw new AccessDeniedException("Acesso negado.");
    }
}
