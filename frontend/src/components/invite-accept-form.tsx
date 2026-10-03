"use client";

import { useState, useTransition } from "react";
import { useRouter } from "next/navigation";
import { acceptInviteAction } from "@/lib/api/ui-actions";
import { Button } from "./ui/button";
import { Input } from "./ui/input";

export function InviteAcceptForm({ token }: { token: string }) {
  const router = useRouter(); const [pending, start] = useTransition(); const [error, setError] = useState("");
  function submit(event: React.FormEvent<HTMLFormElement>) { event.preventDefault(); setError(""); const form = new FormData(event.currentTarget); start(async () => { const result = await acceptInviteAction({ token, name: String(form.get("name")), password: String(form.get("password")), passwordConfirmation: String(form.get("passwordConfirmation")) }); if (!result.ok) setError(result.error); else router.replace("/login?accepted=1"); }); }
  return <form onSubmit={submit} className="mt-7 space-y-5" aria-busy={pending}>
    <div className="space-y-2"><label htmlFor="invite-name" className="text-sm font-medium">Seu nome</label><Input id="invite-name" name="name" required minLength={2} maxLength={100} disabled={pending} autoComplete="name" /></div>
    <div className="space-y-2"><label htmlFor="invite-password" className="text-sm font-medium">Crie sua senha</label><Input id="invite-password" name="password" type="password" required minLength={12} maxLength={128} disabled={pending} autoComplete="new-password" /><p className="text-xs text-muted-foreground">Use pelo menos 12 caracteres.</p></div>
    <div className="space-y-2"><label htmlFor="invite-confirmation" className="text-sm font-medium">Confirme sua senha</label><Input id="invite-confirmation" name="passwordConfirmation" type="password" required minLength={12} maxLength={128} disabled={pending} autoComplete="new-password" /></div>
    {error && <p role="alert" className="text-sm leading-6 text-danger">{error}</p>}
    <Button type="submit" disabled={pending} className="w-full">{pending ? "Criando acesso…" : "Entrar na Tech Sisters"}</Button>
  </form>;
}
