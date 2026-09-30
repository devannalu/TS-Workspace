CREATE TABLE schema_marker (
    id INT NOT NULL PRIMARY KEY,
    name VARCHAR(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO schema_marker (id, name) VALUES (1, 'java-foundation');
