CREATE TABLE meeting (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 title VARCHAR(200) NOT NULL,
 agenda TEXT NULL,
 type VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'AGENDADA',
 team_id VARCHAR(36) NOT NULL,
 start_at TIMESTAMP(6) NOT NULL,
 end_at TIMESTAMP(6) NOT NULL,
 zone_id VARCHAR(100) NOT NULL,
 location VARCHAR(500) NULL,
 link VARCHAR(2048) NULL,
 results TEXT NULL,
 created_by_id VARCHAR(36) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 archived_at TIMESTAMP(6) NULL,
 CONSTRAINT meeting_team_fk FOREIGN KEY(team_id) REFERENCES team(id),
 CONSTRAINT meeting_creator_fk FOREIGN KEY(created_by_id) REFERENCES app_user(id),
 CONSTRAINT meeting_type_ck CHECK(type IN ('REUNIAO','TALK')),
 CONSTRAINT meeting_status_ck CHECK(status IN ('AGENDADA','REALIZADA','CANCELADA')),
 CONSTRAINT meeting_period_ck CHECK(start_at<end_at),
 INDEX meeting_team_idx(team_id,archived_at,status,start_at),
 INDEX meeting_period_idx(archived_at,start_at,end_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE meeting_participant (
 meeting_id VARCHAR(36) NOT NULL,
 user_id VARCHAR(36) NOT NULL,
 responsible BOOLEAN NOT NULL DEFAULT FALSE,
 PRIMARY KEY(meeting_id,user_id),
 CONSTRAINT meeting_participant_meeting_fk FOREIGN KEY(meeting_id) REFERENCES meeting(id) ON DELETE CASCADE,
 CONSTRAINT meeting_participant_user_fk FOREIGN KEY(user_id) REFERENCES app_user(id),
 INDEX meeting_participant_user_idx(user_id,meeting_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO permissions(id,permission_key,name,created_at,updated_at)
SELECT UUID(),chave,chave,NOW(6),NOW(6) FROM
 (SELECT 'meetings.view' chave UNION ALL SELECT 'meetings.create' UNION ALL SELECT 'meetings.edit' UNION ALL SELECT 'meetings.archive') novas
WHERE NOT EXISTS(SELECT 1 FROM permissions p WHERE p.permission_key=novas.chave);
INSERT INTO role_permissions(role_id,permission_id)
SELECT r.id,p.id FROM roles r CROSS JOIN permissions p WHERE p.permission_key LIKE 'meetings.%' AND
 (r.role_key IN ('SUPER_ADMIN','ADMIN') OR (r.role_key='SUPERVISOR' AND p.permission_key<>'meetings.archive') OR (r.role_key='SUPPORT' AND p.permission_key='meetings.view'))
 AND NOT EXISTS(SELECT 1 FROM role_permissions rp WHERE rp.role_id=r.id AND rp.permission_id=p.id);
