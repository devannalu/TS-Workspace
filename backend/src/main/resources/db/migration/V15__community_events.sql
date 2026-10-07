CREATE TABLE community_event (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 name VARCHAR(200) NOT NULL,
 description TEXT NULL,
 format VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'PLANEJADO',
 team_id VARCHAR(36) NOT NULL,
 start_at TIMESTAMP(6) NOT NULL,
 end_at TIMESTAMP(6) NOT NULL,
 zone_id VARCHAR(100) NOT NULL,
 location VARCHAR(500) NULL,
 link VARCHAR(2048) NULL,
 notes TEXT NULL,
 created_by_id VARCHAR(36) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 archived_at TIMESTAMP(6) NULL,
 CONSTRAINT community_event_team_fk FOREIGN KEY(team_id) REFERENCES team(id),
 CONSTRAINT community_event_creator_fk FOREIGN KEY(created_by_id) REFERENCES app_user(id),
 CONSTRAINT community_event_type_ck CHECK(format IN ('PRESENCIAL','ONLINE','HIBRIDO')),
 CONSTRAINT community_event_status_ck CHECK(status IN ('PLANEJADO','CONFIRMADO','EM_ANDAMENTO','CONCLUIDO','CANCELADO')),
 CONSTRAINT community_event_period_ck CHECK(start_at<end_at),
 INDEX community_event_team_idx(team_id,archived_at,status,start_at),
 INDEX community_event_period_idx(archived_at,start_at,end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE community_event_responsible (
 event_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 PRIMARY KEY(event_id,user_id),
 CONSTRAINT community_event_responsible_community_event_fk FOREIGN KEY(event_id) REFERENCES community_event(id) ON DELETE CASCADE,
 CONSTRAINT community_event_responsible_user_fk FOREIGN KEY(user_id) REFERENCES app_user(id),
 INDEX community_event_responsible_user_idx(user_id,event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO permissions(id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,NOW(6),NOW(6) FROM
 (SELECT 'events.view' chave UNION ALL SELECT 'events.create' UNION ALL SELECT 'events.edit' UNION ALL SELECT 'events.archive') novas
WHERE NOT EXISTS(SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE p.permission_key LIKE 'events.%' AND
 (r.role_key IN ('SUPER_ADMIN','ADMIN') OR (r.role_key='SUPERVISOR' AND p.permission_key<>'events.archive') OR (r.role_key='SUPPORT' AND p.permission_key='events.view'))
 AND NOT EXISTS(SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
