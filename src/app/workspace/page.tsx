import { requireAuth } from "@/lib/auth/session";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { LogoutButton } from "@/components/logout-button";
import { hasPermission } from "@/lib/permissions";

export const metadata = { title: "Início" };
export default async function WorkspacePage() {
  const user = await requireAuth();
  const [canUsers, canTeams] = await Promise.all([hasPermission("users.view"), hasPermission("teams.view")]);
  return <WorkspaceShell canUsers={canUsers} canTeams={canTeams}><div className="w-full space-y-8">
    <div className="flex flex-wrap items-start justify-between gap-5"><div><Badge>Seu workspace</Badge><h1 className="mt-5 text-3xl font-semibold tracking-tight sm:text-4xl">Olá, {user.name}.</h1><p className="mt-3 text-muted-foreground">Bom ter você na Tech Sisters.</p></div><LogoutButton /></div>
    <div className="grid gap-6 md:grid-cols-2">
      <Card aria-labelledby="profile-title" className="p-6 sm:p-8"><h2 id="profile-title" className="text-xl font-semibold">Seu perfil</h2><dl className="mt-6 space-y-5"><div><dt className="text-sm text-muted-foreground">E-mail</dt><dd className="mt-1 break-all">{user.email}</dd></div><div><dt className="text-sm text-muted-foreground">Cargo</dt><dd className="mt-1">{user.profile?.jobTitle ?? "Não informado"}</dd></div><div><dt className="text-sm text-muted-foreground">Perfil de acesso</dt><dd className="mt-1">{user.profile?.role.name}</dd></div></dl></Card>
      <Card aria-labelledby="teams-title" className="p-6 sm:p-8"><h2 id="teams-title" className="text-xl font-semibold">Suas equipes</h2>{user.memberships.length ? <ul className="mt-6 space-y-3">{user.memberships.map(({ team }) => <li key={team.id} className="rounded-xl border border-border px-4 py-3">{team.name}</li>)}</ul> : <p className="mt-6 text-sm leading-6 text-muted-foreground">Você ainda não participa de uma equipe ativa.</p>}</Card>
    </div>
  </div></WorkspaceShell>;
}
