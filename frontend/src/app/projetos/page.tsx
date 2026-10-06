import { exigirSessao } from "@/lib/sessao";
import { lerJava } from "@/lib/api/server";
import type { OpcoesProjetos } from "@/lib/api/projetos";
import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { ListaProjetos } from "@/components/projetos/lista-projetos";
import { ErrorState } from "@/components/ui/feedback";
export const metadata = { title: "Projetos" };
export default async function ProjetosPage() {
  const usuario = await exigirSessao();
  if (!usuario.permissions.includes("projects.view"))
    return (
      <ShellWorkspace>
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </ShellWorkspace>
    );
  const opcoes = await lerJava<OpcoesProjetos>("/projects/options");
  return (
    <ShellWorkspace>
      <ListaProjetos
        usuarioId={usuario.id}
        permissoes={usuario.permissions}
        opcoes={opcoes}
      />
    </ShellWorkspace>
  );
}
