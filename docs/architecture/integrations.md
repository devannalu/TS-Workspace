# Contratos de integração

As integrações atendem capacidades existentes, com Java como autoridade. Não há conectores OAuth, publicação social ou sincronização automática com serviços de calendário.

| Integração | Contrato e acesso | Operação |
| --- | --- | --- |
| S3 compatível | Metadados/autorização no Java; PUT/GET temporários por chave controlada, sem cookie de sessão no storage. | Garage local validado; R2/provedor real exige conta/chaves e CORS do domínio de produção. |
| SMTP | Convite depois de commit e confirmação após aceite; token só no corpo da mensagem e na resposta única autorizada. | Captura Mailpit local; provedor, TLS e remetente reais são configuração externa. |
| Calendário | Projeção de cinco fontes com permissões e equipe atuais; GET /api/v1/calendar/export gera snapshot ICS autenticado. | Arquivo privado para importação manual; não cria assinatura pública nem sincroniza alterações posteriores. |
| Atividade de Projeto | Eventos existentes de notas entram na mesma trilha contextual por vínculo exato. | Exibe autora/ação/instante; não recupera título/corpo de notas removidas nem conteúdo de outro projeto. |

## Exportação ICS

Usa os mesmos filtros de período, equipe, tipos e responsável do calendário. Intervalo máximo de 366 dias e limite de 5.000 itens, sem truncamento silencioso. Ultrapassar exige reduzir período ou filtrar. Resposta text/calendar UTF-8, attachment e no-store.

Datas civis usam VALUE=DATE, com fim exclusivo; prazos e períodos não bloqueiam a agenda importada. Encontros usam instantes UTC e preservam a zona de origem em extensão própria, sem horário flutuante. UID permanece estável por tipo/recurso. Texto é escapado e dobrado em até 75 octetos UTF-8, sem quebrar caracteres. A cópia contém título, equipe, datas e link interno; sem email de participantes, nota, descrição privada, token ou sessão.

Exportar exige sessão válida e acesso atual às fontes. O arquivo baixado é uma cópia sob controle da usuária; permissões e alterações futuras continuam sendo verificadas ao abrir os links no Workspace. Não há feed anônimo ou token permanente de calendário.

## Configuração operacional

Veja [storage](../development/storage.md) e [email](../development/email.md). URLs S3 não podem ser reescritas depois de assinadas; o endpoint configurado precisa ser acessível ao navegador. FRONTEND_ORIGIN deve ser a origem oficial que compõe os links de convite e calendário. Nunca incorporar senha, token de provedor ou cookie no frontend.

Google/Microsoft Calendar, Slack e outros conectores permanecem evolução futura dependente de requisito de produto explícito. Não são funcionalidades incompletas deste escopo.

Referência de interoperabilidade: [RFC 5545 — iCalendar](https://www.rfc-editor.org/rfc/rfc5545.html).
