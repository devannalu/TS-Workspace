import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesTarefas } from "@/lib/api/tarefas";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { ErrorState } from "@/components/ui/feedback";
import { Calendario } from "@/components/calendario/calendario";
export const metadata = { title: "Calendário" };
export default async function CalendarioPage({ searchParams }: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const usuario = await exigirSessao();
  if (!usuario.permissions.some(p => p === "tasks.view" || p === "projects.view" || p === "meetings.view" || p === "events.view"))
    return <ShellWorkspace><ErrorState message="Você não tem permissão para acessar esta área." /></ShellWorkspace>;
  const parametros = await searchParams;
  const opcoes = await lerJava<OpcoesTarefas>(usuario.permissions.includes("tasks.view") ? "/tasks/options" : usuario.permissions.includes("projects.view") ? "/projects/options" : usuario.permissions.includes("meetings.view") ? "/meetings/options" : "/events/options");
  return <ShellWorkspace><Calendario usuarioId={usuario.id} permissoes={usuario.permissions} equipes={opcoes.equipes}
    dataInicial={typeof parametros.date === "string" ? parametros.date : undefined}
    visaoInicial={typeof parametros.view === "string" ? parametros.view : undefined} /></ShellWorkspace>;
}
