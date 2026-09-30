"use client";

import { useState, useTransition } from "react";
import { createInviteAction } from "@/app/usuarias/actions";
import { Button } from "./ui/button";
import { Input } from "./ui/input";

type Option = { id: string; name: string };
export function InviteForm({ roles, teams }: { roles: Option[]; teams: Option[] }) {
  const [pending, start] = useTransition();
  const [message, setMessage] = useState("");
  const [url, setUrl] = useState("");
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setMessage(""); setUrl("");
    const form = new FormData(event.currentTarget);
    start(async () => {
      const result = await createInviteAction({ email: String(form.get("email")).trim(), roleId: String(form.get("roleId")), teamIds: form.getAll("teamIds").map(String) });
      if (!result.ok) setMessage(result.error); else { setUrl(result.url); setMessage("Convite criado. Copie o link agora; ele não será exibido novamente."); event.currentTarget.reset(); }
    });
  }
  return <form onSubmit={submit} className="space-y-4 rounded-2xl border border-border bg-card p-5" aria-busy={pending}>
    <h3 className="text-lg font-semibold">Novo convite</h3>
    <div className="space-y-2"><label htmlFor="invite-email" className="text-sm font-medium">E-mail</label><Input id="invite-email" name="email" type="email" required disabled={pending} /></div>
    <div className="space-y-2"><label htmlFor="invite-role" className="text-sm font-medium">Cargo</label><select id="invite-role" name="roleId" required disabled={pending} className="min-h-12 w-full rounded-xl border border-border bg-card px-4 py-3"><option value="">Selecione</option>{roles.map(role => <option key={role.id} value={role.id}>{role.name}</option>)}</select></div>
    <fieldset className="space-y-2"><legend className="text-sm font-medium">Equipes</legend><div className="grid gap-2 sm:grid-cols-2">{teams.map(team => <label key={team.id} className="flex items-center gap-2 text-sm"><input type="checkbox" name="teamIds" value={team.id} disabled={pending} />{team.name}</label>)}</div></fieldset>
    {message && <p role={url ? "status" : "alert"} className="text-sm leading-6">{message}</p>}
    {url && <div className="space-y-2"><label htmlFor="invite-url" className="text-sm font-medium">Link de convite</label><Input id="invite-url" readOnly value={url} onFocus={event => event.currentTarget.select()} /></div>}
    <Button type="submit" disabled={pending}>{pending ? "Criando…" : "Criar convite"}</Button>
  </form>;
}
