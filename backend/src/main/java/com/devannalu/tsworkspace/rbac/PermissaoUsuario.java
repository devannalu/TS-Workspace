package com.devannalu.tsworkspace.rbac;

import jakarta.persistence.*;

@Entity
@Table(name = "user_permissions")
public class PermissaoUsuario {
    @EmbeddedId private IdPermissaoUsuario id;
    @MapsId("userId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private com.devannalu.tsworkspace.auth.User user;
    @MapsId("permissionId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false) private Permissao permission;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 5) private EfeitoPermissao effect;
    protected PermissaoUsuario() { }
}
