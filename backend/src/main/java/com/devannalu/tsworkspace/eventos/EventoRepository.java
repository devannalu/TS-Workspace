package com.devannalu.tsworkspace.eventos;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EventoRepository {
    private final JdbcTemplate jdbc;
    public EventoRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public enum Formato { PRESENCIAL,ONLINE,HIBRIDO }
    public enum Status { PLANEJADO,CONFIRMADO,EM_ANDAMENTO,CONCLUIDO,CANCELADO }
    public record Referencia(String id,String nome) { }
    public record Responsavel(String id,String nome) { }
    public record Registro(String id,String nome,String descricao,Formato formato,Status status,Referencia equipe,
        boolean equipeArquivada,Instant inicio,Instant fim,String zona,String local,String link,String notas,
        Referencia criadaPor,long versao,Instant criadaEm,Instant atualizadaEm,Instant arquivadaEm) { }
    public record Consulta(String sql,List<Object> parametros) { }
    private static final String SELECT="SELECT m.*,e.name equipe_nome,e.archived_at equipe_arquivada,u.name autora_nome FROM community_event m JOIN team e ON e.id=m.team_id JOIN app_user u ON u.id=m.created_by_id";
    private static Instant instante(java.sql.ResultSet r,String campo) throws java.sql.SQLException {
        var valor=r.getTimestamp(campo);return valor==null?null:valor.toInstant();
    }
    private static Registro ler(java.sql.ResultSet r,int indice) throws java.sql.SQLException {
        return new Registro(r.getString("id"),r.getString("name"),r.getString("description"),Formato.valueOf(r.getString("format")),Status.valueOf(r.getString("status")),
            new Referencia(r.getString("team_id"),r.getString("equipe_nome")),r.getTimestamp("equipe_arquivada")!=null,
            instante(r,"start_at"),instante(r,"end_at"),r.getString("zone_id"),r.getString("location"),r.getString("link"),r.getString("notes"),
            new Referencia(r.getString("created_by_id"),r.getString("autora_nome")),r.getLong("version"),instante(r,"created_at"),instante(r,"updated_at"),instante(r,"archived_at"));
    }
    public Consulta consulta(String pessoa,boolean global,String equipe,Status status,String busca,boolean arquivadas,String responsavel) {
        var sql=new StringBuilder(arquivadas?" WHERE m.archived_at IS NOT NULL":" WHERE m.archived_at IS NULL");List<Object> args=new ArrayList<>();
        if(!global){sql.append(" AND EXISTS(SELECT 1 FROM team_member own WHERE own.team_id=m.team_id AND own.user_id=?)");args.add(pessoa);}
        if(equipe!=null){sql.append(" AND m.team_id=?");args.add(equipe);}
        if(status!=null){sql.append(" AND m.status=?");args.add(status.name());}
        if(busca!=null&&!busca.isBlank()){sql.append(" AND (m.name LIKE ? ESCAPE '!' OR m.description LIKE ? ESCAPE '!')");String termo="%"+busca.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(termo);args.add(termo);}
        if(responsavel!=null){sql.append(" AND EXISTS(SELECT 1 FROM community_event_responsible p WHERE p.event_id=m.id AND p.user_id=?)");args.add(responsavel);}
        return new Consulta(sql.toString(),args);
    }
    public List<Registro> listar(Consulta consulta,int pagina,int tamanho) {
        var args=new ArrayList<>(consulta.parametros());args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.query(SELECT+consulta.sql()+" ORDER BY m.start_at DESC,m.id LIMIT ? OFFSET ?",EventoRepository::ler,args.toArray());
    }
    public long contar(Consulta consulta) {return jdbc.queryForObject("SELECT COUNT(*) FROM community_event m"+consulta.sql(),Long.class,consulta.parametros().toArray());}
    public Optional<Registro> buscar(String id) {return jdbc.query(SELECT+" WHERE m.id=?",EventoRepository::ler,id).stream().findFirst();}
    public Map<String,List<Responsavel>> responsaveis(Collection<String> ids) {
        Map<String,List<Responsavel>> resultado=new HashMap<>();if(ids.isEmpty())return resultado;
        String marcadores=String.join(",",Collections.nCopies(ids.size(),"?"));
        jdbc.query("SELECT p.event_id,u.id,u.name FROM community_event_responsible p JOIN app_user u ON u.id=p.user_id WHERE p.event_id IN ("+marcadores+") ORDER BY u.name,u.id",
            (org.springframework.jdbc.core.RowCallbackHandler)r->resultado.computeIfAbsent(r.getString("event_id"),k->new ArrayList<>()).add(new Responsavel(r.getString("id"),r.getString("name"))),ids.toArray());return resultado;
    }
    public void inserir(String id,EventoService.Dados dados,String pessoa,Instant inicio,Instant fim) {
        jdbc.update("INSERT INTO community_event(id,name,description,format,team_id,start_at,end_at,zone_id,location,link,notes,created_by_id,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,NOW(6),NOW(6))",
            id,dados.nome().trim(),dados.descricao(),dados.formato().name(),dados.equipeId(),Timestamp.from(inicio),Timestamp.from(fim),dados.zona(),dados.local(),dados.link(),dados.notas(),pessoa);
    }
    public int atualizar(Registro atual,EventoService.Dados dados,Instant inicio,Instant fim) {
        return jdbc.update("UPDATE community_event SET name=?,description=?,format=?,status=?,team_id=?,start_at=?,end_at=?,zone_id=?,location=?,link=?,notes=?,version=version+1,updated_at=NOW(6) WHERE id=? AND version=?",
            dados.nome().trim(),dados.descricao(),dados.formato().name(),dados.status().name(),dados.equipeId(),Timestamp.from(inicio),Timestamp.from(fim),dados.zona(),dados.local(),dados.link(),dados.notas(),atual.id(),atual.versao());
    }
    public void substituirResponsaveis(String id,Set<String> pessoas) {
        jdbc.update("DELETE FROM community_event_responsible WHERE event_id=?",id);
        for(String pessoa:pessoas)jdbc.update("INSERT INTO community_event_responsible(event_id,user_id) VALUES(?,?)",id,pessoa);
    }
    public int arquivar(Registro atual) {return jdbc.update("UPDATE community_event SET archived_at=NOW(6),updated_at=NOW(6),version=version+1 WHERE id=? AND version=?",atual.id(),atual.versao());}
    public List<Registro> calendario(Consulta consulta,Instant de,Instant ate) {
        var args=new ArrayList<>(consulta.parametros());args.add(Timestamp.from(ate));args.add(Timestamp.from(de));
        return jdbc.query(SELECT+consulta.sql()+" AND m.start_at<? AND m.end_at>? ORDER BY m.start_at,m.id",EventoRepository::ler,args.toArray());
    }
}
