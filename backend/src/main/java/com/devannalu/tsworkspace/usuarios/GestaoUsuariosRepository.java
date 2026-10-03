package com.devannalu.tsworkspace.usuarios;

import java.util.*;
import com.devannalu.tsworkspace.usuarios.UsuarioService.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class GestaoUsuariosRepository {
    private final JdbcTemplate jdbc;

    public GestaoUsuariosRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SQL_USUARIOS="SELECT u.id,u.name,u.email,u.created_at,p.job_title,p.status,r.id role_id,r.role_key,r.name role_name FROM app_user u JOIN app_profile p ON p.user_id=u.id JOIN roles r ON r.id=p.role_id";
    private static UsuarioResponse montarUsuario(java.sql.ResultSet linha,int indiceLinha) throws java.sql.SQLException {
        return new UsuarioResponse(linha.getString("id"),linha.getString("name"),linha.getString("email"),linha.getString("job_title"),linha.getString("status"),
            new PerfilAcesso(linha.getString("role_id"),linha.getString("role_key"),linha.getString("role_name")),List.of(),linha.getTimestamp("created_at").toInstant());
    }
    private List<UsuarioResponse> incluirEquipes(List<UsuarioResponse> usuarios) {
        if(usuarios.isEmpty())return usuarios;
        Map<String,List<EquipeReferencia>> equipesPorUsuario=new HashMap<>();
        var ids=usuarios.stream().map(UsuarioResponse::id).toList();
        jdbc.query("SELECT tm.user_id,t.id,t.name FROM team_member tm JOIN team t ON t.id=tm.team_id WHERE t.archived_at IS NULL AND tm.user_id IN ("+String.join(",",Collections.nCopies(ids.size(),"?"))+") ORDER BY t.name,t.id",
            (org.springframework.jdbc.core.RowCallbackHandler) linha -> equipesPorUsuario.computeIfAbsent(linha.getString("user_id"),k->new ArrayList<>()).add(new EquipeReferencia(linha.getString("id"),linha.getString("name"))),ids.toArray());
        return usuarios.stream().map(u->new UsuarioResponse(u.id(),u.name(),u.email(),u.jobTitle(),u.status(),u.role(),List.copyOf(equipesPorUsuario.getOrDefault(u.id(),List.of())),u.createdAt())).toList();
    }

    public PaginaUsuarios listarUsuarios(int pagina,int tamanhoPagina,String status,String filtroPerfilAcessoId,String equipe,String busca) {
        List<Object> parametros=new ArrayList<>(); String filtrosSql=" WHERE 1=1";
        if(status!=null){filtrosSql+=" AND p.status=?";parametros.add(status);}
        if(filtroPerfilAcessoId!=null){filtrosSql+=" AND p.role_id=?";parametros.add(filtroPerfilAcessoId);}
        if(equipe!=null){filtrosSql+=" AND EXISTS (SELECT 1 FROM team_member tm WHERE tm.user_id=u.id AND tm.team_id=?)";parametros.add(equipe);}
        if(busca!=null&&!busca.isBlank()){filtrosSql+=" AND (LOCATE(LOWER(?),LOWER(u.name))>0 OR LOCATE(LOWER(?),LOWER(u.email))>0)";parametros.add(busca.trim());parametros.add(busca.trim());}
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM app_user u JOIN app_profile p ON p.user_id=u.id"+filtrosSql,Long.class,parametros.toArray());
        parametros.add(tamanhoPagina);parametros.add(pagina*tamanhoPagina);
        return new PaginaUsuarios(incluirEquipes(jdbc.query(SQL_USUARIOS+filtrosSql+" ORDER BY u.created_at DESC,u.id LIMIT ? OFFSET ?",GestaoUsuariosRepository::montarUsuario,parametros.toArray())),total,pagina,tamanhoPagina);
    }

    public OpcoesUsuarios buscarOpcoesUsuarios() {
        return new OpcoesUsuarios(jdbc.query("SELECT id,role_key,name FROM roles ORDER BY name",(linha,indiceLinha)->new PerfilAcesso(linha.getString("id"),linha.getString("role_key"),linha.getString("name"))),
            jdbc.query("SELECT id,name FROM team WHERE archived_at IS NULL ORDER BY name",(linha,indiceLinha)->new EquipeReferencia(linha.getString("id"),linha.getString("name"))));
    }

    public List<UsuarioResponse> buscarUsuariosPorId(String id) {
        return incluirEquipes(jdbc.query(SQL_USUARIOS+" WHERE u.id=?",GestaoUsuariosRepository::montarUsuario,id));
    }

    public List<PerfilAcesso> buscarPerfilAcesso(String proximoPerfilAcessoId) {
        return jdbc.query("SELECT id,role_key,name FROM roles WHERE id=?",(linha,indiceLinha)->new PerfilAcesso(linha.getString("id"),linha.getString("role_key"),linha.getString("name")),proximoPerfilAcessoId);
    }

    public Long contarEquipeAtiva(String equipe) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team WHERE id=? AND archived_at IS NULL",Long.class,equipe);
    }

    public List<String> listarEquipesUsuario(String id) {
        return jdbc.queryForList("SELECT team_id FROM team_member WHERE user_id=?",String.class,id);
    }

    public int removerIntegrante(String id, String equipe) {
        return jdbc.update("DELETE FROM team_member WHERE user_id=? AND team_id=?",id,equipe);
    }

    public int inserirIntegrante(String id, String equipe) {
        return jdbc.update("INSERT INTO team_member VALUES (?,?,CURRENT_TIMESTAMP(6))",id,equipe);
    }

    public int atualizarPerfil(String proximoPerfilAcessoId, String cargoAtualizado, String id) {
        return jdbc.update("UPDATE app_profile SET role_id=?,job_title=?,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=?",proximoPerfilAcessoId,cargoAtualizado,id);
    }

    public int atualizarSituacao(String proximaSituacao, String id) {
        return jdbc.update("UPDATE app_profile SET status=?,updated_at=CURRENT_TIMESTAMP(6) WHERE user_id=?",proximaSituacao,id);
    }

    public Long contarSuperAdminsAtivas() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM app_profile p JOIN roles r ON r.id=p.role_id WHERE p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'",Long.class);
    }

    public String buscarEquipeFundadorasId() {
        return jdbc.queryForObject("SELECT id FROM team WHERE team_key='fundadoras'",String.class);
    }

    public Long contarIntegranteFundadoras(String usuarioId, String equipeFundadorasId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team_member WHERE user_id=? AND team_id=?",Long.class,usuarioId,equipeFundadorasId);
    }

    public Long contarOutrasSuperAdminsFundadoras(String equipeFundadorasId, String usuarioId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM team_member tm JOIN app_profile p ON p.user_id=tm.user_id JOIN roles r ON r.id=p.role_id WHERE tm.team_id=? AND tm.user_id<>? AND p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'",Long.class,equipeFundadorasId,usuarioId);
    }
}
