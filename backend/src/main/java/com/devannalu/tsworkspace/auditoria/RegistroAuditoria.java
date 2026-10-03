package com.devannalu.tsworkspace.auditoria;

import com.devannalu.tsworkspace.usuarios.Usuario;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="audit_log")
public class RegistroAuditoria {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id") private Usuario autor;
    @Column(name="action",nullable=false,length=100) private String acao;
    @Column(name="entity_type",nullable=false,length=50) private String tipoEntidade;
    @Column(name="entity_id",nullable=false,length=100) private String entidadeId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name="metadata_json",columnDefinition="json") private String metadadosJson;
    @Column(name="created_at",nullable=false) private Instant criadoEm;
    protected RegistroAuditoria() { }
}
