# Fase 18 — Parcerias

Checkpoint inicial: cd2e2d642df69049d7649d30b173f3c769c27675.

## Entrega

V17 adiciona partnership com organização, descrição, cinco status, equipe, responsável, contato principal, email/telefone opcionais, próxima ação e data DATE, observações, vínculos opcionais com Evento/Projeto, autoria, versão, timestamps e arquivo. FKs, check de status e índices por equipe/status, responsável e próxima data. Sem CRM complexo ou dependências novas.

Permissions partnerships.view/create/edit/archive e 12 grants aditivos. SUPER_ADMIN/ADMIN globais; SUPERVISOR cria/edita nas equipes atuais; SUPPORT somente leitura. Auditoria registra ações e mudança de status sem dados de contato, descrição ou observações.

API /api/v1/partnerships: lista paginada/busca literal/equipe/status/arquivo, options, detalhe, POST, PATCH e POST archive. Novos vínculos exigem recurso ativo, mesma equipe e leitura da fonte. Vínculos históricos permanecem ao editar outros campos. Responsável deve ser integrante ativa da equipe. Versões evitam sobrescrita silenciosa.

Página /parcerias reutiliza Shell/Design System, cards, filtros, diálogo de detalhe/formulário e confirmação de arquivo. Próxima ação permanece date-only. Calendário conserva suas cinco fontes; este módulo não introduz projeção temporal adicional.

## Verificação

Nove testes integrados novos cobrem pipeline/date-only, versão/arquivo, escopo, responsável, vínculos históricos, limites/busca/seed, contato e auditoria, HTTP/CSRF/401/403/404/409 e concorrência. Seis testes frontend cobrem interação, links, readonly, edição, confirmação, conflito e retry.

HTTP real aprovou criação/edição/arquivo, vínculos, paginação, date-only e 400/401/403/404/409. V17 aplicada com Hibernate validate e health 200. Navegador aprovou criação, alteração para CONFIRMADA e arquivo; detalhe em 1440/1280/1024/768/390px sem overflow horizontal. Corrigido ícone ausente na navegação identificado pelo navegador.

Package completo: 291 testes aprovados, zero failures/errors/skips, JAR gerado. Frontend: typecheck, lint, 162 testes e build aprovados. Teste adicional protege o ícone da navegação. Suporte validado somente leitura; vínculo abre Evento e teclado mantém foco visível. Fixtures removidas com hashes/quantidades oficiais idênticos ao baseline. Secret scan sem achados; arquivos sensíveis ignorados. Na retomada de 13/10/2026, MySQL healthy e backend health 200, sem nova migration ou alteração funcional.

Checkpoint: commit `feat: adiciona gestao de parcerias`; SHA disponível no histórico Git deste documento.
