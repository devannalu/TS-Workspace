package com.devannalu.tsworkspace.parcerias;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/partnerships")
public class ParceriaController {
    private final ParceriaService parcerias;
    public ParceriaController(ParceriaService parcerias) {this.parcerias=parcerias;}
    public record Dados(@NotBlank @Size(max=200) String organizacao,@Size(max=5000) String descricao,
        @NotNull ParceriaRepository.Status status,
        @NotNull UUID equipeId,UUID responsavelId,LocalDate dataProximaAcao,@Size(max=200) String contatoPrincipal,@Email @Size(max=254) String email,@Size(max=50) String telefone,@Size(max=500) String proximaAcao,@Size(max=5000) String observacoes,UUID eventoId,UUID projetoId,@Min(0) Long versao) {
        private static String id(UUID valor) {return valor==null?null:valor.toString();}
        ParceriaService.Dados dominio() {return new ParceriaService.Dados(organizacao,descricao,status,id(equipeId),id(responsavelId),dataProximaAcao,contatoPrincipal,email,telefone,proximaAcao,observacoes,id(eventoId),id(projetoId));}
    }
    public record Versao(@NotNull @Min(0) Long versao) { }
    @GetMapping
    public ParceriaService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId,
        @RequestParam(required=false) ParceriaRepository.Status status,@RequestParam(required=false) String search,@RequestParam(defaultValue="false") boolean archived,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return parcerias.listar(pessoa.id(),teamId==null?null:teamId.toString(),status,search,archived,page,size);
    }
    @GetMapping("/options")
    public ParceriaService.Opcoes opcoes(@AuthenticationPrincipal AppUserPrincipal pessoa,@RequestParam(required=false) UUID teamId) {return parcerias.opcoes(pessoa.id(),teamId==null?null:teamId.toString());}
    @GetMapping("/{id}")
    public ParceriaService.Resposta detalhe(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id) {return parcerias.detalhe(pessoa.id(),id.toString());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ParceriaService.Resposta criar(@AuthenticationPrincipal AppUserPrincipal pessoa,@Valid @RequestBody Dados dados) {return parcerias.criar(pessoa.id(),dados.dominio());}
    @PatchMapping("/{id}")
    public ParceriaService.Resposta editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        if(dados.versao()==null)throw new IllegalArgumentException();return parcerias.editar(pessoa.id(),id.toString(),dados.dominio(),dados.versao());
    }
    @PostMapping("/{id}/archive")
    public ParceriaService.Resposta arquivar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Versao dados) {return parcerias.arquivar(pessoa.id(),id.toString(),dados.versao());}
}
