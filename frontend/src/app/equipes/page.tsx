import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { TeamManagement } from "@/components/team-management";
import { javaRead } from "@/lib/api/server";
import { requireAuth } from "@/lib/auth/session";
import type { JavaTeam } from "@/lib/api/teams";
import type { JavaManagedUser } from "@/lib/api/users";
import type { JavaPage } from "@/lib/api/management";
import { ErrorState } from "@/components/ui/feedback";
export const metadata = { title: "Equipes" };
export default async function TeamsPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const user = await requireAuth(),
    params = await searchParams,
    has = (key: string) => user.permissions.includes(key);
  if (!has("teams.view"))
    return (
      <WorkspaceShell>
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </WorkspaceShell>
    );
  const teams = await javaRead<JavaTeam[]>("/teams");
  const users =
    has("teams.manage_members") && has("users.view")
      ? (
          await javaRead<JavaPage<JavaManagedUser>>(
            "/users?status=ACTIVE&size=25",
          )
        ).items
      : [];
  return (
    <WorkspaceShell>
      <TeamManagement
        teams={teams}
        users={users}
        canCreate={has("teams.create")}
        canEdit={has("teams.edit")}
        canArchive={has("teams.archive")}
        canMembers={has("teams.manage_members")}
        initialCreate={params.nova === "1"}
      />
    </WorkspaceShell>
  );
}
