# Fase 23 — E-mails transacionais

Convites podem ser enviados por SMTP após sua transação confirmar. O aceite envia uma confirmação de acesso, sem senha ou token de convite. Mailpit local captura mensagens de desenvolvimento; produção utiliza um provedor SMTP configurado externamente. Notificações internas não viram e-mails automaticamente.

## Decisões

ServicoEmail utiliza JavaMailSender e spring-boot-starter-mail da mesma versão do BOM Spring Boot 3.5.16, sem nova camada de fila/outbox. Controllers delegam primeiro ao caso de uso transacional e só então ao envio; uma guarda recusa SMTP dentro de uma transação ativa. Falha SMTP retorna FALHOU e não desfaz convite/conta. Não há armazenamento de token em texto claro para reenvio automático.

POST /api/v1/invites preserva invite/token/inviteUrl e acrescenta envioEmail: ENVIADO, DESATIVADO ou FALHOU. A interface informa a situação e mantém a cópia única do link, inclusive quando o provedor falha. Token continua aleatório, de uso único, expirável e persistido apenas como hash SHA-256. Reutilização continua retornando o 400 original, sem alteração estética do contrato.

Templates oferecem texto e HTML UTF-8, Tech Sisters/rosa/creme, CTA simples e prazo humano explícito em UTC. Nome e link são escapados no HTML. Não há imagens remotas nem senha no conteúdo. SMTP possui timeouts finitos e debug desativado; logs de falha não incluem destinatária, credenciais, exceção ou link.

## Configuração

EMAIL_ENABLED=false por padrão. EMAIL_FROM, SMTP_HOST/PORT/USER/PASSWORD/AUTH/STARTTLS/SSL configuram envio. FRONTEND_ORIGIN compõe os links. Remetente e origem são validados quando habilitado. As variáveis reais permanecem fora do Git; backend/run-dev.ps1 importa somente os nomes necessários sem imprimir valores.

compose.yaml oferece profile email e recurso exclusivo ts-workspace-mailpit, imagem oficial fixada v1.31.6, SMTP 127.0.0.1:1029 e interface 127.0.0.1:8029, portas ajustáveis por MAILPIT_SMTP_PORT/UI_PORT. Nenhum relay externo é configurado. Caixa local é ferramenta de desenvolvimento, limitada a 500 mensagens; não é arquivo de e-mail de produção.

Nenhuma migration ou permission nova: users.create e regras existentes continuam sendo autoridade. Não há endpoint de envio arbitrário ou disparo público independente do convite.

## Validação

Sete testes unitários cobrem templates, destinatária, nomes escapados, segredo ausente, SMTP desativado, falha redigida, validação e transação. Quatro integrados usam MySQL e Mailpit isolados: SMTP real, hash/uso único, boas-vindas, falha SMTP preservando convite, validação e bloqueio sem permissão. Package completo: 334 testes Java aprovados, zero failures/errors/skips e JAR gerado. Frontend: 201 testes, typecheck, lint e build aprovados. Acrescenta três interações para as situações de envio, preservando link e evitando repetição.

HTTP real aprovou convite/email, hash, aceite único, boas-vindas e login da conta temporária. Navegador validou campos/cancelamento do formulário, estados pendente/usado e caixa Mailpit com os e-mails recebidos. Criações e aceite foram exercitados pela API; as interações de envio da interface foram cobertas pelos testes frontend. Somente destinatárias example.test, sem envio externo.

Fixtures e mensagens temporárias foram removidas por IDs/destinatárias conhecidas; hashes e quantidades oficiais idênticos ao baseline, caixa local voltou a zero. Secret scan sem achados. Checkpoint: feat: adiciona emails transacionais; SHA no histórico Git deste documento.

## Implantação externa

Credenciais SMTP reais, remetente/domínio verificado e configuração TLS do provedor são pendências externas de implantação. Não bloqueiam a validação local nem os módulos seguintes. Sem deploy externo nesta fase.

Referências: [Spring Boot 3.5 — envio de email](https://docs.spring.io/spring-boot/3.5/reference/io/email.html), [Docker oficial do Mailpit](https://mailpit.axllent.org/docs/install/docker/), [API do Mailpit](https://mailpit.axllent.org/docs/api-v1/).
