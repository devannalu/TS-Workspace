"use client";
import { useState, useTransition, useCallback } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { Search } from "lucide-react";
import type { JavaManagedUser } from "@/lib/api/users";
import { UserList } from "./user-list";
import { UserDialogs } from "./user-dialogs";
import type { JavaPage, JavaRoleRef, JavaTeamRef } from "@/lib/api/management";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Pagination } from "./ui/pagination";
import { Toast } from "./ui/feedback";
export function UserManagement({
  data,
  roles,
  teams,
  canEdit,
  canDisable,
}: {
  data: JavaPage<JavaManagedUser>;
  roles: JavaRoleRef[];
  teams: JavaTeamRef[];
  canEdit: boolean;
  canDisable: boolean;
}) {
  const router = useRouter(),
    query = useSearchParams(),
    [pending, start] = useTransition(),
    [selected, setSelected] = useState<JavaManagedUser | null>(null),
    [mode, setMode] = useState<"edit" | "status" | null>(null),
    [message, setMessage] = useState(""),
    [error, setError] = useState("");
  const dismiss = useCallback(() => setMessage(""), []);
  const updateQuery = (values: Record<string, string>) => {
    const next = new URLSearchParams(query.toString());
    for (const [key, val] of Object.entries(values)) {
      if (val) next.set(key, val);
      else next.delete(key);
    }
    router.push("/usuarias?" + next);
  };
  const close = () => {
    if (!pending) {
      setSelected(null);
      setMode(null);
      setError("");
    }
  };
  const updateUserAndRefresh = (
    mutation: () => Promise<unknown>,
    success: string,
  ) =>
    start(async () => {
      try {
        setError("");
        await mutation();
        setSelected(null);
        setMode(null);
        setMessage(success);
        router.refresh();
      } catch (e) {
        setError(
          e instanceof Error
            ? e.message
            : "Não foi possível concluir a operação.",
        );
      }
    });
  const choose = (user: JavaManagedUser, action: typeof mode) => {
    setError("");
    setSelected(user);
    setMode(action);
  };
  return (
    <div className="space-y-5">
      <form
        key={query.toString()}
        className="grid gap-3 rounded-xl bg-muted p-4 sm:grid-cols-2 xl:grid-cols-[1.6fr_1fr_1fr_1fr_auto]"
        aria-label="Filtros de usuárias"
        onSubmit={(event) => {
          event.preventDefault();
          const form = new FormData(event.currentTarget);
          updateQuery({
            search: String(form.get("search")).trim(),
            status: String(form.get("status")),
            roleId: String(form.get("roleId")),
            teamId: String(form.get("teamId")),
            page: "0",
          });
        }}
      >
        <label className="field">
          Busca
          <Input
            name="search"
            defaultValue={query.get("search") ?? ""}
            placeholder="Nome ou e-mail"
            maxLength={120}
          />
        </label>
        <label className="field">
          Status
          <select name="status" defaultValue={query.get("status") ?? ""}>
            <option value="">Todos</option>
            <option value="ACTIVE">Ativa</option>
            <option value="INACTIVE">Inativa</option>
          </select>
        </label>
        <label className="field">
          Perfil
          <select name="roleId" defaultValue={query.get("roleId") ?? ""}>
            <option value="">Todos</option>
            {roles.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
        </label>
        <label className="field">
          Equipe
          <select name="teamId" defaultValue={query.get("teamId") ?? ""}>
            <option value="">Todas</option>
            {teams.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name}
              </option>
            ))}
          </select>
        </label>
        <div className="flex items-end gap-2">
          <Button type="submit" aria-label="Aplicar filtros">
            <Search size={17} aria-hidden />
          </Button>
          <Button
            variant="ghost"
            onClick={() =>
              updateQuery({
                search: "",
                status: "",
                roleId: "",
                teamId: "",
                page: "0",
              })
            }
          >
            Limpar
          </Button>
        </div>
      </form>
      <UserList
        userPage={data}
        canEdit={canEdit}
        canDisable={canDisable}
        choose={choose}
      />
      <Pagination
        page={data.page}
        size={data.size}
        total={data.total}
        label="usuárias"
        onPage={(page) => updateQuery({ page: String(page) })}
      />
      <UserDialogs
        selected={selected}
        mode={mode}
        roles={roles}
        teams={teams}
        pending={pending}
        error={error}
        close={close}
        updateUserAndRefresh={updateUserAndRefresh}
      />
      <Toast message={message} onClose={dismiss} />
    </div>
  );
}
