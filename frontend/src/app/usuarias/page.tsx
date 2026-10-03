import { ShellWorkspace } from "@/components/layout/shell-workspace";
import { Card } from "@/components/ui/card";
import { ErrorState } from "@/components/ui/feedback";
import { FormularioConvite } from "@/components/convites/formulario-convite";
import { GestaoUsuarios } from "@/components/usuarios/gestao-usuarios";
import { ListaConvites } from "@/components/convites/lista-convites";
import { lerJava } from "@/lib/api/server";
import { exigirSessao } from "@/lib/sessao";
import { normalizarPagina } from "@/lib/ui/formatacao";
import type { UsuarioGerenciado } from "@/lib/api/usuarios";
import type { Convite } from "@/lib/api/convites";
import type { PaginaApi, PerfilAcessoReferencia, EquipeReferencia } from "@/lib/api/contratos";
export const metadata = { title: "Usuárias" };
export default async function UsuariosPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const usuarioAtual = await exigirSessao(),
    parametrosBusca = await searchParams,
    possuiPermissao = (chavePermissao: string) => usuarioAtual.permissions.includes(chavePermissao);
  if (!possuiPermissao("users.view"))
    return (
      <ShellWorkspace>
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </ShellWorkspace>
    );
  const filtrosUsuarios = new URLSearchParams({
    page: String(normalizarPagina(parametrosBusca.page)),
    size: "20",
  });
  for (const key of ["search", "status", "roleId", "teamId"])
    if (typeof parametrosBusca[key] === "string" && parametrosBusca[key])
      filtrosUsuarios.set(key, parametrosBusca[key]);
  const [paginaUsuarios, opcoesUsuarios, paginaConvites] = await Promise.all([
    lerJava<PaginaApi<UsuarioGerenciado>>("/users?" + filtrosUsuarios),
    lerJava<{ roles: PerfilAcessoReferencia[]; teams: EquipeReferencia[] }>("/users/options"),
    lerJava<PaginaApi<Convite>>(
      "/invites?page=" + normalizarPagina(parametrosBusca.invitePage) + "&size=10",
    ),
  ]);
  return (
    <ShellWorkspace>
      <div className="space-y-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="page-title">Usuárias</h1>
            <p className="mt-2 subtle">
              Gerencie integrantes e acessos do Workspace.
            </p>
          </div>
          {possuiPermissao("users.create") && (
            <FormularioConvite
              perfisAcesso={opcoesUsuarios.roles}
              equipes={opcoesUsuarios.teams}
              inicialmenteAberto={parametrosBusca.convidar === "1"}
            />
          )}
        </div>
        <Card className="p-4 sm:p-6">
          <h2 className="section-title">Integrantes</h2>
          <div className="mt-5">
            <GestaoUsuarios
              paginaUsuarios={paginaUsuarios}
              perfisAcesso={opcoesUsuarios.roles}
              equipes={opcoesUsuarios.teams}
              podeEditar={possuiPermissao("users.edit")}
              podeInativar={possuiPermissao("users.disable")}
            />
          </div>
        </Card>
        <Card className="p-4 sm:p-6">
          <h2 className="section-title">Convites</h2>
          <p className="mt-1 subtle">
            Acompanhe os próximos acessos ao Workspace.
          </p>
          <div className="mt-5">
            <ListaConvites paginaConvites={paginaConvites} podeCancelar={possuiPermissao("users.create")} />
          </div>
        </Card>
      </div>
    </ShellWorkspace>
  );
}
