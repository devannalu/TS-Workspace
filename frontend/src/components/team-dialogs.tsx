"use client";
import { useState, useTransition, useEffect } from "react";
import {
  createTeamAction,
  updateTeamAction,
  archiveTeamAction,
  addTeamMemberAction,
  removeTeamMemberAction,
} from "@/lib/api/ui-actions";
import {
  getJavaTeam,
  type JavaTeam,
  type JavaTeamDetail,
} from "@/lib/api/teams";
import { listJavaUsers } from "@/lib/api/users";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Dialog } from "./ui/dialog";
export type Team = Pick<
  JavaTeam,
  "id" | "name" | "description" | "parentId" | "memberCount"
>;
type User = { id: string; name: string; email: string };
export function TeamDialogs({
  mode,
  selected,
  teams,
  users,
  onClose,
  onSaved,
}: {
  mode: "create" | "edit" | "members" | "archive" | null;
  selected: Team | null;
  teams: Team[];
  users: User[];
  onClose: () => void;
  onSaved: (message: string) => void;
}) {
  const [pending, start] = useTransition();
  const [error, setError] = useState("");
  const [availableUsers, setAvailableUsers] = useState(users);
  const [members, setMembers] = useState<JavaTeamDetail["members"]>([]);
  const [loadingMembers, setLoadingMembers] = useState(mode === "members");
  useEffect(() => {
    if (mode !== "members" || !selected) return;
    let active = true;
    getJavaTeam(selected.id)
      .then((detail) => {
        if (active) setMembers(detail.members);
      })
      .catch((error) => {
        if (active)
          setError(
            error instanceof Error
              ? error.message
              : "Não foi possível carregar integrantes.",
          );
      })
      .finally(() => {
        if (active) setLoadingMembers(false);
      });
    return () => {
      active = false;
    };
  }, [mode, selected]);
  const current = selected ? { ...selected, members } : null;
  const root = teams.find((team) => !team.parentId);
  const close = () => {
    if (!pending) onClose();
  };
  const saveTeamChange = (
    mutation: () => Promise<{ ok: boolean; error?: string }>,
    success: string,
  ) =>
    start(async () => {
      setError("");
      const saved = await mutation();
      if (!saved.ok) {
        setError(saved.error ?? "Não foi possível salvar a alteração.");
        return;
      }
      if (mode === "members" && selected) {
        try {
          const detail = await getJavaTeam(selected.id);
          setMembers(detail.members);
        } catch (error) {
          setError(
            error instanceof Error
              ? error.message
              : "Atualização salva, mas não foi possível recarregar integrantes.",
          );
        }
      } else onClose();
      onSaved(success);
    });
  return (
    <>
      <Dialog
        open={mode === "create" || mode === "edit"}
        onClose={close}
        busy={pending}
        title={mode === "edit" ? "Editar equipe" : "Nova equipe"}
        description="Defina o nome e a posição da equipe na organização."
      >
        {(mode === "create" || current) && (
          <form
            key={current?.id ?? "create"}
            className="space-y-4"
            aria-busy={pending}
            onSubmit={(event) => {
              event.preventDefault();
              const form = new FormData(event.currentTarget),
                input = {
                  name: String(form.get("name")).trim(),
                  description: String(form.get("description")),
                  parentId: String(form.get("parentId")),
                };
              saveTeamChange(
                () =>
                  mode === "edit" && current
                    ? updateTeamAction({ teamId: current.id, ...input })
                    : createTeamAction(input),
                mode === "edit" ? "Equipe atualizada." : "Equipe criada.",
              );
            }}
          >
            <label className="field">
              Nome
              <Input
                name="name"
                defaultValue={mode === "edit" ? current?.name : ""}
                required
                maxLength={100}
                minLength={2}
                disabled={pending}
              />
            </label>
            <label className="field">
              Descrição
              <Input
                name="description"
                defaultValue={
                  mode === "edit" ? (current?.description ?? "") : ""
                }
                maxLength={500}
                disabled={pending}
              />
            </label>
            <label className="field">
              Equipe superior
              <select
                name="parentId"
                defaultValue={
                  mode === "edit" ? (current?.parentId ?? root?.id) : root?.id
                }
                required
                disabled={pending}
              >
                {teams
                  .filter((t) => t.id !== current?.id)
                  .map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.name}
                    </option>
                  ))}
              </select>
            </label>
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
                {pending ? "Salvando…" : "Salvar equipe"}
              </Button>
            </div>
          </form>
        )}
      </Dialog>
      <Dialog
        open={mode === "archive"}
        onClose={close}
        busy={pending}
        title="Arquivar equipe"
        description="A equipe será arquivada e não poderá ser usada como equipe superior. Resolva as equipes filhas ativas primeiro."
      >
        <p className="font-medium">{current?.name}</p>
        {error && (
          <p role="alert" className="mt-3 feedback-error">
            {error}
          </p>
        )}
        <div className="mt-5 flex justify-end gap-2">
          <Button variant="secondary" onClick={close} disabled={pending}>
            Voltar
          </Button>
          <Button
            variant="danger"
            disabled={pending}
            onClick={() =>
              current &&
              saveTeamChange(
                () => archiveTeamAction(current.id),
                "Equipe arquivada.",
              )
            }
          >
            {pending ? "Arquivando…" : "Confirmar arquivamento"}
          </Button>
        </div>
      </Dialog>
      <Dialog
        open={mode === "members"}
        onClose={close}
        busy={pending}
        title={`Integrantes · ${current?.name ?? ""}`}
        description="Organize quem participa desta equipe."
      >
        {loadingMembers ? (
          <p role="status" className="subtle">
            Carregando integrantes…
          </p>
        ) : (
          current && (
            <div className="space-y-4">
              <ul className="space-y-2">
                {current.members.map((user) => (
                  <li
                    key={user.id}
                    className="flex items-center justify-between gap-2 rounded-xl border border-border p-3"
                  >
                    <div className="min-w-0">
                      <p className="text-sm font-medium">{user.name}</p>
                      <p className="break-all text-xs text-muted-foreground">
                        {user.email}
                      </p>
                    </div>
                    <Button
                      variant="ghost"
                      disabled={pending}
                      aria-label={`Remover ${user.name}`}
                      onClick={() =>
                        saveTeamChange(
                          () => removeTeamMemberAction(user.id, current.id),
                          "Integrante removida.",
                        )
                      }
                    >
                      Remover
                    </Button>
                  </li>
                ))}
              </ul>
              {!current.members.length && (
                <p className="subtle">Esta equipe ainda não tem integrantes.</p>
              )}
              <form
                className="flex gap-2"
                onSubmit={(event) => {
                  event.preventDefault();
                  const search = String(
                    new FormData(event.currentTarget).get("search"),
                  );
                  start(async () => {
                    try {
                      const page = await listJavaUsers({
                        status: "ACTIVE",
                        size: 25,
                        search,
                      });
                      setAvailableUsers(page.items);
                      setError("");
                    } catch (e) {
                      setError(
                        e instanceof Error
                          ? e.message
                          : "Não foi possível buscar integrantes.",
                      );
                    }
                  });
                }}
              >
                <label className="field flex-1">
                  Buscar integrante
                  <Input
                    name="search"
                    placeholder="Nome ou e-mail"
                    disabled={pending}
                  />
                </label>
                <Button
                  variant="secondary"
                  type="submit"
                  disabled={pending}
                  className="self-end"
                >
                  Buscar
                </Button>
              </form>
              <form
                className="space-y-3"
                onSubmit={(event) => {
                  event.preventDefault();
                  const form = new FormData(event.currentTarget);
                  saveTeamChange(
                    () =>
                      addTeamMemberAction(
                        String(form.get("userId")),
                        current.id,
                      ),
                    "Integrante adicionada.",
                  );
                }}
              >
                <label className="field">
                  Adicionar integrante
                  <select name="userId" required disabled={pending}>
                    <option value="">Selecione uma usuária ativa</option>
                    {availableUsers
                      .filter(
                        (u) =>
                          !current.members.some((member) => member.id === u.id),
                      )
                      .map((u) => (
                        <option key={u.id} value={u.id}>
                          {u.name}
                        </option>
                      ))}
                  </select>
                </label>
                <Button type="submit" disabled={pending}>
                  Adicionar
                </Button>
              </form>
              {error && (
                <p role="alert" className="feedback-error">
                  {error}
                </p>
              )}
            </div>
          )
        )}
      </Dialog>
    </>
  );
}
