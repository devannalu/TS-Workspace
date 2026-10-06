package com.devannalu.tsworkspace.tarefas;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import com.devannalu.tsworkspace.tarefas.PoliticaTarefa.Acesso;
import com.devannalu.tsworkspace.tarefas.TarefaService.FiltrosTarefas;

@Repository
public class TarefaRepository {
    private final JdbcTemplate jdbc;
    public TarefaRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public record Referencia(String id, String nome) { }
    public record EstadoTarefa(String id, String titulo, String descricao, Tarefa.Status status,
        Tarefa.Prioridade prioridade, Referencia equipe, boolean equipeArquivada, Referencia criadaPor,
        LocalDate prazo, int ordem, long versao, Instant arquivadaEm, Instant criadaEm, Instant atualizadaEm,
        Referencia projeto, boolean projetoArquivado) { }
    public record Consulta(String sql, List<Object> parametros) { }

    private static final String SQL_TAREFA = """
        SELECT t.*, e.name equipe_nome, e.archived_at equipe_arquivada, u.name criadora_nome, p.title projeto_titulo, p.archived_at projeto_arquivado
        FROM task t JOIN team e ON e.id=t.team_id JOIN app_user u ON u.id=t.created_by_id
        LEFT JOIN project p ON p.id=t.project_id
        """;

    private static Instant instante(ResultSet linha, String campo) throws SQLException {
        var valor = linha.getTimestamp(campo);
        return valor == null ? null : valor.toInstant();
    }

    private static EstadoTarefa lerTarefa(ResultSet linha, int indice) throws SQLException {
        var prazo = linha.getDate("due_date");
        return new EstadoTarefa(linha.getString("id"), linha.getString("title"), linha.getString("description"),
            Tarefa.Status.valueOf(linha.getString("status")), Tarefa.Prioridade.valueOf(linha.getString("priority")),
            new Referencia(linha.getString("team_id"), linha.getString("equipe_nome")),
            linha.getTimestamp("equipe_arquivada") != null,
            new Referencia(linha.getString("created_by_id"), linha.getString("criadora_nome")),
            prazo == null ? null : prazo.toLocalDate(), linha.getInt("position"), linha.getLong("version"),
            instante(linha, "archived_at"), instante(linha, "created_at"), instante(linha, "updated_at"),
            linha.getString("project_id") == null ? null : new Referencia(linha.getString("project_id"), linha.getString("projeto_titulo")),
            linha.getTimestamp("projeto_arquivado") != null);
    }

    private void limitarEscopo(StringBuilder sql, List<Object> parametros, Acesso acesso) {
        if (!acesso.global()) {
            sql.append(" AND EXISTS (SELECT 1 FROM team_member own WHERE own.team_id=t.team_id AND own.user_id=?)");
            parametros.add(acesso.usuarioId());
        }
    }

    public Consulta montarConsulta(Acesso acesso, FiltrosTarefas filtros) {
        StringBuilder sql = new StringBuilder(filtros.arquivadas() ? " WHERE t.archived_at IS NOT NULL" : " WHERE t.archived_at IS NULL");
        List<Object> parametros = new ArrayList<>();
        limitarEscopo(sql, parametros, acesso);
        if (filtros.equipeId() != null) { sql.append(" AND t.team_id=?"); parametros.add(filtros.equipeId()); }
        if (filtros.projetoId() != null) { sql.append(" AND t.project_id=?"); parametros.add(filtros.projetoId()); }
        if (filtros.status() != null) { sql.append(" AND t.status=?"); parametros.add(filtros.status().name()); }
        if (filtros.prioridade() != null) { sql.append(" AND t.priority=?"); parametros.add(filtros.prioridade().name()); }
        if (filtros.responsavelId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM task_assignee a WHERE a.task_id=t.id AND a.user_id=?)");
            parametros.add(filtros.responsavelId());
        }
        if (filtros.busca() != null && !filtros.busca().isBlank()) {
            sql.append(" AND (t.title LIKE ? ESCAPE '!' OR t.description LIKE ? ESCAPE '!')");
            String busca = "%" + filtros.busca().trim().replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            parametros.add(busca); parametros.add(busca);
        }
        if (filtros.prazoDe() != null) { sql.append(" AND t.due_date>=?"); parametros.add(filtros.prazoDe()); }
        if (filtros.prazoAte() != null) { sql.append(" AND t.due_date<=?"); parametros.add(filtros.prazoAte()); }
        return new Consulta(sql.toString(), parametros);
    }

    public List<EstadoTarefa> listar(Consulta consulta, int pagina, int tamanho) {
        List<Object> parametros = new ArrayList<>(consulta.parametros());
        parametros.add(tamanho); parametros.add(pagina * tamanho);
        return jdbc.query(SQL_TAREFA + consulta.sql() + " ORDER BY t.position,t.id LIMIT ? OFFSET ?",
            TarefaRepository::lerTarefa, parametros.toArray());
    }

    public long contar(Consulta consulta) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM task t" + consulta.sql(), Long.class, consulta.parametros().toArray());
    }

    public Optional<EstadoTarefa> buscar(String id) {
        return jdbc.query(SQL_TAREFA + " WHERE t.id=?", TarefaRepository::lerTarefa, id).stream().findFirst();
    }

    public Map<String, List<Referencia>> buscarResponsaveis(Collection<String> tarefaIds) {
        Map<String, List<Referencia>> resultado = new HashMap<>();
        if (tarefaIds.isEmpty()) return resultado;
        String marcadores = String.join(",", Collections.nCopies(tarefaIds.size(), "?"));
        jdbc.query("SELECT a.task_id,u.id,u.name FROM task_assignee a JOIN app_user u ON u.id=a.user_id WHERE a.task_id IN ("
            + marcadores + ") ORDER BY u.name,u.id", (org.springframework.jdbc.core.RowCallbackHandler) linha ->
                resultado.computeIfAbsent(linha.getString("task_id"), chave -> new ArrayList<>())
                    .add(new Referencia(linha.getString("id"), linha.getString("name"))), tarefaIds.toArray());
        return resultado;
    }

    public boolean integrante(String equipeId, String usuarioId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE team_id=? AND user_id=?", Long.class, equipeId, usuarioId) > 0;
    }

    public boolean possuiResponsabilidadeAtiva(String usuarioId, String equipeId) {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM task_assignee a JOIN task t ON t.id=a.task_id
            WHERE a.user_id=? AND t.team_id=? AND t.archived_at IS NULL
            """, Long.class, usuarioId, equipeId) > 0;
    }

    public Optional<Boolean> equipeArquivada(String equipeId) {
        return jdbc.query("SELECT archived_at FROM team WHERE id=?", (linha, indice) -> linha.getTimestamp(1) != null, equipeId).stream().findFirst();
    }

    public long contarResponsaveisElegiveis(String equipeId, Set<String> ids) {
        if (ids.isEmpty()) return 0;
        List<Object> parametros = new ArrayList<>(); parametros.add(equipeId); parametros.addAll(ids);
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM team_member m JOIN app_profile p ON p.user_id=m.user_id
            WHERE m.team_id=? AND p.status='ACTIVE' AND m.user_id IN (
            """ + String.join(",", Collections.nCopies(ids.size(), "?")) + ")", Long.class, parametros.toArray());
    }

    public List<Referencia> listarEquipesDisponiveis(Acesso acesso) {
        String escopo = acesso.global() ? "" : " AND EXISTS (SELECT 1 FROM team_member m WHERE m.team_id=e.id AND m.user_id=?)";
        return jdbc.query("SELECT e.id,e.name FROM team e WHERE e.archived_at IS NULL" + escopo + " ORDER BY e.name,e.id",
            (linha, indice) -> new Referencia(linha.getString("id"), linha.getString("name")),
            acesso.global() ? new Object[0] : new Object[]{acesso.usuarioId()});
    }

    public List<Referencia> listarResponsaveisElegiveis(String equipeId) {
        return jdbc.query("""
            SELECT u.id,u.name FROM team_member m JOIN app_user u ON u.id=m.user_id
            JOIN app_profile p ON p.user_id=u.id WHERE m.team_id=? AND p.status='ACTIVE' ORDER BY u.name,u.id
            """, (linha, indice) -> new Referencia(linha.getString("id"), linha.getString("name")), equipeId);
    }

    public void inserir(String id, String titulo, String descricao, Tarefa.Prioridade prioridade, String equipeId,
        String criadoraId, LocalDate prazo, String projetoId) {
        int ordem = jdbc.queryForObject("SELECT COALESCE(MAX(position),-1)+1 FROM task WHERE status='A_FAZER' AND archived_at IS NULL", Integer.class);
        jdbc.update("""
            INSERT INTO task (id,title,description,status,priority,team_id,created_by_id,due_date,position,project_id,version,created_at,updated_at)
            VALUES (?,?,?,'A_FAZER',?,?,?,?,?,?,0,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))
            """, id, titulo, descricao, prioridade.name(), equipeId, criadoraId, prazo, ordem, projetoId);
    }

    public int atualizar(EstadoTarefa atual, String titulo, String descricao, Tarefa.Prioridade prioridade,
        String equipeId, LocalDate prazo, String projetoId) {
        return jdbc.update("""
            UPDATE task SET title=?,description=?,priority=?,team_id=?,due_date=?,project_id=?,version=version+1,
            updated_at=CURRENT_TIMESTAMP(6) WHERE id=? AND version=?
            """, titulo, descricao, prioridade.name(), equipeId, prazo, projetoId, atual.id(), atual.versao());
    }

    public void substituirResponsaveis(String tarefaId, Set<String> ids) {
        jdbc.update("DELETE FROM task_assignee WHERE task_id=?", tarefaId);
        for (String id : ids) jdbc.update("INSERT INTO task_assignee (task_id,user_id) VALUES (?,?)", tarefaId, id);
    }

    public List<String> listarColuna(Tarefa.Status status) {
        return jdbc.queryForList("SELECT id FROM task WHERE status=? AND archived_at IS NULL ORDER BY position,id", String.class, status.name());
    }

    public int mudarStatus(EstadoTarefa atual, Tarefa.Status status) {
        return jdbc.update("UPDATE task SET status=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=? AND version=?",
            status.name(), atual.id(), atual.versao());
    }

    public void renumerarColuna(List<String> ids) {
        for (int ordem = 0; ordem < ids.size(); ordem++) {
            jdbc.update("""
                UPDATE task SET position=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6)
                WHERE id=? AND position<>?
                """, ordem, ids.get(ordem), ordem);
        }
    }

    public int arquivar(EstadoTarefa atual) {
        return jdbc.update("UPDATE task SET archived_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6),version=version+1 WHERE id=? AND version=?",
            atual.id(), atual.versao());
    }

    public TarefaService.ResumoTarefas resumir(Acesso acesso, LocalDate hoje) {
        StringBuilder escopo = new StringBuilder(" WHERE t.archived_at IS NULL AND EXISTS (SELECT 1 FROM task_assignee a WHERE a.task_id=t.id AND a.user_id=?)");
        List<Object> parametros = new ArrayList<>(List.of(hoje, hoje, acesso.usuarioId()));
        limitarEscopo(escopo, parametros, acesso);
        return jdbc.queryForObject("""
            SELECT COUNT(*) total, COALESCE(SUM(t.status='EM_ANDAMENTO'),0) andamento,
            COALESCE(SUM(t.due_date=? AND t.status<>'CONCLUIDA'),0) hoje,
            COALESCE(SUM(t.due_date<? AND t.status<>'CONCLUIDA'),0) atrasadas FROM task t
            """ + escopo, (linha, indice) -> new TarefaService.ResumoTarefas(linha.getLong("total"),
                linha.getLong("andamento"), linha.getLong("hoje"), linha.getLong("atrasadas")), parametros.toArray());
    }
}
