# Autorização e RBAC

**Estado Java: em migração.** O RBAC funcional dos módulos atuais é executado
no servidor Next.js com Prisma. A implementação Java possui trabalho local em
revisão e ainda depende de validação integrada, provisionamento e publicação.
Não considerar o endpoint Java de permissões disponível como contrato concluído.

## Modelo

| Conceito | Responsabilidade |
| --- | --- |
| Role | Perfil de acesso com key estável: SUPER_ADMIN, ADMIN, SUPERVISOR ou SUPPORT |
| Permission | Capacidade identificada por resource.action |
| RolePermission | Grant de uma permission para uma role |
| UserPermission | Override ALLOW ou DENY para uma usuária e permission |
| Profile | Dados organizacionais, status e vínculo com a role |

Role e cargo descritivo são distintos. TeamMember define participação em equipes,
não substitui RolePermission. A relação Profile → Role é obrigatória no domínio
Prisma; a migração Java deve garantir essa integridade após provisionar os perfis
existentes. Não usar booleanos administrativos ou uma role sem relacionamento.

Chaves compostas impedem RolePermission repetida e múltiplos overrides da mesma
permission para a mesma usuária. ALLOW e DENY não podem coexistir nesse par.

## Catálogo e matriz vigente

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

São 4 roles, 14 keys e 32 grants padrão. A existência de uma key não significa
que sua tela ou módulo já foi entregue. O seed Prisma completa grants padrão
faltantes sem apagar ajustes administrativos extras.

## Resolução e precedência

A política central do sistema atual aplica:

1. Profile ausente/inativo ou key desconhecida: negar.
2. SUPER_ADMIN ativo: permitir, inclusive diante de override DENY.
3. Nas demais roles, override DENY nega; override ALLOW permite.
4. Sem override, consultar RolePermission; ausência de grant nega.

Portanto DENY individual prevalece sobre grants de ADMIN, SUPERVISOR e SUPPORT,
mas não sobre o bypass de SUPER_ADMIN. Essa regra deve ser preservada na
migração e coberta por testes de compatibilidade.

As implementações atuais estão em `frontend/src/lib/permissions/`, e o seed
em `frontend/prisma/seed-data.ts`. Membership direto ou permissão de gestão
pode autorizar acesso a uma equipe; não há herança automática de membership
para equipes descendentes.

## Estratégia Java em migração

O desenho usa PermissionPolicy, PermissionService e PermissionGuard para
centralizar decisões. O guard recebe a identidade do principal autenticado;
o cliente não escolhe o userId avaliado. Profile, role, grants e overrides são
consultados no banco, sem cache de permissões na sessão.

Method Security deve proteger os métodos com `@EnableMethodSecurity` e, por
exemplo:

```java
@PreAuthorize("@permissionGuard.has(authentication, 'permissions.view')")
```

O endpoint previsto `GET /api/v1/permissions` expõe somente key/name, com 401
para anônima e 403 para autenticada sem acesso. O contrato de `/auth/me` deverá
incluir role key/name e apenas keys efetivas. Inactive continua bloqueada antes
da autorização.

O provisionamento da identidade Java existente deve ser explícito, sem duplicar
User, trocar email ou recriar senha. Seed deve ser idempotente e preservar
decisões administrativas. O procedimento de migração deve ser validado antes
de ser adotado como setup normal.

Evidências e pendências específicas estão no
[registro histórico de RBAC Java](../history/fase-java-2-rbac.md).
As regras vigentes pertencem a este documento e devem acompanhar futuras mudanças.
