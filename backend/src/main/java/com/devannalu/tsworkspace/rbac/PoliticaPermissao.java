package com.devannalu.tsworkspace.rbac;

import java.util.Map;
import java.util.Set;

public final class PoliticaPermissao {
    private PoliticaPermissao() { }

    public record ContextoPermissao(boolean usuarioAtivo, String chavePerfilAcesso, Set<String> catalogo, Set<String> concessoes,
                          Map<String, EfeitoPermissao> excecoes) { }

    public static boolean resolverPermissao(ContextoPermissao contexto, String chavePermissao) {
        if (!contexto.usuarioAtivo() || contexto.chavePerfilAcesso() == null || !CatalogoRbac.PERFIS_ACESSO.containsKey(contexto.chavePerfilAcesso())
            || chavePermissao == null || !contexto.catalogo().contains(chavePermissao)) return false;
        // A compatibilidade exige que SUPER_ADMIN prevaleça sobre DENY individual.
        if (contexto.chavePerfilAcesso().equals("SUPER_ADMIN")) return true;
        EfeitoPermissao excecao = contexto.excecoes().get(chavePermissao);
        if (excecao != null) return excecao == EfeitoPermissao.ALLOW;
        return contexto.concessoes().contains(chavePermissao);
    }
}
