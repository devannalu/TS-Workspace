package com.devannalu.tsworkspace.parcerias;

import java.time.*;
import java.sql.Timestamp;
import java.sql.Date;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ParceriaRepository {
    private final JdbcTemplate jdbc;
    public ParceriaRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public enum Status { PROSPECCAO, CONTATO, NEGOCIACAO, CONFIRMADA, ENCERRADA }
    public record Referencia(String id,String nome) { }
    public record Registro(String id,String organizacao,String descricao,Status status,
        Referencia equipe,boolean equipeArquivada,Referencia responsavel,LocalDate dataProximaAcao,
        String contatoPrincipal,String email,String telefone,String proximaAcao,String observacoes,String eventoId,String projetoId,Referencia criadaPor,long versao,Instant criadaEm,Instant atualizadaEm,Instant arquivadaEm) { }
    public record Consulta(String sql,List<Object> parametros) { }
    private static final String SELECT="SELECT c.*,e.name equipe_nome,e.archived_at equipe_arquivada,u.name autora_nome,r.name responsavel_nome FROM partnership c JOIN team e ON e.id=c.team_id JOIN app_user u ON u.id=c.created_by_id LEFT JOIN app_user r ON r.id=c.responsible_id";
    private static Instant instante(java.sql.ResultSet r,String coluna) throws java.sql.SQLException {
        var t=r.getTimestamp(coluna);return t==null?null:t.toInstant();
    }
    private static Registro ler(java.sql.ResultSet r,int indice) throws java.sql.SQLException {
        var data=r.getDate("next_action_date");var responsavel=r.getString("responsible_id");
        return new Registro(r.getString("id"),r.getString("organization"),r.getString("description"),Status.valueOf(r.getString("status")),new Referencia(r.getString("team_id"),r.getString("equipe_nome")),
            r.getTimestamp("equipe_arquivada")!=null,responsavel==null?null:new Referencia(responsavel,r.getString("responsavel_nome")),
            data==null?null:data.toLocalDate(),r.getString("contact_name"),r.getString("contact_email"),r.getString("contact_phone"),r.getString("next_action"),r.getString("notes"),r.getString("event_id"),r.getString("project_id"),new Referencia(r.getString("created_by_id"),r.getString("autora_nome")),
            r.getLong("version"),instante(r,"created_at"),instante(r,"updated_at"),instante(r,"archived_at"));
    }
    public Consulta consulta(String pessoa,boolean global,String equipe,Status status,String busca,boolean arquivadas,String responsavel) {
        var sql=new StringBuilder(arquivadas?" WHERE c.archived_at IS NOT NULL":" WHERE c.archived_at IS NULL");var args=new ArrayList<Object>();
        if(!global) {sql.append(" AND EXISTS(SELECT 1 FROM team_member own WHERE own.team_id=c.team_id AND own.user_id=?)");args.add(pessoa);}
        if(equipe!=null) {sql.append(" AND c.team_id=?");args.add(equipe);}
        if(status!=null) {sql.append(" AND c.status=?");args.add(status.name());}
        if(responsavel!=null) {sql.append(" AND c.responsible_id=?");args.add(responsavel);}
        if(busca!=null&&!busca.isBlank()) {
            sql.append(" AND (c.organization LIKE ? ESCAPE '!' OR c.description LIKE ? ESCAPE '!')");
            var termo="%"+busca.trim().replace("!","!!").replace("%","!%").replace("_","!_")+"%";args.add(termo);args.add(termo);
        }
        return new Consulta(sql.toString(),args);
    }
    public List<Registro> listar(Consulta q,int pagina,int tamanho) {
        var args=new ArrayList<>(q.parametros());args.add(tamanho);args.add(pagina*tamanho);
        return jdbc.query(SELECT+q.sql()+" ORDER BY c.updated_at DESC,c.id LIMIT ? OFFSET ?",ParceriaRepository::ler,args.toArray());
    }
    public long contar(Consulta q) {return jdbc.queryForObject("SELECT COUNT(*) FROM partnership c"+q.sql(),Long.class,q.parametros().toArray());}
    public Optional<Registro> buscar(String id) {return jdbc.query(SELECT+" WHERE c.id=?",ParceriaRepository::ler,id).stream().findFirst();}
    public void inserir(String id,ParceriaService.Dados d,String pessoa) {
        jdbc.update("INSERT INTO partnership(id,organization,description,status,team_id,responsible_id,next_action_date,contact_name,contact_email,contact_phone,next_action,notes,event_id,project_id,created_by_id,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(6),NOW(6))",
            id,d.organizacao().trim(),d.descricao(),d.status().name(),d.equipeId(),d.responsavelId(),d.dataProximaAcao(),d.contatoPrincipal(),d.email(),d.telefone(),d.proximaAcao(),d.observacoes(),d.eventoId(),d.projetoId(),pessoa);
    }
    public int atualizar(Registro r,ParceriaService.Dados d) {
        return jdbc.update("UPDATE partnership SET organization=?,description=?,status=?,team_id=?,responsible_id=?,next_action_date=?,contact_name=?,contact_email=?,contact_phone=?,next_action=?,notes=?,event_id=?,project_id=?,version=version+1,updated_at=NOW(6) WHERE id=? AND version=?",
            d.organizacao().trim(),d.descricao(),d.status().name(),d.equipeId(),d.responsavelId(),d.dataProximaAcao(),d.contatoPrincipal(),d.email(),d.telefone(),d.proximaAcao(),d.observacoes(),d.eventoId(),d.projetoId(),r.id(),r.versao());
    }
    public int arquivar(Registro r) {return jdbc.update("UPDATE partnership SET archived_at=NOW(6),updated_at=NOW(6),version=version+1 WHERE id=? AND version=?",r.id(),r.versao());}
    public List<Referencia> projetos(String equipe) {return jdbc.query("SELECT id,title FROM project WHERE team_id=? AND archived_at IS NULL ORDER BY title,id LIMIT 100",(r,i)->new Referencia(r.getString(1),r.getString(2)),equipe);}
    public List<Referencia> eventos(String equipe) {return jdbc.query("SELECT id,name FROM community_event WHERE team_id=? AND archived_at IS NULL ORDER BY name,id LIMIT 100",(r,i)->new Referencia(r.getString(1),r.getString(2)),equipe);}
    public boolean projetoAtivo(String id,String equipe) {return jdbc.queryForObject("SELECT COUNT(*) FROM project WHERE id=? AND team_id=? AND archived_at IS NULL",Integer.class,id,equipe)==1;}
    public boolean eventoAtivo(String id,String equipe) {return jdbc.queryForObject("SELECT COUNT(*) FROM community_event WHERE id=? AND team_id=? AND archived_at IS NULL",Integer.class,id,equipe)==1;}
    public List<Registro> calendario(Consulta q,LocalDate de,LocalDate ate) {
        var args=new ArrayList<>(q.parametros());args.add(Date.valueOf(de));args.add(Date.valueOf(ate));
        return jdbc.query(SELECT+q.sql()+" AND c.next_action_date BETWEEN ? AND ? ORDER BY c.next_action_date,c.id",ParceriaRepository::ler,args.toArray());
    }
}
