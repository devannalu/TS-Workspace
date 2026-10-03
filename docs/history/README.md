# Histórico de engenharia

Estes documentos registram fatos, decisões, validações e limitações das suas
respectivas execuções. Não são o guia atual de instalação nem uma descrição
completa do estado presente do produto.

| Registro | Conteúdo |
| --- | --- |
| [Fase 1.1](fase-1.1.md) | Ambiente inicial e fundação Next.js |
| [Fase 1.2](fase-1.2.md) | MySQL, Prisma, identidade, permissões e bootstrap |
| [Fase 1.3](fase-1.3.md) | Convites, gestão de usuárias e equipes |
| [Fundação Java](fundacao-java.md) | Recuperação, monorepo, Spring Boot e integração técnica |
| [Fase Java 1](fase-java-1-auth.md) | Autenticação Java e sessões persistentes |
| [Fase Java 2](fase-java-2-rbac.md) | RBAC, bloqueio inicial de Docker e retomada com validação integrada |
| [Fase Java 3](fase-java-3-teams.md) | Teams, hierarquia, memberships e validação de compatibilidade |
| [Fase Java 4](fase-java-4-users-invites.md) | Usuárias, convites, provisionamento atômico, revogação de sessões e auditoria mínima |
| [Fase Java 5](fase-java-5-cutover.md) | Cutover oficial, inventário e preservação do banco anterior |
| [Fase 6](fase-6-frontend-shell-dashboard.md) | Identidade pastel, shell, login, dashboard real e interfaces administrativas |
| [Fase 7](fase-7-padronizacao-codigo.md) | Nomes PT-BR, responsabilidades de persistência, organização compacta e contratos preservados |

Os registros foram preservados ao reorganizar a documentação. Caminhos citados
nas primeiras fases refletem a raiz usada na época; código Next.js, scripts,
testes e Prisma hoje estão em `frontend/`. Contagens, versões e expressões
como “pendente” descrevem aquele momento, não uma garantia permanente.

Use a documentação viva para orientação atual:
[produto](../product/overview.md), [arquitetura](../architecture/overview.md),
[setup](../development/setup.md), [testes](../development/testing.md) e
[convenções](../development/conventions.md).

[Voltar ao README do projeto](../../README.md).
