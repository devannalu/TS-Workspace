import type { UsuarioAtual } from "../api/autenticacao";
export function navegacaoPermitida(permissoes: readonly string[]) {
  return [
    { label: "Início", href: "/workspace", permissao: null },
    { label: "Equipes", href: "/equipes", permissao: "teams.view" },
    { label: "Usuárias", href: "/usuarias", permissao: "users.view" },
  ].filter((linkNavegacao) => !linkNavegacao.permissao || permissoes.includes(linkNavegacao.permissao));
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
