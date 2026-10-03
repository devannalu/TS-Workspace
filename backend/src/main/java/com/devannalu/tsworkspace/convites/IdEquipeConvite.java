package com.devannalu.tsworkspace.convites;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class IdEquipeConvite implements Serializable {
    @Column(name="invite_id",length=36) private String inviteId;
    @Column(name="team_id",length=36) private String teamId;
    protected IdEquipeConvite() { }
    @Override public boolean equals(Object other) { return other instanceof IdEquipeConvite id&&Objects.equals(inviteId,id.inviteId)&&Objects.equals(teamId,id.teamId); }
    @Override public int hashCode() { return Objects.hash(inviteId,teamId); }
}
