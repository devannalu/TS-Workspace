package com.devannalu.tsworkspace.equipes;

import com.devannalu.tsworkspace.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_member")
public class MembroEquipe {
    @EmbeddedId private IdMembroEquipe id;
    @MapsId("userId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @MapsId("teamId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false) private Equipe team;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected MembroEquipe() { }
}
