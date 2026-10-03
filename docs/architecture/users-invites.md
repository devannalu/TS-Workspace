# Usuárias e convites

## Entrada e identidade

Novas pessoas entram na API Java por convite, nunca por signup público ou
POST administrativo de User. Invite associa email normalizado, Role e Teams;
InviteTeam possui chave composta e FKs. O aceite cria User, Profile ACTIVE e
TeamMembers na mesma transação que marca usedAt e registra auditoria.
Qualquer falha provoca rollback integral. Não cria sessão automaticamente:
a pessoa autentica normalmente depois do aceite.

O token usa SecureRandom com 32 bytes (256 bits), codificados como 64 caracteres
hexadecimais. Somente SHA-256(token) vai para o banco, com índice único.
O token/link aparece apenas na resposta da criação autorizada; não é salvo
em auditoria, logs ou listagens. Prazo padrão: sete dias exatos, com a opção
validada do legado expiresInDays de 1–30 dias. Convites sempre expiram.

O estado é derivado, nesta ordem: usedAt → USED, cancelledAt → CANCELLED,
expiresAt menor ou igual ao instante atual → EXPIRED; demais → PENDING.
Somente PENDING pode ser cancelado. User existente ou convite PENDING válido
para o email impede nova criação. Expirados/cancelados permanecem no histórico
e permitem novo convite com novo token. Teams arquivadas invalidam o aceite.

## API e segurança

| Método e rota | Permissão | Contrato |
| --- | --- | --- |
| GET /api/v1/users | users.view | Page items/total/page/size |
| GET /api/v1/users/{id} | users.view | DTO da identidade/perfil e equipes ativas |
| GET /api/v1/users/options | users.view | Roles existentes e equipes ativas para seleção |
| PATCH /api/v1/users/{id} | users.edit | jobTitle, roleId e/ou teamIds |
| POST /api/v1/users/{id}/deactivate | users.disable | Profile INACTIVE e revogação de sessões |
| POST /api/v1/users/{id}/activate | users.disable | Profile ACTIVE, novo login obrigatório |
| GET /api/v1/invites | users.view | Page de convites, sem token/hash/link |
| GET /api/v1/invites/pending-count | users.view | Número agregado de convites PENDING; sem lista ou dados pessoais |
| POST /api/v1/invites | users.create | email, roleId, 1–20 teamIds; expiresInDays opcional |
| POST /api/v1/invites/{id}/cancel | users.create | Somente convite pendente |
| POST /api/v1/invites/validate | Pública, com CSRF | Body token; email, label de role, nomes de teams e expiresAt |
| POST /api/v1/invites/accept | Pública, com CSRF | token, name, password e passwordConfirmation; 201 |

Listagens usam page a partir de zero, size padrão 25 e máximo 100. Users aceita
status ACTIVE/INACTIVE, roleId, teamId e search literal por nome/email, até
160 caracteres. Ordenação usa createdAt e UUID como desempate. Listagem faz
consulta de total, página e uma consulta bulk de memberships; não há N+1.
Invites também usa consultas bulk para relacionar equipes à página.

PATCH mantém campos ausentes; jobTitle vazio limpa o cargo descritivo.
Role deve existir. Team IDs são deduplicados, limitados a 50 e precisam estar
ativos. Quando teamIds é informado, substitui as memberships, como no legado.
Não altera email ou senha, nem oferece delete físico.

Tokens ficam no corpo, sem rota/query de lookup que os inclua em access logs.
Todos os POST/PATCH exigem o cookie/header CSRF, inclusive validate/accept
sem sessão. CORS segue a origem explícita; não há relaxamento da cadeia global.
Dados extras de email/role/team no aceite não participam do provisionamento:
somente o Invite persistido define esses valores. A resposta pública de erro
para token inexistente, usado, expirado, cancelado ou indisponível é genérica,
400, sem email/hash/IDs internos. Consulta pública válida omite IDs internos.

Endpoints administrativos usam PermissionGuard/Method Security: 401 sem sessão,
403 sem permissão/CSRF, 404 para entidade ausente, 409 para conflito ou proteção
administrativa, 400 para validação. Senha usa o encoder BCrypt força 12 existente,
mínimo 12 caracteres, máximo 128 e limite de 72 bytes UTF-8, com confirmação igual.
DTOs nunca incluem passwordHash, session IDs ou cookies.

## Proteções e concorrência

OrganizationLock adquire Fundadoras com FOR UPDATE no início de todas as
escritas de equipes, Users e Invites. Convites também são carregados FOR UPDATE
no aceite/cancelamento. Isso serializa criação pendente por email, aceite
single-use e alterações simultâneas de administração; constraints de email,
token e memberships complementam os serviços. BCrypt ocorre dentro da
transação do aceite. O bloqueio global favorece integridade nesta etapa e
serializa escritas organizacionais; evolução de escala requer desenho próprio.

A política protege a última SUPER_ADMIN ativa contra perda de role/status,
e impede que a própria administradora perca seu acesso. A atualização de equipes
protege a última Super Admin ativa em Fundadoras. O Java compõe essa proteção
com a de Teams: mudar role/status também não pode deixar a raiz sem uma
Super Admin ativa, mesmo se existir outra Super Admin fora de Fundadoras.
O campo jobTitle novo pode ser atualizado sem mudar acesso administrativo.

PATCH exige users.edit; activate/deactivate exigem users.disable.
Não existe status arbitrário no PATCH. Role/permissões refletem o
banco nas requisições seguintes; só inativação revoga sessões, como no legado.

SessionRevocationService remove todas as sessões pelo email principal indexado,
com remoção em cascade dos atributos. O filtro existente também bloqueia
qualquer sessão de Profile inativa. Reativação preserva identidade e não
restaura sessões antigas. V7 expande principal_name para o tamanho do email.

## Auditoria e coexistência

AuditLog registra invite.created, invite.cancelled, invite.accepted,
user.role_changed, user.status_changed e user.teams_changed. Só registra mudanças
efetivas; não recebe metadados arbitrários. Metadados são nulos nesta fundação,
e nunca incluem token, hash, senha, cookie ou sessão. Eventos participam da
mesma transação da operação. Não existe UI/endpoint público de auditoria.

As páginas oficiais usam users.ts/invites.ts. /convite/[token] valida e aceita
convites Java, com senha confirmada e direcionamento para login.

SMTP, recuperação de senha, gestão visual de sessões, rate limiting distribuído,
Tasks permanecem fora deste módulo. Não há Redis ou rate limit
em memória apresentado como segurança de produção.

## Verificação

Unitários cobrem token/hash, estados, validação e política administrativa.
UsersInvitesIntegrationTest usa MySQL/Testcontainers para V1–V7, seed, DTOs,
aceite, concorrência, rollback provocado por constraint temporária, revogação
de todas as sessões, filtros/paginação, autorização e signup proibido.
A fixture users-invites-baseline.json documenta fontes Git e congela casos
independentes de status/proteção para compatibilidade. O fluxo HTTP real usa
identidades temporárias e cleanup restrito aos dados criados pelo teste.
