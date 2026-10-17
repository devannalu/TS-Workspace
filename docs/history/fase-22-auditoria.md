# Fase 22 — Auditoria administrativa

A auditoria utiliza audit_log e os registros existentes. Não há segundo log, cópia do conteúdo nem trilha paralela. A nova V20 acrescenta somente índice de created_at/id para consulta temporal e ordenação estável.

## Acesso e privacidade

A capacidade audit.view é revalidada em cada listagem, detalhe e consulta de opções; grants e overrides existentes continuam válidos. O perfil não é substituído por uma regra administrativa hardcoded. Leitura administrativa tem escopo organizacional intencional, sem conceder acesso ao conteúdo das fontes.

A resposta inclui identificador do registro, autora/nome, ação conhecida, tipo conhecido, identificador UUID do recurso e instante. Não seleciona metadata_json, email, corpo do recurso, senha, token, cookie ou sessão. Ações/tipos históricos desconhecidos viram UNKNOWN; identificador livre inválido vira null. Conteúdo privado/removido não é recuperado para compor o detalhe. Sem exportação nem mutações.

## Consultas

- GET /api/v1/audit: filtros actor (nome literal), action, resourceType, from/to (instantes), page/size.
- GET /api/v1/audit/options: catálogo fechado de ações e tipos, após autorização.
- GET /api/v1/audit/{id}: detalhe seguro; inexistente recebe 404.

Padrão: últimos 30 dias, 25 por página; máximo 100 e período de 366 dias. Desde é inclusivo; até é exclusivo. Datas escolhidas na interface representam dias locais do dispositivo, convertidos para instantes no contrato HTTP. SQL parametrizado; wildcard do nome tratado literalmente. Ordenação por created_at/id.

## Lacuna corrigida

EquipeService recebe explicitamente a autora das cinco operações. Criação, edição, arquivamento e inclusão/remoção de integrante registram eventos na mesma transação, sem nomes, descrições ou contatos. Arquivo já realizado não gera outro evento. Controllers continuam delegando e contratos HTTP existentes permanecem iguais.

## Interface

/auditoria e navegação com audit.view. Filtros por autora, ação, tipo e período; loading, vazio, erro/retry, paginação e diálogo readonly. Ações e tipos recebem nomes humanos em PT-BR. Não exibe metadados nem busca conteúdo removido. Falhas de permissão limpam dados/detalhe anteriores.

## Validação

Sete testes integrados novos cobrem filtros/paginação, limites temporais, privacidade, permissões/overrides/inactive, eventos de equipe, inexistência e HTTP 401/403/404/400 com bloqueio de mutações. Direcionados com Equipes: 17 aprovados. Expectativas de Flyway foram atualizadas para V20 sem alterar migrations históricas.

Package completo: 323 testes Java aprovados, zero failures/errors/skips e JAR gerado.

Frontend: 198 testes aprovados; typecheck, lint e build aprovados. Seis testes de interação novos e navegação; diálogo, filtros, paginação, falhas, limpeza e vazio. HTTP real aprovou 31 eventos temporários, paginação, filtros, detalhe sem metadata, 401/403/404/400, override atual e mutação negada.

Navegador: filtros, detalhe, Escape, 390/768/1024/1280/1440 px sem overflow. Checkpoint: feat: adiciona auditoria administrativa; SHA no histórico Git deste documento.

Fixtures removidas com hashes e quantidades oficiais preservados. Secret scan sem achados; arquivos sensíveis e verificações locais permanecem ignorados. MySQL/Garage healthy; health Java 200.
