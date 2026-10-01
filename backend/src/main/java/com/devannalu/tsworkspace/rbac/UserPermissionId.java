package com.devannalu.tsworkspace.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserPermissionId implements Serializable {
    @Column(name = "user_id", length = 36) private String userId;
    @Column(name = "permission_id", length = 36) private String permissionId;
    protected UserPermissionId() { }
    public UserPermissionId(String userId, String permissionId) {
        this.userId = userId;
        this.permissionId = permissionId;
    }
    @Override public boolean equals(Object other) {
        return other instanceof UserPermissionId id && Objects.equals(userId, id.userId) && Objects.equals(permissionId, id.permissionId);
    }
    @Override public int hashCode() { return Objects.hash(userId, permissionId); }
}
