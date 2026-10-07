# Módulos

**IMPLEMENTADO** indica funcionalidade presente no produto, no escopo descrito.
**PLANEJADO** indica intenção de produto, sem promessa de disponibilidade.
Ter uma entidade ou uma permissão reservada não torna um módulo completo.

Os módulos de acesso e gestão são atendidos oficialmente pelo Spring Boot.
O Next.js apresenta os dados pelas APIs Auth, RBAC, Teams, Users, Invites, Tasks e Projects.

| Módulo | Estado | Escopo e limites |
| --- | --- | --- |
| Dashboard / Início | IMPLEMENTADO | Equipes pessoais, usuárias ativas, convites pendentes, tarefas atribuídas e projetos no escopo: ativos, em andamento e com prazo próximo. |
| Usuárias | IMPLEMENTADO | Lista paginada com busca e filtros de status/perfil/equipe; permite editar cargo, role e equipes conforme permissão. Protege a última Super Admin ativa e revoga sessões na inativação. |
| Convites | IMPLEMENTADO | Criação, cancelamento, expiração e aceite de uso único. O link aparece uma vez no dialog, com cópia e feedback; envio automático por SMTP é planejado. |
| Equipes | IMPLEMENTADO | Hierarquia visual, criação, edição, arquivamento com confirmação e busca de integrantes; uma usuária pode participar de várias equipes. Proteções contra ciclos e alterações indevidas da raiz. |
| Tarefas | IMPLEMENTADO | Kanban em quatro etapas, prioridades, prazo, equipe, múltiplas responsáveis, criação/edição/detalhe, movimentação e ordenação, filtros e arquivamento. Escopo por equipe e acesso. Comentários, anexos, atividade e checklist contextual no detalhe, com progresso, ordem, conclusão e versões. |
| Projetos | IMPLEMENTADO | Objetivo, equipe, responsáveis, período e quatro status; cards, filtros e detalhe com progresso derivado das tarefas ativas. Conclusão e arquivo exigem ausência de pendências. Tarefas podem permanecer sem projeto; comentários, anexos e atividade contextual nos detalhes. |
| Eventos | IMPLEMENTADO | Nome, descrição, formato, equipe/responsáveis, horários com fuso, cinco status, local/link e notas operacionais. CRUD, versões, arquivo, filtros/paginação e calendário; sem ticketing ou inscrições. |
| Comunicação / Conteúdo | IMPLEMENTADO | Peças com briefing, canal, formato, seis status, responsável/equipe, publicação date-only, vínculos seguros com Evento/Projeto, versões e arquivo. Cards, filtros e calendário; sem CMS público ou publicação automática. |
| Parcerias | PLANEJADO | Acompanhamento de parcerias e atividades relacionadas. |
| Calendário | IMPLEMENTADO | Projeção dos prazos de Tarefas e períodos de Projetos, com mês, semana, agenda, filtros e detalhes reutilizados; reuniões/eventos com horário/fuso e publicação planejada de conteúdo date-only e acesso herdado das fontes; sem persistência própria. |
| Talks / Reuniões | IMPLEMENTADO | Pauta, tipo, status, equipe, participantes/responsáveis, horários com fuso, local/link, resultados, versões e arquivo. Integra o calendário com horários; sem criação automática de videoconferência. |
| Anexos | IMPLEMENTADO | Upload contextual em Tarefas/Projetos, storage S3 privado, download temporário e remoção autorizada; sem módulo global de arquivos ou preview. |
| Chat | PLANEJADO | Comunicação interna contextual ao Workspace. |
| Notificações | IMPLEMENTADO | Central pessoal paginada, contador e leitura individual/em lote; atribuições, comentários, status e prazos de tarefas/projetos. Acesso atual revalidado; atualização a cada 60 segundos com a página visível. |
| Auditoria | PLANEJADO — módulo completo | Registros administrativos existem no Java (convites, roles, status e equipes de usuárias). Consulta administrativa completa ainda não está entregue. |
| Busca | PLANEJADO | Busca global pelos conteúdos e módulos acessíveis à usuária. |

Comentários já integram Tarefas e Projetos; notas permanecem futuras.
Itens de navegação devem ser apresentados quando houver funcionalidade real
e a usuária tiver acesso, evitando telas vazias de “em breve”.

Veja [roadmap](roadmap.md) e [regras de autorização](../architecture/rbac.md).


## Anexos e armazenamento

Anexos contextuais de tarefas e projetos permitem PDF, imagens e texto até 10 MiB. Leitura herda acesso ao recurso. Escrita usa tasks.attach/projects.attach; moderação usa attachments.remove. Arquivamento mantém leitura e bloqueia upload normal.
