package com.devannalu.tsworkspace.seguranca;

import com.devannalu.tsworkspace.seguranca.UsuarioAtivoFilter;
import com.devannalu.tsworkspace.autenticacao.AutenticacaoUsuarioService;
import com.devannalu.tsworkspace.usuarios.PerfilRepository;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SegurancaConfig {
    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean
    DaoAuthenticationProvider daoAuthenticationProvider(AutenticacaoUsuarioService users, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(users);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(),
            new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy() { return new ChangeSessionIdAuthenticationStrategy(); }

    @Bean
    LogoutHandler logoutHandler() {
        return new CompositeLogoutHandler(
            new SecurityContextLogoutHandler(),
            new CookieClearingLogoutHandler("TS_SESSION")
        );
    }

    @Bean
    CookieSerializer cookieSerializer(@Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("TS_SESSION");
        serializer.setCookiePath("/");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setUseSecureCookie(secure);
        serializer.setSameSite("Lax");
        return serializer;
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        return repository;
    }

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        SecurityContextRepository contextRepository,
        CsrfTokenRepository csrfRepository,
        CorsConfigurationSource cors,
        PerfilRepository profiles,
        LogoutHandler logoutHandler
    ) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        return http
            .securityContext(context -> context.securityContextRepository(contextRepository).requireExplicitSave(true))
            .csrf(csrf -> csrf.csrfTokenRepository(csrfRepository).csrfTokenRequestHandler(csrfHandler))
            .cors(corsConfigurer -> corsConfigurer.configurationSource(cors))
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .sessionFixation(fixation -> fixation.changeSessionId()))
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, error) -> writeProblem(response, 401, "Não autenticada", "Autenticação necessária."))
                .accessDeniedHandler((request, response, error) -> writeProblem(response, 403, "Acesso negado", "Acesso negado.")))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/health", "/api/v1/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/invites/validate", "/api/v1/invites/accept").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/users", "/api/v1/users/*", "/api/v1/invites", "/api/v1/invites/pending-count").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/users/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/users/*/deactivate", "/api/v1/users/*/activate", "/api/v1/invites", "/api/v1/invites/*/cancel").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/permissions").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/teams", "/api/v1/teams/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/teams", "/api/v1/teams/*/archive", "/api/v1/teams/*/members").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/v1/teams/*").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/teams/*/members/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/tasks", "/api/v1/tasks/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/tasks", "/api/v1/tasks/*/archive").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/tasks/*", "/api/v1/tasks/*/position").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/projects", "/api/v1/projects/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/projects", "/api/v1/projects/*/archive").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/projects/*").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/tasks/*/comments", "/api/v1/projects/*/comments",
                    "/api/v1/tasks/*/activity", "/api/v1/projects/*/activity").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/tasks/*/comments", "/api/v1/projects/*/comments", "/api/v1/comments/*/remove").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/comments/*").authenticated()
                .anyRequest().denyAll())
            .addFilterAfter(new UsuarioAtivoFilter(profiles, logoutHandler), org.springframework.security.web.context.SecurityContextHolderFilter.class)
            .build();
    }

    @Bean
    CorsConfigurationSource cors(@Value("${app.frontend-origin}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Accept", "Content-Type", "X-XSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }

    private static void writeProblem(jakarta.servlet.http.HttpServletResponse response, int status, String title, String detail) throws IOException {
        response.setStatus(status);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + title + "\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }
}
