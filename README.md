# TS Workspace

Aplicação interna de gestão, operação e produtividade da Tech Sisters.
Construída em fases com Next.js, MySQL, Prisma e Better Auth.

## Estado atual

**IMPLEMENTADO:** Fase 1.3 — banco local, autenticação, convites de uso único,
gestão de usuárias, equipes, memberships, RBAC, seed, bootstrap administrativo,
login e dashboard com dados reais.

**VALIDADO:** migrations, seed repetido, testes unitários e de integração,
fluxos de convite/aceite, concorrência single-use, inativação, hierarquia,
fluxo HTTP no Next, typecheck, lint, build e validação manual local.

- [Registro da Fase 1.1](docs/fase-1.1.md)
- [Decisões, segurança e validações da Fase 1.2](docs/fase-1.2.md)
- [Convites, usuárias e equipes da Fase 1.3](docs/fase-1.3.md)

Não existem ainda SMTP, recuperação de senha ou módulos de produtividade.

## Pré-requisitos

- Node.js 24 LTS; ambiente validado com 24.19.0 e npm 11.17.0.
- Docker Desktop com Engine Linux acessível; validado Engine 29.7.2.
- Git; ambiente validado com 2.51.1.windows.1.
- Arquivo .env local com os valores reais. Use .env.example como referência;
  não use seus placeholders como credenciais.

Os arquivos reais de ambiente já foram preparados neste ambiente local e
estão ignorados pelo Git. Não os sobrescreva ao retomar o projeto.

## Instalação e execução

Na raiz do projeto:

```powershell
npm install --cache .npm-cache --no-fund
docker compose --profile test up -d --wait
npm run db:validate
npm run db:generate
npm run db:migrate -- --name init_core
npm run db:seed
npm run dev -- --hostname 127.0.0.1
```

A migration init_core já foi aplicada neste ambiente. O comando migrate é
para sincronização inicial/desenvolvimento; não gere outra migration de
mesmo propósito. Não use reset. Acesse [localhost:3000](http://localhost:3000),
mesma origem configurada em BETTER_AUTH_URL. Ctrl+C encerra o servidor.

## Comandos disponíveis

| Comando | Uso |
| --- | --- |
| npm run dev | Desenvolvimento |
| npm run build | Build de produção |
| npm run start | Servidor de produção após build |
| npm run typecheck | Gera tipos das rotas e executa tsc --noEmit |
| npm run lint | ESLint, sem tolerância a warnings |
| npm run db:format | Formata schema Prisma |
| npm run db:validate | Valida schema |
| npm run db:generate | Gera Prisma Client |
| npm run db:migrate -- --name NOME | Cria/aplica migration de desenvolvimento |
| npm run db:seed | Seed idempotente |
| npm run auth:schema:check | Check oficial Better Auth contra Prisma Client |
| npm run bootstrap | Primeira Super Admin; requer arquivo local preenchido |
| npm test | Testes unitários |
| npm run test:integration | Aplica migrations no banco de testes e executa integração HTTP/MySQL |
| npm run test:workspace | Testa o build real do Next com banco de testes na porta 3101 |

O teste workspace exige build prévio e banco de testes migrado. Inicia e
encerra seu próprio servidor Next, cria uma identidade temporária no banco
de testes e remove somente os registros de teste que criou.

## Banco e ambiente

Imagem oficial fixa MySQL 8.4.11. Containers e volumes exclusivos:

- ts-workspace-mysql / ts-workspace-mysql-data: banco ts_workspace em
  127.0.0.1:3307, com usuário próprio ts_workspace.
- ts-workspace-mysql-test / ts-workspace-mysql-test-data: banco
  ts_workspace_test em 127.0.0.1:3308, com usuário ts_workspace_test.
- ts_workspace_shadow: banco separado para Prisma Migrate na instância dev.

Variáveis: DATABASE_URL, SHADOW_DATABASE_URL, TEST_DATABASE_URL,
BETTER_AUTH_SECRET, BETTER_AUTH_URL e as quatro senhas MYSQL_* do Compose.
Não é necessário NEXT_PUBLIC_APP_URL: o cliente de auth usa a mesma origem.

Não executar DROP, migrate reset ou remoção de volumes sem autorização.
Não usar banco de desenvolvimento ou produção para integração. O runner
recusa qualquer destino fora do banco/host/porta/usuário de teste esperados.

## Primeira Super Admin

O seed cria quatro cargos e cinco equipes, mas **não cria pessoas**.
O bootstrap lê `.env.bootstrap.local`, ignorado pelo Git, com:
BOOTSTRAP_NAME, BOOTSTRAP_EMAIL e BOOTSTRAP_PASSWORD.

A responsável define a senha localmente, de 12 a 128 caracteres. Não a
passe em argumento de terminal, chat, Git ou relatório. Execute
`npm run bootstrap` depois de seed e preenchimento do arquivo.

É uma operação administrativa one-shot: não executar em dois processos
simultaneamente. A mesma conta totalmente provisionada é idempotente;
segunda Super Admin, conta preexistente ou estado parcial são recusados.
User/Account, Profile, membership e auditoria compartilham uma transação.
Better Auth faz o hashing através da API oficial; não há hashing caseiro.

Depois da conclusão, a responsável pode retirar BOOTSTRAP_PASSWORD do arquivo
local. Uma nova execução idempotente não redefine a senha da conta existente.

## Segurança e domínio

- Signup HTTP bloqueado no Better Auth; nenhuma página /signup. O cadastro
  público continua fechado; contas novas só entram por `/convite/[token]`.
- Autenticação Better Auth separada do RBAC TS Workspace.
- requireAuth consulta sessão, User e Profile active no servidor.
- Profile inactive não cria sessão; acesso com sessão anterior revoga todas.
- Sem cache persistente de autorização ou token em localStorage.
- RBAC: inactive nega; SUPER_ADMIN ativo tem bypass; override individual;
  grants do cargo; negação por padrão. Ver matriz completa no registro 1.2.
- Cinco equipes oficiais, Fundadoras raiz e as outras quatro filhas.
- TeamMember liga User diretamente a Team, permitindo várias equipes.
- Hierarquia validada no servidor, sem ciclos, parent inválido ou segunda raiz.
- AuditLog não recebe credenciais, cookies ou tokens.
- Login e logout têm loading/erro; dashboard usa somente dados persistidos.
- `/usuarias` e `/equipes` usam mutations server-side com Zod, autorização e
  auditoria. Convites não armazenam token puro e não enviam e-mail nesta fase.

## Stack e versões

| Pacote | Versão |
| --- | --- |
| Next.js / eslint-config-next | 16.3.7 |
| React / React DOM | 19.3.0 |
| TypeScript | 6.0.3, strict e noImplicitAny ativos |
| Tailwind / @tailwindcss/postcss | 4.3.3 |
| ESLint | 9.39.5 |
| Lucide React | 1.48.0 |
| Prisma / @prisma/client / @prisma/adapter-mariadb | 7.10.0 |
| Better Auth / @better-auth/prisma-adapter / auth CLI | 1.7.6 |
| Zod | 4.6.5 |
| dotenv | 18.0.4 |
| tsx | 4.23.15 |
| Vitest | 5.0.2 |
| bun-types | 1.4.2, somente declaração sqlite exigida pelo Better Auth |

O runtime continua Node e o banco continua MySQL. Os adapters MariaDB/Bun
nas dependências/tipos não indicam troca de banco ou runtime. O lockfile
fixa a árvore instalada. Overrides de segurança estão documentados no
registro 1.2, incluindo autorização para deepmerge-ts 8.0.2. O audit da
instalação final reportou zero vulnerabilidades.

ESLint 9 foi mantido por compatibilidade do plugin React, apesar do aviso
de suporte encerrado do npm. Não atualizar majors sem revalidar peers.

## Estrutura

```text
docker/mysql/init.sql
prisma/
  schema.prisma
  migrations/20260929153313_init_core/migration.sql
  seed.ts
  seed-data.ts
scripts/
  auth-schema.config.ts
  auth-check.config.ts
  bootstrap.ts
  test-integration.ts
  test-workspace.ts
src/
  app/                 # /, /login, /workspace, API Better Auth
  components/          # layout, formulários e UI utilizada
  generated/prisma/    # gerado, ignorado pelo Git
  lib/
    auth/              # configuração, sessão, bootstrap e políticas
    db/                # cliente e proteção do banco de testes
    permissions/       # RBAC centralizado
    teams/             # validação e serviço de hierarquia
 tests/                # unit e integration
 docs/                 # registros das fases
compose.yaml
prisma.config.ts
prisma.test.config.ts
vitest.config.ts
.env.example
```

AGENTS.md e CLAUDE.md foram gerados pelo Next na Fase 1.1 e preservados.
next-env.d.ts e Prisma Client são gerados, não editados manualmente.
O alias @/* aponta para src/*. Tokens visuais seguem em src/app/globals.css.

## Git e publicação

Repositório local, sem commits ou remote configurado por este trabalho.
.env e .env.bootstrap.local são ignorados; somente .env.example é versionável.
Nenhum deploy, push, envio de e-mail ou reset de banco foi executado.
