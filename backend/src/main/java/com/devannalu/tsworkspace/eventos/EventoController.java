package com.devannalu.tsworkspace.eventos;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventoController {
    private final EventoService eventos;
    public EventoController(EventoService eventos) { this.eventos=eventos; }
    public record Dados(@NotBlank @Size(max=200) String nome,@Size(max=5000) String descricao,@NotNull EventoRepository.Formato formato,
        @NotNull EventoRepository.Status status,@NotNull UUID equipeId,@NotNull LocalDateTime inicioLocal,@NotNull LocalDateTime fimLocal,
        @NotBlank @Size(max=100) String zona,@Size(max=500) String local,@Size(max=2048) String link,@Size(max=5000) String notas,
        @Size(max=50) List<@NotNull UUID> responsavelIds,@Min(0) Long versao) {
        EventoService.Dados dominio() {return new EventoService.Dados(nome,descricao,formato,status,equipeId.toString(),inicioLocal,fimLocal,zona,local,link,notas,
            responsavelIds==null?null:responsavelIds.stream().map(UUID::toString).toList());}
    }
    public record Versao(@NotNull @Min(0) Long versao) { }
    @GetMapping
    public EventoService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId,
        @RequestParam(required=false) EventoRepository.Status status,@RequestParam(required=false) String search,@RequestParam(defaultValue="false") boolean archived,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return eventos.listar(pessoa.id(),teamId==null?null:teamId.toString(),status,search,archived,page,size);
    }
    @GetMapping("/options")
    public EventoService.Opcoes opcoes(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId) {return eventos.opcoes(pessoa.id(),teamId==null?null:teamId.toString());}
    @GetMapping("/{id}")
    public EventoService.Resposta detalhe(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id) {return eventos.detalhe(pessoa.id(),id.toString());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public EventoService.Resposta criar(@AuthenticationPrincipal AppUserPrincipal pessoa,@Valid @RequestBody Dados dados) {return eventos.criar(pessoa.id(),dados.dominio());}
    @PatchMapping("/{id}")
    public EventoService.Resposta editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        if(dados.versao()==null)throw new IllegalArgumentException();return eventos.editar(pessoa.id(),id.toString(),dados.dominio(),dados.versao());
    }
    @PostMapping("/{id}/archive")
    public EventoService.Resposta arquivar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Versao dados) {return eventos.arquivar(pessoa.id(),id.toString(),dados.versao());}
}
