# Fase 12 — Calendário

Checkpoint inicial: c8f1f5919d9a71a055d08b5006f994ab61521cd2.
Entrega concluída e validada em 07/10/2026, com publicação em origin/main.

## Arquitetura e contrato

Calendário é uma projeção de Tarefas e Projetos e não possui persistência própria.
Sem entidade, tabela, migration ou permission de calendário. Flyway permanece em V11.
O domínio compacto calendario contém controller de leitura, serviço e repository JDBC.

GET /api/v1/calendar exige from/to ISO date-only, intervalo inclusivo de até 366 dias,
e aceita teamId, types=TAREFA,PROJETO e responsibleId. Intervalos inválidos retornam 400;
sem sessão, 401; sem nenhuma fonte autorizada, 403. O DTO contém id (tipo:UUID), tipo,
recursoId, titulo, dataInicio/dataFim, equipe, status, prioridade nullable, responsaveis,
concluido e atrasado.

Tarefas usam due_date; sem prazo não aparecem. Projetos usam interseção inclusiva:
COALESCE(start_date,end_date) <= to e COALESCE(end_date,start_date) >= from.
Uma data isolada representa um dia; sem datas não aparece. Arquivados são excluídos.
Concluídos continuam visíveis de forma discreta. Atraso de Tarefa reutiliza a política
existente; Projeto exige prazo final anterior a hoje e status não concluído.

Permissões tasks.view/projects.view habilitam cada fonte separadamente. O escopo
reutiliza a montagem das consultas e as políticas das fontes: ADMIN/SUPER_ADMIN global,
SUPERVISOR/SUPPORT conforme equipes acessíveis. SQL seleciona apenas campos necessários
com responsáveis em lote, até quatro consultas das fontes, sem N+1, descrição, colaboração
ou agregado caro de progresso.

LocalDate e YYYY-MM-DD preservam date-only, sem converter o domínio para Instant.
UTC é usado somente na aritmética interna e formatação explícita do frontend, impedindo
deslocamento de datas. Hoje local segue o calendário da usuária; atraso segue America/Bahia,
como a política existente de Tarefas.

## Interface

/calendario e sidebar exigem tasks.view OU projects.view. Mês começa na segunda-feira,
limita a três cards por célula e oferece +n e lista completa do dia selecionado. Mobile
mostra indicadores e lista diária. Semana mantém sete colunas all-day com scroll contido;
Agenda agrupa cronologicamente e mostra cada projeto uma vez com seu período original.

Há Hoje/anterior/próximo, visão/data na URL, equipe, tipo autorizado, responsável da equipe
selecionada e Meus itens. Controles permanecem durante skeleton, vazio ou erro com retry.
Dados de requisições obsoletas são descartados. Clique consulta o recurso autorizado e
reutiliza DetalheTarefa/DetalheProjeto; mudanças no detalhe atualizam a projeção.

Nenhuma dependência adicionada, evento manual, horário, drag, integração externa, ICS,
lembrete ou alteração na arquitetura do storage.

## Recuperação e infraestrutura

Na retomada, Docker 29.7.2 respondeu Client e Server, em desktop-linux; docker info
funcionou. Foram iniciados somente os containers existentes ts-workspace-mysql-java e
ts-workspace-garage. Ambos healthy. Volumes, bucket privado ts-workspace-dev e dados
oficiais foram preservados, sem recriação, prune, reset ou alteração de outros projetos.

Backend iniciado pelo run-dev.ps1 documentado; frontend em http://localhost:3010,
sem ocupar portas de outros projetos. Health e CSRF HTTP 200. Flyway validou as onze
migrations e informou schema V11 atualizado, sem migration necessária. Hibernate
com ddl-auto: validate inicializou sem erro de schema.

## Validação final

- mvnw.cmd -B test: 241 testes, zero failures/errors/skips, BUILD SUCCESS.
- mvnw.cmd -B package: 241 testes, zero failures/errors/skips, jar executável aprovado.
- Nove testes novos de CalendarioIntegrationTest executados com MySQL real via Testcontainers.
- Frontend: typecheck, lint, 123 testes em 15 arquivos e build aprovados.
- Date-only 2026-10-07 preservado em testes e HTTP real; quatro timezones testados:
  America/Bahia, Europe/Lisbon, Pacific/Kiritimati e America/Los_Angeles.
- HTTP autenticado real: intervalos, datas exatas, tarefas/projetos com e sem datas,
  concluídos/arquivados/atrasados, overlap, filtros, Meus itens, permissões parciais,
  escopo de equipes e ausência de vazamento. 401/400/403 conferidos.
- Navegador real: login, refresh da sessão, sidebar, Mês/Semana/Agenda, Hoje/anterior/próximo,
  seleção do dia, expansão, filtros, detalhes reutilizados e permissões parciais aprovados.
- Mês/Semana/Agenda conferidos em 1440/1280/768/390px, sem overflow horizontal da página.
  Semana em telas menores tem scroll interno. Detalhes de Tarefa/Projeto funcionam no mobile.
- Loading, vazio e erro de conexão seguro com retry verificados. Para o erro, somente o
  backend deste projeto foi interrompido e reiniciado; recuperação e sessão confirmadas.
- Teclado e focus visible conferidos, labels e dias identificáveis; estado/tipo comunicados
  por texto, controles reais e atributos aria. Console sem erros relevantes de CORS/hidratação.
- Todas as fixtures HTTP/navegador removidas: tarefas, projetos, equipes, usuárias,
  permissões temporárias, sessões e registros associados. Hashes e quantidades dos dados
  oficiais idênticos ao baseline após cleanup.

## Correções e revisão

Durante a implementação foram corrigidos: loading preso ao acionar Hoje no mesmo período,
validação da visão da URL contra propriedades de protótipo, abertura de detalhes somente
leitura sem exigir opções de equipe arquivada, e atraso indevido de projeto com apenas início.
Parâmetro obrigatório ausente retorna 400 pelo tratamento central. A retomada não exigiu
redesign nem mudanças de funcionalidade; os testes e o navegador confirmaram o resultado.

Revisão de nomes, responsabilidades, imports, estrutura e diff concluída. Nenhum schema,
permission persistida ou dependência nova. Varreduras de segredos sem achados; configurações,
credenciais, tokens, cookies, URLs assinadas e evidências locais permanecem ignorados.

npm audit runtime: zero achados. Cinco HIGH preexistentes na cadeia de desenvolvimento
ESLint/typescript-eslint/minimatch permanecem, sem audit fix --force. Flyway registra aviso
preexistente de compatibilidade com MySQL 8.4, mas validação, execução e integração passaram.
Nenhuma pendência funcional da Fase 12. Publicação por commit feat: adiciona calendario do workspace,
sem force push; confirmação de SHA e status final entregue no relatório da fase.
