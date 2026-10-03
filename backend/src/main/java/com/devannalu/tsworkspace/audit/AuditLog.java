package com.devannalu.tsworkspace.audit;

import com.devannalu.tsworkspace.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="audit_log")
public class AuditLog {
    @Id @Column(length=36) private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="actor_id") private User actor;
    @Column(nullable=false,length=100) private String action;
    @Column(name="entity_type",nullable=false,length=50) private String entityType;
    @Column(name="entity_id",nullable=false,length=100) private String entityId;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name="metadata_json",columnDefinition="json") private String metadataJson;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected AuditLog() { }
}
