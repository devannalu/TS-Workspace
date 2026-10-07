package com.devannalu.tsworkspace.notificacoes;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificacaoController {
    private final NotificacaoService notificacoes;
    public NotificacaoController(NotificacaoService notificacoes) { this.notificacoes=notificacoes; }
    @GetMapping public NotificacaoService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return notificacoes.listar(pessoa.id(),page,size); }
    @PostMapping("/{id}/read") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void ler(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id) { notificacoes.marcarLida(pessoa.id(),id.toString()); }
    @PostMapping("/read-all") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void lerTodas(@AuthenticationPrincipal AppUserPrincipal pessoa) { notificacoes.marcarLida(pessoa.id(),null); }
}
