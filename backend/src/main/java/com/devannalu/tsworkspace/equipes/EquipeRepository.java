package com.devannalu.tsworkspace.equipes;

import java.util.List;
import java.util.Map;
import com.devannalu.tsworkspace.equipes.EquipeService.ResumoEquipe;
import com.devannalu.tsworkspace.equipes.EquipeService.IntegranteEquipe;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EquipeRepository {
    private final JdbcTemplate jdbc;

    public EquipeRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SQL_RESUMO_EQUIPES = """
        SELECT t.*, COALESCE(m.member_count,0) member_count FROM team t LEFT JOIN (
          SELECT tm.team_id, COUNT(*) member_count FROM team_member tm
          JOIN app_profile p ON p.user_id=tm.user_id AND p.status='ACTIVE' GROUP BY tm.team_id
        ) m ON m.team_id=t.id
        """;
    private static ResumoEquipe montarResumoEquipe(java.sql.ResultSet linha, int indiceLinha) throws java.sql.SQLException {
        return new ResumoEquipe(linha.getString("id"), linha.getString("team_key"), linha.getString("name"), linha.getString("description"),
            linha.getString("parent_id"), linha.getTimestamp("archived_at") != null, linha.getLong("member_count"));
    }

    public List<ResumoEquipe> listarEquipesAtivas() {
        return jdbc.query(SQL_RESUMO_EQUIPES + " WHERE t.archived_at IS NULL ORDER BY t.parent_id, t.name", EquipeRepository::montarResumoEquipe);
    }

    public List<ResumoEquipe> listarEquipesUsuario(String usuarioId) {
        return jdbc.query(SQL_RESUMO_EQUIPES + """
            WHERE t.archived_at IS NULL AND EXISTS (
              SELECT 1 FROM team_member own WHERE own.team_id=t.id AND own.user_id=?
            ) ORDER BY t.parent_id, t.name
            """, EquipeRepository::montarResumoEquipe, usuarioId);
    }

    public List<ResumoEquipe> buscarResumoEquipe(String id) {
        return jdbc.query(SQL_RESUMO_EQUIPES + " WHERE t.id=?", EquipeRepository::montarResumoEquipe, id);
    }

    public List<IntegranteEquipe> listarIntegrantesAtivos(String id) {
        return jdbc.query("""
            SELECT u.id,u.name,u.email FROM team_member tm JOIN app_user u ON u.id=tm.user_id
            JOIN app_profile p ON p.user_id=u.id AND p.status='ACTIVE' WHERE tm.team_id=? ORDER BY u.name,u.id
            """, (linha, indiceLinha) -> new IntegranteEquipe(linha.getString("id"), linha.getString("name"), linha.getString("email")), id);
    }

    public List<PoliticaEquipe.NoHierarquia> listarHierarquia() {
        return jdbc.query("SELECT id,team_key,parent_id,archived_at FROM team", (linha, indiceLinha) ->
            new PoliticaEquipe.NoHierarquia(linha.getString("id"), linha.getString("team_key"), linha.getString("parent_id"), linha.getTimestamp("archived_at") != null));
    }

    public int inserirEquipe(String id, String nome, String descricao, String equipeMaeId) {
        return jdbc.update("""
            INSERT INTO team (id,team_key,name,description,parent_id,created_at,updated_at)
            VALUES (?,?,?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))
            """, id, "team-" + id, nome.trim(), descricao == null ? null : descricao.trim(), equipeMaeId);
    }

    public int atualizarEquipe(String id, String nome, String descricao, String equipeMaeId) {
        return jdbc.update("UPDATE team SET name=?,description=?,parent_id=?,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",
            nome.trim(), descricao == null ? null : descricao.trim(), equipeMaeId, id);
    }

    public Long contarSubequipesAtivas(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE parent_id=? AND archived_at IS NULL", Long.class, id);
    }

    public int arquivarEquipe(String id) {
        return jdbc.update("UPDATE team SET archived_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE id=?", id);
    }

    public List<Map<String, Object>> buscarStatusUsuarioBloqueado(String usuarioId) {
        return jdbc.queryForList("SELECT p.status FROM app_user u LEFT JOIN app_profile p ON p.user_id=u.id WHERE u.id=? FOR UPDATE", usuarioId);
    }

    public Long contarIntegrante(String equipeId, String usuarioId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE team_id=? AND user_id=?", Long.class, equipeId,usuarioId);
    }

    public int inserirIntegrante(String usuarioId, String equipeId) {
        return jdbc.update("INSERT INTO team_member (user_id,team_id,created_at) VALUES (?,?,CURRENT_TIMESTAMP(6))", usuarioId,equipeId);
    }

    public List<Map<String, Object>> buscarPerfilAcessoUsuarioBloqueado(String usuarioId) {
        return jdbc.queryForList("SELECT r.role_key FROM app_user u LEFT JOIN app_profile p ON p.user_id=u.id LEFT JOIN roles r ON r.id=p.role_id WHERE u.id=? FOR UPDATE", usuarioId);
    }

    public Long contarSuperAdminsAtivas(String equipeId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM team_member tm JOIN app_profile p ON p.user_id=tm.user_id
            JOIN roles r ON r.id=p.role_id WHERE tm.team_id=? AND p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'
            """, Long.class, equipeId);
    }

    public int removerIntegrante(String usuarioId, String equipeId) {
        return jdbc.update("DELETE FROM team_member WHERE user_id=? AND team_id=?", usuarioId,equipeId);
    }
}
