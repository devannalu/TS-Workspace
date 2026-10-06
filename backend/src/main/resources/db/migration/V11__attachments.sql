CREATE TABLE attachment (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 task_id VARCHAR(36) NULL,
 project_id VARCHAR(36) NULL,
 uploader_id VARCHAR(36) NOT NULL,
 original_name VARCHAR(255) NOT NULL,
 object_key VARCHAR(255) NOT NULL UNIQUE,
 mime_type VARCHAR(100) NOT NULL,
 size_bytes BIGINT NOT NULL,
 state VARCHAR(20) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 upload_expires_at TIMESTAMP(6) NOT NULL,
 removed_at TIMESTAMP(6) NULL,
 CONSTRAINT attachment_resource_ck CHECK ((task_id IS NOT NULL) <> (project_id IS NOT NULL)),
 CONSTRAINT attachment_size_ck CHECK (size_bytes BETWEEN 1 AND 10485760),
 CONSTRAINT attachment_state_ck CHECK (state IN ('PENDENTE','DISPONIVEL','REMOVIDO')),
 CONSTRAINT attachment_task_fk FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE RESTRICT,
 CONSTRAINT attachment_project_fk FOREIGN KEY (project_id) REFERENCES project(id) ON DELETE RESTRICT,
 CONSTRAINT attachment_uploader_fk FOREIGN KEY (uploader_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 INDEX attachment_task_created_idx(task_id,created_at,id),
 INDEX attachment_project_created_idx(project_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO permissions(id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,CURRENT_TIMESTAMP(6),CURRENT_TIMESTAMP(6)
FROM (SELECT 'tasks.attach' chave UNION ALL SELECT 'projects.attach' UNION ALL SELECT 'attachments.remove') novas
WHERE NOT EXISTS(SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p
WHERE p.permission_key IN ('tasks.attach','projects.attach','attachments.remove') AND (
 r.role_key IN ('SUPER_ADMIN','ADMIN') OR
 (r.role_key='SUPERVISOR' AND p.permission_key IN ('tasks.attach','projects.attach')) OR
 (r.role_key='SUPPORT' AND p.permission_key='tasks.attach')
) AND NOT EXISTS(SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
