package com.devannalu.tsworkspace.invites;

import com.devannalu.tsworkspace.teams.Team;
import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name="invite_team")
public class InviteTeam {
    @EmbeddedId private InviteTeamId id;
    @MapsId("inviteId") @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="invite_id",nullable=false) private Invite invite;
    @MapsId("teamId") @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="team_id",nullable=false) private Team team;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    protected InviteTeam() { }
}
