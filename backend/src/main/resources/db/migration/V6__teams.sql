CREATE TABLE team (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    team_key VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500) NULL,
    parent_id VARCHAR(36) NULL,
    archived_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    root_slot TINYINT GENERATED ALWAYS AS (CASE WHEN parent_id IS NULL THEN 1 ELSE NULL END) STORED,
    CONSTRAINT team_key_uq UNIQUE (team_key),
    CONSTRAINT team_root_uq UNIQUE (root_slot),
    CONSTRAINT team_parent_fk FOREIGN KEY (parent_id) REFERENCES team(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    INDEX team_parent_active_idx (parent_id, archived_at),
    INDEX team_active_name_idx (archived_at, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE team_member (
    user_id VARCHAR(36) NOT NULL,
    team_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id, team_id),
    CONSTRAINT team_member_user_fk FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    CONSTRAINT team_member_team_fk FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE RESTRICT,
    INDEX team_member_team_idx (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO team (id, team_key, name, created_at, updated_at)
VALUES (UUID(), 'fundadoras', 'Fundadoras', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));
INSERT INTO team (id, team_key, name, parent_id, created_at, updated_at)
SELECT UUID(), official.team_key, official.name, root.id, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM team root CROSS JOIN (
    SELECT 'comunicacao' team_key, 'Comunicação' name
    UNION ALL SELECT 'eventos', 'Eventos'
    UNION ALL SELECT 'desenvolvimento-projetos', 'Desenvolvimento de Projetos'
    UNION ALL SELECT 'comunicacao-interna', 'Comunicação Interna'
) official WHERE root.team_key='fundadoras';
INSERT INTO team_member (user_id, team_id, created_at)
SELECT p.user_id, t.id, CURRENT_TIMESTAMP(6) FROM app_profile p
JOIN roles r ON r.id=p.role_id CROSS JOIN team t
WHERE p.status='ACTIVE' AND r.role_key='SUPER_ADMIN' AND t.team_key='fundadoras';
