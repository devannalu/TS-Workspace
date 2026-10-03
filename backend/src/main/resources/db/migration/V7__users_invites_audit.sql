CREATE TABLE invite (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 email VARCHAR(320) NOT NULL,
 token_hash VARCHAR(64) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 used_at TIMESTAMP(6) NULL,
 cancelled_at TIMESTAMP(6) NULL,
 invited_by_id VARCHAR(36) NOT NULL,
 role_id VARCHAR(36) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT invite_token_uq UNIQUE (token_hash),
 CONSTRAINT invite_actor_fk FOREIGN KEY (invited_by_id) REFERENCES app_user(id) ON DELETE RESTRICT,
 CONSTRAINT invite_role_fk FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE RESTRICT,
 INDEX invite_email_state_idx (email, used_at, cancelled_at, expires_at),
 INDEX invite_created_idx (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE invite_team (
 invite_id VARCHAR(36) NOT NULL,
 team_id VARCHAR(36) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 PRIMARY KEY (invite_id, team_id),
 CONSTRAINT invite_team_invite_fk FOREIGN KEY (invite_id) REFERENCES invite(id) ON DELETE CASCADE,
 CONSTRAINT invite_team_team_fk FOREIGN KEY (team_id) REFERENCES team(id) ON DELETE RESTRICT,
 INDEX invite_team_team_idx (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE audit_log (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 actor_id VARCHAR(36) NULL,
 action VARCHAR(100) NOT NULL,
 entity_type VARCHAR(50) NOT NULL,
 entity_id VARCHAR(100) NOT NULL,
 metadata_json JSON NULL,
 created_at TIMESTAMP(6) NOT NULL,
 CONSTRAINT audit_actor_fk FOREIGN KEY (actor_id) REFERENCES app_user(id) ON DELETE SET NULL,
 INDEX audit_actor_created_idx (actor_id, created_at),
 INDEX audit_entity_idx (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- User emails allow 320 characters; Spring Session's principal is the email.
ALTER TABLE SPRING_SESSION MODIFY PRINCIPAL_NAME VARCHAR(320);
