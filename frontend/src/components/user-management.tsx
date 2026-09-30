"use client";

import { useState, useTransition } from "react";
import { updateUserAction } from "@/app/usuarias/actions";
import { Button } from "./ui/button";

type UserRow = { id: string; name: string; email: string; profile: { status: "active" | "inactive"; jobTitle: string | null; role: { id: string; name: string } } | null; memberships: { team: { id: string; name: string } }[] };
type Option = { id: string; name: string };
export function UserManagement({ users, roles, teams }: { users: UserRow[]; roles: Option[]; teams: Option[] }) {
  const [message, setMessage] = useState(""); const [pending, start] = useTransition();
  return <div className="space-y-4">{users.map(user => <article key={user.id} className="rounded-2xl border border-border bg-card p-5">
    <div className="flex flex-wrap items-start justify-between gap-3"><div><h3 className="font-semibold">{user.name}</h3><p className="text-sm text-muted-foreground">{user.email}</p></div><span className="rounded-full bg-muted px-3 py-1 text-xs">{user.profile?.status === "active" ? "Ativa" : "Inativa"}</span></div>
    <form className="mt-5 grid gap-4 md:grid-cols-3" onSubmit={event => { event.preventDefault(); const form = new FormData(event.currentTarget); start(async () => { const result = await updateUserAction({ userId: user.id, roleId: String(form.get("roleId")), status: String(form.get("status")) as "active" | "inactive", teamIds: form.getAll("teamIds").map(String) }); setMessage(result.ok ? "Usuária atualizada." : result.error); }); }}>
      <label className="space-y-2 text-sm"><span>Cargo</span><select name="roleId" defaultValue={user.profile?.role.id} className="min-h-11 w-full rounded-xl border border-border bg-card px-3" disabled={pending}>{roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}</select></label>
      <label className="space-y-2 text-sm"><span>Status</span><select name="status" defaultValue={user.profile?.status ?? "inactive"} className="min-h-11 w-full rounded-xl border border-border bg-card px-3" disabled={pending}><option value="active">Ativa</option><option value="inactive">Inativa</option></select></label>
      <fieldset className="space-y-2 text-sm"><legend>Equipes</legend><div className="flex flex-wrap gap-2">{teams.map(team => <label key={team.id} className="flex items-center gap-1"><input type="checkbox" name="teamIds" value={team.id} defaultChecked={user.memberships.some(member => member.team.id === team.id)} disabled={pending} />{team.name}</label>)}</div></fieldset>
      <Button type="submit" disabled={pending} className="md:col-span-3 md:justify-self-start">{pending ? "Salvando…" : "Salvar alterações"}</Button>
    </form>
  </article>)}{!users.length && <p className="rounded-2xl border border-dashed border-border p-8 text-center text-sm text-muted-foreground">Nenhuma usuária encontrada.</p>}{message && <p role="status" className="text-sm">{message}</p>}</div>;
}
