# Fase 8 — Tarefas

Registro de 06/10/2026. Escopo: módulo de tarefas no Spring Boot e Next.js,
preservando sessão Java, RBAC, equipes/usuárias/convites e identidade pastel.
Projetos, comentários, uploads, calendário, notificações, subtarefas e recorrência
continuam futuros; nenhum campo foi reservado para eles.

## Modelo e banco

V8 cria task/task_assignee e completa cinco permissions com grants idempotentes.
V1–V7 permanecem intactas. Tarefa tem título obrigatório (200), descrição opcional
(5.000 na API), equipe, criadora derivada da sessão, prazo LocalDate, posição,
versão e timestamps Instant. Nenhum booleano persistido de atraso.
Status: A_FAZER, EM_ANDAMENTO, EM_REVISAO, CONCLUIDA. Prioridades: BAIXA, MEDIA
(padrão), ALTA, URGENTE. Responsáveis são 0..N integrantes ativos da equipe,
limitados a 50 por operação; mudança de equipe revalida todos os vínculos.

Arquivamento é lógico, sem DELETE público. Equipe arquivada mantém histórico
somente leitura; arquivar a tarefa histórica exige tasks.archive. Remover uma
membership com responsabilidade em tarefa ativa retorna 409. Isso mantém a
invariante mesmo quando Equipes ou Usuárias removem um vínculo.

## Autorização e concorrência

Matriz: SUPER_ADMIN/ADMIN têm as cinco keys; SUPERVISOR view/create/edit/assign;
SUPPORT view/create/edit. Grants antigos preservados, catálogo total 19 keys/49
grants padrão. DENY individual continua efetivo, exceto bypass SUPER_ADMIN.
SUPER_ADMIN/ADMIN usam escopo global; SUPERVISOR/SUPPORT somente suas equipes.
SUPPORT edita/move apenas tarefas próprias ou atribuídas a si. Overrides não
ampliam escopo de equipe. Capacidades efetivas vêm na resposta Java.

Não foi usado @Version: o domínio usa JDBC como a gestão atual. Cada escrita
adquire o bloqueio transacional existente de Fundadoras antes das leituras,
verifica a versão recebida e executa compare-and-swap no SQL. O teste paralelo
real dispara duas edições com a mesma versão: exatamente uma vence e a outra
recebe 409. Ordenação usa posição inteira global por status, renumerando apenas
colunas afetadas dentro da transação; linhas reposicionadas incrementam versão.
O custo de renumeração e a serialização global devem ser revistos com métricas
se o volume crescer. Não há Redis, filas ou locks somente em memória.

## Contratos e interface

GET /tasks lista; GET /tasks/{id} detalhe; POST /tasks cria; PATCH /tasks/{id}
edita conteúdo; PATCH /tasks/{id}/position move/reordena; POST /tasks/{id}/archive
arquiva. GET /tasks/options fornece equipes/integrantes elegíveis; GET
/tasks/summary agrega tarefas atribuídas à sessão. Todos sob /api/v1.
Filtros: teamId/status/priority/assigneeId/search/dueFrom/dueTo/archived/page/size.
Listagem é paginada (máximo 100), com busca literal e responsáveis em lote.
Respostas não serializam entidades, email, password hash ou sessão.

Auditoria mínima: task.created/updated/status_changed/assignees_changed/archived.
Sem texto do conteúdo ou metadados sensíveis; reordenação isolada não faz ruído.

/tarefas usa shell e tokens existentes, quatro colunas, filtros, histórico,
Minhas tarefas, detalhe/edição/criação e confirmação de arquivamento em Dialog.
Cada coluna carrega 25 registros e permite carregar mais. Mobile usa segmentos
e cartões verticais. Tablet contém scroll horizontal no quadro. Desktop mostra
as quatro etapas. Prioridades têm texto, prazo mantém o dia e responsáveis usam
iniciais com nomes acessíveis e overflow +N.
DnD oferece mouse, touch com atraso e teclado; Mover para é alternativa nativa
também no celular. Nenhuma operação é reenviada automaticamente. 409 preserva
formulário e exige atualização explícita. 401/403, CSRF e offline permanecem
no cliente central. Dashboard recebe resumo SQL real sem baixar tarefas para contar.

## Dependências e correções

Foram adicionados somente @dnd-kit/core 6.3.1 e @dnd-kit/sortable 10.0.0:
sensores de mouse/touch/teclado, ordenação, overlay e acessibilidade no Kanban.
Referência primária: [Sortable dnd-kit](https://dndkit.com/legacy/presets/sortable/overview/).
React 19 exigiu ponte de tipos JSX para as declarações publicadas do dnd-kit;
checagem de bibliotecas permanece ativa. A validação por teclado revelou seleção
incorreta de coluna vazia; closestCenter corrigiu o destino e o movimento foi
conferido no navegador.

Audit antes e após instalar DnD: seis HIGH, uma transitiva de runtime em
source-map-js. Atualização compatível 1.2.1 → 1.2.2 removeu essa ocorrência sem
major/override nem audit fix force. Referência:
[advisory source-map-js](https://github.com/advisories/GHSA-68fv-2mgg-jv7q).
Resultado final: cinco HIGH já conhecidas na cadeia de lint, zero vulnerabilidades
de runtime e nenhuma introduzida pelo DnD. A atualização major da cadeia de lint
fica fora desta fase.

Testes iniciais corrigiram: expectativa antiga do catálogo RBAC; contains(null)
em Set.of vazio, substituído por validação antes de construir o conjunto; fixture
SQL de override que referia timestamps inexistentes. Nenhuma migration antiga
foi alterada e nenhum teste foi desativado.

## Validação

- Docker Engine recuperado pelo Desktop existente; somente MySQL Java do projeto iniciado.
- V8 aplicada no banco local; Hibernate validate e health HTTP 200.
- Maven test: 181 testes, zero failures/errors/skips.
- Maven package: 181 testes, zero failures/errors/skips; JAR empacotado.
- Frontend: 50 testes, typecheck, lint e build aprovados.
- Fluxo HTTP real: criação, múltiplas responsáveis, edição, movimento, arquivo,
  resumo e negativas 401/403/409, usando fixtures temporárias identificadas.
- Dados oficiais comparados por hashes/quantidades antes e depois da limpeza.
- Navegador em localhost:3010: login/logout Java, criação, edição, detalhe,
  arraste com mouse e teclado, reordenação, fallback mobile, filtros, refresh,
  arquivamento e histórico somente leitura. SUPPORT validada sem acesso a outra
  equipe, atribuição ou arquivamento; tarefa alheia fica para leitura.
- Conflito real com formulário aberto: outra escrita HTTP vence; navegador mostra
  409 e mantém o rascunho, sem sobrescrever ou reenviar. Separados bloqueio por
  conflito e loading, evitando apresentar “Salvando” após a operação terminar.
- Quatro larguras: 1440, 1280, 768 e 390; sem overflow da página, com scroll
  contido no quadro/tablet e uma coluna visível no celular. Foco restaurado após
  movimentação, Escape/foco do Dialog, labels e anúncios de status conferidos.

As evidências técnicas, screenshots e dados temporários ficam em .verification
ignorado, sem credenciais nos documentos versionados. A documentação viva de
produto, backend, frontend, banco, RBAC e testes acompanha o comportamento entregue.
