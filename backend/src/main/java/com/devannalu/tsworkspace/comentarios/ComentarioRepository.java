package com.devannalu.tsworkspace.comentarios;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComentarioRepository {
    private final JdbcTemplate jdbc;
    public ComentarioRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public enum Recurso {
        TAREFA("task_id", "Task", "tasks"), PROJETO("project_id", "Project", "projects");
        final String coluna, entidade, rota;
        Recurso(String coluna, String entidade, String rota) {
            this.coluna = coluna; this.entidade = entidade; this.rota = rota;
        }
    }
    public record Pessoa(String id, String nome) { }
    public record EstadoComentario(String id, String tarefaId, String projetoId, Pessoa autora,
        String conteudo, long versao, Instant criadaEm, Instant atualizadaEm, Instant removidaEm) {
        public Recurso recurso() { return tarefaId != null ? Recurso.TAREFA : Recurso.PROJETO; }
        public String recursoId() { return tarefaId != null ? tarefaId : projetoId; }
    }
    public record Evento(String id, String tipo, Pessoa ator, Instant criadaEm) { }
    private static final String SELECT_COMENTARIO = """
        SELECT c.id,c.task_id,c.project_id,c.author_id,u.name,c.content,c.version,
        c.created_at,c.updated_at,c.removed_at FROM workspace_comment c
        JOIN app_user u ON u.id=c.author_id
        """;
    private EstadoComentario mapear(ResultSet r, int linha) throws SQLException {
        var removida = r.getTimestamp("removed_at");
        return new EstadoComentario(r.getString("id"), r.getString("task_id"), r.getString("project_id"),
            new Pessoa(r.getString("author_id"), r.getString("name")), r.getString("content"), r.getLong("version"),
            r.getTimestamp("created_at").toInstant(), r.getTimestamp("updated_at").toInstant(),
            removida == null ? null : removida.toInstant());
    }
    public Optional<EstadoComentario> buscar(String id) {
        return jdbc.query(SELECT_COMENTARIO + " WHERE c.id=?", this::mapear, id).stream().findFirst();
    }
    public List<EstadoComentario> listar(Recurso recurso, String id, int pagina, int tamanho) {
        return jdbc.query(SELECT_COMENTARIO + " WHERE c." + recurso.coluna + "=? ORDER BY c.created_at DESC,c.id DESC LIMIT ? OFFSET ?",
            this::mapear, id, tamanho, pagina * tamanho);
    }
    public long contar(Recurso recurso, String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM workspace_comment WHERE " + recurso.coluna + "=?", Long.class, id);
    }
    public void inserir(String id, Recurso recurso, String recursoId, String autoraId, String conteudo) {
        jdbc.update("INSERT INTO workspace_comment(id," + recurso.coluna + ",author_id,content,created_at,updated_at) VALUES(?,?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",
            id, recursoId, autoraId, conteudo);
    }
    public int editar(String id, long versao, String conteudo) {
        return jdbc.update("UPDATE workspace_comment SET content=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=? AND version=? AND removed_at IS NULL",
            conteudo, id, versao);
    }
    public int remover(String id, long versao, String autoraId) {
        return jdbc.update("UPDATE workspace_comment SET removed_at=CURRENT_TIMESTAMP(6),removed_by_id=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=? AND version=? AND removed_at IS NULL",
            autoraId, id, versao);
    }
    // Somente vínculos exatos e eventos de produto conhecidos entram na atividade.
    private String filtroAtividade(Recurso recurso) {
        String eventos = recurso == Recurso.TAREFA
            ? "'task.created','task.updated','task.status_changed','task.assignees_changed','task.archived'"
            : "'project.created','project.updated','project.status_changed','project.responsibles_changed','project.archived'";
        return "((a.entity_type=? AND a.entity_id=? AND a.action IN (" + eventos + ")) OR "
            + "(a.entity_type='Comment' AND a.action IN ('comment.created','comment.updated','comment.removed') "
            + "AND EXISTS (SELECT 1 FROM workspace_comment c WHERE c.id=a.entity_id AND c." + recurso.coluna + "=?)))";
    }
    public List<Evento> listarAtividade(Recurso recurso, String id, int pagina, int tamanho) {
        return jdbc.query("SELECT a.id,a.action,a.actor_id,u.name,a.created_at FROM audit_log a LEFT JOIN app_user u ON u.id=a.actor_id WHERE "
            + filtroAtividade(recurso) + " ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",
            (r, linha) -> new Evento(r.getString("id"), r.getString("action"),
                r.getString("actor_id") == null ? null : new Pessoa(r.getString("actor_id"), r.getString("name")),
                r.getTimestamp("created_at").toInstant()), recurso.entidade, id, id, tamanho, pagina * tamanho);
    }
    public long contarAtividade(Recurso recurso, String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log a WHERE " + filtroAtividade(recurso),
            Long.class, recurso.entidade, id, id);
    }
}
