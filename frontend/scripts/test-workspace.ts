import assert from "node:assert/strict";
import { randomBytes, randomUUID } from "node:crypto";
import { spawn } from "node:child_process";
import { setTimeout as delay } from "node:timers/promises";
import { config } from "dotenv";
import { requireTestDatabase } from "../src/lib/db/test-safety";
import { createDatabaseClient } from "../src/lib/db/client";
import { seedCore } from "../prisma/seed-data";
import { bootstrapFirstAdmin } from "../src/lib/auth/bootstrap";

config({ quiet: true });
const testUrl = requireTestDatabase(process.env.TEST_DATABASE_URL, process.env.DATABASE_URL);
const db = createDatabaseClient(testUrl);
const baseURL = "http://127.0.0.1:3101";
process.env.BETTER_AUTH_URL = baseURL;
process.env.BETTER_AUTH_SECRET = randomBytes(32).toString("hex");
const credentials = { name: "Integrante de validação HTTP", email: `workspace-${randomUUID()}@example.test`, password: randomBytes(24).toString("hex") };
let userId: string | undefined;
let child: ReturnType<typeof spawn> | undefined;
let stage = "preparação";

async function post(path: string, body: object, cookie = "") {
  return fetch(`${baseURL}/api/auth${path}`, { method: "POST", headers: { "content-type": "application/json", origin: baseURL, ...(cookie ? { cookie } : {}) }, body: JSON.stringify(body), redirect: "manual" });
}

async function assertLoginRedirect(response: Response) {
  if (response.status === 307) {
    assert.equal(response.headers.get("location"), "/login");
    return;
  }
  // loading.tsx inicia streaming: Next documenta o redirecionamento via meta nesse caso.
  assert.equal(response.status, 200);
  const html = await response.text();
  assert.ok(/<meta[^>]*id="__next-page-redirect"[^>]*content="[^"]*url=\/login"/.test(html));
  assert.ok(!html.includes(credentials.email) && !html.includes("Seu perfil"));
}

try {
  await seedCore(db);
  userId = (await bootstrapFirstAdmin(db, credentials)).userId;
  assert.equal((await bootstrapFirstAdmin(db, credentials)).status, "already-provisioned");
  child = spawn(process.execPath, ["node_modules/next/dist/bin/next", "start", "--hostname", "127.0.0.1", "--port", "3101"], {
    env: { ...process.env, DATABASE_URL: testUrl, NEXT_TELEMETRY_DISABLED: "1" }, stdio: "ignore",
  });
  let ready = false;
  for (let attempt = 0; attempt < 60; attempt++) {
    if (child.exitCode !== null) throw new Error("Servidor de teste encerrou antes de iniciar.");
    try { ready = (await fetch(`${baseURL}/login`)).status === 200; } catch { /* Servidor ainda iniciando. */ }
    if (ready) break;
    await delay(500);
  }
  assert.ok(ready, "Servidor de teste não iniciou.");
  stage = "proteção anônima";
  const anonymous = await fetch(`${baseURL}/workspace`, { redirect: "manual" });
  await assertLoginRedirect(anonymous);
  stage = "signup público";
  const count = await db.user.count();
  assert.ok((await post("/sign-up/email", { ...credentials, email: `public-${randomUUID()}@example.test` })).status >= 400);
  assert.equal(await db.user.count(), count);
  stage = "login e dashboard";
  const login = await post("/sign-in/email", { email: credentials.email, password: credentials.password });
  assert.equal(login.status, 200);
  let cookie = login.headers.getSetCookie().map(value => value.split(";")[0]).join("; ");
  assert.ok(login.headers.getSetCookie().some(value => value.includes("HttpOnly")));
  const dashboard = await fetch(`${baseURL}/workspace`, { headers: { cookie }, redirect: "manual" });
  assert.equal(dashboard.status, 200);
  const html = await dashboard.text();
  assert.ok(html.includes(credentials.name) && html.includes(credentials.email) && html.includes("Fundadoras"));
  stage = "inativação e revogação";
  await db.profile.update({ where: { userId }, data: { status: "inactive" } });
  const inactive = await fetch(`${baseURL}/workspace`, { headers: { cookie }, redirect: "manual" });
  await assertLoginRedirect(inactive); assert.equal(await db.session.count({ where: { userId } }), 0);
  await db.profile.update({ where: { userId }, data: { status: "active" } });
  const relogin = await post("/sign-in/email", { email: credentials.email, password: credentials.password });
  assert.equal(relogin.status, 200);
  cookie = relogin.headers.getSetCookie().map(value => value.split(";")[0]).join("; ");
  stage = "logout";
  assert.equal((await post("/sign-out", {}, cookie)).status, 200);
  assert.equal(await db.session.count({ where: { userId } }), 0);
  await assertLoginRedirect(await fetch(`${baseURL}/workspace`, { headers: { cookie }, redirect: "manual" }));
  console.log("Next.js real: rota protegida, signup bloqueado, login, dashboard real, inactive, revogação e logout VALIDADOS no banco de testes.");
} catch {
  console.error(`Validação HTTP do Next falhou na etapa: ${stage}. Segredos e cookies omitidos.`);
  process.exitCode = 1;
} finally {
  if (child && child.exitCode === null) {
    const stopped = new Promise<void>(resolve => child!.once("exit", () => resolve()));
    child.kill(); await stopped;
  }
  if (userId) {
    await db.auditLog.deleteMany({ where: { entityId: userId } });
    await db.user.delete({ where: { id: userId } });
  }
  await db.$disconnect();
}
