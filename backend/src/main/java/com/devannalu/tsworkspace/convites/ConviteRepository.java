package com.devannalu.tsworkspace.convites;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import com.devannalu.tsworkspace.convites.ConviteService.*;
import com.devannalu.tsworkspace.usuarios.UsuarioService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ConviteRepository {
    private final JdbcTemplate jdbc;

    public ConviteRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record EstadoConvite(String id,String email,String perfilAcessoId,Instant expiraEm,Instant utilizadoEm,Instant canceladoEm) { }

    private static Instant lerInstante(java.sql.ResultSet linha,String coluna) throws java.sql.SQLException { var valorColuna=linha.getTimestamp(coluna);return valorColuna==null?null:valorColuna.toInstant(); }

    public List<ConviteResponse> buscarConvites(String sufixoSql,Object...parametros) {
        var convitesEncontrados=jdbc.query("SELECT i.*,r.role_key,r.name role_name,u.name actor_name FROM invite i JOIN roles r ON r.id=i.role_id JOIN app_user u ON u.id=i.invited_by_id "+sufixoSql,(linha,indiceLinha)->new ConviteResponse(linha.getString("id"),linha.getString("email"),new UsuarioService.PerfilAcesso(linha.getString("role_id"),linha.getString("role_key"),linha.getString("role_name")),List.of(),new AutorConvite(linha.getString("invited_by_id"),linha.getString("actor_name")),lerInstante(linha,"created_at"),lerInstante(linha,"expires_at"),PoliticaConvite.calcularSituacao(lerInstante(linha,"used_at"),lerInstante(linha,"cancelled_at"),lerInstante(linha,"expires_at"),Instant.now())),parametros);
        if(convitesEncontrados.isEmpty())return convitesEncontrados;
        Map<String,List<UsuarioService.EquipeReferencia>> equipesPorConvite=new HashMap<>();
        jdbc.query("SELECT it.invite_id,t.id,t.name FROM invite_team it JOIN team t ON t.id=it.team_id WHERE it.invite_id IN ("+String.join(",",Collections.nCopies(convitesEncontrados.size(),"?"))+") ORDER BY t.name,t.id",
            (org.springframework.jdbc.core.RowCallbackHandler) linha -> equipesPorConvite.computeIfAbsent(linha.getString("invite_id"),k->new ArrayList<>()).add(new UsuarioService.EquipeReferencia(linha.getString("id"),linha.getString("name"))),convitesEncontrados.stream().map(ConviteResponse::id).toArray());
        return convitesEncontrados.stream().map(i->new ConviteResponse(i.id(),i.email(),i.role(),List.copyOf(equipesPorConvite.getOrDefault(i.id(),List.of())),i.invitedBy(),i.createdAt(),i.expiresAt(),i.status())).toList();
    }

    public EstadoConvite buscarEstadoConvite(String coluna,String valorColuna,boolean bloquear) {
        // A coluna vem de constantes internas, nunca da requisição.
        var estadosEncontrados=jdbc.query("SELECT id,email,role_id,expires_at,used_at,cancelled_at FROM invite WHERE "+coluna+"=?"+(bloquear?" FOR UPDATE":""),
            (linha,indiceLinha)->new EstadoConvite(linha.getString("id"),linha.getString("email"),linha.getString("role_id"),lerInstante(linha,"expires_at"),lerInstante(linha,"used_at"),lerInstante(linha,"cancelled_at")),valorColuna);
        return estadosEncontrados.isEmpty()?null:estadosEncontrados.get(0);
    }

    public List<String> buscarEquipesConvite(String id) { return jdbc.queryForList("SELECT team_id FROM invite_team WHERE invite_id=?",String.class,id); }

    public Long contarConvites() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM invite",Long.class);
    }

    public Long contarConvitesPendentes() {
        return jdbc.queryForObject("""
            SELECT COUNT(*) FROM invite
            WHERE used_at IS NULL AND cancelled_at IS NULL AND expires_at>CURRENT_TIMESTAMP(6)
            """, Long.class);
    }

    public Long contarUsuariosPorEmail(String email) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_user WHERE email=?",Long.class,email);
    }

    public Long contarConvitesPendentesPorEmail(String email) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM invite WHERE email=? AND used_at IS NULL AND cancelled_at IS NULL AND expires_at>CURRENT_TIMESTAMP(6)",Long.class,email);
    }

    public Long contarPerfilAcesso(String perfilAcessoId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM roles WHERE id=?",Long.class,perfilAcessoId);
    }

    public int inserirConvite(String id, String email, String hashToken, Instant expiraEm, String atorId, String perfilAcessoId, Instant agora) {
        return jdbc.update("INSERT INTO invite (id,email,token_hash,expires_at,invited_by_id,role_id,created_at,updated_at) VALUES (?,?,?,?,?,?,?,?)",id,email,hashToken,Timestamp.from(expiraEm),atorId,perfilAcessoId,Timestamp.from(agora),Timestamp.from(agora));
    }

    public int inserirEquipeConvite(String id, String equipeId, Instant agora) {
        return jdbc.update("INSERT INTO invite_team VALUES (?,?,?)",id,equipeId,Timestamp.from(agora));
    }

    public int cancelarConvite(String id) {
        return jdbc.update("UPDATE invite SET cancelled_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",id);
    }

    public Long contarEquipeAtiva(String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE id=? AND archived_at IS NULL",Long.class,id);
    }

    public int inserirUsuario(String id, String nome, String email, String hashSenha) {
        return jdbc.update("INSERT INTO app_user (id,name,email,password_hash,created_at,updated_at) VALUES (?,?,?,?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",id,nome.trim(),email,hashSenha);
    }

    public int inserirPerfil(String id, String perfilAcessoId) {
        return jdbc.update("INSERT INTO app_profile (user_id,status,role_id,created_at,updated_at) VALUES (?,'ACTIVE',?,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6))",id,perfilAcessoId);
    }

    public int inserirIntegrante(String id, String equipeId) {
        return jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",id,equipeId);
    }

    public int marcarConviteUtilizado(String id) {
        return jdbc.update("UPDATE invite SET used_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE id=?",id);
    }
}
