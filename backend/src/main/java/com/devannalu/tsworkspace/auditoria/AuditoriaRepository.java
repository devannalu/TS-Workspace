package com.devannalu.tsworkspace.auditoria;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuditoriaRepository {
    private final JdbcTemplate jdbc;
    public AuditoriaRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    // Metadados livres são proibidos para evitar credenciais e tokens na auditoria.
    @Transactional(propagation=Propagation.MANDATORY)
    public void registrar(String autorId,String acao,String tipoEntidade,String entidadeId) {
        jdbc.update("INSERT INTO audit_log (id,actor_id,action,entity_type,entity_id,created_at) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP(6))",
            UUID.randomUUID().toString(),autorId,acao,tipoEntidade,entidadeId);
    }
}
