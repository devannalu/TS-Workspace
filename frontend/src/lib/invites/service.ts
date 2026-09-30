import "server-only";
import { z } from "zod";
import { betterAuth } from "better-auth";
import { APIError, createAuthMiddleware } from "better-auth/api";
import { prismaAdapter } from "@better-auth/prisma-adapter";
import type { PrismaClient } from "../../generated/prisma/client";
import { authOptions } from "../auth/options";
import { db } from "../db";
import { requireAuth } from "../auth/session";
import { requirePermission } from "../permissions";
import { createInviteToken, getInviteState, hashInviteToken, INVITE_TTL_DAYS } from "./token";

const createSchema = z.object({
  email: z.email().transform(value => value.toLowerCase()),
  roleId: z.uuid(),
  teamIds: z.array(z.uuid()).min(1).max(20),
  expiresInDays: z.coerce.number().int().min(1).max(30).default(INVITE_TTL_DAYS),
});
const acceptSchema = z.object({ name: z.string().trim().min(2).max(100), password: z.string().min(12).max(128), token: z.string().regex(/^[a-f0-9]{64}$/i) });

function inviteURL(token: string) {
  const base = process.env.NEXT_PUBLIC_APP_URL ?? process.env.BETTER_AUTH_URL;
  if (!base) throw new Error("Configure a URL base para convites.");
  return `${base.replace(/\/$/, "")}/convite/${token}`;
}

export async function createInviteForActor(actorId: string, input: unknown, database = db) {
  const parsed = createSchema.parse(input);
  const uniqueTeamIds = [...new Set(parsed.teamIds)];
  const token = createInviteToken();
  const expiresAt = new Date(Date.now() + parsed.expiresInDays * 24 * 60 * 60 * 1000);
  const result = await database.$transaction(async tx => {
    if (await tx.user.findUnique({ where: { email: parsed.email }, select: { id: true } })) throw new Error("Já existe uma usuária com este e-mail.");
    const pending = await tx.invite.findFirst({ where: { email: parsed.email, usedAt: null, cancelledAt: null, expiresAt: { gt: new Date() } }, select: { id: true } });
    if (pending) throw new Error("Já existe um convite pendente para este e-mail.");
    const [role, teams] = await Promise.all([
      tx.role.findUnique({ where: { id: parsed.roleId }, select: { id: true } }),
      tx.team.findMany({ where: { id: { in: uniqueTeamIds }, archivedAt: null }, select: { id: true } }),
    ]);
    if (!role) throw new Error("Cargo inválido.");
    if (teams.length !== uniqueTeamIds.length) throw new Error("Uma ou mais equipes não estão disponíveis.");
    const invite = await tx.invite.create({ data: {
      email: parsed.email, tokenHash: token.tokenHash, expiresAt, invitedById: actorId, roleId: role.id,
      teams: { create: uniqueTeamIds.map(teamId => ({ teamId })) },
    }, select: { id: true, email: true, expiresAt: true, role: { select: { name: true } }, teams: { select: { team: { select: { id: true, name: true } } } } } });
    await tx.auditLog.create({ data: { actorId, action: "invite.created", entityType: "Invite", entityId: invite.id, metadata: { email: parsed.email, roleId: role.id, teamIds: uniqueTeamIds } } });
    return invite;
  }, { isolationLevel: "Serializable", timeout: 30_000 });
  return { ...result, token: token.token, url: inviteURL(token.token) };
}

export async function createInvite(input: unknown, database = db) {
  const actor = await requireAuth();
  await requirePermission("users.create");
  return createInviteForActor(actor.id, input, database);
}

export async function cancelInviteForActor(actorId: string, inviteId: string, database = db) {
  const id = z.uuid().parse(inviteId);
  return database.$transaction(async tx => {
    const invite = await tx.invite.findUnique({ where: { id }, select: { id: true, usedAt: true, cancelledAt: true, expiresAt: true } });
    if (!invite) throw new Error("Convite não encontrado.");
    if (getInviteState(invite) !== "pending") throw new Error("Apenas convites pendentes podem ser cancelados.");
    const cancelledAt = new Date();
    const updated = await tx.invite.updateMany({ where: { id, usedAt: null, cancelledAt: null }, data: { cancelledAt } });
    if (updated.count !== 1) throw new Error("O convite mudou de estado e não pode mais ser cancelado.");
    await tx.auditLog.create({ data: { actorId, action: "invite.cancelled", entityType: "Invite", entityId: id } });
    return { id, cancelledAt };
  }, { isolationLevel: "Serializable" });
}

export async function cancelInvite(inviteId: string, database = db) {
  const actor = await requireAuth();
  await requirePermission("users.create");
  return cancelInviteForActor(actor.id, inviteId, database);
}

export async function listInvites(database = db) {
  const actor = await requireAuth();
  await requirePermission("users.view");
  const invites = await database.invite.findMany({ orderBy: { createdAt: "desc" }, take: 100, select: {
    id: true, email: true, expiresAt: true, usedAt: true, cancelledAt: true, createdAt: true,
    invitedBy: { select: { name: true } }, role: { select: { name: true } }, teams: { select: { team: { select: { id: true, name: true } } } },
  } });
  void actor;
  return invites.map(invite => ({ ...invite, state: getInviteState(invite) }));
}

export async function inspectInvite(token: string, database = db) {
  if (!/^[a-f0-9]{64}$/i.test(token)) return { state: "invalid" as const };
  const invite = await database.invite.findUnique({ where: { tokenHash: hashInviteToken(token) }, select: {
    id: true, email: true, expiresAt: true, usedAt: true, cancelledAt: true,
    role: { select: { id: true, name: true } }, teams: { select: { team: { select: { id: true, name: true, archivedAt: true } } } },
  } });
  if (!invite) return { state: "invalid" as const };
  const state = getInviteState(invite);
  if (state === "pending" && invite.teams.some(item => item.team.archivedAt)) return { state: "invalid" as const };
  return { state, invite: { id: invite.id, email: invite.email, expiresAt: invite.expiresAt, role: invite.role.name, teams: invite.teams.map(item => item.team.name) } };
}

export async function acceptInvite(input: unknown, database: PrismaClient = db) {
  const parsed = acceptSchema.parse(input);
  const tokenHash = hashInviteToken(parsed.token);
  return database.$transaction(async tx => {
    const invite = await tx.invite.findUnique({ where: { tokenHash }, include: { role: true, teams: { include: { team: true } } } });
    if (!invite) throw new Error("Este convite não é válido.");
    const state = getInviteState(invite);
    if (state === "accepted") throw new Error("Este convite já foi utilizado.");
    if (state === "cancelled") throw new Error("Este convite foi cancelado.");
    if (state === "expired") throw new Error("Este convite expirou.");
    if (invite.teams.some(item => item.team.archivedAt)) throw new Error("Este convite não está mais disponível.");
    if (await tx.user.findUnique({ where: { email: invite.email }, select: { id: true } })) throw new Error("Este e-mail já possui acesso ao workspace.");
    const claimedAt = new Date();
    const claimed = await tx.invite.updateMany({ where: { id: invite.id, usedAt: null, cancelledAt: null, expiresAt: { gt: claimedAt } }, data: { usedAt: claimedAt } });
    if (claimed.count !== 1) throw new Error("Este convite já foi utilizado.");
    const options = authOptions();
    const inviteAuth = betterAuth({ ...options, database: prismaAdapter(tx, { provider: "mysql", transaction: false }), emailAndPassword: { ...options.emailAndPassword, disableSignUp: false }, hooks: { before: createAuthMiddleware(async context => { if (context.request) throw new APIError("FORBIDDEN", { message: "Aceite disponível apenas pelo fluxo de convite." }); }) } });
    const created = await inviteAuth.api.signUpEmail({ body: { name: parsed.name, email: invite.email, password: parsed.password } });
    await tx.profile.create({ data: { userId: created.user.id, roleId: invite.roleId, status: "active" } });
    await tx.teamMember.createMany({ data: invite.teams.map(item => ({ userId: created.user.id, teamId: item.teamId })), skipDuplicates: true });
    await tx.auditLog.create({ data: { actorId: created.user.id, action: "invite.accepted", entityType: "Invite", entityId: invite.id, metadata: { userId: created.user.id } } });
    return { userId: created.user.id, inviteId: invite.id };
  }, { isolationLevel: "Serializable", timeout: 30_000 });
}
