# Recuperação e fundação Java — 30/09/2026

## Baseline e preservação

O checkpoint `fabb67294509f89005c9bae0d7713b077f314691` foi criado e enviado
para `origin/main` antes da reorganização. Contém somente a implementação
Next.js + Prisma + Better Auth das Fases 1.1–1.3. A recuperação registrou
inventário e hashes locais; 100 arquivos restaurados foram conferidos.
O package.json foi alinhado ao lockfile e os overrides já autorizados foram
preservados, incluindo deepmerge-ts 8.0.2 restrito a @prisma/config.

O frontend completo, configurações, Prisma, scripts e testes foram movidos
para `frontend/`. O histórico guarda os caminhos anteriores. Rascunhos Java
anteriores ficaram fora da árvore versionada em `.verification/recovery/`;
eles não são a implementação oficial e nenhuma migration de domínio desses
rascunhos foi aplicada. Não existe cópia ativa permanente em `legacy/`.
Os 77 arquivos versionados movidos (código, testes, scripts, manifest,
lockfile e configurações) foram comparados com o checkpoint Git: conteúdo
idêntico, normalizando apenas a representação CRLF/LF.

O banco Prisma, sua Super Admin, memberships e credencial foram preservados.
Não houve migração de dados reais, reset ou remoção de volumes. O frontend
mantém todas as dependências antigas enquanto a substituição ocorre por módulo.

## Evidências executadas

| Verificação | Baseline | Após mover para frontend/ |
| --- | --- | --- |
| npm install | passou; audit sem vulnerabilidades | passou; audit sem vulnerabilidades |
| typecheck / lint / build | passaram | passaram |
| unitários | 34 aprovados | 34 aprovados |
| integração MySQL | 16 aprovados | 16 aprovados |
| runner Next HTTP real | passou | passou |
| Better Auth schema check | passou | passou |

A suíte de integração cobre seed idempotente, bootstrap atômico e repetido,
rejeição de conta parcial/segunda Super Admin, rollback, signup bloqueado,
login inválido/válido, cookie HttpOnly, sessão, logout, inativação, grants e
overrides RBAC, memberships, convites, cancelamento, expiração e aceite
concorrente single-use. Políticas e hierarquia também têm testes unitários.
O runner Next comprova rota protegida, signup bloqueado, login, dashboard,
inativação, revogação e logout pelo servidor real. Isso não representa um
novo teste manual exaustivo de todas as telas de gestão.

Java: Maven Wrapper 3.3.4, Maven 3.9.16 e Java 17.0.12 verificados.
`mvnw.cmd -B test` e `mvnw.cmd -B package` passaram com cinco testes cada,
sem skip, usando MySQL 8.4.11 efêmero por Testcontainers 1.21.4. O pacote
executável foi gerado. O contexto inicia com Flyway e Hibernate validate;
o teste de nova chamada ao Flyway confirma zero migrations reaplicadas.

MySQL de desenvolvimento Java está saudável na porta 3309. A migration
técnica V1 foi aplicada pelo Spring Boot, sem tabelas User/Role/Team/Invite.
HTTP real no backend confirmou health 200 com apenas status UP, origem
localhost:3000 aceita com credentials, preflight 200 e origem externa 403.
O Next de produção iniciou na porta 3000 após o build.

**Verificação manual aprovada:** `http://localhost:3000/infra` carregou,
confirmou no navegador que o serviço Java e o MySQL estavam disponíveis, e o
console não apresentou erros relevantes de CORS, fetch ou conexão.

## Recursos Docker

Criados exclusivamente para o Java:

- Container `ts-workspace-mysql-java`.
- Volume `ts_workspace_mysql_java_data`.
- Rede `ts-workspace-java-network`.
- Banco `ts_workspace_java`, acessível no host apenas em 127.0.0.1:3309.

Os containers Prisma existentes foram iniciados sem recriação ou alteração
dos volumes: portas 3307 e 3308. A porta 3306 estava ocupada por outro projeto.
Nenhum container desse outro projeto foi parado ou modificado. Testcontainers
cria e limpa apenas os containers efêmeros da própria suíte.

## Erros e observações reais

- A inconsistência inicial entre manifest e lockfile foi corrigida na
  recuperação, sem upgrade da stack frontend.
- O primeiro teste CORS detectou falta de ligação entre a configuração de
  origem e o SecurityFilterChain. A ligação explícita corrigiu a falha;
  os cinco testes e a verificação HTTP real passaram depois.
- Flyway 11.7.2, gerenciado pelo Boot, emite aviso sobre sua matriz de
  versões testadas para MySQL 8.4. Migração, validação e idempotência foram
  executadas com sucesso em MySQL 8.4.11. O aviso não foi suprimido.
- npm avisou sobre quatro scripts de instalação sem aprovação registrada.
  Não houve aprovação indiscriminada, --force ou --legacy-peer-deps.
  Instalação, geração Prisma, testes e build foram executados com sucesso.

## Limites e fontes técnicas

Este checkpoint não implementa Auth/RBAC/Users/Teams/Invites Java nem Tasks.
O health é técnico; não demonstra migração de autenticação. CORS permite
uma origem explícita, CSRF permanece ativo e demais rotas são negadas.
Credenciais Java são locais, separadas e ignoradas. Nenhum segredo deve
ser incluído em logs, exemplos, Git ou relatório.

Versões e configuração consultadas nas fontes oficiais:

- [Requisitos Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html).
- [Inicialização de banco Spring Boot](https://docs.spring.io/spring-boot/3.5/how-to/data-initialization.html).
- [Apache Maven Wrapper](https://maven.apache.org/tools/wrapper/).
- [Testcontainers MySQL](https://java.testcontainers.org/modules/databases/mysql/).
- Guias locais da versão instalada do Next em `frontend/node_modules/next/dist/docs/`.
