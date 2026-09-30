import { betterAuth } from "better-auth";
import { APIError } from "better-auth/api";
import { prismaAdapter } from "@better-auth/prisma-adapter";
import type { PrismaClient } from "../../generated/prisma/client";
import { authOptions } from "./options";
import { hasActiveProfile } from "./access-policy";

export function createPublicAuth(db: PrismaClient) {
  return betterAuth({
    ...authOptions(),
    database: prismaAdapter(db, { provider: "mysql", transaction: true }),
    disabledPaths: ["/sign-up/email"],
    databaseHooks: {
      session: { create: { before: async session => {
        const profile = await db.profile.findUnique({ where: { userId: session.userId }, select: { status: true } });
        if (!hasActiveProfile(profile)) throw new APIError("FORBIDDEN", { message: "Acesso indisponível. Procure uma administradora." });
        return { data: session };
      } } },
    },
  });
}
