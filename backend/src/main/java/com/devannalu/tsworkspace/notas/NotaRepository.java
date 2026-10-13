package com.devannalu.tsworkspace.notas;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotaRepository {
    private final JdbcTemplate jdbc;
    public NotaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public enum Recurso {
        PROJETO("projects", "project_id"), EVENTO("events", "event_id"),
        PARCERIA("partnerships", "partnership_id"), REUNIAO("meetings", "meeting_id");
        final String rota, coluna;
        Recurso(String rota, String coluna) { this.rota = rota; this.coluna = coluna; }
        public static Recurso daRota(String rota) {
            return Arrays.stream(values()).filter(r -> r.rota.equals(rota)).findFirst().orElseThrow(IllegalArgumentException::new);
        }
    }
    public record Pessoa(String id, String nome) { }
    public record Registro(String id, Recurso recurso, String recursoId, Pessoa autora, String titulo,
        String texto, long versao, Instant criadaEm, Instant atualizadaEm, Instant removidaEm) { }
    private static final String SELECT = "SELECT n.*,u.name autora_nome FROM workspace_note n JOIN app_user u ON u.id=n.author_id";
    private Registro ler(ResultSet r, int indice) throws SQLException {
        var recurso = Arrays.stream(Recurso.values()).filter(tipo -> {
            try { return r.getString(tipo.coluna) != null; } catch (SQLException e) { throw new IllegalStateException(e); }
        }).findFirst().orElseThrow();
        var removida = r.getTimestamp("removed_at");
        return new Registro(r.getString("id"), recurso, r.getString(recurso.coluna),
            new Pessoa(r.getString("author_id"),r.getString("autora_nome")),r.getString("title"),r.getString("body"),
            r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),
            removida == null ? null : removida.toInstant());
    }
    public Optional<Registro> buscar(String id) {
        return jdbc.query(SELECT + " WHERE n.id=?",this::ler,id).stream().findFirst();
    }
    public List<Registro> listar(Recurso recurso, String id, int pagina, int tamanho) {
        return jdbc.query(SELECT + " WHERE n."+recurso.coluna+"=? AND n.removed_at IS NULL ORDER BY n.updated_at DESC,n.id DESC LIMIT ? OFFSET ?",
            this::ler,id,tamanho,pagina*tamanho);
    }
    public long contar(Recurso recurso, String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM workspace_note WHERE "+recurso.coluna+"=? AND removed_at IS NULL",Long.class,id);
    }
    public void inserir(String id, Recurso recurso, String recursoId, String autora, String titulo, String texto) {
        jdbc.update("INSERT INTO workspace_note(id,"+recurso.coluna+",author_id,title,body,created_at,updated_at) VALUES(?,?,?,?,?,NOW(6),NOW(6))",id,recursoId,autora,titulo,texto);
    }
    public int editar(String id, long versao, String titulo, String texto) {
        return jdbc.update("UPDATE workspace_note SET title=?,body=?,version=version+1,updated_at=NOW(6) WHERE id=? AND version=? AND removed_at IS NULL",titulo,texto,id,versao);
    }
    public int remover(String id, long versao) {
        return jdbc.update("UPDATE workspace_note SET removed_at=NOW(6),updated_at=NOW(6),version=version+1 WHERE id=? AND version=? AND removed_at IS NULL",id,versao);
    }
}
