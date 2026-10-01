package com.devannalu.tsworkspace.teams;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team")
public class Team {
    @Id @Column(length = 36) private String id;
    @Column(name = "team_key", nullable = false, unique = true, length = 100) private String key;
    @Column(nullable = false, length = 100) private String name;
    @Column(length = 500) private String description;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id") private Team parent;
    @Column(name = "archived_at") private Instant archivedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected Team() { }
    public String getId() { return id; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public Team getParent() { return parent; }
}
