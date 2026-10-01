# Fase Java 3 — Equipes

Registro da validação de 2026-10-01. A documentação atual do domínio está em
[Equipes](../architecture/teams.md).

## Fonte e escopo

Baseline Prisma: `fabb67294509f89005c9bae0d7713b077f314691`. Foram recuperados
schema Team/TeamMember, seed-data, serviços administrativos/hierarquia e testes
de políticas/provisionamento. A key estável já existia no modelo validado.
As cinco equipes e suas relações iniciais foram mantidas exatamente.

V6 cria somente Team/TeamMember e provisiona equipes e membership da Super Admin
Java existente. V1–V5 permanecem intactas. Não altera User/Profile/Role.
Não migra gestão de Users, convites, Tasks ou a página oficial `/equipes`.

O Java preserva bloqueio de ciclos, raiz estrutural, arquivamento com filhas
ativas e remoção da última Super Admin de Fundadoras. A API valida equipe ativa
também na remoção de membership, conforme a exigência desta fase; o serviço
Prisma anterior não verificava esse estado na remoção. Dados históricos ficam
preservados, sem endpoint público de delete físico de Team.

## Erros corrigidos durante validação

O teste de upgrade RBAC ainda esperava uma migration após V4. Com V6 publicada
na mesma cadeia, o resultado correto passa a duas migrations; o teste também
confere cinco equipes e membership da identidade preexistente em Fundadoras.

O novo teste MockMvc misturava o postprocessor CSRF de teste com a obtenção
de cookie real, produzindo cookie nulo após outra requisição de teste.
O helper dessa suíte passou a usar o postprocessor consistentemente. A
validação HTTP separada continuou usando cookies e CSRF reais do backend local.

## HTTP real e dados locais

Backend local em 8080 e MySQL Java em 3309, sem usar instâncias de outros projetos.
Health/login/list/edit retornaram 200; create 201; add/remove/logout 204;
duplicate, archive de Fundadoras e remoção da última Super Admin retornaram 409;
acesso após logout retornou 401. Foram confirmadas cinco equipes e membership
da Super Admin na raiz. A equipe e a usuária temporárias foram removidas,
comparando todos os registros oficiais antes/depois para confirmar preservação.

O fingerprint da identidade confirmou preservação de UUID, email e hash de
senha; permaneceu uma User e uma Profile ACTIVE/SUPER_ADMIN. RBAC permaneceu
com quatro roles, 14 permissions e 32 grants. Flyway V1–V6 aplicado, Hibernate
validate e MySQL saudável. Nenhuma credencial, cookie ou hash foi registrado.

## Resultado final

- `mvnw.cmd -B test`: BUILD SUCCESS, 115 testes, zero falhas/erros/skips.
- `mvnw.cmd -B package`: BUILD SUCCESS, 116 testes, zero falhas/erros/skips;
  inclui o teste adicional de preservação do seed, totalizando 13 unitários
  Teams e nove testes de integração Teams, além dos 94 anteriores.
- Frontend: typecheck, lint sem warnings e build aprovados; somente o cliente
  técnico foi adicionado, mantendo `/equipes` e Prisma/Better Auth.
- Diff sem erros, 78 links relativos válidos, secret scan sem achados.
  `.env`, `frontend/.env`, bootstrap local, logs, helpers e target ignorados.

Os avisos já existentes sobre a matriz de suporte Flyway/MySQL 8.4 e o provider
explícito de autenticação não impediram migrations, Hibernate validate ou testes.
Gestão Java de Users/Invites/Tasks e troca da interface oficial continuam fora
do escopo desta publicação.
