package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.usuarios.PerfilRepository;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PermissaoService {
    private final PerfilRepository perfis;
    private final PermissoesUsuarioRepository permissoesUsuario;

    public PermissaoService(PerfilRepository perfis, PermissoesUsuarioRepository permissoesUsuario) {
        this.perfis = perfis;
        this.permissoesUsuario = permissoesUsuario;
    }

    public record PerfilAcessoResponse(String key, String name) { }
    public record PermissoesEfetivas(PerfilAcessoResponse role, PoliticaPermissao.ContextoPermissao contexto) {
        public List<String> chavesEfetivas() {
            return contexto.catalogo().stream().filter(key -> PoliticaPermissao.resolverPermissao(contexto, key)).sorted().toList();
        }
    }

    public PermissoesEfetivas buscarPermissoesUsuario(String usuarioId) {
        var perfil = perfis.findById(usuarioId).orElse(null);
        if (perfil == null || perfil.getPerfilAcesso() == null || perfil.getStatus() != ProfileStatus.ACTIVE) {
            return new PermissoesEfetivas(null, new PoliticaPermissao.ContextoPermissao(false, null, Set.of(), Set.of(), Map.of()));
        }
        PerfilAcesso perfilAcesso = perfil.getPerfilAcesso();
        Set<String> catalogo = new HashSet<>(permissoesUsuario.listarCatalogo());
        Set<String> concessoes = new HashSet<>(permissoesUsuario.listarConcessoesPerfil(perfilAcesso.getId()));
        Map<String, EfeitoPermissao> excecoes = new HashMap<>();
        permissoesUsuario.listarExcecoesUsuario(usuarioId).forEach(
            (chave, efeito) -> excecoes.put(chave, EfeitoPermissao.valueOf(efeito))
        );
        return new PermissoesEfetivas(new PerfilAcessoResponse(perfilAcesso.getKey(), perfilAcesso.getNome()),
            new PoliticaPermissao.ContextoPermissao(true, perfilAcesso.getKey(), catalogo, concessoes, excecoes));
    }

    public boolean possuiPermissao(String usuarioId, String chavePermissao) {
        return PoliticaPermissao.resolverPermissao(buscarPermissoesUsuario(usuarioId).contexto(), chavePermissao);
    }

    public void exigirPermissao(String usuarioId, String chavePermissao) {
        if (!possuiPermissao(usuarioId, chavePermissao)) throw new AccessDeniedException("Acesso negado.");
    }
}
