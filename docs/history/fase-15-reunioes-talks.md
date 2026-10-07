# Fase 15 — Reuniões e Talks

Checkpoint inicial: 21b039ceec13b028227af5f28ab734b116434688.

## Entrega

V14 adiciona meeting e meeting_participant; quatro keys meetings.view/create/edit/archive e 12 grants incrementais. SUPER_ADMIN/ADMIN globais, SUPERVISOR por equipe com escrita, SUPPORT por equipe somente leitura. Sem novas dependências, videoconferência automática ou sistema paralelo de tarefas.

Domínio compacto: pauta, tipo REUNIAO/TALK, status AGENDADA/REALIZADA/CANCELADA, equipe, participantes, responsáveis, início/fim, fuso, local, link seguro, resultados, autoria, timestamps, versão e arquivo. Responsáveis participam do encontro. Escritas usam auditoria existente e transação; versões evitam sobrescrita.

API /api/v1/meetings oferece GET paginado, options, detalhe; POST criação, PATCH edição e POST /{id}/archive. Filtros por busca literal, equipe, status e arquivo. Limite 100 por página e 50 participantes. Links HTTP(S) sem credenciais; texto tratado como texto simples.

Horários locais e zona resolvidos no servidor, Instant persistido com zona preservada. Bahia e Lisboa testadas; gaps/overlaps de horário de verão rejeitados explicitamente. Calendário REUNIAO acrescenta inicioEm/fimEm/zona sem modificar a semântica date-only de TAREFA/PROJETO. Interseção usa datas locais do encontro; cancelados e arquivados não aparecem. Detalhe reutilizado.

## Verificação

Backend: 263 testes aprovados em test e package, sem failures, errors ou skips. Nove testes integrados novos aprovados: horários/fusos, DST, CRUD/arquivo/versão, participantes/rollback, escopo/RBAC, calendário misto, paginação/seed, concorrência e HTTP/CSRF/URL segura. MySQL real via Testcontainers.

Frontend: typecheck, lint, 142 testes e build aprovados. Sete testes novos cobrem horários, links, leitura, edição sem conversão ingênua, arquivo, lista, vazio/retry, conflito e texto seguro.

HTTP real confirmou Bahia/Lisboa, calendário com horários, acesso somente leitura e isolamento 403, conflito 409, validação 400, arquivo e paginação. Backend iniciou em V14 com Hibernate validate e health 200.

Navegador aprovou criação, edição de resultados/status, arquivo com confirmação e abertura pelo calendário. SUPPORT confirmou somente leitura sem controles de mutação. Foco visível por teclado; sem overflow em 1440/1280/1024/768/390px; console sem erros. Fixtures removidas com hashes e quantidades oficiais preservados. Expectativas antigas de catálogo e ausência de fontes foram atualizadas para as quatro keys e a terceira fonte, preservando o baseline histórico de RBAC.
