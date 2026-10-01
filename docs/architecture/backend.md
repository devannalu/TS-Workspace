# Backend

## Stack e organização

A API utiliza Java 17 e Spring Boot 3.5, com dependências e plugins declarados
em `backend/pom.xml`. O Maven Wrapper fixa a distribuição do Maven e permite
compilar sem instalação global. Testcontainers fornece MySQL efêmero para
testes integrados.

O pacote base é `com.devannalu.tsworkspace`. `auth` reúne identidade e
autenticação, `common` contém configuração de segurança e tratamento de erros,
e `foundation` mantém a verificação de saúde. `rbac` contém catálogo, seed,
resolução de permissões e proteção por Method Security.
`teams` reúne entidades Team/TeamMember, políticas de hierarquia e memberships,
seed, serviço transacional e controller REST. As consultas desse módulo usam
JdbcTemplate, com projeções DTO e contagem agregada para evitar N+1; Hibernate
valida os mapeamentos JPA sem criar o schema.
Novos pacotes devem corresponder a funcionalidades reais, sem pastas vazias
criadas antecipadamente.

## Componentes

| Tecnologia | Papel |
| --- | --- |
| Spring Web | Controllers REST e serialização dos DTOs |
| Spring Security | Autenticação, contexto de segurança, CSRF, CORS e proteção de rotas |
| Spring Session JDBC | Sessões persistidas no MySQL |
| Spring Data JPA | Repositories de acesso ao domínio |
| Hibernate | Mapeamento relacional e validação do schema |
| Flyway | Aplicação e validação de migrations |
| Bean Validation | Restrições dos dados recebidos pelos endpoints |
| Maven Wrapper | Build, execução de testes e empacotamento |
| Testcontainers | Infraestrutura real e isolada para integração |

## Fluxo de requisição

A cadeia de segurança valida a requisição e recupera a sessão. O controller
recebe um DTO, aplica validação e chama os serviços necessários. Serviços
coordenam regras e transações; repositories acessam a persistência. A resposta
usa DTOs e erros controlados, sem SQL, credenciais ou stack traces.

Spring Security integra email/senha por `UserDetailsService` e
`DaoAuthenticationProvider`. O principal contém a identidade autenticada;
dados de autorização devem refletir o estado atual do banco.

## Persistência e operação

Hibernate executa com `ddl-auto=validate`: a aplicação não cria nem atualiza
tabelas por conta própria. Flyway aplica o schema antes da inicialização do JPA.
Operações que alteram dados relacionados devem ser transacionais.

`GET /api/v1/health` lê o banco e retorna apenas status; falha de acesso ao
banco produz resposta controlada. O bind local padrão é `127.0.0.1:8080`.

Consulte [autenticação](authentication.md), [RBAC](rbac.md),
[equipes](teams.md), [banco](database.md) e [testes](../development/testing.md).
