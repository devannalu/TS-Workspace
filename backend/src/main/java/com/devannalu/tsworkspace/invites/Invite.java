package com.devannalu.tsworkspace.invites;

import com.devannalu.tsworkspace.auth.User;
import com.devannalu.tsworkspace.rbac.Role;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="invite")
public class Invite {
    @Id @Column(length=36) private String id;
    @Column(nullable=false,length=320) private String email;
    @Column(name="token_hash",nullable=false,unique=true,length=64) private String tokenHash;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="used_at") private Instant usedAt;
    @Column(name="cancelled_at") private Instant cancelledAt;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invited_by_id",nullable=false) private User invitedBy;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="role_id",nullable=false) private Role role;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="updated_at",nullable=false) private Instant updatedAt;
    protected Invite() { }
}
