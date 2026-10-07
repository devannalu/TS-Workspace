# Fase 16 — Eventos da comunidade

Checkpoint inicial: 5296cb70dda0001aa832cbe98c9aaa06ee01d35e.

## Entrega

V15 adiciona community_event e community_event_responsible; quatro permissões events.view/create/edit/archive e 12 grants aditivos. SUPER_ADMIN/ADMIN globais, SUPERVISOR por equipe com criação/edição, SUPPORT por equipe somente leitura. Arquivo administrativo; escopo atual e versões revalidados no servidor.

Nome, descrição, formato PRESENCIAL/ONLINE/HIBRIDO, status PLANEJADO/CONFIRMADO/EM_ANDAMENTO/CONCLUIDO/CANCELADO, equipe, responsáveis ativas, início/fim, zona, local/link seguro, notas operacionais, autoria, versão, timestamps e arquivo. Sem ticketing, pagamentos ou inscrições. Operação continua usando Tarefas/Projetos existentes.

GET /api/v1/events paginado com busca literal, equipe/status/arquivo; GET /options e /{id}; POST criação, PATCH /{id}, POST /{id}/archive. Páginas até 100 registros; até 50 responsáveis e intervalo máximo de 30 dias. AuditLog existente registra criação, atualização, status e arquivo sem conteúdo operacional.

Horário local é resolvido pelo helper HorarioEncontro, agora compartilhado com reuniões. Instant e zona preservados; horários ambíguos/inexistentes de DST explicitamente rejeitados. EVENTO é quarta fonte do calendário; cancelados e arquivados ficam fora da projeção, mas permanecem consultáveis. Tarefas/Projetos continuam date-only.

## Verificação

Dez testes novos em MySQL/Testcontainers cobrem fusos, DST, CRUD/versão/arquivo, escopo/RBAC, responsáveis/rollback, calendário misto, paginação/seed, concorrência, HTTP/CSRF/URL segura e todos os status/cancelamento. Testes direcionados de eventos, reuniões e calendário: 28 aprovados.

Frontend: 149 testes, typecheck, lint e build aprovados. HTTP real confirmou 401/403/409/400, CSRF, Bahia/Lisboa, responsáveis elegíveis, calendário e arquivo. V15 aplicada com Hibernate validate e inicialização saudável. Navegador aprovou criação, status/notas, arquivo com confirmação, abertura pelo calendário e suporte somente leitura. Foco visível por teclado, console sem erros e ausência de overflow em 1440/1280/1024/768/390px. Fixtures removidas; hashes e quantidades oficiais idênticos ao baseline. As rodadas completas de test e package aprovaram 273 testes, sem failures/errors/skips.
