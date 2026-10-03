# Equipes

## Modelo e compatibilidade

Uma usuária pode participar de várias equipes por TeamMember. Team tem UUID,
key estável, nome, descrição opcional, parent opcional, arquivamento e timestamps.
TeamMember tem chave composta user/equipe e data de criação. Não existe User.teamId.
Entidades JPA não são serializadas pela API.

A fonte validada é o commit `fabb67294509f89005c9bae0d7713b077f314691`:
schema Prisma, seed-data, teams/hierarchy, teams/admin-service e testes de
políticas e provisionamento. A fixture `teams-baseline.json` congela nomes,
keys, árvore e casos da política antiga para comparação com MySQL real.

As cinco equipes iniciais são Fundadoras (raiz), Comunicação, Eventos,
Desenvolvimento de Projetos e Comunicação Interna. As quatro últimas usam
o UUID real de Fundadoras como parent. Frentes conceituais não viram equipes
automaticamente. O seed completa registros ausentes sem duplicar ou desfazer
decisões administrativas. A migration associa a Super Admin já existente à raiz.

## Regras

O fluxo administrativo exige parent para equipes comuns. Rejeita self-parent,
parent inexistente/arquivado, ciclos e descendentes como parent. Equipes
arquivadas não podem ser editadas ou receber/remover membros pela API.
Fundadoras, identificada por key, não pode ser movida ou arquivada. Não há
endpoint de exclusão física. A raiz também permanece protegida pela estrutura.

Como no Prisma, arquivar uma equipe com filhas ativas é recusado: primeiro
mova ou arquive as filhas. Arquivamento repetido é idempotente, e memberships
históricos permanecem. A listagem omite equipes arquivadas; detalhe por ID
permite consultar uma equipe arquivada. Contagens e integrantes visíveis
consideram somente Profiles ativas, como a listagem anterior.

Adicionar membro exige User existente e Profile ativa. A PK impede duplicação.
Remoção permite retirar membership de uma usuária inativa de equipe ativa.
Em Fundadoras, uma integrante com role SUPER_ADMIN não pode ser removida
quando a contagem de Super Admins ativas é menor ou igual a um. Essa condição,
inclusive para alvo inativo, é a regra exata do serviço Prisma validado.

Escritas são transacionais e bloqueiam Fundadoras antes de ler a árvore ou
contar administradoras. Isso impede ciclos causados por movimentos simultâneos
e duas remoções que deixariam a raiz sem Super Admin ativa. Não substitui as
constraints do banco. A [gestão de usuárias](users-invites.md) compartilha esse
bloqueio e protege perda da última administradora da raiz ao mudar role/status.

## API

Sessão Java e CSRF são obrigatórios nas escritas. Method Security reutiliza
PermissionGuard e as permissões existentes, consultadas no banco.

| Método e rota | Permissão | Resposta |
| --- | --- | --- |
| GET /api/v1/teams | teams.view | Lista flat ativa, parentId e memberCount agregado |
| GET /api/v1/teams/{id} | teams.view | Team e integrantes ativas (id, nome, email) |
| POST /api/v1/teams | teams.create | 201, detalhe da equipe criada |
| PUT /api/v1/teams/{id} | teams.edit | 200, detalhe atualizado |
| POST /api/v1/teams/{id}/archive | teams.archive | 200, equipe arquivada |
| POST /api/v1/teams/{id}/members | teams.manage_members | Body userId, 204 |
| DELETE /api/v1/teams/{id}/members/{userId} | teams.manage_members | 204 |

Create/edit recebem nome de 2–100 caracteres após trim, descrição opcional
até 500 e parentId UUID. IDs malformados/dados inválidos retornam 400;
ausência de sessão 401; permissão/CSRF negados 403; entidade inexistente 404;
membership duplicada ou conflito estrutural 409. Não se expõem hashes,
sessões, SQL ou exceptions de persistência.

Consultas de listagem usam uma agregação para memberCount; detalhe faz duas
consultas, sem carregar coleções JPA por equipe. `/auth/me` permanece com o
contrato anterior. O cliente técnico `frontend/src/lib/api/teams.ts` usa cookies
e CSRF; a página `/equipes` permanece no Next.js/Prisma. Não há sincronização
automática entre os bancos. Users e Invites Java possuem clientes técnicos;
Tasks Java ainda não está implementado.

## Validação

TeamPolicyTest cobre regras reais de hierarquia, arquivamento e memberships.
TeamsIntegrationTest usa Testcontainers/MySQL para Flyway, seed idempotente,
compatibilidade com a fixture Prisma, CRUD, FKs, RBAC, CSRF e concorrência
de memberships, movimentos e remoções da última administradora.
RbacUpgradeTest também verifica a associação à raiz ao atualizar uma identidade
preexistente, preservando seu ID, email e hash.
