package com.devannalu.tsworkspace.teams;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {
    private final JdbcTemplate jdbc;
    public TeamService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Summary(String id, String key, String name, String description, String parentId, boolean archived, long memberCount) { }
    public record Member(String id, String name, String email) { }
    public record Detail(Summary team, List<Member> members) { }
    private static final String SUMMARY_SQL = """
        SELECT t.*, COALESCE(m.member_count,0) member_count FROM team t LEFT JOIN (
          SELECT tm.team_id, COUNT(*) member_count FROM team_member tm
          JOIN app_profile p ON p.user_id=tm.user_id AND p.status='ACTIVE' GROUP BY tm.team_id
        ) m ON m.team_id=t.id
        """;
    private static Summary summary(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new Summary(rs.getString("id"), rs.getString("team_key"), rs.getString("name"), rs.getString("description"),
            rs.getString("parent_id"), rs.getTimestamp("archived_at") != null, rs.getLong("member_count"));
    }
    @Transactional(readOnly = true)
    public List<Summary> list() {
        return jdbc.query(SUMMARY_SQL + " WHERE t.archived_at IS NULL ORDER BY t.parent_id, t.name", TeamService::summary);
    }
    @Transactional(readOnly = true)
    public Detail detail(String id) {
        var teams = jdbc.query(SUMMARY_SQL + " WHERE t.id=?", TeamService::summary, id);
        if (teams.isEmpty()) throw TeamProblem.missing("Equipe não encontrada.");
        var members = jdbc.query("""
            SELECT u.id,u.name,u.email FROM team_member tm JOIN app_user u ON u.id=tm.user_id
            JOIN app_profile p ON p.user_id=u.id AND p.status='ACTIVE' WHERE tm.team_id=? ORDER BY u.name,u.id
            """, (rs, row) -> new Member(rs.getString("id"), rs.getString("name"), rs.getString("email")), id);
        return new Detail(teams.get(0), members);
    }
    // All administrative writes acquire the same root row first. This serializes
    // tree changes and membership removals across API instances, including last-admin checks.
    private void lockTree() {
        if (jdbc.queryForList("SELECT id FROM team WHERE team_key='fundadoras' FOR UPDATE", String.class).isEmpty())
            throw TeamProblem.conflict("Raiz estrutural indisponível.");
    }
    private List<TeamPolicy.Node> tree() {
        return jdbc.query("SELECT id,team_key,parent_id,archived_at FROM team", (rs, row) ->
            new TeamPolicy.Node(rs.getString("id"), rs.getString("team_key"), rs.getString("parent_id"), rs.getTimestamp("archived_at") != null));
    }
    private TeamPolicy.Node team(String id) {
        return tree().stream().filter(t -> t.id().equals(id)).findFirst().orElseThrow(() -> TeamProblem.missing("Equipe não encontrada."));
    }
    private TeamPolicy.Node activeTeam(String id) {
        var t = team(id);
        if (t.archived()) throw TeamProblem.conflict("Equipe indisponível.");
        return t;
    }
    @Transactional
    public Detail create(String name, String description, String parentId) {
        lockTree();
        String id = UUID.randomUUID().toString();
        TeamPolicy.parent(tree(), id, parentId);
        jdbc.update("""
            INSERT INTO team (id,team_key,name,description,parent_id,created_at,updated_at)
            VALUES (?,?,?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))
            """, id, "team-" + id, name.trim(), description == null ? null : description.trim(), parentId);
        return detail(id);
    }
    @Transactional
    public Detail edit(String id, String name, String description, String parentId) {
        lockTree(); team(id);
        TeamPolicy.parent(tree(), id, parentId);
        jdbc.update("UPDATE team SET name=?,description=?,parent_id=?,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",
            name.trim(), description == null ? null : description.trim(), parentId, id);
        return detail(id);
    }
    @Transactional
    public Detail archive(String id) {
        lockTree(); var t = team(id);
        long children = jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE parent_id=? AND archived_at IS NULL", Long.class, id);
        TeamPolicy.archive(t, children);
        if (!t.archived()) jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE id=?", id);
        return detail(id);
    }
    @Transactional
    public void addMember(String teamId, String userId) {
        lockTree(); activeTeam(teamId);
        var profiles = jdbc.queryForList("SELECT p.status FROM app_user u LEFT JOIN app_profile p ON p.user_id=u.id WHERE u.id=? FOR UPDATE", userId);
        if (profiles.isEmpty()) throw TeamProblem.missing("Usuária não encontrada.");
        boolean duplicate = jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE team_id=? AND user_id=?", Long.class, teamId,userId) > 0;
        TeamPolicy.addMember("ACTIVE".equals(profiles.get(0).get("status")), duplicate);
        jdbc.update("INSERT INTO team_member (user_id,team_id,created_at) VALUES (?,?,CURRENT_TIMESTAMP(6))", userId,teamId);
    }
    @Transactional
    public void removeMember(String teamId, String userId) {
        lockTree(); var t = activeTeam(teamId);
        var profiles = jdbc.queryForList("SELECT r.role_key FROM app_user u LEFT JOIN app_profile p ON p.user_id=u.id LEFT JOIN roles r ON r.id=p.role_id WHERE u.id=? FOR UPDATE", userId);
        if (profiles.isEmpty()) throw TeamProblem.missing("Usuária não encontrada.");
        if (jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE team_id=? AND user_id=?", Long.class, teamId,userId) == 0)
            throw TeamProblem.missing("A integrante não está nesta equipe.");
        long admins = jdbc.queryForObject("""
            SELECT COUNT(*) FROM team_member tm JOIN app_profile p ON p.user_id=tm.user_id
            JOIN roles r ON r.id=p.role_id WHERE tm.team_id=? AND p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'
            """, Long.class, teamId);
        TeamPolicy.removeMember(t.key(), (String) profiles.get(0).get("role_key"), admins);
        jdbc.update("DELETE FROM team_member WHERE user_id=? AND team_id=?", userId,teamId);
    }
}
