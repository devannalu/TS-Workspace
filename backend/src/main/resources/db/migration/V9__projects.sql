CREATE TABLE project (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 description TEXT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PLANEJADO',
 team_id VARCHAR(36) NOT NULL,
 created_by_id VARCHAR(36) NOT NULL,
 start_date DATE NULL,
 end_date DATE NULL,
 version BIGINT NOT NULL DEFAULT 0,
 first_task_linked_at TIMESTAMP(6) NULL,
 archived_at TIMESTAMP(6) NULL,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT project_team_fk FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE RESTRICT,
 CONSTRAINT project_creator_fk FOREIGN KEY (created_by_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT project_status_ck CHECK (status IN ('PLANEJADO','EM_ANDAMENTO','PAUSADO','CONCLUIDO')),
 CONSTRAINT project_period_ck CHECK (start_date IS NULL OR end_date IS NULL OR start_date<=end_date),
 INDEX project_team_idx (team_id,archived_at,status),
 INDEX project_due_idx (archived_at,end_date,status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE project_responsible (
 project_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 PRIMARY KEY (project_id,user_id),
 CONSTRAINT project_responsible_project_fk FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE CASCADE,
 CONSTRAINT project_responsible_user_fk FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 INDEX project_responsible_user_idx (user_id,project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE task ADD COLUMN project_id VARCHAR(36) NULL,
 ADD CONSTRAINT task_project_fk FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE RESTRICT,
 ADD INDEX task_project_idx (project_id,archived_at,status);

INSERT INTO permissions (id,permission_key,name,created_at,updated_at)
SELECT UUID(), chave, chave, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM (SELECT 'projects.view' chave UNION ALL SELECT 'projects.create' UNION ALL SELECT 'projects.edit'
 UNION ALL SELECT 'projects.manage_members' UNION ALL SELECT 'projects.archive') novas
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);

INSERT INTO role_permissions (role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key LIKE 'projects.%' AND (
 r.role_key IN ('SUPER_ADMIN','ADMIN') OR
 (r.role_key='SUPERVISOR' AND p.permission_key<>'projects.archive') OR
 (r.role_key='SUPPORT' AND p.permission_key='projects.view')
) AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
