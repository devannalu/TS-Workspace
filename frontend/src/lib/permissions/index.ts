import "server-only";
import { db } from "../db";
import { requireAuth } from "../auth/session";
import { loadUserPermissions } from "./data";
import { resolvePermission, type PermissionKey } from "./policy";

export async function getUserPermissions() {
  const user = await requireAuth();
  return loadUserPermissions(db, user.id);
}
export async function hasPermission(key: PermissionKey) {
  return resolvePermission(await getUserPermissions(), key);
}
export async function requirePermission(key: PermissionKey) {
  if (!await hasPermission(key)) throw new Error("Você não tem permissão para esta ação.");
}
export async function canAccessTeam(teamId: string) {
  const user = await requireAuth();
  const team = await db.team.findUnique({ where: { id: teamId }, select: { archivedAt: true } });
  if (!team || team.archivedAt) return false;
  if (await hasPermission("teams.manage_members")) return true;
  return Boolean(await db.teamMember.findUnique({ where: { userId_teamId: { userId: user.id, teamId } } }));
}
