# Backend

## Stack e organização

A API utiliza Java 17 e Spring Boot 3.5, com dependências e plugins declarados
em `backend/pom.xml`. O Maven Wrapper fixa a distribuição do Maven e permite
compilar sem instalação global. Testcontainers fornece MySQL efêmero para
testes integrados.

O pacote base é `com.devannalu.tsworkspace`. A organização acompanha o domínio:

- `autenticacao`: login, identidade inicial e revogação de sessões;
- `seguranca`: cadeia Spring Security e bloqueio de usuárias inativas;
- `rbac`: catálogo, permissões, perfis de acesso e autorização;
- `equipes`: hierarquia, integrantes e operações transacionais;
- `usuarios`: identidade, perfil e gestão paginada;
- `convites`: criação, validação e provisionamento atômico;
- `tarefas`: regras, escopo, responsáveis, consulta paginada e ordenação do trabalho;
- `projetos`: objetivos, período, escopo, responsáveis e progresso derivado das tarefas;
- `auditoria`: persistência de eventos mínimos;
- `compartilhado`: bloqueio de Fundadoras e erros de domínio;
- `infraestrutura`: verificação de saúde e marcador do schema.

Controllers recebem DTOs e delegam. Serviços coordenam regras e transações.
Repositories possuem as consultas JPA/JDBC; políticas são funções de domínio
sem acesso a banco. `GestaoUsuariosRepository` concentra as consultas da gestão,
enquanto `UsuarioRepository` mantém a identidade usada na autenticação.
`AuditoriaRepository` apenas persiste dentro da transação existente, sem serviço
intermediário que só encaminharia argumentos. DTOs pequenos permanecem junto
aos seus casos de uso; não há hierarquia genérica de mappers ou repositories.

`BloqueioOrganizacao` protege invariantes compartilhadas de Fundadoras.
`ProblemaDominio` e `TratamentoErrosApi` centralizam os erros controlados.
`RevogacaoSessaoService` remove sessões da identidade pelo email indexado.

`auth.AppUserPrincipal` e `auth.ProfileStatus` permanecem nos nomes originais
para preservar sessões Java serializadas. `EmailNormalizer.normalize` e
`RbacSeed.seed` permanecem por serem referenciados pela migration Java V5,
que não pode ser reescrita após aplicação. Essa compatibilidade não cria duas
implementações de autenticação.

## Componentes

Tarefas usam `TarefaController`, `TarefaService`, `PoliticaTarefa` e
`TarefaRepository`; entidades `Tarefa` e `ResponsavelTarefa` documentam o mapping
validado pelo Hibernate. Consultas JDBC retornam DTOs e carregam responsáveis em
lote, evitando N+1. Capacidades de editar/atribuir/arquivar vêm do backend.

API `/api/v1/tasks`: GET lista, GET `/{id}`, POST criação, PATCH `/{id}` edição,
PATCH `/{id}/position` status/posição, POST `/{id}/archive`, GET `/summary` e
GET `/options?teamId`. Não existe DELETE público. Edição recebe o formulário
completo de conteúdo; `responsavelIds` ausente preserva vínculos. Criadora vem
somente da sessão. Payloads novos usam PT-BR; parâmetros HTTP preservam convenções
`teamId/status/priority/assigneeId/search/dueFrom/dueTo/archived/page/size`.

Todas as escritas de tarefas bloqueiam Fundadoras antes das leituras, compartilhando
a transação com gestão de equipes/usuárias. O cliente deve enviar `versao`; o SQL
faz compare-and-swap (`WHERE id=? AND version=?`) e incrementa a versão. Conflitos
retornam 409, sem reenvio automático. Ordenação global por status usa inteiros
contíguos e renumeração transacional das colunas afetadas. Posições alteradas
incrementam também a versão das demais tarefas; esse custo simples é adequado
ao volume atual e deve ser medido antes de escalar para ordens fracionárias.

Resumo é uma única agregação SQL das tarefas ativas atribuídas à sessão, dentro
do escopo. Atraso deriva de `LocalDate.now(America/Bahia)`, prazo anterior ao dia
e status diferente de CONCLUIDA. Auditoria registra `task.created`, `task.updated`,
`task.status_changed`, `task.assignees_changed` e `task.archived`, sem texto da
tarefa ou metadados sensíveis; ordenação isolada não gera evento.

| Tecnologia | Papel |
| --- | --- |
| Spring Web | Controllers REST e serialização dos DTOs |
| Spring Security | Autenticação, contexto de segurança, CSRF, CORS e proteção de rotas |
| Spring Session JDBC | Sessões persistidas no MySQL |
| Spring Data JPA | Repositories de acesso ao domínio |
| Hibernate | Mapeamento relacional e validação do schema |
| Flyway | Aplicação e validação de migrations |
| Bean Validation | Restrições dos dados recebidos pelos endpoints |
| Maven Wrapper | Build, execução de testes e empacotamento |
| Testcontainers | Infraestrutura real e isolada para integração |

## Fluxo de requisição

Projetos usam `ProjetoController`, `ProjetoService`, `PoliticaProjeto` e
`ProjetoRepository`, com entidades `Projeto` e `ResponsavelProjeto` validadas pelo
Hibernate. DTOs permanecem junto ao serviço/controller. Listagem paginada agrega
contagens de tarefas em SQL e carrega responsáveis em lote, sem consultas por card.

API `/api/v1/projects`: GET lista, GET `/{id}`, POST criação, PATCH `/{id}` edição,
POST `/{id}/archive`, GET `/options?teamId` e GET `/summary`. Filtros:
`teamId/status/responsibleId/search/startFrom/startTo/dueFrom/dueTo/archived/page/size`.
Os payloads usam `titulo/descricao/equipeId/responsavelIds/dataInicio/dataFim/status/versao`.
Criação sempre começa PLANEJADO; criadora vem da sessão. Status adicionais são
EM_ANDAMENTO, PAUSADO e CONCLUIDO. Arquivamento é um timestamp separado.

Conclusão e arquivamento exigem que toda tarefa ativa esteja CONCLUIDA; projeto
vazio pode ser concluído explicitamente. Progresso não conclui automaticamente.
Projeto concluído bloqueia novos vínculos/criação de tarefas e reabertura de
tarefas concluídas; reabra o projeto antes. PAUSADO não bloqueia movimentos.
Projeto/equipe arquivados são somente leitura. Equipe só muda antes do primeiro
vínculo, com revalidação explícita das responsáveis. Remover integrante responsável
por projeto não arquivado é bloqueado, tanto em Equipes como em Usuárias.

Tarefas recebem `projetoId` opcional no formulário completo de criação/edição;
null remove o vínculo. `projectId` filtra a lista HTTP. Vínculo exige mesma equipe,
projeto acessível e não arquivado; tarefas sem projeto continuam funcionando.
Todas as mutações compartilham o bloqueio de Fundadoras antes de ler invariantes.
Projetos também fazem CAS por versão; primeiro vínculo incrementa sua versão.
409 de versão é diferente de conclusão/arquivo pendente e vínculo incompatível.

Progresso é tarefas concluídas / tarefas ativas, arredondado ao inteiro mais
próximo; sem tarefas, percentual é null. Resumo conta projetos não arquivados no
escopo. Prazo próximo inclui data final de hoje até hoje + 7 dias, inclusive,
excluindo CONCLUIDO, no fuso America/Bahia. Auditoria registra `project.created`,
`project.updated`, `project.status_changed`, `project.responsibles_changed` e
`project.archived`; mudança de vínculo reutiliza `task.updated`.

A cadeia de segurança valida a requisição e recupera a sessão. O controller
recebe um DTO, aplica validação e chama os serviços necessários. Serviços
coordenam regras e transações; repositories acessam a persistência. A resposta
usa DTOs e erros controlados, sem SQL, credenciais ou stack traces.

Spring Security integra email/senha por `UserDetailsService` e
`DaoAuthenticationProvider`. O principal contém a identidade autenticada;
dados de autorização devem refletir o estado atual do banco.

## Persistência e operação

Hibernate executa com `ddl-auto=validate`: a aplicação não cria nem atualiza
tabelas por conta própria. Flyway aplica o schema antes da inicialização do JPA.
Operações que alteram dados relacionados devem ser transacionais.

`GET /api/v1/health` lê o banco e retorna apenas status; falha de acesso ao
banco produz resposta controlada. O bind local padrão é `127.0.0.1:8080`.

Consulte [autenticação](authentication.md), [RBAC](rbac.md),
[equipes](teams.md), [usuárias e convites](users-invites.md), [banco](database.md)
e [testes](../development/testing.md).

## Comentários e atividade contextual

O domínio `comentarios` contém entidade, repository, serviço e controller.
Acesso reutiliza buscarTarefa/buscarProjeto; leitura herda a permissão e o escopo
do recurso. Criar/editar/remover próprio exige tasks.comment ou projects.comment.
Só a autora edita. comments.moderate permite remover alheios no escopo autorizado;
em recurso/equipe arquivados apenas ADMIN/SUPER_ADMIN moderam. SUPPORT mantém
Projetos somente leitura mesmo com override. Conclusão não congela conversa.

GET/POST /api/v1/tasks/{id}/comments e /projects/{id}/comments;
PATCH /api/v1/comments/{id}; POST /api/v1/comments/{id}/remove.
Texto simples trim até 5000 caracteres; autora vem da sessão. Edição/remoção
usam versão e CAS, dentro do bloqueio transacional organizacional existente.
Removidos continuam na conversa, com conteúdo null e capacidades false.

GET /api/v1/tasks/{id}/activity e /projects/{id}/activity projetam audit_log.
Eventos Task/Project usam entity_type/entity_id; eventos Comment são relacionados
pelo registro persistido workspace_comment. Allowlist exclui ações técnicas,
eventos desconhecidos e associações sem vínculo exato. Não expõe metadata_json,
conteúdo nem identificador de quem removeu. Actor é carregado em JOIN.
Comentários e atividade têm paginação limitada, padrão 25; sem N+1.


## Anexos e armazenamento

Anexos ficam no domínio `anexos`: serviço valida acesso herdado, grants e arquivamento; repository persiste somente metadados; ArmazenamentoArquivo usa protocolo S3 com URLs temporárias. V11 adiciona attachment e três permissões. Ver configuração e consistência em [storage](../development/storage.md).

## Calendário

Calendário é uma projeção de Tarefas e Projetos e não possui persistência própria.
O domínio compacto `calendario` contém controller de leitura, serviço de projeção e repository.
`GET /api/v1/calendar` exige `from`/`to` ISO date-only, intervalo inclusivo de até 366 dias,
e aceita `teamId`, `types=TAREFA,PROJETO` e `responsibleId`. Retorna uma lista normalizada:
`id` (tipo:UUID), `tipo`, `recursoId`, `titulo`, `dataInicio`, `dataFim`, `equipe`, `status`,
`prioridade` nullable, `responsaveis`, `concluido`, `atrasado`.

Permissões tasks.view/projects.view habilitam cada fonte separadamente. Sem ambas, 403;
sem sessão, 401. Controller não replica RBAC. Consultas reutilizam montagem de escopo dos
repositories de Tarefas/Projetos e os tipos Acesso das políticas existentes.
SQL seleciona apenas campos usados na projeção, exclui recursos arquivados, filtra por
intervalo/equipe/responsável e busca responsáveis em lote (até quatro consultas das fontes,
sem N+1). Nenhuma descrição, comentário, anexo ou agregado de progresso é carregado.

Tarefa usa due_date. Projeto usa COALESCE(start_date,end_date) <= to e
COALESCE(end_date,start_date) >= from, com limites inclusivos. Sem datas não aparece;
uma única data corresponde a um dia. Atraso de Tarefa reutiliza PoliticaTarefa; Projeto exige
end_date anterior a hoje e status diferente de CONCLUIDO. Projeto com apenas início
não possui prazo final para ser marcado atrasado. Hoje segue America/Bahia, como Tarefas.
Tudo usa LocalDate, sem converter as datas de domínio para Instant.

## Notificações pessoais

O domínio notificacoes usa JDBC e a migration V12. Alterações relevantes de tarefas, projetos e comentários produzem avisos na mesma transação. A consulta revalida permissões, equipe e arquivo da fonte, e projeta seu título atual. Prazos são materializados com chave única ao consultar a central, sem scheduler externo. GET /api/v1/notifications é paginado; POST /{id}/read e /read-all exigem sessão e CSRF.

## Checklist de tarefas

ChecklistController, ChecklistService e ChecklistRepository ficam junto ao domínio tarefas. GET /tasks/{id}/checklist herda leitura da tarefa; POST, PATCH, order e remove herdam sua capacidade editar. Cada item tem versão; reordenação exige conjunto completo com versões. Escritas bloqueiam a tarefa, sem incrementar sua versão: checklist não modifica os campos da tarefa. Lista limitada a 200 itens de 500 caracteres para evitar abuso. Remoção lógica mantém autoria e timestamps; progresso é calculado.

## Reuniões e Talks

Domínio reunioes contém repository JDBC, serviço e controller em /api/v1/meetings: listagem paginada/busca/equipe/status/arquivo, detalhe, opções, criação, edição versionada e arquivo. Participantes são integrantes ativas da equipe; responsáveis também participam. Consultas fazem carregamento em lote. Escritas transacionais registram ações importantes na auditoria existente. Link aceita somente HTTP(S), host presente e sem credenciais.

Horários locais e zone id chegam à API; ela resolve os offsets e persiste Instant com zona preservada. Horário inexistente ou ambíguo em mudança de verão retorna 400, sem escolha silenciosa. Período exige fim posterior ao início. Calendário agrega REUNIAO com inicioEm/fimEm/zona, mantendo null nesses campos para fontes date-only. Encontro aparece nas datas de seu fuso de origem; cancelados/arquivados são excluídos.

## Eventos da comunidade

Domínio eventos concentra API /api/v1/events, validação e JDBC; versões, escopo atual, responsáveis ativas, arquivo e auditoria existentes. HorarioEncontro resolve horários locais de reuniões/eventos sem escolher silenciosamente offsets de DST.

## Comunicação e Conteúdo

O domínio conteudos atende /api/v1/content com JDBC, escopo por equipe, versões e auditoria. Novos vínculos de fonte exigem permissão de leitura, atividade e mesma equipe; vínculos históricos são preservados.
