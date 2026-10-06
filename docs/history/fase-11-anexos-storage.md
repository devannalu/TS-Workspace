# Fase 11 — Anexos e storage

Implementação concluída e validada em 06/10/2026. Publicação acompanha o commit `feat: adiciona anexos e armazenamento de arquivos`.

## Entrega

V11 cria `attachment` com vínculo exclusivo a tarefa ou projeto, metadados, chaves únicas e estados PENDENTE/DISPONIVEL/REMOVIDO. V1–V10 foram preservadas. As permissões tasks.attach/projects.attach/attachments.remove ampliam o catálogo para 30 chaves e 82 concessões. SUPPORT permanece somente leitura em projetos, inclusive com override; arquivamento preserva leitura e restringe escrita. Eventos attachment.created/removed integram a timeline pelo vínculo exato, sem expor conteúdo ou chaves.

Garage 2.4.1, imagem oficial `dxflrs/garage:v2.4.1`, utiliza bucket privado ts-workspace-dev, S3 em loopback 3900 e volumes próprios. Inicialização automática single-node e CORS são documentados e verificáveis. RPC 3901 não é publicado. Produção S3/R2 utiliza configuração, sem adaptadores de domínio. AWS SDK S3 e URLConnection 2.55.6 são as únicas dependências diretas novas; nenhum pacote frontend foi adicionado. MinIO não é dependência.

Upload direto pré-assinado, confirmação com HEAD e inspeção real do conteúdo, chave definitiva sem PUT público e confirmação idempotente. Repetir uma autorização recupera o mesmo registro e vencimento. Download temporário até 300 segundos usa fetch com credentials omit e Blob, sem enviar sessão ao storage. Remoção lógica precede a limpeza física e permite recuperação de falhas sem reabrir o download. A interface apresenta lista contextual, seletor nativo, erros, retry, paginação e confirmação de remoção sem percentual fictício.

## Validação

- Backend: test e package aprovados, cada um com 232 testes, zero failures/errors/skips.
- Frontend: typecheck, lint, 100 testes e build aprovados.
- Garage real: PUT/HEAD/GET com conteúdo, leitura anônima bloqueada, URL expirada recusada, CORS e DELETE com limpeza.
- HTTP Java real: upload, repetição de autorização sem duplicação ou extensão de prazo, confirmação idempotente, listagem, download e remoção em tarefas e projetos; 401/403 e health/CSRF 200.
- Navegador: upload nativo, download com conteúdo conferido no arquivo salvo, tipo proibido, remoção, leitura por SUPPORT e histórico arquivado; quatro abas e teclado. Tarefas e projetos conferidos em 1440/1280/768/390, sem overflow horizontal. Viewport restaurado e conta oficial preservada.
- Limpeza: dados temporários removidos; hashes e quantidades dos dados oficiais idênticos ao baseline.
- Segurança: varredura de segredos sem achados; env reais e evidências locais ignorados. OSV sem achados nos 32 pacotes Java novos. npm audit de runtime sem achados; cinco vulnerabilidades high preexistentes na cadeia de desenvolvimento de lint permanecem registradas, sem novas dependências frontend.

## Limites e pendências operacionais

10 MiB; PDF/PNG/JPEG/WEBP/TXT/CSV. Extensão, MIME, assinatura binária ou UTF-8 são verificados. Isso não substitui antivírus nem um parser completo de formato. Limpeza de pendentes vencidos e objetos temporários é procedimento operacional documentado, sem scheduler nesta fase. Compatibilidade S3 foi exercitada no Garage; uma conta R2 real não foi usada. Credenciais, endpoint HTTPS e CORS de produção devem ser configurados na implantação.

Ver [configuração e decisões de storage](../development/storage.md).
