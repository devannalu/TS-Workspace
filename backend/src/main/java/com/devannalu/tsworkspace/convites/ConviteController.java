package com.devannalu.tsworkspace.convites;

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
public class ConviteController {
    private final ConviteService invites;
    private final com.devannalu.tsworkspace.emails.ServicoEmail emails;
    public ConviteController(ConviteService invites,com.devannalu.tsworkspace.emails.ServicoEmail emails){this.invites=invites;this.emails=emails;}
    public record ConviteEnviadoResponse(ConviteService.ConviteResponse invite,String token,String inviteUrl,com.devannalu.tsworkspace.emails.ServicoEmail.Situacao envioEmail) { }
    public record CriarConviteRequest(@NotBlank @Email @Size(max=320) String email,@NotNull UUID roleId,@NotNull @Size(min=1,max=20) List<@NotNull UUID> teamIds,@Min(1) @Max(30) Integer expiresInDays) {
        public CriarConviteRequest { email=email==null?null:EmailNormalizer.normalize(email); expiresInDays=expiresInDays==null?PoliticaConvite.PRAZO_PADRAO_CONVITE_DIAS:expiresInDays; }
    }
    // Tokens ficam no corpo para não aparecerem nos logs de URLs da API.
    public record ConsultarConviteRequest(@NotBlank @Size(max=64) String token) { }
    public record AceitarConviteRequest(@NotBlank @Size(max=64) String token,@NotBlank @Size(min=2,max=100) String name,@NotBlank @Size(min=12,max=128) String password,@NotBlank @Size(max=128) String passwordConfirmation) { }
    @GetMapping @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public ConviteService.PaginaConvites listarConvites(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size){return invites.listarConvites(page,size);}
    @GetMapping("/pending-count") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public long contarConvitesPendentes() { return invites.contarConvitesPendentes(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.create')")
    public ConviteEnviadoResponse criarConvite(Authentication auth,@Valid @RequestBody CriarConviteRequest input){
        var convite=invites.criarConvite(((AppUserPrincipal)auth.getPrincipal()).id(),input.email(),input.roleId().toString(),input.teamIds().stream().map(UUID::toString).toList(),input.expiresInDays());
        return new ConviteEnviadoResponse(convite.invite(),convite.token(),convite.inviteUrl(),emails.enviarConvite(convite));
    }
    @PostMapping("/{id}/cancel") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.create')")
    public ConviteService.ConviteResponse cancelarConvite(Authentication auth,@PathVariable UUID id){return invites.cancelarConvite(((AppUserPrincipal)auth.getPrincipal()).id(),id.toString());}
    @PostMapping("/validate")
    public ConviteService.ConvitePublicoResponse consultarConvitePublico(@Valid @RequestBody ConsultarConviteRequest input){return invites.consultarConvitePublico(input.token());}
    @PostMapping("/accept") @ResponseStatus(HttpStatus.CREATED)
    public ConviteService.ConviteAceitoResponse aceitarConvite(@Valid @RequestBody AceitarConviteRequest input){
        var conta=invites.aceitarConvite(input.token(),input.name(),input.password(),input.passwordConfirmation());
        emails.confirmarConta(conta.userId());return conta;
    }
}
