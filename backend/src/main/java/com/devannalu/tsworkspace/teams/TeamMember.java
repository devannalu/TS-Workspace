package com.devannalu.tsworkspace.teams;

import com.devannalu.tsworkspace.auth.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_member")
public class TeamMember {
    @EmbeddedId private TeamMemberId id;
    @MapsId("userId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) private User user;
    @MapsId("teamId") @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false) private Team team;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected TeamMember() { }
}
