import { inspectInvite } from "@/lib/invites/service";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { InviteAcceptForm } from "@/components/invite-accept-form";

export const dynamic = "force-dynamic";
export const metadata = { title: "Aceitar convite" };
export default async function InvitePage({ params }: { params: Promise<{ token: string }> }) {
  const { token } = await params;
  const result = await inspectInvite(token);
  const message = result.state === "expired" ? "Este convite expirou." : result.state === "cancelled" ? "Este convite foi cancelado." : result.state === "accepted" ? "Este convite já foi utilizado." : "Este convite não é válido.";
  return <WorkspaceShell><div className="mx-auto w-full max-w-xl"><Card className="p-6 sm:p-8"><Badge>Tech Sisters</Badge>{result.state === "pending" && result.invite ? <><h1 className="mt-5 text-3xl font-semibold tracking-tight">Boas-vindas ao workspace</h1><p className="mt-3 text-muted-foreground">Você foi convidada para entrar como <strong>{result.invite.role}</strong> nas equipes {result.invite.teams.join(", ")}.</p><p className="mt-3 text-sm text-muted-foreground">E-mail convidado: <strong>{result.invite.email}</strong></p><InviteAcceptForm token={token} /></> : <><h1 className="mt-5 text-2xl font-semibold">Não foi possível usar este convite</h1><p className="mt-3 text-muted-foreground">{message}</p></>}</Card></div></WorkspaceShell>;
}
