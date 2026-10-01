CREATE TABLE roles (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    role_key VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE permissions (
    id VARCHAR(36) NOT NULL PRIMARY KEY,
    permission_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE role_permissions (
    role_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id) REFERENCES roles(id),
    FOREIGN KEY (permission_id) REFERENCES permissions(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE user_permissions (
    user_id VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    effect VARCHAR(5) NOT NULL,
    PRIMARY KEY (user_id, permission_id),
    FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id),
    CONSTRAINT user_permissions_effect_ck CHECK (effect IN ('ALLOW', 'DENY'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE app_profile ADD COLUMN role_id VARCHAR(36) NULL,
    ADD CONSTRAINT app_profile_role_fk FOREIGN KEY (role_id) REFERENCES roles(id);
