import "server-only";
import { z } from "zod";
import { db } from "../db";
import { requireAuth } from "../auth/session";
import { requirePermission } from "../permissions";
import { validateTeamParent } from "./hierarchy";

const teamSchema = z.object({ name: z.string().trim().min(2).max(100), description: z.string().trim().max(500).optional().default(""), parentId: z.uuid() });
const updateSchema = teamSchema.extend({ teamId: z.uuid() });

export async function listTeams(database = db) {
  await requireAuth();
  await requirePermission("teams.view");
  return database.team.findMany({ where: { archivedAt: null }, orderBy: [{ parentId: "asc" }, { name: "asc" }], select: {
    id: true, key: true, name: true, description: true, parentId: true, createdAt: true,
    members: { where: { user: { profile: { status: "active" } } }, select: { user: { select: { id: true, name: true, email: true } } } },
  } });
}

export async function listActiveUsersForTeams(database = db) {
  await requireAuth();
  await requirePermission("teams.manage_members");
  return database.user.findMany({ where: { profile: { status: "active" } }, orderBy: { name: "asc" }, select: { id: true, name: true, email: true } });
}

export async function createTeamForActor(actorId: string, input: unknown, database = db) {
  const parsed = teamSchema.parse(input);
  return database.$transaction(async tx => {
    const parent = await tx.team.findUnique({ where: { id: parsed.parentId }, select: { id: true, archivedAt: true } });
    if (!parent || parent.archivedAt) throw new Error("Equipe superior inválida.");
    const key = `${parsed.name.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "")}-${Date.now().toString(36)}`;
    const team = await tx.team.create({ data: { key, name: parsed.name, description: parsed.description, parentId: parsed.parentId } });
    await tx.auditLog.create({ data: { actorId, action: "team.created", entityType: "Team", entityId: team.id, metadata: { parentId: parsed.parentId } } });
    return team;
  });
}

export async function createTeam(input: unknown, database = db) {
  const actor = await requireAuth(); await requirePermission("teams.create"); return createTeamForActor(actor.id, input, database);
}

export async function updateTeamForActor(actorId: string, input: unknown, database = db) {
  const parsed = updateSchema.parse(input);
  return database.$transaction(async tx => {
    const teams = await tx.team.findMany({ select: { id: true, parentId: true, archivedAt: true } });
    validateTeamParent(teams, parsed.teamId, parsed.parentId);
    const team = await tx.team.update({ where: { id: parsed.teamId }, data: { name: parsed.name, description: parsed.description, parentId: parsed.parentId } });
    await tx.auditLog.create({ data: { actorId, action: "team.updated", entityType: "Team", entityId: team.id, metadata: { parentId: parsed.parentId } } });
    return team;
  }, { isolationLevel: "Serializable" });
}

export async function updateTeam(input: unknown, database = db) {
  const actor = await requireAuth(); await requirePermission("teams.edit"); return updateTeamForActor(actor.id, input, database);
}

export async function archiveTeamForActor(actorId: string, teamId: string, database = db) {
  const id = z.uuid().parse(teamId);
  return database.$transaction(async tx => {
    const team = await tx.team.findUnique({ where: { id }, select: { id: true, parentId: true, key: true, archivedAt: true } });
    if (!team) throw new Error("Equipe não encontrada.");
    if (team.parentId === null || team.key === "fundadoras") throw new Error("A equipe Fundadoras é estrutural e não pode ser arquivada.");
    if (team.archivedAt) return team;
    const children = await tx.team.count({ where: { parentId: id, archivedAt: null } });
    if (children) throw new Error("Mova ou arquive as subequipes antes de arquivar esta equipe.");
    const archivedAt = new Date();
    await tx.team.update({ where: { id }, data: { archivedAt } });
    await tx.auditLog.create({ data: { actorId, action: "team.archived", entityType: "Team", entityId: id } });
    return { ...team, archivedAt };
  });
}

export async function archiveTeam(teamId: string, database = db) {
  const actor = await requireAuth(); await requirePermission("teams.archive"); return archiveTeamForActor(actor.id, teamId, database);
}

export async function addTeamMemberForActor(actorId: string, userId: string, teamId: string, database = db) {
  const ids = z.object({ userId: z.uuid(), teamId: z.uuid() }).parse({ userId, teamId });
  return database.$transaction(async tx => {
    const [user, team] = await Promise.all([
      tx.user.findUnique({ where: { id: ids.userId }, select: { id: true, profile: { select: { status: true } } } }),
      tx.team.findUnique({ where: { id: ids.teamId }, select: { id: true, archivedAt: true } }),
    ]);
    if (!user?.profile || user.profile.status !== "active") throw new Error("Usuária indisponível.");
    if (!team || team.archivedAt) throw new Error("Equipe indisponível.");
    if (await tx.teamMember.findUnique({ where: { userId_teamId: ids } })) throw new Error("A integrante já está nesta equipe.");
    await tx.teamMember.create({ data: ids });
    await tx.auditLog.create({ data: { actorId, action: "team.member_added", entityType: "TeamMember", entityId: `${ids.userId}:${ids.teamId}` } });
    return ids;
  });
}

export async function addTeamMember(userId: string, teamId: string, database = db) {
  const actor = await requireAuth(); await requirePermission("teams.manage_members"); return addTeamMemberForActor(actor.id, userId, teamId, database);
}

export async function removeTeamMemberForActor(actorId: string, userId: string, teamId: string, database = db) {
  const ids = z.object({ userId: z.uuid(), teamId: z.uuid() }).parse({ userId, teamId });
  return database.$transaction(async tx => {
    const team = await tx.team.findUnique({ where: { id: ids.teamId }, select: { key: true, parentId: true } });
    if (team?.key === "fundadoras") {
      const membership = await tx.user.findUnique({ where: { id: ids.userId }, select: { profile: { select: { role: { select: { key: true } } } } } });
      if (membership?.profile?.role.key === "SUPER_ADMIN") {
        const count = await tx.teamMember.count({ where: { teamId: ids.teamId, user: { profile: { role: { key: "SUPER_ADMIN" }, status: "active" } } } });
        if (count <= 1) throw new Error("A última Super Admin não pode ser removida de Fundadoras.");
      }
    }
    const deleted = await tx.teamMember.deleteMany({ where: ids });
    if (!deleted.count) throw new Error("A integrante não está nesta equipe.");
    await tx.auditLog.create({ data: { actorId, action: "team.member_removed", entityType: "TeamMember", entityId: `${ids.userId}:${ids.teamId}` } });
    return ids;
  });
}

export async function removeTeamMember(userId: string, teamId: string, database = db) {
  const actor = await requireAuth(); await requirePermission("teams.manage_members"); return removeTeamMemberForActor(actor.id, userId, teamId, database);
}
