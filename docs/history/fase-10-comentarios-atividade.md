# Fase 10 — Comentários e atividade contextual

## Inspeção da auditoria antes da V10

`audit_log` possui id, actor_id nullable (FK para app_user), action,
entity_type, entity_id, metadata_json nullable e created_at. Não há entidade
JPA: AuditoriaRepository grava no JdbcTemplate dentro da transação do caso de
uso. Metadados livres são deliberadamente proibidos. Tarefas e Projetos já
registram seus IDs com os tipos Task e Project, de forma confiável.

A atividade reutilizará esses eventos e os eventos Comment ligados pelo registro
workspace_comment. Não é necessário alterar audit_log nem criar tabela de
atividade. Eventos desconhecidos ou sem vínculo exato serão excluídos.

## Implementação

V10 cria workspace_comment com exatamente um recurso, FKs, índices e conteúdo
simples até 5000 caracteres. Versão com CAS; remoção lógica registra autora da
remoção e data, sem expor esses detalhes na API. Conteúdo removido é null.
Somente a própria autora edita; moderação remove alheios. Recurso/equipe
arquivados congelam colaboração normal, mantendo moderação administrativa.
Conclusão permite conversa. SUPPORT não comenta Projeto.

Três permissões adicionadas: tasks.comment, projects.comment e comments.moderate.
SUPER_ADMIN/ADMIN recebem as três, SUPERVISOR as duas de comentário e SUPPORT
somente tasks.comment. Grants anteriores preservados; 27 keys e 73 grants.

Atividade reutiliza auditoria sem mudar seu schema: Task/Project por ID exato e
Comment pela FK do registro. Allowlist de tipos; resposta limitada a id, tipo,
ator e data, sem texto/metadata. Eventos sem vínculo confiável são excluídos.
Auditoria comment.created/updated/removed não registra conteúdo.

UI reutiliza diálogos existentes com abas, edição inline, composer, confirmação
acessível e placeholders. Paginação explícita de 25 itens; conversa cronológica
e atividade mais recente primeiro. Sem polling, localStorage, HTML/Markdown,
realtime, mentions, notificações, anexos ou feed global.

## Validações

- Maven test e package: 221 testes, zero failure/error/skip; JAR produzido.
- Frontend: 86 testes, typecheck, lint sem warnings e build aprovados.
- HTTP real: login, comentários de Tarefa/Projeto, edição própria, moderação,
  exclusão lógica, placeholder, atividade sem conteúdo, 403 e conflito 409;
  SUPPORT sem projects.comment; logout 204 com sessão encerrada.
- MySQL Java healthy; Flyway V10; Hibernate validate; health/CSRF HTTP 200.
- Navegador: criação, edição inline, remoção e atividade; conflito real preserva
  rascunho e exige refresh explícito. Larguras 1440/1280/768/390 sem overflow.
  Tabs com teclado; foco de edição e confirmação; Escape no diálogo.

A verificação corrigiu a expectativa do teste para a exceção de CHECK recebida
do driver MySQL, o estado inicial de carregamento, codificação de mensagens e
foco após edição/remoção. Não há dependência nova. Cinco HIGH existentes na
cadeia de lint permanecem; nenhuma vulnerabilidade runtime nova.

SUPPORT no navegador confirmou Projeto sem composer/ações e Tarefa arquivada
somente leitura. Estado vazio e falha real de conexão preservando rascunho
foram conferidos; o backend foi reiniciado com o mecanismo documentado.

Cleanup concluído: duas tarefas, um projeto e três usuárias auxiliares, comentários
e auditoria temporária removidos. Hashes e quantidades oficiais idênticos ao
baseline; nenhum volume removido e nenhum dado oficial alterado. Secret scan sem
achados; arquivos locais sensíveis e evidências permanecem ignorados.
