import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { GestaoEquipes } from "@/components/equipes/gestao-equipes";
import { lerJava } from "@/lib/api/server";
import { exigirSessao } from "@/lib/sessao";
import type { Equipe } from "@/lib/api/equipes";
import type { UsuarioGerenciado } from "@/lib/api/usuarios";
import type { PaginaApi } from "@/lib/api/contratos";
import { ErrorState } from "@/components/ui/feedback";
export const metadata = { title: "Equipes" };
export default async function EquipesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const usuarioAtual = await exigirSessao(),
    parametrosBusca = await searchParams,
    possuiPermissao = (chavePermissao: string) => usuarioAtual.permissions.includes(chavePermissao);
  if (!possuiPermissao("teams.view"))
    return (
      <ShellWorkspace>
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </ShellWorkspace>
    );
  const equipes = await lerJava<Equipe[]>("/teams");
  const usuarios =
    possuiPermissao("teams.manage_members") && possuiPermissao("users.view")
      ? (
          await lerJava<PaginaApi<UsuarioGerenciado>>(
            "/users?status=ACTIVE&size=25",
          )
        ).items
      : [];
  return (
    <ShellWorkspace>
      <GestaoEquipes
        equipes={equipes}
        usuarios={usuarios}
        podeCriar={possuiPermissao("teams.create")}
        podeEditar={possuiPermissao("teams.edit")}
        podeArquivar={possuiPermissao("teams.archive")}
        podeGerenciarIntegrantes={possuiPermissao("teams.manage_members")}
        abrirCriacaoInicial={parametrosBusca.nova === "1"}
      />
    </ShellWorkspace>
  );
}
