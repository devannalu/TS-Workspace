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
import { exigirSessao, buscarMinhasEquipes } from "@/lib/sessao";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { EmptyState } from "@/components/ui/feedback";
import { lerJava } from "@/lib/api/server";
import { buscarTotaisPainel } from "@/lib/ui/painel";
import { acessosPainel } from "@/lib/ui/permissoes";
export const metadata = { title: "Início" };
export default async function PainelPage() {
  const usuario = await exigirSessao(),
    acessos = acessosPainel(usuario);
  const [minhasEquipes, totais] = await Promise.all([
    buscarMinhasEquipes(usuario),
    buscarTotaisPainel(usuario, lerJava),
  ]);
  const indicadores = [
    {
      label: "Minhas equipes",
      value: minhasEquipes.length,
      Icon: Network,
      tone: "bg-lilac",
      show: acessos.podeVerEquipes,
    },
    {
      label: "Usuárias ativas",
      value: totais.usuariosAtivos,
      Icon: Users,
      tone: "bg-butter",
      show: acessos.podeVerUsuarios,
    },
    {
      label: "Convites pendentes",
      value: totais.convitesPendentes,
      Icon: Mail,
      tone: "bg-peach",
      show: acessos.podeVerUsuarios,
    },
    {
      label: "Meu acesso",
      value: usuario.role.name,
      Icon: ShieldCheck,
      tone: "bg-accent",
      show: true,
    },
  ].filter((s) => s.show);
  return (
    <ShellWorkspace>
      <div className="space-y-7">
        <section className="relative overflow-hidden rounded-2xl border border-border bg-accent p-6 sm:p-8">
          <Badge tone="pink">TECH SISTERS / WORKSPACE</Badge>
          <h1 className="mt-4 page-title">Olá, {usuario.name.split(" ")[0]}.</h1>
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
          className={`grid gap-4 sm:grid-cols-2 ${indicadores.length === 4 ? "xl:grid-cols-4" : indicadores.length === 3 ? "xl:grid-cols-3" : indicadores.length === 2 ? "xl:grid-cols-2" : "sm:grid-cols-1"}`}
        >
          {indicadores.map(({ label, value, Icon, tone }) => (
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
              {acessos.podeVerEquipes && (
                <Link
                  href="/equipes"
                  className="text-sm font-medium text-primary"
                >
                  Ver equipes
                </Link>
              )}
            </div>
            <div className="mt-5 space-y-3">
              {minhasEquipes.length ? (
                minhasEquipes.map((equipe) => (
                  <Link
                    href="/equipes"
                    key={equipe.id}
                    className="flex items-center gap-4 rounded-xl border border-border p-4 hover:bg-muted"
                  >
                    <span className="rounded-xl bg-lilac p-3">
                      <Network size={20} aria-hidden />
                    </span>
                    <div className="min-w-0">
                      <p className="font-medium">{equipe.name}</p>
                      <p className="mt-1 subtle">
                        {equipe.description || "Sua equipe no Workspace."}
                      </p>
                      <p className="mt-1 text-xs text-muted-foreground">
                        {equipe.parentId
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
              {acessos.podeCriarConvite && (
                <Link
                  className="sidebar-link border-border"
                  href="/usuarias?convidar=1"
                >
                  <Mail size={18} aria-hidden />
                  Convidar usuária
                  <ArrowUpRight size={15} aria-hidden className="ml-auto" />
                </Link>
              )}
              {acessos.podeCriarEquipe && (
                <Link
                  className="sidebar-link border-border"
                  href="/equipes?nova=1"
                >
                  <Plus size={18} aria-hidden />
                  Criar equipe
                  <ArrowUpRight size={15} aria-hidden className="ml-auto" />
                </Link>
              )}
              {acessos.podeVerUsuarios && (
                <Link className="sidebar-link" href="/usuarias">
                  <Users size={18} aria-hidden />
                  Ver usuárias
                </Link>
              )}
              {acessos.podeVerEquipes && (
                <Link className="sidebar-link" href="/equipes">
                  <Network size={18} aria-hidden />
                  Ver equipes
                </Link>
              )}
              {!acessos.podeVerUsuarios && !acessos.podeVerEquipes && (
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
    </ShellWorkspace>
  );
}
