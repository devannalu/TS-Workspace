package com.devannalu.tsworkspace.equipes;

import jakarta.validation.Valid;
import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teams")
public class EquipeController {
    private final EquipeService teams;
    public EquipeController(EquipeService teams) { this.teams = teams; }
    public record SalvarEquipeRequest(@NotBlank @Size(min=2,max=100) String name, @Size(max=500) String description, UUID parentId) {
        public SalvarEquipeRequest { name = name == null ? null : name.trim(); description = description == null ? null : description.trim(); }
    }
    public record AdicionarIntegranteRequest(@NotNull UUID userId) { }
    @GetMapping @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.view')")
    public List<EquipeService.ResumoEquipe> listarEquipes() { return teams.listarEquipes(); }
    @GetMapping("/mine") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.view')")
    public List<EquipeService.ResumoEquipe> mine(Authentication authentication) {
        return teams.listarEquipesUsuario(((AppUserPrincipal) authentication.getPrincipal()).id());
    }
    @GetMapping("/{id}") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.view')")
    public EquipeService.DetalheEquipe buscarDetalheEquipe(@PathVariable UUID id) { return teams.buscarDetalheEquipe(id.toString()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.create')")
    public EquipeService.DetalheEquipe criarEquipe(@Valid @RequestBody SalvarEquipeRequest input) {
        return teams.criarEquipe(input.name(), input.description(), input.parentId() == null ? null : input.parentId().toString());
    }
    @PutMapping("/{id}") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.edit')")
    public EquipeService.DetalheEquipe editarEquipe(@PathVariable UUID id, @Valid @RequestBody SalvarEquipeRequest input) {
        return teams.editarEquipe(id.toString(), input.name(), input.description(), input.parentId() == null ? null : input.parentId().toString());
    }
    @PostMapping("/{id}/archive") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.archive')")
    public EquipeService.DetalheEquipe arquivarEquipe(@PathVariable UUID id) { return teams.arquivarEquipe(id.toString()); }
    @PostMapping("/{id}/members") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.manage_members')")
    public void add(@PathVariable UUID id, @Valid @RequestBody AdicionarIntegranteRequest input) { teams.adicionarIntegrante(id.toString(), input.userId().toString()); }
    @DeleteMapping("/{id}/members/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'teams.manage_members')")
    public void remove(@PathVariable UUID id, @PathVariable UUID userId) { teams.removerIntegrante(id.toString(), userId.toString()); }
}
