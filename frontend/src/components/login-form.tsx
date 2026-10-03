"use client";
import { useState, type FormEvent } from "react";
import { ArrowRight } from "lucide-react";
import { useRouter } from "next/navigation";
import { loginJava } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/http";
import { Button } from "./ui/button";
import { Input } from "./ui/input";

export function LoginForm() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(""); setPending(true);
    const form = new FormData(event.currentTarget);
    try {
      await loginJava(String(form.get("email")).trim(), String(form.get("password")));
      router.replace("/workspace");
      router.refresh();
    } catch (error) {
      setError(error instanceof ApiError
        ? error.status === 401 ? "Não foi possível entrar. Confira suas credenciais ou procure uma administradora." : error.message
        : "Não foi possível conectar. Tente novamente em instantes.");
      setPending(false);
    }
  }
  return (
    <form onSubmit={submit} className="mt-8 space-y-5" aria-busy={pending}>
      <div className="space-y-2"><label htmlFor="email" className="block text-sm font-medium">E-mail</label><Input id="email" name="email" type="email" autoComplete="username" required maxLength={254} disabled={pending} /></div>
      <div className="space-y-2"><label htmlFor="password" className="block text-sm font-medium">Senha</label><Input id="password" name="password" type="password" autoComplete="current-password" required maxLength={128} disabled={pending} /></div>
      {error && <p role="alert" className="text-sm leading-6 text-danger">{error}</p>}
      <Button type="submit" disabled={pending} className="w-full gap-2">{pending ? "Entrando…" : "Entrar"}<ArrowRight aria-hidden="true" size={16} /></Button>
    </form>
  );
}
