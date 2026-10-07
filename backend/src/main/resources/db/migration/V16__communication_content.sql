CREATE TABLE communication_content (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 briefing TEXT NULL,
 channel VARCHAR(30) NOT NULL,
 format VARCHAR(30) NOT NULL,
 status VARCHAR(30) NOT NULL,
 team_id VARCHAR(36) NOT NULL,
 responsible_id VARCHAR(36) NULL,
 planned_publication_date DATE NULL,
 event_id VARCHAR(36) NULL,
 project_id VARCHAR(36) NULL,
 created_by_id VARCHAR(36) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 archived_at TIMESTAMP(6) NULL,
 CONSTRAINT content_team_fk FOREIGN KEY(team_id) REFERENCES team(id),
 CONSTRAINT content_responsible_fk FOREIGN KEY(responsible_id) REFERENCES app_user(id),
 CONSTRAINT content_event_fk FOREIGN KEY(event_id) REFERENCES community_event(id),
 CONSTRAINT content_project_fk FOREIGN KEY(project_id) REFERENCES project(id),
 CONSTRAINT content_creator_fk FOREIGN KEY(created_by_id) REFERENCES app_user(id),
 CONSTRAINT content_channel_ck CHECK(channel IN ('INSTAGRAM','LINKEDIN','EMAIL','SITE','INTERNO')),
 CONSTRAINT content_format_ck CHECK(format IN ('POST','CARROSSEL','VIDEO','ARTIGO','EMAIL','TEXTO')),
 CONSTRAINT content_status_ck CHECK(status IN ('IDEIA','PLANEJADO','EM_PRODUCAO','EM_REVISAO','APROVADO','PUBLICADO')),
 INDEX content_team_idx(team_id,archived_at,status,updated_at),
 INDEX content_calendar_idx(archived_at,planned_publication_date),
 INDEX content_responsible_idx(responsible_id,archived_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO permissions(id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,NOW(6),NOW(6) FROM
 (SELECT 'content.view' chave UNION ALL SELECT 'content.create' UNION ALL SELECT 'content.edit' UNION ALL SELECT 'content.archive') novas
WHERE NOT EXISTS(SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE p.permission_key LIKE 'content.%' AND
 (r.role_key IN ('SUPER_ADMIN','ADMIN') OR (r.role_key='SUPERVISOR' AND p.permission_key<>'content.archive') OR (r.role_key='SUPPORT' AND p.permission_key='content.view'))
 AND NOT EXISTS(SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
