package com.devannalu.tsworkspace.tarefas;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tasks")
public class TarefaController {
    private final TarefaService tarefas;
    public TarefaController(TarefaService tarefas) { this.tarefas = tarefas; }

    public record CriarTarefaRequest(@NotBlank @Size(max=200) String titulo, @Size(max=5000) String descricao,
        Tarefa.Prioridade prioridade, @NotNull UUID equipeId, LocalDate prazo,
        @Size(max=50) List<@NotNull UUID> responsavelIds, UUID projetoId) { }
    public record AtualizarTarefaRequest(@NotBlank @Size(max=200) String titulo, @Size(max=5000) String descricao,
        @NotNull Tarefa.Prioridade prioridade, @NotNull UUID equipeId, LocalDate prazo,
        @Size(max=50) List<@NotNull UUID> responsavelIds, @NotNull @Min(0) Long versao, UUID projetoId) { }
    public record MoverTarefaRequest(@NotNull Tarefa.Status status, UUID antesDeId, @NotNull @Min(0) Long versao) { }
    public record ArquivarTarefaRequest(@NotNull @Min(0) Long versao) { }

    private static String id(UUID id) { return id == null ? null : id.toString(); }
    private static List<String> ids(List<UUID> ids) { return ids == null ? null : ids.stream().map(UUID::toString).toList(); }

    @GetMapping
    public TarefaService.PaginaTarefas listar(@AuthenticationPrincipal AppUserPrincipal usuario,
        @RequestParam(required=false) UUID teamId, @RequestParam(required=false) Tarefa.Status status,
        @RequestParam(required=false) Tarefa.Prioridade priority, @RequestParam(required=false) UUID assigneeId,
        @RequestParam(required=false) String search, @RequestParam(required=false) LocalDate dueFrom,
        @RequestParam(required=false) LocalDate dueTo, @RequestParam(defaultValue="false") boolean archived,
        @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size,
        @RequestParam(required=false) UUID projectId) {
        return tarefas.listarTarefas(usuario.id(), new TarefaService.FiltrosTarefas(id(teamId), status, priority,
            id(assigneeId), search, dueFrom, dueTo, archived, page, size, id(projectId)));
    }

    @GetMapping("/summary")
    public TarefaService.ResumoTarefas resumir(@AuthenticationPrincipal AppUserPrincipal usuario) {
        return tarefas.resumirTarefas(usuario.id());
    }

    @GetMapping("/options")
    public TarefaService.OpcoesTarefas opcoes(@AuthenticationPrincipal AppUserPrincipal usuario,
        @RequestParam(required=false) UUID teamId) {
        return tarefas.buscarOpcoes(usuario.id(), id(teamId));
    }

    @GetMapping("/{id}")
    public TarefaService.TarefaResponse detalhe(@AuthenticationPrincipal AppUserPrincipal usuario, @PathVariable UUID id) {
        return tarefas.buscarTarefa(usuario.id(), id.toString());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarefaService.TarefaResponse criar(@AuthenticationPrincipal AppUserPrincipal usuario, @Valid @RequestBody CriarTarefaRequest dados) {
        return tarefas.criarTarefa(usuario.id(), dados.titulo(), dados.descricao(), dados.prioridade(), id(dados.equipeId()),
            dados.prazo(), ids(dados.responsavelIds()), id(dados.projetoId()));
    }

    @PatchMapping("/{id}")
    public TarefaService.TarefaResponse editar(@AuthenticationPrincipal AppUserPrincipal usuario, @PathVariable UUID id,
        @Valid @RequestBody AtualizarTarefaRequest dados) {
        return tarefas.editarTarefa(usuario.id(), id.toString(), dados.titulo(), dados.descricao(), dados.prioridade(),
            id(dados.equipeId()), dados.prazo(), ids(dados.responsavelIds()), dados.versao(), id(dados.projetoId()));
    }

    @PatchMapping("/{id}/position")
    public TarefaService.TarefaResponse mover(@AuthenticationPrincipal AppUserPrincipal usuario, @PathVariable UUID id,
        @Valid @RequestBody MoverTarefaRequest dados) {
        return tarefas.moverTarefa(usuario.id(), id.toString(), dados.status(), id(dados.antesDeId()), dados.versao());
    }

    @PostMapping("/{id}/archive")
    public TarefaService.TarefaResponse arquivar(@AuthenticationPrincipal AppUserPrincipal usuario, @PathVariable UUID id,
        @Valid @RequestBody ArquivarTarefaRequest dados) {
        return tarefas.arquivarTarefa(usuario.id(), id.toString(), dados.versao());
    }
}
