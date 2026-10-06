CREATE TABLE workspace_comment (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 task_id VARCHAR(36) NULL,
 project_id VARCHAR(36) NULL,
 author_id VARCHAR(36) NOT NULL,
 content TEXT NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 removed_at TIMESTAMP(6) NULL,
 removed_by_id VARCHAR(36) NULL,
 CONSTRAINT workspace_comment_resource_ck CHECK ((task_id IS NOT NULL) <> (project_id IS NOT NULL)),
 CONSTRAINT workspace_comment_content_ck CHECK (CHAR_LENGTH(TRIM(content)) BETWEEN 1 AND 5000),
 CONSTRAINT workspace_comment_task_fk FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE RESTRICT,
 CONSTRAINT workspace_comment_project_fk FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE RESTRICT,
 CONSTRAINT workspace_comment_author_fk FOREIGN KEY (author_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT workspace_comment_remover_fk FOREIGN KEY (removed_by_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 INDEX workspace_comment_task_created_idx (task_id,created_at,id),
 INDEX workspace_comment_project_created_idx (project_id,created_at,id),
 INDEX workspace_comment_author_idx (author_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permissions (id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6)
FROM (SELECT 'tasks.comment' chave UNION ALL SELECT 'projects.comment' UNION ALL SELECT 'comments.moderate') novas
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);

INSERT INTO role_permissions (role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key IN ('tasks.comment','projects.comment','comments.moderate') AND (
 r.role_key IN ('SUPER_ADMIN','ADMIN') OR
 (r.role_key='SUPERVISOR' AND p.permission_key IN ('tasks.comment','projects.comment')) OR
 (r.role_key='SUPPORT' AND p.permission_key='tasks.comment')
) AND NOT EXISTS (SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
