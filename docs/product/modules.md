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
| Tarefas | IMPLEMENTADO | Kanban em quatro etapas, prioridades, prazo, equipe, múltiplas responsáveis, criação/edição/detalhe, movimentação e ordenação, filtros e arquivamento. Escopo por equipe e acesso. Comentários, checklist e arquivos permanecem futuros. |
| Projetos | IMPLEMENTADO | Objetivo, equipe, responsáveis, período e quatro status; cards, filtros e detalhe com progresso derivado das tarefas ativas. Conclusão e arquivo exigem ausência de pendências. Tarefas podem permanecer sem projeto. |
| Eventos | PLANEJADO | Organização da operação e das atividades de eventos da comunidade. |
| Comunicação / Conteúdo | PLANEJADO | Organização de conteúdo e campanhas, articulando as frentes de Comunicação. |
| Parcerias | PLANEJADO | Acompanhamento de parcerias e atividades relacionadas. |
| Calendário | PLANEJADO | Visão temporal das atividades e compromissos do Workspace. |
| Talks / Reuniões | PLANEJADO | Organização de encontros, informações e atividades decorrentes. |
| Arquivos | PLANEJADO | Organização de materiais associados ao trabalho. |
| Chat | PLANEJADO | Comunicação interna contextual ao Workspace. |
| Notificações | PLANEJADO | Avisos relevantes sobre atividades e mudanças. |
| Auditoria | PLANEJADO — módulo completo | Registros administrativos existem no Java (convites, roles, status e equipes de usuárias). Consulta administrativa completa ainda não está entregue. |
| Busca | PLANEJADO | Busca global pelos conteúdos e módulos acessíveis à usuária. |

Notas e comentários também fazem parte da evolução de colaboração.
Itens de navegação devem ser apresentados quando houver funcionalidade real
e a usuária tiver acesso, evitando telas vazias de “em breve”.

Veja [roadmap](roadmap.md) e [regras de autorização](../architecture/rbac.md).
