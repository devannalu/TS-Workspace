CREATE TABLE chat_message (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 team_id VARCHAR(36) NULL,
 author_id VARCHAR(36) NOT NULL,
 content TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 removed_at TIMESTAMP(6) NULL,
 removed_by_id VARCHAR(36) NULL,
 CONSTRAINT chat_team_fk FOREIGN KEY(team_id) REFERENCES team(id),
 CONSTRAINT chat_author_fk FOREIGN KEY(author_id) REFERENCES app_user(id),
 CONSTRAINT chat_moderator_fk FOREIGN KEY(removed_by_id) REFERENCES app_user(id),
 INDEX chat_channel_idx(team_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO permissions(id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,NOW(6),NOW(6) FROM
 (SELECT 'chat.view' chave UNION ALL SELECT 'chat.send' UNION ALL SELECT 'chat.moderate') novas
WHERE NOT EXISTS(SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE p.permission_key LIKE 'chat.%' AND
 (r.role_key IN ('SUPER_ADMIN','ADMIN') OR (r.role_key IN ('SUPERVISOR','SUPPORT') AND p.permission_key<>'chat.moderate'))
 AND NOT EXISTS(SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
