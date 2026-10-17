# E-mail transacional

O envio é opcional e fica desativado até EMAIL_ENABLED=true. O backend envia convite depois de confirmar a criação e boas-vindas depois do aceite. Não envia senha, não dispara notificações internas por e-mail e não oferece relay público.

## Desenvolvimento

Na raiz:

```powershell
docker compose --profile email up -d --no-deps mailpit
$env:EMAIL_ENABLED='true'
$env:FRONTEND_ORIGIN='http://localhost:3010'
./backend/run-dev.ps1
```

SMTP: 127.0.0.1:1029. Caixa capturada: http://localhost:8029. O profile email não inicia serviços legados nem recria MySQL/Garage. MAILPIT_SMTP_PORT/UI_PORT permitem portas diferentes; ajuste SMTP_PORT se alterar a primeira. Prefira destinatárias example.test nas verificações. Não configure relay externo no Mailpit local.

A caixa é ferramenta de desenvolvimento, com máximo de 500 mensagens e sem volume de arquivo permanente. Não a use para e-mails reais de produção ou como backup.

## Variáveis

| Nome | Uso |
| --- | --- |
| EMAIL_ENABLED | Habilitação explícita; padrão false. |
| EMAIL_FROM | Remetente validado; padrão local workspace@example.test. |
| SMTP_HOST / SMTP_PORT | Servidor e porta; padrão local 127.0.0.1:1029. |
| SMTP_USER / SMTP_PASSWORD | Credenciais do provedor, somente ambiente privado. |
| SMTP_AUTH | Autenticação do transporte; false somente para captura local ou relay expressamente autorizado. |
| SMTP_STARTTLS | Ativa e exige STARTTLS quando true. |
| SMTP_SSL | TLS implícito, conforme a porta e o provedor. |
| FRONTEND_ORIGIN | Origem oficial que compõe o link pessoal e a página de login. |

Timeouts de conexão, leitura e escrita são finitos; debug SMTP é desativado. Não copie credenciais para comandos, logs, exemplos ou frontend. backend/run-dev.ps1 carrega os nomes necessários do .env ignorado.

## Resultado e falhas

Criação de convite retorna envioEmail: ENVIADO, DESATIVADO ou FALHOU. ENVIADO significa aceito pelo transporte SMTP. O link é exibido uma vez no dialog e continua disponível para compartilhar se o envio estiver desativado ou falhar. Não existe reenvio automático com token persistido em texto claro; cancele e crie outro convite quando precisar emitir um novo link.

Boas-vindas usa somente nome/e-mail da conta e link de login. Uma falha não desfaz o acesso criado. Logs de falha são genéricos e não incluem corpo, destinatária, senha, cookie ou token.

## Produção

Configure provedor SMTP, remetente/domínio verificado, autenticação/TLS e origem HTTPS reais no ambiente de implantação. As credenciais e o destino não foram fornecidos: essa configuração é uma pendência externa de implantação, sem impedir os testes locais ou os demais módulos. Mailpit não deve ser exposto como serviço de produção.

Referências: [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/reference/io/email.html) e [Mailpit Docker](https://mailpit.axllent.org/docs/install/docker/).
