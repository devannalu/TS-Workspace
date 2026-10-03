package com.devannalu.tsworkspace.equipes;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InicializacaoEquipesService {
    private final JdbcTemplate jdbc;
    public InicializacaoEquipesService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    @Transactional
    public void seed() {
        insert("fundadoras", "Fundadoras", null);
        var roots = jdbc.queryForList("SELECT id, parent_id, archived_at FROM team WHERE team_key='fundadoras' FOR UPDATE");
        var root = roots.get(0);
        if (root.get("parent_id") != null || root.get("archived_at") != null)
            throw new IllegalStateException("Equipe Fundadoras em estado inválido; seed cancelado.");
        String rootId = (String) root.get("id");
        insert("comunicacao", "Comunicação", rootId);
        insert("eventos", "Eventos", rootId);
        insert("desenvolvimento-projetos", "Desenvolvimento de Projetos", rootId);
        insert("comunicacao-interna", "Comunicação Interna", rootId);
        jdbc.update("""
            INSERT INTO team_member (user_id, team_id, created_at)
            SELECT p.user_id, ?, CURRENT_TIMESTAMP(6) FROM app_profile p JOIN roles r ON r.id=p.role_id
            WHERE p.status='ACTIVE' AND r.role_key='SUPER_ADMIN'
            ON DUPLICATE KEY UPDATE user_id=team_member.user_id
            """, rootId);
    }
    private void insert(String key, String name, String validarEquipeMae) {
        jdbc.update("""
            INSERT INTO team (id, team_key, name, parent_id, created_at, updated_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
            ON DUPLICATE KEY UPDATE id=team.id
            """, UUID.randomUUID().toString(), key, name, validarEquipeMae);
    }
}
