package com.devannalu.tsworkspace.reunioes;

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
@RequestMapping("/api/v1/meetings")
public class ReuniaoController {
    private final ReuniaoService reunioes;
    public ReuniaoController(ReuniaoService reunioes) { this.reunioes=reunioes; }
    public record Dados(@NotBlank @Size(max=200) String titulo,@Size(max=5000) String pauta,@NotNull ReuniaoRepository.Tipo tipo,
        @NotNull ReuniaoRepository.Status status,@NotNull UUID equipeId,@NotNull LocalDateTime inicioLocal,@NotNull LocalDateTime fimLocal,
        @NotBlank @Size(max=100) String zona,@Size(max=500) String local,@Size(max=2048) String link,@Size(max=5000) String resultados,
        @Size(max=50) List<@NotNull UUID> participanteIds,@Size(max=50) List<@NotNull UUID> responsavelIds,@Min(0) Long versao) {
        ReuniaoService.Dados dominio() {return new ReuniaoService.Dados(titulo,pauta,tipo,status,equipeId.toString(),inicioLocal,fimLocal,zona,local,link,resultados,
            participanteIds==null?null:participanteIds.stream().map(UUID::toString).toList(),responsavelIds==null?null:responsavelIds.stream().map(UUID::toString).toList());}
    }
    public record Versao(@NotNull @Min(0) Long versao) { }
    @GetMapping
    public ReuniaoService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId,
        @RequestParam(required=false) ReuniaoRepository.Status status,@RequestParam(required=false) String search,@RequestParam(defaultValue="false") boolean archived,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return reunioes.listar(pessoa.id(),teamId==null?null:teamId.toString(),status,search,archived,page,size);
    }
    @GetMapping("/options")
    public ReuniaoService.Opcoes opcoes(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId) {return reunioes.opcoes(pessoa.id(),teamId==null?null:teamId.toString());}
    @GetMapping("/{id}")
    public ReuniaoService.Resposta detalhe(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id) {return reunioes.detalhe(pessoa.id(),id.toString());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ReuniaoService.Resposta criar(@AuthenticationPrincipal AppUserPrincipal pessoa,@Valid @RequestBody Dados dados) {return reunioes.criar(pessoa.id(),dados.dominio());}
    @PatchMapping("/{id}")
    public ReuniaoService.Resposta editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        if(dados.versao()==null)throw new IllegalArgumentException();return reunioes.editar(pessoa.id(),id.toString(),dados.dominio(),dados.versao());
    }
    @PostMapping("/{id}/archive")
    public ReuniaoService.Resposta arquivar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Versao dados) {return reunioes.arquivar(pessoa.id(),id.toString(),dados.versao());}
}
