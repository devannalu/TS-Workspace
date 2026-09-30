import { createServer, type Server } from "node:http";
import { randomBytes, randomUUID } from "node:crypto";
import { config } from "dotenv";
import { toNodeHandler } from "better-auth/node";
import { afterAll, beforeAll, describe, expect, it, vi } from "vitest";
import { createDatabaseClient } from "../../src/lib/db/client";
import { requireTestDatabase } from "../../src/lib/db/test-safety";
import { seedCore } from "../../prisma/seed-data";
import { bootstrapFirstAdmin } from "../../src/lib/auth/bootstrap";
import { createPublicAuth } from "../../src/lib/auth/factory";
import { findCurrentUser } from "../../src/lib/auth/current-user";
import { loadUserPermissions } from "../../src/lib/permissions/data";
import { resolvePermission } from "../../src/lib/permissions/policy";

config({ quiet: true });
const db = createDatabaseClient(requireTestDatabase(process.env.TEST_DATABASE_URL, process.env.DATABASE_URL));
const suffix = randomUUID();
const admin = { name: "Integrante de teste", email: `admin-${suffix}@example.test`, password: randomBytes(24).toString("hex") };
const createdIds: string[] = [];
let server: Server;
let auth: ReturnType<typeof createPublicAuth>;
let baseURL: string;
let userId: string;
let cookie: string;

async function post(path: string, body: object, cookieValue?: string) {
  return fetch(`${baseURL}/api/auth${path}`, { method: "POST", headers: { "content-type": "application/json", origin: baseURL, ...(cookieValue ? { cookie: cookieValue } : {}) }, body: JSON.stringify(body) });
}

beforeAll(async () => {
  server = createServer((request, response) => { void toNodeHandler(auth)(request, response); });
  await new Promise<void>(resolve => server.listen(0, "127.0.0.1", resolve));
  const address = server.address();
  if (!address || typeof address === "string") throw new Error("Servidor de teste indisponível.");
  baseURL = `http://127.0.0.1:${address.port}`;
  process.env.BETTER_AUTH_URL = baseURL;
  process.env.BETTER_AUTH_SECRET = randomBytes(32).toString("hex");
  auth = createPublicAuth(db);
});

afterAll(async () => {
  if (createdIds.length) {
    await db.auditLog.deleteMany({ where: { entityId: { in: createdIds } } });
    await db.user.deleteMany({ where: { id: { in: createdIds } } });
  }
  await db.$disconnect();
  if (server) await new Promise<void>((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
});

describe("Better Auth com MySQL real e HTTP", () => {
  it("seed é idempotente e cria exatamente a árvore oficial", async () => {
    const first = await seedCore(db);
    const ids = (await db.team.findMany({ orderBy: { key: "asc" } })).map(team => team.id);
    expect(await seedCore(db)).toEqual(first);
    expect(first).toEqual({ roles: 4, permissions: 14, rolePermissions: 32, teams: 5 });
    const teams = await db.team.findMany({ orderBy: { key: "asc" } });
    expect(teams.map(team => team.id)).toEqual(ids);
    const root = teams.find(team => team.key === "fundadoras")!;
    expect(root.parentId).toBeNull();
    expect(teams.filter(team => team.id !== root.id).every(team => team.parentId === root.id)).toBe(true);
  });
  it("signup HTTP público é rejeitado sem criar User", async () => {
    const before = await db.user.count();
    for (const path of ["/sign-up/email", "/sign-up/email/"]) {
      const response = await post(path, admin);
      expect(response.status).toBeGreaterThanOrEqual(400);
    }
    await expect(auth.api.signUpEmail({ body: admin })).rejects.toThrow();
    expect(await db.user.count()).toBe(before);
  });
  it("conta preexistente parcial não é elevada nem apagada", async () => {
    const partial = await db.user.create({ data: { name: "Teste parcial", email: `partial-${suffix}@example.test` } });
    createdIds.push(partial.id);
    await expect(bootstrapFirstAdmin(db, { ...admin, email: partial.email })).rejects.toThrow("parcial");
    expect(await db.user.findUnique({ where: { id: partial.id } })).not.toBeNull();
    expect(await db.profile.count({ where: { userId: partial.id } })).toBe(0);
  });
  it("falha após User/Account causa rollback completo", async () => {
    const transactional = db.$transaction.bind(db);
    const spy = vi.spyOn(db, "$transaction").mockImplementation(async (callback, options) => {
      if (typeof callback !== "function") throw new Error("Esperada transação interativa");
      return transactional(async tx => {
        const fail = vi.spyOn(tx.profile, "create").mockRejectedValueOnce(new Error("Falha de provisionamento induzida"));
        try { return await callback(tx); } finally { fail.mockRestore(); }
      }, options);
    });
    const before = await db.account.count();
    try { await expect(bootstrapFirstAdmin(db, admin)).rejects.toThrow("induzida"); }
    finally { spy.mockRestore(); }
    expect(await db.user.findUnique({ where: { email: admin.email } })).toBeNull();
    expect(await db.account.count()).toBe(before);
  });
  it("bootstrap cria identidade, credencial, perfil, membership e auditoria atomicamente", async () => {
    const result = await bootstrapFirstAdmin(db, admin);
    userId = result.userId; createdIds.push(userId);
    expect(result.status).toBe("created");
    const user = await db.user.findUniqueOrThrow({ where: { id: userId }, include: { profile: { include: { role: true } }, accounts: true, memberships: { include: { team: true } } } });
    expect(user.profile?.role.key).toBe("SUPER_ADMIN");
    expect(user.profile?.status).toBe("active");
    expect(user.accounts.some(account => account.providerId === "credential" && Boolean(account.password) && account.password !== admin.password)).toBe(true);
    expect(user.memberships.map(member => member.team.name)).toEqual(["Fundadoras"]);
    expect(await db.auditLog.count({ where: { entityId: userId, action: "bootstrap.super_admin_created" } })).toBe(1);
  });
  it("bootstrap repetido é idempotente e segunda Super Admin é recusada", async () => {
    expect((await bootstrapFirstAdmin(db, admin)).status).toBe("already-provisioned");
    await expect(bootstrapFirstAdmin(db, { ...admin, email: `second-${suffix}@example.test` })).rejects.toThrow("outra");
    expect(await db.auditLog.count({ where: { entityId: userId } })).toBe(1);
  });
  it("login inválido não cria sessão", async () => {
    const response = await post("/sign-in/email", { email: admin.email, password: "invalid-test-password" });
    expect(response.status).toBe(401);
    expect(await db.session.count({ where: { userId } })).toBe(0);
  });
  it("login válido emite cookie HttpOnly e persiste sessão", async () => {
    const response = await post("/sign-in/email", { email: admin.email, password: admin.password });
    expect(response.status).toBe(200);
    const cookies = response.headers.getSetCookie();
    const sessionCookie = cookies.find(value => value.includes("session_token="));
    expect(Boolean(sessionCookie?.toLowerCase().includes("httponly"))).toBe(true);
    expect(Boolean(sessionCookie?.toLowerCase().includes("samesite=lax"))).toBe(true);
    cookie = cookies.map(value => value.split(";")[0]).join("; ");
    expect(await db.session.count({ where: { userId } })).toBe(1);
    const sessionResponse = await fetch(`${baseURL}/api/auth/get-session`, { headers: { cookie } });
    expect(sessionResponse.status).toBe(200);
    const session = await sessionResponse.json();
    expect(session.user.id).toBe(userId);
    expect((await findCurrentUser(db, auth, new Headers({ cookie })))?.id).toBe(userId);
  });
  it("CSRF de origem externa é rejeitado", async () => {
    const response = await fetch(`${baseURL}/api/auth/sign-out`, { method: "POST", headers: { origin: "https://example.invalid", cookie, "content-type": "application/json" }, body: "{}" });
    expect(response.status).toBe(403);
    expect(await db.session.count({ where: { userId } })).toBe(1);
  });
  it("RBAC carrega grants/overrides reais e membership aceita várias equipes", async () => {
    const role = await db.role.findUniqueOrThrow({ where: { key: "SUPPORT" } });
    const member = await db.user.create({ data: { name: "Integrante RBAC", email: `member-${suffix}@example.test`, profile: { create: { roleId: role.id } } } });
    createdIds.push(member.id);
    const teams = await db.team.findMany({ take: 2 });
    await db.teamMember.createMany({ data: teams.map(team => ({ userId: member.id, teamId: team.id })) });
    expect(await db.teamMember.count({ where: { userId: member.id } })).toBe(2);
    const permission = await db.permission.findUniqueOrThrow({ where: { key: "teams.view" } });
    await db.userPermission.create({ data: { userId: member.id, permissionId: permission.id, effect: "DENY" } });
    expect(resolvePermission(await loadUserPermissions(db, member.id), "teams.view")).toBe(false);
    await db.userPermission.update({ where: { userId_permissionId: { userId: member.id, permissionId: permission.id } }, data: { effect: "ALLOW" } });
    expect(resolvePermission(await loadUserPermissions(db, member.id), "teams.view")).toBe(true);
  });
  it("logout revoga a sessão persistida", async () => {
    expect((await post("/sign-out", {}, cookie)).status).toBe(200);
    expect(await db.session.count({ where: { userId } })).toBe(0);
    expect(await findCurrentUser(db, auth, new Headers({ cookie }))).toBeNull();
  });
  it("inactive bloqueia sessão existente e novo login", async () => {
    const response = await post("/sign-in/email", { email: admin.email, password: admin.password });
    expect(response.status).toBe(200);
    cookie = response.headers.getSetCookie().map(value => value.split(";")[0]).join("; ");
    await db.profile.update({ where: { userId }, data: { status: "inactive" } });
    expect(await findCurrentUser(db, auth, new Headers({ cookie }))).toBeNull();
    expect(await db.session.count({ where: { userId } })).toBe(0);
    const denied = await post("/sign-in/email", { email: admin.email, password: admin.password });
    expect(denied.status).toBe(403);
  });
});
