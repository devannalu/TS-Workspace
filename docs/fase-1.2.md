# Fase 1.2 — banco e autenticação

## Estado

**IMPLEMENTADO:** MySQL local, Prisma, modelos centrais, migration, seed,
Better Auth, login/logout, sessões, proteção do workspace, RBAC base,
validação de hierarquia, bootstrap transacional e dashboard com dados reais.

**VALIDADO:** MySQL, migration de desenvolvimento e testes, seed repetido,
schema Prisma e Better Auth, 29 testes unitários, 12 testes de integração,
fluxo HTTP no servidor Next real, typecheck, lint, build e dev.

**PENDENTE:** executar o bootstrap da conta real após a responsável preencher
a senha em `.env.bootstrap.local`. Os testes de bootstrap usam somente o
banco separado e não substituem a criação da primeira Super Admin real.

## Ambiente de banco

Docker Engine 29.7.2, Docker Desktop 4.90.0, Linux/amd64. As consultas no
sandbox falharam por permissão, mas `docker version` e `docker info` passaram
com a execução autorizada. Não houve instalação de software de sistema.

Imagem oficial fixa `mysql:8.4.11`. O catálogo oficial da imagem expunha essa
versão para a linha 8.4 LTS na consulta; não foi usada a tag `latest`.

| Recurso | Desenvolvimento | Testes |
| --- | --- | --- |
| Container | ts-workspace-mysql | ts-workspace-mysql-test |
| Banco | ts_workspace | ts_workspace_test |
| Usuário da aplicação | ts_workspace | ts_workspace_test |
| Porta local | 127.0.0.1:3307 | 127.0.0.1:3308 |
| Volume | ts-workspace-mysql-data | ts-workspace-mysql-test-data |

Ambos ficaram healthy e responderam SELECT com versão 8.4.11, banco e usuário
esperados. O banco `ts_workspace_shadow`, na instância de desenvolvimento,
é reservado ao Prisma Migrate; não é usado pela aplicação nem pelos testes.
O SQL de inicialização cria apenas esse banco e seu grant. Schema e tabelas
de domínio são responsabilidade exclusiva das migrations do Prisma.

Credenciais aleatórias locais e BETTER_AUTH_SECRET foram gravados em `.env`,
ignorado pelo Git, sem impressão dos valores. A aplicação não usa root.
O usuário local de desenvolvimento também executa migrations e, portanto,
possui DDL nos bancos de desenvolvimento e shadow. Separar usuário migrador
e usuário runtime quando houver ambiente de produção.

## Versões e compatibilidade

Prisma, client e adapter-mariadb: 7.10.0. Better Auth, adapter Prisma e CLI
`auth`: 1.7.6. Zod 4.6.5, dotenv 18.0.4, tsx 4.23.15, Vitest 5.0.2,
server-only 0.0.1 e bun-types 1.4.2 (somente declaração `bun:sqlite`).
A base Next 16.3.7, React 19.3.0, TS 6.0.3, Tailwind 4.3.3 e ESLint 9.39.5
foi preservada. Nenhum runtime Bun ou banco SQLite foi instalado/configurado.

Prisma 7 usa `prisma.config.ts`, generator `prisma-client`, saída explícita
e driver adapter. `@prisma/adapter-mariadb` é o driver oficial usado para
conectar ao servidor **MySQL**. O adapter de identidade é outro pacote:
`@better-auth/prisma-adapter`, configurado com provider mysql.

Overrides de segurança explícitos:

- `@prisma/adapter-mariadb > mariadb`: 3.5.4.
- `mysql2`: 3.24.4.
- `@prisma/config > deepmerge-ts`: 8.0.2, autorizado pela responsável.

A instalação final reportou zero vulnerabilidades. Não foi usado audit fix
--force. Os avisos npm sobre scripts de instalação pendentes não foram
ignorados através de liberação global; geração, CLI, testes e build passaram.

## Schema e migration

Models: User, Session, Account, Verification, Profile, Role, Permission,
RolePermission, UserPermission, Team, TeamMember e AuditLog.

O CLI oficial gerou os modelos Better Auth em `.verification/better-auth.prisma`
antes da incorporação. Foram preservados campos, mapeamentos e índices, com
defaults UUID e relações de domínio acrescentados. O check oficial posterior
aprovou o Prisma Client gerado.

Migration: `prisma/migrations/20260929153313_init_core/migration.sql`.
Criada/aplicada com Prisma Migrate, sem edição manual do SQL e sem reset.
O arquivo está presente e versionável, ainda sem commit conforme instrução.

IDs UUID são gerados pelo Prisma e pelo Better Auth. Seeds usam chaves
estáveis de Role, Permission e Team; não existem IDs fixos.

## Seed

Primeira e segunda execução: 4 Roles, 10 Permissions, 24 RolePermissions e
5 Teams, sem duplicação. Testes também verificam a estabilidade dos IDs.

Fundadoras é a raiz. Comunicação, Eventos, Desenvolvimento de Projetos e
Comunicação Interna são filhas usando o ID real da raiz. Nenhuma frente
conceitual foi criada como equipe.

O seed é transacional. Defaults de RolePermission são aplicados na criação
do cargo; execuções posteriores preservam ajustes administrativos existentes.
Da mesma forma, não renomeia ou reparenta equipes já existentes silenciosamente.

## Autenticação e autorização

Better Auth gerencia credenciais, hashing, cookies, sessão, login e logout.
Não se utiliza seu plugin de roles/admin para substituir o RBAC do domínio.
Nenhum provedor social, signup público ou recuperação de senha foi habilitado.

Signup é bloqueado em duas camadas oficiais: `disableSignUp: true` e
`disabledPaths: ["/sign-up/email"]`. A API server-side pública também recusa
signUpEmail. Testes HTTP verificam rejeição e contagem de User inalterada.

Sessões persistidas em MySQL, cookie HttpOnly e SameSite=Lax; cookies seguros
conforme comportamento oficial em produção. Sem token em localStorage e sem
cookieCache. Duração configurada: 7 dias, atualização diária; login da UI usa
rememberMe false. Revogação é consultada no banco em cada requisição.

O hook de criação de Session recusa Profile ausente/inactive. `requireAuth`
valida sessão, User e Profile ativo. Ao detectar perfil ausente/inativo,
revoga todas as sessões da usuária e impede o acesso. Não há tela de gestão
de status nesta subfase. `cache()` do React apenas deduplica na requisição,
não mantém autorização entre requisições.

Rotas `/`, `/login` e `/workspace` usam a mesma validação server-side.
O estado loading pode iniciar streaming: o redirecionamento é então emitido
por meta do Next, em vez de HTTP 307. O teste de rota verifica ambos os
comportamentos documentados e a ausência dos dados privados na resposta.

Precedência RBAC: perfil inactive/ausente nega tudo; SUPER_ADMIN ativo tem
bypass centralizado; depois override ALLOW/DENY; depois grants do cargo;
ausência de grant nega. Chaves desconhecidas são negadas, inclusive para
SUPER_ADMIN. ADMIN recebe as 10 permissões iniciais exceto permissions.manage;
SUPERVISOR recebe users.view, teams.view e settings.view; SUPPORT recebe
teams.view e settings.view. A matriz é inicial, sem UI administrativa ainda.

`getUserPermissions`, `hasPermission` e `requirePermission` sempre exigem a
usuária autenticada. `canAccessTeam` recusa equipes arquivadas/inexistentes
e aceita membership direto ou teams.manage_members. Membership não concede
automaticamente acesso a descendentes.

O serviço server-side de mudança de parent exige teams.edit, valida IDs
com Zod e atualiza árvore/auditoria em transação Serializable. O validador
recusa self-parent, ciclos, descendentes como parent, parent inexistente ou
arquivado, nova raiz e movimentação da raiz existente. Não há UI de gestão
de equipes nesta etapa.

Auditoria registra bootstrap e mudança de parent com metadados limitados a
IDs. Senhas, cookies, tokens e URLs de conexão não são registrados. Erros
de UI usam mensagens genéricas e o logger bruto do Better Auth está desativado.
Rate limiting de login: 5 tentativas por minuto, armazenamento em memória
adequado ao único processo local; deverá ser revisto para múltiplas instâncias.

## Bootstrap

Operação CLI explícita em `scripts/bootstrap.ts`. Lê `.env` e
`.env.bootstrap.local`; não aceita senha na linha de comando nem a imprime.
Não executar simultaneamente em dois processos.

Dentro de uma transação Prisma, verifica seed, ausência de outra Super Admin
e qualquer conta preexistente. Cria uma instância Better Auth isolada ligada
ao TransactionClient para chamar a API oficial signUpEmail. Esse handler
nunca é montado em HTTP, e seu hook rejeita qualquer Request HTTP. Não há
flag global mutável nem alteração da instância pública.

User/Account, Profile active, Role SUPER_ADMIN, TeamMember Fundadoras e AuditLog
compartilham a mesma transação. O teste induz uma falha após criar a credencial
e confirma rollback de User e Account. Nenhuma conta preexistente é apagada.

A mesma conta completamente provisionada retorna sem alterações. Conta
preexistente/estado parcial exige revisão; uma segunda Super Admin é recusada.
A execução idempotente não troca a senha nem atualiza o nome da conta existente.

## Resultados e correções

- Prisma format, validate e generate: sucesso.
- Migration dev e deploy no banco de testes: sucesso.
- Seed duas vezes: mesma contagem e IDs preservados.
- Check de schema Better Auth: sucesso.
- 29 testes unitários e 12 de integração: sucesso.
- Teste HTTP com servidor Next de produção: sucesso.
- Bootstrap real da primeira Super Admin: sucesso; User, Profile active,
  Role SUPER_ADMIN, TeamMember Fundadoras e AuditLog confirmados no MySQL.
- Segunda execução do bootstrap: idempotente, sem alteração de dados.
- Login real local, sessão, dashboard com nome/cargo/equipe e logout:
  sucesso no fluxo HTTP do Next.
- Typecheck strict, lint com zero warnings e build: sucesso.
- Dev iniciado, tela de login renderizada; desktop/mobile sem overflow.
- Tentativa de login inválida exibe feedback; visita anônima a /workspace
  retorna ao login; console sem erro de aplicação na inspeção.

Falhas corrigidas:

1. Tipos Next duplicados após alternar dev/build: typecheck usa `next typegen`
   e exclui `.next/dev`; não desativa strict ou skipLibCheck.
2. Declaração bun:sqlite referenciada pelo Better Auth: carrega somente a
   declaração oficial `bun-types/sqlite`; carregar todos os globais Bun causou
   conflitos reais com Node/Next e foi descartado.
3. CSRF no ambiente Vitest: Better Auth tem default específico para test.
   As checagens ficaram explicitamente ativas também nos testes.
4. Lint de navegação: login/logout usam router.replace + router.refresh.
5. Teste de rota aguardava apenas 307; passou a verificar o redirecionamento
   via meta documentado para streaming, sem reduzir a verificação de sigilo.
6. Audit de dependências: drivers corrigidos e override major autorizado;
   instalação final sem vulnerabilidades reportadas.

## Fontes oficiais

Revisão final de Git: 63 arquivos versionáveis não rastreados, incluindo os
arquivos preservados da Fase 1.1; sem commit, remote ou push. `.env`,
`.env.bootstrap.local` e Prisma Client gerado estão ignorados. A comparação
dos segredos locais com o conteúdo dos arquivos versionáveis não encontrou
vazamentos. O servidor de desenvolvimento foi encerrado após a inspeção;
os containers MySQL e seus volumes foram preservados.

**FASE 1.2 VALIDADA**. A primeira Super Admin real foi criada e o fluxo
autenticado local foi confirmado até logout. Nenhum segredo foi registrado.

- [Prisma MySQL e adapter](https://www.prisma.io/docs/orm/v7/core-concepts/supported-databases/mysql)
- [Better Auth Prisma](https://better-auth.com/docs/adapters/prisma)
- [CLI e verificação de schema](https://better-auth.com/docs/concepts/cli)
- [Opções oficiais](https://better-auth.com/docs/reference/options)
- [Sessões](https://better-auth.com/docs/concepts/session-management)
- [Integração Next](https://better-auth.com/docs/integrations/next)
- [Next redirect e streaming](https://nextjs.org/docs/app/api-reference/functions/redirect)
- [Catálogo de imagens oficiais MySQL](https://github.com/docker-library/official-images/blob/master/library/mysql)
- [Deepmerge-ts 8](https://github.com/RebeccaStevens/deepmerge-ts/releases/tag/v8.0.0)

Também foram consultados o registry npm e a documentação local da versão
instalada do Next em node_modules/next/dist/docs.
