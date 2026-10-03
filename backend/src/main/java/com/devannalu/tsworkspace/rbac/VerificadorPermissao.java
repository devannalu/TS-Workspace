package com.devannalu.tsworkspace.rbac;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("verificadorPermissao")
public class VerificadorPermissao {
    private final PermissaoService permissoes;
    public VerificadorPermissao(PermissaoService permissoes) { this.permissoes = permissoes; }

    public boolean possuiPermissao(Authentication authentication, String chavePermissao) {
        return authentication != null && authentication.isAuthenticated()
            && authentication.getPrincipal() instanceof AppUserPrincipal principal
            && permissoes.possuiPermissao(principal.id(), chavePermissao);
    }
}
