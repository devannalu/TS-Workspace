import { betterAuth } from "better-auth";
import { APIError, createAuthMiddleware } from "better-auth/api";
import { prismaAdapter } from "@better-auth/prisma-adapter";
import { z } from "zod";
import type { PrismaClient } from "../../generated/prisma/client";
import { authOptions } from "./options";
import { decideBootstrap } from "./bootstrap-policy";

const bootstrapInput = z.object({
  name: z.string().trim().min(2).max(100),
  email: z.email().transform(value => value.toLowerCase()),
  password: z.string().min(12).max(128),
});

// Exclusivo do CLI administrativo: nunca importar em Route Handlers ou Server Actions.
export async function bootstrapFirstAdmin(db: PrismaClient, input: unknown) {
  const parsed = bootstrapInput.safeParse(input);
  if (!parsed.success) throw new Error("Dados de bootstrap inválidos. Confira nome, e-mail e senha de 12 a 128 caracteres.");
  const data = parsed.data;
  return db.$transaction(async tx => {
    const role = await tx.role.findUnique({ where: { key: "SUPER_ADMIN" } });
    const founders = await tx.team.findUnique({ where: { key: "fundadoras" } });
    if (!role || !founders || founders.archivedAt || founders.parentId) throw new Error("Execute o seed e confira a equipe Fundadoras antes do bootstrap.");
    const existing = await tx.user.findUnique({ where: { email: data.email }, include: { profile: true, accounts: true, memberships: true } });
    const admins = await tx.profile.findMany({ where: { roleId: role.id }, select: { userId: true } });
    const audit = existing && await tx.auditLog.findFirst({ where: { action: "bootstrap.super_admin_created", entityId: existing.id, actorId: existing.id } });
    const decision = decideBootstrap({
      existingUserId: existing?.id ?? null,
      superAdminIds: admins.map(admin => admin.userId),
      fullyProvisioned: Boolean(existing?.profile?.status === "active" && existing.profile.roleId === role.id && existing.accounts.some(account => account.providerId === "credential" && account.password) && existing.memberships.some(member => member.teamId === founders.id) && audit),
    });
    if (decision === "already-provisioned") return { status: decision, userId: existing!.id };

    // Instância isolada por operação, ligada à mesma transação do domínio; nunca montada em HTTP.
    const options = authOptions();
    const bootstrapAuth = betterAuth({
      ...options,
      database: prismaAdapter(tx, { provider: "mysql", transaction: false }),
      emailAndPassword: { ...options.emailAndPassword, disableSignUp: false },
      hooks: { before: createAuthMiddleware(async context => {
        if (context.request) throw new APIError("FORBIDDEN", { message: "Operação exclusivamente administrativa local." });
      }) },
    });
    const result = await bootstrapAuth.api.signUpEmail({ body: data });
    await tx.profile.create({ data: { userId: result.user.id, roleId: role.id, status: "active" } });
    await tx.teamMember.create({ data: { userId: result.user.id, teamId: founders.id } });
    await tx.auditLog.create({ data: { actorId: result.user.id, action: "bootstrap.super_admin_created", entityType: "User", entityId: result.user.id } });
    return { status: "created" as const, userId: result.user.id };
  }, { timeout: 30_000, isolationLevel: "Serializable" });
}
