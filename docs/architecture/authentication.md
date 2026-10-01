# Autenticação

## Identidade e sessão

A API Java utiliza autenticação email/senha baseada em sessão.
Spring Security valida credenciais por `UserDetailsService` e
`DaoAuthenticationProvider`; `BCryptPasswordEncoder` com força 12 gera hashes.
O email é normalizado com trim e lowercase usando Locale.ROOT.
Credenciais inválidas recebem mensagem genérica.

Spring Session JDBC persiste sessões em `SPRING_SESSION` e
`SPRING_SESSION_ATTRIBUTES`. O tempo de inatividade configurado é de oito
horas. Login troca o identificador de sessão; logout invalida a sessão e
inutiliza o cookie.

## Cookie e CSRF

O cookie de autenticação é `TS_SESSION`, com HttpOnly, SameSite=Lax e Path=/.
`SESSION_COOKIE_SECURE` deve ser true em produção com HTTPS; false é reservado
ao desenvolvimento HTTP local. O valor de configuração precisa chegar ao
processo Java, conforme o [setup](../development/setup.md).

CSRF permanece habilitado. `GET /api/v1/auth/csrf` fornece o token e o cookie
`XSRF-TOKEN`; login/logout enviam `X-XSRF-TOKEN`. O token CSRF não é uma
credencial de autenticação. A aplicação não utiliza JWT nem guarda a sessão
em localStorage.

## CORS

A API permite a origem configurada em `FRONTEND_ORIGIN`, por padrão
`http://localhost:3000`, com credentials. Métodos e headers são explícitos.
Não se usa origem curinga com cookies. Origem inclui protocolo, host e porta;
`localhost` e `127.0.0.1` não são intercambiáveis na configuração de CORS.

## Endpoints

| Método | Caminho | Contrato |
| --- | --- | --- |
| GET | /api/v1/auth/csrf | Obtém token CSRF |
| POST | /api/v1/auth/login | Recebe email/senha, exige CSRF e cria sessão |
| GET | /api/v1/auth/me | Retorna id, name, email, jobTitle e status da usuária autenticada |
| POST | /api/v1/auth/logout | Exige CSRF, encerra sessão e retorna 204 |

As respostas não expõem hash, identificador de sessão ou dados internos.
Não autenticada recebe 401; requisição sem CSRF válido recebe 403.
Role e permission keys em `/auth/me` fazem parte da [migração RBAC](rbac.md).

## Perfil inativo e bootstrap

Profile INACTIVE não pode autenticar. Em requisição com sessão existente,
o filtro consulta o estado atual do Profile, invalida a sessão e bloqueia o
acesso quando o perfil está ausente ou inativo. Permissões não contornam isso.

A primeira identidade é criada por bootstrap administrativo explícito com
variáveis locais; não existe promoção automática da primeira pessoa cadastrada.
User e Profile são criados em transação. Repetições completas são idempotentes;
estados parciais exigem revisão. A associação à role no Java está em migração.

## Coexistência dos serviços

O login oficial do Workspace continua usando Better Auth e seu banco Prisma.
Convites são o fluxo de entrada de novas usuárias, com signup público bloqueado.
A autenticação Java é independente; sua sessão não concede acesso automático
aos módulos atendidos pelo serviço anterior.

Gestão completa de sessões e rate limiting distribuído são evoluções previstas,
não recursos já oferecidos pela API Java.
Veja [RBAC](rbac.md) e [setup](../development/setup.md).
