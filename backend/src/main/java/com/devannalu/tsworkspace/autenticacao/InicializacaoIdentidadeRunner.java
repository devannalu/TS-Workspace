package com.devannalu.tsworkspace.autenticacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class InicializacaoIdentidadeRunner implements ApplicationRunner {
    private final InicializacaoIdentidadeService bootstrap;
    private final String name;
    private final String email;
    private final String password;
    private final ConfigurableApplicationContext context;
    private final boolean exitOnComplete;

    public InicializacaoIdentidadeRunner(
        InicializacaoIdentidadeService bootstrap,
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
        bootstrap.criarPrimeiraIdentidade(name, email, password);
        if (exitOnComplete) SpringApplication.exit(context);
    }
}
