# Módulos

**IMPLEMENTADO** indica funcionalidade presente no produto, no escopo descrito.
**PLANEJADO** indica intenção de produto, sem promessa de disponibilidade.
Ter uma entidade ou uma permissão reservada não torna um módulo completo.

Os módulos de acesso e gestão são atendidos oficialmente pelo Spring Boot.
O Next.js apresenta os dados pelas APIs Auth, RBAC, Teams, Users e Invites.

| Módulo | Estado | Escopo e limites |
| --- | --- | --- |
| Dashboard / Início | IMPLEMENTADO | Equipes pessoais, total de usuárias ativas e convites pendentes conforme permissions, perfil de acesso e ações rápidas. Sem métricas dos módulos futuros. |
| Usuárias | IMPLEMENTADO | Lista paginada com busca e filtros de status/perfil/equipe; permite editar cargo, role e equipes conforme permissão. Protege a última Super Admin ativa e revoga sessões na inativação. |
| Convites | IMPLEMENTADO | Criação, cancelamento, expiração e aceite de uso único. O link aparece uma vez no dialog, com cópia e feedback; envio automático por SMTP é planejado. |
| Equipes | IMPLEMENTADO | Hierarquia visual, criação, edição, arquivamento com confirmação e busca de integrantes; uma usuária pode participar de várias equipes. Proteções contra ciclos e alterações indevidas da raiz. |
| Tasks | PLANEJADO | Unidade central de trabalho, com responsáveis, acompanhamento, comentários e checklist. |
| Projetos | PLANEJADO | Organização de iniciativas e suas entregas, relacionadas às Tasks. |
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
