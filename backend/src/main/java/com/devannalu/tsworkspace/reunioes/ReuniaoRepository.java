package com.devannalu.tsworkspace.reunioes;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ReuniaoRepository {
    private final JdbcTemplate jdbc;
    public ReuniaoRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public enum Tipo { REUNIAO,TALK }
    public enum Status { AGENDADA,REALIZADA,CANCELADA }
    public record Referencia(String id,String nome) { }
    public record Participante(String id,String nome,boolean responsavel) { }
    public record Registro(String id,String titulo,String pauta,Tipo tipo,Status status,Referencia equipe,
        boolean equipeArquivada,Instant inicio,Instant fim,String zona,String local,String link,String resultados,
        Referencia criadaPor,long versao,Instant criadaEm,Instant atualizadaEm,Instant arquivadaEm) { }
    public record Consulta(String sql,List<Object> parametros) { }
    private static final String SELECT="SELECT m.*,e.name equipe_nome,e.archived_at equipe_arquivada,u.name autora_nome FROM meeting m JOIN team e ON e.id=m.team_id JOIN app_user u ON u.id=m.created_by_id";
    private static Instant instante(java.sql.ResultSet r,String campo) throws java.sql.SQLException {
        var valor=r.getTimestamp(campo);return valor==null?null:valor.toInstant();
    }
    private static Registro ler(java.sql.ResultSet r,int indice) throws java.sql.SQLException {
        return new Registro(r.getString("id"),r.getString("title"),r.getString("agenda"),Tipo.valueOf(r.getString("type")),Status.valueOf(r.getString("status")),
            new Referencia(r.getString("team_id"),r.getString("equipe_nome")),r.getTimestamp("equipe_arquivada")!=null,
            instante(r,"start_at"),instante(r,"end_at"),r.getString("zone_id"),r.getString("location"),r.getString("link"),r.getString("results"),
            new Referencia(r.getString("created_by_id"),r.getString("autora_nome")),r.getLong("version"),instante(r,"created_at"),instante(r,"updated_at"),instante(r,"archived_at"));
    }
    public Consulta consulta(String pessoa,boolean global,String equipe,Status status,String busca,boolean arquivadas,String participante) {
        var sql=new StringBuilder(arquivadas?" WHERE m.archived_at IS NOT NULL":" WHERE m.archived_at IS NULL");List<Object> args=new ArrayList<>();
        if(!global){sql.append(" AND EXISTS(SELECT 1 FROM team_member own WHERE own.team_id=m.team_id AND own.user_id=?)");args.add(pessoa);}
        if(equipe!=null){sql.append(" AND m.team_id=?");args.add(equipe);}
        if(status!=null){sql.append(" AND m.status=?");args.add(status.name());}
        if(busca!=null&&!busca.isBlank()){sql.append(" AND (m.title LIKE ? ESCAPE '!' OR m.agenda LIKE ? ESCAPE '!')");String termo="%"+busca.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(termo);args.add(termo);}
        if(participante!=null){sql.append(" AND EXISTS(SELECT 1 FROM meeting_participant p WHERE p.meeting_id=m.id AND p.user_id=?)");args.add(participante);}
        return new Consulta(sql.toString(),args);
    }
    public List<Registro> listar(Consulta consulta,int pagina,int tamanho) {
        var args=new ArrayList<>(consulta.parametros());args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.query(SELECT+consulta.sql()+" ORDER BY m.start_at DESC,m.id LIMIT ? OFFSET ?",ReuniaoRepository::ler,args.toArray());
    }
    public long contar(Consulta consulta) {return jdbc.queryForObject("SELECT COUNT(*) FROM meeting m"+consulta.sql(),Long.class,consulta.parametros().toArray());}
    public Optional<Registro> buscar(String id) {return jdbc.query(SELECT+" WHERE m.id=?",ReuniaoRepository::ler,id).stream().findFirst();}
    public Map<String,List<Participante>> participantes(Collection<String> ids) {
        Map<String,List<Participante>> resultado=new HashMap<>();if(ids.isEmpty())return resultado;
        String marcadores=String.join(",",Collections.nCopies(ids.size(),"?"));
        jdbc.query("SELECT p.meeting_id,u.id,u.name,p.responsible FROM meeting_participant p JOIN app_user u ON u.id=p.user_id WHERE p.meeting_id IN ("+marcadores+") ORDER BY u.name,u.id",
            (org.springframework.jdbc.core.RowCallbackHandler)r->resultado.computeIfAbsent(r.getString("meeting_id"),k->new ArrayList<>()).add(new Participante(r.getString("id"),r.getString("name"),r.getBoolean("responsible"))),ids.toArray());return resultado;
    }
    public void inserir(String id,ReuniaoService.Dados dados,String pessoa,Instant inicio,Instant fim) {
        jdbc.update("INSERT INTO meeting(id,title,agenda,type,team_id,start_at,end_at,zone_id,location,link,results,created_by_id,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,NOW(6),NOW(6))",
            id,dados.titulo().trim(),dados.pauta(),dados.tipo().name(),dados.equipeId(),Timestamp.from(inicio),Timestamp.from(fim),dados.zona(),dados.local(),dados.link(),dados.resultados(),pessoa);
    }
    public int atualizar(Registro atual,ReuniaoService.Dados dados,Instant inicio,Instant fim) {
        return jdbc.update("UPDATE meeting SET title=?,agenda=?,type=?,status=?,team_id=?,start_at=?,end_at=?,zone_id=?,location=?,link=?,results=?,version=version+1,updated_at=NOW(6) WHERE id=? AND version=?",
            dados.titulo().trim(),dados.pauta(),dados.tipo().name(),dados.status().name(),dados.equipeId(),Timestamp.from(inicio),Timestamp.from(fim),dados.zona(),dados.local(),dados.link(),dados.resultados(),atual.id(),atual.versao());
    }
    public void substituirParticipantes(String id,Set<String> pessoas,Set<String> responsaveis) {
        jdbc.update("DELETE FROM meeting_participant WHERE meeting_id=?",id);
        for(String pessoa:pessoas)jdbc.update("INSERT INTO meeting_participant(meeting_id,user_id,responsible) VALUES(?,?,?)",id,pessoa,responsaveis.contains(pessoa));
    }
    public int arquivar(Registro atual) {return jdbc.update("UPDATE meeting SET archived_at=NOW(6),updated_at=NOW(6),version=version+1 WHERE id=? AND version=?",atual.id(),atual.versao());}
    public List<Registro> calendario(Consulta consulta,Instant de,Instant ate) {
        var args=new ArrayList<>(consulta.parametros());args.add(Timestamp.from(ate));args.add(Timestamp.from(de));
        return jdbc.query(SELECT+consulta.sql()+" AND m.start_at<? AND m.end_at>? ORDER BY m.start_at,m.id",ReuniaoRepository::ler,args.toArray());
    }
}
