package com.devannalu.tsworkspace.autenticacao;

import com.devannalu.tsworkspace.auth.AppUserPrincipal;
import com.devannalu.tsworkspace.auth.EmailNormalizer;
import com.devannalu.tsworkspace.auth.Profile;
import com.devannalu.tsworkspace.auth.ProfileRepository;
import com.devannalu.tsworkspace.auth.ProfileStatus;
import com.devannalu.tsworkspace.auth.User;
import com.devannalu.tsworkspace.auth.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.List;
import com.devannalu.tsworkspace.rbac.PermissaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AutenticacaoController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final LogoutHandler logoutHandler;
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final PermissaoService permissions;

    public AutenticacaoController(
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        SessionAuthenticationStrategy sessionAuthenticationStrategy,
        LogoutHandler logoutHandler,
        UserRepository users,
        ProfileRepository profiles,
        PermissaoService permissions
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.logoutHandler = logoutHandler;
        this.users = users;
        this.profiles = profiles;
        this.permissions = permissions;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        Authentication authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(EmailNormalizer.normalize(request.email()), request.password())
        );
        servletRequest.getSession(true);
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return montarUsuarioAtual((AppUserPrincipal) authentication.getPrincipal());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, SecurityContextHolder.getContext().getAuthentication());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return montarUsuarioAtual((AppUserPrincipal) authentication.getPrincipal());
    }

    private UserResponse montarUsuarioAtual(AppUserPrincipal principal) {
        User user = users.findById(principal.id()).orElseThrow(() -> new IllegalStateException("Usuária não encontrada."));
        Profile profile = profiles.findById(user.getId()).orElseThrow(() -> new IllegalStateException("Perfil não encontrado."));
        var rbac = permissions.buscarPermissoesUsuario(principal.id());
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), profile.getJobTitle(), profile.getStatus(), rbac.role(), rbac.chavesEfetivas());
    }

    public record LoginRequest(@NotBlank String email, @NotBlank String password) { }
    public record UserResponse(String id, String name, String email, String jobTitle, ProfileStatus status,
                               PermissaoService.PerfilAcessoResponse role, List<String> permissions) { }
}
