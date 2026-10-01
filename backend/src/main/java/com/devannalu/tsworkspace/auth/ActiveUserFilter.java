package com.devannalu.tsworkspace.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

public class ActiveUserFilter extends OncePerRequestFilter {
    private final ProfileRepository profiles;
    private final LogoutHandler logoutHandler;

    public ActiveUserFilter(ProfileRepository profiles, LogoutHandler logoutHandler) {
        this.profiles = profiles;
        this.logoutHandler = logoutHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof AppUserPrincipal principal) {
            ProfileStatus status = profiles.findById(principal.id()).map(Profile::getStatus).orElse(ProfileStatus.INACTIVE);
            if (status != ProfileStatus.ACTIVE) {
                logoutHandler.logout(request, response, authentication);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/problem+json");
                response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Acesso negado\",\"status\":403,\"detail\":\"Usuária inativa.\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
