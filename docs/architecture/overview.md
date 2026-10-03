# Arquitetura do sistema

## Arquitetura oficial

```mermaid
flowchart LR
    UI["Next.js / React"] -->|API REST · JSON · cookie de sessão| API["Spring Boot"]
    API -->|Spring Data JPA / JDBC| DB[("MySQL")]
    F["Flyway"] -->|schema versionado| DB
```

O frontend apresenta dados e fluxos de interação. A API centraliza regras de
negócio, autenticação, autorização e transações. O MySQL persiste identidade,
sessões e dados de domínio. A organização é um monorepo com módulos por domínio.

## Responsabilidades

| Componente | Responsabilidade |
| --- | --- |
| Next.js | Rotas, renderização, formulários, estados de interface e consumo da API |
| Spring Boot | Contratos REST, validação, segurança e serviços de aplicação |
| JPA / Hibernate | Mapeamento e acesso aos dados de domínio |
| Spring Session JDBC | Persistência e invalidação de sessões Java |
| Flyway | Evolução versionada do schema Java |
| MySQL | Integridade relacional, persistência e transações |

## Funcionamento oficial

Login, sessão, dashboard, equipes, usuárias e convites usam a API Spring Boot.
Leituras em Server Components encaminham somente o cookie TS_SESSION ao Java.
Mutações partem do navegador, com credentials include e CSRF centralizado.
Spring Security e VerificadorPermissao permanecem a autoridade dos dados.
O frontend não possui banco próprio nem regras de autorização duplicadas.

## Contratos

- A autenticação identifica a usuária; o RBAC decide o que ela pode fazer.
- O backend deriva a identidade da sessão, não de um userId escolhido pelo cliente.
- DTOs limitam o que a API expõe; entidades de persistência não são o contrato público.
- Mudanças de banco são migrations versionadas, sem geração automática de schema.
- UI condicional ajuda a navegação, mas não substitui autorização server-side.

Detalhes: [backend](backend.md), [frontend](frontend.md), [banco](database.md),
[autenticação](authentication.md), [RBAC](rbac.md) e [setup](../development/setup.md).
