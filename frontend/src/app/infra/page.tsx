"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getJavaHealth } from "@/lib/api/client";
import { getCsrf } from "@/lib/api/auth";

export default function InfrastructurePage() {
  const [state, setState] = useState<"loading" | "up" | "error">("loading");
  const [attempt, setAttempt] = useState(0);
  const [authState, setAuthState] = useState<"loading" | "up" | "error">("loading");

  useEffect(() => {
    let active = true;
    getJavaHealth().then(
      () => { if (active) setState("up"); },
      () => { if (active) setState("error"); },
    );
    getCsrf().then(
      () => { if (active) setAuthState("up"); },
      () => { if (active) setAuthState("error"); },
    );
    return () => { active = false; };
  }, [attempt]);

  return (
    <main className="mx-auto flex min-h-screen max-w-xl flex-col justify-center gap-6 px-6 py-12">
      <p className="text-sm font-medium">TS Workspace</p>
      <h1 className="text-3xl font-semibold">Conexão com o serviço Java</h1>
      <p>Verificação técnica da fundação Java e do banco MySQL.</p>
      <p role="status" aria-live="polite">
        {state === "loading" && "Verificando conexão…"}
        {state === "up" && "Conexão confirmada. Serviço Java e MySQL disponíveis."}
        {state === "error" && "Não foi possível conectar ao serviço Java. Verifique se ele está em execução."}
      </p>
      <p role="status" aria-live="polite">
        Sessão Java: {authState === "loading" && "verificando CSRF…"}
        {authState === "up" && "infraestrutura disponível (CSRF ativo)."}
        {authState === "error" && "infraestrutura de autenticação indisponível."}
      </p>
      <button
        type="button"
        className="w-fit rounded-lg border px-4 py-2 disabled:opacity-50"
        disabled={state === "loading"}
        onClick={() => { setState("loading"); setAttempt((value) => value + 1); }}
      >
        Verificar novamente
      </button>
      <Link className="w-fit underline" href="/">Voltar ao workspace</Link>
    </main>
  );
}
