import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { ErrorState } from "@/components/ui/feedback";
import { InviteForm } from "@/components/invite-form";
import { UserManagement } from "@/components/user-management";
import { InviteList } from "@/components/invite-list";
import { javaRead } from "@/lib/api/server";
import { requireAuth } from "@/lib/auth/session";
import { pageNumber } from "@/lib/ui/format";
import type { JavaManagedUser } from "@/lib/api/users";
import type { JavaInvite } from "@/lib/api/invites";
import type { JavaPage, JavaRoleRef, JavaTeamRef } from "@/lib/api/management";
export const metadata = { title: "Usuárias" };
export default async function UsersPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const user = await requireAuth(),
    params = await searchParams,
    has = (key: string) => user.permissions.includes(key);
  if (!has("users.view"))
    return (
      <WorkspaceShell>
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </WorkspaceShell>
    );
  const query = new URLSearchParams({
    page: String(pageNumber(params.page)),
    size: "20",
  });
  for (const key of ["search", "status", "roleId", "teamId"])
    if (typeof params[key] === "string" && params[key])
      query.set(key, params[key]);
  const [users, options, invites] = await Promise.all([
    javaRead<JavaPage<JavaManagedUser>>("/users?" + query),
    javaRead<{ roles: JavaRoleRef[]; teams: JavaTeamRef[] }>("/users/options"),
    javaRead<JavaPage<JavaInvite>>(
      "/invites?page=" + pageNumber(params.invitePage) + "&size=10",
    ),
  ]);
  return (
    <WorkspaceShell>
      <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="page-title">Usuárias</h1>
            <p className="mt-2 subtle">
              Gerencie integrantes e acessos do Workspace.
            </p>
          </div>
          {has("users.create") && (
            <InviteForm
              roles={options.roles}
              teams={options.teams}
              initialOpen={params.convidar === "1"}
            />
          )}
        </div>
        <Card className="p-4 sm:p-6">
          <h2 className="section-title">Integrantes</h2>
          <div className="mt-5">
            <UserManagement
              data={users}
              roles={options.roles}
              teams={options.teams}
              canEdit={has("users.edit")}
              canDisable={has("users.disable")}
            />
          </div>
        </Card>
        <Card className="p-4 sm:p-6">
          <h2 className="section-title">Convites</h2>
          <p className="mt-1 subtle">
            Acompanhe os próximos acessos ao Workspace.
          </p>
          <div className="mt-5">
            <InviteList data={invites} canCancel={has("users.create")} />
          </div>
        </Card>
      </div>
    </WorkspaceShell>
  );
}
