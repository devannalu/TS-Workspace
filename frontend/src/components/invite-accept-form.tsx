"use client";
import Link from "next/link";
import { useState, useTransition } from "react";
import { CheckCircle2 } from "lucide-react";
import { acceptInviteAction } from "@/lib/api/ui-actions";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { PasswordInput } from "./ui/password-input";
export function InviteAcceptForm({ token }: { token: string }) {
  const [pending, start] = useTransition(),
    [error, setError] = useState(""),
    [success, setSuccess] = useState(false);
  if (success)
    return (
      <div role="status" className="mt-6 rounded-xl bg-success-soft p-5">
        <CheckCircle2 size={28} aria-hidden className="text-success" />
        <h2 className="mt-3 text-xl font-semibold">Seu acesso está pronto.</h2>
        <p className="mt-2 subtle">
          Entre com seu e-mail e a senha que acabou de criar.
        </p>
        <Link
          href="/login?accepted=1"
          className="mt-5 inline-flex min-h-11 items-center rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground"
        >
          Ir para o login
        </Link>
      </div>
    );
  return (
    <form
      className="mt-6 space-y-4"
      aria-busy={pending}
      onSubmit={(event) => {
        event.preventDefault();
        setError("");
        const form = new FormData(event.currentTarget),
          password = String(form.get("password")),
          passwordConfirmation = String(form.get("passwordConfirmation"));
        if (password !== passwordConfirmation) {
          setError("As senhas precisam ser iguais.");
          return;
        }
        if (new TextEncoder().encode(password).length > 72) {
          setError("Use uma senha de até 72 bytes.");
          return;
        }
        start(async () => {
          const result = await acceptInviteAction({
            token,
            name: String(form.get("name")).trim(),
            password,
            passwordConfirmation,
          });
          if (!result.ok) setError(result.error);
          else setSuccess(true);
        });
      }}
    >
      <label className="field">
        Seu nome
        <Input
          name="name"
          autoComplete="name"
          required
          minLength={2}
          maxLength={100}
          disabled={pending}
        />
      </label>
      <div className="field">
        <label htmlFor="invite-password">Crie sua senha</label>
        <PasswordInput
          id="invite-password"
          name="password"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={72}
          disabled={pending}
        />
      </div>
      <p className="text-xs text-muted-foreground">
        Use pelo menos 12 caracteres.
      </p>
      <div className="field">
        <label htmlFor="invite-confirmation">Confirme sua senha</label>
        <PasswordInput
          id="invite-confirmation"
          name="passwordConfirmation"
          autoComplete="new-password"
          required
          minLength={12}
          maxLength={72}
          disabled={pending}
        />
      </div>
      {error && (
        <p role="alert" className="feedback-error">
          {error}
        </p>
      )}
      <Button type="submit" disabled={pending} className="w-full">
        {pending ? "Criando acesso…" : "Criar meu acesso"}
      </Button>
    </form>
  );
}
