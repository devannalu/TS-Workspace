package com.devannalu.tsworkspace.audit;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    // No arbitrary metadata input: these events never receive credentials or tokens.
    @Transactional(propagation=Propagation.MANDATORY)
    public void record(String actor,String action,String entityType,String entityId) {
        jdbc.update("INSERT INTO audit_log (id,actor_id,action,entity_type,entity_id,created_at) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP(6))",
            UUID.randomUUID().toString(),actor,action,entityType,entityId);
    }
}
