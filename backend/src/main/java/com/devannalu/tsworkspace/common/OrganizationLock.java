package com.devannalu.tsworkspace.common;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrganizationLock {
    private final JdbcTemplate jdbc;
    public OrganizationLock(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    /** Called inside a write transaction, before reading any organizational state. */
    public void acquire() {
        if(jdbc.queryForList("SELECT id FROM team WHERE team_key='fundadoras' FOR UPDATE",String.class).isEmpty())
            throw DomainProblem.conflict("Raiz estrutural indisponível.");
    }
}
