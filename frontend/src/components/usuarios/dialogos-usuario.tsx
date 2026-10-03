"use client";
import {
  editarUsuario,
  inativarUsuario,
  reativarUsuario,
  type UsuarioGerenciado,
} from "@/lib/api/usuarios";
import type { PerfilAcessoReferencia, EquipeReferencia } from "@/lib/api/contratos";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Dialog } from "../ui/dialog";
import { Shield } from "lucide-react";
export function DialogosUsuario({
  usuarioSelecionado,
  modo,
  perfisAcesso,
  equipes,
  salvando,
  mensagemErro,
  fecharDialogo,
  atualizarUsuarioERecarregar,
}: {
  usuarioSelecionado: UsuarioGerenciado | null;
  modo: "edit" | "status" | null;
  perfisAcesso: PerfilAcessoReferencia[];
  equipes: EquipeReferencia[];
  salvando: boolean;
  mensagemErro: string;
  fecharDialogo: () => void;
  atualizarUsuarioERecarregar: (
    mutacao: () => Promise<unknown>,
    mensagemSucesso: string,
  ) => void;
}) {
  return (
    <>
      <Dialog
        open={modo === "edit"}
        onClose={fecharDialogo}
        busy={salvando}
        title="Editar usuária"
        description="Atualize cargo, perfil de acesso e equipes. O e-mail permanece o mesmo."
      >
        {usuarioSelecionado && (
          <form
            key={usuarioSelecionado.id}
            className="space-y-4"
            aria-busy={salvando}
            onSubmit={(event) => {
              event.preventDefault();
              const dadosFormulario = new FormData(event.currentTarget);
              atualizarUsuarioERecarregar(
                () =>
                  editarUsuario(usuarioSelecionado.id, {
                    jobTitle: String(dadosFormulario.get("jobTitle")),
                    roleId: String(dadosFormulario.get("roleId")),
                    teamIds: dadosFormulario.getAll("teamIds").map(String),
                  }),
                "Usuária atualizada.",
              );
            }}
          >
            <p className="font-medium">{usuarioSelecionado.name}</p>
            <label className="field">
              Cargo descritivo
              <Input
                name="jobTitle"
                defaultValue={usuarioSelecionado.jobTitle ?? ""}
                maxLength={160}
                disabled={salvando}
              />
            </label>
            <label className="field">
              Perfil de acesso
              <select
                name="roleId"
                defaultValue={usuarioSelecionado.role.id}
                required
                disabled={salvando}
              >
                {perfisAcesso.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
            <fieldset>
              <legend className="mb-2 text-sm font-medium">Equipes</legend>
              <div className="space-y-2">
                {equipes.map((t) => (
                  <label key={t.id} className="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      name="teamIds"
                      value={t.id}
                      defaultChecked={usuarioSelecionado.teams.some(
                        (item) => item.id === t.id,
                      )}
                      disabled={salvando}
                    />
                    {t.name}
                  </label>
                ))}
              </div>
            </fieldset>
            {mensagemErro && (
              <p role="alert" className="feedback-error">
                {mensagemErro}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={fecharDialogo} disabled={salvando}>
                Cancelar
              </Button>
              <Button type="submit" disabled={salvando}>
                {salvando ? "Salvando…" : "Salvar alterações"}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Dialog
        open={modo === "status"}
        onClose={fecharDialogo}
        busy={salvando}
        title={
          usuarioSelecionado?.status === "ACTIVE"
            ? "Inativar usuária"
            : "Reativar usuária"
        }
        description={
          usuarioSelecionado?.status === "ACTIVE"
            ? "A usuária perderá acesso e suas sessões serão encerradas."
            : "A usuária poderá entrar novamente com suas credenciais."
        }
      >
        <div className="flex items-center gap-3">
          <Shield size={20} aria-hidden />
          <p className="font-medium">{usuarioSelecionado?.name}</p>
        </div>
        {mensagemErro && (
          <p role="alert" className="mt-4 feedback-error">
            {mensagemErro}
          </p>
        )}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={fecharDialogo} disabled={salvando}>
            Voltar
          </Button>
          <Button
            variant={usuarioSelecionado?.status === "ACTIVE" ? "danger" : "primary"}
            disabled={salvando}
            onClick={() =>
              usuarioSelecionado &&
              atualizarUsuarioERecarregar(
                () =>
                  usuarioSelecionado.status === "ACTIVE"
                    ? inativarUsuario(usuarioSelecionado.id)
                    : reativarUsuario(usuarioSelecionado.id),
                usuarioSelecionado.status === "ACTIVE"
                  ? "Usuária inativada."
                  : "Usuária reativada.",
              )
            }
          >
            {salvando
              ? "Salvando…"
              : usuarioSelecionado?.status === "ACTIVE"
                ? "Confirmar inativação"
                : "Confirmar reativação"}
          </Button>
        </div>
      </Dialog>
    </>
  );
}
