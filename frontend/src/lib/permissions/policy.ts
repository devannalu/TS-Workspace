export const permissionKeys = [
  "users.view", "users.create", "users.edit", "users.disable", "users.manage",
  "teams.view", "teams.create", "teams.edit", "teams.archive", "teams.manage_members",
  "permissions.view", "permissions.manage", "settings.view", "audit.view",
] as const;
export type PermissionKey = typeof permissionKeys[number];
export type PermissionContext = {
  active: boolean;
  role: string;
  rolePermissions: readonly string[];
  overrides: Readonly<Record<string, "ALLOW" | "DENY">>;
};

export function resolvePermission(context: PermissionContext, key: PermissionKey): boolean {
  if (!context.active || !permissionKeys.includes(key)) return false;
  if (context.role === "SUPER_ADMIN") return true;
  const override = context.overrides[key];
  if (override) return override === "ALLOW";
  return context.rolePermissions.includes(key);
}
