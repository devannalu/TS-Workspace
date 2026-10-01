package com.devannalu.tsworkspace.rbac;

import jakarta.persistence.*;

@Entity
@Table(name = "role_permissions")
public class RolePermission {
    @EmbeddedId private RolePermissionId id;
    @MapsId("roleId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false) private Role role;
    @MapsId("permissionId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false) private Permission permission;
    protected RolePermission() { }
}
