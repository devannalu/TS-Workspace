package com.devannalu.tsworkspace.teams;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class TeamMemberId implements Serializable {
    @Column(name = "user_id", length = 36) private String userId;
    @Column(name = "team_id", length = 36) private String teamId;
    protected TeamMemberId() { }
    public TeamMemberId(String userId, String teamId) { this.userId = userId; this.teamId = teamId; }
    @Override public boolean equals(Object other) {
        return other instanceof TeamMemberId id && Objects.equals(userId, id.userId) && Objects.equals(teamId, id.teamId);
    }
    @Override public int hashCode() { return Objects.hash(userId, teamId); }
}
