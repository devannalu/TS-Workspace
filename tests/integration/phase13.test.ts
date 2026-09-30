import { randomUUID } from "node:crypto";
import { config } from "dotenv";
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { createDatabaseClient } from "../../src/lib/db/client";
import { requireTestDatabase } from "../../src/lib/db/test-safety";
import { seedCore } from "../../prisma/seed-data";
import { acceptInvite, cancelInviteForActor, createInviteForActor } from "../../src/lib/invites/service";
import { createInviteToken } from "../../src/lib/invites/token";

config({ quiet: true });
const db = createDatabaseClient(requireTestDatabase(process.env.TEST_DATABASE_URL, process.env.DATABASE_URL));
const createdUsers: string[] = [];
const createdInvites: string[] = [];
let supportRole: { id: string };
let teamIds: string[];

beforeAll(async () => {
  await seedCore(db);
  supportRole = await db.role.findUniqueOrThrow({ where: { key: "SUPPORT" }, select: { id: true } });
  teamIds = (await db.team.findMany({ where: { archivedAt: null }, orderBy: { key: "asc" }, take: 2, select: { id: true } })).map(team => team.id);
  process.env.BETTER_AUTH_URL = "http://127.0.0.1:3999";
  process.env.BETTER_AUTH_SECRET = "phase13-test-secret-012345678901234567890";
});

afterAll(async () => {
  if (createdInvites.length) await db.invite.deleteMany({ where: { id: { in: createdInvites } } });
  if (createdUsers.length) await db.user.deleteMany({ where: { id: { in: createdUsers } } });
  await db.$disconnect();
});

async function makeInvite(overrides: Partial<{ expiresAt: Date; cancelledAt: Date | null }> = {}) {
  const token = createInviteToken();
  const inviter = await db.user.create({ data: { name: "Admin de teste", email: `inviter-${randomUUID()}@example.test`, profile: { create: { roleId: (await db.role.findUniqueOrThrow({ where: { key: "SUPER_ADMIN" }, select: { id: true } })).id } } } });
  createdUsers.push(inviter.id);
  const invite = await db.invite.create({ data: { email: `invitee-${randomUUID()}@example.test`, tokenHash: token.tokenHash, expiresAt: overrides.expiresAt ?? new Date(Date.now() + 7 * 86400000), cancelledAt: overrides.cancelledAt ?? null, invitedById: inviter.id, roleId: supportRole.id, teams: { create: teamIds.map(teamId => ({ teamId })) } } });
  createdInvites.push(invite.id);
  return { invite, token: token.token };
}

describe("Fase 1.3 — convites e provisionamento", () => {
  it("aceita convite, não persiste token puro e cria User/Profile/teams", async () => {
    const { invite, token } = await makeInvite();
    const result = await acceptInvite({ token, name: "Integrante convidada", password: "senha-segura-2026" }, db);
    createdUsers.push(result.userId);
    const user = await db.user.findUniqueOrThrow({ where: { id: result.userId }, include: { profile: { include: { role: true } }, memberships: true, accounts: true } });
    expect(user.email).toBe(invite.email);
    expect(user.profile?.status).toBe("active");
    expect(user.profile?.role.key).toBe("SUPPORT");
    expect(user.memberships).toHaveLength(teamIds.length);
    expect(user.accounts.some(account => account.password && account.password !== "senha-segura-2026")).toBe(true);
    expect((await db.invite.findUniqueOrThrow({ where: { id: invite.id } })).usedAt).not.toBeNull();
    expect((await db.invite.findUniqueOrThrow({ where: { id: invite.id } })).tokenHash).not.toContain(token);
    await expect(acceptInvite({ token, name: "Outra", password: "senha-segura-2026" }, db)).rejects.toThrow("utilizado");
  });
  it("recusa convite expirado e cancelado", async () => {
    const expired = await makeInvite({ expiresAt: new Date(Date.now() - 1000) });
    await expect(acceptInvite({ token: expired.token, name: "Expirada", password: "senha-segura-2026" }, db)).rejects.toThrow("expirou");
    const cancelled = await makeInvite({ cancelledAt: new Date() });
    await expect(acceptInvite({ token: cancelled.token, name: "Cancelada", password: "senha-segura-2026" }, db)).rejects.toThrow("cancelado");
  });
  it("cria e cancela convite com auditoria, sem duplicar pendência", async () => {
    const admin = await db.user.create({ data: { name: "Criadora de convites", email: `creator-${randomUUID()}@example.test`, profile: { create: { roleId: (await db.role.findUniqueOrThrow({ where: { key: "SUPER_ADMIN" }, select: { id: true } })).id } } } });
    createdUsers.push(admin.id);
    const email = `new-${randomUUID()}@example.test`;
    const result = await createInviteForActor(admin.id, { email, roleId: supportRole.id, teamIds }, db);
    createdInvites.push(result.id);
    expect("tokenHash" in result).toBe(false);
    expect(result.url).toContain(`/convite/${result.token}`);
    await expect(createInviteForActor(admin.id, { email, roleId: supportRole.id, teamIds }, db)).rejects.toThrow("pendente");
    await cancelInviteForActor(admin.id, result.id, db);
    expect((await db.invite.findUniqueOrThrow({ where: { id: result.id } })).cancelledAt).not.toBeNull();
    expect(await db.auditLog.count({ where: { entityId: result.id, action: "invite.cancelled" } })).toBe(1);
  });
  it("protege aceite concorrente single-use", async () => {
    const { invite, token } = await makeInvite();
    const results = await Promise.allSettled([
      acceptInvite({ token, name: "Concorrente A", password: "senha-segura-2026" }, db),
      acceptInvite({ token, name: "Concorrente B", password: "senha-segura-2026" }, db),
    ]);
    const fulfilled = results.filter(result => result.status === "fulfilled");
    expect(fulfilled).toHaveLength(1);
    if (fulfilled[0]?.status === "fulfilled") createdUsers.push(fulfilled[0].value.userId);
    expect(await db.user.count({ where: { email: invite.email } })).toBe(1);
    expect(await db.invite.count({ where: { id: invite.id, usedAt: { not: null } } })).toBe(1);
  });
});
