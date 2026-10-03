<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` (resolved from this file's directory; in monorepos the `next` package may not be visible from the repo root) before writing any code. Heed deprecation notices.

This block is written and re-added by `next dev` — verify at `node_modules/next/dist/server/lib/generate-agent-files.js`. Removing it from a diff only re-creates the uncommitted change; committing it with your work keeps the tree clean.

<!-- END:nextjs-agent-rules -->

# Padrões de código do TS Workspace

- O domínio deve usar PT-BR, com nomes naturais e identificadores sem acentos.
- Inglês exige razão técnica: framework, protocolo, interface externa ou convenção universal.
- Contratos HTTP/JSON, permission keys e schema persistido não mudam por estética.
- Comentários próprios devem ser curtos, úteis, em PT-BR e explicar o porquê.
- Preferir nomes explícitos, métodos coesos, early returns e responsabilidades claras.
- Organizar por domínio/feature, com arquivos próximos e estrutura compacta.
- Evitar excesso de pastas, arquivos, camadas, DTOs, hooks e abstrações prematuras.
- Não criar páginas, módulos, dependências ou estado global sem necessidade atual.
- Remover código morto, imports abandonados, duplicação relevante e TODOs obsoletos.
- Controllers delegam; serviços expressam casos de uso; persistência mantém responsabilidade própria.
- Preservar segurança, comportamento, identidade visual, schema e migrations existentes.
- Refatorar por módulo; compilar e executar testes relevantes antes de avançar.
- Antes de concluir, revisar nomes, responsabilidades, comentários, imports, estrutura e dependências.
- Uma desenvolvedora nova deve entender rapidamente o propósito de cada arquivo.
