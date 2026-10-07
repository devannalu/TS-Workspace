CREATE TABLE notification (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 recipient_id VARCHAR(36) NOT NULL,
 source_type VARCHAR(20) NOT NULL,
 resource_id VARCHAR(36) NOT NULL,
 kind VARCHAR(30) NOT NULL,
 event_key VARCHAR(160) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL,
 read_at TIMESTAMP(6) NULL,
 CONSTRAINT notification_recipient_fk FOREIGN KEY(recipient_id) REFERENCES app_user(id) ON DELETE CASCADE,
 CONSTRAINT notification_source_ck CHECK(source_type IN ('TAREFA','PROJETO')),
 CONSTRAINT notification_kind_ck CHECK(kind IN ('ATRIBUICAO','COMENTARIO','ALTERACAO','PRAZO','ATRASO')),
 UNIQUE KEY notification_event_uk(recipient_id,event_key),
 INDEX notification_recipient_idx(recipient_id,read_at,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
