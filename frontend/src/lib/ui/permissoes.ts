import type { UsuarioAtual } from "../api/autenticacao";
export function navegacaoPermitida(permissoes: readonly string[]) {
  return [
    { label: "Início", href: "/workspace", permissoes: [] },
    { label: "Tarefas", href: "/tarefas", permissoes: ["tasks.view"] },
    { label: "Projetos", href: "/projetos", permissoes: ["projects.view"] },
    { label: "Calendário", href: "/calendario", permissoes: ["tasks.view", "projects.view"] },
    { label: "Equipes", href: "/equipes", permissoes: ["teams.view"] },
    { label: "Usuárias", href: "/usuarias", permissoes: ["users.view"] },
  ].filter(link => !link.permissoes.length || link.permissoes.some(p => permissoes.includes(p)));
}
export function acessosPainel(usuario: Pick<UsuarioAtual, "permissions">) {
  return {
    podeVerEquipes: usuario.permissions.includes("teams.view"),
    podeVerUsuarios: usuario.permissions.includes("users.view"),
    podeCriarConvite:
      usuario.permissions.includes("users.create") &&
      usuario.permissions.includes("users.view"),
    podeCriarEquipe:
      usuario.permissions.includes("teams.create") &&
      usuario.permissions.includes("teams.view"),
  };
}
