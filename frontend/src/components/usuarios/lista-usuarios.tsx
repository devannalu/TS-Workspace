"use client";
import type { UsuarioGerenciado } from "@/lib/api/usuarios";
import type { PaginaApi } from "@/lib/api/contratos";
import { Button } from "../ui/button";
import { Avatar } from "../ui/avatar";
import { Badge } from "../ui/badge";
import { EmptyState } from "../ui/feedback";
import { MoreHorizontal, Pencil, UserRoundMinus } from "lucide-react";
export function ListaUsuarios({
  paginaUsuarios,
  podeEditar,
  podeInativar,
  escolherAcaoUsuario,
}: {
  paginaUsuarios: PaginaApi<UsuarioGerenciado>;
  podeEditar: boolean;
  podeInativar: boolean;
  escolherAcaoUsuario: (usuario: UsuarioGerenciado, acao: "edit" | "status") => void;
}) {
  const acoesUsuario = (user: UsuarioGerenciado) =>
    (podeEditar || podeInativar) && (
      <details
        className="relative"
        onKeyDown={(event) => {
          if (event.key === "Escape") {
            event.currentTarget.open = false;
            event.currentTarget.querySelector("summary")?.focus();
          }
        }}
      >
        <summary
          aria-label={`Ações de ${user.name}`}
          className="inline-flex size-11 list-none items-center justify-center rounded-xl hover:bg-muted"
        >
          <MoreHorizontal size={19} aria-hidden />
        </summary>
        <div className="absolute right-0 z-10 w-48 rounded-xl border border-border bg-card p-1 shadow-lg">
          {podeEditar && (
            <Button
              variant="ghost"
              className="w-full justify-start"
              onClick={(event) => {
                event.currentTarget.closest("details")?.removeAttribute("open");
                event.currentTarget
                  .closest("details")
                  ?.querySelector("summary")
                  ?.focus();
                escolherAcaoUsuario(user, "edit");
              }}
            >
              <Pencil size={15} aria-hidden />
              Editar acesso e equipes
            </Button>
          )}
          {podeInativar && (
            <Button
              variant="ghost"
              className="w-full justify-start"
              onClick={(event) => {
                event.currentTarget.closest("details")?.removeAttribute("open");
                event.currentTarget
                  .closest("details")
                  ?.querySelector("summary")
                  ?.focus();
                escolherAcaoUsuario(user, "status");
              }}
            >
              <UserRoundMinus size={15} aria-hidden />
              {user.status === "ACTIVE" ? "Inativar" : "Reativar"}
            </Button>
          )}
        </div>
      </details>
    );
  return (
    <>
      {!paginaUsuarios.items.length ? (
        <EmptyState
          title="Nenhuma usuária encontrada"
          description="Experimente ajustar os filtros ou limpar a busca."
        />
      ) : (
        <>
          <div className="hidden xl:block">
            <table className="w-full table-fixed text-left text-sm break-words">
              <caption className="sr-only">Usuárias do Workspace</caption>
              <thead className="border-b border-border text-xs text-muted-foreground">
                <tr>
                  {[
                    "Usuária",
                    "Cargo",
                    "Perfil",
                    "Equipes",
                    "Status",
                    "Ações",
                  ].map((t) => (
                    <th
                      key={t}
                      scope="col"
                      className={`px-3 py-3 font-medium ${t === "Usuária" ? "w-[28%]" : t === "Ações" ? "w-16" : ""}`}
                    >
                      {t}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {paginaUsuarios.items.map((user) => (
                  <tr
                    key={user.id}
                    className="border-b border-border-soft last:border-0"
                  >
                    <td className="px-3 py-4">
                      <div className="flex items-center gap-3">
                        <Avatar name={user.name} />
                        <div className="min-w-0 flex-1">
                          <p className="font-medium">{user.name}</p>
                          <p className="break-all text-xs text-muted-foreground">
                            {user.email}
                          </p>
                        </div>
                      </div>
                    </td>
                    <td className="px-3 py-4 text-muted-foreground">
                      {user.jobTitle || "—"}
                    </td>
                    <td className="px-3 py-4">
                      <Badge>{user.role.name}</Badge>
                    </td>
                    <td className="max-w-52 px-3 py-4 text-muted-foreground">
                      {user.teams.map((t) => t.name).join(", ") || "Sem equipe"}
                    </td>
                    <td className="px-3 py-4">
                      <Badge
                        tone={user.status === "ACTIVE" ? "success" : "neutral"}
                      >
                        {user.status === "ACTIVE" ? "Ativa" : "Inativa"}
                      </Badge>
                    </td>
                    <td className="px-3 py-4">{acoesUsuario(user)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="space-y-3 xl:hidden">
            {paginaUsuarios.items.map((user) => (
              <article
                key={user.id}
                className="rounded-xl border border-border p-4"
              >
                <div className="flex items-start gap-3">
                  <Avatar name={user.name} />
                  <div className="min-w-0 flex-1">
                    <h3 className="font-medium">{user.name}</h3>
                    <p className="break-all text-xs text-muted-foreground">
                      {user.email}
                    </p>
                  </div>
                  {acoesUsuario(user)}
                </div>
                <div className="mt-3 flex gap-2">
                  <Badge>{user.role.name}</Badge>
                  <Badge
                    tone={user.status === "ACTIVE" ? "success" : "neutral"}
                  >
                    {user.status === "ACTIVE" ? "Ativa" : "Inativa"}
                  </Badge>
                </div>
                <p className="mt-3 subtle">
                  {user.jobTitle || "Sem cargo descritivo"}
                </p>
                <p className="text-xs text-muted-foreground">
                  {user.teams.map((t) => t.name).join(", ") ||
                    "Sem equipes ativas"}
                </p>
              </article>
            ))}
          </div>
        </>
      )}
    </>
  );
}
