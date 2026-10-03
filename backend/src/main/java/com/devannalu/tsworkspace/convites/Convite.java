package com.devannalu.tsworkspace.convites;

import com.devannalu.tsworkspace.usuarios.Usuario;
import com.devannalu.tsworkspace.rbac.PerfilAcesso;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="invite")
public class Convite {
    @Id @Column(length=36) private String id;
    @Column(nullable=false,length=320) private String email;
    @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="used_at") private Instant usedAt;
    @Column(name="cancelled_at") private Instant cancelledAt;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invited_by_id",nullable=false) private Usuario invitedBy;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="role_id",nullable=false) private PerfilAcesso role;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected Convite() { }
}
