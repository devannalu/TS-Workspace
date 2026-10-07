import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesTarefas } from "@/lib/api/tarefas";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { QuadroTarefas } from "@/components/tarefas/quadro-tarefas";
import { ErrorState } from "@/components/ui/feedback";
export const metadata = { title: "Tarefas" };
export default async function TarefasPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const usuario = await exigirSessao();
  if (!usuario.permissions.includes("tasks.view"))
    return (
      <ShellWorkspace>
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </ShellWorkspace>
    );
  const parametros = await searchParams;
  const opcoes = await lerJava<OpcoesTarefas>("/tasks/options");
  return (
    <ShellWorkspace>
      <QuadroTarefas
        abrirInicial={typeof parametros.abrir === "string" ? parametros.abrir : undefined}
        usuarioId={usuario.id}
        permissoes={usuario.permissions}
        opcoesIniciais={opcoes}
        minhasInicial={parametros.minhas === "1"}
        projetoInicial={
          typeof parametros.projetoId === "string"
            ? parametros.projetoId
            : undefined
        }
      />
    </ShellWorkspace>
  );
}
