# Fase Java 2 — RBAC

## Fonte de verdade e matriz

Baseline recuperado por `git show`, sem alterar o checkout:
`fabb67294509f89005c9bae0d7713b077f314691`.
Fontes: `prisma/seed-data.ts`, `src/lib/permissions/policy.ts`,
`src/lib/permissions/data.ts` e `tests/unit/policies.test.ts` naquele commit.

| Permission | SUPER_ADMIN | ADMIN | SUPERVISOR | SUPPORT |
| --- | --- | --- | --- | --- |
| users.view | sim | sim | sim | não |
| users.create | sim | sim | não | não |
| users.edit | sim | sim | não | não |
| users.disable | sim | sim | não | não |
| users.manage | sim | sim | não | não |
| teams.view | sim | sim | sim | sim |
| teams.create | sim | sim | não | não |
| teams.edit | sim | sim | não | não |
| teams.archive | sim | sim | não | não |
| teams.manage_members | sim | sim | não | não |
| permissions.view | sim | sim | não | não |
| permissions.manage | sim | não | não | não |
| settings.view | sim | sim | sim | sim |
| audit.view | sim | sim | não | não |

Total inicial: 4 Roles, 14 Permissions e 32 RolePermissions (14/13/3/2).
As keys seguem `resource.action`. O fixture de teste `rbac-baseline.json`
congela a matriz independentemente da implementação Java.

## Precedência confirmada

O baseline **não** usa DENY antes de SUPER_ADMIN. O teste antigo declara
explicitamente que SUPER_ADMIN possui bypass inclusive de DENY.

1. Profile ausente/inativo, role ausente/inválida ou permission desconhecida: negar.
2. SUPER_ADMIN: permitir a key existente no catálogo, inclusive com DENY.
3. Override individual: DENY nega; ALLOW permite.
4. Sem override: consultar RolePermission; ausência nega.

UserPermission tem chave composta `(user_id, permission_id)` e effect
ALLOW/DENY validado pelo banco. Assim não existem overrides duplicados ou
ALLOW e DENY simultâneos para a mesma usuária/permissão.

## Arquitetura

Role e Permission possuem UUID textual, key única, nome, descrição opcional e
timestamps. RolePermission e UserPermission são entities com IDs compostos,
FKs e associações LAZY. Profile → Role é obrigatório; o repository usa fetch
explícito para carregar a role. DTOs evitam serialização de entities.

PermissionPolicy centraliza a resolução. PermissionService carrega Profile,
Role, catálogo, grants e overrides em transação de leitura, sem cache. As
consultas de grants/overrides são conjuntos, sem loop de queries por permission.
PermissionGuard recebe o userId exclusivamente do principal autenticado.
Mudanças no banco afetam inclusive sessões existentes.

Method Security usa `@EnableMethodSecurity` e:

```java
@PreAuthorize("@permissionGuard.has(authentication, 'permissions.view')")
```

Referência: [Spring Security 6.5 — Method Security](https://docs.spring.io/spring-security/reference/6.5/servlet/authorization/method-security.html).

`GET /api/v1/permissions` retorna somente key/name do catálogo. Anônima recebe
401; autenticada sem permissão recebe 403. O filtro existente bloqueia inactive
e invalida a sessão antes do RBAC. `/auth/me` e login incluem role key/name e
somente permission keys efetivas. Não há API para escolher outro userId.

## Flyway e provisionamento seguro

V1/V2/V3 permanecem intactas. V4 cria `roles`, `permissions`, `role_permissions`,
`user_permissions` e `app_profile.role_id` inicialmente nullable com FK.
V5 é migration Java registrada como bean, executada pelo Flyway antes do JPA.
Ela usa a própria conexão do Flyway e o seed idempotente; ao final executa
`ALTER TABLE ... role_id ... NOT NULL`.

Em banco vazio, V5 apenas inicializa a matriz e exige integridade. Em banco
da Fase Java 1, exige **uma única User com Profile ACTIVE completa**,
`JAVA_RBAC_PROVISION_EXISTING=true`, email correspondente e senha válida do
bootstrap. Qualquer conta inesperada, estado parcial ou credencial incorreta
interrompe a migration antes do seed/provisionamento. Não há atribuição genérica
de SUPER_ADMIN a perfis existentes.

O fluxo administrativo é:

```powershell
powershell -NoProfile -File backend/bootstrap.ps1
powershell -NoProfile -File backend/run-dev.ps1
```

O script lê configurações locais ignoradas, habilita o provisionamento apenas
naquele processo, executa em 18081 e encerra. Nunca imprime credenciais. V5
altera apenas a role do Profile identificado; preserva User.id, email, nome,
passwordHash e quantidade de usuárias. A transação do bootstrap cria novas
identidades com SUPER_ADMIN localizada pela key e recusa estado parcial.

Não é necessário manter `JAVA_RBAC_PROVISION_EXISTING` habilitado no servidor.
Após V5 aplicada, execuções normais não precisam das credenciais de bootstrap.
O seed é reexecutado explicitamente pelo bootstrap: completa grants padrão
faltantes e preserva decisões administrativas extras, como o seed Prisma.

MySQL faz commit implícito em DDL; uma falha real de migration exige inspecionar
o estado antes de qualquer `repair`. Não executar `clean`, reset ou edição de
migrations aplicadas. V5 e RbacBaseline constituem baseline versionado: futuras
alterações da matriz devem usar nova migration, sem modificar esse baseline.

## Validação e limites

Testes unitários exercitam resolver e guard; compatibilidade cobre as 56
combinações. Integração MySQL/Testcontainers verifica matriz persistida, seed
idempotente, constraints, overrides, SUPER_ADMIN, inactive, 401/403, DTOs,
Method Security e atualização de permissões em sessão existente. Um teste
dedicado migra banco V3 com identidade e confirma preservação de credenciais.

Comandos: `backend/mvnw.cmd -f backend/pom.xml test` e `package`; em
`frontend/`, `npm run typecheck`, `npm run lint` e `npm run build`.

O cliente técnico Java tipa role e permissions. O frontend oficial permanece
em Better Auth/Prisma. Não há Teams, gestão de Users, Invites ou Tasks Java,
nem cache, Redis ou AuditLog novo. Rate limiting continua hardening futuro.

### Estado da validação em 2026-10-01

Implementação local ainda não publicada. Passaram 13 testes unitários Java,
56 casos de compatibilidade, typecheck, lint e build do frontend. Uma comparação
executou o policy/seed extraído do Git e confirmou que o fixture coincide com
as 14 keys e os 32 vínculos originais. Secret scan sem achados; V1/V2/V3 intactas.

A suíte completa foi interrompida no setup dos quatro grupos Testcontainers
porque o Docker Engine não estava disponível. O Docker Desktop registrou falha
ao acessar/renomear `sailor-ingest.sock` durante sua inicialização. Nenhuma
migration foi aplicada ao banco local e a Super Admin existente não foi alterada.

Pendentes: recuperar o Engine, executar testes integrados/upgrade, package,
bootstrap administrativo, HTTP real, revisão final e commit/push. Não considerar
esta fase validada até concluir essas verificações.
