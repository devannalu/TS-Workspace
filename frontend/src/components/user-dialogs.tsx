"use client";
import {
  editJavaUser,
  deactivateJavaUser,
  activateJavaUser,
  type JavaManagedUser,
} from "@/lib/api/users";
import type { JavaRoleRef, JavaTeamRef } from "@/lib/api/management";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Dialog } from "./ui/dialog";
import { Shield } from "lucide-react";
export function UserDialogs({
  selected,
  mode,
  roles,
  teams,
  pending,
  error,
  close,
  updateUserAndRefresh,
}: {
  selected: JavaManagedUser | null;
  mode: "edit" | "status" | null;
  roles: JavaRoleRef[];
  teams: JavaTeamRef[];
  pending: boolean;
  error: string;
  close: () => void;
  updateUserAndRefresh: (
    mutation: () => Promise<unknown>,
    success: string,
  ) => void;
}) {
  return (
    <>
      <Dialog
        open={mode === "edit"}
        onClose={close}
        busy={pending}
        title="Editar usuária"
        description="Atualize cargo, perfil de acesso e equipes. O e-mail permanece o mesmo."
      >
        {selected && (
          <form
            key={selected.id}
            className="space-y-4"
            aria-busy={pending}
            onSubmit={(event) => {
              event.preventDefault();
              const form = new FormData(event.currentTarget);
              updateUserAndRefresh(
                () =>
                  editJavaUser(selected.id, {
                    jobTitle: String(form.get("jobTitle")),
                    roleId: String(form.get("roleId")),
                    teamIds: form.getAll("teamIds").map(String),
                  }),
                "Usuária atualizada.",
              );
            }}
          >
            <p className="font-medium">{selected.name}</p>
            <label className="field">
              Cargo descritivo
              <Input
                name="jobTitle"
                defaultValue={selected.jobTitle ?? ""}
                maxLength={160}
                disabled={pending}
              />
            </label>
            <label className="field">
              Perfil de acesso
              <select
                name="roleId"
                defaultValue={selected.role.id}
                required
                disabled={pending}
              >
                {roles.map((r) => (
                  <option key={r.id} value={r.id}>
                    {r.name}
                  </option>
                ))}
              </select>
            </label>
            <fieldset>
              <legend className="mb-2 text-sm font-medium">Equipes</legend>
              <div className="space-y-2">
                {teams.map((t) => (
                  <label key={t.id} className="flex items-center gap-2 text-sm">
                    <input
                      type="checkbox"
                      name="teamIds"
                      value={t.id}
                      defaultChecked={selected.teams.some(
                        (item) => item.id === t.id,
                      )}
                      disabled={pending}
                    />
                    {t.name}
                  </label>
                ))}
              </div>
            </fieldset>
            {error && (
              <p role="alert" className="feedback-error">
                {error}
              </p>
            )}
            <div className="flex justify-end gap-2">
              <Button variant="secondary" onClick={close} disabled={pending}>
                Cancelar
              </Button>
              <Button type="submit" disabled={pending}>
                {pending ? "Salvando…" : "Salvar alterações"}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Dialog
        open={mode === "status"}
        onClose={close}
        busy={pending}
        title={
          selected?.status === "ACTIVE"
            ? "Inativar usuária"
            : "Reativar usuária"
        }
        description={
          selected?.status === "ACTIVE"
            ? "A usuária perderá acesso e suas sessões serão encerradas."
            : "A usuária poderá entrar novamente com suas credenciais."
        }
      >
        <div className="flex items-center gap-3">
          <Shield size={20} aria-hidden />
          <p className="font-medium">{selected?.name}</p>
        </div>
        {error && (
          <p role="alert" className="mt-4 feedback-error">
            {error}
          </p>
        )}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={close} disabled={pending}>
            Voltar
          </Button>
          <Button
            variant={selected?.status === "ACTIVE" ? "danger" : "primary"}
            disabled={pending}
            onClick={() =>
              selected &&
              updateUserAndRefresh(
                () =>
                  selected.status === "ACTIVE"
                    ? deactivateJavaUser(selected.id)
                    : activateJavaUser(selected.id),
                selected.status === "ACTIVE"
                  ? "Usuária inativada."
                  : "Usuária reativada.",
              )
            }
          >
            {pending
              ? "Salvando…"
              : selected?.status === "ACTIVE"
                ? "Confirmar inativação"
                : "Confirmar reativação"}
          </Button>
        </div>
      </Dialog>
    </>
  );
}
