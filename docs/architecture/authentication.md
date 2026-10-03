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
| GET | /api/v1/auth/me | Retorna id, name, email, jobTitle, status, role key/name e permission keys efetivas |
| POST | /api/v1/auth/logout | Exige CSRF, encerra sessão e retorna 204 |

As respostas não expõem hash, identificador de sessão ou dados internos.
Não autenticada recebe 401; requisição sem CSRF válido recebe 403.
Login e `/auth/me` incluem role e keys efetivas conforme o [RBAC](rbac.md).

## Perfil inativo e bootstrap

Perfil INACTIVE não pode autenticar. Em requisição com sessão existente,
o filtro consulta o estado atual do Perfil, invalida a sessão e bloqueia o
acesso quando o perfil está ausente ou inativo. Permissões não contornam isso.

A inativação administrativa Java revoga imediatamente todas as sessões pelo
principal indexado (email da Usuario) na mesma transação da mudança de Perfil.
Reativação mantém Usuario/Perfil e exige novo login, sem restaurar sessões antigas.
PerfilAcesso/permissões continuam sendo consultadas no banco em requisições autenticadas.

Novas usuárias Java entram por [convite](users-invites.md), sem endpoint de signup
ou criação administrativa direta. Aceite cria Usuario, Perfil ACTIVE, role e
memberships atomicamente, sem criar sessão. Usa BCrypt força 12; a senha exige
12–128 caracteres e no máximo 72 bytes UTF-8, respeitando o limite do encoder,
com confirmação igual. Recuperação de senha e SMTP não estão implementados.

Validação/aceite recebem o token secreto no corpo de POST e exigem CSRF mesmo
sem autenticação. O cliente obtém o cookie/header em `/auth/csrf`; CORS permanece
restrito à origem configurada. Não existe exceção global de CSRF para convites.

A primeira identidade é criada por bootstrap administrativo explícito com
variáveis locais; não existe promoção automática da primeira pessoa cadastrada.
Usuario e Perfil são criados em transação. Repetições completas são idempotentes;
estados parciais exigem revisão. O bootstrap localiza SUPER_ADMIN pela key e
associa essa role ao Perfil, sem IDs fixos.

## Frontend oficial

O login usa csrf → login → me da API Java. Logout invalida a sessão no
servidor; me retorna 401 depois dele. O frontend não mantém outra autoridade
de sessão. Convites são a única entrada de novas integrantes.

Gestão completa de sessões e rate limiting distribuído são evoluções previstas,
não recursos já oferecidos pela API Java.
Veja [RBAC](rbac.md) e [setup](../development/setup.md).
