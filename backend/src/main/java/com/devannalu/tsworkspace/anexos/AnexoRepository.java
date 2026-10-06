package com.devannalu.tsworkspace.anexos;

import com.devannalu.tsworkspace.anexos.Anexo.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnexoRepository {
    private final JdbcTemplate jdbc;
    public AnexoRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String SELECT="SELECT a.*,u.name FROM attachment a JOIN app_user u ON u.id=a.uploader_id";
    private Anexo mapear(ResultSet r,int linha) throws SQLException {
        var removida=r.getTimestamp("removed_at");
        return new Anexo(r.getString("id"),r.getString("task_id"),r.getString("project_id"),
            r.getString("uploader_id"),r.getString("name"),r.getString("original_name"),r.getString("object_key"),
            r.getString("mime_type"),r.getLong("size_bytes"),Estado.valueOf(r.getString("state")),
            r.getTimestamp("created_at").toInstant(),r.getTimestamp("upload_expires_at").toInstant(),
            removida==null?null:removida.toInstant());
    }
    public Optional<Anexo> buscar(String id) {
        return jdbc.query(SELECT+" WHERE a.id=?",this::mapear,id).stream().findFirst();
    }
    public List<Anexo> listar(Recurso recurso,String id,int pagina,int tamanho) {
        return jdbc.query(SELECT+" WHERE a."+recurso.coluna+"=? AND a.state='DISPONIVEL' ORDER BY a.created_at DESC,a.id DESC LIMIT ? OFFSET ?",
            this::mapear,id,tamanho,pagina*tamanho);
    }
    public long contar(Recurso recurso,String id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM attachment WHERE "+recurso.coluna+"=? AND state='DISPONIVEL'",Long.class,id);
    }
    public void inserir(String id,Recurso recurso,String recursoId,String autora,String nome,String chave,String mime,long tamanho,Instant expira) {
        jdbc.update("INSERT INTO attachment(id,"+recurso.coluna+",uploader_id,original_name,object_key,mime_type,size_bytes,state,created_at,upload_expires_at) VALUES(?,?,?,?,?,?,?,'PENDENTE',CURRENT_TIMESTAMP(6),?)",
            id,recursoId,autora,nome,chave,mime,tamanho,Timestamp.from(expira));
    }
    public int disponibilizar(String id) {
        return jdbc.update("UPDATE attachment SET state='DISPONIVEL' WHERE id=? AND state='PENDENTE'",id);
    }
    public int remover(String id) {
        return jdbc.update("UPDATE attachment SET state='REMOVIDO',removed_at=CURRENT_TIMESTAMP(6) WHERE id=? AND state<>'REMOVIDO'",id);
    }
}
