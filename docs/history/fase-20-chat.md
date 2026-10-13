# Fase 20 — Chat interno

Checkpoint inicial: da93f9fa75ebf8991a90d105491234f1f7d4cf65.

## Entrega

V19 adiciona chat_message: canal geral (team_id NULL) ou equipe existente, autoria, texto simples até 2000 caracteres, versão, criação/atualização e remoção lógica com moderadora. FKs e índice por equipe/criação/id. Canais são derivados das equipes, sem duplicar entidades. Equipes arquivadas preservam histórico somente leitura.

Permissions chat.view/send/moderate; dez grants aditivos. Todos os perfis recebem leitura/envio; somente ADMIN/SUPER_ADMIN recebem moderação por padrão. Overrides continuam válidos. Geral exige chat.view; canais de equipe revalidam membership atual, salvo acesso global administrativo. Editar exige autoria e envio atual; remover exige autoria/envio ou moderação, no mesmo escopo. Inactive revoga sessão.

API /api/v1/chat/channels; GET/POST /chat/channels/{canal}/messages; PATCH /chat/messages/{id}; POST /chat/messages/{id}/remove. Cursor before é UUID de mensagem do mesmo canal; ordenação criação/id mantém paginação estável com novas mensagens. Até 100 por requisição, 50 na UI. Removidas mantêm marcador de autoria/data, sem texto na API.

Página /chat reutiliza Shell/Design System. Rascunhos locais por canal, histórico, texto seguro, edição própria, remoção confirmada e readonly. Consulta a cada três segundos, somente com página visível e nas mensagens recentes; histórico pausa polling. Requisições não se sobrepõem; 401/403 limpa conteúdo e interrompe consulta até ação explícita. Sem DM, threads, reações, uploads ou infraestrutura distribuída.

AuditLog existente registra edição/remoção, sem texto. Envio não duplica o registro da própria conversa na auditoria nem gera notificações indiscriminadas.

## Correção temporal

O navegador detectou timestamps adiantados em três horas: Connector/J interpretava TIMESTAMP do MySQL UTC no fuso local. Hikari agora fixa connectionTimeZone +00:00, forceConnectionTimeZoneToSession e preserveInstants; Hibernate usa UTC. Nenhuma migration histórica ou dado oficial foi alterado, e não havia registros operacionais oficiais afetados. Date-only e zona dos encontros permanecem preservados.

A configuração segue o [processamento temporal oficial do Connector/J](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-datetime-types-processing.html). Teste compara Instant atual e UNIX_TIMESTAMP do banco sob Bahia, Lisboa e Kiritimati. HTTP real confirmou o mesmo instante na gravação, leitura e atualização.

## Verificação

Onze testes integrados novos cobrem canais/escopo, cursor, autoria, versão/remoção, moderação, membership revogada, arquivo, override, limites/auditoria/seed, HTTP/CSRF/401/403/404/409/inactive, concorrência e UTC. Direcionados temporais: 57 aprovados. Package completo após a correção: 310 aprovados, zero failures/errors/skips e JAR gerado.

Frontend: 184 testes, typecheck, lint e build aprovados. Dez testes de Chat cobrem interação, rascunhos, histórico, readonly, conflito, remoção, 403 e pausa com página oculta. Teste da navegação cobre ícone/rota; tratamento central preserva conflitos humanos de Chat/Notas/Parcerias e oculta detalhes técnicos desconhecidos.

HTTP real aprovou geral/equipe, 52 mensagens temporárias para paginação, autoria, moderação, overrides e inactive. Navegador aprovou envio/edição/remoção, histórico, rascunhos separados, readonly após revogação, foco visível e 1440/1280/1024/768/390px sem overflow. Console sem erros após reinício controlado do backend.

Checkpoint: commit `feat: adiciona chat interno`; SHA disponível no histórico Git deste documento.

Fixtures removidas; hashes e quantidades oficiais idênticos ao baseline, inclusive após correção de UTC. Secret scan sem achados; arquivos sensíveis e verificações locais ignorados. MySQL/Garage healthy e backend health 200.
