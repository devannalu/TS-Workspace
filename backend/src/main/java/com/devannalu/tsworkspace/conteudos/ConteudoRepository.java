package com.devannalu.tsworkspace.conteudos;

import java.time.*;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConteudoRepository {
    private final JdbcTemplate jdbc;
    public ConteudoRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public enum Canal { INSTAGRAM, LINKEDIN, EMAIL, SITE, INTERNO }
    public enum Formato { POST, CARROSSEL, VIDEO, ARTIGO, EMAIL, TEXTO }
    public enum Status { IDEIA, PLANEJADO, EM_PRODUCAO, EM_REVISAO, APROVADO, PUBLICADO }
    public record Referencia(String id,String nome) { }
    public record Registro(String id,String titulo,String briefing,Canal canal,Formato formato,Status status,
        Referencia equipe,boolean equipeArquivada,Referencia responsavel,LocalDate publicacaoPlanejada,
        String eventoId,String projetoId,Referencia criadaPor,long versao,Instant criadaEm,Instant atualizadaEm,Instant arquivadaEm) { }
    public record Consulta(String sql,List<Object> parametros) { }
    private static final String SELECT="SELECT c.*,e.name equipe_nome,e.archived_at equipe_arquivada,u.name autora_nome,r.name responsavel_nome FROM communication_content c JOIN team e ON e.id=c.team_id JOIN app_user u ON u.id=c.created_by_id LEFT JOIN app_user r ON r.id=c.responsible_id";
    private static Instant instante(java.sql.ResultSet r,String coluna) throws java.sql.SQLException {
        var t=r.getTimestamp(coluna);return t==null?null:t.toInstant();
    }
    private static Registro ler(java.sql.ResultSet r,int indice) throws java.sql.SQLException {
        var data=r.getDate("planned_publication_date");var responsavel=r.getString("responsible_id");
        return new Registro(r.getString("id"),r.getString("title"),r.getString("briefing"),Canal.valueOf(r.getString("channel")),
            Formato.valueOf(r.getString("format")),Status.valueOf(r.getString("status")),new Referencia(r.getString("team_id"),r.getString("equipe_nome")),
            r.getTimestamp("equipe_arquivada")!=null,responsavel==null?null:new Referencia(responsavel,r.getString("responsavel_nome")),
            data==null?null:data.toLocalDate(),r.getString("event_id"),r.getString("project_id"),new Referencia(r.getString("created_by_id"),r.getString("autora_nome")),
            r.getLong("version"),instante(r,"created_at"),instante(r,"updated_at"),instante(r,"archived_at"));
    }
    public Consulta consulta(String pessoa,boolean global,String equipe,Status status,String busca,boolean arquivadas,String responsavel) {
        var sql=new StringBuilder(arquivadas?" WHERE c.archived_at IS NOT NULL":" WHERE c.archived_at IS NULL");var args=new ArrayList<Object>();
        if(!global) {sql.append(" AND EXISTS(SELECT 1 FROM team_member own WHERE own.team_id=c.team_id AND own.user_id=?)");args.add(pessoa);}
        if(equipe!=null) {sql.append(" AND c.team_id=?");args.add(equipe);}
        if(status!=null) {sql.append(" AND c.status=?");args.add(status.name());}
        if(responsavel!=null) {sql.append(" AND c.responsible_id=?");args.add(responsavel);}
        if(busca!=null&&!busca.isBlank()) {
            sql.append(" AND (c.title LIKE ? ESCAPE '!' OR c.briefing LIKE ? ESCAPE '!')");
            var termo="%"+busca.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(termo);args.add(termo);
        }
        return new Consulta(sql.toString(),args);
    }
    public List<Registro> listar(Consulta q,int pagina,int tamanho) {
        var args=new ArrayList<>(q.parametros());args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.query(SELECT+q.sql()+" ORDER BY c.updated_at DESC,c.id LIMIT ? OFFSET ?",ConteudoRepository::ler,args.toArray());
    }
    public long contar(Consulta q) {return jdbc.queryForObject("SELECT COUNT(*) FROM communication_content c"+q.sql(),Long.class,q.parametros().toArray());}
    public Optional<Registro> buscar(String id) {return jdbc.query(SELECT+" WHERE c.id=?",ConteudoRepository::ler,id).stream().findFirst();}
    public void inserir(String id,ConteudoService.Dados d,String pessoa) {
        jdbc.update("INSERT INTO communication_content(id,title,briefing,channel,format,status,team_id,responsible_id,planned_publication_date,event_id,project_id,created_by_id,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,NOW(6),NOW(6))",
            id,d.titulo().trim(),d.briefing(),d.canal().name(),d.formato().name(),d.status().name(),d.equipeId(),d.responsavelId(),d.publicacaoPlanejada(),d.eventoId(),d.projetoId(),pessoa);
    }
    public int atualizar(Registro r,ConteudoService.Dados d) {
        return jdbc.update("UPDATE communication_content SET title=?,briefing=?,channel=?,format=?,status=?,team_id=?,responsible_id=?,planned_publication_date=?,event_id=?,project_id=?,version=version+1,updated_at=NOW(6) WHERE id=? AND version=?",
            d.titulo().trim(),d.briefing(),d.canal().name(),d.formato().name(),d.status().name(),d.equipeId(),d.responsavelId(),d.publicacaoPlanejada(),d.eventoId(),d.projetoId(),r.id(),r.versao());
    }
    public int arquivar(Registro r) {return jdbc.update("UPDATE communication_content SET archived_at=NOW(6),updated_at=NOW(6),version=version+1 WHERE id=? AND version=?",r.id(),r.versao());}
    public List<Referencia> projetos(String equipe) {return jdbc.query("SELECT id,title FROM project WHERE team_id=? AND archived_at IS NULL ORDER BY title,id LIMIT 100",(r,i)->new Referencia(r.getString(1),r.getString(2)),equipe);}
    public List<Referencia> eventos(String equipe) {return jdbc.query("SELECT id,name FROM community_event WHERE team_id=? AND archived_at IS NULL ORDER BY name,id LIMIT 100",(r,i)->new Referencia(r.getString(1),r.getString(2)),equipe);}
    public boolean projetoAtivo(String id,String equipe) {return jdbc.queryForObject("SELECT COUNT(*) FROM project WHERE id=? AND team_id=? AND archived_at IS NULL",Integer.class,id,equipe)==1;}
    public boolean eventoAtivo(String id,String equipe) {return jdbc.queryForObject("SELECT COUNT(*) FROM community_event WHERE id=? AND team_id=? AND archived_at IS NULL",Integer.class,id,equipe)==1;}
    public List<Registro> calendario(Consulta q,LocalDate de,LocalDate ate) {
        var args=new ArrayList<>(q.parametros());args.add(Date.valueOf(de));args.add(Date.valueOf(ate));
        return jdbc.query(SELECT+q.sql()+" AND c.planned_publication_date BETWEEN ? AND ? ORDER BY c.planned_publication_date,c.id",ConteudoRepository::ler,args.toArray());
    }
}
