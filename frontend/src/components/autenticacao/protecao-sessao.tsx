"use client";
import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { buscarUsuarioAtual } from "@/lib/api/autenticacao";
import { ErroApi } from "@/lib/api/http";
import { Skeleton, ErrorState } from "../ui/feedback";

export function ProtecaoSessao({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [situacaoSessao, definirSituacaoSessao] = useState<
    | "loading"
    | "authenticated"
    | "unauthenticated"
    | "forbidden"
    | "unavailable"
  >("loading");
  useEffect(() => {
    let componenteAtivo = true;
    const verificarSessao = () =>
      buscarUsuarioAtual()
        .then((usuario) => {
          if (!componenteAtivo) return;
          definirSituacaoSessao(usuario ? "authenticated" : "unauthenticated");
          if (!usuario) router.replace("/login");
        })
        .catch((error) => {
          if (componenteAtivo)
            definirSituacaoSessao(
              error instanceof ErroApi && error.status === 403
                ? "forbidden"
                : "unavailable",
            );
        });
    const tratarSessaoExpirada = () => {
      definirSituacaoSessao("unauthenticated");
      router.replace("/login");
    };
    void verificarSessao();
    window.addEventListener("focus", verificarSessao);
    window.addEventListener("java-session-expired", tratarSessaoExpirada);
    return () => {
      componenteAtivo = false;
      window.removeEventListener("focus", verificarSessao);
      window.removeEventListener("java-session-expired", tratarSessaoExpirada);
    };
  }, [router]);
  if (situacaoSessao === "loading")
    return (
      <main className="mx-auto max-w-6xl p-5">
        <Skeleton />
      </main>
    );
  if (situacaoSessao === "unauthenticated")
    return <p role="status">Sessão encerrada. Redirecionando para entrar…</p>;
  if (situacaoSessao === "forbidden")
    return (
      <main className="mx-auto max-w-lg p-6">
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </main>
    );
  if (situacaoSessao === "unavailable")
    return (
      <main className="mx-auto max-w-lg p-6">
        <ErrorState
          message="Não foi possível verificar sua sessão. Confira sua conexão."
          onRetry={() => window.location.reload()}
        />
      </main>
    );
  return children;
}
