package com.devannalu.tsworkspace.chat;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {
    private final JdbcTemplate jdbc;
    public ChatRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Canal(String id,String nome,boolean arquivado) { }
    public record Pessoa(String id,String nome) { }
    public record Mensagem(String id,String equipeId,Pessoa autora,String texto,long versao,Instant criadaEm,Instant atualizadaEm,boolean removida) { }
    private static final String SELECT="SELECT m.*,u.name autora_nome FROM chat_message m JOIN app_user u ON u.id=m.author_id";
    private Mensagem ler(ResultSet r,int indice) throws SQLException {
        return new Mensagem(r.getString("id"),r.getString("team_id"),new Pessoa(r.getString("author_id"),r.getString("autora_nome")),
            r.getString("content"),r.getLong("version"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant(),r.getTimestamp("removed_at")!=null);
    }
    public List<Canal> canais(String pessoa,boolean global) {
        String escopo=global?"":" WHERE EXISTS(SELECT 1 FROM team_member m WHERE m.team_id=e.id AND m.user_id=?)";
        return jdbc.query("SELECT e.id,e.name,e.archived_at FROM team e"+escopo+" ORDER BY e.archived_at IS NOT NULL,e.name,e.id",
            (r,i)->new Canal(r.getString(1),r.getString(2),r.getTimestamp(3)!=null),global?new Object[0]:new Object[]{pessoa});
    }
    public Optional<Canal> canal(String id) {
        return jdbc.query("SELECT id,name,archived_at FROM team WHERE id=?",(r,i)->new Canal(r.getString(1),r.getString(2),r.getTimestamp(3)!=null),id).stream().findFirst();
    }
    public boolean integrante(String pessoa,String equipe) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE user_id=? AND team_id=?",Integer.class,pessoa,equipe)==1;
    }
    public Optional<Mensagem> buscar(String id) {return jdbc.query(SELECT+" WHERE m.id=?",this::ler,id).stream().findFirst();}
    public List<Mensagem> listar(String equipe,Mensagem antes,int tamanho) {
        var sql=new StringBuilder(equipe==null?" WHERE m.team_id IS NULL":" WHERE m.team_id=?");var args=new ArrayList<Object>();
        if(equipe!=null)args.add(equipe);
        if(antes!=null) {
            sql.append(" AND (m.created_at<? OR (m.created_at=? AND m.id<?))");
            args.add(Timestamp.from(antes.criadaEm()));args.add(Timestamp.from(antes.criadaEm()));args.add(antes.id());
        }
        args.add(tamanho);return jdbc.query(SELECT+sql+" ORDER BY m.created_at DESC,m.id DESC LIMIT ?",this::ler,args.toArray());
    }
    public void inserir(String id,String equipe,String pessoa,String texto) {
        jdbc.update("INSERT INTO chat_message(id,team_id,author_id,content,created_at,updated_at) VALUES(?,?,?,?,NOW(6),NOW(6))",id,equipe,pessoa,texto);
    }
    public int editar(String id,long versao,String texto) {
        return jdbc.update("UPDATE chat_message SET content=?,version=version+1,updated_at=NOW(6) WHERE id=? AND version=? AND removed_at IS NULL",texto,id,versao);
    }
    public int remover(String id,long versao,String pessoa) {
        return jdbc.update("UPDATE chat_message SET removed_at=NOW(6),removed_by_id=?,updated_at=NOW(6),version=version+1 WHERE id=? AND version=? AND removed_at IS NULL",pessoa,id,versao);
    }
}
