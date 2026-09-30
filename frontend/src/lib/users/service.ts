import "server-only";
import { z } from "zod";
import { db } from "../db";
import { requireAuth } from "../auth/session";
import { requirePermission } from "../permissions";
import type { Prisma } from "../../generated/prisma/client";

const updateSchema = z.object({ userId: z.uuid(), roleId: z.uuid(), status: z.enum(["active", "inactive"]), teamIds: z.array(z.uuid()).max(50) });

async function protectAdministrativeAccess(tx: Prisma.TransactionClient, actorId: string, targetId: string, roleId: string, status: "active" | "inactive") {
  const target = await tx.profile.findUnique({ where: { userId: targetId }, include: { role: true } });
  if (!target) throw new Error("Perfil não encontrado.");
  const superAdmin = await tx.role.findUniqueOrThrow({ where: { key: "SUPER_ADMIN" } });
  const targetWillBeAdmin = roleId === superAdmin.id && status === "active";
  if (target.role.key === "SUPER_ADMIN" && !targetWillBeAdmin) {
    const count = await tx.profile.count({ where: { roleId: superAdmin.id, status: "active" } });
    if (count <= 1) throw new Error("A última Super Admin ativa não pode perder o acesso administrativo.");
  }
  if (targetId === actorId && !targetWillBeAdmin) throw new Error("Você não pode remover seu próprio acesso administrativo.");
}

export async function listUsers(database = db) {
  await requireAuth();
  await requirePermission("users.view");
  return database.user.findMany({ take: 100, orderBy: { createdAt: "desc" }, select: {
    id: true, name: true, email: true, createdAt: true,
    profile: { select: { status: true, jobTitle: true, role: { select: { id: true, key: true, name: true } } } },
    memberships: { where: { team: { archivedAt: null } }, select: { team: { select: { id: true, name: true } } }, orderBy: { team: { name: "asc" } } },
  } });
}

export async function getUserManagementData(database = db) {
  await requireAuth();
  await requirePermission("users.view");
  const [users, roles, teams] = await Promise.all([
    listUsers(database),
    database.role.findMany({ orderBy: { name: "asc" }, select: { id: true, key: true, name: true } }),
    database.team.findMany({ where: { archivedAt: null }, orderBy: { name: "asc" }, select: { id: true, name: true } }),
  ]);
  return { users, roles, teams };
}

export async function updateUser(input: unknown, database = db) {
  const actor = await requireAuth();
  await requirePermission("users.edit");
  const parsed = updateSchema.parse(input);
  await requirePermission("users.disable");
  const teamIds = [...new Set(parsed.teamIds)];
  return database.$transaction(async tx => {
    await protectAdministrativeAccess(tx, actor.id, parsed.userId, parsed.roleId, parsed.status);
    const teams = await tx.team.findMany({ where: { id: { in: teamIds }, archivedAt: null }, select: { id: true } });
    if (teams.length !== teamIds.length) throw new Error("Uma ou mais equipes não estão disponíveis.");
    const before = await tx.profile.findUniqueOrThrow({ where: { userId: parsed.userId }, select: { roleId: true, status: true } });
    const founders = await tx.team.findUnique({ where: { key: "fundadoras" }, select: { id: true } });
    const superAdmin = await tx.role.findUniqueOrThrow({ where: { key: "SUPER_ADMIN" }, select: { id: true } });
    if (founders && before.roleId === superAdmin.id && before.status === "active" && !teamIds.includes(founders.id)) {
      const remaining = await tx.teamMember.count({ where: { teamId: founders.id, userId: { not: parsed.userId }, user: { profile: { roleId: superAdmin.id, status: "active" } } } });
      if (!remaining) throw new Error("A última Super Admin ativa não pode ser removida de Fundadoras.");
    }
    await tx.profile.update({ where: { userId: parsed.userId }, data: { roleId: parsed.roleId, status: parsed.status } });
    await tx.teamMember.deleteMany({ where: { userId: parsed.userId, teamId: { notIn: teamIds } } });
    if (teamIds.length) await tx.teamMember.createMany({ data: teamIds.map(teamId => ({ userId: parsed.userId, teamId })), skipDuplicates: true });
    if (before.roleId !== parsed.roleId) await tx.auditLog.create({ data: { actorId: actor.id, action: "user.role_changed", entityType: "User", entityId: parsed.userId, metadata: { from: before.roleId, to: parsed.roleId } } });
    if (before.status !== parsed.status) {
      await tx.auditLog.create({ data: { actorId: actor.id, action: "user.status_changed", entityType: "User", entityId: parsed.userId, metadata: { from: before.status, to: parsed.status } } });
      if (parsed.status === "inactive") await tx.session.deleteMany({ where: { userId: parsed.userId } });
    }
    await tx.auditLog.create({ data: { actorId: actor.id, action: "user.teams_changed", entityType: "User", entityId: parsed.userId, metadata: { teamIds } } });
    return { userId: parsed.userId };
  }, { isolationLevel: "Serializable" });
}
