import { redirect } from "next/navigation";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Badge } from "@/components/ui/badge";
import { TeamManagement } from "@/components/team-management";
import { listActiveUsersForTeams, listTeams } from "@/lib/teams/admin-service";
import { requireAuth } from "@/lib/auth/session";
import { hasPermission } from "@/lib/permissions";

export const metadata = { title: "Equipes" };
export default async function TeamsPage() {
  await requireAuth();
  if (!await hasPermission("teams.view")) redirect("/workspace");
  const teams = await listTeams();
  const canMembers = await hasPermission("teams.manage_members");
  const users = canMembers ? await listActiveUsersForTeams() : [];
  const [canCreate, canEdit, canArchive] = await Promise.all([hasPermission("teams.create"), hasPermission("teams.edit"), hasPermission("teams.archive")]);
  return <WorkspaceShell canUsers={await hasPermission("users.view")} canTeams><div className="w-full space-y-8"><div><Badge>Administração</Badge><h1 className="mt-5 text-3xl font-semibold tracking-tight sm:text-4xl">Equipes</h1><p className="mt-3 max-w-2xl text-muted-foreground">A hierarquia é carregada do banco; Fundadoras permanece como raiz estrutural.</p></div><TeamManagement teams={teams} users={users} canCreate={canCreate} canEdit={canEdit} canArchive={canArchive} canMembers={canMembers} /></div></WorkspaceShell>;
}
