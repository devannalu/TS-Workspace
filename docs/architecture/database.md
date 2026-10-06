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

`V8__tasks.sql` adiciona `task` e `task_assignee`, sem editar V1–V7. Task contém
title/description/status/priority/team_id/created_by_id/due_date/position/version
e timestamps de criação, alteração e arquivamento. Prazo é DATE; timestamps são
TIMESTAMP(6). Checks restringem quatro status, quatro prioridades e posição não
negativa. FKs de equipe/criadora impedem exclusões indevidas; assignee tem PK
`(task_id,user_id)`, FK da tarefa com cascade e da usuária com restrict.
Índices cobrem quadro ativo por status/posição, equipe, prazo e responsável.
V8 também completa cinco permissions e seus grants, preservando concessões antigas.

Arquivamento mantém conteúdo, vínculos e auditoria. Equipe arquivada mantém
tarefas históricas legíveis, mas impede criação, edição e movimentação; arquivar
uma tarefa histórica ainda é permitido com `tasks.archive`. Remover membership
com responsabilidade em tarefa ativa retorna 409; primeiro retire a atribuição
ou arquive a tarefa. Tarefas arquivadas não bloqueiam essa remoção.

`V9__projects.sql` adiciona `project` e `project_responsible`; acrescenta
`task.project_id` nullable, com FK restrict e índice por projeto/arquivo/status.
As tarefas anteriores mantêm null. Project guarda título (200), descrição (5000
validado na API), status, equipe obrigatória, criadora, datas DATE opcionais,
versão e timestamps. Checks garantem status e período não invertido. Responsáveis
usam PK `(project_id,user_id)`, FK de projeto cascade e de usuária restrict.
Índices atendem equipe, status, prazo e responsável. Não há coluna de progresso.

`first_task_linked_at` registra uma única vez o primeiro vínculo de tarefa. Não
é métrica nem antecipação de funcionalidade: impede trocar a equipe de um projeto
que já teve tarefas, mesmo após desvincular todas. O marco permanece; atualizá-lo
incrementa a versão. Serviço serializa vínculos, status, troca de equipe e arquivo
com o bloqueio transacional existente. V9 também acrescenta cinco permissions e
15 grants, sem alterar V1–V8 ou concessões anteriores. Arquivar projeto mantém
tarefas concluídas e vínculos históricos, sem autoarquivar tarefas.

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

## Comentários — V10

workspace_comment: id, task_id nullable, project_id nullable, author_id, content
TEXT, version, created_at, updated_at, removed_at e removed_by_id nullable.
CHECK exige exatamente um recurso; conteúdo trim entre 1 e 5000 caracteres.
FKs RESTRICT preservam recurso/autoria/histórico. Índices por tarefa/data/id,
projeto/data/id e autora; FK de remoção mantém integridade administrativa.
V1–V9 não mudam. V10 adiciona três permissions e nove grants, preservando antigos.
Audit_log não muda: atividade é leitura contextual, sem persistência duplicada.


## Anexos e armazenamento

V11 adiciona attachment: vínculo exclusivo task_id/project_id, uploader_id, original_name, object_key único, MIME, size_bytes limitado a 10 MiB, state PENDENTE/DISPONIVEL/REMOVIDO e timestamps. Nenhum binário ou URL assinada no MySQL.
