package com.devannalu.tsworkspace.rbac;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "roles")
public class Role {
    @Id @Column(length = 36) private String id;
    @Column(name = "role_key", nullable = false, unique = true, length = 32) private String key;
    @Column(nullable = false, length = 160) private String name;
    @Column(length = 500) private String description;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Role() { }
    public String getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
}
