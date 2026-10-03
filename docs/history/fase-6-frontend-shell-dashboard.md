# Fase 6 — Frontend Shell e Dashboard

Checkpoint de 3 de outubro de 2026, após o cutover Java publicado em
`870a471886f4c0cb07eab4f51ccf17307a51ba6f`.

## Interface entregue

Identidade Tech Sisters com creme, rosa blush, lavanda, manteiga e pêssego,
texto escuro e tokens centralizados. Bordô deixou de ser a cor dominante;
o favicon original e o monograma foram preservados. Fontes de sistema e
Lucide existentes; sem imagens geradas, novas fontes ou biblioteca de UI.

AppShell reúne sidebar desktop, drawer mobile, contexto de página, avatar de
iniciais e menu com identidade, perfil e logout. Links e ações respeitam as
permissions Java. Módulos futuros não aparecem na navegação. Login e convite
usam um shell público próprio, com estados de conexão, validação e sucesso.

Dashboard mostra identidade de /auth/me, equipes pessoais, total de usuárias
ativas, convites pendentes, perfil e ações rápidas permitidas. Não há métricas
inventadas nem feed de auditoria sem API. Equipes mantêm hierarquia, criação,
edição, arquivamento confirmado e gestão de integrantes. Usuárias têm busca,
filtros, paginação, tabela desktop, cards tablet/mobile, edição e confirmação
de inativação/reativação. Convites têm paginação, datas, status e cancelamento.

Links de convite aparecem uma única vez, em estado volátil, com cópia e feedback,
e são apagados ao fechar. Aceite solicita nome, senha e confirmação; mostra
sucesso e direciona para login sem criar sessão. A API mantém a mesma resposta
400 para convite inválido, expirado, cancelado ou usado; a UI apresenta uma
mensagem comum, sem simular distinções que comprometeriam a privacidade.

Primitivas compartilhadas incluem Button, Input, PasswordInput, Card, Badge,
Avatar, Dialog/Drawer, Toast, Skeleton, EmptyState, ErrorState e Pagination.
Menus usam disclosure nativo. Há estados de loading, falha de rede, ausência
de resultados, acesso recusado e 404.

## Revisão de arquitetura

Listas e diálogos foram separados em poucos arquivos próximos. Não foram
adicionados hooks artificiais, stores globais, mappers ou camadas genéricas.
AdminNav sem uso foi removido. Comentários alterados explicam decisões em PT-BR.

Duas leituras mínimas foram necessárias para evitar loops de consulta:
GET /teams/mine reutiliza o resumo existente e filtra pela identidade da sessão;
GET /invites/pending-count faz COUNT dos convites não usados, não cancelados
e não expirados. Exigem teams.view e users.view, respectivamente, mantendo
as recusas 401/403. Não houve migration, alteração de escrita ou novo módulo.
Integrantes de equipes são buscadas apenas quando seu diálogo é aberto.

## Verificações

- Frontend: 36 testes aprovados; typecheck, lint sem warnings e build aprovados.
- Java: 148 testes aprovados em test e package, sem failure/error/skip. Os dois
  testes novos verificam isolamento por membership, arquivamento, contagem de
  estados de convite e permissões das leituras acrescentadas.
- Docker Engine acessível; somente o MySQL Java do projeto foi iniciado após
  a retomada. Banco saudável, health HTTP 200 e sete migrations validadas,
  schema em V7, sem mudança nas migrations. Hibernate iniciou normalmente.
- Navegador em localhost:3010: login, logout, refresh de sessão, redirecionamento
  anônimo, perfil amplo/limitado, recusa de área administrativa, filtros,
  edição, inativação/reativação, criação/cópia/aceite/cancelamento de convite,
  convite expirado, gestão de integrantes e arquivamento.
- Dashboard inspecionado em 1440, 1280, 1024, 768 e 390 pixels, sem overflow
  horizontal. Usuárias usam cards abaixo de 1280 pixels, evitando colunas
  comprimidas no tablet. Login, equipes e experiência pública também revisados.
- Teclado: foco preso no diálogo, Esc, retorno ao acionador e drawer mobile.
  Labels associados, foco visível, alvos de 44 pixels e reduced motion.
- Contraste medido: texto principal 13,15:1; muted 5,59:1; texto soft 4,77:1;
  branco no primary 5,58:1; estados semânticos acima de 5,4:1; borda de campo
  3,48:1. Essas medições não representam auditoria completa por leitor de tela.
- Console final sem erros ou warnings. Dados temporários removidos com escopo
  por IDs de teste; contagens e digests dos dados oficiais iguais ao baseline.
- Secret scan sem achados. Env reais, bootstrap, logs, evidências e artefatos
  compilados permanecem ignorados; não há dados reais de teste no commit.

Testing Library, user-event e jsdom foram adicionados somente como dependências
de desenvolvimento para testar comportamento real de componentes. Nenhuma nova
dependência de produção. A configuração eslint-config-next 16.3.8 encontrada
durante a execução foi preservada e validada com Next.js 16.3.7.

## Falhas corrigidas e limites

Foram corrigidos labels de senha, menu de ações cortado, tabela estreita no
tablet, foco de diálogos desmontados, erro seguro de rede/JSON e fechamento do
link recém-criado durante refresh. O teste da nova leitura de convites detectou
ausência na lista explícita de rotas permitidas; a rota foi incluída mantendo
Method Security. Uma falha de encoding de comentário foi normalizada para UTF-8.

npm audit aponta cinco vulnerabilidades HIGH na cadeia de lint de desenvolvimento
(@next/eslint-plugin-next, fast-glob, micromatch e braces). npm audit --omit=dev
aponta zero vulnerabilidades. A correção automática proposta envolve downgrade
major incompatível; não foi aplicada. A cadeia de ferramentas requer atualização
compatível posterior. Flyway mantém o aviso já existente sobre suporte a MySQL
8.4; migrations e testes reais passaram.

Atividade recente, Tasks e demais módulos futuros permanecem fora do escopo.
Nenhum serviço de outro projeto foi encerrado, nenhum volume foi removido e
nenhuma alteração de segurança foi flexibilizada.

Commit do checkpoint: `feat: redesign workspace shell and dashboard`.
O SHA definitivo e a confirmação do push constam no relatório de entrega;
este registro acompanha o próprio commit, sem referência circular ao seu SHA.

Guias atuais: [frontend](../architecture/frontend.md),
[Design System](../architecture/design-system.md) e
[convenções](../development/conventions.md).
