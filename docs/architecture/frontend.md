# Frontend

## Stack

A aplicação em `frontend/` utiliza Next.js App Router, React, TypeScript
estrito e Tailwind CSS. As versões exatas estão em `package.json` e
`package-lock.json`; o Node suportado está declarado em `engines`.

## Responsabilidades

O frontend organiza rotas, navegação, formulários, feedback e apresentação.
Componentes devem considerar carregamento, vazio, erro, sucesso e falta de
permissão. A interface prioriza HTML semântico, labels, contraste e foco visível,
com adaptação para desktop, tablet e mobile.

`src/app/` contém páginas e ações; `src/components/` contém componentes de
interface; `src/lib/` contém integrações e serviços. Módulos atuais de gestão
ainda possuem ações e regras server-side ligadas ao Prisma. Sua migração não
deve deslocar autorização para o navegador.

## Consumo da API Java

O cliente em `src/lib/api/` concentra chamadas técnicas ao Java.
`NEXT_PUBLIC_JAVA_API_URL` define a origem da API e é incorporada no build.
Variáveis públicas nunca podem conter segredos.

Chamadas de autenticação usam `credentials: "include"`. Antes de login/logout,
o cliente obtém CSRF e envia o header exigido pelo backend. A sessão fica em
cookie HttpOnly; tokens de autenticação não são armazenados em localStorage.

A página `/infra` verifica health e disponibilidade de CSRF. Ela é uma
ferramenta técnica, não uma segunda página de login.

## Rotas e autorização

`/login`, `/workspace`, `/usuarias`, `/equipes` e o aceite em
`/convite/[token]` pertencem ao fluxo oficial atual. Sessão e permissões são
verificadas no servidor. Esconder uma ação ou item de menu não autoriza nem
protege uma operação por si só.

O cliente técnico tipa role e permission keys da [API Java](rbac.md).
`src/lib/api/teams.ts` oferece list/detail/create/edit/archive e memberships da
[API de equipes](teams.md), com sessão por cookie e CSRF em cada escrita.
Esse cliente ainda não alimenta a página oficial `/equipes`.
`users.ts` e `invites.ts` oferecem gestão Java paginada e aceite por convite,
compartilhando cookies/CSRF em `management.ts`. São clientes técnicos, sem
nova interface administrativa. Links de convites Java ainda não são consumidos
pela página oficial `/convite/[token]`, que continua no serviço Prisma.
A troca do login e dos módulos deve ocorrer somente após a substituição
correspondente estar validada.

Para APIs do Next, consulte os guias da versão instalada em
`frontend/node_modules/next/dist/docs/`, conforme `AGENTS.md`.
Veja [setup](../development/setup.md) e [testes](../development/testing.md).
