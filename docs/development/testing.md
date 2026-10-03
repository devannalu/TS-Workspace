# Testes e verificações

Testes devem validar comportamento observável, incluindo falhas e limites de
autorização. Os resultados de uma execução específica ficam no
[histórico](../history/README.md), não neste guia.

## Java

Na raiz, com Java 17 e Docker Engine acessível:

```powershell
backend\mvnw.cmd -f backend/pom.xml -B test
backend\mvnw.cmd -f backend/pom.xml -B package
```

Alternativamente, entre em `backend/` e use `.\mvnw.cmd -B test` ou
`.\mvnw.cmd -B package`. Package executa testes e produz o JAR em
`backend/target/`. Não usar skip de testes como evidência de validação.

A suíte cobre inicialização, Flyway, validação JPA, health, autenticação,
sessões persistidas, logout, CSRF, CORS, inactive e bootstrap. Testes de
autenticação usam credenciais exclusivamente de teste.

A suíte de [RBAC](../architecture/rbac.md) acrescenta unitários do resolver e
guard, compatibilidade da matriz, integração de overrides/Method Security e
upgrade de identidade existente. O teste HTTP inicia Spring Boot em porta
aleatória com MySQL efêmero e cookies reais para validar 200/401/403, CSRF,
roles e logout, sem modificar o acesso da identidade de desenvolvimento.

A suíte de [Equipes](../architecture/teams.md) cobre políticas de hierarquia,
seed, memberships many-to-many, arquivamento, última Super Admin, compatibility
com o baseline Prisma, RBAC e constraints. Testes concorrentes verificam
duplicação de integrante, movimentos que formariam ciclo e remoção simultânea
das administradoras. O upgrade valida a membership de identidade preexistente.

### Testcontainers

A suíte de [Users/Invites](../architecture/users-invites.md) cobre token/hash,
estados, validação, proteção administrativa, provisionamento atômico,
concorrência, rollback, auditoria, paginação, filtros, autorização e revogação
de todas as sessões. A falha intermediária usa uma constraint temporária
somente no banco Testcontainers, sem exigir privilégios globais MySQL.

Os testes integrados Java criam MySQL real efêmero e recebem URL, usuário,
senha e porta do container. Não utilizam o banco Java de desenvolvimento.
Falha no setup do Docker bloqueia essa validação: não substituir silenciosamente
o MySQL por mocks ou pelo banco com dados reais.

Testes unitários independem de Docker; seleção por classe pode ser útil no
desenvolvimento, mas não substitui a suíte completa antes de publicar mudanças
de autenticação, autorização ou schema.

## Frontend

Dentro de `frontend/`:

```powershell
npm run typecheck
npm run lint
npm test
npm run build
```

Typecheck gera tipos de rotas e executa TypeScript; lint exige zero warnings.
Os unitários verificam políticas, validações e hierarquia. Build valida a
compilação de produção.

## Integração dos módulos atuais

Prepare o serviço de testes na raiz:

```powershell
docker compose --profile test up -d mysql-test
```

Configure `TEST_DATABASE_URL` em `frontend/.env` para o banco separado
`ts_workspace_test`, na porta 3308. Dentro de `frontend/`:

```powershell
npm run test:integration
npm run build
npm run test:workspace
npm run auth:schema:check
```

- `test:integration` valida o destino, aplica migrations versionadas ao banco
  de teste e executa integração de identidade, seed, RBAC, equipes e convites.
- `test:workspace` requer build e schema de teste preparados. Inicia um Next
  real em `127.0.0.1:3101`, exercita acesso protegido, login, sessão, dashboard,
  inativação e logout, e remove seus dados temporários.
- `auth:schema:check` verifica compatibilidade da configuração Better Auth com
  o schema, como apoio à integração.

Os runners recusam destinos incompatíveis com os bancos de teste. Não contorne
essa proteção. Não use uma conta real externa como identidade automatizada.

## HTTP e navegador

Para mudanças na API Java, valide com o servidor real: health, obtenção de CSRF,
login, cookie HttpOnly, me, endpoint protegido e logout. O caso sem sessão deve
ser distinguido do caso autenticado sem permissão. Não registrar valores de
cookies, tokens, senhas ou hashes nas evidências.

Na interface, verifique o fluxo afetado, erros de conexão, navegação por teclado
e adaptação às larguras relevantes. `/infra` comprova integração técnica com
health/CSRF; não comprova sozinho toda a autenticação ou autorização.

## Documentação

Mudanças apenas documentais pedem conferência de links relativos e âncoras,
revisão de conteúdo, diff e segredos. Quando houver trabalho prévio não
relacionado, compare o conteúdo antes/depois e selecione apenas os arquivos
documentais para o commit. Não é necessário repetir suítes da aplicação se
nenhum código, configuração, migration ou dependência mudou.
