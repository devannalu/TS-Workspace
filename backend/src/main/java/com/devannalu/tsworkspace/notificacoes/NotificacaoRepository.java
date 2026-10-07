package com.devannalu.tsworkspace.notificacoes;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class NotificacaoRepository {
    private final JdbcTemplate jdbc;
    public NotificacaoRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Item(String id, String tipo, String recursoId, String titulo, String motivo, Instant criadaEm, boolean lida) { }
    public record Consulta(String sql, List<Object> parametros) { }
    public void inserir(String destinataria, String tipo, String recurso, String motivo, String chave) {
        jdbc.update("INSERT IGNORE INTO notification(id,recipient_id,source_type,resource_id,kind,event_key,created_at) VALUES(?,?,?,?,?,?,CURRENT_TIMESTAMP(6))",
            UUID.randomUUID().toString(), destinataria, tipo, recurso, motivo, chave);
    }
    public List<String> responsaveis(String tipo, String id) {
        return jdbc.queryForList(tipo.equals("TAREFA") ? "SELECT user_id FROM task_assignee WHERE task_id=?" :
            "SELECT user_id FROM project_responsible WHERE project_id=?", String.class, id);
    }
    public Consulta consulta(String pessoa, boolean global, boolean tarefas, boolean projetos) {
        // A projeção revalida acesso atual; um aviso antigo nunca mantém acesso revogado.
        String fonte = " FROM notification n LEFT JOIN task t ON n.source_type='TAREFA' AND t.id=n.resource_id LEFT JOIN project p ON n.source_type='PROJETO' AND p.id=n.resource_id"
            + " WHERE n.recipient_id=? AND ((n.source_type='TAREFA' AND ? AND t.archived_at IS NULL AND t.id IS NOT NULL) OR (n.source_type='PROJETO' AND ? AND p.archived_at IS NULL AND p.id IS NOT NULL))"
            + " AND (? OR EXISTS(SELECT 1 FROM team_member m WHERE m.user_id=? AND m.team_id=COALESCE(t.team_id,p.team_id)))";
        return new Consulta(fonte,List.of(pessoa,tarefas,projetos,global,pessoa));
    }
    public List<Item> listar(Consulta consulta, int pagina, int tamanho) {
        var parametros=new ArrayList<>(consulta.parametros());parametros.add(tamanho);parametros.add(pagina*tamanho);
        return jdbc.query("SELECT n.*,COALESCE(t.title,p.title) titulo"+consulta.sql()+" ORDER BY n.created_at DESC,n.id DESC LIMIT ? OFFSET ?",
            (r,i)->new Item(r.getString("id"),r.getString("source_type"),r.getString("resource_id"),r.getString("titulo"),
                r.getString("kind"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("read_at")!=null),parametros.toArray());
    }
    public long contar(Consulta consulta, boolean naoLidas) {
        return jdbc.queryForObject("SELECT COUNT(*)"+consulta.sql()+(naoLidas?" AND n.read_at IS NULL":""),Long.class,consulta.parametros().toArray());
    }
    public boolean pertence(String id,String pessoa) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM notification WHERE id=? AND recipient_id=?)",Boolean.class,id,pessoa));
    }
    public void ler(String pessoa,String id) {
        jdbc.update("UPDATE notification SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP(6)) WHERE recipient_id=?"+(id==null?"":" AND id=?"),
            id==null?new Object[]{pessoa}:new Object[]{pessoa,id});
    }
    public record Prazo(String tipo,String id,LocalDate data) { }
    public List<Prazo> prazos(String pessoa,LocalDate limite) {
        return jdbc.query("SELECT 'TAREFA' tipo,t.id,t.due_date prazo FROM task t JOIN task_assignee a ON a.task_id=t.id WHERE a.user_id=? AND t.archived_at IS NULL AND t.status<>'CONCLUIDA' AND t.due_date<=?"
            +" UNION ALL SELECT 'PROJETO',p.id,p.end_date FROM project p JOIN project_responsible a ON a.project_id=p.id WHERE a.user_id=? AND p.archived_at IS NULL AND p.status<>'CONCLUIDO' AND p.end_date<=?",
            (r,i)->new Prazo(r.getString("tipo"),r.getString("id"),r.getObject("prazo",LocalDate.class)),pessoa,limite,pessoa,limite);
    }
}
