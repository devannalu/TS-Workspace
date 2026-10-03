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

## Interface e identidade

Usar os [tokens e componentes compartilhados](../architecture/design-system.md).
Páginas autenticadas usam ShellWorkspace; experiências públicas usam ShellPublico.
Navegação e ações refletem permissions Java, sem substituir autorização backend.
Não criar indicadores fictícios ou links para módulos inexistentes.

Mutações reutilizam o cliente com CSRF. Confirmar ações destrutivas em Dialog,
com foco, Esc e mensagem clara; nunca window.confirm ou alert. Links de convite
permanecem somente no estado transitório de criação. Listagens usam filtros e
paginação da API, sem carregar todas as usuárias.

Testes de UI verificam comportamento com Testing Library/jsdom; dados de fixture
existem apenas nos testes. Browser validation deve incluir desktop, tablet e
mobile, além de acesso amplo e limitado. Conferir contraste e teclado.

## Guias Next.js

Seguir `AGENTS.md` e consultar a documentação da versão instalada em
`frontend/node_modules/next/dist/docs/` antes de mudanças relevantes em APIs,
convenções ou estrutura. Manter TypeScript estrito e regras de lint.

## Clareza e responsabilidades

Manter arquivos relacionados próximos, com poucos níveis de pastas e abstração.
Nomes devem revelar o caso de uso. Métodos curtos, early returns e parâmetros
explícitos são preferíveis a handlers genéricos e booleanos sem significado.
Separar apresentação de listas, diálogos e acesso à API quando houver uma
responsabilidade própria; não criar arquivo, hook ou camada para cada detalhe.

Controllers delegam regras a serviços; queries pertencem ao acesso a dados.
DTOs protegem contratos, sem expor entidades. Não criar mappers ou repositories
genéricos quando a implementação local e o Spring já resolvem o problema.
Comentários devem explicar o porquê, ser curtos e escritos em PT-BR.

Revisar nomes, duplicação, métodos grandes, código morto, dependências e consultas
antes de concluir. Evitar N+1, fetch duplicado, loops de consultas e listagens
inteiras sem paginação. Não adicionar estado global, dependências, rotas ou
módulos sem necessidade atual. Uma pessoa nova deve entender cada arquivo em
poucos minutos.

## Padrão permanente de nomes e comentários

Código próprio de domínio usa PT-BR natural, sem acentos nos identificadores:
`EquipeService`, `criarConvite`, `FormularioLogin`, `buscarUsuarioDaSessao`.
Arquivos e pastas acompanham o caso de uso; siglas técnicas como RBAC e nomes
exigidos pelo framework mantêm sua grafia. Contratos HTTP/JSON, schema e chaves
de permissão não são traduzidos em uma refatoração interna.

Comentários explicam decisões ou restrições que o código não demonstra sozinho.
Não narrar getters, persistência óbvia ou o nome de uma classe. Javadoc é útil
para contrato público ou decisão não evidente; não é obrigatório em cada classe.
Exceções de compatibilidade precisam de motivo concreto, como a classe do
principal serializado ou o símbolo chamado por uma migration imutável.

Revisar e testar um módulo antes de avançar ao próximo. Consolidar wrappers
que apenas repassam argumentos; separar arquivos grandes quando existe uma
responsabilidade própria. Não criar pastas DTO/mapper/hook por rotina, nem uma
camada genérica para evitar nomes de domínio. Dependências novas exigem uso real.
