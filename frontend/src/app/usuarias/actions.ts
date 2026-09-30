"use server";

import { cancelInvite, createInvite } from "@/lib/invites/service";
import { updateUser } from "@/lib/users/service";

export async function createInviteAction(input: { email: string; roleId: string; teamIds: string[] }) {
  try { const result = await createInvite(input); return { ok: true as const, url: result.url }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível criar o convite." }; }
}

export async function cancelInviteAction(inviteId: string) {
  try { await cancelInvite(inviteId); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível cancelar o convite." }; }
}

export async function updateUserAction(input: { userId: string; roleId: string; status: "active" | "inactive"; teamIds: string[] }) {
  try { await updateUser(input); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível atualizar a usuária." }; }
}
