import { SessionBoundary } from "@/components/session-boundary";
import { redirect } from "next/navigation";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { InviteForm } from "@/components/invite-form";
import { UserManagement } from "@/components/user-management";
import { javaRead } from "@/lib/api/server";
import type { JavaManagedUser } from "@/lib/api/users";
import type { JavaInvite } from "@/lib/api/invites";
import type { JavaPage, JavaRoleRef, JavaTeamRef } from "@/lib/api/management";

import { requireAuth } from "@/lib/auth/session";
import { hasPermission } from "@/lib/permissions";
import { CancelInviteButton } from "@/components/cancel-invite-button";

export const metadata = { title: "Usuárias" };
export default async function UsersPage() {
  await requireAuth();
  if (!await hasPermission("users.view")) redirect("/workspace");
  const [userPage, options, invitePage] = await Promise.all([javaRead<JavaPage<JavaManagedUser>>("/users?size=100"), javaRead<{roles:JavaRoleRef[];teams:JavaTeamRef[]}>("/users/options"), javaRead<JavaPage<JavaInvite>>("/invites?size=100")]);
  const users=userPage.items, {roles,teams}=options, invites=invitePage.items;
  const canCreate = await hasPermission("users.create");
  return <SessionBoundary><WorkspaceShell canUsers canTeams={await hasPermission("teams.view")}><div className="w-full space-y-8">
    <div><Badge>Administração</Badge><h1 className="mt-5 text-3xl font-semibold tracking-tight sm:text-4xl">Usuárias</h1><p className="mt-3 max-w-2xl text-muted-foreground">Acompanhe acessos, cargos, status e equipes com dados do workspace.</p></div>
    {canCreate && <InviteForm roles={roles} teams={teams} />}
    <Card className="p-5 sm:p-7"><div className="flex flex-wrap items-end justify-between gap-3"><div><h2 className="text-xl font-semibold">Acessos existentes</h2><p className="mt-1 text-sm text-muted-foreground">O e-mail de autenticação não pode ser alterado nesta fase.</p></div></div><div className="mt-5"><UserManagement users={users} roles={roles} teams={teams} canEdit={await hasPermission("users.edit")} canDisable={await hasPermission("users.disable")} /></div></Card>
    <Card className="p-5 sm:p-7"><h2 className="text-xl font-semibold">Convites</h2><div className="mt-5 space-y-3">{invites.map(invite => <div key={invite.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border p-4"><div><p className="font-medium">{invite.email}</p><p className="text-sm text-muted-foreground">{invite.role.name} · {invite.teams.map(item => item.name).join(", ")}</p></div><div className="flex items-center gap-3"><span className="rounded-full bg-muted px-3 py-1 text-xs">{({ PENDING: "Pendente", USED: "Aceito", CANCELLED: "Cancelado", EXPIRED: "Expirado" } as const)[invite.status]}</span>{invite.status === "PENDING" && canCreate && <CancelInviteButton inviteId={invite.id} />}</div></div>)}{!invites.length && <p className="text-sm text-muted-foreground">Nenhum convite criado.</p>}</div></Card>
  </div></WorkspaceShell></SessionBoundary>;
}
