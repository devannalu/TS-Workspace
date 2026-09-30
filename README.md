# TS Workspace

Workspace interno da Tech Sisters. Monorepo com frontend Next.js, backend
Java/Spring Boot e bancos MySQL separados durante a migração gradual.

## Estado desta etapa

A Fase 1.1–1.3 foi recuperada, validada e publicada no checkpoint
[`fabb67294509f89005c9bae0d7713b077f314691`](https://github.com/devannalu/TS-Workspace/commit/fabb67294509f89005c9bae0d7713b077f314691).
O frontend existente foi movido intacto para `frontend/`: login, sessão,
logout, RBAC, equipes, convites e usuárias continuam usando Prisma/Better Auth.

O Java oferece somente a fundação: Spring Web/Security/Data JPA/Validation,
MySQL isolado, Flyway, validação Hibernate e `GET /api/v1/health`.
A página técnica `/infra` usa `frontend/src/lib/api/client.ts` para consultar
esse endpoint diretamente do navegador. Autenticação e domínio Java ainda
não foram implementados. Nenhum dado real foi migrado.

## Estrutura

```text
frontend/
  src/app/                 # páginas existentes + /infra
  src/components/          # UI existente preservada
  src/lib/api/             # cliente central para o Java
  src/lib/auth/            # Better Auth e autorização existentes
  prisma/                  # schema, migrations e seed existentes
  scripts/                 # bootstrap e runners existentes
  tests/                   # suítes existentes
  package.json
  package-lock.json
  .env.example
backend/
  .mvn/wrapper/            # distribuição Maven com checksum
  mvnw / mvnw.cmd
  pom.xml
  run-dev.ps1
  src/main/java/com/devannalu/tsworkspace/
  src/main/resources/application.yml
  src/main/resources/db/migration/V1__foundation_marker.sql
  src/test/java/com/devannalu/tsworkspace/FoundationTest.java
docker/mysql/init.sql       # suporte ao banco Prisma existente
docs/
compose.yaml
.env.example
.gitignore
README.md
```

Não há diretório `legacy/` permanente. O histórico Git preserva o baseline;
a única implementação ativa anterior está dentro de `frontend/`.
Os documentos das fases anteriores são registros históricos, com caminhos
relativos à antiga raiz: hoje esses caminhos estão sob `frontend/`.

## Ambiente validado

| Recurso | Versão |
| --- | --- |
| Node / npm | 24.19.0 / 11.17.0 |
| Next / React / TypeScript / Tailwind | 16.3.7 / 19.3.0 / 6.0.3 / 4.3.3 |
| Prisma / Better Auth | 7.10.0 / 1.7.6 |
| Java | 17.0.12, Oracle JDK |
| Spring Boot | 3.5.16 |
| Maven Wrapper / Maven | 3.3.4 / 3.9.16 |
| Docker Client e Engine | 29.7.2, contexto desktop-linux |
| MySQL | 8.4.11 |
| Flyway / Testcontainers | 11.7.2 / 1.21.4, gerenciados pelo Spring Boot |

Java 17, Node 24 e Docker Engine Linux acessível são necessários.
Maven global não é necessário: todos os comandos usam o Wrapper oficial.
O Wrapper baixa o Maven e valida o SHA-256 fixado no repositório.

## Configuração local

Use os exemplos para preparar os arquivos locais, sem sobrescrever os
ambientes reais já existentes. Nunca use os placeholders como senhas.

- `.env` da raiz: credenciais do Compose legado e Java. O novo banco usa
  `JAVA_MYSQL_PASSWORD` e `JAVA_MYSQL_ROOT_PASSWORD`, diferentes das antigas.
- `frontend/.env`: URLs Prisma, `BETTER_AUTH_SECRET`, `BETTER_AUTH_URL` e,
  opcionalmente, `NEXT_PUBLIC_JAVA_API_URL=http://localhost:8080`.
- `frontend/.env.bootstrap.local`: dados privados do bootstrap antigo.

O `backend/run-dev.ps1` lê somente as variáveis Java necessárias do `.env`
da raiz, sem imprimir valores. Variáveis já definidas no processo têm
precedência. O backend não recebe a senha root do MySQL. Os padrões são
`jdbc:mysql://127.0.0.1:3309/ts_workspace_java`, usuário `ts_workspace_java`,
origem `http://localhost:3000` e bind `127.0.0.1:8080`.

`NEXT_PUBLIC_JAVA_API_URL` é público e incorporado no build do Next. Nunca
coloque credenciais em variáveis `NEXT_PUBLIC_*`. Alterar a URL exige novo
build. Alterar a origem do frontend exige também ajustar `FRONTEND_ORIGIN`
no backend e a configuração de origem do Better Auth.

## Bancos e portas

| Serviço | Host local | Banco | Volume |
| --- | --- | --- | --- |
| Prisma existente | 127.0.0.1:3307 | ts_workspace | ts-workspace-mysql-data |
| Testes Prisma | 127.0.0.1:3308 | ts_workspace_test | ts-workspace-mysql-test-data |
| Java | 127.0.0.1:3309 | ts_workspace_java | ts_workspace_mysql_java_data |
| Spring Boot | 127.0.0.1:8080 | — | — |
| Next.js | localhost:3000 | — | — |
| Runner HTTP Next | 127.0.0.1:3101 | banco de testes | — |

A porta 3306 pertence a outro projeto e foi preservada. O serviço `mysql-java`
usa container `ts-workspace-mysql-java` e rede `ts-workspace-java-network`.
O banco shadow Prisma permanece na instância legada. O Testcontainers usa
containers efêmeros e portas aleatórias; nunca usa os bancos de desenvolvimento.

## Iniciar neste ambiente (PowerShell)

Na raiz, para o MySQL Java:

```powershell
docker compose config --quiet
docker compose up -d mysql-java
```

Para os containers Prisma já existentes, se estiverem parados:

```powershell
docker start ts-workspace-mysql ts-workspace-mysql-test
```

Em um terminal na raiz, iniciar o backend:

```powershell
backend\mvnw.cmd --version
powershell -NoProfile -File backend/run-dev.ps1
```

Em outro terminal, entrar em `frontend/` e executar:

```powershell
npm install --cache ../.npm-cache
npm run db:generate
npm run build
npm run start -- --hostname 127.0.0.1
```

Acesse [workspace](http://localhost:3000) ou
[verificação Java](http://localhost:3000/infra). Ctrl+C encerra cada servidor.
As migrations Prisma do ambiente existente já estão aplicadas. Não gere
outra migration de mesmo propósito nem reinicialize esse banco. O script
`test:integration` aplica as migrations versionadas somente no banco de testes.

## Flyway e segurança da fundação

Na inicialização, Flyway aplica `V1__foundation_marker.sql` exclusivamente
no banco Java, criando uma tabela técnica com uma linha. Hibernate usa
`ddl-auto=validate`; não cria nem atualiza tabelas. `clean-disabled=true`.
Uma segunda execução valida a migration e não a reaplica.

`GET /api/v1/health` consulta a tabela via JPA e retorna apenas
`{"status":"UP"}` quando disponível. Falha de banco retorna 503 sem SQL,
credenciais, connection string ou stacktrace na resposta.

CORS permite somente `http://localhost:3000`, GET e credentials. Origem
externa é rejeitada. CSRF permanece habilitado e as demais rotas são
negadas por padrão. Não existe login, senha gerada ou cadastro Java.

## Testes

Dentro de `backend/`, com Docker acessível:

```powershell
.\mvnw.cmd -B test
.\mvnw.cmd -B package
```

Ambos executam os cinco testes reais com MySQL Testcontainers: contexto,
Flyway idempotente e validação JPA, health, CORS permitido/rejeitado,
CSRF e negação das demais rotas. O package produz
`backend/target/ts-workspace-backend-0.1.0.jar`, ignorado pelo Git.
Nenhum comando usa `-DskipTests`.

Dentro de `frontend/`, com os bancos Prisma existentes acessíveis:

```powershell
npm run typecheck
npm run lint
npm test
npm run build
npm run test:integration
npm run test:workspace
npm run auth:schema:check
```

Foram aprovados 34 testes unitários e 16 de integração, além do runner
HTTP do Next real. O runner usa a porta 3101 e remove somente seus próprios
dados temporários no banco de testes. Consulte a cobertura e as limitações
em [Fundação Java](docs/fundacao-java.md).

## Histórico e cuidados

- [Fase 1.1](docs/fase-1.1.md)
- [Fase 1.2](docs/fase-1.2.md)
- [Fase 1.3](docs/fase-1.3.md)
- [Recuperação e fundação Java](docs/fundacao-java.md)
- [Repositório oficial](https://github.com/devannalu/TS-Workspace), branch `main`.

Arquivos reais de ambiente, logs, caches e artefatos gerados são ignorados.
Migrations Flyway e Prisma, Maven Wrapper e lockfile são versionados.
Não executar reset, DROP, remoção de volumes, prune ou alterações em
containers de outros projetos. O frontend antigo só deve ser retirado por
módulo depois que sua substituição Java correspondente estiver validada.
