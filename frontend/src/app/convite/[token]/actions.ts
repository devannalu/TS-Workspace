"use server";

import { acceptInvite } from "@/lib/invites/service";

export async function acceptInviteAction(input: { token: string; name: string; password: string }) {
  try { await acceptInvite(input); return { ok: true as const }; }
  catch (error) { return { ok: false as const, error: error instanceof Error ? error.message : "Não foi possível aceitar este convite." }; }
}
