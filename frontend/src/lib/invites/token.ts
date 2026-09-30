import { createHash, randomBytes } from "node:crypto";

export const INVITE_TTL_DAYS = 7;

export function createInviteToken() {
  const token = randomBytes(32).toString("hex");
  return { token, tokenHash: hashInviteToken(token) };
}

export function hashInviteToken(token: string) {
  return createHash("sha256").update(token, "utf8").digest("hex");
}

export type InviteState = "pending" | "accepted" | "cancelled" | "expired";

export function getInviteState(invite: { usedAt: Date | null; cancelledAt: Date | null; expiresAt: Date }, now = new Date()): InviteState {
  if (invite.usedAt) return "accepted";
  if (invite.cancelledAt) return "cancelled";
  if (invite.expiresAt <= now) return "expired";
  return "pending";
}

export function inviteStateLabel(state: InviteState) {
  return ({ pending: "Pendente", accepted: "Aceito", cancelled: "Cancelado", expired: "Expirado" } as const)[state];
}
