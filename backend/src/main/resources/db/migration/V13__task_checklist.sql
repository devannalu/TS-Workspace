CREATE TABLE task_checklist (
 id VARCHAR(36) NOT NULL PRIMARY KEY,
 task_id VARCHAR(36) NOT NULL,
 text VARCHAR(500) NOT NULL,
 completed BOOLEAN NOT NULL DEFAULT FALSE,
 position INT NOT NULL,
 created_by_id VARCHAR(36) NOT NULL,
 version BIGINT NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 deleted_at TIMESTAMP(6) NULL,
 CONSTRAINT checklist_task_fk FOREIGN KEY(task_id) REFERENCES task(id) ON DELETE CASCADE,
 CONSTRAINT checklist_creator_fk FOREIGN KEY(created_by_id) REFERENCES app_user(id),
 CONSTRAINT checklist_position_ck CHECK(position>=0),
 INDEX checklist_task_idx(task_id,deleted_at,position,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
