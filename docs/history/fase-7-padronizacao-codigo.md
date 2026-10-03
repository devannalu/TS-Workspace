# Fase 7 — Padronização de código e arquitetura limpa

Checkpoint de 3 de outubro de 2026, iniciado sobre `0e240871070ca276fbe935d67a309493d2e9c37d`.
Escopo: refatoração interna, sem novas funcionalidades, redesign ou Tasks.

## Backend revisado

Autenticação, segurança, RBAC, equipes, usuárias/perfis, convites, auditoria,
compartilhado e infraestrutura foram tratados em sequência, com compilação e
testes relevantes antes de avançar. O total final permaneceu em 148 testes.

| Antes | Depois |
| --- | --- |
| AuthController / AppUserDetailsService | AutenticacaoController / AutenticacaoUsuarioService |
| BootstrapService / BootstrapRunner | InicializacaoIdentidadeService / InicializacaoIdentidadeRunner |
| SessionRevocationService | RevogacaoSessaoService |
| SecurityConfig / ActiveUserFilter | SegurancaConfig / UsuarioAtivoFilter |
| Role / Permission / PermissionGuard | PerfilAcesso / Permissao / VerificadorPermissao |
| PermissionPolicy / PermissionService | PoliticaPermissao / PermissaoService |
| Team / TeamMember / TeamService / TeamPolicy | Equipe / MembroEquipe / EquipeService / PoliticaEquipe |
| User / Profile / UserManagementService | Usuario / Perfil / UsuarioService |
| Invite / InviteService / InvitePolicy | Convite / ConviteService / PoliticaConvite |
| AuditLog / AuditService | RegistroAuditoria / AuditoriaRepository |
| OrganizationLock / DomainProblem | BloqueioOrganizacao / ProblemaDominio |
| ApiErrors / HealthController | TratamentoErrosApi / SaudeController |

Também foram traduzidos os repositories, controllers, IDs compostos, records
internos e nomes dos testes relacionados. DTOs pequenos continuam junto ao caso
de uso. Não foi criada uma árvore de pastas DTO/mapper/service/impl.

Casos de uso agora têm nomes como `listarEquipes`, `criarEquipe`, `adicionarIntegrante`,
`editarUsuario`, `alterarSituacaoUsuario`, `criarConvite`, `aceitarConvite`,
`revogarSessoesDoUsuario`, `buscarPermissoesUsuario` e `registrar`.

Queries saíram dos serviços de equipes, gestão de usuárias, convites e permissões
para `EquipeRepository`, `GestaoUsuariosRepository`, `ConviteRepository` e
`PermissoesUsuarioRepository`. Políticas e transações permanecem nos serviços.
A validação da paginação permanece no serviço para preservar o erro HTTP 400;
ela não fica sujeita à tradução de exceções de persistência do Spring.

`AuditoriaRepository` apenas grava dentro da transação mandatória existente;
um serviço que só encaminharia essa chamada foi evitado. O erro duplicado
`TeamProblem` foi consolidado em `ProblemaDominio`.

## Frontend revisado

API, sessão/autenticação, shells, painel, Equipes, Usuárias, Convites, utilitários,
Design System e testes foram revisados em sequência. Componentes estão agrupados
em `autenticacao`, `equipes`, `usuarios` e `convites`, próximos aos seus consumidores.

| Antes | Depois |
| --- | --- |
| LoginForm / LogoutButton / SessionBoundary | FormularioLogin / SairButton / ProtecaoSessao |
| AppShell / PublicShell / WorkspaceShell / Brand | ShellAplicacao / ShellPublico / ShellWorkspace / MarcaWorkspace |
| TeamManagement / TeamDialogs | GestaoEquipes / DialogosEquipe / DialogoIntegrantesEquipe |
| UserManagement / UserList / UserDialogs | GestaoUsuarios / ListaUsuarios / DialogosUsuario |
| InviteForm / InviteList / PublicInvite | FormularioConvite / ListaConvites / ConvitePublico |
| InviteAcceptForm / CancelInviteButton | FormularioAceiteConvite / CancelarConviteButton |

`DialogoIntegrantesEquipe` separa busca, leitura e mutações de integrantes do
formulário de equipe, preservando carregamento, fechamento, transição e feedback.
Não foi criado hook genérico para justificar essa separação.

`lib/auth/session.ts` virou `lib/sessao.ts`; os três contratos compartilhados
ficam em `lib/api/contratos.ts`. O wrapper `managementRequest` foi eliminado;
`requisitarJava` é a autoridade única de cookies, CSRF e erros. A consulta de saúde
foi consolidada em `http.ts`, permitindo remover `client.ts`.

Os módulos API são `autenticacao.ts`, `equipes.ts`, `usuarios.ts`, `convites.ts`
e `acoes.ts`. Utilitários são `painel.ts`, `permissoes.ts` e `formatacao.ts`.
Funções expressam a intenção: `entrarNoWorkspace`, `exigirSessao`, `buscarMinhasEquipes`,
`capturarResultadoMutacao`, `buscarTotaisPainel`, `formatarData` e `normalizarPagina`.

O helper morto em `lib/permissions/index.ts` e quatro exports sem consumidores
(`listarEquipes`, `listarConvites`, `buscarUsuario`, `buscarOpcoesUsuarios`) foram
removidos. Leituras server-side continuam usando `lerJava`. Aliases de DTO repetidos
foram substituídos pelos contratos já existentes. Pastas antigas ficaram vazias e
foram removidas; não há cópia esquecida do código anterior.

## Comentários e exceções técnicas

Comentários e Javadocs que narravam classes, getters ou operações evidentes foram
removidos. Permaneceram explicações de proteção de detalhes técnicos, renovação
CSRF sem replay, foco após desmontagem do diálogo, envio de mutações direto ao Java
e auditoria sem metadados arbitrários. Esses comentários explicam restrições reais.
O Javadoc histórico da migration Java V5 não foi reescrito.

Exceções de nomes em inglês:

- `auth.AppUserPrincipal` e `auth.ProfileStatus`: classes usadas em sessões serializadas;
- `EmailNormalizer.normalize` e `RbacSeed.seed`: referências da migration V5 imutável;
- fields dos records públicos e payloads JSON: contrato externo existente;
- `Repository`, APIs Spring/React/Next, HTML, enums persistidos, RBAC e HTTP: convenções técnicas;
- Button, Input, Badge, Card, Dialog, Avatar, Pagination, Toast, PasswordInput,
  Skeleton, ErrorState e EmptyState: primitivas compartilhadas do Design System.

Não há segunda implementação de autenticação. O backend Java continua sendo a
única autoridade; os nomes de compatibilidade não reativam Prisma/Better Auth.

## Contratos, banco e segurança

A comparação confirmou 25 endpoints, 25 records públicos e 13 permission keys.
Endpoints, métodos HTTP, payloads, DTOs externos, códigos de erro, permission keys,
roles, eventos de auditoria, cookies, CSRF e precedência de autorização foram preservados.
A comparação das declarações dos records públicos identificou e corrigiu a tradução
indevida de `page`, `size` e `invitedBy.name`; o teste existente passou a proteger
explicitamente essas chaves. O navegador confirmou a paginação correta.

Schema e migrations V1–V7, incluindo V5 Java, permanecem sem diff desde o checkpoint
inicial. Hibernate continua em validate. O backend iniciou com V7 atual, sete migrations
validadas e nenhuma migration nova executada. Health e CSRF responderam HTTP 200.
MySQL Java permanece healthy; bancos legados continuam desligados, volumes preservados.

Nenhuma dependência foi adicionada, removida ou atualizada. POM, package.json e
lockfile não mudaram. Secret scan não encontrou segredos; uma comparação adicional
com quatro valores reais dos arquivos locais também não encontrou correspondências
no conteúdo versionável. `.env`, ambientes locais do frontend, target e `.verification`
continuam ignorados. Evidências não contêm credenciais no repositório.

## Validação

- Java: 148 testes em `test` e 148 em `package`; zero failures/errors/skips.
- Frontend: typecheck, lint sem warnings, 36 testes e build aprovados.
- Navegador em localhost:3010: login oficial e conta Suporte, logout, proteção sem sessão,
  refresh mantendo sessão, painel, Equipes, Usuárias, criação/cancelamento/aceite de
  convite, acesso limitado e bloqueio de login da conta inativa.
- A sessão aberta antes da refatoração sobreviveu ao reinício do backend.
- Diálogo de integrantes: leitura, inclusão e remoção de conta temporária aprovadas.
- Desktop 1440, tablet 768 e mobile 390 sem overflow; menu móvel e Escape preservados.
- Dados transitórios: uma usuária, uma equipe e dois convites removidos com escopo
  explícito. Contagens e hashes das seis tabelas oficiais ficaram idênticos ao baseline.

Logs, screenshots e auxiliares permanecem locais em `.verification`, ignorados.

## Documentação e commits

`AGENTS.md` recebeu regras permanentes antes das mudanças. Convenções, testes e
arquitetura viva foram atualizados; documentos históricos anteriores não foram reescritos.

Commits da fase:

1. `199ddf1` — autenticação, segurança, RBAC e equipes;
2. `818828d` — usuárias, convites, auditoria e infraestrutura;
3. `67aa0f4` — preservação explícita do JSON dos convites;
4. `e75d3b9` — organização do frontend, utilitários e testes;
5. documentação e registro deste checkpoint.

O SHA do último commit é informado na entrega e verificável pelo Git. Publicação
usa push normal para origin/main, sem force, com comparação local/remoto.

## Pendências reais

A auditoria npm confirma cinco alertas HIGH na cadeia de lint já existente:
`eslint-config-next`, `@next/eslint-plugin-next`, `fast-glob`, `micromatch` e `braces`.
A auditoria com `--omit=dev` retorna zero achados. Resolver a cadeia de desenvolvimento
exige atualização própria e não foi mascarado por um upgrade forçado nesta refatoração.
Flyway mantém o aviso existente sobre MySQL 8.4 ser mais novo que o suporte declarado
8.1; validação e execução passaram. Nenhuma dessas pendências foi introduzida nesta fase.
Tasks, SMTP, recuperação de senha e outras funcionalidades futuras não foram iniciadas.
