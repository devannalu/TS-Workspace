# Fase 19 — Notas contextuais

Checkpoint inicial: 90104ec9e46b0537e95eafe9ac2c501e1fd4b3c0.

## Entrega

V18 adiciona workspace_note com um único vínculo obrigatório entre Projeto, Evento, Parceria e Reunião, validado por CHECK e FKs. Título, texto simples, autoria, versão, criação/atualização e remoção lógica; índices por contexto, remoção e atualização. Não cria módulo global ou editor rich-text.

Nota documenta informação do recurso; comentário mantém conversa cronológica. Campos resumidos existentes de resultados/notas operacionais permanecem como dados do recurso. Notas contextuais acrescentam autoria própria e documentação independente, ordenada pela atualização.

Nenhuma permission nova: o serviço de origem valida leitura/escopo e fornece capacidade de edição atual. Criar exige edição do recurso ativo/equipe ativa; editar/remover também exige autoria própria. Administradoras não sobrescrevem notas de outra autora. Revogação de leitura, membership ou edição vale na próxima requisição. Arquivos ficam somente leitura. Removidas não aparecem na lista nem expõem texto pela API.

GET/POST /api/v1/{projects|events|partnerships|meetings}/{id}/notes; PATCH /api/v1/notes/{id}; POST /api/v1/notes/{id}/remove. Lista paginada até 100; título 200 e texto 5000 caracteres. Versão obrigatória evita sobrescritas concorrentes. AuditLog existente registra note.created/updated/removed, sem título/texto.

UI contextual nos quatro detalhes, com criação, edição própria, remoção confirmada, loading/error/empty e paginação. Projeto preserva abas de Comentários/Anexos/Atividade. Não há página ou item de navegação global de Notas.

## Verificação

Oito testes MySQL/Testcontainers cobrem quatro fontes, autoria/capacidade atual, escopo/leitura, arquivo de recurso/equipe, versões e remoção lógica, limites/constraint, HTTP/CSRF/401/403/404/409 e concorrência. Ajustada expectativa JDBC para identificar a constraint violada sem depender de subclassificação do driver.

Sete testes frontend cobrem texto seguro/autoria, criação, edição, confirmação, readonly, conflito com rascunho preservado e retry. Detalhes existentes usam mock do componente contextual para manter testes de cada módulo isolados.

HTTP real aprovou os quatro contextos, autoria, readonly, versão, 401/403/404/409 e remoção lógica. Navegador aprovou criação/edição/remoção em Projeto, contexto de Evento/Parceria, foco visível e cinco larguras sem overflow horizontal. Botões visuais compactos mantêm nomes acessíveis específicos da nota.

Checkpoint: commit `feat: adiciona notas contextuais`; SHA disponível no histórico Git deste documento.

Package completo: 299 testes aprovados, zero failures/errors/skips, JAR gerado. Frontend: typecheck/lint, 169 testes e build aprovados. V18 aplicada, Hibernate validate e health 200; MySQL/Garage healthy.

Suporte somente leitura confirmado no navegador, console sem erros. Fixtures removidas; hashes e quantidades oficiais idênticos ao baseline. Secret scan sem achados e arquivos locais sensíveis ignorados.
