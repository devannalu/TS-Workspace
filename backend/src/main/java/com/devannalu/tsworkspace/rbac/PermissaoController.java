package com.devannalu.tsworkspace.rbac;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PermissaoController {
    private final PermissaoRepository permissoes;
    public PermissaoController(PermissaoRepository permissoes) { this.permissoes = permissoes; }

    @GetMapping("/api/v1/permissions")
    @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'permissions.view')")
    public List<PermissaoResponse> listarPermissoes() {
        return permissoes.findAllByOrderByKeyAsc().stream().map(p -> new PermissaoResponse(p.getKey(), p.getName())).toList();
    }

    public record PermissaoResponse(String key, String name) { }
}
