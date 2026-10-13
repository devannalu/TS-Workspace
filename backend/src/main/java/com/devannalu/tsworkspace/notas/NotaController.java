package com.devannalu.tsworkspace.notas;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class NotaController {
    private final NotaService notas;
    public NotaController(NotaService notas) {this.notas=notas;}
    public record Dados(@NotBlank @Size(max=200) String titulo,@NotBlank @Size(max=5000) String texto,@Min(0) Long versao) { }
    public record Versao(@NotNull @Min(0) Long versao) { }
    @GetMapping("/{recurso:projects|events|partnerships|meetings}/{id}/notes")
    public NotaService.Pagina listar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable String recurso,@PathVariable UUID id,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return notas.listar(pessoa.id(),NotaRepository.Recurso.daRota(recurso),id.toString(),page,size);
    }
    @PostMapping("/{recurso:projects|events|partnerships|meetings}/{id}/notes")
    @ResponseStatus(HttpStatus.CREATED)
    public NotaService.Resposta criar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable String recurso,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        return notas.criar(pessoa.id(),NotaRepository.Recurso.daRota(recurso),id.toString(),dados.titulo(),dados.texto());
    }
    @PatchMapping("/notes/{id}")
    public NotaService.Resposta editar(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Dados dados) {
        if(dados.versao()==null)throw new IllegalArgumentException();
        return notas.editar(pessoa.id(),id.toString(),dados.titulo(),dados.texto(),dados.versao());
    }
    @PostMapping("/notes/{id}/remove")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remover(@AuthenticationPrincipal AppUserPrincipal pessoa,@PathVariable UUID id,@Valid @RequestBody Versao dados) {
        notas.remover(pessoa.id(),id.toString(),dados.versao());
    }
}
