package com.devannalu.tsworkspace.tarefas;

import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChecklistRepository {
    private final JdbcTemplate jdbc;
    public ChecklistRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Item(String id, String texto, boolean concluido, int ordem, long versao,
        TarefaRepository.Referencia criadaPor, Instant criadaEm, Instant atualizadaEm) { }
    public void bloquearTarefa(String id) {
        var ids=jdbc.queryForList("SELECT id FROM task WHERE id=? FOR UPDATE", String.class, id);
        if(ids.isEmpty())throw com.devannalu.tsworkspace.compartilhado.ProblemaDominio.naoEncontrado("Tarefa não encontrada.");
    }
    public List<Item> listar(String tarefa) {
        return jdbc.query("SELECT c.*,u.name FROM task_checklist c JOIN app_user u ON u.id=c.created_by_id WHERE c.task_id=? AND c.deleted_at IS NULL ORDER BY c.position,c.id",
            (r,i) -> new Item(r.getString("id"),r.getString("text"),r.getBoolean("completed"),r.getInt("position"),r.getLong("version"),
                new TarefaRepository.Referencia(r.getString("created_by_id"),r.getString("name")),
                r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),tarefa);
    }
    public void inserir(String id,String tarefa,String texto,String autora,int ordem) {
        jdbc.update("INSERT INTO task_checklist(id,task_id,text,position,created_by_id,created_at,updated_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",id,tarefa,texto,ordem,autora);
    }
    public void editar(String id,String texto,boolean concluido) {
        jdbc.update("UPDATE task_checklist SET text=?,completed=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",texto,concluido,id);
    }
    public void ordenar(String id,int ordem) {
        jdbc.update("UPDATE task_checklist SET position=?,version=version+1,updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",ordem,id);
    }
    public void remover(String id) {
        jdbc.update("UPDATE task_checklist SET deleted_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6),version=version+1 WHERE id=?",id);
    }
}
