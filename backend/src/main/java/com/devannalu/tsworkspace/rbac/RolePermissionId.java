package com.devannalu.tsworkspace.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class RolePermissionId implements Serializable {
    @Column(name = "role_id", length = 36) private String roleId;
    @Column(name = "permission_id", length = 36) private String permissionId;
    protected RolePermissionId() { }
    public RolePermissionId(String roleId, String permissionId) {
        this.roleId = roleId;
        this.permissionId = permissionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof RolePermissionId id && Objects.equals(roleId, id.roleId) && Objects.equals(permissionId, id.permissionId);
    }
    @Override public int hashCode() { return Objects.hash(roleId, permissionId); }
}
