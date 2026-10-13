CREATE TABLE partnership (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 organization VARCHAR(200) NOT NULL,
 description TEXT NULL,
 status VARCHAR(30) NOT NULL,
 team_id VARCHAR(36) NOT NULL,
 responsible_id VARCHAR(36) NULL,
 next_action_date DATE NULL,
 contact_name VARCHAR(200) NULL,
 contact_email VARCHAR(254) NULL,
 contact_phone VARCHAR(50) NULL,
 next_action VARCHAR(500) NULL,
 notes TEXT NULL,
 event_id VARCHAR(36) NULL,
 project_id VARCHAR(36) NULL,
 created_by_id VARCHAR(36) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 archived_at TIMESTAMP(6) NULL,
 CONSTRAINT partnership_team_fk FOREIGN KEY(team_id) REFERENCES team(id),
 CONSTRAINT partnership_responsible_fk FOREIGN KEY(responsible_id) REFERENCES app_user(id),
 CONSTRAINT partnership_event_fk FOREIGN KEY(event_id) REFERENCES community_event(id),
 CONSTRAINT partnership_project_fk FOREIGN KEY(project_id) REFERENCES project(id),
 CONSTRAINT partnership_creator_fk FOREIGN KEY(created_by_id) REFERENCES app_user(id),
 CONSTRAINT partnership_status_ck CHECK(status IN ('PROSPECCAO','CONTATO','NEGOCIACAO','CONFIRMADA','ENCERRADA')),
 INDEX partnership_team_idx(team_id,archived_at,status,updated_at),
 INDEX partnership_calendar_idx(archived_at,next_action_date),
 INDEX partnership_responsible_idx(responsible_id,archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO permissions(id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,NOW(6),NOW(6) FROM
 (SELECT 'partnerships.view' chave UNION ALL SELECT 'partnerships.create' UNION ALL SELECT 'partnerships.edit' UNION ALL SELECT 'partnerships.archive') novas
WHERE NOT EXISTS(SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE p.permission_key LIKE 'partnerships.%' AND
 (r.role_key IN ('SUPER_ADMIN','ADMIN') OR (r.role_key='SUPERVISOR' AND p.permission_key<>'partnerships.archive') OR (r.role_key='SUPPORT' AND p.permission_key='partnerships.view'))
 AND NOT EXISTS(SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
