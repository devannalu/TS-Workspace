import "server-only";
import { z } from "zod";
import { db } from "../db";
import { requirePermission } from "../permissions";
import { requireAuth } from "../auth/session";
import { validateTeamParent } from "./hierarchy";

export async function changeTeamParent(input: unknown) {
  const actor = await requireAuth();
  await requirePermission("teams.edit");
  const { teamId, parentId } = z.object({ teamId: z.uuid(), parentId: z.uuid().nullable() }).parse(input);
  return db.$transaction(async tx => {
    const teams = await tx.team.findMany({ select: { id: true, parentId: true, archivedAt: true } });
    if (!teams.some(team => team.id === teamId)) throw new Error("Equipe inexistente.");
    validateTeamParent(teams, teamId, parentId);
    const team = await tx.team.update({ where: { id: teamId }, data: { parentId } });
    await tx.auditLog.create({ data: { actorId: actor.id, action: "team.parent_changed", entityType: "Team", entityId: team.id, metadata: { parentId } } });
    return team;
  }, { isolationLevel: "Serializable" });
}
