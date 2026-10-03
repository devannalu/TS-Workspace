package com.devannalu.tsworkspace.rbac;

import jakarta.persistence.*;

@Entity
@Table(name = "role_permissions")
public class PermissaoPerfilAcesso {
    @EmbeddedId private IdPermissaoPerfilAcesso id;
    @MapsId("roleId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false) private PerfilAcesso role;
    @MapsId("permissionId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false) private Permissao permission;
    protected PermissaoPerfilAcesso() { }
}
