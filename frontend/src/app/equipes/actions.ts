"use server";

import { addTeamMember, archiveTeam, createTeam, removeTeamMember, updateTeam } from "@/lib/teams/admin-service";

export async function createTeamAction(input: { name: string; description: string; parentId: string }) {
  try { await createTeam(input); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível criar a equipe." }; }
}
export async function updateTeamAction(input: { teamId: string; name: string; description: string; parentId: string }) {
  try { await updateTeam(input); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível atualizar a equipe." }; }
}
export async function archiveTeamAction(teamId: string) {
  try { await archiveTeam(teamId); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível arquivar a equipe." }; }
}
export async function addTeamMemberAction(userId: string, teamId: string) {
  try { await addTeamMember(userId, teamId); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível adicionar a integrante." }; }
}
export async function removeTeamMemberAction(userId: string, teamId: string) {
  try { await removeTeamMember(userId, teamId); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível remover a integrante." }; }
}
