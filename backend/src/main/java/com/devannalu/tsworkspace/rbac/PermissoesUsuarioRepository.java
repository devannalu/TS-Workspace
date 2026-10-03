package com.devannalu.tsworkspace.rbac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PermissoesUsuarioRepository {
    private final JdbcTemplate jdbc;

    public PermissoesUsuarioRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<String> listarCatalogo() {
        return jdbc.queryForList("SELECT permission_key FROM permissions", String.class);
    }

    public List<String> listarConcessoesPerfil(String perfilId) {
        return jdbc.queryForList("SELECT p.permission_key FROM role_permissions rp JOIN permissions p ON p.id=rp.permission_id WHERE rp.role_id=?", String.class, perfilId);
    }

    public Map<String, String> listarExcecoesUsuario(String usuarioId) {
        Map<String, String> excecoes = new HashMap<>();
        jdbc.query("SELECT p.permission_key, up.effect FROM user_permissions up JOIN permissions p ON p.id=up.permission_id WHERE up.user_id=?",
            (org.springframework.jdbc.core.RowCallbackHandler) linha -> excecoes.put(linha.getString(1), linha.getString(2)), usuarioId);
        return excecoes;
    }
}
