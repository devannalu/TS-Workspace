CREATE DATABASE IF NOT EXISTS ts_workspace_shadow CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON ts_workspace_shadow.* TO 'ts_workspace'@'%';
