package com.devannalu.tsworkspace.usuarios;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/users")
public class UsuarioController {
    private final UsuarioService users;
    public UsuarioController(UsuarioService users){this.users=users;}
    public record Patch(@Size(max=160) String jobTitle,UUID roleId,@Size(max=50) List<UUID> teamIds) { }
    @GetMapping @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public UsuarioService.PaginaUsuarios listarUsuarios(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size,
        @RequestParam(required=false) ProfileStatus status,@RequestParam(required=false) UUID roleId,@RequestParam(required=false) UUID teamId,@RequestParam(required=false) String search){
        return users.listarUsuarios(page,size,status==null?null:status.name(),roleId==null?null:roleId.toString(),teamId==null?null:teamId.toString(),search);
    }
    @GetMapping("/{id}") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public UsuarioService.UsuarioResponse buscarUsuario(@PathVariable UUID id){return users.buscarUsuario(id.toString());}
    @GetMapping("/options") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.view')")
    public UsuarioService.OpcoesUsuarios buscarOpcoesUsuarios(){return users.buscarOpcoesUsuarios();}
    @PatchMapping("/{id}") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.edit')")
    public UsuarioService.UsuarioResponse editarUsuario(Authentication auth,@PathVariable UUID id,@Valid @RequestBody Patch input){
        if(input.teamIds()!=null&&input.teamIds().contains(null))throw new IllegalArgumentException();
        return users.editarUsuario(((AppUserPrincipal)auth.getPrincipal()).id(),id.toString(),input.jobTitle(),input.roleId()==null?null:input.roleId().toString(),input.teamIds()==null?null:input.teamIds().stream().map(UUID::toString).toList());
    }
    @PostMapping("/{id}/deactivate") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.disable')")
    public UsuarioService.UsuarioResponse deactivate(Authentication auth,@PathVariable UUID id){return users.alterarSituacaoUsuario(((AppUserPrincipal)auth.getPrincipal()).id(),id.toString(),false);}
    @PostMapping("/{id}/activate") @PreAuthorize("@verificadorPermissao.possuiPermissao(authentication,'users.disable')")
    public UsuarioService.UsuarioResponse activate(Authentication auth,@PathVariable UUID id){return users.alterarSituacaoUsuario(((AppUserPrincipal)auth.getPrincipal()).id(),id.toString(),true);}
}
