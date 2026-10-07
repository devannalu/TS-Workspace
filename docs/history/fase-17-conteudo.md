# Fase 17 — Comunicação e Conteúdo

Checkpoint inicial: 67715e2f2efb1d9dda0aa0004bf67be10a3c55e6.

## Entrega

V16 adiciona communication_content: título, briefing, canal, formato, status, equipe, responsável, data planejada de publicação (DATE), vínculos opcionais com Evento/Projeto, autoria, versão, timestamps e arquivo. FKs e checks; índices de equipe/status, responsável e publicação. Sem CMS público, redes sociais automáticas ou dependências novas.

Quatro permissions content.view/create/edit/archive; 12 grants aditivos. ADMIN/SUPER_ADMIN globais; SUPERVISOR cria/edita nas equipes atuais; SUPPORT lê somente. Auditoria existente registra criação, alteração, status e arquivo, sem briefing.

API /api/v1/content com lista paginada/busca literal/status/equipe/arquivo, options, detalhe, POST criação, PATCH edição e POST arquivo. Até 100 por página. Novos vínculos exigem leitura da fonte, mesma equipe e recurso ativo. Relações históricas permanecem ao editar outros campos; abrir a fonte continua sujeito à autorização própria.

CONTEUDO é quinta fonte do calendário. A publicação é date-only e não sofre conversão de fuso. Sem data/arquivado fica fora da projeção. Página /conteudos usa cards, filtros e detalhe/formulário existentes no Design System, com fluxo IDEIA → PLANEJADO → EM_PRODUCAO → EM_REVISAO → APROVADO → PUBLICADO. A usuária escolhe o status, sem transições artificiais obrigatórias.

## Verificação

Nove testes integrados novos cobrem fluxo/date-only, versão/arquivo, escopo/RBAC, responsável ativa/rollback, vínculos/permissão/histórico, paginação/seed, calendário misto, HTTP/CSRF/401/403/404/409 e concorrência. Direcionados de conteúdo/calendário/eventos: 28 aprovados. Frontend: typecheck, lint, 155 testes e build aprovados.

HTTP real aprovou vínculos, data-only, status, paginação, arquivo, 400/401/403/404/409. V16 aplicada, Hibernate validate e health 200. Navegador aprovou criação, briefing/status, arquivo com confirmação, vínculos que abrem Evento/Projeto, detalhe pelo calendário e suporte somente leitura. Teclado/foco visível, console sem erros e ausência de overflow em 1440/1280/1024/768/390px. Fixtures removidas; hashes e quantidades oficiais idênticos ao baseline.

Package completo: 282 testes aprovados, zero failures/errors/skips; JAR gerado.
