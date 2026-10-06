package com.devannalu.tsworkspace.anexos;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.anexos.Anexo.Recurso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1")
public class AnexoController {
    private final AnexoService anexos;
    public AnexoController(AnexoService anexos) {this.anexos=anexos;}
    public record CriarUploadRequest(@NotBlank @Size(max=255) String nomeOriginal,@NotBlank String tipoMime,@Min(1) long tamanhoBytes, UUID solicitacaoId) { }

    @GetMapping("/tasks/{id}/attachments")
    public AnexoService.PaginaAnexos listarTarefa(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return anexos.listar(usuario.id(),Recurso.TAREFA,id.toString(),page,size);
    }
    @PostMapping("/tasks/{id}/attachments/upload") @ResponseStatus(HttpStatus.CREATED)
    public AnexoService.UploadResponse uploadTarefa(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,
        @Valid @RequestBody CriarUploadRequest dados) {
        return anexos.criarUpload(usuario.id(),Recurso.TAREFA,id.toString(),dados.nomeOriginal(),dados.tipoMime(),dados.tamanhoBytes(),dados.solicitacaoId());
    }
    @PostMapping("/tasks/{id}/attachments/{attachmentId}/confirm")
    public AnexoService.AnexoResponse confirmarTarefa(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,
        @PathVariable UUID attachmentId) {
        return anexos.confirmar(usuario.id(),Recurso.TAREFA,id.toString(),attachmentId.toString());
    }

    @GetMapping("/projects/{id}/attachments")
    public AnexoService.PaginaAnexos listarProjeto(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return anexos.listar(usuario.id(),Recurso.PROJETO,id.toString(),page,size);
    }
    @PostMapping("/projects/{id}/attachments/upload") @ResponseStatus(HttpStatus.CREATED)
    public AnexoService.UploadResponse uploadProjeto(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,
        @Valid @RequestBody CriarUploadRequest dados) {
        return anexos.criarUpload(usuario.id(),Recurso.PROJETO,id.toString(),dados.nomeOriginal(),dados.tipoMime(),dados.tamanhoBytes(),dados.solicitacaoId());
    }
    @PostMapping("/projects/{id}/attachments/{attachmentId}/confirm")
    public AnexoService.AnexoResponse confirmarProjeto(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id,
        @PathVariable UUID attachmentId) {
        return anexos.confirmar(usuario.id(),Recurso.PROJETO,id.toString(),attachmentId.toString());
    }

    @PostMapping("/attachments/{id}/download")
    public AnexoService.DownloadResponse download(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id) {
        return anexos.download(usuario.id(),id.toString());
    }
    @PostMapping("/attachments/{id}/remove") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal AppUserPrincipal usuario,@PathVariable UUID id) { anexos.remover(usuario.id(),id.toString()); }
}
