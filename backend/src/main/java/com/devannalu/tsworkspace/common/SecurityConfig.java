package com.devannalu.tsworkspace.common;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, @org.springframework.beans.factory.annotation.Qualifier("javaCors") CorsConfigurationSource source) throws Exception {
        // CSRF remains enabled. No login/signup or business authorization exists in Java yet.
        return http.cors(cors -> cors.configurationSource(source))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll()
                .anyRequest().denyAll())
            .build();
    }

    @Bean
    UserDetailsService noFoundationUsers() {
        // Suppress Boot's generated development password without opening authentication.
        return new InMemoryUserDetailsManager();
    }

    @Bean
    CorsConfigurationSource javaCors(@Value("${app.frontend-origin}") String origin) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(List.of("GET"));
        config.setAllowedHeaders(List.of("Accept", "Content-Type"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/v1/**", config);
        return source;
    }
}
