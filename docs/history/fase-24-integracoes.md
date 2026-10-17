# Fase 24 — Integrações e consolidação

A revisão do roadmap confirmou S3/R2, SMTP e calendário como integrações concretas. S3 permanece por ArmazenamentoArquivo com Garage real validado e R2 configurável; SMTP permanece no ServicoEmail validado na Fase 23. Não se inventam conectores OAuth, APIs sociais ou contas externas.

## Entrega

GET /api/v1/calendar/export utiliza a projeção e filtros existentes, com sessão Java e permissões atuais. Gera ICS privado de Tarefas, Projetos, Reuniões, Eventos e Conteúdo. Datas civis não mudam de dia; fim date-only é exclusivo no arquivo. Horários reais são UTC, preservando a zona original como extensão. UID estável, CRLF, escape de texto e dobra UTF-8 de 75 octetos preservam interoperabilidade, inclusive emoji.

Exportação limita período a 366 dias e 5.000 itens, com erro explícito em excesso. Não persiste outro calendário nem trunca resultados. Inclui título/equipe/datas/link interno; não inclui descrição privada, corpo de nota, emails, token ou sessão. Não há feed público ou sincronização automática.

O botão Exportar agenda (ICS) utiliza o período e os filtros atuais. O cliente HTTP central aceita leitura binária e mantém cookies, erros, 401/403 e no-store, sem duplicar tratamento de sessão/CSRF. URL local do Blob é liberada após iniciar o download.

A atividade de Projetos agora inclui criação, edição e remoção de notas, usando audit_log e vínculo exato no workspace_note. Exibe somente autora, ação e instante, inclusive quando a nota foi removida; não lê seu corpo ou informações de outro contexto.

Sem migration, permission nova ou dependência adicional.

## Verificação

Package Java aprovado: 342 testes, nenhum failure/error/skip. Frontend: typecheck, lint, 206 testes e build aprovados. O encerramento dos contextos de teste ainda produz tentativas de conexão após o fechamento dos containers; a correção desse ciclo de vida pertence à revisão final da Fase 25.

Cinco unitários de ICS cobrem datas/fim exclusivo, UTC em Bahia/Lisboa/Kiritimati, escape/injeção, caracteres UTF-8, UID, links e limite sem truncamento. Dois testes integrados de calendário cobrem arquivo/headers, filtros/escopo/membership, 401/403/400; um integrado de notas cobre trilha e privacidade. Suíte de comentários existente passou após a consolidação.

Frontend cobre arquivo binário com cookie, erro JSON seguro, exportação com filtros/URL temporária, falha sem download e atividade de nota. HTTP real aprovou cinco fontes, datas civis, horários Bahia/Lisboa, filtro de responsável, escopo, erro de período e atividade sem texto privado.

Navegador baixou ts-workspace.ics válido com seis itens da fixture no escopo global, reutilizou detalhe e atividade de projeto, e aprovou 390/768/1024/1280/1440 px sem overflow. Download temporário e fixtures removidos, com hashes/quantidades oficiais preservados. PUT/HEAD/GET, conteúdo, privacidade, CORS e DELETE foram revalidados no Garage com limpeza do objeto de verificação.

Referências e contratos estão em [integrações](../architecture/integrations.md). Conta/chaves R2 e SMTP reais continuam pendências externas de implantação, sem bloquear a configuração local ou a revisão final de produção.

Checkpoint: feat: consolida integracoes e exporta calendario; SHA no histórico Git deste documento.
