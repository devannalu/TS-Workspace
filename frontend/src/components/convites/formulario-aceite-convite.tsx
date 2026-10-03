"use client";
import Link from "next/link";
import { useState, useTransition } from "react";
import { CheckCircle2 } from "lucide-react";
import { aceitarConviteComFeedback } from "@/lib/api/acoes";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { PasswordInput } from "../ui/password-input";
export function FormularioAceiteConvite({ token }: { token: string }) {
  const [salvando, iniciarTransicao] = useTransition(),
    [mensagemErro, definirMensagemErro] = useState(""),
    [conviteAceito, definirConviteAceito] = useState(false);
  if (conviteAceito)
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
      aria-busy={salvando}
      onSubmit={(event) => {
        event.preventDefault();
        definirMensagemErro("");
        const dadosFormulario = new FormData(event.currentTarget),
          senha = String(dadosFormulario.get("password")),
          confirmacaoSenha = String(dadosFormulario.get("passwordConfirmation"));
        if (senha !== confirmacaoSenha) {
          definirMensagemErro("As senhas precisam ser iguais.");
          return;
        }
        if (new TextEncoder().encode(senha).length > 72) {
          definirMensagemErro("Use uma senha de até 72 bytes.");
          return;
        }
        iniciarTransicao(async () => {
          const resultadoOperacao = await aceitarConviteComFeedback({
            token,
            name: String(dadosFormulario.get("name")).trim(),
            password: senha,
            passwordConfirmation: confirmacaoSenha,
          });
          if (!resultadoOperacao.ok) definirMensagemErro(resultadoOperacao.error);
          else definirConviteAceito(true);
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
          disabled={salvando}
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
          disabled={salvando}
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
          disabled={salvando}
        />
      </div>
      {mensagemErro && (
        <p role="alert" className="feedback-error">
          {mensagemErro}
        </p>
      )}
      <Button type="submit" disabled={salvando} className="w-full">
        {salvando ? "Criando acesso…" : "Criar meu acesso"}
      </Button>
    </form>
  );
}
