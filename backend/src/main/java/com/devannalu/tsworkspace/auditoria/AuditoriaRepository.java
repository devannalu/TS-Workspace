package com.devannalu.tsworkspace.auditoria;

import java.util.*;
import java.time.Instant;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuditoriaRepository {
    public static final Set<String> ACOES=Set.of("attachment.created","attachment.removed","chat.message_removed","chat.message_updated","comment.created","comment.removed","comment.updated","content.archived","content.created","content.status_changed","content.updated","event.archived","event.created","event.status_changed","event.updated","invite.accepted","invite.cancelled","invite.created","meeting.archived","meeting.created","meeting.status_changed","meeting.updated","note.created","note.removed","note.updated","partnerships.archived","partnerships.created","partnerships.status_changed","partnerships.updated","project.archived","project.created","project.responsibles_changed","project.status_changed","project.updated","task.archived","task.assignees_changed","task.created","task.status_changed","task.updated","team.archived","team.created","team.member_added","team.member_removed","team.updated","user.role_changed","user.status_changed","user.teams_changed");
    public static final Set<String> TIPOS=Set.of("User","Invite","Team","Task","Project","Comment","Attachment","Meeting","Event","Content","Partnership","Note","ChatMessage");
    private final JdbcTemplate jdbc;
    public AuditoriaRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Autora(String id,String nome) { }
    public record Evento(String id,Autora autora,String acao,String tipo,String recursoId,Instant ocorridoEm) { }
    public record Filtros(String autora,String acao,String tipo,Instant desde,Instant ate) { }
    private record Consulta(String sql,List<Object> parametros) { }
    private static final String SELECT="SELECT a.id,a.actor_id,u.name,a.action,a.entity_type,a.entity_id,a.created_at FROM audit_log a LEFT JOIN app_user u ON u.id=a.actor_id";
    private static String identificador(String valor) {
        if(valor==null)return null;
        try {return UUID.fromString(valor).toString();}catch(IllegalArgumentException e){return null;}
    }
    private static String chave(String valor,Set<String> catalogo) {return catalogo.contains(valor)?valor:"UNKNOWN";}
    private Evento ler(java.sql.ResultSet r,int indice) throws java.sql.SQLException {
        String autora=r.getString("actor_id");
        return new Evento(r.getString("id"),autora==null?null:new Autora(autora,r.getString("name")),chave(r.getString("action"),ACOES),
            chave(r.getString("entity_type"),TIPOS),identificador(r.getString("entity_id")),r.getTimestamp("created_at").toInstant());
    }
    private Consulta consulta(Filtros filtro) {
        var sql=new StringBuilder(" WHERE a.created_at>=? AND a.created_at<?");var args=new ArrayList<Object>();
        args.add(Timestamp.from(filtro.desde()));args.add(Timestamp.from(filtro.ate()));
        if(filtro.autora()!=null&&!filtro.autora().isBlank()) {sql.append(" AND u.name LIKE ? ESCAPE '!'");args.add("%"+filtro.autora().trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%");}
        if(filtro.acao()!=null) {sql.append(" AND a.action=?");args.add(filtro.acao());}
        if(filtro.tipo()!=null) {sql.append(" AND a.entity_type=?");args.add(filtro.tipo());}
        return new Consulta(sql.toString(),args);
    }
    public List<Evento> listar(Filtros filtro,int pagina,int tamanho) {
        var q=consulta(filtro);var args=new ArrayList<>(q.parametros());args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.query(SELECT+q.sql()+" ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",this::ler,args.toArray());
    }
    public long contar(Filtros filtro) {var q=consulta(filtro);return jdbc.queryForObject("SELECT COUNT(*) FROM audit_log a LEFT JOIN app_user u ON u.id=a.actor_id"+q.sql(),Long.class,q.parametros().toArray());}
    public Optional<Evento> buscar(String id) {return jdbc.query(SELECT+" WHERE a.id=?",this::ler,id).stream().findFirst();}
    // Metadados livres são proibidos para evitar credenciais e tokens na auditoria.
    @Transactional(propagation=Propagation.MANDATORY)
    public void registrar(String autorId,String acao,String tipoEntidade,String entidadeId) {
        jdbc.update("INSERT INTO audit_log (id,actor_id,action,entity_type,entity_id,created_at) VALUES (?,?,?,?,?,CURRENT_TIMESTAMP(6))",
            UUID.randomUUID().toString(),autorId,acao,tipoEntidade,entidadeId);
    }
}
