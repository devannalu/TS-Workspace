# Fase 9 — Projetos

Implementação sobre o checkpoint `3019ea3bfa3dd9d846515a839bf3260f9c325f2c`,
preservando Auth, RBAC, Equipes, Usuárias/Convites, Design System e Tarefas.

## Domínio e persistência

V9 cria `project`, `project_responsible` e `task.project_id` nullable, com FKs,
checks e índices. V1–V8 permanecem intactas. Nenhuma tarefa anterior recebe
projeto obrigatório. Projeto tem título, descrição, equipe, criadora da sessão,
responsáveis da equipe, datas opcionais, versão e arquivamento lógico.

Status: PLANEJADO, EM_ANDAMENTO, PAUSADO e CONCLUIDO. Datas usam LocalDate/DATE,
com início <= fim quando ambas existem. Título admite repetição e até 200
caracteres; descrição é texto simples limitado a 5000. Não existem comentários,
anexos, calendário, orçamento ou schema antecipando outros módulos.

Progresso deriva somente das tarefas não arquivadas. Percentual é concluídas /
total, arredondado; sem tarefas é null e a interface mostra “Sem tarefas”.
Chegar a 100% não conclui o projeto automaticamente. Conclusão e arquivo exigem
ausência de tarefas ativas pendentes. Projeto vazio pode ser concluído. Projeto
CONCLUIDO bloqueia novos vínculos e reabertura de suas tarefas concluídas; é
preciso reabrir o projeto. PAUSADO não bloqueia edição/movimentos.

Projeto e tarefa devem ter a mesma equipe. A tarefa admite adicionar, trocar ou
remover projeto no formulário completo; null representa ausência de vínculo.
`first_task_linked_at` é um marco persistente que impede trocar equipe depois
de qualquer vínculo, mesmo após desvincular todas as tarefas. Não armazena
contagens/progresso. Troca anterior ao primeiro vínculo revalida responsáveis,
sem remoção silenciosa. Remover membership exige retirar responsabilidades de
projetos ativos ou arquivá-los. Arquivar projeto não arquiva tarefas concluídas;
mantém vínculos históricos. Equipe/projeto arquivados são somente leitura.

## API, autorização e concorrência

Domínio compacto em `projetos`, com duas entidades, política, repository,
serviço e controller; DTOs são records próximos aos casos de uso. Listagem
paginada agrega tarefas em SQL e carrega responsáveis em lote. Não há N+1
por card ou segunda implementação de Tarefas.

`/api/v1/projects`: lista, detalhe, criação, edição, arquivo, options e summary.
Filtros de equipe, status, responsável, busca literal, período, arquivo e página.
`/api/v1/tasks` recebe `projetoId` no corpo e `projectId` no filtro HTTP.
Resumo conta projetos não arquivados do escopo. Prazo próximo é hoje até +7
dias inclusive, exceto CONCLUIDO, no fuso America/Bahia.

Cinco novas keys: projects.view/create/edit/manage_members/archive. Catálogo
totaliza 24 keys e 64 grants padrão. ADMIN possui todas; SUPERVISOR somente
suas equipes e sem archive; SUPPORT somente leitura de suas equipes; SUPER_ADMIN
mantém bypass ativo. Responsabilidade não amplia escopo/permissão. Overrides
continuam funcionando; SUPPORT permanece somente leitura no domínio Projetos.
Grants antigos são preservados e seed permanece idempotente.

Escritas de projetos e vínculos usam o mesmo bloqueio transacional de Fundadoras
antes das leituras e CAS de versão no SQL. Primeiro vínculo incrementa versão
do projeto. Teste concorrente real aceita uma atualização e rejeita a outra.
409 de versão preserva rascunho e pede refresh explícito. Pendências de conclusão/
arquivo, troca de equipe histórica e vínculos incompatíveis usam mensagens
distintas. Cliente aceita somente uma lista de mensagens seguras conhecidas,
sem expor detalhes SQL ou repetir mutações automaticamente.

Auditoria mínima: project.created/updated/status_changed/responsibles_changed/
archived. Mudança de vínculo reutiliza task.updated; sem payloads sensíveis
ou eventos duplicados para alterações exclusivamente de responsáveis/status.

## Interface

`/projetos` na sidebar com projects.view. Cards exibem objetivo, equipe, status,
período, iniciais das responsáveis e progresso acessível acompanhado de contagem.
Lista com 24 por página, filtros recolhíveis, busca e “Sou responsável”. Criação,
edição, detalhes e confirmação de arquivo usam Dialog nativo existente. Tarefas
relacionadas aparecem como resumo e link `/tarefas?projetoId=<id>`.

Tarefas reutilizam seletor pesquisável/paginado, limitado à equipe e projetos
elegíveis; trocar equipe limpa vínculo e avisa. Card mostra referência discreta.
Dashboard acrescenta resumo real somente com projects.view, sem substituir
indicadores anteriores ou redesenhar o shell. Nenhuma dependência nova.

## Validação

- Maven `test`: 205 testes, zero failure/error/skip.
- Maven `package`: 205 testes, zero failure/error/skip e JAR produzido.
- Frontend: 71 testes; typecheck, lint com zero warnings e build aprovados.
- MySQL Java healthy; Flyway V9 e Hibernate validate; health e CSRF HTTP 200.
- HTTP real: login, criar/listar/editar projeto, responsáveis, status, tarefa
  vinculada, progresso, conclusão/reabertura, arquivo e logout 204. Confirmados
  401/403/409, incompatibilidade entre equipes, pendências e novos vínculos
  bloqueados em projeto concluído.
- Navegador em localhost:3010: sidebar, criação, edição de status/responsáveis,
  cards, detalhe, progresso de 50%, filtros, arquivo/histórico, resumo Dashboard,
  link para Kanban e seletor de projeto da tarefa. Troca de equipe mostrou aviso.
- Larguras 1440, 1280, 768 e 390: cards, filtros e diálogo, sem overflow horizontal.
- SUPPORT: apenas projeto da própria equipe, sem criar/editar/arquivar; detalhe
  somente leitura. Conflito de versão real preservou rascunho e exigiu atualização;
  pendência de arquivo recebeu instrução contextual.

Os ajustes encontrados durante verificação foram na consulta de auditoria do
teste, na expectativa de logout (204 existente) e na espera do carregamento de
opções antes de submeter formulários em testes. O formulário de Projeto passou
a indicar carregamento inicial das responsáveis antes de habilitar salvar.
O filtro rápido “Sou responsável” também mantém “Eu” selecionado no filtro
de responsável, mesmo sem equipe selecionada.

Dados de validação usam nomes identificados e equipes temporárias. A limpeza
é restrita a essas fixtures, seguida de comparação de hashes e quantidades dos
dados oficiais. Volumes e bancos de outros projetos permanecem preservados.
Prisma/Better Auth continuam sem dependência runtime; bancos legados permanecem
parados. Nenhum módulo posterior foi iniciado.

Auditoria de dependências: nenhuma vulnerabilidade runtime; permanecem cinco
achados HIGH na cadeia de ferramentas de lint já existente. Não foram feitas
atualizações de dependências fora do escopo desta fase.
