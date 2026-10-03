import type { JavaUser } from "../api/auth";
import type { JavaPage } from "../api/management";
import { dashboardAccess } from "./permissions";
export async function dashboardTotals(
  user: JavaUser,
  read: <T>(path: string) => Promise<T>,
) {
  const access = dashboardAccess(user);
  const users = access.users
    ? read<JavaPage<unknown>>("/users?status=ACTIVE&size=1").then(
        (page) => page.total,
      )
    : Promise.resolve(null);
  const pending = access.users
    ? read<number>("/invites/pending-count")
    : Promise.resolve(null);
  const [activeUsers, pendingInvites] = await Promise.all([users, pending]);
  return { activeUsers, pendingInvites };
}
