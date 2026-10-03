"use client";
import { useEffect, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { getCurrentJavaUser } from "@/lib/api/auth";
import { ApiError } from "@/lib/api/http";
import { Skeleton, ErrorState } from "./ui/feedback";

export function SessionBoundary({ children }: { children: ReactNode }) {
  const router = useRouter();
  const [state, setState] = useState<
    | "loading"
    | "authenticated"
    | "unauthenticated"
    | "forbidden"
    | "unavailable"
  >("loading");
  useEffect(() => {
    let active = true;
    const check = () =>
      getCurrentJavaUser()
        .then((user) => {
          if (!active) return;
          setState(user ? "authenticated" : "unauthenticated");
          if (!user) router.replace("/login");
        })
        .catch((error) => {
          if (active)
            setState(
              error instanceof ApiError && error.status === 403
                ? "forbidden"
                : "unavailable",
            );
        });
    const expired = () => {
      setState("unauthenticated");
      router.replace("/login");
    };
    void check();
    window.addEventListener("focus", check);
    window.addEventListener("java-session-expired", expired);
    return () => {
      active = false;
      window.removeEventListener("focus", check);
      window.removeEventListener("java-session-expired", expired);
    };
  }, [router]);
  if (state === "loading")
    return (
      <main className="mx-auto max-w-6xl p-5">
        <Skeleton />
      </main>
    );
  if (state === "unauthenticated")
    return <p role="status">Sessão encerrada. Redirecionando para entrar…</p>;
  if (state === "forbidden")
    return (
      <main className="mx-auto max-w-lg p-6">
        <ErrorState message="Você não tem permissão para acessar esta área." />
      </main>
    );
  if (state === "unavailable")
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
