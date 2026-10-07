package com.devannalu.tsworkspace.conteudos;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/content")
public class ConteudoController {
    private final ConteudoService conteudos;
    public ConteudoController(ConteudoService conteudos) {this.conteudos=conteudos;}
    public record Dados(@NotBlank @Size(max=200) String titulo,@Size(max=5000) String briefing,
        @NotNull ConteudoRepository.Canal canal,@NotNull ConteudoRepository.Formato formato,@NotNull ConteudoRepository.Status status,
        @NotNull UUID equipeId,UUID responsavelId,LocalDate publicacaoPlanejada,UUID eventoId,UUID projetoId,@Min(0) Long versao) {
        private static String id(UUID valor) {return valor==null?null:valor.toString();}
        ConteudoService.Dados dominio() {return new ConteudoService.Dados(titulo,briefing,canal,formato,status,id(equipeId),id(responsavelId),publicacaoPlanejada,id(eventoId),id(projetoId));}
    }
    public record Versao(@NotNull @Min(0) Long versao) { }
    @GetMapping
    public ConteudoService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId,
        @RequestParam(required=false) ConteudoRepository.Status status,@RequestParam(required=false) String search,@RequestParam(defaultValue="false") boolean archived,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return conteudos.listar(pessoa.id(),teamId==null?null:teamId.toString(),status,search,archived,page,size);
    }
    @GetMapping("/options")
    public ConteudoService.Opcoes opcoes(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId) {return conteudos.opcoes(pessoa.id(),teamId==null?null:teamId.toString());}
    @GetMapping("/{id}")
    public ConteudoService.Resposta detalhe(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id) {return conteudos.detalhe(pessoa.id(),id.toString());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ConteudoService.Resposta criar(@AuthenticationPrincipal AppUserPrincipal pessoa,@Valid @RequestBody Dados dados) {return conteudos.criar(pessoa.id(),dados.dominio());}
    @PatchMapping("/{id}")
    public ConteudoService.Resposta editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        if(dados.versao()==null)throw new IllegalArgumentException();return conteudos.editar(pessoa.id(),id.toString(),dados.dominio(),dados.versao());
    }
    @PostMapping("/{id}/archive")
    public ConteudoService.Resposta arquivar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Versao dados) {return conteudos.arquivar(pessoa.id(),id.toString(),dados.versao());}
}
