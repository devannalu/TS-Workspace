# Fase 1.3 — convites, usuárias e equipes

Esta etapa adiciona o módulo administrativo sobre a fundação da Fase 1.2.

## Convites

`Invite` é uma entidade própria e `InviteTeam` normaliza as equipes. O token
é gerado com `randomBytes(32)`, salvo apenas como SHA-256 e devolvido uma vez
como link de desenvolvimento. O prazo padrão é de sete dias. Estados são
derivados de `usedAt`, `cancelledAt` e `expiresAt`.

O aceite usa uma instância Better Auth isolada dentro de uma transação Prisma:
ela cria a credencial, Profile active, cargo e memberships e registra
`invite.accepted`. O `updateMany` condicional reserva o convite dentro da
transação; por isso duas aceitações concorrentes só podem concluir uma vez.
Falhas fazem rollback completo. SMTP não foi adicionado.

## Administração

`/usuarias` lista dados reais do MySQL, cria/cancela convites e permite alterar
cargo, status e equipes. Inativação revoga sessões e o guard de sessão bloqueia
novos acessos; a última Super Admin ativa não pode ser removida do controle.

`/equipes` carrega a árvore real, cria, edita, arquiva e gerencia integrantes.
Fundadoras permanece como raiz estrutural. A validação server-side impede
self-parent, parent inválido/arquivado, ciclos e segunda raiz; memberships usam
chave composta e a última Super Admin não pode ser removida de Fundadoras.

Permissões novas: `users.create`, `users.edit`, `users.disable` e
`teams.archive`, além das permissões existentes. O seed completa grants novos
sem remover decisões administrativas existentes.

## Validação

- Migration `20260929174551_add_invites_and_user_management` criada e aplicada.
- Seed executado duas vezes: 4 roles, 14 permissions, 32 role permissions e 5 teams.
- Testes unitários e 16 testes de integração com MySQL real passaram.
- Typecheck, lint e build passaram.
- O fluxo de aceite, single-use, expiração, cancelamento e concorrência foi
  exercitado em `tests/integration/phase13.test.ts`.

Nenhum SMTP foi configurado, nenhuma migration anterior foi alterada e nenhum
token ou senha é persistido em claro.
