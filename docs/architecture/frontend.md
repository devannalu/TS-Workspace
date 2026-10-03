# Frontend

A aplicação em frontend/ utiliza Next.js App Router, React, TypeScript estrito
e Tailwind CSS. As versões estão no package.json e lockfile.

## API oficial

Todos os módulos usam src/lib/api/. http.ts centraliza base URL, cookies,
CSRF, parsing de Problem Details e ErroApi com status 400/401/403/404/409.
Mensagens controladas não expõem SQL, stacks ou detalhes de autenticação.
NEXT_PUBLIC_JAVA_API_URL é incorporada no build e nunca contém segredos.

CSRF fica somente em memória, com preparação concorrente deduplicada.
Login/logout invalidam esse cache para acompanhar a rotação do Spring.
401/403 também invalidam; a próxima ação explícita prepara novo token.
Nenhuma mutação é reenviada automaticamente.

## Sessão e rotas

A sessão permanece no cookie HttpOnly TS_SESSION, sem localStorage ou
sessionStorage. lib/sessao.ts consulta /auth/me com o cookie da requisição
e no-store. A ausência de sessão redireciona as páginas protegidas para login.
ProtecaoSessao trata loading, authenticated, unauthenticated, forbidden e
indisponibilidade, revalida no foco e trata expiração recebida pelo cliente.
A proteção Next é navegação; autorização real pertence ao Spring Boot.

Login/logout usam autenticacao.ts. Dashboard usa me e equipes reais. /equipes preserva
hierarquia, criação, edição, arquivamento e integrantes; /usuarias usa Users
e Invites. Editar role/cargo/equipes e ativar/inativar são ações separadas,
com suas permissões próprias. Menus e botões refletem as keys da sessão.

Server Components fazem somente GET usando server.ts; não encaminham cookies
para destinos escolhidos pelo usuário. Escritas vão diretamente do browser ao
Java. /convite/[token] valida por POST com CSRF, pede nome/senha/confirmação
e direciona para login após aceite, sem sessão automática. Link aparece apenas
na criação, em estado volátil; a página pública usa referrer no-referrer.

/infra oferece health e CSRF. Os guias da versão instalada ficam em
frontend/node_modules/next/dist/docs/, conforme AGENTS.md.

## Shell, páginas e dados

ShellWorkspace é uma composição server-side que exige sessão e entrega a
identidade Java ao ShellAplicacao. A navegação deriva das permissions atuais; o
ProtecaoSessao bloqueia a exibição durante bootstrap. Sidebar desktop e drawer
mobile compartilham os links reais. O header oferece avatar por iniciais,
contexto da página e menu pessoal com logout. Login e convite usam ShellPublico,
sem navegação autenticada.

O dashboard consulta auth/me e /teams/mine, que deriva as memberships da sessão,
sem aceitar identificador de outra usuária. Usuárias ativas usam o total da
consulta /users?status=ACTIVE&size=1. /invites/pending-count devolve a contagem
agregada de convites não usados, não cancelados e não expirados. Essas duas
leituras Java evitam percorrer equipes e páginas de convites. Somente
users.view habilita essas consultas e cards. Ações rápidas também exigem as keys
de criação correspondentes. RegistroAuditoria não tem endpoint de leitura apropriado;
atividade recente permanece futura.

Usuárias são paginadas no servidor (20 por página), com parâmetros de busca,
status, role e equipe. Convites têm paginação própria. Integrantes de equipes
podem ser procuradas pela API em lotes de 25; não é necessário carregar todas.
A listagem de equipes usa os resumos com memberCount; integrantes são carregadas
somente ao abrir o diálogo. Componentes de listagem e diálogos ficam próximos,
com responsabilidades separadas, sem hooks ou estado global artificiais.
Dialogs nativos fornecem confirmação, foco e Esc; feedback usa toasts locais.

A validação pública de convite mantém o erro genérico Java para links inválidos,
expirados, cancelados ou usados, sem revelar o estado pelo token. Falhas de
conexão oferecem nova tentativa. Após aceite, a página mostra sucesso e um
atalho para login, sem sessão automática.

Veja o [Design System](design-system.md) para tokens, componentes e responsividade.

Veja [setup](../development/setup.md), [RBAC](rbac.md) e [testes](../development/testing.md).

## Organização do código

`components/autenticacao`, `components/equipes`, `components/usuarios` e
`components/convites` reúnem apresentação por domínio. `components/layout`
compõe os shells; `components/ui` contém primitivas técnicas compartilhadas.
`DialogosEquipe` trata criação, edição e arquivamento; `DialogoIntegrantesEquipe`
possui a busca e as mutações de integrantes. Ambos preservam os estados e o
comportamento anterior, sem um hook genérico de diálogo.

`lib/api/contratos.ts` concentra os três tipos comuns. `http.ts` é o único
cliente de mutações Java e também expõe a consulta de saúde. `acoes.ts` converte
erros controlados em feedback local. `lib/sessao.ts` contém as leituras de sessão;
`lib/ui/painel.ts`, `permissoes.ts` e `formatacao.ts` são utilitários concretos.
Rotas, chaves JSON, permissions e eventos de expiração permanecem compatíveis.
Nomes técnicos de React, HTML e componentes do Design System ficam em inglês.
