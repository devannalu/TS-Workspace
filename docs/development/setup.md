# Ambiente de desenvolvimento

Node.js 24, npm, Java 17 e Docker Engine Linux acessível são requisitos.
O Maven Wrapper acompanha backend/. Não é necessário MySQL ou Maven global.
Confira Client e Server em docker version antes de iniciar.

## Configuração

Copie os exemplos para .env e frontend/.env somente se esses destinos ainda
não existirem. Substitua placeholders localmente. Arquivos reais são ignorados.

| Arquivo | Variáveis |
| --- | --- |
| .env | JAVA_MYSQL_PASSWORD, JAVA_MYSQL_ROOT_PASSWORD, JAVA_DATABASE_URL, JAVA_MYSQL_USER |
| .env | FRONTEND_ORIGIN, origem exata do Next, padrão http://localhost:3000 |
| frontend/.env | NEXT_PUBLIC_JAVA_API_URL, padrão http://localhost:8080 |
| .env ou frontend/.env.bootstrap.local | BOOTSTRAP_NAME, BOOTSTRAP_EMAIL, BOOTSTRAP_PASSWORD, somente comando administrativo |

O frontend não recebe credenciais do banco. NEXT_PUBLIC é público.
backend/run-dev.ps1 importa apenas configurações Java selecionadas sem imprimir
valores. SESSION_COOKIE_SECURE deve ser fornecida no processo, true em HTTPS
produção; false somente em HTTP local. O bind Java padrão é 127.0.0.1:8080.
SERVER_PORT e SERVER_ADDRESS permitem ajuste explícito.

## Iniciar

Na raiz, em um terminal:

```powershell
docker compose up -d mysql-java
powershell -NoProfile -File backend/run-dev.ps1
```

Aguarde ts-workspace-mysql-java healthy. O banco ts_workspace_java usa porta
3309. Flyway versiona o schema e Hibernate executa validate. Serviços auxiliares
preservados não fazem parte do perfil padrão. Não use down -v, reset ou prune.

Em outro terminal:

```powershell
cd frontend
npm install
npm run dev
```

Abra [Workspace](http://localhost:3000) ou [/infra](http://localhost:3000/infra).
Se a porta estiver ocupada, preserve o outro projeto e use
`npm run dev -- --port 3010`. Ajuste FRONTEND_ORIGIN para http://localhost:3010
no ambiente Java e reinicie apenas esta API. A validação local desta fase usa
3010. Mantenha o mesmo hostname para frontend e API: cookies não são
compartilhados entre localhost e 127.0.0.1.

## Acesso inicial

Não existe signup público. Para instalação nova, configure as variáveis
BOOTSTRAP em arquivo ignorado e execute:

```powershell
powershell -NoProfile -File backend/bootstrap.ps1
```

O comando usa porta 18081 e encerra. Cria User/Profile ACTIVE SUPER_ADMIN e
membership em Fundadoras de forma explícita/idempotente. Não substitui
identidades existentes. Novas integrantes entram por convite. Não mantenha
JAVA_BOOTSTRAP_ENABLED ou JAVA_RBAC_PROVISION_EXISTING no runtime normal.
Veja [RBAC](../architecture/rbac.md).

## Build e verificações

Dentro de frontend/: npm run typecheck, npm run lint, npm test e npm run build.
Use npm run start depois do build. Dev e start não devem ocupar a mesma porta.
Consulte [testes](testing.md), [convenções](conventions.md) e
[histórico operacional](../history/fase-java-5-cutover.md).
