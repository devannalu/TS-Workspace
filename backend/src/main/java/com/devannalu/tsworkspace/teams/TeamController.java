package com.devannalu.tsworkspace.teams;

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
public class TeamController {
    private final TeamService teams;
    public TeamController(TeamService teams) { this.teams = teams; }
    public record WriteTeam(@NotBlank @Size(min=2,max=100) String name, @Size(max=500) String description, UUID parentId) {
        public WriteTeam { name = name == null ? null : name.trim(); description = description == null ? null : description.trim(); }
    }
    public record AddMember(@NotNull UUID userId) { }
    @GetMapping @PreAuthorize("@permissionGuard.has(authentication, 'teams.view')")
    public List<TeamService.Summary> list() { return teams.list(); }
    @GetMapping("/mine") @PreAuthorize("@permissionGuard.has(authentication, 'teams.view')")
    public List<TeamService.Summary> mine(Authentication authentication) {
        return teams.memberships(((AppUserPrincipal) authentication.getPrincipal()).id());
    }
    @GetMapping("/{id}") @PreAuthorize("@permissionGuard.has(authentication, 'teams.view')")
    public TeamService.Detail detail(@PathVariable UUID id) { return teams.detail(id.toString()); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@permissionGuard.has(authentication, 'teams.create')")
    public TeamService.Detail create(@Valid @RequestBody WriteTeam input) {
        return teams.create(input.name(), input.description(), input.parentId() == null ? null : input.parentId().toString());
    }
    @PutMapping("/{id}") @PreAuthorize("@permissionGuard.has(authentication, 'teams.edit')")
    public TeamService.Detail edit(@PathVariable UUID id, @Valid @RequestBody WriteTeam input) {
        return teams.edit(id.toString(), input.name(), input.description(), input.parentId() == null ? null : input.parentId().toString());
    }
    @PostMapping("/{id}/archive") @PreAuthorize("@permissionGuard.has(authentication, 'teams.archive')")
    public TeamService.Detail archive(@PathVariable UUID id) { return teams.archive(id.toString()); }
    @PostMapping("/{id}/members") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionGuard.has(authentication, 'teams.manage_members')")
    public void add(@PathVariable UUID id, @Valid @RequestBody AddMember input) { teams.addMember(id.toString(), input.userId().toString()); }
    @DeleteMapping("/{id}/members/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@permissionGuard.has(authentication, 'teams.manage_members')")
    public void remove(@PathVariable UUID id, @PathVariable UUID userId) { teams.removeMember(id.toString(), userId.toString()); }
}
