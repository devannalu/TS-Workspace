# Fase 21 — Busca global

A busca encontra Tarefas, Projetos, Reuniões, Eventos, Conteúdo, Parcerias e Notas sem persistir cópias ou criar infraestrutura adicional. MySQL executa consultas parametrizadas; tipos e colunas vêm de uma enumeração fechada. Não há nova migration ou permission key.

## Autorização e limites

Cada grupo herda a permission de leitura da fonte. Super Admin/Admin têm escopo global; os demais perfis precisam de vínculo atual com a equipe. A consulta aplica esses filtros antes de selecionar ou contar. Recursos e equipes arquivados, notas removidas e fontes sem permissão não aparecem. Contatos de parcerias, convites, credenciais, sessões, mensagens de chat e arquivos não são pesquisados.

Busca textual em título e descrição/briefing/pauta; notas usam título e corpo. A interface apresenta resumos de até 200 caracteres como texto simples. O termo aceita 2–160 caracteres e trata `%`, `_` e `!` literalmente. Todos os grupos retornam cinco resultados; seleção de um tipo permite paginação de até 100 por página, com padrão 20. Ordenação determinística por título e id. Os índices de equipe existentes limitam escopo; não se introduz full-text sem evidência de necessidade. Consulta por substring pode exigir varredura dos recursos ativos autorizados; revisar sob volume real.

## HTTP e interface

- `GET /api/v1/search?q=...&type=PROJETO&page=0&size=20` retorna tipos autorizados, grupos, totais e links internos.
- `GET /api/v1/{projects|events|partnerships|meetings}/{id}/notes/{notaId}` revalida o contexto e a fonte, retornando 404 para nota removida ou de outro recurso.
- `/busca` oferece formulário, grupos, filtro, paginação, loading, vazio e retry. Campo no header desktop e acesso compacto nas telas menores reutilizam a mesma rota.
- Resultados abrem detalhes existentes. Nota fora da página inicial aparece em destaque no contexto correto. Revogação de leitura limpa resultados anteriores.

## Validação

Seis testes integrados novos cobrem grupos, contato excluído, equipes, permissões revogadas, arquivos, remoção, contexto da nota, literalidade, paginação, limites, 401/403/400 e inactive. Banco MySQL isolado aplica V1–V19.

Package completo: 316 testes Java aprovados, sem failures/errors/skips; JAR gerado.

Frontend: 191 testes aprovados, typecheck, lint e build aprovados. Cobre envio explícito, links/texto seguro, filtro/paginação, tipos autorizados, vazio, falha com limpeza, navegação e nota fora da página inicial. Typecheck, lint, testes e build são obrigatórios no checkpoint.

HTTP real validou sessão, escopo, grupos, paginação, nota contextual e 404. Navegador validou campo global, resultados, detalhe com nota fora da primeira página, vazio, foco visível e 390/768/1024/1280/1440 px sem overflow; console sem erros. Fixtures removidas com hashes/quantidades oficiais idênticos ao baseline. Segredos locais permanecem ignorados.

Checkpoint: commit `feat: adiciona busca global`; SHA no histórico Git deste documento.
