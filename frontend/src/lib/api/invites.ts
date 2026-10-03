import { managementRequest, type JavaPage, type JavaRoleRef, type JavaTeamRef } from "./management";

export type JavaInvite = {
  id: string; email: string; role: JavaRoleRef; teams: JavaTeamRef[];
  invitedBy: { id: string; name: string }; createdAt: string; expiresAt: string;
  status: "PENDING" | "USED" | "CANCELLED" | "EXPIRED";
};
export type JavaInviteCreated = { invite: JavaInvite; token: string; inviteUrl: string };
export type JavaInviteInput = { email: string; roleId: string; teamIds: string[]; expiresInDays?: number };
export type JavaPublicInvite = { email: string; role: string; teams: string[]; expiresAt: string };
export type JavaInviteAcceptance = { token: string; name: string; password: string; passwordConfirmation: string };

export const listJavaInvites = (page = 0, size = 25) => managementRequest<JavaPage<JavaInvite>>(`/invites?page=${page}&size=${size}`);
export const createJavaInvite = (input: JavaInviteInput) => managementRequest<JavaInviteCreated>("/invites", "POST", input);
export const cancelJavaInvite = (id: string) => managementRequest<JavaInvite>(`/invites/${encodeURIComponent(id)}/cancel`, "POST");
export const validateJavaInvite = (token: string) => managementRequest<JavaPublicInvite>("/invites/validate", "POST", { token });
export const acceptJavaInvite = (input: JavaInviteAcceptance) => managementRequest<{ userId: string; inviteId: string }>("/invites/accept", "POST", input);
