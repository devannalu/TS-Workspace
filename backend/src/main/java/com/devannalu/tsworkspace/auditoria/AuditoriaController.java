package com.devannalu.tsworkspace.auditoria;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/audit")
public class AuditoriaController {
    private final AuditoriaService auditoria;
    public AuditoriaController(AuditoriaService auditoria) {this.auditoria=auditoria;}
    @GetMapping("/options")
    public AuditoriaService.Opcoes opcoes(@AuthenticationPrincipal AppUserPrincipal pessoa) {return auditoria.opcoes(pessoa.id());}
    @GetMapping
    public AuditoriaService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) String actor,
        @RequestParam(required=false) String action,@RequestParam(required=false) String resourceType,
        @RequestParam(required=false) Instant from,@RequestParam(required=false) Instant to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return auditoria.listar(pessoa.id(),actor,action,resourceType,from,to,page,size);
    }
    @GetMapping("/{id}")
    public AuditoriaRepository.Evento detalhe(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id) {return auditoria.detalhe(pessoa.id(),id.toString());}
}
