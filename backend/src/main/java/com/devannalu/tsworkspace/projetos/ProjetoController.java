package com.devannalu.tsworkspace.projetos;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/projects")
public class ProjetoController {
    private final ProjetoService projetos;
    public ProjetoController(ProjetoService projetos){this.projetos=projetos;}
    public record CriarProjetoRequest(@NotBlank @Size(max=200) String titulo,@Size(max=5000) String descricao,
        @NotNull UUID equipeId,@Size(max=50) List<@NotNull UUID> responsavelIds,LocalDate dataInicio,LocalDate dataFim) { }
    public record AtualizarProjetoRequest(@NotBlank @Size(max=200) String titulo,@Size(max=5000) String descricao,
        @NotNull Projeto.Status status,@NotNull UUID equipeId,@Size(max=50) List<@NotNull UUID> responsavelIds,
        LocalDate dataInicio,LocalDate dataFim,@NotNull @Min(0) Long versao) { }
    public record ArquivarProjetoRequest(@NotNull @Min(0) Long versao) { }
    private static String id(UUID id){return id==null?null:id.toString();}
    private static List<String> ids(List<UUID> ids){return ids==null?null:ids.stream().map(UUID::toString).toList();}
    @GetMapping
    public ProjetoService.PaginaProjetos listar(@AuthenticationPrincipal AppUserPrincipal usuario,
        @RequestParam(required=false) UUID teamId,@RequestParam(required=false) Projeto.Status status,
        @RequestParam(required=false) UUID responsibleId,@RequestParam(required=false) String search,
        @RequestParam(required=false) LocalDate startFrom,@RequestParam(required=false) LocalDate startTo,
        @RequestParam(required=false) LocalDate dueFrom,@RequestParam(required=false) LocalDate dueTo,
        @RequestParam(defaultValue="false") boolean archived,@RequestParam(defaultValue="0") int page,
        @RequestParam(defaultValue="24") int size) {
        return projetos.listarProjetos(usuario.id(),new ProjetoService.FiltrosProjetos(id(teamId),status,id(responsibleId),search,
            startFrom,startTo,dueFrom,dueTo,archived,page,size));
    }
    @GetMapping("/{id}")
    public ProjetoService.ProjetoResponse detalhe(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id){return projetos.buscarProjeto(usuario.id(),id.toString());}
    @GetMapping("/options")
    public ProjetoService.OpcoesProjetos opcoes(@AuthenticationPrincipal AppUserPrincipal usuario,@RequestParam(required=false) UUID teamId){return projetos.buscarOpcoes(usuario.id(),id(teamId));}
    @GetMapping("/summary")
    public ProjetoService.ResumoProjetos resumo(@AuthenticationPrincipal AppUserPrincipal usuario){return projetos.resumirProjetos(usuario.id());}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProjetoService.ProjetoResponse criar(@AuthenticationPrincipal AppUserPrincipal usuario,@Valid @RequestBody CriarProjetoRequest d){
        return projetos.criarProjeto(usuario.id(),d.titulo(),d.descricao(),id(d.equipeId()),ids(d.responsavelIds()),d.dataInicio(),d.dataFim());
    }
    @PatchMapping("/{id}")
    public ProjetoService.ProjetoResponse editar(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,@Valid @RequestBody AtualizarProjetoRequest d){
        return projetos.editarProjeto(usuario.id(),id.toString(),d.titulo(),d.descricao(),d.status(),id(d.equipeId()),ids(d.responsavelIds()),d.dataInicio(),d.dataFim(),d.versao());
    }
    @PostMapping("/{id}/archive")
    public ProjetoService.ProjetoResponse arquivar(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,@Valid @RequestBody ArquivarProjetoRequest d){
        return projetos.arquivarProjeto(usuario.id(),id.toString(),d.versao());
    }
}
