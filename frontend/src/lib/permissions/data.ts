import type { PrismaClient } from "../../generated/prisma/client";
import { hasActiveProfile } from "../auth/access-policy";
import type { PermissionContext } from "./policy";

export async function loadUserPermissions(db: PrismaClient, userId: string): Promise<PermissionContext> {
  const user = await db.user.findUnique({ where: { id: userId }, select: {
    profile: { include: { role: { include: { permissions: { include: { permission: true } } } } } },
    permissions: { include: { permission: true } },
  } });
  return { active: hasActiveProfile(user?.profile), role: user?.profile?.role.key ?? "",
    rolePermissions: user?.profile?.role.permissions.map(item => item.permission.key) ?? [],
    overrides: Object.fromEntries(user?.permissions.map(item => [item.permission.key, item.effect]) ?? []),
  };
}
