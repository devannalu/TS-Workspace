package com.devannalu.tsworkspace.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class BootstrapRunner implements ApplicationRunner {
    private final BootstrapService bootstrap;
    private final String name;
    private final String email;
    private final String password;
    private final ConfigurableApplicationContext context;
    private final boolean exitOnComplete;

    public BootstrapRunner(
        BootstrapService bootstrap,
        @Value("${BOOTSTRAP_NAME:}") String name,
        @Value("${BOOTSTRAP_EMAIL:}") String email,
        @Value("${BOOTSTRAP_PASSWORD:}") String password,
        ConfigurableApplicationContext context,
        @Value("${app.bootstrap.exit-on-complete:false}") boolean exitOnComplete
    ) {
        this.bootstrap = bootstrap;
        this.name = name;
        this.email = email;
        this.password = password;
        this.context = context;
        this.exitOnComplete = exitOnComplete;
    }

    @Override
    public void run(ApplicationArguments args) {
        bootstrap.createFirstIdentity(name, email, password);
        if (exitOnComplete) SpringApplication.exit(context);
    }
}
