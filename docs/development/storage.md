# Armazenamento de arquivos

Desenvolvimento usa Garage 2.4.1 (`dxflrs/garage:v2.4.1`), serviço externo AGPL sem código incorporado. Produção usa Cloudflare R2 ou outro endpoint S3 compatível. O domínio Java conhece apenas `ArmazenamentoArquivo`.

## Iniciar localmente

Configure no `.env` ignorado: `STORAGE_ENDPOINT=http://localhost:3900`, `STORAGE_REGION=garage`, `STORAGE_BUCKET=ts-workspace-dev`, `STORAGE_ACCESS_KEY` (GK seguido de 32 dígitos hexadecimais), `STORAGE_SECRET_KEY` (64 dígitos hexadecimais) e `STORAGE_LOCAL_RPC_SECRET` (64 dígitos hexadecimais aleatórios). Não reutilize credenciais locais em produção.

Execute `python docker/garage/inicializar.py` para subir somente Garage, aguardar health e configurar CORS via S3. O script lê o `.env` ignorado sem imprimir segredos. Alternativamente, `docker compose up -d garage` sobe o serviço, mas CORS precisa ser configurado separadamente. Os parâmetros `--single-node --default-bucket` aplicam o layout, importam a chave e criam/autorizaram o bucket privado automaticamente. Volumes exclusivos: `ts_workspace_garage_meta` e `ts_workspace_garage_data`. Somente S3 é publicado, no loopback 3900. RPC 3901 permanece interno. Verifique `docker exec ts-workspace-garage /garage status` e o healthcheck do container. Não apagar volumes para solucionar problemas.

O backend roda no host e usa o mesmo endpoint acessível pelo navegador. Não há necessidade atual de `STORAGE_PUBLIC_ENDPOINT`. Se o backend for containerizado, definir comunicação e assinatura pelo endereço externo roteável sem alterar URLs depois de assinar.

## CORS e produção

Configure CORS do bucket pela API S3 PutBucketCors: origin `http://localhost:3010`, métodos PUT/GET/HEAD, header Content-Type, exposição ETag e max age 300. Nenhum wildcard de origin e nenhuma credencial de sessão é enviada ao storage. Em produção substitua pelo domínio oficial.

R2: endpoint HTTPS da conta, região `auto`, bucket privado e chave com acesso restrito ao bucket. Use as mesmas variáveis STORAGE_ENDPOINT/REGION/BUCKET/ACCESS_KEY/SECRET_KEY. `STORAGE_URL_SECONDS` padrão 300, máximo 300. Nunca publicar credenciais nem URLs assinadas.

## Fluxo e consistência

MySQL guarda somente metadados em `attachment`. O backend gera chave imprevisível; nome original jamais é caminho. A interface envia um identificador de solicitação; o backend deriva um ID limitado à usuária e recurso para recuperar a mesma autorização caso a resposta se perca. A chave do objeto permanece aleatória e gerada exclusivamente no backend. Repetir não estende o vencimento nem permite mudar o arquivo. Autorização gera estado PENDENTE e PUT temporário para `pending/`. Confirmar faz HEAD, leitura limitada e inspeção do conteúdo, depois grava bytes aprovados na chave definitiva, que jamais recebe URL de upload. Isso impede alteração do objeto aprovado reutilizando PUT. DISPONIVEL e auditoria são confirmados na mesma transação de banco; se essa transação falhar, repetir confirmação revalida e recupera o registro pendente.

Remoção grava REMOVIDO e auditoria antes de limpar objetos. Falha de storage retorna erro, mantém download bloqueado e permite repetir remoção para concluir limpeza. Não existe transação distribuída. URLs de GET já emitidas são capacidades válidas até expirar; excluir objeto impede novos downloads no storage.

Uploads falhos/abandonados permanecem PENDENTE. Limpeza operacional deve selecionar pendentes com autorização vencida, verificar vínculo e remover tanto chave temporária quanto definitiva antes de descartar metadados; nunca remover disponíveis. Objetos `pending/` de anexos confirmados também devem ser limpos após vencimento da autorização, para evitar recriação por PUT ainda válido. Não há scheduler nesta fase. Essa rotina de manutenção é uma pendência operacional documentada.

## Limites

10 MiB, PDF/PNG/JPEG/WEBP/TXT/CSV. Extensão e MIME devem coincidir; confirmação verifica assinatura binária ou texto UTF-8 sem controles binários e marcadores ativos comuns. Sem Office, SVG, HTML ou executáveis. Validação de formato não equivale a antivírus nem garante inocuidade de PDF ou texto. Download usa disposition attachment e application/octet-stream. Nunca executar arquivos recebidos; antivírus permanece evolução futura.

## Verificação real

Execute `python docker/garage/verificar.py`: valida presigned PUT, HEAD, presigned GET com conteúdo, bloqueio de leitura anônima, CORS e DELETE em objeto temporário próprio, com limpeza em finally. Validação HTTP Java também cobre o cliente AWS SDK real; checksums opcionais usam WHEN_REQUIRED para interoperabilidade S3 sem streaming AWS específico. URLs expiradas são recusadas pelo Garage com HTTP 400/InvalidRequest (Date is too old).

O cliente desativa chunked encoding conforme o [exemplo oficial Java do R2](https://developers.cloudflare.com/r2/examples/aws/aws-sdk-java/), mantendo path-style e região configurável. A compatibilidade foi exercitada no Garage; não foi usada uma conta R2 real nesta validação.
