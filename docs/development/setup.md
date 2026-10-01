# Ambiente de desenvolvimento

## Requisitos

- Node.js 24 e npm, conforme `frontend/package.json` e seu lockfile.
- Java 17 disponível no PATH ou por JAVA_HOME.
- Docker Desktop/Engine com containers Linux e Docker Compose.
- Git e acesso aos repositórios de dependências para o primeiro download.
- PowerShell para os scripts locais fornecidos.

O MySQL é executado pelo Compose; não é necessário instalar um servidor global.
O Maven Wrapper de `backend/` baixa a distribuição fixada, com verificação de
checksum. Maven global não é requisito.

A partir da raiz:

```powershell
node --version
npm --version
java -version
docker version
docker info
backend\mvnw.cmd --version
```

Docker precisa mostrar o **Server/Engine**, não apenas o Client. Se estiver
indisponível, não executar migrations ou declarar os testes integrados aprovados.
Preserve volumes e dados ao diagnosticar o ambiente.

## Configuração

Na primeira preparação, copie os exemplos somente se os destinos não existirem:

```powershell
if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
}
if (-not (Test-Path -LiteralPath 'frontend/.env')) {
    Copy-Item -LiteralPath 'frontend/.env.example' -Destination 'frontend/.env'
}
```

Substitua placeholders localmente. Os arquivos reais são ignorados pelo Git.
Não imprimir variáveis sensíveis nem incluí-las em comandos compartilhados.

| Local | Variáveis e finalidade |
| --- | --- |
| `.env` da raiz | `JAVA_MYSQL_PASSWORD`, `JAVA_MYSQL_ROOT_PASSWORD`, `JAVA_DATABASE_URL`, `JAVA_MYSQL_USER`, `FRONTEND_ORIGIN`; configuração Java e Compose |
| `.env` da raiz | `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MYSQL_TEST_PASSWORD`, `MYSQL_TEST_ROOT_PASSWORD`; bancos Prisma e testes |
| `frontend/.env` | `DATABASE_URL`, `SHADOW_DATABASE_URL`, `TEST_DATABASE_URL`; destinos Prisma compatíveis com o Compose |
| `frontend/.env` | `BETTER_AUTH_SECRET`, `BETTER_AUTH_URL`; identidade do login oficial |
| `frontend/.env` | `NEXT_PUBLIC_JAVA_API_URL`; origem pública da API, padrão http://localhost:8080 |
| `frontend/.env.bootstrap.local` | `BOOTSTRAP_NAME`, `BOOTSTRAP_EMAIL`, `BOOTSTRAP_PASSWORD`; bootstrap explícito |

Use credenciais distintas para bancos Java, Prisma e testes. O backend usa
usuário de aplicação, não root. As URLs Prisma devem corresponder às credenciais
e aos bancos declarados no Compose; não reutilizar a URL de desenvolvimento
como URL de testes.

O script `backend/run-dev.ps1` importa somente variáveis Java selecionadas
da raiz. Valores já presentes no processo têm precedência. `SERVER_PORT` e
`SERVER_ADDRESS` podem ajustar o bind; o padrão é `127.0.0.1:8080`.

`SESSION_COOKIE_SECURE` não é importada por esse script: se necessário, forneça
a variável diretamente no ambiente do processo. Use true no ambiente HTTPS de
produção e false apenas no HTTP local. Não confundir `NEXT_PUBLIC_*` com
configuração privada: essas variáveis são incorporadas no código do navegador.

Mantenha frontend e backend coerentes: `BETTER_AUTH_URL` e `FRONTEND_ORIGIN`
usam `http://localhost:3000` por padrão. A origem inclui protocolo, host e porta.

## Bancos locais

Na raiz, com os arquivos de ambiente preenchidos:

```powershell
docker compose config --quiet
docker compose up -d mysql mysql-java
docker compose --profile test up -d mysql-test
docker compose ps
```

| Serviço | Container | Porta | Banco |
| --- | --- | --- | --- |
| Prisma | ts-workspace-mysql | 127.0.0.1:3307 | ts_workspace |
| Testes Prisma | ts-workspace-mysql-test | 127.0.0.1:3308 | ts_workspace_test |
| Java | ts-workspace-mysql-java | 127.0.0.1:3309 | ts_workspace_java |

Aguarde os healthchecks. Os volumes persistem dados; não use `down -v`,
prune ou reset para iniciar o projeto. Testcontainers gerencia seus próprios
containers efêmeros e portas dinâmicas.

## Preparar o frontend e o schema Prisma

Dentro de `frontend/`:

```powershell
npm ci
npm run db:generate
npm run db:validate
npx prisma migrate deploy
npm run db:seed
```

`migrate deploy` aplica migrations já versionadas ao banco configurado.
Confira o destino antes de executar. Em ambiente já preparado, não recrie
migrations nem reinicialize os dados. O seed completa os dados organizacionais
padrão e não é uma restauração de banco.

## Acesso inicial

O Workspace não possui signup público. Para uma instalação nova, preencha
as variáveis BOOTSTRAP no arquivo local ignorado e execute dentro de
`frontend/`:

```powershell
npm run bootstrap
```

O fluxo cria explicitamente a primeira Super Admin e sua participação em
Fundadoras. A mesma identidade completa é idempotente; conta preexistente
incompatível, estado parcial ou segunda Super Admin são rejeitados.
Novas integrantes entram pelo fluxo de convites. SMTP não está configurado.

A identidade Java é independente. Seu comando administrativo, a partir da
raiz, é:

```powershell
powershell -NoProfile -File backend/bootstrap.ps1
```

O script utiliza os valores locais, executa na porta técnica 18081 e encerra.
Em banco vazio, inicializa o catálogo RBAC e cria User/Profile SUPER_ADMIN.
Para um banco com a identidade Java anterior sem role, execute esse bootstrap
antes de iniciar normalmente a API: o script habilita
`JAVA_RBAC_PROVISION_EXISTING=true` somente no processo. V5 exige uma única
User/Profile ACTIVE, email correspondente e senha válida; associa SUPER_ADMIN
sem alterar a identidade ou credencial e torna `role_id` obrigatório.
Estado parcial ou conta incompatível interrompe o provisionamento.

Repetir o bootstrap reexecuta o seed idempotente sem duplicar dados. Após V5
aplicada, o servidor normal não precisa das credenciais nem da flag de upgrade.
Não manter essa flag habilitada na configuração de runtime.
Consulte [RBAC](../architecture/rbac.md) para matriz e precedência.

## Iniciar os servidores

Em um terminal na raiz:

```powershell
powershell -NoProfile -File backend/run-dev.ps1
```

Flyway aplica as migrations Java e Hibernate valida o schema.
Em outro terminal:

```powershell
cd frontend
npm run dev
```

Abra [Workspace](http://localhost:3000) e
[verificação técnica Java](http://localhost:3000/infra).
O [health Java](http://localhost:8080/api/v1/health) deve responder UP quando
a aplicação e o banco estiverem disponíveis.

Para executar o frontend como build local de produção, dentro de `frontend/`:

```powershell
npm run build
npm run start
```

Ctrl+C encerra cada servidor. Não execute dev e start na mesma porta.
Os próximos passos estão em [testes](testing.md) e [convenções](conventions.md).
