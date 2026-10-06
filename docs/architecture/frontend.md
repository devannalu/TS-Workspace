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

## Tarefas

`/tarefas` usa `tasks.view` e a navegação real do shell. `lib/api/tarefas.ts`
concentra contratos, filtros e escritas Java com CSRF central. Componentes ficam
juntos em `components/tarefas`: quadro, cartão, formulário e detalhe. O backend
devolve capacidades efetivas; não há cópia das regras de SUPPORT no React.

O quadro carrega 25 tarefas por coluna, com total e botão para mais páginas.
Busca/filtros são aplicados no servidor; digitação é agrupada em 300 ms e respostas
obsoletas são descartadas. “Minhas tarefas” usa o mesmo quadro. Arquivadas são
uma consulta separada, somente leitura. Nenhuma requisição por cartão na listagem.

`@dnd-kit/core` 6.3.1 e `@dnd-kit/sortable` 10.0.0 oferecem mouse, touch com atraso
e teclado (espaço/setas/Escape), overlay e anúncios em PT-BR. A ponte de tipos
`dnd-kit.d.ts` adapta JSX global ao namespace React.JSX do React 19 sem desativar
checagem de bibliotecas. “Mover para…” funciona sem arraste e permanece no celular.
Desktop mostra quatro colunas; tablet limita scroll ao quadro; celular usa
segmentos e uma coluna vertical. Cartões têm prioridade textual, prazo sem
conversão de fuso, equipe e iniciais com nomes acessíveis.

Criação, edição, detalhe e confirmação de arquivo compartilham Dialog nativo.
409 de versão mantém o formulário e pede atualização explícita antes de outra tentativa;
400/401/403/404 e conexão continuam no tratamento central. Respostas são
reconsultadas após escrita para obter versões/posições atuais. O Dashboard usa
`/tasks/summary`, sem baixar tarefas para contar, e preserva seus indicadores anteriores.

Veja [setup](../development/setup.md), [RBAC](rbac.md) e [testes](../development/testing.md).

## Projetos

`/projetos` exige `projects.view`, com navegação real no shell. A feature fica em
`components/projetos`: lista paginada, card/progresso, formulário, detalhe e
seletor reutilizado por Tarefas. `lib/api/projetos.ts` concentra os contratos.
Não há Kanban de projetos nem rota de detalhe adicional. O diálogo apresenta
objetivo, responsáveis, período e resumo agregado das tarefas, com “Ver tarefas”
para `/tarefas?projetoId=<id>` no Kanban existente.

Cards mostram percentual acompanhado de contagem e barra acessível; projetos
vazios exibem “Sem tarefas”. Filtros ficam em seção recolhível, com equipe,
status, responsável, busca, “Sou responsável” e arquivados. Lista usa 24 itens
por página, digitação agrupada e descarte de respostas obsoletas. Celular usa
uma coluna, tablet duas e desktop três. Formulários têm labels, limites de texto,
datas opcionais e responsáveis da equipe, com seleção revisável ao trocar equipe.

Capacidades vêm do Java. SUPPORT não recebe controles de edição/arquivo. Conflito
de versão mantém rascunho e exige atualização explícita; conflitos de pendências
ou vínculos mostram instrução contextual sem bloquear como versão. O cliente
aceita somente mensagens 409 conhecidas; detalhes técnicos desconhecidos continuam
ocultos. Não há retry automático de escritas.

Tarefas têm seletor opcional de projeto por equipe e filtro que interpreta a query
string. Opções são pesquisáveis e paginadas; não baixam todos os projetos de uma
vez. Projetos concluídos não são elegíveis para novos vínculos. Ao trocar equipe,
o formulário limpa o projeto incompatível e avisa explicitamente. Cards de tarefas
mostram uma referência discreta. O Dashboard consulta `/projects/summary` somente
com `projects.view`, sem espaço reservado ou contagens fictícias para quem não pode ver.

## Organização por feature

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

## Colaboração contextual

Detalhes de Tarefas/Projetos reutilizam AbasColaboracao: Detalhes, Comentários,
Atividade. A API é consultada só ao abrir aba, sem polling, feed global ou novas
rotas. Componentes compactos em comentarios reutilizam Button, Dialog, feedback
e uma primitiva Textarea. Tabs usam setas/Home/End, labels e foco visível.

Conversa em ordem cronológica; carregamento anterior de 25 itens com controle
explícito. Composer com quebra de linha normal; edição inline; confirmação de
remoção com foco e placeholder cronológico. Texto é renderizado pelo React,
sem HTML/Markdown. Rascunho permanece na falha/409 e refresh exige ação explícita.
Estado é local, sem localStorage. Capacidades do servidor determinam ações;
401/403 e CSRF seguem o cliente Java central. Atividade paginada usa mensagens
seguras e data humana, sem conteúdo duplicado. Sem realtime/mentions/notificações.
