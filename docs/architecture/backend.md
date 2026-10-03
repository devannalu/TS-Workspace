# Backend

## Stack e organização

A API utiliza Java 17 e Spring Boot 3.5, com dependências e plugins declarados
em `backend/pom.xml`. O Maven Wrapper fixa a distribuição do Maven e permite
compilar sem instalação global. Testcontainers fornece MySQL efêmero para
testes integrados.

O pacote base é `com.devannalu.tsworkspace`. A organização acompanha o domínio:

- `autenticacao`: login, identidade inicial e revogação de sessões;
- `seguranca`: cadeia Spring Security e bloqueio de usuárias inativas;
- `rbac`: catálogo, permissões, perfis de acesso e autorização;
- `equipes`: hierarquia, integrantes e operações transacionais;
- `usuarios`: identidade, perfil e gestão paginada;
- `convites`: criação, validação e provisionamento atômico;
- `auditoria`: persistência de eventos mínimos;
- `compartilhado`: bloqueio de Fundadoras e erros de domínio;
- `infraestrutura`: verificação de saúde e marcador do schema.

Controllers recebem DTOs e delegam. Serviços coordenam regras e transações.
Repositories possuem as consultas JPA/JDBC; políticas são funções de domínio
sem acesso a banco. `GestaoUsuariosRepository` concentra as consultas da gestão,
enquanto `UsuarioRepository` mantém a identidade usada na autenticação.
`AuditoriaRepository` apenas persiste dentro da transação existente, sem serviço
intermediário que só encaminharia argumentos. DTOs pequenos permanecem junto
aos seus casos de uso; não há hierarquia genérica de mappers ou repositories.

`BloqueioOrganizacao` protege invariantes compartilhadas de Fundadoras.
`ProblemaDominio` e `TratamentoErrosApi` centralizam os erros controlados.
`RevogacaoSessaoService` remove sessões da identidade pelo email indexado.

`auth.AppUserPrincipal` e `auth.ProfileStatus` permanecem nos nomes originais
para preservar sessões Java serializadas. `EmailNormalizer.normalize` e
`RbacSeed.seed` permanecem por serem referenciados pela migration Java V5,
que não pode ser reescrita após aplicação. Essa compatibilidade não cria duas
implementações de autenticação.

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
[equipes](teams.md), [usuárias e convites](users-invites.md), [banco](database.md)
e [testes](../development/testing.md).
