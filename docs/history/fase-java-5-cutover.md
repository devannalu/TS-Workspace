# Fase Java 5 — Cutover do frontend

Base: `a1874f6d89549a792235802829fd3609f0421a69`. Data: 2026-10-02.

## Inventário e decisão

O banco Prisma foi acessado somente para leitura, sem copiar credenciais.

| Recurso | Legado | Comparação com Java |
| --- | --- | --- |
| User/Profile | 1/1 | Mesma Super Admin, nome/email/cargo/status equivalentes |
| Role | 4 | Keys e nomes equivalentes |
| Team | 5 ativas | Keys, nomes, descrições e hierarquia equivalentes |
| TeamMember | 1 | Mesmo vínculo por email e team key |
| Permission/RolePermission | 14/32 | Grants equivalentes por keys |
| UserPermission | 0 | Nenhum override a migrar |
| Invite/InviteTeam | 0/0 | Nenhum convite em qualquer estado |
| AuditLog | 3 | Eventos históricos preservados no banco original |
| Account/Session/Verification | 1/1/0 | Sessão anterior não é reutilizada |

Não havia usuária real ausente no Java, nem dados organizacionais divergentes.
Não houve migração de dados, cópia de hashes, redefinição de senhas ou envio de
convites reais. O Java já tinha credencial BCrypt validada; não se presumiu
compatibilidade com o scrypt do Better Auth. Os eventos históricos de bootstrap
e remoção/adição de membership continuam intactos no banco anterior.

## Implementação

Login/logout, sessão e dashboard utilizam Spring Boot. Leituras SSR encaminham
somente TS_SESSION; as escritas vão diretamente do browser ao Java, com CSRF.
Equipes, usuárias e convites mantêm os componentes visuais. Editar cargo/role/
equipes e ativar/inativar são ações separadas, com suas permissões próprias.
Aceite público exige confirmação de senha e retorna ao login, sem sessão automática.

http.ts centraliza URL, cookies, CSRF em memória com deduplicação/rotação,
Problem Details e erros por status. Não reenvia mutações. SessionBoundary trata
carregamento, expiração, acesso negado e indisponibilidade. Spring Security
permanece a única autoridade de autenticação e autorização dos dados.

## Remoções justificadas

Páginas e fluxos Java foram verificados por HTTP antes da retirada dos serviços.
Removidos `frontend/prisma/`, configs Prisma, serviços `src/lib/db/`, `teams/`,
`users/`, `invites/`, helpers antigos de auth (mantido session.ts reescrito),
permissions/data.ts e policy.ts, rota api/auth/[...all] e as três actions antigas.
Seus consumidores restantes eram scripts e testes exclusivos também retirados.
Schema, migrations e evidências anteriores permanecem no histórico Git.

Retirados scripts de schema/check/bootstrap/testes antigos e testes do backend
anterior. A regressão atual reside no Java e em dez testes do cliente oficial.
O client gerado local antigo foi retirado, sem referência ativa.

Dependências sem consumidores removidas: prisma, @prisma/client,
@prisma/adapter-mariadb, better-auth, @better-auth/prisma-adapter, auth,
bun-types, tsx, dotenv e zod. Overrides exclusivos removidos; npm install
regenerou o lockfile. Tipos Bun exclusivos removidos do tsconfig. Arquivos
AGENTS/CLAUDE são gerados pelo Next instalado e preservados conforme suas instruções.

## Banco preservado e rollback

Somente ts-workspace-mysql e ts-workspace-mysql-test foram parados. Nenhum
container, banco ou volume foi removido. mysql-java permanece healthy.
Os perfis Compose legacy/legacy-test são opt-in; o serviço oficial não os exige.

O código anterior pode ser recuperado num checkout separado do commit base,
sem reset ou force no trabalho atual. Ambientes anteriores desta máquina foram
preservados intencionalmente nos arquivos ignorados `.verification/legacy-frontend.env`
e `.verification/legacy-compose.env`; não são configuração runtime do frontend.
O frontend atual contém somente NEXT_PUBLIC_JAVA_API_URL, sem segredos.

Para inspeção/rollback manual local, sem remover dados:

```powershell
docker compose --env-file .verification/legacy-compose.env --profile legacy up -d mysql
```

Esse comando não faz parte do setup oficial. Sessões não são intercambiáveis;
hashes não devem ser copiados sem prova de compatibilidade. Remoção física do
banco anterior exige decisão futura explícita.

## Verificações

- Java test/package: 146 testes cada, zero failures/errors/skips; BUILD SUCCESS.
- Frontend: typecheck, lint, dez testes do cliente e build de produção aprovados.
- Com bancos anteriores parados: HTTP/SSR das páginas, sessão, CSRF, CORS, 401/403,
  convites de uso único, revogação de sessões, inativação/reativação e Teams CRUD.
- Navegador real: login/dashboard, equipe temporária criada/editada, integrante
  removida/adicionada, convite aceito sem sessão, login Suporte com ações ocultas,
  logout, inativação/reativação, edição de cargo, cancelado rejeitado e arquivamento.
- Dados temporários foram removidos por UUID/email específicos. A comparação
  antes/depois confirmou os dados oficiais intactos: uma usuária, cinco equipes
  e nenhum convite, sem expor hashes ou tokens.
- Secret scan sem achados. Auditoria npm de runtime sem achados. Cinco achados
  altos preexistentes na cadeia de desenvolvimento ESLint/fast-glob/micromatch/
  braces; braces 3.0.3 antes/depois. Audit oferece downgrade major, não aplicado.

## Ajustes e operação local

Porta 3000 pertence a outro projeto via IPv6 e foi preservada. A primeira
verificação recebeu o 404 daquele projeto. A usuária pediu evitar 3000/3001;
o Workspace passou para **http://localhost:3010**, com FRONTEND_ORIGIN local
correspondente. Somente processos deste Workspace foram reiniciados.
O bloqueio inicial do navegador foi superado na nova URL autorizada.

O teste HTTP foi ajustado para redirects por meta refresh em streaming do
Next 16, sem dados privados no HTML anônimo. A mensagem de login diferencia
401 de falha de rede. Logs de requests de /convite/ são omitidos no dev para
não expor o token na URL. Tasks não foi iniciado.
