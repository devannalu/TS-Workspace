import type { PrismaClient } from "../../generated/prisma/client";
import type { createPublicAuth } from "./factory";
import { hasActiveProfile } from "./access-policy";

export async function findCurrentUser(db: PrismaClient, auth: ReturnType<typeof createPublicAuth>, headers: Headers) {
  const session = await auth.api.getSession({ headers });
  if (!session) return null;
  const user = await db.user.findUnique({
    where: { id: session.user.id },
    select: { id: true, name: true, email: true, image: true,
      profile: { include: { role: true } },
      memberships: { where: { team: { archivedAt: null } }, select: { team: { select: { id: true, name: true } } }, orderBy: { team: { name: "asc" } } },
    },
  });
  if (!user || !hasActiveProfile(user.profile)) {
    await db.session.deleteMany({ where: { userId: session.user.id } });
    return null;
  }
  return user;
}
