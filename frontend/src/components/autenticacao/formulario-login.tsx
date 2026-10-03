"use client";
import { useState, type FormEvent } from "react";
import { ArrowRight } from "lucide-react";
import { useRouter } from "next/navigation";
import { entrarNoWorkspace } from "@/lib/api/autenticacao";
import { ErroApi } from "@/lib/api/http";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { PasswordInput } from "../ui/password-input";

export function FormularioLogin() {
  const router = useRouter();
  const [enviando, definirEnviando] = useState(false);
  const [mensagemErro, definirMensagemErro] = useState("");
  async function entrar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    definirMensagemErro("");
    definirEnviando(true);
    const dadosFormulario = new FormData(event.currentTarget);
    try {
      await entrarNoWorkspace(
        String(dadosFormulario.get("email")).trim(),
        String(dadosFormulario.get("password")),
      );
      router.replace("/workspace");
      router.refresh();
    } catch (mensagemErro) {
      definirMensagemErro(
        mensagemErro instanceof ErroApi
          ? mensagemErro.status === 401
            ? "Não foi possível entrar. Confira suas credenciais ou procure uma administradora."
            : mensagemErro.message
          : "Não foi possível conectar. Tente novamente em instantes.",
      );
      definirEnviando(false);
    }
  }
  return (
    <form onSubmit={entrar} className="mt-8 space-y-5" aria-busy={enviando}>
      <div className="space-y-2">
        <label htmlFor="email" className="block text-sm font-medium">
          E-mail
        </label>
        <Input
          id="email"
          name="email"
          type="email"
          placeholder="Seu e-mail"
          autoComplete="username"
          required
          maxLength={254}
          disabled={enviando}
          aria-invalid={!!mensagemErro}
          aria-describedby={mensagemErro ? "login-error" : undefined}
        />
      </div>
      <div className="space-y-2">
        <label htmlFor="password" className="block text-sm font-medium">
          Senha
        </label>
        <PasswordInput
          id="password"
          name="password"
          placeholder="Digite sua senha"
          autoComplete="current-password"
          required
          maxLength={128}
          disabled={enviando}
          aria-invalid={!!mensagemErro}
          aria-describedby={mensagemErro ? "login-error" : undefined}
        />
      </div>
      {mensagemErro && (
        <p
          id="login-error"
          role="alert"
          className="text-sm leading-6 text-danger"
        >
          {mensagemErro}
        </p>
      )}
      <Button type="submit" disabled={enviando} className="w-full gap-2">
        {enviando ? "Entrando…" : "Entrar"}
        <ArrowRight aria-hidden="true" size={16} />
      </Button>
    </form>
  );
}
