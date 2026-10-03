# Banco de dados

## MySQL oficial

O serviço mysql-java do Compose usa ts_workspace_java em 127.0.0.1:3309.
O frontend não acessa bancos. Testcontainers usa instâncias efêmeras isoladas.
Volumes persistem dados e não devem ser removidos como parte do setup.

## Modelagem

- Identificadores de domínio usam UUID; seeds localizam registros por keys estáveis.
- Unicidade e relações devem ser garantidas também por constraints e foreign keys.
- User representa identidade; Profile representa dados organizacionais e status.
- O RBAC relaciona Role, Permission, RolePermission e UserPermission.
- Usuárias e equipes possuem relação many-to-many por TeamMember no domínio atual.
- Convites guardam hash do token e relacionam equipes por InviteTeam.
- Senhas, cookies e tokens não pertencem a metadados de auditoria.

No Java, identidade, sessões e [RBAC](rbac.md) estão disponíveis. Profile tem
relação obrigatória com Role, e as tabelas RBAC possuem FKs e constraints de
unicidade. Team, TeamMember e Invites são atendidos pela API oficial.

`V6__teams.sql` cria `team` e `team_member`, preservando V1–V5. `team_key`
é uma chave única estável, usada para reconhecer
Fundadoras sem fixar UUID. A self FK restringe exclusão/alteração do parent.
Uma coluna gerada `root_slot` com índice único limita o banco a uma raiz.
TeamMember tem PK `(user_id, team_id)`, FK para User com cascade e FK para
Team com restrict; índices atendem busca por equipe, parent e status.

A migration cria as cinco equipes e associa Super Admins ativas existentes
a Fundadoras, sem alterar User/Profile/Role. `TeamSeed.seed()` completa dados
faltantes de forma idempotente e preserva nomes, parents e arquivamentos
administrativos; recusa Fundadoras em estado inválido. A execução explícita
também completa memberships de Super Admins ativas em Fundadoras.

`V7__users_invites_audit.sql` adiciona Invite, InviteTeam e AuditLog, com FKs,
PK composta invite/team, token_hash único e índices por email/estado/expiração,
criação e entidade/ator de auditoria. Só o SHA-256 do token é persistido;
estado do convite é derivado dos timestamps. AuditLog permite ator nulo e
metadados JSON nulos, sem credenciais ou tokens. A coluna principal_name de
Spring Session passa a 320 caracteres para comportar o email da User.

Aceite é inteiramente atômico no MySQL: identidade, Profile, memberships,
usedAt e auditoria. Não depende de uma API externa de cadastro. Revogação
de sessões usa o índice principal_name e cascade das session attributes.
V1–V6 permanecem intactas; Hibernate continua em validate.

## Migrations

Flyway controla o schema Java em `backend/src/main/resources/db/migration/`;
migrations Java, quando usadas, são registradas explicitamente. Hibernate
mantém `ddl-auto=validate`, e Flyway possui `clean-disabled=true`.

Não editar migrations já aplicadas ou usar reset para contornar erros.
Mudanças recebem novas versões.

Ao adicionar coluna obrigatória a tabelas com dados, separar criação,
provisionamento controlado e imposição de integridade. DDL MySQL pode realizar
commit implícito; não tratar toda alteração estrutural como rollback automático.
Inspecionar falhas antes de qualquer reparo de histórico.

## Transações

Criação de identidade/perfil, aceite de convite e mudanças organizacionais
devem preservar atomicidade e suas invariantes. No backend atual, operações
sensíveis de hierarquia e gestão usam transações e validações server-side.
No Java, serviços usam transações Spring para operações relacionadas.
Todas as escritas administrativas de equipes bloqueiam primeiro a mesma linha
Fundadoras com `FOR UPDATE`. Isso serializa alterações da árvore e checagens da
última Super Admin entre instâncias da API. Constraints complementam as regras
do serviço. Veja [Equipes](teams.md).

## Testes e dados locais

Testcontainers cria MySQL efêmero com portas dinâmicas para integração Java.
O banco de desenvolvimento não substitui o banco de testes.

Volumes locais persistem dados. Não usar remoção de volumes, reset, clean ou
limpeza de outros projetos como parte de um setup rotineiro.
Veja [setup](../development/setup.md) e [testes](../development/testing.md).
