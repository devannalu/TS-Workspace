CREATE TABLE task (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 description TEXT NULL,
 status VARCHAR(20) NOT NULL,
 priority VARCHAR(10) NOT NULL,
 team_id VARCHAR(36) NOT NULL,
 created_by_id VARCHAR(36) NOT NULL,
 due_date DATE NULL,
 position INT NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 archived_at TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT task_team_fk FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE RESTRICT,
 CONSTRAINT task_creator_fk FOREIGN KEY (created_by_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT task_status_ck CHECK (status IN ('A_FAZER','EM_ANDAMENTO','EM_REVISAO','CONCLUIDA')),
 CONSTRAINT task_priority_ck CHECK (priority IN ('BAIXA','MEDIA','ALTA','URGENTE')),
 CONSTRAINT task_position_ck CHECK (position >= 0),
 INDEX task_board_idx (archived_at,status,position,id),
 INDEX task_team_board_idx (team_id,archived_at,status,position),
 INDEX task_due_idx (archived_at,due_date,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE task_assignee (
 task_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 PRIMARY KEY (task_id,user_id),
 CONSTRAINT task_assignee_task_fk FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
 CONSTRAINT task_assignee_user_fk FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 INDEX task_assignee_user_idx (user_id,task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permissions (id,permission_key,name,created_at,updated_at)
SELECT UUID(), chave, chave, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM (SELECT 'tasks.view' chave UNION ALL SELECT 'tasks.create' UNION ALL SELECT 'tasks.edit'
 UNION ALL SELECT 'tasks.assign' UNION ALL SELECT 'tasks.archive') novas
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);

INSERT INTO role_permissions (role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key LIKE 'tasks.%' AND (
 r.role_key IN ('SUPER_ADMIN','ADMIN') OR
 (r.role_key='SUPERVISOR' AND p.permission_key<>'tasks.archive') OR
 (r.role_key='SUPPORT' AND p.permission_key IN ('tasks.view','tasks.create','tasks.edit'))
) AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
