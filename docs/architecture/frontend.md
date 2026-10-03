# Frontend

A aplicação em frontend/ utiliza Next.js App Router, React, TypeScript estrito
e Tailwind CSS. As versões estão no package.json e lockfile.

## API oficial

Todos os módulos usam src/lib/api/. http.ts centraliza base URL, cookies,
CSRF, parsing de Problem Details e ApiError com status 400/401/403/404/409.
Mensagens controladas não expõem SQL, stacks ou detalhes de autenticação.
NEXT_PUBLIC_JAVA_API_URL é incorporada no build e nunca contém segredos.

CSRF fica somente em memória, com preparação concorrente deduplicada.
Login/logout invalidam esse cache para acompanhar a rotação do Spring.
401/403 também invalidam; a próxima ação explícita prepara novo token.
Nenhuma mutação é reenviada automaticamente.

## Sessão e rotas

A sessão permanece no cookie HttpOnly TS_SESSION, sem localStorage ou
sessionStorage. auth/session.ts consulta /auth/me com o cookie da requisição
e no-store. A ausência de sessão redireciona as páginas protegidas para login.
SessionBoundary trata loading, authenticated, unauthenticated, forbidden e
indisponibilidade, revalida no foco e trata expiração recebida pelo cliente.
A proteção Next é navegação; autorização real pertence ao Spring Boot.

Login/logout usam auth.ts. Dashboard usa me e equipes reais. /equipes preserva
hierarquia, criação, edição, arquivamento e integrantes; /usuarias usa Users
e Invites. Editar role/cargo/equipes e ativar/inativar são ações separadas,
com suas permissões próprias. Menus e botões refletem as keys da sessão.

Server Components fazem somente GET usando server.ts; não encaminham cookies
para destinos escolhidos pelo usuário. Escritas vão diretamente do browser ao
Java. /convite/[token] valida por POST com CSRF, pede nome/senha/confirmação
e direciona para login após aceite, sem sessão automática. Link aparece apenas
na criação, em estado volátil; a página pública usa referrer no-referrer.

/infra oferece health e CSRF. Design, labels e responsividade permanecem
nos componentes existentes. Os guias da versão instalada ficam em
frontend/node_modules/next/dist/docs/, conforme AGENTS.md.

Veja [setup](../development/setup.md), [RBAC](rbac.md) e [testes](../development/testing.md).
