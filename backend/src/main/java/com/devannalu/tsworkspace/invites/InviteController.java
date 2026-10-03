package com.devannalu.tsworkspace.invites;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.auth.EmailNormalizer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/invites")
public class InviteController {
    private final InviteService invites;
    public InviteController(InviteService invites){this.invites=invites;}
    public record Create(@NotBlank @Email @Size(max=320) String email,@NotNull UUID roleId,@NotNull @Size(min=1,max=20) List<@NotNull UUID> teamIds,@Min(1) @Max(30) Integer expiresInDays) {
        public Create { email=email==null?null:EmailNormalizer.normalize(email); expiresInDays=expiresInDays==null?InvitePolicy.TTL_DAYS:expiresInDays; }
    }
    // Tokens ficam no corpo para não aparecerem nos logs de URLs da API.
    public record Token(@NotBlank @Size(max=64) String token) { }
    public record Accept(@NotBlank @Size(max=64) String token,@NotBlank @Size(min=2,max=100) String name,@NotBlank @Size(min=12,max=128) String password,@NotBlank @Size(max=128) String passwordConfirmation) { }
    @GetMapping @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public InviteService.Page list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return invites.list(page,size);}
    @GetMapping("/pending-count") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public long pendingCount() { return invites.pendingCount(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.create')")
    public InviteService.Created create(Authentication auth,@Valid @RequestBody Create input){return invites.create(((AppUserPrincipal)auth.getPrincipal()).id(),input.email(),input.roleId().toString(),input.teamIds().stream().map(UUID::toString).toList(),input.expiresInDays());}
    @PostMapping("/{id}/cancel") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.create')")
    public InviteService.InviteDto cancel(Authentication auth,@PathVariable UUID id){return invites.cancel(((AppUserPrincipal)auth.getPrincipal()).id(),id.toString());}
    @PostMapping("/validate")
    public InviteService.PublicInvite inspect(@Valid @RequestBody Token input){return invites.inspect(input.token());}
    @PostMapping("/accept") @ResponseStatus(HttpStatus.CREATED)
    public InviteService.Accepted accept(@Valid @RequestBody Accept input){return invites.accept(input.token(),input.name(),input.password(),input.passwordConfirmation());}
}
