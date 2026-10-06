package com.devannalu.tsworkspace.comentarios;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.comentarios.ComentarioRepository.Recurso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ComentarioController {
    private final ComentarioService comentarios;
    public ComentarioController(ComentarioService comentarios) { this.comentarios = comentarios; }
    public record CriarComentarioRequest(@NotBlank @Size(max=5000) String conteudo) { }
    public record AtualizarComentarioRequest(@NotBlank @Size(max=5000) String conteudo, @NotNull @Min(0) Long versao) { }
    public record RemoverComentarioRequest(@NotNull @Min(0) Long versao) { }

    @GetMapping("/tasks/{id}/comments")
    public ComentarioService.PaginaComentarios listarTarefa(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return comentarios.listar(usuario.id(), Recurso.TAREFA, id.toString(), page, size);
    }
    @PostMapping("/tasks/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    public ComentarioService.ComentarioResponse criarTarefa(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @Valid @RequestBody CriarComentarioRequest dados) {
        return comentarios.criar(usuario.id(), Recurso.TAREFA, id.toString(), dados.conteudo());
    }
    @GetMapping("/tasks/{id}/activity")
    public ComentarioService.PaginaAtividade atividadeTarefa(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return comentarios.listarAtividade(usuario.id(), Recurso.TAREFA, id.toString(), page, size);
    }

    @GetMapping("/projects/{id}/comments")
    public ComentarioService.PaginaComentarios listarProjeto(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return comentarios.listar(usuario.id(), Recurso.PROJETO, id.toString(), page, size);
    }
    @PostMapping("/projects/{id}/comments") @ResponseStatus(HttpStatus.CREATED)
    public ComentarioService.ComentarioResponse criarProjeto(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @Valid @RequestBody CriarComentarioRequest dados) {
        return comentarios.criar(usuario.id(), Recurso.PROJETO, id.toString(), dados.conteudo());
    }
    @GetMapping("/projects/{id}/activity")
    public ComentarioService.PaginaAtividade atividadeProjeto(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return comentarios.listarAtividade(usuario.id(), Recurso.PROJETO, id.toString(), page, size);
    }

    @PatchMapping("/comments/{id}")
    public ComentarioService.ComentarioResponse editar(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @Valid @RequestBody AtualizarComentarioRequest dados) {
        return comentarios.editar(usuario.id(), id.toString(), dados.conteudo(), dados.versao());
    }
    @PostMapping("/comments/{id}/remove")
    public ComentarioService.ComentarioResponse remover(@AuthenticationPrincipal AppUserPrincipal usuario,
        @PathVariable UUID id, @Valid @RequestBody RemoverComentarioRequest dados) {
        return comentarios.remover(usuario.id(), id.toString(), dados.versao());
    }
}
