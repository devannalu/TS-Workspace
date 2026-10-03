# Fase Java 4 — Usuárias e convites

Checkpoint finalizado em 2026-10-02, a partir de
`a44d1fa0967abc34c5726f7c4d09c870b7f6458a`. Fonte Prisma validada:
`fabb67294509f89005c9bae0d7713b077f314691`, serviços users/invites,
token policy, schema e testes de convites/provisionamento.
Veja [Users/Invites](../architecture/users-invites.md) para a documentação viva.

## Implementação e decisões

V7 cria Invite, InviteTeam e AuditLog; expande principal_name de Spring Session
a 320 caracteres para suportar emails permitidos por User. V1–V6 intactas,
Hibernate validate, identidade e role oficial preservadas.

Tokens usam SecureRandom de 32 bytes, hex e SHA-256. O prazo padrão é sete dias,
com expiresInDays opcional de 1–30 conforme o legado. Estados são derivados;
somente PENDING pode ser cancelado. Email existente e pendência válida bloqueiam
novo convite; token/link é devolvido uma vez, sem persistência do segredo.

Users possui paginação, filtros, options, jobTitle, role/equipes e endpoints
separados de activate/deactivate com suas permissões correspondentes.
OrganizationLock compartilha Fundadoras entre Teams, Users e Invites;
as proteções global e da raiz compõem-se também na mudança de role/status.

Validação/aceite públicos usam POST com token no corpo e CSRF mantido.
Não existe signup público, cadastro administrativo direto ou sessão automática
após aceite. BCrypt 12 exige mínimo de 12 caracteres, máximo 128 e 72 bytes
UTF-8, com confirmação, respeitando o encoder sem truncamento silencioso.

User/Profile/memberships/usedAt/auditoria participam de uma transação.
O aceite/cancelamento também bloqueia o Invite FOR UPDATE.
Inativação revoga todas as sessões; reativação não restaura sessões antigas.
Auditoria registra seis eventos mínimos com IDs e metadados nulos, sem segredos.

## Correções

O primeiro teste de falha intermediária usava trigger. O usuário MySQL de teste
não possui SUPER com binlog habilitado (erro 1419). O teste passou a usar uma
CHECK temporária em audit_log do banco efêmero, comprovando rollback integral
sem alterar privilégios ou configuração global.

Expectativas de versão Flyway/upgrade foram atualizadas para V7. O aviso existente
da matriz Flyway/MySQL 8.4 não impediu migrations, validate ou testes.

## Evidências

- Test e package: 146 testes cada, zero falhas, erros ou skips; BUILD SUCCESS e JAR.
- Novos testes: nove InvitePolicy, sete UserPolicy e 14 de integração UsersInvites,
  incluindo compatibilidade com fixture independente e concorrência real.
- Frontend: typecheck, lint sem warnings e build aprovados; clientes técnicos
  users.ts/invites.ts/management.ts, sem troca das interfaces oficiais.
- HTTP real: login/listagem 200, criação/aceite 201, validação 200, ausência de
  sessão automática 401, login/me da nova User 200, logout 204, inativação 200,
  sessão revogada/login inativo 401, reativação/login 200, sessão antiga ainda
  inválida 401, alteração de role/equipe 200, cancelamento 200 e aceite cancelado 400.
- Seis eventos de auditoria confirmados sem token/senha. Cleanup somente dos
  dados temporários; comparação integral dos dados oficiais antes/depois.
- Identidade/hash preservados, uma User/Profile ACTIVE SUPER_ADMIN, cinco equipes,
  quatro roles, 14 permissions e 32 grants. Health 200, MySQL saudável, V1–V7 aplicadas.

## Limites preservados

Sem cutover de login, usuárias, equipes ou convite. inviteUrl Java é preparado
para o futuro frontend; a página Prisma preservada não o consome. Nesta etapa,
aceite Java utiliza o cliente técnico/API. SMTP, recuperação de senha, rate
limiting distribuído e Tasks/Projects/Events ficam fora do escopo.
Prisma/Better Auth permanecem preservados.
