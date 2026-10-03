package com.devannalu.tsworkspace.compartilhado;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BloqueioOrganizacao {
    private final JdbcTemplate jdbc;
    public BloqueioOrganizacao(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    // Bloquear antes da leitura evita decisões concorrentes sobre a mesma organização.
    public void adquirir() {
        if(jdbc.queryForList("SELECT id FROM team WHERE team_key='fundadoras' FOR UPDATE",String.class).isEmpty())
            throw ProblemaDominio.conflito("Raiz estrutural indisponível.");
    }
}
