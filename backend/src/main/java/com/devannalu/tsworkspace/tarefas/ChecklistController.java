package com.devannalu.tsworkspace.tarefas;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks/{tarefa}/checklist")
public class ChecklistController {
    private final ChecklistService checklist;
    public ChecklistController(ChecklistService checklist) { this.checklist=checklist; }
    public record Criar(@NotBlank @Size(max=500) String texto) { }
    public record Editar(@NotBlank @Size(max=500) String texto,@NotNull Boolean concluido,@NotNull @Min(0) Long versao) { }
    public record Versao(@NotNull @Min(0) Long versao) { }
    public record Posicao(@NotNull UUID id,@NotNull @Min(0) Long versao) { }
    public record Ordenar(@NotNull @Size(max=200) List<@Valid @NotNull Posicao> items) { }
    @GetMapping
    public ChecklistService.Lista listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID tarefa) {
        return checklist.listar(pessoa.id(),tarefa.toString());
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ChecklistService.Lista criar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID tarefa,@Valid @RequestBody Criar dados) {
        return checklist.criar(pessoa.id(),tarefa.toString(),dados.texto());
    }
    @PatchMapping("/{item}")
    public ChecklistService.Lista editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID tarefa,@PathVariable UUID item,@Valid @RequestBody Editar dados) {
        return checklist.editar(pessoa.id(),tarefa.toString(),item.toString(),dados.texto(),dados.concluido(),dados.versao());
    }
    @PostMapping("/{item}/remove")
    public ChecklistService.Lista remover(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID tarefa,@PathVariable UUID item,@Valid @RequestBody Versao dados) {
        return checklist.remover(pessoa.id(),tarefa.toString(),item.toString(),dados.versao());
    }
    @PatchMapping("/order")
    public ChecklistService.Lista ordenar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID tarefa,@Valid @RequestBody Ordenar dados) {
        return checklist.ordenar(pessoa.id(),tarefa.toString(),dados.items().stream().map(p->new ChecklistService.Posicao(p.id().toString(),p.versao())).toList());
    }
}
