import { SessionBoundary } from "@/components/session-boundary";
import { redirect } from "next/navigation";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Badge } from "@/components/ui/badge";
import { TeamManagement } from "@/components/team-management";
import { javaRead } from "@/lib/api/server";
import type { JavaTeam, JavaTeamDetail } from "@/lib/api/teams";
import type { JavaManagedUser } from "@/lib/api/users";
import type { JavaPage } from "@/lib/api/management";
import { requireAuth } from "@/lib/auth/session";
import { hasPermission } from "@/lib/permissions";

export const metadata = { title: "Equipes" };
export default async function TeamsPage() {
  await requireAuth();
  if (!await hasPermission("teams.view")) redirect("/workspace");
  const summaries=await javaRead<JavaTeam[]>("/teams");
  const details=await Promise.all(summaries.filter(t=>!t.archived).map(t=>javaRead<JavaTeamDetail>("/teams/"+t.id)));
  const teams=details.map(d=>({...d.team,members:d.members.map(user=>({user}))}));
  const canMembers = await hasPermission("teams.manage_members");
  const users = canMembers && await hasPermission("users.view") ? (await javaRead<JavaPage<JavaManagedUser>>("/users?status=ACTIVE&size=100")).items : [];
  const [canCreate, canEdit, canArchive] = await Promise.all([hasPermission("teams.create"), hasPermission("teams.edit"), hasPermission("teams.archive")]);
  return <SessionBoundary><WorkspaceShell canUsers={await hasPermission("users.view")} canTeams><div className="w-full space-y-8"><div><Badge>Administração</Badge><h1 className="mt-5 text-3xl font-semibold tracking-tight sm:text-4xl">Equipes</h1><p className="mt-3 max-w-2xl text-muted-foreground">A hierarquia é carregada do banco; Fundadoras permanece como raiz estrutural.</p></div><TeamManagement teams={teams} users={users} canCreate={canCreate} canEdit={canEdit} canArchive={canArchive} canMembers={canMembers} /></div></WorkspaceShell></SessionBoundary>;
}
