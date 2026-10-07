# Fase 13 — Notificações

Checkpoint inicial: 3d21dc7b2d494a6817e76472dfe450266e8c1ae8.

## Entrega

Central pessoal no header, contador de não lidas, paginação, leitura individual e leitura de todos os avisos. Links reutilizam detalhes de tarefas/projetos. Polling de 60 segundos somente com página visível, sem nova dependência, websocket ou estado global.

V12 cria notification com destinatária, origem, motivo e leitura. Chave única impede repetição do mesmo evento. Atribuições novas excluem a autora; comentários e mudanças relevantes avisam responsáveis. Prazos próximos e atrasados são gerados na consulta, uma vez por recurso/data/motivo. Não há envio de email nesta fase.

GET /api/v1/notifications?page&size permite até 100 itens. POST /api/v1/notifications/{id}/read e POST /api/v1/notifications/read-all exigem CSRF. Sessão obrigatória; tentativa de leitura de aviso de outra pessoa retorna 404. Título vem da fonte atual, sem conteúdo de comentário persistido. Permissões e participação na equipe são revalidadas em cada consulta, excluindo recursos arquivados. Nenhuma permission key nova.

## Verificação

Backend: 247 testes aprovados em test e package, JAR executável gerado, zero failures/errors/skips. Seis testes novos verificam atribuição, autoria, comentários, mudanças, escopo revogado, prazo/atraso, idempotência, paginação, sessão e CSRF com MySQL real via Testcontainers.

Frontend: typecheck, lint, 129 testes e build aprovados. Seis testes novos cobrem contador, links, leitura individual/em lote, vazio, retry e paginação.

HTTP real conferiu sessão, autoria, destinatárias, isolamento, leitura idempotente, paginação, limites e códigos 200/204/400/401/404. Navegador conferiu login, sino, contador, leitura, navegação aos dois detalhes e foco visível por teclado. Console sem erros relevantes; estado vazio confirmado após limpeza. Central sem overflow da página em 1440/1280/1024/768/390px.

Fixtures exclusivas removidas; hashes e quantidades dos dados oficiais iguais ao baseline. MySQL e storage locais preservados. Expectativas de versão e quantidade no teste de upgrade foram atualizadas para V12; migrations históricas permanecem intactas.
