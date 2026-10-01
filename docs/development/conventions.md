# Convenções de desenvolvimento

## Monorepo

`frontend/` contém a aplicação Next.js, seus scripts, dependências e testes.
`backend/` contém a API Java, Maven Wrapper e testes. Infraestrutura local fica
na raiz e em `docker/`. Execute comandos no diretório indicado pela documentação.

Agrupe código por domínio e mantenha responsabilidades claras. Evite pastas
vazias, camadas genéricas sem uso e cópias permanentes de implementações.
Durante a migração, retire código anterior somente após validar sua substituição
e autorizar a troca correspondente.

## Dados e migrations

- Flyway é responsável pelo schema Java; Hibernate permanece em validate.
- Prisma continua responsável pelo schema dos módulos que atende.
- Não editar migrations aplicadas; criar novas versões para mudanças.
- Planejar backfill/provisionamento antes de impor novas constraints obrigatórias.
- Usar FKs, unicidade, relações normalizadas e transações para proteger invariantes.
- Localizar dados de seed por keys estáveis, sem IDs hardcoded.
- Testes integrados usam dados e bancos separados.

## Contratos e validação

Controllers Java recebem/retornam DTOs; entidades JPA não devem ser expostas
diretamente. Bean Validation valida entrada, e serviços aplicam regras de
domínio. O frontend utiliza validações de formulário sem substituir as
validações no servidor.

Tratar erros com mensagens controladas e status apropriados. Não expor SQL,
stack traces ou detalhes que revelem existência de credenciais. Evitar fetches
desnecessários, N+1 e coleções sem limite conforme os módulos evoluem.

## Segurança

Autenticação identifica; autorização decide. A identidade vem da sessão.
Permissões são centralizadas e avaliadas server-side. Estado INACTIVE não pode
ser contornado por role, override ou interface. Não trocar precedência RBAC
silenciosamente.

Não desabilitar CSRF para viabilizar integração. CORS deve listar origens
necessárias, e cookies devem respeitar o ambiente HTTP local ou HTTPS.
Não armazenar autenticação em localStorage.

Versionar exemplos somente com placeholders. Arquivos reais de ambiente,
credenciais, hashes, cookies, tokens de convite e segredos de bootstrap ficam
fora do Git e dos logs. Revisar também diffs, fixtures e saídas de comandos
antes de publicá-los.

## Git e verificação

Trabalhar com commits de escopo claro, por exemplo `feat:`, `fix:` ou `docs:`.
Revisar `git status`, diff de trabalho e staged diff antes do commit.
Não incluir trabalho preexistente de outro escopo por meio de `git add -A`
sem conferir os arquivos.

Executar os testes adequados e registrar falhas reais, sem desativar verificações
para obter aprovação artificial. Em publicação autorizada, usar push normal,
confirmar SHA remoto e reportar alterações locais remanescentes.
Não usar reset destrutivo ou force push como rotina.

## Documentação viva e histórico

| Local | Conteúdo |
| --- | --- |
| README | Apresentação do projeto, execução resumida e navegação |
| docs/product | Objetivos, módulos e roadmap do produto |
| docs/architecture | Arquitetura, contratos e regras vigentes |
| docs/development | Como configurar, desenvolver e testar |
| docs/history | Fases, checkpoints, resultados e decisões de execução |

Quando uma implementação mudar, atualizar o documento vivo correspondente.
Uma fase pode gerar registro histórico, mas não transforma README e guias em
diário. Manter evidência de implementação separada da intenção de produto.

Por exemplo, concluir RBAC Java exige atualizar
[arquitetura de RBAC](../architecture/rbac.md); resultados e SHA podem ir ao
[histórico](../history/README.md). Registros históricos preservam os fatos da
época, inclusive limitações então existentes. Corrigir links após movimentos
sem reescrever a história.

## Next.js

Seguir `AGENTS.md` e consultar a documentação da versão instalada em
`frontend/node_modules/next/dist/docs/` antes de mudanças relevantes em APIs,
convenções ou estrutura. Manter TypeScript estrito e regras de lint.
