# Banco de dados

## MySQL e separação de dados

O projeto utiliza MySQL. O Compose fixa a imagem utilizada pelos serviços
locais e restringe suas portas ao loopback.

| Uso | Banco | Porta local |
| --- | --- | --- |
| Módulos atuais via Prisma | ts_workspace | 3307 |
| Integração Prisma | ts_workspace_test | 3308 |
| API Java | ts_workspace_java | 3309 |

O banco shadow Prisma é separado do banco da aplicação, na instância local
correspondente. O Java não acessa tabelas Prisma; não existe sincronização
automática entre identidades ou sessões dos dois serviços.

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
unicidade. Equipes e convites continuam no schema Prisma.

## Migrations

Flyway controla o schema Java em `backend/src/main/resources/db/migration/`;
migrations Java, quando usadas, são registradas explicitamente. Hibernate
mantém `ddl-auto=validate`, e Flyway possui `clean-disabled=true`.

Prisma mantém seu schema e migrations em `frontend/prisma/`. Não editar uma
migration já aplicada, recriar uma migration equivalente ou usar reset para
contornar erros. Mudanças posteriores recebem novas versões.

Ao adicionar coluna obrigatória a tabelas com dados, separar criação,
provisionamento controlado e imposição de integridade. DDL MySQL pode realizar
commit implícito; não tratar toda alteração estrutural como rollback automático.
Inspecionar falhas antes de qualquer reparo de histórico.

## Transações

Criação de identidade/perfil, aceite de convite e mudanças organizacionais
devem preservar atomicidade e suas invariantes. No backend atual, operações
sensíveis de hierarquia e gestão usam transações e validações server-side.
No Java, serviços usam transações Spring para operações relacionadas.

## Testes e dados locais

Testcontainers cria MySQL efêmero com portas dinâmicas para integração Java.
Os testes Prisma usam `TEST_DATABASE_URL`, diferente da URL da aplicação;
os runners validam o destino antes de executar. O banco de desenvolvimento
não é um substituto para o banco de testes.

Volumes locais persistem dados. Não usar remoção de volumes, reset, clean ou
limpeza de outros projetos como parte de um setup rotineiro.
Veja [setup](../development/setup.md) e [testes](../development/testing.md).
