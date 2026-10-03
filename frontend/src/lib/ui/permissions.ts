import type { JavaUser } from "../api/auth";
export function navigationFor(permissions: readonly string[]) {
  return [
    { label: "Início", href: "/workspace", permission: null },
    { label: "Equipes", href: "/equipes", permission: "teams.view" },
    { label: "Usuárias", href: "/usuarias", permission: "users.view" },
  ].filter((item) => !item.permission || permissions.includes(item.permission));
}
export function dashboardAccess(user: Pick<JavaUser, "permissions">) {
  return {
    teams: user.permissions.includes("teams.view"),
    users: user.permissions.includes("users.view"),
    invite:
      user.permissions.includes("users.create") &&
      user.permissions.includes("users.view"),
    createTeam:
      user.permissions.includes("teams.create") &&
      user.permissions.includes("teams.view"),
  };
}
