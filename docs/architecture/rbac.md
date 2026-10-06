# Autorização e RBAC

O RBAC oficial pertence ao Spring Boot. VerificadorPermissao protege métodos;
/auth/me retorna apenas permissões efetivas. O frontend usa essas keys para
menus e botões, sem decidir acesso aos dados.

## Modelo

| Conceito | Responsabilidade |
| --- | --- |
| PerfilAcesso | Perfil de acesso com key estável: SUPER_ADMIN, ADMIN, SUPERVISOR ou SUPPORT |
| Permissao | Capacidade identificada por resource.action |
| PermissaoPerfilAcesso | Grant de uma permission para uma role |
| PermissaoUsuario | Override ALLOW ou DENY para uma usuária e permission |
| Perfil | Dados organizacionais, status e vínculo com a role |

PerfilAcesso e cargo descritivo são distintos. MembroEquipe define participação em equipes,
não substitui PermissaoPerfilAcesso. A relação Perfil → PerfilAcesso é obrigatória no domínio
Java. O schema Java exige `role_id` válido após provisionamento
controlado dos perfis existentes. Não usar booleanos administrativos ou uma
role sem relacionamento.

Chaves compostas impedem PermissaoPerfilAcesso repetida e múltiplos overrides da mesma
permission para a mesma usuária. ALLOW e DENY não podem coexistir nesse par.

## Catálogo e matriz vigente

| Permissao | SUPER_ADMIN | ADMIN | SUPERVISOR | SUPPORT |
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
| tasks.view | sim | sim | sim | sim |
| tasks.create | sim | sim | sim | sim |
| tasks.edit | sim | sim | sim | sim |
| tasks.assign | sim | sim | sim | não |
| tasks.archive | sim | sim | não | não |
| projects.view | sim | sim | sim | sim |
| projects.create | sim | sim | sim | não |
| projects.edit | sim | sim | sim | não |
| projects.manage_members | sim | sim | sim | não |
| projects.archive | sim | sim | não | não |

São 4 roles, 24 keys e 64 grants padrão. A existência de uma key não significa
que sua tela ou módulo já foi entregue. O seed completa grants padrão
faltantes sem apagar ajustes administrativos extras.

Tarefas centralizam escopo em PoliticaTarefa/TarefaRepository: SUPER_ADMIN e
ADMIN têm escopo global condicionado às permissions; SUPERVISOR e SUPPORT
acessam somente equipes das quais participam. SUPPORT edita/move somente as
tarefas que criou ou que lhe foram atribuídas. Override ALLOW não amplia o
escopo de equipe. DENY continua prevalecendo fora do bypass SUPER_ADMIN.

## Resolução e precedência

Projetos centralizam política em `PoliticaProjeto` e escopo SQL em
`ProjetoRepository`. ADMIN/SUPER_ADMIN têm escopo global; SUPERVISOR e SUPPORT
somente suas equipes. Responsabilidade não amplia acesso nem concede permissions.
SUPPORT permanece somente leitura neste domínio, inclusive diante de ALLOW de
escrita. Equipe arquivada mantém leitura histórica, sem mutações. Criar/editar/
gerenciar responsáveis/arquivar são capacidades separadas; mudar responsáveis
exige `projects.manage_members`, além da permissão da operação. V9 e seed
acrescentam grants sem alterar Users, Teams, Tasks ou Invites.

A política central do sistema atual aplica:

1. Perfil ausente/inativo ou key desconhecida: negar.
2. SUPER_ADMIN ativo: permitir, inclusive diante de override DENY.
3. Nas demais roles, override DENY nega; override ALLOW permite.
4. Sem override, consultar PermissaoPerfilAcesso; ausência de grant nega.

Portanto DENY individual prevalece sobre grants de ADMIN, SUPERVISOR e SUPPORT,
mas não sobre o bypass de SUPER_ADMIN. Testes de compatibilidade verificam que
a implementação Java reproduz essa regra e a matriz vigente.

PoliticaPermissao, PermissaoService e VerificadorPermissao ficam em
backend/src/main/java/com/devannalu/tsworkspace/rbac/. Membership direto ou
permissão de gestão autoriza acesso à equipe; não há herança automática.

## Estratégia Java

O desenho usa PoliticaPermissao, PermissaoService e VerificadorPermissao para
centralizar decisões. O guard recebe a identidade do principal autenticado;
o cliente não escolhe o userId avaliado. Perfil, role, grants e overrides são
consultados no banco, sem cache de permissões na sessão.

Method Security protege os métodos com `@EnableMethodSecurity` e, por
exemplo:

```java
@PreAuthorize("@verificadorPermissao.possuiPermissao(authentication, 'permissions.view')")
```

O endpoint `GET /api/v1/permissions` exige `permissions.view` e expõe somente
key/name, com 401 para anônima e 403 para autenticada sem acesso. Login e
`/auth/me` incluem role key/name e apenas keys efetivas. Inactive continua bloqueada antes
da autorização.

O provisionamento da identidade Java existente é explícito, sem duplicar Usuario,
trocar email ou recriar senha. V4 cria as relações; V5 verifica a identidade
única ACTIVE mediante as credenciais locais, associa SUPER_ADMIN e impõe
`role_id NOT NULL`. O seed é idempotente, completa grants padrão faltantes e
preserva decisões administrativas extras. Veja o [setup](../development/setup.md).

Evidências e pendências específicas estão no
[registro histórico de RBAC Java](../history/fase-java-2-rbac.md).
As regras vigentes pertencem a este documento e devem acompanhar futuras mudanças.
