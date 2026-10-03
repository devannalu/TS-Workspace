package com.devannalu.tsworkspace.equipes;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IdMembroEquipe implements Serializable {
    @Column(name = "user_id", length = 36) private String userId;
    @Column(name = "team_id", length = 36) private String teamId;
    protected IdMembroEquipe() { }
    public IdMembroEquipe(String userId, String teamId) { this.userId = userId; this.teamId = teamId; }
    @Override public boolean equals(Object other) {
        return other instanceof IdMembroEquipe id && Objects.equals(userId, id.userId) && Objects.equals(teamId, id.teamId);
    }
    @Override public int hashCode() { return Objects.hash(userId, teamId); }
}
