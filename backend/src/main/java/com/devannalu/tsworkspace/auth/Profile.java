package com.devannalu.tsworkspace.auth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "app_profile")
public class Profile {
    @Id
    @Column(name = "user_id", length = 36, updatable = false)
    private String userId;

    @Column(name = "job_title", length = 160)
    private String jobTitle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProfileStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Profile() { }

    public Profile(String userId, ProfileStatus status) {
        this.userId = userId;
        this.status = status;
    }

    @PrePersist
    void beforeInsert() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void beforeUpdate() { updatedAt = Instant.now(); }

    public String getUserId() { return userId; }
    public String getJobTitle() { return jobTitle; }
    public ProfileStatus getStatus() { return status; }
}
