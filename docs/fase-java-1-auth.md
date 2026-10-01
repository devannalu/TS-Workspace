# Fase Java 1 — autenticação e sessões

Esta fase adiciona autenticação Java paralela ao Better Auth existente. O
frontend oficial, `/login`, middleware, RBAC, equipes, convites e usuárias
continuam no caminho Prisma/Better Auth. Nenhuma migration antiga foi editada.

## Modelo e banco

Flyway V2 cria `app_user` e `app_profile`. User usa UUID textual, nome,
email único normalizado, hash de senha e timestamps. Profile usa `user_id`,
`job_title` opcional, `status` (`ACTIVE` ou `INACTIVE`) e timestamps. Não há
Role, booleano administrativo ou campo `isSuperAdmin`.

Flyway V3 cria `SPRING_SESSION` e `SPRING_SESSION_ATTRIBUTES`. Hibernate fica
em `ddl-auto=validate`; o banco Java continua separado do banco Prisma.

## Segurança e fluxo

`BCryptPasswordEncoder` com força 12 é o único mecanismo de hash. O
`UserDetailsService` carrega User/Profile sem acoplar o controller a JPA.
Login normaliza o email com `trim().toLowerCase(Locale.ROOT)` e retorna uma
mensagem genérica para senha inválida ou email inexistente.

Spring Session JDBC persiste a autenticação e permite revogação. O cookie
`TS_SESSION` é HttpOnly, SameSite=Lax, Path `/` e Secure depende de
`SESSION_COOKIE_SECURE` (false apenas no HTTP local). A proteção padrão contra
session fixation troca o identificador ao autenticar.

CSRF permanece habilitado. `GET /api/v1/auth/csrf` expõe o token necessário
para o cliente técnico; `XSRF-TOKEN` não é credencial de autenticação e não é
armazenado em localStorage. POST login/logout exigem `X-XSRF-TOKEN`.

CORS aceita somente `FRONTEND_ORIGIN` (por padrão `http://localhost:3000`),
com credentials e os headers necessários. `/auth/me` retorna somente id,
nome, email, jobTitle e status. O filtro consulta o Profile no banco em cada
request autenticada; ao detectar INACTIVE, invalida a sessão e responde 403.

## Endpoints

| Método | Endpoint | Regra |
| --- | --- | --- |
| GET | `/api/v1/auth/csrf` | público; entrega token CSRF |
| POST | `/api/v1/auth/login` | público + CSRF; cria sessão |
| POST | `/api/v1/auth/logout` | CSRF; invalida sessão e cookie |
| GET | `/api/v1/auth/me` | sessão ativa obrigatória |

Problemas de autenticação retornam 401 genérico; CSRF, origem indevida e
usuária inativa retornam 403 sem stack trace ou detalhes internos.

## Bootstrap

`backend/bootstrap.ps1` é deliberadamente explícito. Ele carrega apenas
`BOOTSTRAP_NAME`, `BOOTSTRAP_EMAIL` e `BOOTSTRAP_PASSWORD` de arquivos locais
ignorados, habilita o runner, cria User + Profile ACTIVE na mesma transação e
encerra. A senha nunca é registrada. Repetição com a identidade completa é
idempotente; User sem Profile produz erro de estado parcial.

O bootstrap não associa Role nesta fase. A identidade fica pronta para a
associação correta ao SUPER_ADMIN quando o RBAC Java for implementado.

## Cliente técnico e validação

`frontend/src/lib/api/auth.ts` fornece `getCsrf`, `loginJava`, `logoutJava` e
`getCurrentJavaUser`. A página `/infra` apenas mostra que o CSRF está
disponível; ela não substitui o login oficial.

`AuthIntegrationTest` usa MySQL 8.4.11 real via Testcontainers e valida
migrations, hashing, login, senha inválida, email inexistente, sessão JDBC,
`/me`, logout, CSRF válido/ausente, CORS, inactive, invalidação, bootstrap,
idempotência e estado parcial. O teste HTTP real validou CSRF 200, login sem
CSRF 403, login 200, `/me` 200, logout 204 e `/me` pós-logout 401. Nenhum
cookie, senha ou hash é incluído no relatório.

O pacote Java, typecheck, lint, build, unitários e integrações existentes do
frontend também passaram. Rate limiting distribuído continua hardening futuro;
Redis e contadores em memória não foram adicionados.
