import Link from "next/link";
import {
  ArrowUpRight,
  Network,
  Users,
  Mail,
  ShieldCheck,
  Sparkles,
  Plus,
} from "lucide-react";
import { requireAuth, getMyTeams } from "@/lib/auth/session";
import { WorkspaceShell } from "@/components/layout/workspace-shell";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/ui/feedback";
import { javaRead } from "@/lib/api/server";
import { dashboardTotals } from "@/lib/ui/dashboard";
import { dashboardAccess } from "@/lib/ui/permissions";
export const metadata = { title: "Início" };
export default async function WorkspacePage() {
  const user = await requireAuth(),
    access = dashboardAccess(user);
  const [memberships, totals] = await Promise.all([
    getMyTeams(user),
    dashboardTotals(user, javaRead),
  ]);
  const stats = [
    {
      label: "Minhas equipes",
      value: memberships.length,
      Icon: Network,
      tone: "bg-lilac",
      show: access.teams,
    },
    {
      label: "Usuárias ativas",
      value: totals.activeUsers,
      Icon: Users,
      tone: "bg-butter",
      show: access.users,
    },
    {
      label: "Convites pendentes",
      value: totals.pendingInvites,
      Icon: Mail,
      tone: "bg-peach",
      show: access.users,
    },
    {
      label: "Meu acesso",
      value: user.role.name,
      Icon: ShieldCheck,
      tone: "bg-accent",
      show: true,
    },
  ].filter((s) => s.show);
  return (
    <WorkspaceShell>
      <div className="space-y-7">
        <section className="relative overflow-hidden rounded-2xl border border-border bg-accent p-6 sm:p-8">
          <Badge tone="pink">TECH SISTERS / WORKSPACE</Badge>
          <h1 className="mt-4 page-title">Olá, {user.name.split(" ")[0]}.</h1>
          <p className="mt-2 max-w-xl subtle">
            Tudo pronto para mais um dia no TS Workspace. Seu espaço para
            organizar e construir juntas.
          </p>
          <Sparkles
            size={72}
            aria-hidden
            className="absolute top-8 right-8 hidden text-primary/20 sm:block"
          />
        </section>
        <section
          aria-label="Resumo do workspace"
          className={`grid gap-4 sm:grid-cols-2 ${stats.length === 4 ? "xl:grid-cols-4" : stats.length === 3 ? "xl:grid-cols-3" : stats.length === 2 ? "xl:grid-cols-2" : "sm:grid-cols-1"}`}
        >
          {stats.map(({ label, value, Icon, tone }) => (
            <Card key={label} className="p-5">
              <div className="flex items-center justify-between">
                <p className="text-sm text-muted-foreground">{label}</p>
                <span className={`rounded-lg p-2 ${tone}`}>
                  <Icon size={17} aria-hidden />
                </span>
              </div>
              <p className="mt-4 text-2xl font-semibold tracking-tight">
                {value}
              </p>
            </Card>
          ))}
        </section>
        <div className="grid items-start gap-5 xl:grid-cols-[1.5fr_1fr]">
          <Card className="p-5 sm:p-6">
            <div className="flex items-center justify-between">
              <h2 className="section-title">Minhas equipes</h2>
              {access.teams && (
                <Link
                  href="/equipes"
                  className="text-sm font-medium text-primary"
                >
                  Ver equipes
                </Link>
              )}
            </div>
            <div className="mt-5 space-y-3">
              {memberships.length ? (
                memberships.map((team) => (
                  <Link
                    href="/equipes"
                    key={team.id}
                    className="flex items-center gap-4 rounded-xl border border-border p-4 hover:bg-muted"
                  >
                    <span className="rounded-xl bg-lilac p-3">
                      <Network size={20} aria-hidden />
                    </span>
                    <div className="min-w-0">
                      <p className="font-medium">{team.name}</p>
                      <p className="mt-1 subtle">
                        {team.description || "Sua equipe no Workspace."}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {team.parentId
                          ? "Área da Tech Sisters"
                          : "Equipe raiz · Tech Sisters"}
                      </p>
                    </div>
                    <ArrowUpRight
                      size={17}
                      aria-hidden
                      className="ml-auto shrink-0 text-muted-foreground"
                    />
                  </Link>
                ))
              ) : (
                <EmptyState
                  title="Seu próximo encontro começa aqui"
                  description="Você ainda não participa de uma equipe ativa."
                />
              )}
            </div>
          </Card>
          <Card className="p-5 sm:p-6">
            <h2 className="section-title">Ações rápidas</h2>
            <p className="mt-1 subtle">O que você precisa fazer hoje?</p>
            <div className="mt-5 space-y-2">
              {access.invite && (
                <Link
                  className="sidebar-link border-border"
                  href="/usuarias?convidar=1"
                >
                  <Mail size={18} aria-hidden />
                  Convidar usuária
                  <ArrowUpRight size={15} aria-hidden className="ml-auto" />
                </Link>
              )}
              {access.createTeam && (
                <Link
                  className="sidebar-link border-border"
                  href="/equipes?nova=1"
                >
                  <Plus size={18} aria-hidden />
                  Criar equipe
                  <ArrowUpRight size={15} aria-hidden className="ml-auto" />
                </Link>
              )}
              {access.users && (
                <Link className="sidebar-link" href="/usuarias">
                  <Users size={18} aria-hidden />
                  Ver usuárias
                </Link>
              )}
              {access.teams && (
                <Link className="sidebar-link" href="/equipes">
                  <Network size={18} aria-hidden />
                  Ver equipes
                </Link>
              )}
              {!access.users && !access.teams && (
                <p className="subtle">
                  Seu acesso está pronto. Procure uma administradora para entrar
                  em uma equipe.
                </p>
              )}
            </div>
            <p className="mt-6 border-t border-border pt-4 text-xs leading-5 text-muted-foreground">
              Mais recursos estão chegando ao Workspace.
            </p>
          </Card>
        </div>
      </div>
    </WorkspaceShell>
  );
}
