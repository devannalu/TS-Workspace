package com.devannalu.tsworkspace.rbac;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity
public class RbacConfig {
    @Bean
    V5__provision_rbac rbacProvisioningMigration(
        @Value("${JAVA_RBAC_PROVISION_EXISTING:false}") boolean enabled,
        @Value("${BOOTSTRAP_EMAIL:}") String email,
        @Value("${BOOTSTRAP_PASSWORD:}") String password
    ) {
        return new V5__provision_rbac(enabled, email, password);
    }
}
